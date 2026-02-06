package com.astro.app.core.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import com.astro.app.data.model.ConstellationData;
import com.astro.app.data.model.GeocentricCoords;
import com.astro.app.data.model.StarData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

import com.astro.app.core.math.TimeUtilsKt;

/**
 * Canvas-based sky renderer that displays real star data.
 *
 * <p>Converts astronomical coordinates (RA/Dec) to screen coordinates
 * based on observer location, time, and device orientation.</p>
 */
public class SkyCanvasView extends View {
    private static final String TAG = "SkyCanvasView";

    /**
     * Listener interface for star selection events.
     */
    public interface OnStarSelectedListener {
        void onStarSelected(StarData star);
    }

    public interface OnObjectSelectedListener {
        void onObjectSelected(SelectableObject obj);
    }

    public interface OnSkyTapListener {
        void onSkyTap(float x, float y);
    }

    public interface OnManualModeListener {
        void onManualModeChanged(boolean isManual);
    }

    private OnStarSelectedListener starSelectedListener;
    private OnObjectSelectedListener objectSelectedListener;
    private OnSkyTapListener skyTapListener;
    private OnManualModeListener manualModeListener;

    private Paint starPaint;
    private Paint linePaint;
    private Paint labelPaint;
    private Paint backgroundPaint;

    private boolean nightMode = false;
    private float fieldOfView = 90f;

    // Observer parameters
    private double observerLatitude = 40.7128;  // Default: New York
    private double observerLongitude = -74.0060;
    private float azimuthOffset = 0f;  // Device orientation in degrees
    private float altitudeOffset = 45f; // Device tilt (0 = horizon, 90 = zenith)

    // Manual mode state
    private boolean isManualMode = false;
    private float manualAzimuth = 0f;
    private float manualAltitude = 45f;

    // Gesture detectors
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private boolean isPinching = false;

    // FOV limits
    private static final float MIN_FOV = 20f;
    private static final float MAX_FOV = 120f;
    private long observationTime = System.currentTimeMillis(); // Time for sky calculations

    // Star data from repository
    private List<StarData> realStarData = new CopyOnWriteArrayList<>();
    private final java.util.Set<String> topStarIds = new java.util.HashSet<>();

    // Planet data: name -> {ra, dec, color, size}
    private final java.util.Map<String, float[]> planetData = new HashMap<>();
    private boolean showPlanets = false;
    private Paint planetPaint;
    private Paint planetLabelPaint;
    private Paint highlightRingPaint;
    private Paint highlightGlowPaint;

    // Constellation data
    private List<ConstellationData> constellations = new CopyOnWriteArrayList<>();
    private final java.util.Map<String, StarData> starLookupMap = new HashMap<>();
    private boolean showConstellations = true;
    private boolean showConstellationLabels = true;
    private Paint constellationLinePaint;
    private Paint constellationLabelPaint;
    private static final int CONSTELLATION_LINE_COLOR = Color.argb(100, 100, 150, 255);
    private static final int CONSTELLATION_LINE_COLOR_NIGHT = Color.argb(100, 150, 50, 50);

    // Grid data
    private boolean showGrid = false;
    private Paint gridLinePaint;
    private Paint gridLabelPaint;
    private static final int GRID_LINE_COLOR = Color.argb(60, 100, 200, 100);
    private static final int GRID_LINE_COLOR_NIGHT = Color.argb(60, 150, 80, 80);
    private static final int GRID_LABEL_COLOR = Color.argb(150, 100, 200, 100);
    private static final int GRID_LABEL_COLOR_NIGHT = Color.argb(150, 150, 80, 80);

    // Reticle settings for center selection
    private static final int RETICLE_COLOR = Color.argb(80, 255, 255, 255);
    private static final int RETICLE_COLOR_NIGHT = Color.argb(80, 200, 100, 100);
    private static final int HIGHLIGHT_COLOR = Color.argb(255, 255, 80, 80);  // Red for highlighted planets
    private static final int STAR_HIGHLIGHT_COLOR = Color.argb(255, 255, 80, 80);  // Red for highlighted stars
    private static final int NIGHT_TOP_STAR_COLOR = Color.argb(255, 255, 215, 0); // Yellow for top stars
    private float reticleRadiusPx = 120f;  // Default 120 pixels radius
    private Paint reticlePaint;

    // Highlighted object for selection preview
    private StarData highlightedStar = null;
    private String highlightedPlanetName = null;

    // Rendered elements (computed from star data)
    private List<float[]> stars = new CopyOnWriteArrayList<>();  // x, y, size, color, isTopStar
    private List<float[]> lines = new CopyOnWriteArrayList<>();  // x1, y1, x2, y2, color
    private List<Object[]> labels = new CopyOnWriteArrayList<>(); // x, y, text

    // Use simple star map mode (guaranteed to show stars)
    private boolean useSimpleStarMap = true;
    private boolean searchModeActive = false;

    public SkyCanvasView(Context context) {
        super(context);
        init();
        initGestureDetectors();
    }

