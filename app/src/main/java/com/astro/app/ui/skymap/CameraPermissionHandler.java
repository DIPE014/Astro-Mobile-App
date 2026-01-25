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
 * Invoked when the user grants the camera permission.
 */
        void onPermissionGranted();

        /**
 * Notifies that camera permission was denied.
 *
 * @param shouldShowRationale `true` if the app should show a rationale (the user denied without selecting "Don't ask again"), `false` if the permission is permanently denied and the user must enable it from settings
 */
        void onPermissionDenied(boolean shouldShowRationale);
    }

    /**
     * Initializes a CameraPermissionHandler bound to the provided Activity.
     *
     * @param activity the Activity used to display dialogs and start settings or permission-related intents
     */
    public CameraPermissionHandler(@NonNull Activity activity) {
        this.activity = activity;
        this.context = activity;
    }

    /**
     * Register or clear the callback that will receive camera permission results.
     *
     * @param callback the PermissionCallback to notify when permission is granted or denied; pass
     *                 `null` to clear any previously set callback
     */
    public void setCallback(@Nullable PermissionCallback callback) {
        this.callback = callback;
    }

    /**
     * Determine whether the app currently has camera permission.
     *
     * @return `true` if the camera permission is granted, `false` otherwise.
     */
    public boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(context, CAMERA_PERMISSION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Determines whether the app should display a rationale before requesting camera permission.
     *
     * @return true if a rationale should be shown, false otherwise.
     */
    public boolean shouldShowRationale() {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION);
    }

    /**
     * Initiates a camera permission request using the provided ActivityResultLauncher.
     *
     * <p>The permission result is delivered to the launcher's registered callback.</p>
     *
     * @param launcher the ActivityResultLauncher used to start the permission request
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
         * Shows a non-cancelable dialog that directs the user to the app's settings page
         * so they can manually grant the camera permission.
         *
         * <p>The dialog's positive action opens the application settings; the negative
         * action dismisses the dialog.</p>
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
     * Process the camera permission result and notify the configured callback.
     *
     * <p>If a callback has been set, invokes {@code onPermissionGranted()} when {@code isGranted}
     * is true; otherwise invokes {@code onPermissionDenied(boolean)} supplying whether a rationale
     * should be shown.</p>
     *
     * @param isGranted true if the camera permission was granted, false otherwise
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
     * Checks camera permission and proceeds with the appropriate request or callback flow.
     *
     * <p>If the camera permission is already granted, invokes {@link PermissionCallback#onPermissionGranted()}
     * when a callback is set. If a rationale should be shown, displays the rationale dialog; otherwise
     * launches the permission request using the provided launcher.</p>
     *
     * @param launcher the ActivityResultLauncher used to request the camera permission
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