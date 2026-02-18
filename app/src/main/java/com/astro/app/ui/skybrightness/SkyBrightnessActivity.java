package com.astro.app.ui.skybrightness;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.media.ExifInterface;

import com.astro.app.R;
import com.astro.app.data.model.SkyBrightnessResult;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity that lets the user take or pick a sky photo and estimates
 * the Bortle dark-sky class from image brightness and EXIF metadata.
 */
public class SkyBrightnessActivity extends AppCompatActivity {

    private static final String TAG = "SkyBrightnessActivity";
    private static final int MAX_IMAGE_DIMENSION = 1920;

    // --- Views ---
    private ImageView ivPreview;
    private ProgressBar progressBar;
    private MaterialButton btnCapture;
    private MaterialButton btnGallery;

    // Result views
    private MaterialCardView cardResult;
    private TextView tvBortleNumber;
    private TextView tvBortleLabel;
    private TextView tvBortleDescription;
    private BortleScaleView bortleGauge;

    private MaterialCardView cardExif;
    private TextView tvIso;
    private TextView tvExposureTime;
    private TextView tvFNumber;

    private TextView tvNormalizedBrightness;

    private MaterialCardView cardTip;
    private TextView tvTip;

    private TextView tvDisclaimer;

    // --- State ---
    private Uri cameraPhotoUri;
    private ExecutorService executor;

