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
 * Notifies that a camera error occurred.
 *
 * @param message a human-readable description of the error
 */
        void onCameraError(String message);
    }

    /**
     * Construct a CameraManager and retain the application's Context for CameraX interactions.
     *
     * @param context an Application or Activity Context; the constructor stores its application context for internal use
     */
    @Inject
    public CameraManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Specify the PreviewView that will host the camera preview surface.
     *
     * @param previewView the PreviewView used as the preview display and surface provider
     */
    public void setPreviewView(@NonNull PreviewView previewView) {
        this.previewView = previewView;
    }

    /**
     * Start and bind the camera preview to the provided lifecycle owner.
     *
     * Initializes CameraX and binds a Preview use case to the given lifecycle; invokes
     * the provided callback when the camera has started or when an error occurs.
     *
     * @param lifecycleOwner the LifecycleOwner to bind camera use cases to
     * @param callback       an optional CameraCallback invoked on success or error; may be null
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
     * Bind the camera preview use case to the stored lifecycle owner and start the camera.
     *
     * Attempts to unbind any existing use cases, create and attach a Preview (using the stored
     * PreviewView and back-facing camera), and mark the camera as running.
     *
     * @param callback optional callback invoked with lifecycle events: `onCameraStarted()` when the
     *                 camera has been bound successfully, or `onCameraError(String)` if binding
     *                 fails or required dependencies are missing
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
     * Stops the camera preview and releases camera-related resources.
     *
     * <p>Unbinds all camera use cases from the internal ProcessCameraProvider and clears internal references.</p>
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
     * Indicates whether the camera preview is currently running.
     *
     * @return `true` if the camera is active and showing the preview, `false` otherwise.
     */
    public boolean isCameraRunning() {
        return isCameraRunning;
    }

    /**
     * Provides access to the currently bound Camera instance.
     *
     * @return the current Camera, or null if no camera is bound
     */
    @Nullable
    public Camera getCamera() {
        return camera;
    }

    /**
     * Provides an estimated horizontal field of view in degrees.
     *
     * @return Estimated horizontal field of view in degrees.
     */
    public float getHorizontalFov() {
        // Typical smartphone camera horizontal FOV
        // This could be made more accurate using Camera2 API's CameraCharacteristics
        return 66.0f;
    }

    /**
     * Gets the camera's vertical field of view in degrees.
     *
     * <p>This value is an approximation based on typical smartphone cameras.</p>
     *
     * @return Estimated vertical field of view in degrees.
     */
    public float getVerticalFov() {
        // Typical smartphone camera vertical FOV (4:3 aspect ratio)
        return 50.0f;
    }

    /**
     * Toggle the camera's torch (flashlight) when a camera with a flash unit is available.
     *
     * @param enabled `true` to enable the torch, `false` to disable it
     */
    public void setTorchEnabled(boolean enabled) {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(enabled);
        }
    }

    /**
     * Release camera resources and clear internal references.
     *
     * Stops the camera if running and clears the cached ProcessCameraProvider, lifecycle owner, and preview view references.
     */
    public void release() {
        stopCamera();
        cameraProvider = null;
        lifecycleOwner = null;
        previewView = null;
    }
}