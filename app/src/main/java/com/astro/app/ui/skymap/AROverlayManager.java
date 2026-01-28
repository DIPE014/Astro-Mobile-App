package com.astro.app.ui.skymap;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.core.math.Vector3;
import com.astro.app.core.renderer.SkyGLSurfaceView;
import com.astro.app.core.renderer.SkyRenderer;
import com.astro.app.data.model.GeocentricCoords;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.StarRepository;

import java.util.List;

/**
 * Manages the AR overlay of the sky renderer on the camera preview.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>Calibrating the camera FOV with sky coordinates</li>
 *   <li>Converting screen coordinates to celestial coordinates</li>
 *   <li>Finding celestial objects at screen positions (tap-to-select)</li>
 *   <li>Managing transparency of the sky overlay</li>
 * </ul>
 * </p>
 *
 * <h3>Coordinate System:</h3>
 * <p>The AR overlay maps screen coordinates (pixels) to celestial coordinates
 * (Right Ascension / Declination). The transformation depends on:</p>
 * <ul>
 *   <li>Camera field of view (FOV)</li>
 *   <li>Current device orientation (azimuth, elevation)</li>
 *   <li>Screen dimensions</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * AROverlayManager arManager = new AROverlayManager(starRepository);
 * arManager.setSkyView(skyGLSurfaceView);
 * arManager.calibrate(cameraHorizontalFov, cameraVerticalFov);
 *
 * // On tap event
 * StarData star = arManager.findObjectAtScreenPosition(x, y);
 * if (star != null) {
 *     showStarInfo(star);
 * }
 * }</pre>
 */
public class AROverlayManager {

    private static final String TAG = "AROverlayManager";

    /** Default search radius in degrees for tap-to-select */
    private static final float DEFAULT_SEARCH_RADIUS_DEGREES = 5.0f;

    /** Minimum magnitude for visible stars (filter out very dim stars) */
    private static final float MAX_MAGNITUDE_FOR_TAP = 6.0f;

    private final StarRepository starRepository;

    @Nullable
    private SkyGLSurfaceView skyView;

    @Nullable
    private SkyRenderer renderer;

    // Camera FOV in degrees
    private float cameraHorizontalFov = 66.0f;
    private float cameraVerticalFov = 50.0f;

    // Screen dimensions
    private int screenWidth = 1;
    private int screenHeight = 1;

    // Current view orientation (from device sensors)
    private float currentAzimuth = 0.0f;   // Horizontal angle in radians
    private float currentElevation = 0.0f; // Vertical angle in radians

    // Calibration state
    private boolean isCalibrated = false;

    // Search radius for tap-to-select
    private float searchRadiusDegrees = DEFAULT_SEARCH_RADIUS_DEGREES;

    /**
     * Callback interface for object selection events.
     */
    public interface ObjectSelectionCallback {
        /**
         * Called when a celestial object is selected by tap.
         *
         * @param star The selected star, or null if no object found at position
         * @param screenX The X screen coordinate of the tap
         * @param screenY The Y screen coordinate of the tap
         */
        void onObjectSelected(@Nullable StarData star, float screenX, float screenY);
    }

    @Nullable
    private ObjectSelectionCallback selectionCallback;

    /**
     * Creates a new AROverlayManager.
     *
     * @param starRepository Repository for accessing star data
     */
    public AROverlayManager(@NonNull StarRepository starRepository) {
        this.starRepository = starRepository;
    }

    /**
     * Sets the SkyGLSurfaceView to manage.
     *
     * @param skyView The sky view to overlay on the camera
     */
    public void setSkyView(@NonNull SkyGLSurfaceView skyView) {
        this.skyView = skyView;
        this.renderer = skyView.getSkyRenderer();
    }

    /**
     * Sets the callback for object selection events.
     *
     * @param callback The callback to receive selection events
     */
    public void setObjectSelectionCallback(@Nullable ObjectSelectionCallback callback) {
        this.selectionCallback = callback;
    }

