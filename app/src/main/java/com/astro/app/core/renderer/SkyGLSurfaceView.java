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
         * Called when the user taps on the view.
         *
         * @param x Screen X coordinate
         * @param y Screen Y coordinate
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
         * Called when the user long-presses on the view.
         *
         * @param x Screen X coordinate
         * @param y Screen Y coordinate
         */
        void onLongPress(float x, float y);

        /**
         * Called when the view orientation changes.
         *
         * @param azimuth   Horizontal angle in radians
         * @param elevation Vertical angle in radians
         */
        void onOrientationChanged(float azimuth, float elevation);

        /**
         * Called when the field of view changes.
         *
         * @param fov Field of view in degrees
         */
        void onFovChanged(float fov);
    }

    /**
     * Simple implementation of GestureListener with empty methods.
     * Extend this class to override only the methods you need.
     */
    public static class SimpleGestureListener implements GestureListener {
        @Override
        public void onTap(float x, float y) {}

        @Override
        public void onDoubleTap(float x, float y) {}

        @Override
        public void onLongPress(float x, float y) {}

        @Override
        public void onOrientationChanged(float azimuth, float elevation) {}

        @Override
        public void onFovChanged(float fov) {}
    }

    /**
     * Creates a SkyGLSurfaceView with the given renderer.
     *
     * @param context  Android context
     * @param renderer The SkyRenderer to use
     */
    public SkyGLSurfaceView(@NonNull Context context, @NonNull SkyRenderer renderer) {
        super(context);
        this.renderer = renderer;
        init(context);
    }

    /**
     * Creates a SkyGLSurfaceView from XML attributes.
     *
     * <p>Note: When using this constructor, you must call {@link #setRenderer(Renderer)}
     * with a SkyRenderer before using the view.</p>
     *
     * @param context Android context
     * @param attrs   XML attributes
     */
    public SkyGLSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.renderer = new SkyRenderer();
        init(context);
    }

    /**
     * Initializes the view.
     */
    private void init(Context context) {
        // Set OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        // Set the renderer
        setRenderer(renderer);

        // Use continuous rendering
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Initialize gesture detectors
        gestureDetector = new GestureDetector(context, new GestureHandler());
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleHandler());

        // Update initial orientation
        updateViewOrientation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Let scale gesture detector handle pinch gestures first
        boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);

        // Let gesture detector handle other gestures
        boolean gestureHandled = gestureDetector.onTouchEvent(event);

        return scaleHandled || gestureHandled || super.onTouchEvent(event);
    }

    /**
     * Updates the view orientation in the renderer based on current azimuth and elevation.
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
            float newUpX = upX * cosRoll + rightX * sinRoll;
            float newUpY = upY * cosRoll + rightY * sinRoll;
            float newUpZ = upZ * cosRoll + rightZ * sinRoll;

            upX = newUpX;
            upY = newUpY;
            upZ = newUpZ;
        }

        // Update renderer
        queueEvent(() -> renderer.setViewOrientation(lookX, lookY, lookZ, upX, upY, upZ));
    }

    /**
     * Gesture handler for pan, tap, and long press.
     */
    private class GestureHandler extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true; // Must return true to receive subsequent events
        }

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

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (gestureListener != null) {
                gestureListener.onTap(e.getX(), e.getY());
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (gestureListener != null) {
                gestureListener.onDoubleTap(e.getX(), e.getY());
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (gestureListener != null) {
                gestureListener.onLongPress(e.getX(), e.getY());
            }
        }

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

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isPinching = true;
            return true;
        }

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

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isPinching = false;
        }
    }

    // ========== Public API ==========

    /**
     * Returns the SkyRenderer.
     *
     * @return The renderer
     */
    @NonNull
    public SkyRenderer getSkyRenderer() {
        return renderer;
    }

    /**
     * Sets the gesture listener.
     *
     * @param listener The listener, or null to remove
     */
    public void setGestureListener(@Nullable GestureListener listener) {
        this.gestureListener = listener;
    }

    /**
     * Sets the view orientation.
     *
     * @param azimuth   Horizontal angle in radians
     * @param elevation Vertical angle in radians
     */
    public void setOrientation(float azimuth, float elevation) {
        this.azimuth = azimuth;
        this.elevation = Math.max(-((float) Math.PI / 2 - 0.01f),
                Math.min((float) Math.PI / 2 - 0.01f, elevation));
        updateViewOrientation();
    }

    /**
     * Sets the view orientation with roll.
     *
     * @param azimuth   Horizontal angle in radians
     * @param elevation Vertical angle in radians
     * @param roll      Roll angle in radians
     */
    public void setOrientation(float azimuth, float elevation, float roll) {
        this.azimuth = azimuth;
        this.elevation = Math.max(-((float) Math.PI / 2 - 0.01f),
                Math.min((float) Math.PI / 2 - 0.01f, elevation));
        this.roll = roll;
        updateViewOrientation();
    }

    /**
     * Returns the current azimuth angle.
     *
     * @return Azimuth in radians
     */
    public float getAzimuth() {
        return azimuth;
    }

    /**
     * Returns the current elevation angle.
     *
     * @return Elevation in radians
     */
    public float getElevation() {
        return elevation;
    }

    /**
     * Returns the current roll angle.
     *
     * @return Roll in radians
     */
    public float getRoll() {
        return roll;
    }

    /**
     * Sets the field of view.
     *
     * @param fovDegrees FOV in degrees
     */
    public void setFieldOfView(float fovDegrees) {
        final float fov = Math.max(minFov, Math.min(maxFov, fovDegrees));
        queueEvent(() -> renderer.setFieldOfView(fov));
    }

    /**
     * Returns the current field of view.
     *
     * @return FOV in degrees
     */
    public float getFieldOfView() {
        return renderer.getFieldOfView();
    }

    /**
     * Sets the FOV limits for zoom.
     *
     * @param minFov Minimum FOV in degrees
     * @param maxFov Maximum FOV in degrees
     */
    public void setFovLimits(float minFov, float maxFov) {
        this.minFov = minFov;
        this.maxFov = maxFov;
    }

    /**
     * Sets the pan sensitivity.
     *
     * @param sensitivity Sensitivity factor (default 0.005)
     */
    public void setPanSensitivity(float sensitivity) {
        this.panSensitivity = sensitivity;
    }

    /**
     * Sets the zoom sensitivity.
     *
     * @param sensitivity Sensitivity factor (default 1.0)
     */
    public void setZoomSensitivity(float sensitivity) {
        this.zoomSensitivity = sensitivity;
    }

    /**
     * Points the view toward a celestial object.
     *
     * @param ra  Right Ascension in degrees
     * @param dec Declination in degrees
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
     * Points the view toward a celestial object specified by a Vector3.
     *
     * @param direction The direction vector (will be normalized)
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
     * Resets the view to default orientation (looking at RA=0, Dec=0).
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
     * Enables or disables night mode.
     *
     * @param enabled true to enable night mode
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
