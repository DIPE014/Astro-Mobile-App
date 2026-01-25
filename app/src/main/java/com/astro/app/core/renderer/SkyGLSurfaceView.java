package com.astro.app.core.renderer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.core.math.Vector3;

/**
 * Custom GLSurfaceView for the sky map with built-in gesture handling.
 *
 * <p>This view extends GLSurfaceView and adds support for common gestures
 * needed in a sky map application:</p>
 * <ul>
 *   <li><b>Drag</b> - Pan the view around the celestial sphere</li>
 *   <li><b>Pinch zoom</b> - Adjust the field of view</li>
 *   <li><b>Double tap</b> - Reset to default view</li>
 *   <li><b>Single tap</b> - Select object (optional callback)</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * SkyRenderer renderer = new SkyRenderer();
 * SkyGLSurfaceView surfaceView = new SkyGLSurfaceView(context, renderer);
 * surfaceView.setGestureListener(new GestureListener() {
 *     @Override
 *     public void onTap(float x, float y) {
 *         // Handle tap to select object
 *     }
 * });
 * }</pre>
 *
 * <h3>Coordinate System:</h3>
 * <p>Screen coordinates (pixels) are mapped to movements on the celestial sphere.
 * Horizontal drag rotates around the polar axis, vertical drag changes
 * elevation.</p>
 */
public class SkyGLSurfaceView extends GLSurfaceView {

    private static final String TAG = "SkyGLSurfaceView";

    /** The sky renderer */
    private final SkyRenderer renderer;

    /** Gesture detectors */
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    /** Gesture listener callback */
    @Nullable
    private GestureListener gestureListener;

    // View orientation state
    private float azimuth = 0.0f;    // Horizontal angle in radians
    private float elevation = 0.0f;  // Vertical angle in radians (-PI/2 to PI/2)
    private float roll = 0.0f;       // Roll angle in radians

    // Touch sensitivity
    private float panSensitivity = 0.005f;
    private float zoomSensitivity = 1.0f;

    // FOV limits
    private float minFov = 20.0f;
    private float maxFov = 100.0f;

    // State tracking
    private boolean isPinching = false;

    /**
     * Callback interface for gesture events.
     */
    public interface GestureListener {
        /**
 * Invoked when the user performs a single tap on the view.
 *
 * @param x horizontal tap coordinate in view pixels, measured from the left edge
 * @param y vertical tap coordinate in view pixels, measured from the top edge
 */
        void onTap(float x, float y);

        /**
         * Called when the user double-taps on the view.
         *
         * @param x Screen X coordinate
         * @param y Screen Y coordinate
         */
        void onDoubleTap(float x, float y);

        /**
 * Invoked when the user long-presses the view at the given screen coordinates.
 *
 * @param x the X coordinate of the long-press in pixels
 * @param y the Y coordinate of the long-press in pixels
 */
        void onLongPress(float x, float y);

        /**
 * Invoked when the view orientation changes.
 *
 * @param azimuth   azimuth (horizontal) angle in radians, normalized to [0, 2*pi)
 * @param elevation elevation (vertical) angle in radians, clamped to [-pi/2, pi/2]
 */
        void onOrientationChanged(float azimuth, float elevation);

        /**
 * Notifies that the field of view has changed.
 *
 * @param fov the new field of view, in degrees
 */
        void onFovChanged(float fov);
    }

    /**
     * Simple implementation of GestureListener with empty methods.
     * Extend this class to override only the methods you need.
     */
    public static class SimpleGestureListener implements GestureListener {
        /**
         * Called when the user performs a single tap on the view.
         *
         * Default implementation does nothing; override to handle tap events.
         *
         * @param x horizontal coordinate of the tap in view pixels
         * @param y vertical coordinate of the tap in view pixels
         */
        @Override
        public void onTap(float x, float y) {}

        /**
         * Called when the user performs a double tap at the given view coordinates.
         *
         * @param x the x coordinate of the tap in view pixels
         * @param y the y coordinate of the tap in view pixels
         */
        @Override
        public void onDoubleTap(float x, float y) {}

        /**
         * Invoked when a long-press gesture is detected at the given coordinates.
         *
         * @param x the x coordinate of the press in view pixels
         * @param y the y coordinate of the press in view pixels
         */
        @Override
        public void onLongPress(float x, float y) {}

