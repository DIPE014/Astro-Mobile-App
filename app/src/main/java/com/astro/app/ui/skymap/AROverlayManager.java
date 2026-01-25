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
 * Notifies that a celestial object was selected by a user tap.
 *
 * @param star    the selected star, or {@code null} if no object was found at the tapped position
 * @param screenX the X coordinate of the tap in screen pixels
 * @param screenY the Y coordinate of the tap in screen pixels
 */
        void onObjectSelected(@Nullable StarData star, float screenX, float screenY);
    }

    @Nullable
    private ObjectSelectionCallback selectionCallback;

    /**
     * Initialize a new AROverlayManager that manages the AR sky overlay and interaction between screen space and celestial coordinates.
     *
     * @param starRepository repository providing star catalog data used for object lookup and selection
     */
    public AROverlayManager(@NonNull StarRepository starRepository) {
        this.starRepository = starRepository;
    }

    /**
     * Attach the SkyGLSurfaceView used for AR overlay rendering.
     *
     * Stores the provided view and caches its SkyRenderer for rendering and coordinate conversions.
     *
     * @param skyView the SkyGLSurfaceView to overlay on the camera (must not be null)
     */
    public void setSkyView(@NonNull SkyGLSurfaceView skyView) {
        this.skyView = skyView;
        this.renderer = skyView.getSkyRenderer();
    }

    /**
     * Register or clear the callback invoked when a celestial object is selected via tap.
     *
     * @param callback the callback to receive selection events, or `null` to clear the callback
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
     * Calibrates the AR overlay using common smartphone camera field-of-view defaults (horizontal 66°, vertical 50°).
     */
    public void calibrateDefault() {
        calibrate(66.0f, 50.0f);
    }

    /**
     * Set the current screen size used for coordinate transformations.
     *
     * Values are clamped to at least 1 pixel to avoid invalid calculations.
     *
     * @param width  screen width in pixels
     * @param height screen height in pixels
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
         * Map a screen pixel position to celestial coordinates (right ascension and declination).
         *
         * <p>The screen center corresponds to the current look direction; offsets from center are
         * translated into angular offsets using the configured horizontal and vertical FOVs.
         * The resulting coordinates are expressed as geocentric radians.</p>
         *
         * @param screenX horizontal screen coordinate in pixels
         * @param screenY vertical screen coordinate in pixels
         * @return a GeocentricCoords representing the RA/Dec at the given screen position, or
         *         `null` if the AR overlay is not calibrated
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
         * Locate the nearest star to the given screen coordinates, if any.
         *
         * Converts the tap position to sky coordinates (requires calibration) and returns the nearest star within the currently configured search radius.
         *
         * @param screenX screen X coordinate in pixels
         * @param screenY screen Y coordinate in pixels
         * @return the nearest StarData within the configured search radius, or null if no star is found or the screen-to-sky conversion fails (for example, if not calibrated)
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
         * Locate the brightest candidate star nearest to the provided celestial coordinates within a maximum angular distance.
         *
         * @param coords             target celestial coordinates (geocentric)
         * @param maxDistanceDegrees maximum angular separation in degrees to consider
         * @return                   the nearest StarData within {@code maxDistanceDegrees}, or {@code null} if none found
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
     * Process a tap at the specified screen coordinates and invoke the registered object-selection callback with the star found (or `null`) at that location.
     *
     * @param screenX the x coordinate of the tap in pixels
     * @param screenY the y coordinate of the tap in pixels
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
     * Configure the tap-to-select search radius.
     *
     * The provided value is clamped to the range 1.0–20.0 degrees.
     *
     * @param radiusDegrees desired radius in degrees; values outside [1, 20] are clamped
     */
    public void setSearchRadius(float radiusDegrees) {
        this.searchRadiusDegrees = Math.max(1.0f, Math.min(20.0f, radiusDegrees));
    }

    /**
     * Indicates whether the AR overlay has been calibrated.
     *
     * @return true if calibration has been performed, false otherwise.
     */
    public boolean isCalibrated() {
        return isCalibrated;
    }

    /**
     * Toggle AR mode by switching the sky background between transparent and opaque.
     *
     * @param enabled true to use a transparent background for AR mode, false to use an opaque background for map-only mode
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
     * Get the current look direction as a 3D unit vector.
     *
     * @return a Vector3 unit vector pointing toward the current view direction; components are (x: east, y: north, z: up)
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
     * Get the current horizontal field of view used for coordinate transformations.
     *
     * @return Horizontal field of view in degrees
     */
    public float getHorizontalFov() {
        return cameraHorizontalFov;
    }

    /**
     * Get the current vertical field of view used for screen-to-sky mappings.
     *
     * @return the vertical field of view in degrees
     */
    public float getVerticalFov() {
        return cameraVerticalFov;
    }
}