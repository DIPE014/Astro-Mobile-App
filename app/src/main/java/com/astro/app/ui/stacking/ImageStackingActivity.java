package com.astro.app.ui.stacking;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.astro.app.R;
import com.astro.app.native_.ImageStackingManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for image stacking feature.
 * Captures multiple frames from camera, aligns them using triangle asterism matching,
 * and averages pixel values to improve SNR and reveal fainter stars.
 */
public class ImageStackingActivity extends AppCompatActivity {
    private static final String TAG = "ImageStackingActivity";

    // Views
    private PreviewView cameraPreview;
    private MaterialButton btnCapture;
    private MaterialButton btnFinish;
    private MaterialButton btnBack;
    private TextView tvFrameCount;
    private TextView tvStatus;
    private ImageView ivStackedPreview;
    private CircularProgressIndicator progressIndicator;
    private View loadingOverlay;

    // CameraX
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    // Stacking
    private ImageStackingManager stackingManager;
    private boolean sessionStarted = false;
    private int targetFrames = 10;  // Default target

    // Permission
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required for stacking",
                    Toast.LENGTH_LONG).show();
                finish();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_stacking);

        // Keep screen on during stacking
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialize views
        initializeViews();

        // Initialize managers
        stackingManager = new ImageStackingManager();
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Setup listeners
        setupClickListeners();

        // Check permission and start camera
        checkCameraPermission();
    }

    private void initializeViews() {
        cameraPreview = findViewById(R.id.cameraPreview);
        btnCapture = findViewById(R.id.btnCapture);
        btnFinish = findViewById(R.id.btnFinish);
        btnBack = findViewById(R.id.btnBack);
        tvFrameCount = findViewById(R.id.tvFrameCount);
        tvStatus = findViewById(R.id.tvStatus);
        ivStackedPreview = findViewById(R.id.ivStackedPreview);
        progressIndicator = findViewById(R.id.progressIndicator);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Initial state
        btnCapture.setEnabled(false);
        btnFinish.setEnabled(false);
        tvFrameCount.setText("0 / " + targetFrames + " frames");
        tvStatus.setText("Initializing camera...");
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this, "Failed to initialize camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        // ImageCapture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Bind to lifecycle
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            // Enable capture button
            btnCapture.setEnabled(true);
            tvStatus.setText("Tap Capture to start stacking");
        } catch (Exception e) {
            Log.e(TAG, "Camera binding failed", e);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCapture.setOnClickListener(v -> captureFrame());

        btnFinish.setOnClickListener(v -> finishStacking());
    }

    private void captureFrame() {
        if (imageCapture == null) return;

        // Show processing
        showLoading(true);
        btnCapture.setEnabled(false);

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(ImageProxy imageProxy) {
                Bitmap bitmap = imageProxyToBitmap(imageProxy);
                imageProxy.close();

                if (bitmap != null) {
                    processFrame(bitmap);
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        btnCapture.setEnabled(true);
                        Toast.makeText(ImageStackingActivity.this,
                            "Failed to process image", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(ImageCaptureException exception) {
                Log.e(TAG, "Capture failed", exception);
                runOnUiThread(() -> {
                    showLoading(false);
                    btnCapture.setEnabled(true);
                    Toast.makeText(ImageStackingActivity.this,
                        "Capture failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void processFrame(Bitmap bitmap) {
        // Process on background thread
        cameraExecutor.execute(() -> {
            boolean success;

            if (!sessionStarted) {
                // First frame - start session
                success = stackingManager.startSession(bitmap, createCallback());
                sessionStarted = success;
            } else {
                // Subsequent frames
                success = stackingManager.addFrame(bitmap);
            }

            final boolean finalSuccess = success;
            runOnUiThread(() -> {
                showLoading(false);

                if (finalSuccess) {
                    updateUI();
                    btnCapture.setEnabled(stackingManager.getFrameCount() < targetFrames);
                    btnFinish.setEnabled(true);
                } else {
                    btnCapture.setEnabled(true);
                    Toast.makeText(ImageStackingActivity.this,
                        "Failed to stack frame", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private ImageStackingManager.StackingCallback createCallback() {
        return new ImageStackingManager.StackingCallback() {
            @Override
            public void onFrameStacked(int frameNumber, int totalFrames, int inliers, double rmsError) {
                runOnUiThread(() -> {
                    Log.i(TAG, String.format("Frame %d stacked: %d inliers, RMS=%.2f px",
                        frameNumber, inliers, rmsError));
                    tvStatus.setText(String.format("Aligned: %d stars, RMS: %.1f px",
                        inliers, rmsError));
                });
            }

            @Override
            public void onAlignmentFailed(int frameNumber, String reason) {
                runOnUiThread(() -> {
                    Log.w(TAG, "Frame " + frameNumber + " alignment failed: " + reason);
                    tvStatus.setText("Alignment failed: " + reason);
                });
            }

            @Override
            public void onStarDetectionFailed(int frameNumber, int starCount) {
                runOnUiThread(() -> {
                    Log.w(TAG, "Frame " + frameNumber + " star detection failed: " + starCount);
                    tvStatus.setText("Too few stars detected: " + starCount);
                });
            }
        };
    }

    private void updateUI() {
        int frameCount = stackingManager.getFrameCount();
        tvFrameCount.setText(frameCount + " / " + targetFrames + " frames");

        // Update preview thumbnail
        if (frameCount > 0) {
            Bitmap stacked = stackingManager.getResult();
            if (stacked != null) {
                ivStackedPreview.setImageBitmap(stacked);
                ivStackedPreview.setVisibility(View.VISIBLE);
            }
        }
    }

    private void finishStacking() {
        // Get final result
        Bitmap result = stackingManager.getResult();

        if (result != null) {
            // TODO: Save to gallery or pass back to caller
            Toast.makeText(this, "Stacking complete: " +
                stackingManager.getFrameCount() + " frames", Toast.LENGTH_LONG).show();

            // For now, just finish
            finish();
        } else {
            Toast.makeText(this, "No stacked result available", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (stackingManager != null) {
            stackingManager.release();
        }
    }
}
