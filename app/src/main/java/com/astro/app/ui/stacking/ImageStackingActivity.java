package com.astro.app.ui.stacking;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.astro.app.R;
import com.astro.app.data.model.SkyBrightnessResult;
import com.astro.app.native_.AstrometryNative;
import com.astro.app.native_.ConstellationOverlay;
import com.astro.app.native_.ImageStackingManager;
import com.astro.app.native_.NativePlateSolver;
import com.astro.app.ui.skybrightness.BortleScaleView;
import com.astro.app.ui.skybrightness.SkyBrightnessAnalyzer;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Unified camera activity for plate solving and optional image stacking.
 *
 * Two modes:
 *  - Stacking OFF (default): Capture or pick 1 image, plate solve it, show result
 *    with constellation overlay and sky quality analysis.
 *  - Stacking ON: Capture/pick up to 10 frames, stack them, plate solve the
 *    stacked result, show result with constellation overlay and sky quality.
 */
public class ImageStackingActivity extends AppCompatActivity {
    private static final String TAG = "ImageStackingActivity";
    private static final int MAX_FRAMES = 10;
    private static final int MAX_PROCESSING_DIMENSION = 4096;
    private static final String PREFS_NAME = "astro_settings";
    private static final String KEY_HAS_SEEN_CAMERA_TIPS = "has_seen_camera_tips_stacking";

    // Tooltip tutorial
    private com.astro.app.ui.onboarding.TooltipManager tooltipManager;

    // ---- Capture mode views ----
    private View topControls;
    private View stackingToggleRow;
    private MaterialCardView statusCard;
    private View btnViewExample;
    private View bottomControls;
    private View loadingOverlay;
    private CircularProgressIndicator progressIndicator;

    private MaterialButton btnCapture;
    private MaterialButton btnFinish;
    private MaterialButton btnBack;
    private MaterialButton btnPickImages;
    private TextView tvFrameCount;
    private TextView tvStatus;
    private SwitchMaterial switchStacking;

    // ---- Collection ----
    private final List<Uri> collectedUris = new ArrayList<>();
    private ThumbnailAdapter thumbnailAdapter;
    private RecyclerView thumbnailGrid;

    // ---- Result mode views ----
    private View resultContainer;
    private View resultBottomBar;
    private ImageView resultImageView;
    private ProgressBar resultProgressBar;
    private TextView resultStatus;
    private MaterialButton btnNewScan;
    private Button btnSkyQuality;
    private CheckBox cbConstellations;

    // ---- Background executor ----
    private ExecutorService backgroundExecutor;

    // ---- Stacking ----
    private ImageStackingManager stackingManager;
    private volatile boolean isDestroyed = false;
    private volatile boolean isCancelled = false;

    // ---- Plate solving ----
    private NativePlateSolver solver;
    private ConstellationOverlay constellationOverlay;

    // ---- Result state ----
    private Bitmap capturedBitmap;    // single-frame color bitmap (stacking OFF)
    private Bitmap originalBitmap;    // bitmap shown in result view (before overlay)
    private Bitmap overlayBitmap;     // bitmap with constellation overlay drawn
    private SkyBrightnessResult skyResult;
    private boolean inResultMode = false;

    // ---- Camera (native app via TakePicture contract) ----
    private Uri pendingCameraUri;