        /**
         * Called when the view orientation changes.
         *
         * @param azimuth   new azimuth in radians, normalized to [0, 2π)
         * @param elevation new elevation in radians, clamped to [-π/2, π/2]
         */
        @Override
        public void onOrientationChanged(float azimuth, float elevation) {}

        /**
         * Called when the view's field of view changes.
         *
         * Default no-op implementation; override to respond to FOV updates.
         *
         * @param fov the new field of view in degrees
         */
        @Override
        public void onFovChanged(float fov) {}
    }

    /**
     * Create a SkyGLSurfaceView backed by the provided SkyRenderer.
     *
     * @param context  Android context used to configure the view
     * @param renderer SkyRenderer instance that will drive rendering for this view
     */
    public SkyGLSurfaceView(@NonNull Context context, @NonNull SkyRenderer renderer) {
        super(context);
        this.renderer = renderer;
        init(context);
    }

    /**
     * Constructs a SkyGLSurfaceView when inflated from XML.
     *
     * <p>Initializes the view with a default SkyRenderer and performs OpenGL and gesture setup.</p>
     *
     * @param context the Android Context used to access resources and system services
     * @param attrs   the XML attributes supplied to the view (may be null)
     */
    public SkyGLSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.renderer = new SkyRenderer();
        init(context);
    }

    /**
     * Initialize the GL surface, attach the renderer, and set up gesture handling and the initial view orientation.
     *
     * Configures OpenGL ES 2.0 and an EGL config, sets the SkyRenderer and continuous render mode, creates the
     * GestureDetector and ScaleGestureDetector, and applies the current orientation to the renderer.
     *
     * @param context Android Context used to construct gesture detectors and access resources
     */
    private void init(Context context) {
        android.util.Log.d(TAG, "init() called - starting GLSurfaceView initialization");

        // Set OpenGL ES 2.0 context - MUST be called BEFORE setRenderer()
        setEGLContextClientVersion(2);
        android.util.Log.d(TAG, "setEGLContextClientVersion(2) called");

        // Configure EGL - use simple config for maximum compatibility
        // IMPORTANT: setEGLConfigChooser must be called BEFORE setRenderer
        // Using default config (no alpha) for better compatibility
        // AR transparency will be handled via setZOrderOnTop() instead
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);  // RGB888 with 16-bit depth, no alpha, no stencil
        android.util.Log.d(TAG, "setEGLConfigChooser called");

        // Set the renderer - MUST be called AFTER setEGLContextClientVersion and setEGLConfigChooser
        setRenderer(renderer);
        android.util.Log.d(TAG, "setRenderer() called with renderer: " + renderer);

        // Use continuous rendering to ensure onDrawFrame is called repeatedly
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        android.util.Log.d(TAG, "setRenderMode(RENDERMODE_CONTINUOUSLY) called");

        // Initialize gesture detectors
        gestureDetector = new GestureDetector(context, new GestureHandler());
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleHandler());

        // Update initial orientation
        updateViewOrientation();

        android.util.Log.d(TAG, "init() complete - GLSurfaceView should now render");
    }

    /**
     * Dispatches the touch event to the scale and gesture detectors and reports if it was consumed.
     *
     * @param event the MotionEvent to handle
     * @return `true` if the event was handled by the scale detector, gesture detector, or the superclass; `false` otherwise
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Let scale gesture detector handle pinch gestures first
        boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);

        // Let gesture detector handle other gestures
        boolean gestureHandled = gestureDetector.onTouchEvent(event);

        return scaleHandled || gestureHandled || super.onTouchEvent(event);
    }

    /**
         * Update the renderer's view orientation from the current azimuth, elevation, and roll.
         *
         * Computes forward (look) and up vectors from the spherical coordinates and, if a roll
         * is present, rotates the up vector around the look direction; the final vectors are
         * posted to the GL thread and applied to the renderer.
         */
    private void updateViewOrientation() {
        // Calculate look direction from spherical coordinates
        float cosElev = (float) Math.cos(elevation);
        float sinElev = (float) Math.sin(elevation);
        float cosAz = (float) Math.cos(azimuth);
        float sinAz = (float) Math.sin(azimuth);

        // Look direction
        float lookX = cosElev * cosAz;
        float lookY = cosElev * sinAz;
        float lookZ = sinElev;

        // Up direction (always toward zenith, adjusted for roll)
        float upX, upY, upZ;
        if (Math.abs(elevation) > Math.PI / 2 - 0.01f) {
            // Near poles, use a different up vector
            upX = -sinElev * cosAz;
            upY = -sinElev * sinAz;
            upZ = cosElev;
        } else {
            upX = -sinElev * cosAz;
            upY = -sinElev * sinAz;
            upZ = cosElev;
        }

        // Apply roll rotation around look direction
        if (Math.abs(roll) > 0.001f) {
            float cosRoll = (float) Math.cos(roll);
            float sinRoll = (float) Math.sin(roll);

            // Right vector = look cross up
            float rightX = lookY * upZ - lookZ * upY;
            float rightY = lookZ * upX - lookX * upZ;
            float rightZ = lookX * upY - lookY * upX;

            // Rotate up around look direction
            upX = upX * cosRoll + rightX * sinRoll;
            upY = upY * cosRoll + rightY * sinRoll;
            upZ = upZ * cosRoll + rightZ * sinRoll;
        }

        // Update renderer - use final variables for lambda
        final float finalUpX = upX;
        final float finalUpY = upY;
        final float finalUpZ = upZ;
        queueEvent(() -> renderer.setViewOrientation(lookX, lookY, lookZ, finalUpX, finalUpY, finalUpZ));
    }

    /**
     * Gesture handler for pan, tap, and long press.
     */
    private class GestureHandler extends GestureDetector.SimpleOnGestureListener {

        /**
         * Signals that the gesture detector should continue receiving subsequent gesture events after a down event.
         *
         * @param e the MotionEvent representing the initial down event
         * @return `true` to receive subsequent gesture events, `false` otherwise
         */
        @Override
        public boolean onDown(MotionEvent e) {
            return true; // Must return true to receive subsequent events
        }

        /**
         * Handles drag gestures to pan the sky view by updating azimuth and elevation.
         *
         * Updates the stored orientation, clamps elevation to valid bounds, normalizes azimuth into
         * [0, 2*PI), applies the new orientation to the renderer, and notifies the gesture listener.
         * If a pinch gesture is active, the scroll is ignored.
         *
         * @param e1        the initial down MotionEvent that started the gesture
         * @param e2        the current MotionEvent for the scroll
         * @param distanceX horizontal distance in pixels since the last call
         * @param distanceY vertical distance in pixels since the last call
         * @return          `true` if the scroll was handled and orientation updated, `false` if ignored
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (isPinching) {
                return false; // Don't pan while pinching
            }

            // Update orientation based on drag
            float fovFactor = renderer.getFieldOfView() / 60.0f;
            azimuth -= distanceX * panSensitivity * fovFactor;
            elevation += distanceY * panSensitivity * fovFactor;

            // Clamp elevation to valid range
            elevation = Math.max(-((float) Math.PI / 2 - 0.01f),
                    Math.min((float) Math.PI / 2 - 0.01f, elevation));

            // Normalize azimuth to [0, 2*PI)
            while (azimuth < 0) azimuth += 2 * Math.PI;
            while (azimuth >= 2 * Math.PI) azimuth -= 2 * Math.PI;

            updateViewOrientation();

            if (gestureListener != null) {
                gestureListener.onOrientationChanged(azimuth, elevation);
            }

            return true;
        }

        /**
         * Handle a confirmed single-tap gesture and notify the registered GestureListener.
         *
         * @param e the MotionEvent for the confirmed single tap (provides tap coordinates)
         * @return true if the event was handled
         */
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (gestureListener != null) {
                gestureListener.onTap(e.getX(), e.getY());
            }
            return true;
        }

        /**
         * Handle a double-tap gesture by forwarding the tap coordinates to the registered GestureListener.
         *
         * @param e the MotionEvent for the double-tap; its X and Y coordinates are passed to the listener
         * @return `true` if the double-tap was handled, `false` otherwise
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (gestureListener != null) {
                gestureListener.onDoubleTap(e.getX(), e.getY());
            }
            return true;
        }

        /**
         * Handles a long-press gesture and notifies the configured GestureListener with the press coordinates.
         *
         * @param e the MotionEvent for the long-press; the event's X and Y are passed to {@code gestureListener.onLongPress(float, float)} if a listener is set
         */
        @Override
        public void onLongPress(MotionEvent e) {
            if (gestureListener != null) {
                gestureListener.onLongPress(e.getX(), e.getY());
            }
        }

        /**
         * Placeholder for handling a fling (swipe) gesture; intended to be overridden to implement momentum scrolling.
         *
         * @param e1 the first down motion event that started the fling
         * @param e2 the move motion event that triggered the current onFling
         * @param velocityX horizontal velocity of the fling in pixels per second
         * @param velocityY vertical velocity of the fling in pixels per second
         * @return `true` if the fling was handled, `false` otherwise
         */
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Could implement momentum scrolling here
            return false;
        }
    }

    /**
     * Scale handler for pinch zoom.
     */
    private class ScaleHandler extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        /**
         * Marks the start of a pinch gesture so the view enters a pinching state and will handle scale updates.
         *
         * @param detector the ScaleGestureDetector reporting the begin event
         * @return `true` to accept the scale gesture and receive subsequent scale events, `false` otherwise
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isPinching = true;
            return true;
        }

        /**
         * Adjusts the renderer's field of view based on the current pinch scale gesture.
         *
         * The method computes a new FOV from the detector's scale factor (pinch-in zooms in), clamps it to the configured limits,
         * queues the update to the renderer, and notifies the gesture listener of the change.
         *
         * @param detector the active ScaleGestureDetector providing the current scale factor
         * @return `true` if the scale gesture was handled, `false` otherwise
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();

            // Invert scale factor: pinch in = zoom in = smaller FOV
            float fov = renderer.getFieldOfView();
            fov /= (float) Math.pow(scaleFactor, zoomSensitivity);

            // Clamp FOV to valid range
            fov = Math.max(minFov, Math.min(maxFov, fov));

            final float finalFov = fov;
            queueEvent(() -> renderer.setFieldOfView(finalFov));

            if (gestureListener != null) {
                gestureListener.onFovChanged(fov);
            }

            return true;
        }

        /**
         * Marks the end of the pinch gesture by clearing the internal pinching flag.
         *
         * @param detector the ScaleGestureDetector that triggered the end of the gesture
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isPinching = false;
        }
    }

    // ========== Public API ==========

    /**
         * Get the SkyRenderer used by this view.
         *
         * @return the SkyRenderer instance used by this view (never null)
         */
    @NonNull
    public SkyRenderer getSkyRenderer() {
        return renderer;
    }

    /**
     * Register or remove the listener that receives gesture callbacks from this view.
     *
     * @param listener the GestureListener to register, or `null` to remove any existing listener
     */
    public void setGestureListener(@Nullable GestureListener listener) {
        this.gestureListener = listener;
    }

    /**
     * Set the view orientation to the specified azimuth and elevation.
     *
     * @param azimuth   Horizontal angle in radians.
     * @param elevation Vertical angle in radians; values are clamped to the range
     *                  [-π/2 + 0.01, π/2 - 0.01].
     */
    public void setOrientation(float azimuth, float elevation) {
        this.azimuth = azimuth;
        this.elevation = Math.max(-((float) Math.PI / 2 - 0.01f),
                Math.min((float) Math.PI / 2 - 0.01f, elevation));
        updateViewOrientation();
    }

    /**
     * Update the view orientation including an explicit roll.
     *
     * Elevation is constrained to be between -π/2 + 0.01 and π/2 - 0.01 radians. This method
     * updates the internal orientation state and applies the change to the renderer.
     *
     * @param azimuth   horizontal angle in radians
     * @param elevation vertical angle in radians; will be clamped to the valid range
     * @param roll      rotation around the viewing (look) direction in radians
     */
    public void setOrientation(float azimuth, float elevation, float roll) {
        this.azimuth = azimuth;
        this.elevation = Math.max(-((float) Math.PI / 2 - 0.01f),
                Math.min((float) Math.PI / 2 - 0.01f, elevation));
        this.roll = roll;
        updateViewOrientation();
    }

    /**
     * Current azimuth angle of the view.
     *
     * @return Azimuth angle in radians.
     */
    public float getAzimuth() {
        return azimuth;
    }

    /**
     * Retrieves the current elevation angle.
     *
     * @return the elevation angle in radians
     */
    public float getElevation() {
        return elevation;
    }

    /**
     * Gets the view's roll angle.
     *
     * @return the current roll angle in radians
     */
    public float getRoll() {
        return roll;
    }

    /**
     * Updates the renderer's field of view (FOV) in degrees.
     *
     * The provided value is clamped to the current FOV limits before being applied.
     *
     * @param fovDegrees desired FOV in degrees; values outside the configured limits are clamped
     */
    public void setFieldOfView(float fovDegrees) {
        final float fov = Math.max(minFov, Math.min(maxFov, fovDegrees));
        queueEvent(() -> renderer.setFieldOfView(fov));
    }

    /**
     * Gets the current field of view.
     *
     * @return the field of view, in degrees
     */
    public float getFieldOfView() {
        return renderer.getFieldOfView();
    }

    /**
     * Set the allowed field-of-view range used for zoom, in degrees.
     *
     * @param minFov minimum field of view, in degrees (inclusive)
     * @param maxFov maximum field of view, in degrees (inclusive)
     */
    public void setFovLimits(float minFov, float maxFov) {
        this.minFov = minFov;
        this.maxFov = maxFov;
    }

    /**
     * Adjusts drag responsiveness for changing the view orientation.
     *
     * @param sensitivity sensitivity factor where larger values make panning respond more strongly; default 0.005
     */
    public void setPanSensitivity(float sensitivity) {
        this.panSensitivity = sensitivity;
    }

    /**
     * Adjusts how quickly pinch gestures change the field of view.
     *
     * Larger values increase zoom speed; values between 0 and 1 decrease it. Default is 1.0.
     *
     * @param sensitivity multiplicative factor applied to pinch-zoom gestures
     */
    public void setZoomSensitivity(float sensitivity) {
        this.zoomSensitivity = sensitivity;
    }

    /**
     * Point the view toward the specified celestial coordinates.
     *
     * @param ra  Right Ascension in degrees; treated as azimuth and converted to radians.
     * @param dec Declination in degrees; treated as elevation and converted to radians.
     */
    public void lookAt(float ra, float dec) {
        // Convert RA/Dec to azimuth/elevation
        // Note: This is a simplified conversion that works for celestial coordinates
        this.azimuth = (float) Math.toRadians(ra);
        this.elevation = (float) Math.toRadians(dec);
        updateViewOrientation();

        if (gestureListener != null) {
            gestureListener.onOrientationChanged(azimuth, elevation);
        }
    }

    /**
     * Reorients the view to look along the given 3D direction vector.
     *
     * The vector is normalized before use; if its length is effectively zero the view remains unchanged.
     * Updates the internal azimuth (normalized to [0, 2π)) and elevation (radians) and applies the new view.
     * If a GestureListener is set, its onOrientationChanged callback is invoked with the new azimuth and elevation.
     *
     * @param direction the direction vector in world coordinates; will be normalized before use
     */
    public void lookAt(@NonNull Vector3 direction) {
        // Convert Cartesian to spherical coordinates
        float length = direction.getLength();
        if (length < 0.0001f) return;

        float x = direction.x / length;
        float y = direction.y / length;
        float z = direction.z / length;

        this.elevation = (float) Math.asin(z);
        this.azimuth = (float) Math.atan2(y, x);

        if (this.azimuth < 0) {
            this.azimuth += 2 * Math.PI;
        }

        updateViewOrientation();

        if (gestureListener != null) {
            gestureListener.onOrientationChanged(azimuth, elevation);
        }
    }

    /**
     * Reset the view to the default orientation and field of view.
     *
     * Resets azimuth, elevation, and roll to the defaults (looking at RA=0, Dec=0),
     * applies a 60° field of view, and notifies the GestureListener (if set)
     * via onOrientationChanged and onFovChanged.
     */
    public void resetView() {
        azimuth = 0;
        elevation = 0;
        roll = 0;
        updateViewOrientation();

        queueEvent(() -> renderer.setFieldOfView(60.0f));

        if (gestureListener != null) {
            gestureListener.onOrientationChanged(azimuth, elevation);
            gestureListener.onFovChanged(60.0f);
        }
    }

    /**
     * Set the renderer's night mode, toggling a low-brightness color scheme for dark environments.
     *
     * @param enabled true to enable night mode, false to disable it
     */
    public void setNightMode(boolean enabled) {
        queueEvent(() -> renderer.setNightMode(enabled));
    }

    /**
     * Requests a layer update on the next frame.
     */
    public void requestLayerUpdate() {
        queueEvent(renderer::requestLayerUpdate);
    }
}