    // --- Activity result launchers ---

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        processImage(uri);
                    }
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraPhotoUri != null) {
                    processImage(cameraPhotoUri);
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            });

    // -----------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sky_brightness);

        executor = Executors.newSingleThreadExecutor();

        bindViews();
        setupToolbar();
        setupButtons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    // -----------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------

    private void bindViews() {
        ivPreview = findViewById(R.id.ivPreview);
        progressBar = findViewById(R.id.progressBar);
        btnCapture = findViewById(R.id.btnCapture);
        btnGallery = findViewById(R.id.btnGallery);

        cardResult = findViewById(R.id.cardResult);
        tvBortleNumber = findViewById(R.id.tvBortleNumber);
        tvBortleLabel = findViewById(R.id.tvBortleLabel);
        tvBortleDescription = findViewById(R.id.tvBortleDescription);
        bortleGauge = findViewById(R.id.bortleGauge);

        cardExif = findViewById(R.id.cardExif);
        tvIso = findViewById(R.id.tvIso);
        tvExposureTime = findViewById(R.id.tvExposureTime);
        tvFNumber = findViewById(R.id.tvFNumber);

        tvNormalizedBrightness = findViewById(R.id.tvNormalizedBrightness);

        cardTip = findViewById(R.id.cardTip);
        tvTip = findViewById(R.id.tvTip);

        tvDisclaimer = findViewById(R.id.tvDisclaimer);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupButtons() {
        btnCapture.setOnClickListener(v -> onCaptureClicked());
        btnGallery.setOnClickListener(v -> onGalleryClicked());
    }

    // -----------------------------------------------------------------
    // Button handlers
    // -----------------------------------------------------------------

    private void onCaptureClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        try {
            File photoFile = new File(getExternalCacheDir(),
                    "sky_brightness_" + System.currentTimeMillis() + ".jpg");
            cameraPhotoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraPhotoUri);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch camera", e);
            Toast.makeText(this, "Failed to launch camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void onGalleryClicked() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        galleryLauncher.launch(Intent.createChooser(intent, "Select Sky Image"));
    }

    // -----------------------------------------------------------------
    // Image processing
    // -----------------------------------------------------------------

    private void processImage(@NonNull Uri uri) {
        setLoading(true);
        hideResults();

        executor.execute(() -> {
            try {
                // Load bitmap (scaled down to avoid OOM)
                Bitmap bitmap = loadScaledBitmap(uri);
                if (bitmap == null) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Extract EXIF
                ExifInterface exif = loadExif(uri);

                // Analyze
                SkyBrightnessResult result = SkyBrightnessAnalyzer.analyze(bitmap, exif);

                // Show on UI thread
                runOnUiThread(() -> {
                    setLoading(false);
                    ivPreview.setImageBitmap(bitmap);
                    displayResult(result);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error processing image", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error analyzing image: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Bitmap loadScaledBitmap(@NonNull Uri uri) {
        try {
            // First pass: get dimensions
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, opts);
            }

            // Calculate sample size
            int maxDim = Math.max(opts.outWidth, opts.outHeight);
            int sampleSize = 1;
            while (maxDim / sampleSize > MAX_IMAGE_DIMENSION) {
                sampleSize *= 2;
            }

            // Second pass: decode
            opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            opts.inScaled = false;
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                return BitmapFactory.decodeStream(is, null, opts);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to load bitmap", e);
            return null;
        }
    }

    private ExifInterface loadExif(@NonNull Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            return new ExifInterface(is);
        } catch (Exception e) {
            Log.w(TAG, "Failed to read EXIF", e);
            return null;
        }
    }

    // -----------------------------------------------------------------
    // Display
    // -----------------------------------------------------------------

    private void displayResult(@NonNull SkyBrightnessResult result) {
        // Bortle number with colour
        int bortle = result.getBortleClass();
        tvBortleNumber.setText(String.valueOf(bortle));
        tvBortleNumber.setTextColor(bortleColor(bortle));

        tvBortleLabel.setText(result.getLabel());
        tvBortleDescription.setText(result.getDescription());
        cardResult.setVisibility(View.VISIBLE);

        // Gauge
        bortleGauge.setBortleClass(bortle);
        bortleGauge.setVisibility(View.VISIBLE);

        // EXIF card
        if (result.hasExifData()) {
            tvIso.setText(String.valueOf(result.getIso()));
            tvExposureTime.setText(formatExposureTime(result.getExposureTime()));
            tvFNumber.setText(String.format(Locale.US, "f/%.1f", result.getFNumber()));
            cardExif.setVisibility(View.VISIBLE);
        } else {
            cardExif.setVisibility(View.GONE);
        }

        // Normalized brightness
        String brightnessText;
        if (result.hasExifData()) {
            brightnessText = String.format(Locale.US,
                    "Normalized brightness: %.6f  |  Median pixel: %.0f",
                    result.getNormalizedBrightness(), result.getMedianPixelValue());
        } else {
            brightnessText = String.format(Locale.US,
                    "Median pixel: %.0f / 255  (no EXIF data -- estimate less accurate)",
                    result.getMedianPixelValue());
        }
        tvNormalizedBrightness.setText(brightnessText);
        tvNormalizedBrightness.setVisibility(View.VISIBLE);

        // Tip
        tvTip.setText(result.getTip());
        cardTip.setVisibility(View.VISIBLE);

        // Disclaimer
        tvDisclaimer.setVisibility(View.VISIBLE);
    }

    private void hideResults() {
        cardResult.setVisibility(View.GONE);
        bortleGauge.setVisibility(View.GONE);
        cardExif.setVisibility(View.GONE);
        tvNormalizedBrightness.setVisibility(View.GONE);
        cardTip.setVisibility(View.GONE);
        tvDisclaimer.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnCapture.setEnabled(!loading);
        btnGallery.setEnabled(!loading);
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    /**
     * Returns a colour for the Bortle number text:
     * green for 1-3, yellow for 4-5, orange for 6-7, red for 8-9.
     */
    private static int bortleColor(int bortle) {
        if (bortle <= 3) return 0xFF4CAF50; // green
        if (bortle <= 5) return 0xFFFFEB3B; // yellow
        if (bortle <= 7) return 0xFFFF9800; // orange
        return 0xFFF44336;                  // red
    }

    /**
     * Formats exposure time for display (e.g. "1/60s" or "2.5s").
     */
    private static String formatExposureTime(double seconds) {
        if (seconds <= 0) return "--";
        if (seconds < 1.0) {
            int denominator = (int) Math.round(1.0 / seconds);
            return "1/" + denominator + "s";
        }
        if (seconds == Math.floor(seconds)) {
            return (int) seconds + "s";
        }
        return String.format(Locale.US, "%.1fs", seconds);
    }
}