    // ---- Camera launcher ----
    private final ActivityResultLauncher<Uri> cameraLauncher =
        registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && pendingCameraUri != null) {
                addCollectedImage(pendingCameraUri);
            }
        });

    // ---- Camera permission ----
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                launchNativeCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        });

    // ---- Photo Picker - multi-select (stacking ON) ----
    private final ActivityResultLauncher<PickVisualMediaRequest> galleryMultiLauncher =
        registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(MAX_FRAMES), uris -> {
            if (uris != null) {
                for (Uri uri : uris) {
                    if (collectedUris.size() < MAX_FRAMES) {
                        addCollectedImage(uri);
                    }
                }
            }
        });

    // ---- Photo Picker - single-select (stacking OFF) ----
    private final ActivityResultLauncher<PickVisualMediaRequest> gallerySingleLauncher =
        registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                collectedUris.clear();
                thumbnailAdapter.notifyDataSetChanged();
                addCollectedImage(uri);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_stacking);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeViews();

        // Initialize managers
        stackingManager = new ImageStackingManager();
        backgroundExecutor = Executors.newSingleThreadExecutor();

        // Initialize plate solver and constellation overlay
        solver = new NativePlateSolver(this);
        constellationOverlay = new ConstellationOverlay();
        constellationOverlay.loadConstellations(this);

        if (AstrometryNative.isLibraryLoaded()) {
            try {
                solver.loadIndexesFromAssets("indexes");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load index files: " + e.getMessage());
            }
        }

        setupClickListeners();
        showCameraTipsIfNeeded();
    }

    private void initializeViews() {
        // Capture mode views
        topControls = findViewById(R.id.topControls);
        stackingToggleRow = findViewById(R.id.stackingToggleRow);
        statusCard = findViewById(R.id.statusCard);
        btnViewExample = findViewById(R.id.btnViewExample);
        if (btnViewExample != null) {
            btnViewExample.setOnClickListener(v -> showExampleImageDialog());
        }
        bottomControls = findViewById(R.id.bottomControls);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        progressIndicator = findViewById(R.id.progressIndicator);

        btnCapture = findViewById(R.id.btnCapture);
        btnFinish = findViewById(R.id.btnFinish);
        btnBack = findViewById(R.id.btnBack);
        btnPickImages = findViewById(R.id.btnPickImages);
        tvFrameCount = findViewById(R.id.tvFrameCount);
        tvStatus = findViewById(R.id.tvStatus);
        switchStacking = findViewById(R.id.switchStacking);

        // Thumbnail grid
        thumbnailGrid = findViewById(R.id.thumbnailGrid);
        thumbnailAdapter = new ThumbnailAdapter();
        thumbnailGrid.setLayoutManager(new GridLayoutManager(this, 3));
        thumbnailGrid.setAdapter(thumbnailAdapter);

        // Result mode views
        resultContainer = findViewById(R.id.resultContainer);
        resultBottomBar = findViewById(R.id.resultBottomBar);
        resultImageView = findViewById(R.id.resultImageView);
        resultProgressBar = findViewById(R.id.resultProgressBar);
        resultStatus = findViewById(R.id.resultStatus);
        btnNewScan = findViewById(R.id.btnNewScan);
        btnSkyQuality = findViewById(R.id.btnSkyQuality);
        cbConstellations = findViewById(R.id.cbConstellations);

        // Initial state
        btnCapture.setEnabled(true);
        btnFinish.setVisibility(View.VISIBLE);
        btnFinish.setEnabled(false);
        tvFrameCount.setText("0 / 1 frames");
        tvStatus.setText("Tap Capture or pick an image.");

        updateModeUI();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        MaterialButton btnInfo = findViewById(R.id.btnInfo);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> showCameraTipsDialog());
        }

        btnCapture.setOnClickListener(v -> captureFrame());
        btnFinish.setOnClickListener(v -> finishStacking());
        btnPickImages.setOnClickListener(v -> openGalleryPicker());

        switchStacking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            resetSession();
            updateModeUI();
        });

        // Cancel stacking by tapping the loading overlay
        loadingOverlay.setOnClickListener(v -> {
            isCancelled = true;
            tvStatus.setText("Cancelling...");
        });

        // Result mode listeners
        btnNewScan.setOnClickListener(v -> showCaptureMode());

        btnSkyQuality.setOnClickListener(v -> showSkyBrightnessDialog());

        cbConstellations.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (resultImageView == null || originalBitmap == null) return;
            if (isChecked && overlayBitmap != null) {
                resultImageView.setImageBitmap(overlayBitmap);
            } else {
                resultImageView.setImageBitmap(originalBitmap);
            }
        });
    }

    // =========================================================================
    // Mode UI
    // =========================================================================

    private void updateModeUI() {
        boolean stacking = switchStacking.isChecked();
        if (stacking) {
            btnCapture.setText("Take Photo");
            btnPickImages.setText("Pick from Gallery");
            tvFrameCount.setVisibility(View.VISIBLE);
            btnFinish.setVisibility(View.VISIBLE);
            btnFinish.setText("Stack & Solve");
            tvStatus.setText("Stacking ON \u2014 capture up to 10 frames, then Stack & Solve.");
        } else {
            btnCapture.setText("Capture");
            btnPickImages.setText("Pick Image");
            tvFrameCount.setVisibility(View.GONE);
            btnFinish.setVisibility(View.GONE);
            tvStatus.setText("Tap Capture or pick an image.");
        }
        btnFinish.setEnabled(false);
        tvFrameCount.setText("0 / " + (stacking ? MAX_FRAMES : 1) + " frames");
    }

    private void showResultMode(Bitmap bitmap) {
        inResultMode = true;

        // Hide capture views
        topControls.setVisibility(View.GONE);
        stackingToggleRow.setVisibility(View.GONE);
        thumbnailGrid.setVisibility(View.GONE);
        statusCard.setVisibility(View.GONE);
        if (btnViewExample != null) btnViewExample.setVisibility(View.GONE);
        bottomControls.setVisibility(View.GONE);

        // Show result views
        resultContainer.setVisibility(View.VISIBLE);
        resultBottomBar.setVisibility(View.VISIBLE);

        // Reset result state
        overlayBitmap = null;
        skyResult = null;
        cbConstellations.setVisibility(View.GONE);
        cbConstellations.setEnabled(false);
        cbConstellations.setChecked(false);
        btnSkyQuality.setVisibility(View.GONE);

        originalBitmap = bitmap;
        resultImageView.setImageBitmap(bitmap);
    }

    private void showCaptureMode() {
        inResultMode = false;

        // Show capture views
        topControls.setVisibility(View.VISIBLE);
        stackingToggleRow.setVisibility(View.VISIBLE);
        thumbnailGrid.setVisibility(View.VISIBLE);
        statusCard.setVisibility(View.VISIBLE);
        bottomControls.setVisibility(View.VISIBLE);

        // Hide result views
        resultContainer.setVisibility(View.GONE);
        resultBottomBar.setVisibility(View.GONE);

        // Reset state
        resetSession();
        updateModeUI();
        updateCollectionUI();

        // Show example card again
        if (btnViewExample != null) {
            btnViewExample.setVisibility(View.VISIBLE);
        }

        // Clear bitmaps to free memory
        originalBitmap = null;
        overlayBitmap = null;
        capturedBitmap = null;
        skyResult = null;
    }

    private void resetSession() {
        capturedBitmap = null;
        collectedUris.clear();
        if (thumbnailAdapter != null) thumbnailAdapter.notifyDataSetChanged();
        stackingManager.release();
        stackingManager = new ImageStackingManager();
    }

    // =========================================================================
    // Collection management
    // =========================================================================

    private void addCollectedImage(Uri uri) {
        if (collectedUris.size() >= MAX_FRAMES) {
            Toast.makeText(this, "Maximum " + MAX_FRAMES + " images", Toast.LENGTH_SHORT).show();
            return;
        }
        collectedUris.add(uri);
        thumbnailAdapter.notifyItemInserted(collectedUris.size() - 1);

        // Hide example card once user starts adding images
        if (btnViewExample != null) {
            btnViewExample.setVisibility(View.GONE);
        }

        updateCollectionUI();

        // If stacking OFF, auto-trigger processing
        if (!switchStacking.isChecked()) {
            processAllImages();
        }
    }

    private void removeCollectedImage(int position) {
        if (position < 0 || position >= collectedUris.size()) return;
        collectedUris.remove(position);
        thumbnailAdapter.notifyItemRemoved(position);
        thumbnailAdapter.notifyItemRangeChanged(position, collectedUris.size() - position);
        updateCollectionUI();
    }

    private void updateCollectionUI() {
        int count = collectedUris.size();
        boolean stacking = switchStacking.isChecked();
        tvFrameCount.setText(count + " / " + (stacking ? MAX_FRAMES : 1) + " frames");

        if (stacking) {
            btnCapture.setEnabled(count < MAX_FRAMES);
            btnPickImages.setEnabled(count < MAX_FRAMES);
            btnFinish.setEnabled(count >= 1);
            btnFinish.setText("Stack & Solve (" + count + " frame" + (count == 1 ? "" : "s") + ")");
            if (count == 0) {
                tvStatus.setText("Stacking ON \u2014 capture up to 10 frames, then Stack & Solve.");
            } else {
                tvStatus.setText(count + " image(s) collected. Add more or tap Stack & Solve.");
            }
        } else {
            btnCapture.setEnabled(count == 0);
            btnPickImages.setEnabled(count == 0);
            tvStatus.setText(count == 0 ? "Tap Capture or pick an image." : "Processing...");
        }
    }

    // =========================================================================
    // Gallery picker
    // =========================================================================

    private void openGalleryPicker() {
        PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();
        if (switchStacking.isChecked()) {
            galleryMultiLauncher.launch(request);
        } else {
            gallerySingleLauncher.launch(request);
        }
    }

    /**
     * Load a bitmap from URI with optional max dimension clamping.
     * @param uri        image URI
     * @param maxDimension  max width/height (0 = no limit, full resolution)
     */
    private Bitmap loadBitmapFromUri(Uri uri, int maxDimension) {
        try {
            int sampleSize = 1;
            if (maxDimension > 0) {
                // First pass: decode bounds only to determine dimensions
                try (InputStream boundsStream = getContentResolver().openInputStream(uri)) {
                    if (boundsStream == null) return null;
                    BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
                    boundsOpts.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(boundsStream, null, boundsOpts);

                    int maxDim = Math.max(boundsOpts.outWidth, boundsOpts.outHeight);
                    while (maxDim / sampleSize > maxDimension) {
                        sampleSize *= 2;
                    }
                }
            }

            // Decode at target resolution
            try (InputStream imageStream = getContentResolver().openInputStream(uri)) {
                if (imageStream == null) return null;
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inScaled = false;
                opts.inSampleSize = sampleSize;
                return BitmapFactory.decodeStream(imageStream, null, opts);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode bitmap from URI", e);
            return null;
        }
    }

    private Bitmap loadBitmapFromUri(Uri uri) {
        return loadBitmapFromUri(uri, 0);
    }

    // =========================================================================
    // Camera capture
    // =========================================================================

    private void captureFrame() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchNativeCamera();
        }
    }

    private void launchNativeCamera() {
        File photoFile = new File(getCacheDir(), "stack_capture_" + System.currentTimeMillis() + ".jpg");
        pendingCameraUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", photoFile);
        cameraLauncher.launch(pendingCameraUri);
    }

    // =========================================================================
    // Batch processing
    // =========================================================================

    private void processAllImages() {
        if (collectedUris.isEmpty()) {
            Toast.makeText(this, "No images to process", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        btnCapture.setEnabled(false);
        btnPickImages.setEnabled(false);
        btnFinish.setEnabled(false);
        switchStacking.setEnabled(false);
        tvStatus.setText("Processing " + collectedUris.size() + " image(s)...");

        final List<Uri> uris = new ArrayList<>(collectedUris);
        final boolean stacking = switchStacking.isChecked();
        isCancelled = false;

        backgroundExecutor.execute(() -> {
            Bitmap resultBitmap = null;
            try {
                if (stacking && uris.size() > 1) {
                    resultBitmap = stackAllFrames(uris);
                } else {
                    resultBitmap = loadBitmapFromUri(uris.get(0), MAX_PROCESSING_DIMENSION);
                }
            } catch (Exception e) {
                Log.e(TAG, "Processing failed", e);
            }

            final Bitmap finalResult;
            if (isCancelled) {
                if (resultBitmap != null) resultBitmap.recycle();
                finalResult = null;
            } else {
                finalResult = resultBitmap;
            }
            final boolean wasCancelled = isCancelled;
            runOnUiThread(() -> {
                if (isDestroyed) return;
                showLoading(false);
                switchStacking.setEnabled(true);
                if (wasCancelled) {
                    // User cancelled — silently return to capture mode
                    btnCapture.setEnabled(true);
                    btnPickImages.setEnabled(true);
                    if (stacking) btnFinish.setEnabled(!collectedUris.isEmpty());
                } else if (finalResult != null) {
                    showResultMode(finalResult);
                    solvePlate(finalResult);
                    analyzeSkyBrightness(finalResult);
                } else {
                    Toast.makeText(ImageStackingActivity.this, "Processing failed", Toast.LENGTH_SHORT).show();
                    btnCapture.setEnabled(true);
                    btnPickImages.setEnabled(true);
                    if (stacking) btnFinish.setEnabled(!collectedUris.isEmpty());
                }
            });
        });
    }

    private Bitmap stackAllFrames(List<Uri> uris) {
        int targetW = 0, targetH = 0;
        boolean sessionOk = false;

        for (int i = 0; i < uris.size(); i++) {
            if (isDestroyed || isCancelled) {
                if (isCancelled) {
                    stackingManager.release();
                    runOnUiThread(() -> tvStatus.setText("Stacking cancelled."));
                }
                return null;
            }

            final int idx = i;
            runOnUiThread(() -> tvStatus.setText("Processing frame " + (idx + 1) + " / " + uris.size() + "..."));

            Bitmap bitmap = loadBitmapFromUri(uris.get(i), MAX_PROCESSING_DIMENSION);
            if (bitmap == null) {
                Log.w(TAG, "Failed to load frame " + i);
                continue;
            }

            // Scale to match first frame if dimensions differ
            if (targetW == 0) {
                targetW = bitmap.getWidth();
                targetH = bitmap.getHeight();
            } else if (bitmap.getWidth() != targetW || bitmap.getHeight() != targetH) {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true);
                bitmap.recycle();
                bitmap = scaled;
            }

            boolean ok;
            if (!sessionOk) {
                ok = stackingManager.startSession(bitmap, createCallback());
                sessionOk = ok;
            } else {
                ok = stackingManager.addFrame(bitmap);
            }
            bitmap.recycle();
            bitmap = null;
            System.gc(); // Hint to reclaim bitmap memory before next frame

            if (!ok) {
                Log.w(TAG, "Stacking failed for frame " + i);
            }
        }

        if (sessionOk && stackingManager.getFrameCount() > 0) {
            Bitmap result = stackingManager.getResult();
            stackingManager.release(); // Free native accumulator memory promptly
            return result;
        }
        return null;
    }

    private ImageStackingManager.StackingCallback createCallback() {
        return new ImageStackingManager.StackingCallback() {
            @Override
            public void onFrameStacked(int frameNumber, int totalFrames, int inliers, double rmsError) {
                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    Log.i(TAG, String.format("Frame %d stacked: %d inliers, RMS=%.2f px",
                        frameNumber, inliers, rmsError));
                    tvStatus.setText(String.format("Aligned: %d stars, RMS: %.1f px",
                        inliers, rmsError));
                });
            }

            @Override
            public void onAlignmentFailed(int frameNumber, String reason) {
                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    Log.w(TAG, "Frame " + frameNumber + " alignment failed: " + reason);
                    tvStatus.setText("Alignment failed: " + reason);
                });
            }

            @Override
            public void onStarDetectionFailed(int frameNumber, int starCount) {
                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    Log.w(TAG, "Frame " + frameNumber + " star detection failed: " + starCount);
                    tvStatus.setText("Too few stars detected: " + starCount);
                });
            }
        };
    }

    // =========================================================================
    // Finish: get result bitmap, switch to result mode, plate solve
    // =========================================================================

    private void finishStacking() {
        processAllImages();
    }

    // =========================================================================
    // Plate solving
    // =========================================================================

    private void solvePlate(Bitmap bitmap) {
        if (resultProgressBar == null || resultStatus == null || solver == null) return;
        if (backgroundExecutor.isShutdown()) return;

        resultProgressBar.setVisibility(View.VISIBLE);
        resultStatus.setVisibility(View.VISIBLE);
        resultStatus.setText("Detecting constellations...");

        backgroundExecutor.execute(() -> {
            try {
                solver.solve(bitmap, new NativePlateSolver.SolveCallback() {
                    @Override
                    public void onProgress(String message) {
                        runOnUiThread(() -> {
                            if (resultStatus != null) resultStatus.setText(message);
                        });
                    }

                    @Override
                    public void onSuccess(AstrometryNative.SolveResult result) {
                        try {
                            overlayBitmap = constellationOverlay.drawOverlay(originalBitmap, result);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to draw overlay", e);
                        }
                        runOnUiThread(() -> {
                            if (isDestroyed) return;
                            resultProgressBar.setVisibility(View.GONE);
                            resultStatus.setVisibility(View.GONE);
                            if (overlayBitmap != null) {
                                resultImageView.setImageBitmap(overlayBitmap);
                                cbConstellations.setVisibility(View.VISIBLE);
                                cbConstellations.setEnabled(true);
                                cbConstellations.setChecked(true);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            if (isDestroyed) return;
                            resultProgressBar.setVisibility(View.GONE);
                            resultStatus.setText("Could not identify star field");
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Plate solve crashed", e);
                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    resultProgressBar.setVisibility(View.GONE);
                    resultStatus.setText("Plate solve failed");
                });
            }
        });
    }

    // =========================================================================
    // Sky brightness analysis
    // =========================================================================

    private void analyzeSkyBrightness(Bitmap bitmap) {
        if (backgroundExecutor.isShutdown()) return;
        backgroundExecutor.execute(() -> {
            SkyBrightnessResult result = SkyBrightnessAnalyzer.analyze(bitmap, null);
            skyResult = result;
            runOnUiThread(() -> {
                if (isDestroyed) return;
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

    // =========================================================================
    // Helpers
    // =========================================================================

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // =========================================================================
    // Back button
    // =========================================================================

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (inResultMode) {
            showCaptureMode();
        } else {
            super.onBackPressed();
        }
    }

    // =========================================================================
    // Thumbnail adapter
    // =========================================================================

    private class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail;
            ImageButton btnRemove;
            TextView tvFrameNumber;

            ViewHolder(View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
                btnRemove = itemView.findViewById(R.id.btnRemove);
                tvFrameNumber = itemView.findViewById(R.id.tvFrameNumber);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stacking_thumbnail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Uri uri = collectedUris.get(position);
            holder.tvFrameNumber.setText(String.valueOf(position + 1));

            // Load thumbnail efficiently with proper stream management
            try {
                int scale = 1;
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(is, null, opts);
                        scale = Math.max(opts.outWidth, opts.outHeight) / 200;
                        if (scale < 1) scale = 1;
                    }
                }
                try (InputStream is2 = getContentResolver().openInputStream(uri)) {
                    if (is2 != null) {
                        BitmapFactory.Options opts2 = new BitmapFactory.Options();
                        opts2.inSampleSize = scale;
                        Bitmap thumb = BitmapFactory.decodeStream(is2, null, opts2);
                        holder.ivThumbnail.setImageBitmap(thumb);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load thumbnail", e);
                holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            holder.btnRemove.setOnClickListener(v -> {
                @SuppressWarnings("deprecation")
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    removeCollectedImage(pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return collectedUris.size();
        }
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;

        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
            try {
                if (!backgroundExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Background executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for executor shutdown", e);
                Thread.currentThread().interrupt();
            }
        }

        if (stackingManager != null) {
            stackingManager.release();
        }

        if (tooltipManager != null) {
            tooltipManager.dismiss();
        }
    }

    private void showCameraTipsIfNeeded() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_HAS_SEEN_CAMERA_TIPS, false)) {
            showDetectTooltipIfNeeded();
            return;
        }
        showCameraTipsDialog();
    }

    private void showCameraTipsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_plate_solve_tips, null);

        CheckBox cbDontShowAgain = dialogView.findViewById(R.id.cbDontShowAgain);
        com.google.android.material.button.MaterialButton btnGotIt = dialogView.findViewById(R.id.btnGotIt);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(
                this, R.style.Theme_AstroApp_AlertDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnGotIt.setOnClickListener(v -> {
            if (cbDontShowAgain.isChecked()) {
                android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_HAS_SEEN_CAMERA_TIPS, true).apply();
            }
            dialog.dismiss();
            showDetectTooltipIfNeeded();
        });

        dialog.setOnCancelListener(d -> showDetectTooltipIfNeeded());

        dialog.show();
    }

    private void showExampleImageDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_example_capture, null);

        com.google.android.material.button.MaterialButton btnGotIt = dialogView.findViewById(R.id.btnGotIt);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(
                this, R.style.Theme_AstroApp_AlertDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnGotIt.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDetectTooltipIfNeeded() {
        if (com.astro.app.ui.onboarding.TooltipManager.hasCompletedTutorial(
                this, com.astro.app.ui.onboarding.TooltipManager.KEY_DETECT_TUTORIAL)) {
            return;
        }

        findViewById(android.R.id.content).post(() -> {
            tooltipManager =
                new com.astro.app.ui.onboarding.TooltipManager(this,
                    com.astro.app.ui.onboarding.TooltipManager.KEY_DETECT_TUTORIAL);

            if (btnCapture != null) {
                tooltipManager.addTooltip(new com.astro.app.ui.onboarding.TooltipConfig(
                    btnCapture,
                    getString(R.string.tooltip_detect_capture),
                    com.astro.app.ui.onboarding.TooltipConfig.TooltipPosition.ABOVE,
                    true
                ));
            }

            if (switchStacking != null) {
                tooltipManager.addTooltip(new com.astro.app.ui.onboarding.TooltipConfig(
                    switchStacking,
                    getString(R.string.tooltip_detect_stacking),
                    com.astro.app.ui.onboarding.TooltipConfig.TooltipPosition.BELOW,
                    true
                ));
            }

            if (btnPickImages != null) {
                tooltipManager.addTooltip(new com.astro.app.ui.onboarding.TooltipConfig(
                    btnPickImages,
                    getString(R.string.tooltip_detect_gallery),
                    com.astro.app.ui.onboarding.TooltipConfig.TooltipPosition.ABOVE,
                    true
                ));
            }

            tooltipManager.start();
        });
    }
}
