package com.astro.app.ui.platesolve;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.astro.app.R;
import com.astro.app.data.model.SkyBrightnessResult;
import com.astro.app.native_.AstrometryNative;
import com.astro.app.native_.ConstellationOverlay;
import com.astro.app.native_.NativePlateSolver;
import com.astro.app.ui.skybrightness.BortleScaleView;
import com.astro.app.ui.skybrightness.SkyBrightnessAnalyzer;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Constellation detection activity.
 * Capture or pick a photo of the night sky, automatically detect and overlay constellations.
 */
public class PlateSolveActivity extends AppCompatActivity {
    private static final String TAG = "PlateSolveActivity";
    private static final String PREFS_NAME = "astro_settings";
    private static final String KEY_HAS_SEEN_TIPS = "has_seen_plate_solve_tips";

    private ImageView imageView;
    private TextView tvStatus;
    private Button btnPickImage;
    private Button btnCapture;
    private ProgressBar progressBar;
    private CheckBox cbShowStars;

    private Bitmap originalBitmap;
    private Bitmap overlayBitmap;
    private NativePlateSolver solver;
    private ConstellationOverlay constellationOverlay;
    private ExecutorService executor;
    private Uri cameraPhotoUri;
    private SkyBrightnessResult skyResult;
    private Button btnSkyQuality;

    // Gallery picker
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    loadImageAndSolve(imageUri);
                }
            });

    // Camera capture
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraPhotoUri != null) {
                    loadImageAndSolve(cameraPhotoUri);
                }
            });

    // Camera permission request
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plate_solve);

        imageView = findViewById(R.id.imageView);
        tvStatus = findViewById(R.id.tvStatus);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnCapture = findViewById(R.id.btnCapture);
        progressBar = findViewById(R.id.progressBar);
        cbShowStars = findViewById(R.id.cbShowStars);
        btnSkyQuality = findViewById(R.id.btnSkyQuality);
        if (btnSkyQuality != null) {
            btnSkyQuality.setOnClickListener(v -> showSkyBrightnessDialog());
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        ImageButton btnInfo = findViewById(R.id.btnInfo);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> showTipsDialog());
        }

        // Show tips dialog on first launch
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_HAS_SEEN_TIPS, false)) {
            showTipsDialog();
        }

        executor = Executors.newSingleThreadExecutor();
        solver = new NativePlateSolver(this);

        constellationOverlay = new ConstellationOverlay();
        constellationOverlay.loadConstellations(this);

        if (cbShowStars != null) {
            cbShowStars.setOnCheckedChangeListener((buttonView, isChecked) -> updateImageDisplay());
        }

        // Load index files
        if (AstrometryNative.isLibraryLoaded()) {
            try {
                solver.loadIndexesFromAssets("indexes");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load index files: " + e.getMessage());
            }
        }

        btnPickImage.setOnClickListener(v -> pickImage());
        btnCapture.setOnClickListener(v -> captureImage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Star Field Image"));
    }

    private void captureImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        File photoFile = new File(getCacheDir(), "capture_" + System.currentTimeMillis() + ".jpg");
        cameraPhotoUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", photoFile);
        cameraLauncher.launch(cameraPhotoUri);
    }

    private void loadImageAndSolve(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            originalBitmap = BitmapFactory.decodeStream(inputStream, null, opts);
            inputStream.close();

            if (originalBitmap != null) {
                imageView.setImageBitmap(originalBitmap);
                overlayBitmap = null;
                skyResult = null;
                if (cbShowStars != null) {
                    cbShowStars.setChecked(false);
                    cbShowStars.setEnabled(false);
                    cbShowStars.setVisibility(View.GONE);
                }
                if (btnSkyQuality != null) {
                    btnSkyQuality.setVisibility(View.GONE);
                }
                analyzeSkyBrightness(uri);
                solvePlate();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load image", e);
            showStatus("Failed to load image");
        }
    }

    private void updateImageDisplay() {
        if (originalBitmap == null) return;

        if (cbShowStars != null && cbShowStars.isChecked() && overlayBitmap != null) {
            imageView.setImageBitmap(overlayBitmap);
        } else {
            imageView.setImageBitmap(originalBitmap);
        }
    }

    private void solvePlate() {
        if (originalBitmap == null) return;

        setLoading(true);
        showStatus("Detecting constellations...");

        executor.execute(() -> {
            solver.solve(originalBitmap, new NativePlateSolver.SolveCallback() {
                @Override
                public void onProgress(String message) {
                    runOnUiThread(() -> showStatus(message));
                }

                @Override
                public void onSuccess(AstrometryNative.SolveResult result) {
                    overlayBitmap = constellationOverlay.drawOverlay(originalBitmap, result);

                    runOnUiThread(() -> {
                        setLoading(false);
                        hideStatus();

                        imageView.setImageBitmap(overlayBitmap);

                        if (cbShowStars != null) {
                            cbShowStars.setVisibility(View.VISIBLE);
                            cbShowStars.setEnabled(true);
                            cbShowStars.setChecked(true);
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showStatus("Could not identify star field");
                    });
                }
            });
        });
    }

    private void showStatus(String text) {
        tvStatus.setText(text);
        tvStatus.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        tvStatus.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPickImage.setEnabled(!loading);
        btnCapture.setEnabled(!loading);
    }

    private void analyzeSkyBrightness(Uri uri) {
        Bitmap bitmapSnapshot = originalBitmap;
        executor.execute(() -> {
            ExifInterface exif = null;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    exif = new ExifInterface(is);
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not read EXIF for sky analysis", e);
            }
            SkyBrightnessResult result = SkyBrightnessAnalyzer.analyze(bitmapSnapshot, exif);
            skyResult = result;
            runOnUiThread(() -> {
                if (btnSkyQuality != null) {
                    btnSkyQuality.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void showSkyBrightnessDialog() {
        if (skyResult == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sky_brightness, null);

        TextView tvBortleNumber = dialogView.findViewById(R.id.tvBortleNumber);
        TextView tvBortleLabel = dialogView.findViewById(R.id.tvBortleLabel);
        TextView tvDescription = dialogView.findViewById(R.id.tvDescription);
        BortleScaleView bortleGauge = dialogView.findViewById(R.id.bortleGauge);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

        int bortle = skyResult.getBortleClass();
        tvBortleNumber.setText(String.valueOf(bortle));
        tvBortleNumber.setTextColor(bortleColor(bortle));
        tvBortleLabel.setText(skyResult.getLabel());
        tvDescription.setText(skyResult.getDescription());
        bortleGauge.setBortleClass(bortle);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_AstroApp_AlertDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private static int bortleColor(int bortle) {
        if (bortle <= 3) return 0xFF4CAF50;
        if (bortle <= 5) return 0xFFFFEB3B;
        if (bortle <= 7) return 0xFFFF9800;
        return 0xFFF44336;
    }

    private void showTipsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plate_solve_tips, null);

        CheckBox cbDontShowAgain = dialogView.findViewById(R.id.cbDontShowAgain);
        com.google.android.material.button.MaterialButton btnGotIt = dialogView.findViewById(R.id.btnGotIt);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_AstroApp_AlertDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnGotIt.setOnClickListener(v -> {
            if (cbDontShowAgain.isChecked()) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_HAS_SEEN_TIPS, true).apply();
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}