    /**
     * Calibrates the AR overlay with the camera's field of view.
     *
     * <p>This method should be called after the camera is started to match
     * the sky renderer's FOV with the camera's actual FOV.</p>
     *
     * @param horizontalFov Camera horizontal FOV in degrees
     * @param verticalFov   Camera vertical FOV in degrees
     */
    public void calibrate(float horizontalFov, float verticalFov) {
        this.cameraHorizontalFov = horizontalFov;
        this.cameraVerticalFov = verticalFov;

        // Update the sky renderer's FOV to match the camera
        if (skyView != null) {
            // Use the vertical FOV for the perspective projection
            skyView.setFieldOfView(verticalFov);
        }

        isCalibrated = true;
        Log.d(TAG, String.format("Calibrated: H-FOV=%.1f, V-FOV=%.1f", horizontalFov, verticalFov));
    }

    /**
     * Calibrates with default smartphone camera FOV values.
     */
    public void calibrateDefault() {
        calibrate(66.0f, 50.0f);
    }

    /**
     * Updates the screen dimensions.
     *
     * @param width  Screen width in pixels
     * @param height Screen height in pixels
     */
    public void setScreenDimensions(int width, int height) {
        this.screenWidth = Math.max(1, width);
        this.screenHeight = Math.max(1, height);
    }

    /**
     * Updates the current view orientation.
     *
     * <p>This should be called when device sensors detect orientation changes.</p>
     *
     * @param azimuth   Horizontal angle in radians
     * @param elevation Vertical angle in radians
     */
    public void setOrientation(float azimuth, float elevation) {
        this.currentAzimuth = azimuth;
        this.currentElevation = elevation;

        // Update the sky view orientation
        if (skyView != null) {
            skyView.setOrientation(azimuth, elevation);
        }
    }

    /**
     * Converts screen coordinates to celestial coordinates.
     *
     * <p>The screen center is mapped to the current look direction.
     * Offset from center is mapped based on the FOV.</p>
     *
     * @param screenX Screen X coordinate in pixels
     * @param screenY Screen Y coordinate in pixels
     * @return GeocentricCoords at the screen position, or null if conversion fails
     */
    @Nullable
    public GeocentricCoords screenToSky(float screenX, float screenY) {
        if (!isCalibrated) {
            Log.w(TAG, "screenToSky called before calibration");
            return null;
        }

        // Calculate normalized screen coordinates (-1 to 1)
        float normalizedX = (screenX / screenWidth - 0.5f) * 2.0f;
        float normalizedY = (0.5f - screenY / screenHeight) * 2.0f; // Invert Y

        // Calculate angular offset from center based on FOV
        float horizontalAngle = (float) Math.toRadians(normalizedX * cameraHorizontalFov / 2.0f);
        float verticalAngle = (float) Math.toRadians(normalizedY * cameraVerticalFov / 2.0f);

        // Calculate the celestial coordinates
        float ra = currentAzimuth + horizontalAngle;
        float dec = currentElevation + verticalAngle;

        // Normalize RA to [0, 2*PI)
        while (ra < 0) ra += 2 * Math.PI;
        while (ra >= 2 * Math.PI) ra -= 2 * Math.PI;

        // Clamp Dec to [-PI/2, PI/2]
        dec = Math.max(-(float) Math.PI / 2, Math.min((float) Math.PI / 2, dec));

        // Convert from azimuth/elevation to RA/Dec
        // Note: This is a simplified conversion; actual conversion depends on
        // observer location and sidereal time
        return GeocentricCoords.fromRadians(ra, dec);
    }

    /**
     * Finds the nearest celestial object to a screen position.
     *
     * @param screenX Screen X coordinate in pixels
     * @param screenY Screen Y coordinate in pixels
     * @return The nearest StarData within search radius, or null if none found
     */
    @Nullable
    public StarData findObjectAtScreenPosition(float screenX, float screenY) {
        GeocentricCoords tapCoords = screenToSky(screenX, screenY);
        if (tapCoords == null) {
            return null;
        }

        return findNearestStar(tapCoords, searchRadiusDegrees);
    }

    /**
     * Finds the nearest star to the given celestial coordinates.
     *
     * @param coords The celestial coordinates to search near
     * @param maxDistanceDegrees Maximum angular distance in degrees
     * @return The nearest StarData within the search radius, or null if none found
     */
    @Nullable
    public StarData findNearestStar(@NonNull GeocentricCoords coords, float maxDistanceDegrees) {
        List<StarData> stars = starRepository.getStarsByMagnitude(MAX_MAGNITUDE_FOR_TAP);

        StarData nearestStar = null;
        float nearestDistance = Float.MAX_VALUE;

        for (StarData star : stars) {
            GeocentricCoords starCoords = GeocentricCoords.fromDegrees(star.getRa(), star.getDec());
            float distance = coords.angularDistanceTo(starCoords);

            if (distance < maxDistanceDegrees && distance < nearestDistance) {
                nearestDistance = distance;
                nearestStar = star;
            }
        }

        if (nearestStar != null) {
            Log.d(TAG, String.format("Found star: %s at distance %.2f degrees",
                    nearestStar.getName(), nearestDistance));
        }

        return nearestStar;
    }

