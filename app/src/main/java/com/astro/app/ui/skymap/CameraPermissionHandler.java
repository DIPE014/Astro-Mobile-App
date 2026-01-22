package com.astro.app.ui.skymap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.astro.app.R;

/**
 * Handles camera permission requests for the AR sky map feature.
 *
 * <p>This class provides a streamlined way to check and request camera permissions,
 * including handling the rationale dialog when the user has previously denied
 * the permission.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // In Activity onCreate or fragment onViewCreated
 * CameraPermissionHandler permissionHandler = new CameraPermissionHandler(this);
 * permissionHandler.setCallback(new CameraPermissionHandler.PermissionCallback() {
 *     @Override
 *     public void onPermissionGranted() {
 *         startCamera();
 *     }
 *
 *     @Override
 *     public void onPermissionDenied(boolean shouldShowRationale) {
 *         if (shouldShowRationale) {
 *             // Show explanation to user
 *         } else {
 *             // Permission permanently denied, guide to settings
 *         }
 *     }
 * });
 *
 * // When you need to check/request permission
 * if (permissionHandler.hasCameraPermission()) {
 *     startCamera();
 * } else {
 *     permissionHandler.requestCameraPermission(permissionLauncher);
 * }
 * }</pre>
 *
 * <h3>Permission Flow:</h3>
 * <ol>
 *   <li>Check if permission is already granted using {@link #hasCameraPermission()}</li>
 *   <li>If not granted, check if rationale should be shown using {@link #shouldShowRationale()}</li>
 *   <li>Show rationale dialog if needed using {@link #showRationaleDialog(ActivityResultLauncher)}</li>
 *   <li>Request permission using {@link #requestCameraPermission(ActivityResultLauncher)}</li>
 *   <li>Handle result in the activity result callback</li>
 * </ol>
 */
public class CameraPermissionHandler {

    private static final String TAG = "CameraPermissionHandler";

    /** The camera permission string */
    public static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    private final Context context;
    private final Activity activity;

    @Nullable
    private PermissionCallback callback;

    /**
     * Callback interface for permission request results.
     */
    public interface PermissionCallback {
        /**
         * Called when camera permission has been granted.
         */
        void onPermissionGranted();

        /**
         * Called when camera permission has been denied.
         *
         * @param shouldShowRationale true if the user denied without checking
         *                            "Don't ask again", false if permission is
         *                            permanently denied
         */
        void onPermissionDenied(boolean shouldShowRationale);
    }

    /**
     * Creates a CameraPermissionHandler for the given activity.
     *
     * @param activity The activity context
     */
    public CameraPermissionHandler(@NonNull Activity activity) {
        this.activity = activity;
        this.context = activity;
    }

    /**
     * Sets the callback for permission results.
     *
     * @param callback The callback to receive permission results
     */
    public void setCallback(@Nullable PermissionCallback callback) {
        this.callback = callback;
    }

    /**
     * Checks if the camera permission is currently granted.
     *
     * @return true if CAMERA permission is granted
     */
    public boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(context, CAMERA_PERMISSION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if the app should show a rationale for requesting camera permission.
     *
     * <p>Returns true if the user has previously denied the permission but hasn't
     * checked "Don't ask again". In this case, you should explain why the
     * permission is needed before requesting it again.</p>
     *
     * @return true if rationale should be shown
     */
    public boolean shouldShowRationale() {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION);
    }

    /**
     * Requests camera permission using the provided launcher.
     *
     * <p>This method launches the system permission dialog. The result should be
     * handled by calling {@link #handlePermissionResult(boolean)} from your
     * ActivityResultCallback.</p>
     *
     * @param launcher The ActivityResultLauncher for permission requests
     */
    public void requestCameraPermission(@NonNull ActivityResultLauncher<String> launcher) {
        launcher.launch(CAMERA_PERMISSION);
    }

    /**
     * Shows a rationale dialog explaining why camera permission is needed,
     * then requests the permission if the user agrees.
     *
     * <p>Use this method when {@link #shouldShowRationale()} returns true.</p>
     *
     * @param launcher The ActivityResultLauncher for permission requests
     */
    public void showRationaleDialog(@NonNull ActivityResultLauncher<String> launcher) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_camera_title)
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(R.string.permission_grant, (dialog, which) -> {
                    requestCameraPermission(launcher);
                })
                .setNegativeButton(R.string.permission_deny, (dialog, which) -> {
                    dialog.dismiss();
                    if (callback != null) {
                        callback.onPermissionDenied(true);
                    }
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Shows a dialog directing the user to app settings when permission
     * has been permanently denied.
     *
     * <p>Use this when permission is denied and {@link #shouldShowRationale()}
     * returns false (meaning the user selected "Don't ask again").</p>
     */
    public void showSettingsDialog() {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_camera_title)
                .setMessage(R.string.permission_camera_settings)
                .setPositiveButton(R.string.permission_settings, (dialog, which) -> {
                    openAppSettings();
                })
                .setNegativeButton(R.string.permission_cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Handles the result of a permission request.
     *
     * <p>Call this method from your ActivityResultCallback to notify the handler
     * of the permission result.</p>
     *
     * @param isGranted true if permission was granted, false otherwise
     */
    public void handlePermissionResult(boolean isGranted) {
        if (callback == null) {
            return;
        }

        if (isGranted) {
            callback.onPermissionGranted();
        } else {
            // Check if we should show rationale (user didn't check "Don't ask again")
            boolean showRationale = shouldShowRationale();
            callback.onPermissionDenied(showRationale);
        }
    }

    /**
     * Checks permission and automatically handles the request flow.
     *
     * <p>This is a convenience method that combines checking, rationale display,
     * and permission request into a single call.</p>
     *
     * @param launcher The ActivityResultLauncher for permission requests
     */
    public void checkAndRequestPermission(@NonNull ActivityResultLauncher<String> launcher) {
        if (hasCameraPermission()) {
            if (callback != null) {
                callback.onPermissionGranted();
            }
        } else if (shouldShowRationale()) {
            showRationaleDialog(launcher);
        } else {
            requestCameraPermission(launcher);
        }
    }

    /**
     * Opens the app settings page where the user can manually grant permissions.
     */
    private void openAppSettings() {
        android.content.Intent intent = new android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        android.net.Uri uri = android.net.Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
}