    public SkyCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        initGestureDetectors();
    }

    private void init() {
        // Make the view clickable and focusable for touch events
        setClickable(true);
        setFocusable(true);

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1.5f);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(24f);
        labelPaint.setColor(Color.WHITE);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.rgb(5, 5, 20)); // Dark blue

        planetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        planetPaint.setStyle(Paint.Style.FILL);

        planetLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        planetLabelPaint.setTextSize(28f);
        planetLabelPaint.setColor(Color.rgb(255, 183, 77)); // Warm orange for planet labels

        highlightRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightRingPaint.setStyle(Paint.Style.STROKE);
        highlightRingPaint.setStrokeWidth(4f);
        highlightRingPaint.setColor(HIGHLIGHT_COLOR);

        highlightGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightGlowPaint.setStyle(Paint.Style.FILL);
        highlightGlowPaint.setColor(HIGHLIGHT_COLOR);

        constellationLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        constellationLinePaint.setStyle(Paint.Style.STROKE);
        constellationLinePaint.setStrokeWidth(1.5f);
        constellationLinePaint.setColor(CONSTELLATION_LINE_COLOR);

        constellationLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        constellationLabelPaint.setTextSize(22f);
        constellationLabelPaint.setTextAlign(Paint.Align.CENTER);
        constellationLabelPaint.setColor(Color.argb(180, 150, 180, 255));

        gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridLinePaint.setStyle(Paint.Style.STROKE);
        gridLinePaint.setStrokeWidth(1f);
        gridLinePaint.setColor(GRID_LINE_COLOR);

        gridLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridLabelPaint.setTextSize(18f);
        gridLabelPaint.setTextAlign(Paint.Align.LEFT);
        gridLabelPaint.setColor(GRID_LABEL_COLOR);

        reticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reticlePaint.setStyle(Paint.Style.STROKE);
        reticlePaint.setStrokeWidth(3f);
        reticlePaint.setColor(RETICLE_COLOR);
    }

    private void initGestureDetectors() {
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isPinching = true;
                enterManualMode();
                return true;
            }
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                fieldOfView /= scaleFactor;
                fieldOfView = Math.max(MIN_FOV, Math.min(MAX_FOV, fieldOfView));
                invalidate();
                return true;
            }
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isPinching = false;
            }
        });

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (isPinching || !isManualMode) return false;
                float pixelsPerDegree = Math.min(getWidth(), getHeight()) / fieldOfView;
                manualAzimuth += distanceX / pixelsPerDegree;
                manualAltitude += distanceY / pixelsPerDegree;
                manualAltitude = Math.max(-90f, Math.min(90f, manualAltitude));
                manualAzimuth = ((manualAzimuth % 360f) + 360f) % 360f;
                azimuthOffset = manualAzimuth;
                altitudeOffset = manualAltitude;
                invalidate();
                return true;
            }
        });
    }

    public void enterManualMode() {
        if (!isManualMode) {
            isManualMode = true;
            manualAzimuth = azimuthOffset;
            manualAltitude = altitudeOffset;
            if (manualModeListener != null) manualModeListener.onManualModeChanged(true);
        }
    }

    public void exitManualMode() {
        isManualMode = false;
        fieldOfView = 90f;
        if (manualModeListener != null) manualModeListener.onManualModeChanged(false);
        invalidate();
    }

    public boolean isManualMode() { return isManualMode; }

    public void setOnManualModeListener(OnManualModeListener l) { this.manualModeListener = l; }

    /**
     * Sets the real star data from the repository.
     * This replaces any demo data and converts coordinates for display.
     *
     * @param starList List of StarData objects from the repository
     */
    public void setStarData(List<StarData> starList) {
        this.realStarData.clear();
        this.topStarIds.clear();
        if (starList != null) {
            this.realStarData.addAll(starList);
            // Build star lookup map for constellation line rendering
            buildStarLookupMap(starList);
            cacheTopStars(starList);
        }
        Log.d(TAG, "STARS: Received " + realStarData.size() + " stars from repository");
        if (useSimpleStarMap) {
            // Simple mode - just invalidate to trigger onDraw which renders directly
            invalidate();
        } else {
            updateStarPositions();
        }
    }

    private void cacheTopStars(List<StarData> starList) {
        int limit = Math.min(100, starList.size());
        for (int i = 0; i < limit; i++) {
            StarData star = starList.get(i);
            if (star != null && star.getId() != null) {
                topStarIds.add(star.getId());
            }
        }
    }

    /**
     * Builds a lookup map from star IDs to StarData for constellation rendering.
     *
     * @param starList List of stars to index
     */
    private void buildStarLookupMap(List<StarData> starList) {
        starLookupMap.clear();
        for (StarData star : starList) {
            if (star.getId() != null) {
                starLookupMap.put(star.getId(), star);
            }
            // Also index by name for fallback
            if (star.getName() != null && !star.getName().isEmpty()) {
                starLookupMap.put(star.getName().toLowerCase(), star);
            }
        }
        Log.d(TAG, "CONSTELLATIONS: Built star lookup map with " + starLookupMap.size() + " entries");
    }

    /**
     * Sets the constellation data for line rendering.
     *
     * @param constellationList List of ConstellationData objects
     */
    public void setConstellationData(List<ConstellationData> constellationList) {
        this.constellations.clear();
        if (constellationList != null) {
            this.constellations.addAll(constellationList);
        }
        Log.d(TAG, "CONSTELLATIONS: Received " + constellations.size() + " constellations");
        invalidate();
    }

    /**
     * Sets the visibility of constellation lines.
     *
     * @param visible true to show constellation lines
     */
    public void setConstellationsVisible(boolean visible) {
        this.showConstellations = visible;
        Log.d(TAG, "CONSTELLATIONS: Visibility set to " + visible);
        invalidate();
    }

    /**
     * Sets the visibility of constellation labels.
     *
     * @param visible true to show constellation names
     */
    public void setConstellationLabelsVisible(boolean visible) {
        this.showConstellationLabels = visible;
        invalidate();
    }

    /**
     * Returns whether constellations are visible.
     */
    public boolean isConstellationsVisible() {
        return showConstellations;
    }

    /**
     * Sets the visibility of the coordinate grid.
     *
     * @param visible true to show the grid
     */
    public void setGridVisible(boolean visible) {
        this.showGrid = visible;
        Log.d(TAG, "GRID: Visibility set to " + visible);
        invalidate();
    }

    /**
     * Returns whether the coordinate grid is visible.
     */
    public boolean isGridVisible() {
        return showGrid;
    }

    /**
     * Sets the observer's location for coordinate conversion.
     *
     * @param latitude  Observer's latitude in degrees (-90 to 90)
     * @param longitude Observer's longitude in degrees (-180 to 180)
     */
    public void setObserverLocation(double latitude, double longitude) {
        this.observerLatitude = latitude;
        this.observerLongitude = longitude;
        updateStarPositions();
    }

    /**
     * Sets the device orientation for view direction.
     *
     * @param azimuth  Compass direction in degrees (0 = North, 90 = East)
     * @param altitude Vertical angle in degrees (0 = horizon, 90 = zenith)
     */
    public void setOrientation(float azimuth, float altitude) {
        if (isManualMode) return;  // Ignore sensor updates in manual mode
        this.azimuthOffset = azimuth;
        this.altitudeOffset = altitude;
        // Always invalidate to trigger redraw with new orientation
        invalidate();
    }

    /**
     * Gets the current view RA by converting the view direction (Alt/Az) back to RA/Dec.
     *
     * @return Current view RA in degrees (0-360)
     */
    public float getViewRa() {
        double[] raDec = altAzToRaDec(altitudeOffset, azimuthOffset);
        return (float) raDec[0];
    }

    /**
     * Gets the current view Dec by converting the view direction (Alt/Az) back to RA/Dec.
     *
     * @return Current view Dec in degrees (-90 to +90)
     */
    public float getViewDec() {
        double[] raDec = altAzToRaDec(altitudeOffset, azimuthOffset);
        return (float) raDec[1];
    }

    /**
     * Converts Altitude/Azimuth to Right Ascension/Declination.
     * This is the inverse of raDecToAltAz().
     *
     * Standard astronomical conventions:
     * - Azimuth: 0 = North, 90 = East, 180 = South, 270 = West
     * - Hour Angle: positive West of meridian, negative East of meridian
     * - RA = LST - HA
     *
     * @param altitude Altitude in degrees (0 = horizon, 90 = zenith)
     * @param azimuth  Azimuth in degrees (0 = North, 90 = East)
     * @return Array of [RA, Dec] in degrees
     */
    private double[] altAzToRaDec(double altitude, double azimuth) {
        // Calculate Local Sidereal Time
        double lst = calculateLocalSiderealTime();

        // Convert to radians
        double latRad = Math.toRadians(observerLatitude);
        double altRad = Math.toRadians(altitude);
        double azRad = Math.toRadians(azimuth);

        // Calculate declination
        // sin(dec) = sin(alt) * sin(lat) + cos(alt) * cos(lat) * cos(az)
        double sinDec = Math.sin(altRad) * Math.sin(latRad) +
                        Math.cos(altRad) * Math.cos(latRad) * Math.cos(azRad);
        sinDec = Math.max(-1, Math.min(1, sinDec)); // Clamp to [-1, 1]
        double dec = Math.toDegrees(Math.asin(sinDec));

        // Calculate hour angle
        // cos(HA) = (sin(alt) - sin(lat) * sin(dec)) / (cos(lat) * cos(dec))
        double decRad = Math.toRadians(dec);
        double cosLat = Math.cos(latRad);
        double cosDec = Math.cos(decRad);

        double cosHA;
        if (Math.abs(cosLat) < 0.0001 || Math.abs(cosDec) < 0.0001) {
            // At poles or looking at celestial pole
            cosHA = 1;
        } else {
            cosHA = (Math.sin(altRad) - Math.sin(latRad) * Math.sin(decRad)) /
                    (cosLat * cosDec);
            cosHA = Math.max(-1, Math.min(1, cosHA)); // Clamp to [-1, 1]
        }

        double ha = Math.toDegrees(Math.acos(cosHA));

        // Determine the sign of HA based on azimuth
        // Standard convention:
        // - sin(Az) > 0 (East, Az 0-180): HA is negative (object hasn't crossed meridian)
        // - sin(Az) < 0 (West, Az 180-360): HA is positive (object has crossed meridian)
        if (Math.sin(azRad) > 0) {
            ha = -ha;
        }

        // Calculate RA from hour angle and LST
        // RA = LST - HA
        double ra = lst - ha;

        // Normalize RA to 0-360 range
        while (ra < 0) ra += 360;
        while (ra >= 360) ra -= 360;

        return new double[]{ra, dec};
    }

    /**
     * Gets the current view azimuth.
     *
     * @return Azimuth in degrees (0 = North)
     */
    public float getViewAzimuth() {
        return azimuthOffset;
    }

    /**
     * Gets the current view altitude.
     *
     * @return Altitude in degrees (0 = horizon, 90 = zenith)
     */
    public float getViewAltitude() {
        return altitudeOffset;
    }

    /**
     * Gets the current field of view in degrees.
     *
     * @return Field of view in degrees
     */
    public float getFieldOfView() {
        return fieldOfView;
    }

    /**
     * Sets the time for sky calculations (for time travel feature).
     *
     * @param timeMillis time in milliseconds since epoch
     */
    public void setTime(long timeMillis) {
        this.observationTime = timeMillis;
        Log.d(TAG, "TIME_TRAVEL: Time set to " + new java.util.Date(timeMillis));
        invalidate();
    }

    /**
     * Updates star screen positions based on current time, location, and orientation.
     */
    private void updateStarPositions() {
        stars.clear();
        labels.clear();

        if (realStarData.isEmpty()) {
            Log.d(TAG, "No star data available, adding demo stars");
            addDemoStars();
            invalidate();
            return;
        }

        // Calculate Local Sidereal Time
        double lst = calculateLocalSiderealTime();

        for (StarData star : realStarData) {
            // Convert RA/Dec to Alt/Az
            double[] altAz = raDecToAltAz(star.getRa(), star.getDec(), lst);
            double altitude = altAz[0];
            double azimuth = altAz[1];

            // Check if star is above horizon
            if (altitude < 0) {
                continue;
            }

            // Convert Alt/Az to screen coordinates based on view direction
            float[] screenPos = altAzToScreen(altitude, azimuth);
            if (screenPos == null) {
                continue; // Star is outside field of view
            }

            // Calculate star size based on magnitude (brighter = larger)
            float size = magnitudeToSize(star.getMagnitude());

            // Get star color
            int color = star.getColor() != 0 ? star.getColor() : Color.WHITE;
            float isTopStar = topStarIds.contains(star.getId()) ? 1f : 0f;

            stars.add(new float[]{screenPos[0], screenPos[1], size, color, isTopStar});

            // Add label for bright stars (magnitude < 2)
            if (star.getMagnitude() < 2.0f && star.getName() != null) {
                labels.add(new Object[]{screenPos[0], screenPos[1] - 0.02f, star.getName()});
            }
        }

        Log.d(TAG, "Rendered " + stars.size() + " visible stars");
        invalidate();
    }

    /**
     * Calculates the Local Sidereal Time for the observer's location.
     * Uses the proven implementation from TimeUtils matching stardroid.
     *
     * The formula is:
     * 1. Calculate Julian Day (including fractional day for current time)
     * 2. Calculate days since J2000.0: delta = JD - 2451545.0
     * 3. Greenwich Sidereal Time: GST = 280.461 + 360.98564737 * delta
     * 4. Local Sidereal Time: LST = GST + longitude
     *
     * @return LST in degrees (0-360)
     */
    private double calculateLocalSiderealTime() {
        // Use the proven meanSiderealTime implementation from TimeUtils.kt
        // This correctly handles the time of day in the Julian Day calculation
        Date observationDate = new Date(observationTime);
        double lst = TimeUtilsKt.meanSiderealTime(observationDate, (float) observerLongitude);

        // Log LST for time travel debugging (only log occasionally to avoid spam)
        if (lastLoggedLst < 0 || Math.abs(lst - lastLoggedLst) > 1.0) {
            Log.d(TAG, "TIME_TRAVEL: LST = " + String.format("%.2f", lst) + "° for time " +
                new Date(observationTime));
            lastLoggedLst = lst;
        }

        return lst;
    }

    // Track last logged LST to reduce log spam
    private double lastLoggedLst = -1;

    /**
     * Converts Right Ascension/Declination to Altitude/Azimuth.
     *
     * @param ra  Right Ascension in degrees
     * @param dec Declination in degrees
     * @param lst Local Sidereal Time in degrees
     * @return Array of [altitude, azimuth] in degrees
     */
    private double[] raDecToAltAz(double ra, double dec, double lst) {
        // Convert to radians
        double latRad = Math.toRadians(observerLatitude);
        double decRad = Math.toRadians(dec);

        // Hour Angle
        double ha = lst - ra;
        if (ha < 0) ha += 360;
        double haRad = Math.toRadians(ha);

        // Calculate altitude
        double sinAlt = Math.sin(decRad) * Math.sin(latRad) +
                        Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad);
        double altitude = Math.toDegrees(Math.asin(sinAlt));

        // Calculate azimuth
        double cosA = (Math.sin(decRad) - Math.sin(Math.toRadians(altitude)) * Math.sin(latRad)) /
                      (Math.cos(Math.toRadians(altitude)) * Math.cos(latRad));
        cosA = Math.max(-1, Math.min(1, cosA)); // Clamp to [-1, 1]
        double azimuth = Math.toDegrees(Math.acos(cosA));

        if (Math.sin(haRad) > 0) {
            azimuth = 360 - azimuth;
        }

        return new double[]{altitude, azimuth};
    }

    /**
     * Converts Altitude/Azimuth to normalized screen coordinates.
     *
     * @param altitude Altitude in degrees
     * @param azimuth  Azimuth in degrees
     * @return Array of [x, y] in range [0, 1], or null if outside FOV
     */
    private float[] altAzToScreen(double altitude, double azimuth) {
        // Calculate angular distance from view center
        double viewAz = azimuthOffset;
        double viewAlt = altitudeOffset;

        // Convert to radians
        double viewAzRad = Math.toRadians(viewAz);
        double viewAltRad = Math.toRadians(viewAlt);
        double starAzRad = Math.toRadians(azimuth);
        double starAltRad = Math.toRadians(altitude);

        // Calculate 3D position on unit sphere
        double starX = Math.cos(starAltRad) * Math.sin(starAzRad);
        double starY = Math.sin(starAltRad);
        double starZ = Math.cos(starAltRad) * Math.cos(starAzRad);

        // Rotate to view coordinates
        // First rotate around Y axis (azimuth)
        double cosAz = Math.cos(-viewAzRad);
        double sinAz = Math.sin(-viewAzRad);
        double x1 = starX * cosAz - starZ * sinAz;
        double z1 = starX * sinAz + starZ * cosAz;
        double y1 = starY;

        // Then rotate around X axis (altitude)
        double cosAlt = Math.cos(-viewAltRad);
        double sinAlt = Math.sin(-viewAltRad);
        double y2 = y1 * cosAlt - z1 * sinAlt;
        double z2 = y1 * sinAlt + z1 * cosAlt;
        double x2 = x1;

        // Check if star is in front of us
        if (z2 < 0.01) {
            return null;
        }

        // Project to screen (simple perspective projection)
        double fovRad = Math.toRadians(fieldOfView / 2);
        double scale = 1.0 / Math.tan(fovRad);

        double screenX = (x2 / z2) * scale;
        double screenY = (y2 / z2) * scale;

        // Check if within field of view
        if (Math.abs(screenX) > 1 || Math.abs(screenY) > 1) {
            return null;
        }

        // Convert to normalized screen coordinates (0 to 1)
        float normalizedX = (float) ((screenX + 1) / 2);
        float normalizedY = (float) ((1 - screenY) / 2); // Flip Y for screen coordinates

        return new float[]{normalizedX, normalizedY};
    }

    /**
     * Converts star magnitude to screen size.
     * Brighter stars (lower magnitude) appear larger.
     *
     * @param magnitude Star's apparent magnitude
     * @return Size in pixels
     */
    private float magnitudeToSize(float magnitude) {
        // Map magnitude range [-1.5, 6.5] to size range [8, 1]
        float normalizedMag = Math.max(-1.5f, Math.min(6.5f, magnitude));
        float size = 8 - ((normalizedMag + 1.5f) / 8.0f) * 7;
        return Math.max(1, Math.min(10, size));
    }

    /**
     * Adds demo stars when no real data is available.
     */
    private void addDemoStars() {
        // Add random stars for demo
        for (int i = 0; i < 200; i++) {
            float x = (float) (Math.random());
            float y = (float) (Math.random());
            float size = (float) (Math.random() * 4 + 1);
            int color = Color.WHITE;
            stars.add(new float[]{x, y, size, color});
        }

        // Add some constellation lines
        lines.add(new float[]{0.1f, 0.2f, 0.15f, 0.25f, Color.argb(100, 100, 150, 255)});
        lines.add(new float[]{0.15f, 0.25f, 0.2f, 0.22f, Color.argb(100, 100, 150, 255)});

        // Add a label
        labels.add(new Object[]{0.5f, 0.1f, "Polaris"});
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Draw background
        if (nightMode) {
            backgroundPaint.setColor(Color.rgb(20, 0, 0)); // Dark red for night mode
        } else {
            backgroundPaint.setColor(Color.rgb(5, 5, 20)); // Dark blue
        }
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // Use simple star map mode if enabled and we have real star data
        if (useSimpleStarMap && realStarData != null && !realStarData.isEmpty()) {
            // Draw grid first (behind everything)
            if (showGrid) {
                drawGrid(canvas, width, height);
            }
            // Draw constellation lines (behind stars)
            if (showConstellations) {
                drawConstellations(canvas, width, height);
            }
            drawSimpleStarMap(canvas, width, height);
            // Draw planets on top of stars
            if (showPlanets) {
                drawPlanets(canvas, width, height);
            } else if (highlightedPlanetName != null) {
                drawHighlightedPlanetOverlay(canvas, width, height);
            }
        } else {
            // Draw constellation lines (from complex projection mode)
            for (float[] line : lines) {
                linePaint.setColor((int) line[4]);
                canvas.drawLine(
                    line[0] * width, line[1] * height,
                    line[2] * width, line[3] * height,
                    linePaint
                );
            }

            // Draw stars (from complex projection mode)
            for (float[] star : stars) {
                float x = star[0] * width;
                float y = star[1] * height;
                float size = star[2];
                int color = (int) star[3];
                boolean isTopStar = star.length > 4 && star[4] == 1f;
                if (nightMode && isTopStar) {
                    starPaint.setColor(NIGHT_TOP_STAR_COLOR);
                } else {
                    starPaint.setColor(color);
                }
                canvas.drawCircle(x, y, size, starPaint);
            }

            // Draw labels
            for (Object[] label : labels) {
                float x = (float) label[0] * width;
                float y = (float) label[1] * height;
                String text = (String) label[2];
                if (nightMode) {
                    labelPaint.setColor(Color.rgb(255, 100, 100));
                } else {
                    labelPaint.setColor(Color.WHITE);
                }
                canvas.drawText(text, x, y, labelPaint);
            }
        }

        // Draw crosshair at center
        drawCrosshair(canvas, width, height);
    }

    /**
     * Draws stars using RA/Dec to screen coordinate mapping with device orientation.
     * The view is centered on where the device is pointing (azimuth/altitude).
     *
     * Uses proper spherical (gnomonic) projection to correctly handle the convergence
     * of azimuth lines near the zenith.
     */
    private void drawSimpleStarMap(Canvas canvas, int width, int height) {
        int starsDrawn = 0;

        // Calculate Local Sidereal Time for RA to azimuth conversion
        double lst = calculateLocalSiderealTime();

        // Center of screen
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Pixels per degree - determines how zoomed in the view is
        float pixelsPerDegree = Math.min(width, height) / fieldOfView;

        for (StarData star : realStarData) {
            // Get RA and Dec
            float ra = star.getRa();    // 0-360 degrees
            float dec = star.getDec();  // -90 to +90 degrees

            // Convert RA/Dec to Alt/Az for the observer's location and time
            double[] altAz = raDecToAltAz(ra, dec, lst);
            double starAlt = altAz[0];  // Altitude in degrees (0 = horizon, 90 = zenith)
            double starAz = altAz[1];   // Azimuth in degrees (0 = North, 90 = East)

            // No altitude filter - allow viewing stars in all directions
            // This enables full 360° sky rotation as if Earth was transparent

            // Use proper spherical projection
            float[] screenPos = projectToScreen(starAlt, starAz,
                    altitudeOffset, azimuthOffset,
                    centerX, centerY, pixelsPerDegree);

            // Skip if not visible (behind us)
            if (screenPos[2] < 0.5f) {
                continue;
            }

            float x = screenPos[0];
            float y = screenPos[1];

            // Skip if off screen (with margin for partially visible stars)
            if (x < -50 || x > width + 50 || y < -50 || y > height + 50) {
                continue;
            }

            // Star size based on magnitude (brighter = lower magnitude = larger)
            float magnitude = star.getMagnitude();
            float size = Math.max(2f, 8f - magnitude);  // mag 1 = size 7, mag 5 = size 3

            // Get star color or default to white
            int color = star.getColor() != 0 ? star.getColor() : Color.WHITE;

            // Check if this star is highlighted
            boolean isHighlighted = highlightedStar != null && star.getId().equals(highlightedStar.getId());

            if (isHighlighted) {
                starPaint.setColor(STAR_HIGHLIGHT_COLOR);
                size = size * 1.5f;  // Make highlighted star larger
            } else if (nightMode && topStarIds.contains(star.getId())) {
                starPaint.setColor(NIGHT_TOP_STAR_COLOR);
            } else {
                starPaint.setColor(color);
            }

            canvas.drawCircle(x, y, size, starPaint);
            starsDrawn++;
            if (isHighlighted) {
                float pulse = (float) (0.75f + 0.25f *
                        Math.sin((System.currentTimeMillis() % 1200L) / 1200.0 * Math.PI * 2.0));
                float ringRadius = size * (3.0f * pulse);
                int glowAlpha = (int) (80 + 60 * pulse);

                highlightGlowPaint.setColor(STAR_HIGHLIGHT_COLOR);
                highlightGlowPaint.setAlpha(glowAlpha);
                canvas.drawCircle(x, y, ringRadius, highlightGlowPaint);

                highlightRingPaint.setColor(STAR_HIGHLIGHT_COLOR);
                highlightRingPaint.setAlpha(200);
                canvas.drawCircle(x, y, ringRadius, highlightRingPaint);

                postInvalidateOnAnimation();
            }

            // Draw label for bright stars (magnitude < 2)
            if (magnitude < 2.0f) {
                if (nightMode) {
                    labelPaint.setColor(Color.rgb(255, 100, 100));
                } else {
                    labelPaint.setColor(Color.WHITE);
                }
                // Show actual name if available
                String label;
                String name = star.getName();
                if (name != null && !name.isEmpty() && !name.equals("null") && !name.startsWith("Star ")) {
                    label = name;
                } else {
                    // Format as coordinates if no real name
                    label = String.format("%.1fh %.1f\u00b0", ra / 15f, dec);
                }
                canvas.drawText(label, x + size + 4, y + 4, labelPaint);
            }
        }

        // Log sparingly to avoid flooding logcat
        if (starsDrawn != lastDrawnStarCount) {
            Log.d(TAG, "STARS: Drew " + starsDrawn + " stars (viewing Az=" +
                    String.format("%.1f", azimuthOffset) + ", Alt=" +
                    String.format("%.1f", altitudeOffset) + ")");
            lastDrawnStarCount = starsDrawn;
        }
    }

    // Track last drawn count to reduce log spam
    private int lastDrawnStarCount = -1;

    /**
     * Projects a celestial object from Alt/Az coordinates to screen coordinates using
     * proper spherical (gnomonic) projection. This correctly handles the convergence
     * of azimuth lines near the zenith.
     *
     * @param objectAlt Altitude of the object in degrees (0 = horizon, 90 = zenith)
     * @param objectAz  Azimuth of the object in degrees (0 = North, 90 = East)
     * @param viewAlt   Altitude of the view center in degrees
     * @param viewAz    Azimuth of the view center in degrees
     * @param centerX   Screen center X coordinate
     * @param centerY   Screen center Y coordinate
     * @param pixelsPerDegree Scale factor for projection
     * @return float array [x, y, visible] where visible is 1.0f if in front, 0.0f if behind
     */
    private float[] projectToScreen(double objectAlt, double objectAz,
                                    double viewAlt, double viewAz,
                                    float centerX, float centerY,
                                    float pixelsPerDegree) {
        // Convert view direction to 3D unit vector (x=East, y=North, z=Up)
        double viewAltRad = Math.toRadians(viewAlt);
        double viewAzRad = Math.toRadians(viewAz);
        double vx = Math.cos(viewAltRad) * Math.sin(viewAzRad);
        double vy = Math.cos(viewAltRad) * Math.cos(viewAzRad);
        double vz = Math.sin(viewAltRad);

        // Convert object direction to 3D unit vector
        double objectAltRad = Math.toRadians(objectAlt);
        double objectAzRad = Math.toRadians(objectAz);
        double ox = Math.cos(objectAltRad) * Math.sin(objectAzRad);
        double oy = Math.cos(objectAltRad) * Math.cos(objectAzRad);
        double oz = Math.sin(objectAltRad);

        // Check if object is in front of us using dot product
        double dot = vx * ox + vy * oy + vz * oz;
        if (dot <= 0.01) {
            // Object is behind or at edge - not visible
            return new float[]{0, 0, 0.0f};
        }

        // Build a local coordinate system for the view plane
        // right vector = view x up, then normalize (points East when looking North at horizon)
        // For general case, we use cross product with world up (0,0,1) then adjust

        // Up vector in world coords
        double worldUpX = 0, worldUpY = 0, worldUpZ = 1;

        // Right = view cross worldUp (gives us the horizontal right direction in the view plane)
        double rightX = vy * worldUpZ - vz * worldUpY;
        double rightY = vz * worldUpX - vx * worldUpZ;
        double rightZ = vx * worldUpY - vy * worldUpX;

        // Normalize right vector
        double rightLen = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        if (rightLen < 0.0001) {
            // Looking straight up or down - right is arbitrary, use East
            rightX = 1; rightY = 0; rightZ = 0;
            rightLen = 1;
        }
        rightX /= rightLen;
        rightY /= rightLen;
        rightZ /= rightLen;

        // Up in view plane = right cross view (perpendicular to both view and right)
        double upX = rightY * vz - rightZ * vy;
        double upY = rightZ * vx - rightX * vz;
        double upZ = rightX * vy - rightY * vx;

        // Normalize up vector
        double upLen = Math.sqrt(upX * upX + upY * upY + upZ * upZ);
        upX /= upLen;
        upY /= upLen;
        upZ /= upLen;

        // Project object onto the view plane using gnomonic projection
        // The object direction relative to view center
        double relX = ox - vx * dot;
        double relY = oy - vy * dot;
        double relZ = oz - vz * dot;

        // Project onto right and up axes of the view plane
        double screenRight = (relX * rightX + relY * rightY + relZ * rightZ) / dot;
        double screenUp = (relX * upX + relY * upY + relZ * upZ) / dot;

        // Convert to pixel coordinates
        // Note: screenRight positive = right on screen, screenUp positive = up on screen
        // We need to convert angular offset to pixels. For small angles, the gnomonic projection
        // gives us tan(angle), so we convert back to degrees and scale
        double angleRight = Math.toDegrees(Math.atan(screenRight));
        double angleUp = Math.toDegrees(Math.atan(screenUp));

        float x = centerX + (float)(angleRight * pixelsPerDegree);
        float y = centerY - (float)(angleUp * pixelsPerDegree);  // Y is inverted on screen

        return new float[]{x, y, 1.0f};
    }

    /**
     * Draws planets on the sky map.
     * Uses proper spherical (gnomonic) projection for correct rendering near zenith.
     */
    private void drawPlanets(Canvas canvas, int width, int height) {
        if (planetData.isEmpty()) {
            return;
        }

        // Calculate LST for coordinate conversion
        double lst = calculateLocalSiderealTime();

        // Center of screen
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Pixels per degree
        float pixelsPerDegree = Math.min(width, height) / fieldOfView;

        int planetsDrawn = 0;
        for (java.util.Map.Entry<String, float[]> entry : planetData.entrySet()) {
            String name = entry.getKey();
            float[] data = entry.getValue();
            float ra = data[0];
            float dec = data[1];
            int color = Float.floatToIntBits(data[2]);
            float size = data[3];

            // Convert RA/Dec to Alt/Az
            double[] altAz = raDecToAltAz(ra, dec, lst);
            double planetAlt = altAz[0];
            double planetAz = altAz[1];

            // Use proper spherical projection
            float[] screenPos = projectToScreen(planetAlt, planetAz,
                    altitudeOffset, azimuthOffset,
                    centerX, centerY, pixelsPerDegree);

            // Skip if not visible (behind us)
            if (screenPos[2] < 0.5f) {
                continue;
            }

            float x = screenPos[0];
            float y = screenPos[1];

            // Skip if off screen
            if (x < -50 || x > width + 50 || y < -50 || y > height + 50) {
                continue;
            }

            // Check if this planet is highlighted
            boolean isHighlighted = highlightedPlanetName != null && name.equals(highlightedPlanetName);

            // Draw planet point (larger than stars)
            if (isHighlighted) {
                planetPaint.setColor(HIGHLIGHT_COLOR);  // Red highlight
                size = size * 2.0f;  // Make highlighted planet larger
            } else if (nightMode) {
                planetPaint.setColor(Color.rgb(255, 100, 100)); // Red tint for night mode
            } else {
                planetPaint.setColor(color);
            }
            canvas.drawCircle(x, y, size, planetPaint);

            if (isHighlighted) {
                float pulse = (float) (0.75f + 0.25f *
                        Math.sin((System.currentTimeMillis() % 1200L) / 1200.0 * Math.PI * 2.0));
                float ringRadius = size * (2.2f * pulse);
                int glowAlpha = (int) (80 + 60 * pulse);

                highlightGlowPaint.setColor(HIGHLIGHT_COLOR);
                highlightGlowPaint.setAlpha(glowAlpha);
                canvas.drawCircle(x, y, ringRadius, highlightGlowPaint);

                highlightRingPaint.setColor(HIGHLIGHT_COLOR);
                highlightRingPaint.setAlpha(200);
                canvas.drawCircle(x, y, ringRadius, highlightRingPaint);

                postInvalidateOnAnimation();
            }

            // Draw planet name label
            if (nightMode) {
                planetLabelPaint.setColor(Color.rgb(255, 120, 120));
            } else {
                planetLabelPaint.setColor(Color.rgb(255, 183, 77)); // Warm orange
            }
            canvas.drawText(name, x + size + 6, y + 6, planetLabelPaint);

            planetsDrawn++;
        }

        if (planetsDrawn > 0) {
            Log.d(TAG, "PLANETS: Drew " + planetsDrawn + " planets on screen");
        }
    }

    /**
     * Draws only the highlighted planet when planets are hidden.
     */
    private void drawHighlightedPlanetOverlay(Canvas canvas, int width, int height) {
        if (highlightedPlanetName == null) {
            return;
        }

        float[] data = planetData.get(highlightedPlanetName);
        if (data == null) {
            return;
        }

        double lst = calculateLocalSiderealTime();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float pixelsPerDegree = Math.min(width, height) / fieldOfView;

        float ra = data[0];
        float dec = data[1];
        int color = Float.floatToIntBits(data[2]);
        float size = data[3];

        double[] altAz = raDecToAltAz(ra, dec, lst);
        double planetAlt = altAz[0];
        double planetAz = altAz[1];

        float[] screenPos = projectToScreen(planetAlt, planetAz,
                altitudeOffset, azimuthOffset,
                centerX, centerY, pixelsPerDegree);

        if (screenPos[2] < 0.5f) {
            return;
        }

        float x = screenPos[0];
        float y = screenPos[1];

        if (x < -50 || x > width + 50 || y < -50 || y > height + 50) {
            return;
        }

        planetPaint.setColor(HIGHLIGHT_COLOR);
        float drawSize = size * 2.0f;
        canvas.drawCircle(x, y, drawSize, planetPaint);

        float pulse = (float) (0.75f + 0.25f *
                Math.sin((System.currentTimeMillis() % 1200L) / 1200.0 * Math.PI * 2.0));
        float ringRadius = drawSize * (2.2f * pulse);
        int glowAlpha = (int) (80 + 60 * pulse);

        highlightGlowPaint.setColor(HIGHLIGHT_COLOR);
        highlightGlowPaint.setAlpha(glowAlpha);
        canvas.drawCircle(x, y, ringRadius, highlightGlowPaint);

        highlightRingPaint.setColor(HIGHLIGHT_COLOR);
        highlightRingPaint.setAlpha(200);
        canvas.drawCircle(x, y, ringRadius, highlightRingPaint);

        if (nightMode) {
            planetLabelPaint.setColor(Color.rgb(255, 120, 120));
        } else {
            planetLabelPaint.setColor(Color.rgb(255, 183, 77));
        }
        canvas.drawText(highlightedPlanetName, x + drawSize + 6, y + 6, planetLabelPaint);

        postInvalidateOnAnimation();
    }

    /**
     * Draws constellation lines on the sky map.
     * Uses proper spherical (gnomonic) projection for correct rendering near zenith.
     */
    private void drawConstellations(Canvas canvas, int width, int height) {
        if (constellations.isEmpty()) {
            return;
        }

        // Set line paint color based on night mode
        if (nightMode) {
            constellationLinePaint.setColor(CONSTELLATION_LINE_COLOR_NIGHT);
            constellationLabelPaint.setColor(Color.argb(150, 200, 100, 100));
        } else {
            constellationLinePaint.setColor(CONSTELLATION_LINE_COLOR);
            constellationLabelPaint.setColor(Color.argb(180, 150, 180, 255));
        }

        // Calculate LST for coordinate conversion
        double lst = calculateLocalSiderealTime();

        // Center of screen
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Pixels per degree
        float pixelsPerDegree = Math.min(width, height) / fieldOfView;

        int linesDrawn = 0;
        int labelsDrawn = 0;

        for (ConstellationData constellation : constellations) {
            List<int[]> lineIndices = constellation.getLineIndices();

            // Build position map for this constellation's stars (screen coords)
            // Format: [x, y, visible, starAz] where visible is 1.0 if in front
            java.util.Map<Integer, float[]> starPositions = new HashMap<>();

            // Use embedded coordinates if available (preferred), otherwise fall back to star lookup
            boolean hasEmbeddedCoords = constellation.hasStarCoordinates();
            int starCount = hasEmbeddedCoords ? constellation.getStarCoordinates().size() : constellation.getStarIds().size();

            for (int i = 0; i < starCount; i++) {
                float ra, dec;

                if (hasEmbeddedCoords) {
                    // Use embedded coordinates directly from constellation data
                    GeocentricCoords coords = constellation.getStarCoordinatesAt(i);
                    if (coords == null) continue;
                    ra = coords.getRa();
                    dec = coords.getDec();
                } else {
                    // Fall back to star lookup (legacy path)
                    String starId = constellation.getStarIds().get(i);
                    StarData star = findStarForConstellation(starId);
                    if (star == null) continue;
                    ra = star.getRa();
                    dec = star.getDec();
                }

                // Convert RA/Dec to Alt/Az
                double[] altAz = raDecToAltAz(ra, dec, lst);
                double starAlt = altAz[0];
                double starAz = altAz[1];

                // Use proper spherical projection
                float[] screenPos = projectToScreen(starAlt, starAz,
                        altitudeOffset, azimuthOffset,
                        centerX, centerY, pixelsPerDegree);

                // Store screen position, visibility flag, and azimuth
                starPositions.put(i, new float[]{screenPos[0], screenPos[1], screenPos[2], (float)starAz});
            }

            // Draw lines - prefer direct line segments, fall back to index-based approach
            if (constellation.hasLineSegments()) {
                // Use direct line segments from protobuf (more reliable)
                for (float[] segment : constellation.getLineSegments()) {
                    float startRa = segment[0];
                    float startDec = segment[1];
                    float endRa = segment[2];
                    float endDec = segment[3];

                    // Convert start RA/Dec to Alt/Az
                    double[] startAltAz = raDecToAltAz(startRa, startDec, lst);
                    double[] endAltAz = raDecToAltAz(endRa, endDec, lst);

                    // Project to screen
                    float[] startScreen = projectToScreen(startAltAz[0], startAltAz[1],
                            altitudeOffset, azimuthOffset,
                            centerX, centerY, pixelsPerDegree);
                    float[] endScreen = projectToScreen(endAltAz[0], endAltAz[1],
                            altitudeOffset, azimuthOffset,
                            centerX, centerY, pixelsPerDegree);

                    // Skip if either point is behind us
                    if (startScreen[2] < 0.5f || endScreen[2] < 0.5f) {
                        continue;
                    }

                    // Only draw if at least one endpoint is on screen
                    boolean startOnScreen = startScreen[0] >= -50 && startScreen[0] <= width + 50 &&
                                            startScreen[1] >= -50 && startScreen[1] <= height + 50;
                    boolean endOnScreen = endScreen[0] >= -50 && endScreen[0] <= width + 50 &&
                                          endScreen[1] >= -50 && endScreen[1] <= height + 50;
                    if (startOnScreen || endOnScreen) {
                        // Check for azimuth wraparound
                        float azDiff = Math.abs((float)startAltAz[1] - (float)endAltAz[1]);
                        if (azDiff > 180) {
                            continue;
                        }

                        canvas.drawLine(startScreen[0], startScreen[1], endScreen[0], endScreen[1], constellationLinePaint);
                        linesDrawn++;
                    }
                }
            } else {
                // Fall back to index-based approach (legacy)
                for (int[] indices : lineIndices) {
                    if (indices.length >= 2) {
                        float[] start = starPositions.get(indices[0]);
                        float[] end = starPositions.get(indices[1]);

                        if (start != null && end != null) {
                            // Skip if either star is behind us
                            if (start[2] < 0.5f || end[2] < 0.5f) {
                                continue;
                            }

                            // Only draw if at least one endpoint is on screen
                            boolean startOnScreen = start[0] >= -50 && start[0] <= width + 50 &&
                                                    start[1] >= -50 && start[1] <= height + 50;
                            boolean endOnScreen = end[0] >= -50 && end[0] <= width + 50 &&
                                                  end[1] >= -50 && end[1] <= height + 50;
                            if (startOnScreen || endOnScreen) {
                                // Check for azimuth wraparound (stars on opposite sides of sky)
                                float azDiff = Math.abs(start[3] - end[3]);
                                if (azDiff > 180) {
                                    continue; // Skip lines that would wrap around
                                }

                                canvas.drawLine(start[0], start[1], end[0], end[1], constellationLinePaint);
                                linesDrawn++;
                            }
                        }
                    }
                }
            }

            // Draw constellation label at center if visible
            if (showConstellationLabels && constellation.hasCenterPoint()) {
                float centerRa = constellation.getCenterRa();
                float centerDec = constellation.getCenterDec();

                // Convert center to Alt/Az
                double[] altAz = raDecToAltAz(centerRa, centerDec, lst);
                double cAlt = altAz[0];
                double cAz = altAz[1];

                // Use proper spherical projection for label position
                float[] labelPos = projectToScreen(cAlt, cAz,
                        altitudeOffset, azimuthOffset,
                        centerX, centerY, pixelsPerDegree);

                // Only draw if visible and on screen
                if (labelPos[2] > 0.5f) {
                    float labelX = labelPos[0];
                    float labelY = labelPos[1];

                    if (labelX >= 0 && labelX <= width && labelY >= 0 && labelY <= height) {
                        canvas.drawText(constellation.getName(), labelX, labelY, constellationLabelPaint);
                        labelsDrawn++;
                    }
                }
            }
        }

        // Reduce log spam
        if (linesDrawn != lastDrawnLineCount) {
            Log.d(TAG, "CONSTELLATIONS: Drew " + linesDrawn + " lines, " + labelsDrawn + " labels");
            lastDrawnLineCount = linesDrawn;
        }
    }

    // Track last drawn count to reduce log spam
    private int lastDrawnLineCount = -1;

    /**
     * Draws the coordinate grid (Alt/Az lines) on the sky map.
     * Uses proper spherical projection to match star rendering.
     *
     * @param canvas Canvas to draw on
     * @param width  Canvas width
     * @param height Canvas height
     */
    private void drawGrid(Canvas canvas, int width, int height) {
        // Set grid colors based on night mode
        if (nightMode) {
            gridLinePaint.setColor(GRID_LINE_COLOR_NIGHT);
            gridLabelPaint.setColor(GRID_LABEL_COLOR_NIGHT);
        } else {
            gridLinePaint.setColor(GRID_LINE_COLOR);
            gridLabelPaint.setColor(GRID_LABEL_COLOR);
        }

        // Center of screen
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Pixels per degree
        float pixelsPerDegree = Math.min(width, height) / fieldOfView;

        // Draw altitude circles (every 15 degrees) using spherical projection
        // Full range from -90° (nadir) to +90° (zenith) for 360° rotation
        for (int alt = -90; alt <= 90; alt += 15) {
            Paint linePaint = gridLinePaint;
            if (alt == 0) {
                // Horizon gets a special stronger line
                linePaint = new Paint(gridLinePaint);
                linePaint.setStrokeWidth(2f);
                linePaint.setColor(nightMode ? Color.argb(120, 180, 100, 100) : Color.argb(120, 100, 200, 100));
            }

            // Draw altitude circle as a series of connected points
            float lastX = -1, lastY = -1;
            for (int az = 0; az <= 360; az += 5) {
                float[] pos = projectToScreen(alt, az, altitudeOffset, azimuthOffset,
                        centerX, centerY, pixelsPerDegree);
                if (pos[2] > 0.5f) {
                    float x = pos[0];
                    float y = pos[1];
                    if (lastX >= 0 && x >= -50 && x <= width + 50 && y >= -50 && y <= height + 50) {
                        // Check for azimuth wraparound
                        if (Math.abs(x - lastX) < width / 2) {
                            canvas.drawLine(lastX, lastY, x, y, linePaint);
                        }
                    }
                    lastX = x;
                    lastY = y;
                } else {
                    lastX = -1;
                    lastY = -1;
                }
            }

            // Draw altitude label at azimuth 0 (North) if visible
            float[] labelPos = projectToScreen(alt, 0, altitudeOffset, azimuthOffset,
                    centerX, centerY, pixelsPerDegree);
            if (labelPos[2] > 0.5f && labelPos[0] >= 0 && labelPos[0] <= width &&
                labelPos[1] >= 0 && labelPos[1] <= height) {
                String label = alt + "\u00b0";
                canvas.drawText(label, labelPos[0] + 4, labelPos[1] - 4, gridLabelPaint);
            }
        }

        // Draw azimuth lines (every 30 degrees) using spherical projection
        for (int az = 0; az < 360; az += 30) {
            // Draw azimuth line as a series of connected points from nadir to zenith
            float lastX = -1, lastY = -1;
            for (int alt = -90; alt <= 90; alt += 5) {
                float[] pos = projectToScreen(alt, az, altitudeOffset, azimuthOffset,
                        centerX, centerY, pixelsPerDegree);
                if (pos[2] > 0.5f) {
                    float x = pos[0];
                    float y = pos[1];
                    if (lastX >= 0 && x >= -50 && x <= width + 50 && y >= -50 && y <= height + 50) {
                        canvas.drawLine(lastX, lastY, x, y, gridLinePaint);
                    }
                    lastX = x;
                    lastY = y;
                } else {
                    lastX = -1;
                    lastY = -1;
                }
            }

            // Draw azimuth label at horizon level if visible
            float[] labelPos = projectToScreen(5, az, altitudeOffset, azimuthOffset,
                    centerX, centerY, pixelsPerDegree);
            if (labelPos[2] > 0.5f && labelPos[0] >= 0 && labelPos[0] <= width &&
                labelPos[1] >= 0 && labelPos[1] <= height) {
                String label;
                if (az == 0) label = "N";
                else if (az == 90) label = "E";
                else if (az == 180) label = "S";
                else if (az == 270) label = "W";
                else label = az + "\u00b0";
                canvas.drawText(label, labelPos[0] + 4, labelPos[1] + 16, gridLabelPaint);
            }
        }

        // Draw zenith marker if visible (altitude = +90°)
        float[] zenithPos = projectToScreen(90, 0, altitudeOffset, azimuthOffset,
                centerX, centerY, pixelsPerDegree);
        if (zenithPos[2] > 0.5f && zenithPos[0] >= 0 && zenithPos[0] <= width &&
            zenithPos[1] >= 0 && zenithPos[1] <= height) {
            Paint zenithPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            zenithPaint.setColor(nightMode ? Color.argb(150, 200, 150, 150) : Color.argb(150, 200, 200, 255));
            zenithPaint.setStyle(Paint.Style.STROKE);
            zenithPaint.setStrokeWidth(2f);
            canvas.drawCircle(zenithPos[0], zenithPos[1], 15f, zenithPaint);
            canvas.drawText("Zenith", zenithPos[0] + 20, zenithPos[1], gridLabelPaint);
        }

        // Draw nadir marker if visible (altitude = -90°, below horizon)
        float[] nadirPos = projectToScreen(-90, 0, altitudeOffset, azimuthOffset,
                centerX, centerY, pixelsPerDegree);
        if (nadirPos[2] > 0.5f && nadirPos[0] >= 0 && nadirPos[0] <= width &&
            nadirPos[1] >= 0 && nadirPos[1] <= height) {
            Paint nadirPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            nadirPaint.setColor(nightMode ? Color.argb(150, 150, 100, 100) : Color.argb(150, 150, 150, 200));
            nadirPaint.setStyle(Paint.Style.STROKE);
            nadirPaint.setStrokeWidth(2f);
            canvas.drawCircle(nadirPos[0], nadirPos[1], 15f, nadirPaint);
            canvas.drawText("Nadir", nadirPos[0] + 20, nadirPos[1], gridLabelPaint);
        }
    }

    /**
     * Finds a star for constellation rendering, with fallback strategies.
     *
     * @param starId The star ID to look up
     * @return StarData if found, null otherwise
     */
    private StarData findStarForConstellation(String starId) {
        // Try direct lookup first
        StarData star = starLookupMap.get(starId);
        if (star != null) {
            return star;
        }

        // Try lowercase lookup
        star = starLookupMap.get(starId.toLowerCase());
        if (star != null) {
            return star;
        }

        // For coordinate-based IDs (cstar_RA_Dec format), extract coordinates and find nearest
        if (starId.startsWith("cstar_")) {
            try {
                String[] parts = starId.substring(6).split("_");
                if (parts.length >= 2) {
                    float ra = Float.parseFloat(parts[0]) / 1000f;
                    float dec = Float.parseFloat(parts[1]) / 1000f;

                    // Find nearest star within 5 degrees (increased tolerance for constellation matching)
                    return findNearestStarByCoords(ra, dec, 5.0f);
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }

        return null;
    }

    /**
     * Finds the nearest star to the given celestial coordinates within a radius.
     *
     * @param ra     Target RA in degrees
     * @param dec    Target Dec in degrees
     * @param radius Maximum angular distance in degrees
     * @return Nearest StarData if within radius, null otherwise
     */
    private StarData findNearestStarByCoords(float ra, float dec, float radius) {
        StarData nearest = null;
        float nearestDist = radius * radius;  // Use squared distance for comparison

        for (StarData star : realStarData) {
            float dRa = star.getRa() - ra;
            float dDec = star.getDec() - dec;

            // Handle RA wraparound
            if (dRa > 180) dRa -= 360;
            if (dRa < -180) dRa += 360;

            float distSq = dRa * dRa + dDec * dDec;
            if (distSq < nearestDist) {
                nearestDist = distSq;
                nearest = star;
            }
        }

        return nearest;
    }

    /**
     * Draws a crosshair and reticle circle at the center of the screen.
     * The reticle circle indicates the selection area for objects.
     */
    private void drawCrosshair(Canvas canvas, int width, int height) {
        float centerX = width / 2f;
        float centerY = height / 2f;
        float crosshairSize = 30f;

        Paint crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crosshairPaint.setColor(Color.argb(150, 255, 255, 255));
        crosshairPaint.setStrokeWidth(2f);
        crosshairPaint.setStyle(Paint.Style.STROKE);

        // Draw crosshair lines
        canvas.drawLine(centerX - crosshairSize, centerY, centerX + crosshairSize, centerY, crosshairPaint);
        canvas.drawLine(centerX, centerY - crosshairSize, centerX, centerY + crosshairSize, crosshairPaint);

        // Draw small circle in center
        canvas.drawCircle(centerX, centerY, 5f, crosshairPaint);

        // Draw reticle circle for selection area
        reticlePaint.setColor(nightMode ? RETICLE_COLOR_NIGHT : RETICLE_COLOR);
        canvas.drawCircle(centerX, centerY, reticleRadiusPx, reticlePaint);
    }

    /**
     * Sets the visibility of planets in the sky view.
     *
     * @param visible true to show planets, false to hide
     */
    public void setPlanetsVisible(boolean visible) {
        this.showPlanets = visible;
        Log.d(TAG, "Planets visibility set to: " + visible);
        invalidate();
    }

    /**
     * Returns whether planets are currently visible.
     *
     * @return true if planets are shown
     */
    public boolean isPlanetsVisible() {
        return showPlanets;
    }

    /**
     * Adds or updates a planet in the view.
     *
     * @param name  Planet name (e.g., "Mars", "Jupiter")
     * @param ra    Right Ascension in degrees
     * @param dec   Declination in degrees
     * @param color ARGB color value
     * @param size  Size in pixels
     */
    public void setPlanet(String name, float ra, float dec, int color, float size) {
        planetData.put(name, new float[]{ra, dec, Float.intBitsToFloat(color), size});
        if (showPlanets) {
            invalidate();
        }
    }

    /**
     * Clears all planet data.
     */
    public void clearPlanets() {
        planetData.clear();
        invalidate();
    }

    public void setNightMode(boolean enabled) {
        this.nightMode = enabled;
        invalidate();
    }

    public void setFieldOfView(float fov) {
        this.fieldOfView = fov;
        updateStarPositions();
    }

    public void setStars(List<float[]> newStars) {
        stars.clear();
        stars.addAll(newStars);
        invalidate();
    }

    public void setLines(List<float[]> newLines) {
        lines.clear();
        lines.addAll(newLines);
        invalidate();
    }

    public void setLabels(List<Object[]> newLabels) {
        labels.clear();
        labels.addAll(newLabels);
        invalidate();
    }

    /**
     * Enables or disables simple star map mode.
     * Simple mode shows all stars using RA/Dec directly mapped to screen coordinates.
     * Complex mode uses Alt/Az projection based on observer location and time.
     *
     * @param enabled true for simple mode (guaranteed visible stars), false for complex projection
     */
    public void setSimpleStarMapMode(boolean enabled) {
        this.useSimpleStarMap = enabled;
        if (!enabled) {
            updateStarPositions();
        }
        invalidate();
    }

    /**
     * Returns whether simple star map mode is enabled.
     *
     * @return true if simple mode is active
     */
    public boolean isSimpleStarMapMode() {
        return useSimpleStarMap;
    }

    /**
     * Returns the number of stars currently loaded.
     *
     * @return count of stars in the star data list
     */
    public int getStarCount() {
        return realStarData != null ? realStarData.size() : 0;
    }

    /**
     * Sets the listener for star selection events.
     *
     * @param listener The listener to notify when a star is tapped
     */

    /**
     * Sets the listener for non-star object taps (planets, constellations).
     */
    public void setOnObjectSelectedListener(OnObjectSelectedListener listener) {
        this.objectSelectedListener = listener;
    }

    /**
     * Sets the reticle radius in pixels.
     *
     * @param radiusPx Reticle radius in pixels
     */
    public void setReticleRadius(float radiusPx) {
        this.reticleRadiusPx = radiusPx;
        invalidate();
    }

    /**
     * Gets the reticle radius in pixels.
     *
     * @return Reticle radius in pixels
     */
    public float getReticleRadius() {
        return reticleRadiusPx;
    }

    /**
     * Data class representing a selectable object (star or planet) in the reticle.
     */
    public static class SelectableObject {
        public final String id;
        public final String name;
        public final String type;  // "star" or "planet"
        public final float magnitude;
        public final float ra;
        public final float dec;

        public SelectableObject(String id, String name, String type, float magnitude, float ra, float dec) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.magnitude = magnitude;
            this.ra = ra;
            this.dec = dec;
        }

        /**
         * Returns a display name for the object.
         */
        public String getDisplayName() {
            if (name != null && !name.isEmpty() && !name.equals("null")) {
                return name;
            }
            return id;
        }
    }

    /**
     * Gets all celestial objects (stars and planets) within the center reticle.
     *
     * @return List of SelectableObject within the reticle area
     */
    public List<SelectableObject> getObjectsInReticle() {
        List<SelectableObject> objects = new ArrayList<>();

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return objects;

        float centerX = width / 2f;
        float centerY = height / 2f;

        // Calculate LST for coordinate conversion
        double lst = calculateLocalSiderealTime();

        // Pixels per degree
        float pixelsPerDegree = Math.min(width, height) / fieldOfView;

        // Check stars
        for (StarData star : realStarData) {
            float ra = star.getRa();
            float dec = star.getDec();

            // Convert RA/Dec to Alt/Az
            double[] altAz = raDecToAltAz(ra, dec, lst);
            double starAlt = altAz[0];
            double starAz = altAz[1];

            // Use proper spherical projection
            float[] screenPos = projectToScreen(starAlt, starAz,
                    altitudeOffset, azimuthOffset,
                    centerX, centerY, pixelsPerDegree);

            // Skip if not visible (behind us)
            if (screenPos[2] < 0.5f) {
                continue;
            }

            float x = screenPos[0];
            float y = screenPos[1];

            // Check if within reticle
            float distFromCenter = (float) Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
            if (distFromCenter <= reticleRadiusPx) {
                objects.add(new SelectableObject(
                        star.getId(),
                        star.getName(),
                        "star",
                        star.getMagnitude(),
                        star.getRa(),
                        star.getDec()
                ));
            }
        }

        // Check planets if visible
        if (showPlanets && !planetData.isEmpty()) {
            for (java.util.Map.Entry<String, float[]> entry : planetData.entrySet()) {
                String name = entry.getKey();
                float[] data = entry.getValue();
                float ra = data[0];
                float dec = data[1];

                // Convert RA/Dec to Alt/Az
                double[] altAz = raDecToAltAz(ra, dec, lst);
                double planetAlt = altAz[0];
                double planetAz = altAz[1];

                // Use proper spherical projection
                float[] screenPos = projectToScreen(planetAlt, planetAz,
                        altitudeOffset, azimuthOffset,
                        centerX, centerY, pixelsPerDegree);

                // Skip if not visible (behind us)
                if (screenPos[2] < 0.5f) {
                    continue;
                }

                float x = screenPos[0];
                float y = screenPos[1];

                // Check if within reticle
                float distFromCenter = (float) Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                if (distFromCenter <= reticleRadiusPx) {
                    objects.add(new SelectableObject(
                            "planet_" + name.toLowerCase(),
                            name,
                            "planet",
                            -2.0f,  // Planets are generally very bright
                            ra,
                            dec
                    ));
                }
            }
        }

        // Check constellations if visible
        if (showConstellations && constellations != null && !constellations.isEmpty()) {
            for (ConstellationData constellation : constellations) {
                if (!constellation.hasCenterPoint()) {
                    continue;
                }
                float ra = constellation.getCenterRa();
                float dec = constellation.getCenterDec();

                double[] altAz = raDecToAltAz(ra, dec, lst);
                double constellationAlt = altAz[0];
                double constellationAz = altAz[1];

                float[] screenPos = projectToScreen(constellationAlt, constellationAz,
                        altitudeOffset, azimuthOffset,
                        centerX, centerY, pixelsPerDegree);

                if (screenPos[2] < 0.5f) {
                    continue;
                }

                float x = screenPos[0];
                float y = screenPos[1];
                float distFromCenter = (float) Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                if (distFromCenter <= reticleRadiusPx) {
                    objects.add(new SelectableObject(
                            constellation.getId(),
                            constellation.getName(),
                            "constellation",
                            0.0f,
                            ra,
                            dec
                    ));
                }
            }
        }

        // Sort by magnitude (brightest first for stars, planets first overall)
        objects.sort((a, b) -> {
            // Planets come first
            if (!a.type.equals(b.type)) {
                return a.type.equals("planet") ? -1 : 1;
            }
            // Then sort by magnitude (lower = brighter = first)
            return Float.compare(a.magnitude, b.magnitude);
        });

        return objects;
    }

    /**
     * Checks if there are any objects within the center reticle.
     *
     * @return true if at least one object is in the reticle
     */
    public boolean hasObjectsInReticle() {
        return !getObjectsInReticle().isEmpty();
    }

    /**
     * Sets the highlighted star for selection preview.
     * The highlighted star will be drawn in red.
     *
     * @param star The star to highlight, or null to clear highlight
     */
    public void setHighlightedStar(StarData star) {
        this.highlightedStar = star;
        this.highlightedPlanetName = null;  // Clear planet highlight
        invalidate();
    }

    /**
     * Sets the highlighted planet for selection preview.
     * The highlighted planet will be drawn in red.
     *
     * @param planetName The planet name to highlight, or null to clear highlight
     */
    public void setHighlightedPlanet(String planetName) {
        this.highlightedPlanetName = planetName;
        this.highlightedStar = null;  // Clear star highlight
        invalidate();
    }

    /**
     * Clears all object highlights.
     */
    public void clearHighlight() {
        this.highlightedStar = null;
        this.highlightedPlanetName = null;
        invalidate();
    }

    public void setSearchModeActive(boolean active) {
        this.searchModeActive = active;
    }

    public void setOnSkyTapListener(@Nullable OnSkyTapListener listener) {
        this.skyTapListener = listener;
    }

    /**
     * Gets the currently highlighted star, if any.
     *
     * @return The highlighted StarData, or null if none
     */
    public StarData getHighlightedStar() {
        return highlightedStar;
    }

    /**
     * Gets the currently highlighted planet name, if any.
     *
     * @return The highlighted planet name, or null if none
     */
    public String getHighlightedPlanetName() {
        return highlightedPlanetName;
    }

    /**
     * Gets a StarData object by its ID.
     *
     * @param starId The star ID to look up
     * @return The StarData, or null if not found
     */
    public StarData getStarById(String starId) {
        if (starId == null) return null;
        return starLookupMap.get(starId);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scaleGestureDetector != null) scaleGestureDetector.onTouchEvent(event);
        if (gestureDetector != null) gestureDetector.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_UP && !isPinching) {
            float touchX = event.getX();
            float touchY = event.getY();

            if (searchModeActive) {
                if (skyTapListener != null) {
                    skyTapListener.onSkyTap(touchX, touchY);
                }
                return true;
            }

            // Find nearest star within tap radius
            StarData nearestStar = findNearestStar(touchX, touchY, 120f);
            if (nearestStar != null && starSelectedListener != null) {
                starSelectedListener.onStarSelected(nearestStar);
                return true;
            }

            SelectableObject nearestPlanet = findNearestPlanet(touchX, touchY, 60f);
            if (nearestPlanet != null && objectSelectedListener != null) {
                objectSelectedListener.onObjectSelected(nearestPlanet);
                return true;
            }

            SelectableObject nearestConstellation = findNearestConstellation(touchX, touchY, 80f);
            if (nearestConstellation != null && objectSelectedListener != null) {
                objectSelectedListener.onObjectSelected(nearestConstellation);
                return true;
            }
        }
        return true;
    }

    @Nullable
    private SelectableObject findNearestPlanet(float touchX, float touchY, float maxDistance) {
        if (!showPlanets || planetData == null || planetData.isEmpty()) return null;

        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float pixelsPerDegree = Math.min(width, height) / fieldOfView;
        double lst = calculateLocalSiderealTime();

        SelectableObject nearest = null;
        float minDist = maxDistance;

        for (java.util.Map.Entry<String, float[]> entry : planetData.entrySet()) {
            String name = entry.getKey();
            float[] data = entry.getValue();
            float ra = data[0];
            float dec = data[1];

            double[] altAz = raDecToAltAz(ra, dec, lst);
            double alt = altAz[0];
            double az = altAz[1];

            float[] screenPos = projectToScreen(alt, az,
                    altitudeOffset, azimuthOffset,
                    centerX, centerY, pixelsPerDegree);

            if (screenPos[2] < 0.5f) {
                continue;
            }

            float x = screenPos[0];
            float y = screenPos[1];
            float dist = (float) Math.sqrt(Math.pow(x - touchX, 2) + Math.pow(y - touchY, 2));
            if (dist <= minDist) {
                minDist = dist;
                nearest = new SelectableObject(
                        "planet_" + name.toLowerCase(),
                        name,
                        "planet",
                        -2.0f,
                        ra,
                        dec
                );
            }
        }

        return nearest;
    }

    @Nullable
    private SelectableObject findNearestConstellation(float touchX, float touchY, float maxDistance) {
        if (!showConstellations || constellations == null || constellations.isEmpty()) return null;

        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float pixelsPerDegree = Math.min(width, height) / fieldOfView;
        double lst = calculateLocalSiderealTime();

        SelectableObject nearest = null;
        float minDist = maxDistance;

        for (ConstellationData constellation : constellations) {
            if (!constellation.hasCenterPoint()) {
                continue;
            }
            float ra = constellation.getCenterRa();
            float dec = constellation.getCenterDec();

            double[] altAz = raDecToAltAz(ra, dec, lst);
            double alt = altAz[0];
            double az = altAz[1];

            float[] screenPos = projectToScreen(alt, az,
                    altitudeOffset, azimuthOffset,
                    centerX, centerY, pixelsPerDegree);

            if (screenPos[2] < 0.5f) {
                continue;
            }

            float x = screenPos[0];
            float y = screenPos[1];
            float dist = (float) Math.sqrt(Math.pow(x - touchX, 2) + Math.pow(y - touchY, 2));
            if (dist <= minDist) {
                minDist = dist;
                nearest = new SelectableObject(
                        constellation.getId(),
                        constellation.getName(),
                        "constellation",
                        0.0f,
                        ra,
                        dec
                );
            }
        }

        return nearest;
    }

    /**
     * Finds the nearest star to the given touch coordinates.
     * Uses proper spherical (gnomonic) projection matching drawSimpleStarMap.
     *
     * @param touchX The x coordinate of the touch
     * @param touchY The y coordinate of the touch
     * @param maxDistance The maximum distance in pixels to consider
     * @return The nearest star within maxDistance, or null if none found
     */
    private StarData findNearestStar(float touchX, float touchY, float maxDistance) {
        if (realStarData == null) return null;

        int width = getWidth();
        int height = getHeight();
        StarData nearest = null;
        float minDist = maxDistance;

        // Calculate LST for coordinate conversion (same as drawSimpleStarMap)
        double lst = calculateLocalSiderealTime();

        // Center of screen
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Pixels per degree
        float pixelsPerDegree = Math.min(width, height) / fieldOfView;

        // Prefer highlighted star if it is within tap radius
        if (highlightedStar != null) {
            float ra = highlightedStar.getRa();
            float dec = highlightedStar.getDec();
            double[] altAz = raDecToAltAz(ra, dec, lst);
            double starAlt = altAz[0];
            double starAz = altAz[1];
            if (starAlt >= -5) {
                float[] screenPos = projectToScreen(starAlt, starAz,
                        altitudeOffset, azimuthOffset,
                        centerX, centerY, pixelsPerDegree);
                if (screenPos[2] >= 0.5f) {
                    float x = screenPos[0];
                    float y = screenPos[1];
                    float dist = (float) Math.sqrt(Math.pow(touchX - x, 2) + Math.pow(touchY - y, 2));
                    if (dist <= maxDistance) {
                        return highlightedStar;
                    }
                }
            }
        }

        for (StarData star : realStarData) {
            float ra = star.getRa();
            float dec = star.getDec();

            // Convert RA/Dec to Alt/Az (same as drawSimpleStarMap)
            double[] altAz = raDecToAltAz(ra, dec, lst);
            double starAlt = altAz[0];
            double starAz = altAz[1];

            // Skip stars below horizon
            if (starAlt < -5) continue;

            // Use proper spherical projection (same as drawSimpleStarMap)
            float[] screenPos = projectToScreen(starAlt, starAz,
                    altitudeOffset, azimuthOffset,
                    centerX, centerY, pixelsPerDegree);

            // Skip if not visible (behind us)
            if (screenPos[2] < 0.5f) {
                continue;
            }

            float x = screenPos[0];
            float y = screenPos[1];

            float dist = (float) Math.sqrt(Math.pow(touchX - x, 2) + Math.pow(touchY - y, 2));
            if (dist < minDist) {
                minDist = dist;
                nearest = star;
            }
        }
        return nearest;
    }
}