    /**
     * Handles a tap event at the given screen position.
     *
     * <p>This method finds any celestial object at the tap position and
     * notifies the callback.</p>
     *
     * @param screenX Screen X coordinate in pixels
     * @param screenY Screen Y coordinate in pixels
     */
    public void handleTap(float screenX, float screenY) {
        StarData star = findObjectAtScreenPosition(screenX, screenY);

        if (selectionCallback != null) {
            selectionCallback.onObjectSelected(star, screenX, screenY);
        }
    }

    /**
     * Converts celestial coordinates to screen coordinates.
     *
     * @param coords The celestial coordinates
     * @return float array [x, y] of screen coordinates, or null if object is not visible
     */
    @Nullable
    public float[] skyToScreen(@NonNull GeocentricCoords coords) {
        if (!isCalibrated) {
            return null;
        }

        // Calculate angular offset from current view direction
        float ra = coords.getRaRadians();
        float dec = coords.getDecRadians();

        float deltaAzimuth = ra - currentAzimuth;
        float deltaElevation = dec - currentElevation;

        // Normalize delta azimuth to [-PI, PI]
        while (deltaAzimuth > Math.PI) deltaAzimuth -= 2 * Math.PI;
        while (deltaAzimuth < -Math.PI) deltaAzimuth += 2 * Math.PI;

        // Check if object is within FOV
        float halfHFov = (float) Math.toRadians(cameraHorizontalFov / 2);
        float halfVFov = (float) Math.toRadians(cameraVerticalFov / 2);

        if (Math.abs(deltaAzimuth) > halfHFov || Math.abs(deltaElevation) > halfVFov) {
            return null; // Object is outside the field of view
        }

        // Convert to normalized screen coordinates
        float normalizedX = deltaAzimuth / halfHFov;
        float normalizedY = deltaElevation / halfVFov;

        // Convert to screen pixels
        float screenX = (normalizedX + 1.0f) / 2.0f * screenWidth;
        float screenY = (1.0f - normalizedY) / 2.0f * screenHeight;

        return new float[] {screenX, screenY};
    }

    /**
     * Sets the search radius for tap-to-select operations.
     *
     * @param radiusDegrees Search radius in degrees
     */
    public void setSearchRadius(float radiusDegrees) {
        this.searchRadiusDegrees = Math.max(1.0f, Math.min(20.0f, radiusDegrees));
    }

    /**
     * Returns whether the overlay is calibrated.
     *
     * @return true if calibration has been performed
     */
    public boolean isCalibrated() {
        return isCalibrated;
    }

    /**
     * Enables AR mode by making the sky background transparent.
     *
     * @param enabled true to enable AR mode (transparent background)
     */
    public void setARModeEnabled(boolean enabled) {
        if (renderer != null) {
            if (enabled) {
                // Make background fully transparent for AR mode
                renderer.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.0f);
            } else {
                // Use opaque black background for map-only mode
                renderer.setBackgroundColor(0.0f, 0.0f, 0.0f, 1.0f);
            }
        }
    }

    /**
     * Gets the current look direction as a Vector3.
     *
     * @return Vector3 representing the current look direction
     */
    @NonNull
    public Vector3 getLookDirection() {
        float cosElev = (float) Math.cos(currentElevation);
        float sinElev = (float) Math.sin(currentElevation);
        float cosAz = (float) Math.cos(currentAzimuth);
        float sinAz = (float) Math.sin(currentAzimuth);

        return new Vector3(cosElev * cosAz, cosElev * sinAz, sinElev);
    }

    /**
     * Returns the current horizontal FOV.
     *
     * @return Horizontal FOV in degrees
     */
    public float getHorizontalFov() {
        return cameraHorizontalFov;
    }

    /**
     * Returns the current vertical FOV.
     *
     * @return Vertical FOV in degrees
     */
    public float getVerticalFov() {
        return cameraVerticalFov;
    }
}
