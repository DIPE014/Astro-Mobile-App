package com.astro.app.ui.platesolve;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.astro.app.AstroApplication;
import com.astro.app.R;
import com.astro.app.ui.skymap.CameraManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

/**
 * Activity for capturing photos of the night sky for plate solving.
 *
 * <p>This activity provides a camera preview and capture button for taking
 * photos that will be analyzed by the Tetra3 plate solving algorithm to
 * identify stars and constellations.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Full-screen camera preview</li>
 *   <li>Large capture button for easy interaction in the dark</li>
 *   <li>Loading overlay during capture</li>
 *   <li>Automatic transition to result activity after capture</li>
 * </ul>
 *
 * <h3>Permissions:</h3>
 * <p>Requires CAMERA permission. If not granted, the user is prompted to grant it.</p>
 *
 * @see PlateSolveResultActivity
 * @see CameraManager
 */
public class PhotoCaptureActivity extends AppCompatActivity {

    private static final String TAG = "PhotoCaptureActivity";

    /** Extra key for the captured image path passed to result activity */
    public static final String EXTRA_IMAGE_PATH = "image_path";

    private static final int PERMISSION_REQUEST_CODE = 101;

    // Injected dependencies
    @Inject
    CameraManager cameraManager;

    // Views
    private PreviewView previewView;
    private FloatingActionButton fabCapture;
    private FrameLayout loadingOverlay;
    private TextView tvLoadingMessage;
    private TextView tvStatus;

    // State
    private boolean isCapturing = false;

    // Permission launcher
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, R.string.permission_camera_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on during capture
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_photo_capture);

        // Inject dependencies
        ((AstroApplication) getApplication()).getAppComponent().inject(this);

        initializeViews();
        setupClickListeners();

        // Check camera permission
        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Initializes view references.
     */
    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        fabCapture = findViewById(R.id.fabCapture);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);
        tvStatus = findViewById(R.id.tvStatus);
    }

    /**
     * Sets up click listeners for UI elements.
     */
    private void setupClickListeners() {
        // Back button
        MaterialButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Capture button
        fabCapture.setOnClickListener(v -> capturePhoto());
    }

    /**
     * Checks if camera permission is granted.
     *
     * @return true if permission is granted
     */
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests camera permission using the modern ActivityResult API.
     */
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // Show rationale before requesting
            Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show();
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    /**
     * Starts the camera preview.
     */
    private void startCamera() {
        cameraManager.setPreviewView(previewView);
        cameraManager.startCamera(this, new CameraManager.CameraCallback() {
            @Override
            public void onCameraStarted() {
                Log.d(TAG, "Camera started successfully");
                tvStatus.setText(R.string.plate_solve_status_ready);
            }

            @Override
            public void onCameraError(String message) {
                Log.e(TAG, "Camera error: " + message);
                Toast.makeText(PhotoCaptureActivity.this,
                        getString(R.string.camera_error, message),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /**
     * Captures a photo for plate solving.
     */
    private void capturePhoto() {
        if (isCapturing) {
            return;
        }

        isCapturing = true;
        showLoading(true);
        fabCapture.setEnabled(false);

        // Create output file
        File outputFile = createImageFile();
        if (outputFile == null) {
            showLoading(false);
            isCapturing = false;
            fabCapture.setEnabled(true);
            Toast.makeText(this, R.string.error_storage, Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Capturing image to: " + outputFile.getAbsolutePath());

        cameraManager.captureImage(outputFile, new CameraManager.ImageCaptureCallback() {
            @Override
            public void onImageCaptured(File imageFile) {
                Log.d(TAG, "Image captured: " + imageFile.getAbsolutePath());
                runOnUiThread(() -> {
                    // Navigate to result activity
                    Intent intent = new Intent(PhotoCaptureActivity.this,
                            PlateSolveResultActivity.class);
                    intent.putExtra(EXTRA_IMAGE_PATH, imageFile.getAbsolutePath());
                    startActivity(intent);

                    // Reset state (in case user comes back)
                    showLoading(false);
                    isCapturing = false;
                    fabCapture.setEnabled(true);
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Capture error: " + message);
                runOnUiThread(() -> {
                    showLoading(false);
                    isCapturing = false;
                    fabCapture.setEnabled(true);
                    Toast.makeText(PhotoCaptureActivity.this,
                            getString(R.string.camera_error, message),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Creates a unique file for storing the captured image.
     *
     * @return The file, or null if creation failed
     */
    private File createImageFile() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String filename = "STARS_" + timestamp + ".jpg";
            File storageDir = getExternalFilesDir("captures");
            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs();
            }
            return new File(storageDir, filename);
        } catch (Exception e) {
            Log.e(TAG, "Error creating image file", e);
            return null;
        }
    }

    /**
     * Shows or hides the loading overlay.
     *
     * @param show true to show loading, false to hide
     */
    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            tvLoadingMessage.setText(R.string.plate_solve_capturing);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasCameraPermission() && !cameraManager.isCameraRunning()) {
            startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraManager.stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraManager.release();
    }
}
