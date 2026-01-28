package com.astro.app.ui.skymap;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

/**
 * Manages CameraX camera operations for the AR sky map feature.
 *
 * <p>This class handles all camera-related operations including:
 * <ul>
 *   <li>CameraX initialization and configuration</li>
 *   <li>Binding camera to lifecycle</li>
 *   <li>Setting up preview use case</li>
 *   <li>Starting and stopping camera preview</li>
 * </ul>
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * CameraManager cameraManager = new CameraManager(context);
 * cameraManager.setPreviewView(previewView);
 * cameraManager.startCamera(lifecycleOwner, new CameraManager.CameraCallback() {
 *     @Override
 *     public void onCameraStarted() {
 *         // Camera is ready
 *     }
 *
 *     @Override
 *     public void onCameraError(String message) {
 *         // Handle error
 *     }
 * });
 * }</pre>
 *
 * <h3>Thread Safety:</h3>
 * <p>This class is designed to be called from the main thread. Camera operations
 * are performed asynchronously with callbacks delivered on the main thread.</p>
 */
public class CameraManager {

    private static final String TAG = "CameraManager";

    private final Context context;

    @Nullable
    private ProcessCameraProvider cameraProvider;

    @Nullable
    private Camera camera;

    @Nullable
    private Preview preview;

    @Nullable
    private PreviewView previewView;

    @Nullable
    private LifecycleOwner lifecycleOwner;

    private boolean isCameraRunning = false;

    /**
     * Callback interface for camera events.
     */
    public interface CameraCallback {
        /**
         * Called when the camera has started successfully.
         */
        void onCameraStarted();

        /**
         * Called when an error occurs during camera operation.
         *
         * @param message Error description
         */
        void onCameraError(String message);
    }

    /**
     * Creates a new CameraManager with the given context.
     *
     * @param context Application or Activity context
     */
    @Inject
    public CameraManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Sets the PreviewView where the camera preview will be displayed.
     *
     * @param previewView The PreviewView to use for camera preview
     */
    public void setPreviewView(@NonNull PreviewView previewView) {
        this.previewView = previewView;
    }

    /**
     * Starts the camera and binds it to the lifecycle.
     *
     * <p>This method initializes CameraX, creates a preview use case, and binds
     * the camera to the provided lifecycle owner. The callback is invoked when
     * the camera is ready or if an error occurs.</p>
     *
     * @param lifecycleOwner The lifecycle owner to bind the camera to
     * @param callback       Callback for camera events (can be null)
     */
    public void startCamera(@NonNull LifecycleOwner lifecycleOwner,
                           @Nullable CameraCallback callback) {
        this.lifecycleOwner = lifecycleOwner;

        if (previewView == null) {
            Log.e(TAG, "PreviewView not set. Call setPreviewView() first.");
            if (callback != null) {
                callback.onCameraError("PreviewView not set");
            }
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(callback);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error getting camera provider", e);
                if (callback != null) {
                    callback.onCameraError("Failed to initialize camera: " + e.getMessage());
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    /**
     * Binds camera use cases to the lifecycle.
     *
     * @param callback Callback for camera events
     */
    private void bindCameraUseCases(@Nullable CameraCallback callback) {
        if (cameraProvider == null || lifecycleOwner == null || previewView == null) {
            Log.e(TAG, "Cannot bind camera use cases: missing dependencies");
            if (callback != null) {
                callback.onCameraError("Camera not properly initialized");
            }
            return;
        }

        try {
            // Unbind any existing use cases before rebinding
            cameraProvider.unbindAll();

            // Build the preview use case
            preview = new Preview.Builder()
                    .build();

            // Set up the preview surface provider
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            // Select back camera
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
            );

            isCameraRunning = true;
            Log.d(TAG, "Camera started successfully");

            if (callback != null) {
                callback.onCameraStarted();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error binding camera use cases", e);
            isCameraRunning = false;
            if (callback != null) {
                callback.onCameraError("Failed to bind camera: " + e.getMessage());
            }
        }
    }

    /**
     * Stops the camera preview and releases resources.
     *
     * <p>This method unbinds all camera use cases. The camera can be
     * started again by calling {@link #startCamera(LifecycleOwner, CameraCallback)}.</p>
     */
    public void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            Log.d(TAG, "Camera stopped");
        }

        isCameraRunning = false;
        camera = null;
        preview = null;
    }

    /**
     * Checks if the camera is currently running.
     *
     * @return true if the camera is active and showing preview
     */
    public boolean isCameraRunning() {
        return isCameraRunning;
    }

    /**
     * Returns the current Camera instance.
     *
     * @return The Camera instance, or null if camera is not running
     */
    @Nullable
    public Camera getCamera() {
        return camera;
    }

    /**
     * Returns the horizontal field of view of the camera in degrees.
     *
     * <p>This is an estimated value based on typical smartphone cameras.
     * For more accurate values, use CameraCharacteristics from Camera2 API.</p>
     *
     * @return Estimated horizontal FOV in degrees
     */
    public float getHorizontalFov() {
        // Typical smartphone camera horizontal FOV
        // This could be made more accurate using Camera2 API's CameraCharacteristics
        return 66.0f;
    }

    /**
     * Returns the vertical field of view of the camera in degrees.
     *
     * <p>This is an estimated value based on typical smartphone cameras.</p>
     *
     * @return Estimated vertical FOV in degrees
     */
    public float getVerticalFov() {
        // Typical smartphone camera vertical FOV (4:3 aspect ratio)
        return 50.0f;
    }

    /**
     * Enables or disables the camera torch (flashlight).
     *
     * @param enabled true to turn on the torch, false to turn it off
     */
    public void setTorchEnabled(boolean enabled) {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(enabled);
        }
    }

    /**
     * Releases all camera resources.
     *
     * <p>Call this when the camera is no longer needed.</p>
     */
    public void release() {
        stopCamera();
        cameraProvider = null;
        lifecycleOwner = null;
        previewView = null;
    }
}
