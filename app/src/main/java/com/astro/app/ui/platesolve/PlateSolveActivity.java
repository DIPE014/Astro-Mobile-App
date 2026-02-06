package com.astro.app.ui.platesolve;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.astro.app.R;
import com.astro.app.native_.AstrometryNative;
import com.astro.app.native_.ConstellationOverlay;
import com.astro.app.native_.NativePlateSolver;

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

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
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
                if (cbShowStars != null) {
                    cbShowStars.setChecked(false);
                    cbShowStars.setEnabled(false);
                    cbShowStars.setVisibility(View.GONE);
                }
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
}
