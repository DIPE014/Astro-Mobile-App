package com.astro.app.core.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.astro.app.data.model.ConstellationData;
import com.astro.app.data.model.StarData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

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
        /**
 * Handle a star being selected by the user in the sky view.
 *
 * @param star the StarData representing the selected star
 */
void onStarSelected(StarData star);
    }

    private OnStarSelectedListener starSelectedListener;

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
    private long observationTime = System.currentTimeMillis(); // Time for sky calculations

    // Star data from repository
    private List<StarData> realStarData = new CopyOnWriteArrayList<>();

    // Planet data: name -> {ra, dec, color, size}
    private final java.util.Map<String, float[]> planetData = new HashMap<>();
    private boolean showPlanets = false;
    private Paint planetPaint;
    private Paint planetLabelPaint;

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

    // Rendered elements (computed from star data)
    private List<float[]> stars = new CopyOnWriteArrayList<>();  // x, y, size, color
    private List<float[]> lines = new CopyOnWriteArrayList<>();  // x1, y1, x2, y2, color
    private List<Object[]> labels = new CopyOnWriteArrayList<>(); // x, y, text

    // Use simple star map mode (guaranteed to show stars)
    private boolean useSimpleStarMap = true;

    /**
     * Creates a SkyCanvasView for the provided Context and initializes rendering state.
     *
     * @param context the Context the view is running in
     */
    public SkyCanvasView(Context context) {
        super(context);
        init();
    }

    /**
     * Create a SkyCanvasView configured for use in layouts and initialize its rendering state.
     *
     * @param context the Context the view is running in, used to access resources and services
     * @param attrs   the attributes from the XML tag that is inflating the view, or null
     */
    public SkyCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initialize view interaction flags and configure Paint objects used for rendering.
     *
     * Sets the view to be clickable and focusable, and creates/configures the Paint
     * instances used for stars, lines, labels, background, planets, constellation
     * lines/labels, and grid drawing.
     */
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
    }

    /**
     * Replace the current star dataset with the provided list and refresh the view's rendering state.
     *
     * If `starList` is non-null the internal star collection is replaced and the constellation lookup is rebuilt;
     * if `starList` is null the existing star data is cleared. The method then either recomputes star positions
     * for the projection-based renderer or triggers a redraw for the simple star map mode.
     *
     * @param starList the new list of StarData objects to use for rendering, or `null` to clear existing stars
     */
    public void setStarData(List<StarData> starList) {
        this.realStarData.clear();
        if (starList != null) {
            this.realStarData.addAll(starList);
            // Build star lookup map for constellation line rendering
            buildStarLookupMap(starList);
        }
        Log.d(TAG, "STARS: Received " + realStarData.size() + " stars from repository");
        if (useSimpleStarMap) {
            // Simple mode - just invalidate to trigger onDraw which renders directly
            invalidate();
        } else {
            updateStarPositions();
        }
    }

    /**
     * Create a lookup mapping from star identifiers and lowercase names to their StarData for constellation rendering.
     *
     * Clears the existing map, adds entries for non-null IDs, and adds entries for non-empty names using their
     * lowercase form as a fallback key.
     *
     * @param starList the list of StarData objects to index; null entries are ignored
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
     * Replace the view's constellation list used for drawing constellation lines and labels.
     *
     * If `constellationList` is null the existing list is cleared. After updating the data the view
     * is invalidated to trigger a redraw.
     *
     * @param constellationList the new list of ConstellationData to render, or null to clear existing constellations
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
     * Indicates whether constellation lines and labels are currently enabled for rendering.
     *
     * @return `true` if constellation rendering is enabled, `false` otherwise.
     */
    public boolean isConstellationsVisible() {
        return showConstellations;
    }

    /**
     * Toggle the visibility of the sky coordinate grid and request a redraw.
     *
     * @param visible true to show the grid, false to hide it
     */
    public void setGridVisible(boolean visible) {
        this.showGrid = visible;
        Log.d(TAG, "GRID: Visibility set to " + visible);
        invalidate();
    }

    /**
     * Determines if the coordinate grid is currently visible.
     *
     * @return `true` if the coordinate grid is visible, `false` otherwise.
     */
    public boolean isGridVisible() {
        return showGrid;
    }

    /**
     * Update the observer's geographic location used for converting celestial coordinates to screen positions.
     *
     * @param latitude  observer latitude in degrees, positive north, valid range -90 to 90
     * @param longitude observer longitude in degrees, positive east, valid range -180 to 180
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
        this.azimuthOffset = azimuth;
        this.altitudeOffset = altitude;
        // Always invalidate to trigger redraw with new orientation
        invalidate();
    }

    /**
     * Approximate right ascension of the view center derived from local sidereal time and the azimuth offset.
     *
     * @return the view right ascension in degrees, normalized to the range [0, 360)
     */
    public float getViewRa() {
        // Approximate: convert azimuth to RA using LST
        // RA = LST - HA, where HA is related to azimuth
        double lst = calculateLocalSiderealTime();
        // Simplified: azimuth 0 (North) roughly corresponds to objects at meridian
        float ra = (float) ((lst - azimuthOffset + 360) % 360);
        return ra;
    }

    /**
     * Approximate declination of the view center in degrees.
     *
     * <p>This is an approximation and currently uses the view's altitude offset as the declination value.
     *
     * @return the approximate declination (degrees) of the view center
     */
    public float getViewDec() {
        // Simplified approximation for view declination
        // When looking at altitude 90 (zenith), Dec = latitude
        // When looking at horizon (alt 0), Dec varies with azimuth
        return altitudeOffset;
    }

    /**
     * Current view azimuth of the sky canvas.
     *
     * @return Azimuth in degrees where 0 = North.
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
     * Update the observation time used for sky calculations and request a redraw.
     *
     * This changes the internal time used for Local Sidereal Time and projection computations
     * and invalidates the view so the sky is re-rendered for the new time.
     *
     * @param timeMillis time in milliseconds since the Unix epoch (UTC)
     */
    public void setTime(long timeMillis) {
        this.observationTime = timeMillis;
        Log.d(TAG, "TIME_TRAVEL: Time set to " + new java.util.Date(timeMillis));
        invalidate();
    }

    /**
     * Recomputes positions and visual attributes of rendered stars for the current observer, time, and view orientation.
     *
     * <p>Clears previously computed star and label buffers, then:
     * - if no star catalog is present, inserts demo stars and requests a redraw;
     * - otherwise computes each star's altitude/azimuth and, if visible (above the horizon and inside the field of view),
     *   computes screen coordinates, display size (from magnitude), and color, and appends entries to the internal
     *   star and label lists. Bright stars (magnitude &lt; 2.0) get a name label placed slightly above the star.
     *
     * <p>The method logs the number of rendered stars and invalidates the view to trigger a redraw.
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

            stars.add(new float[]{screenPos[0], screenPos[1], size, color});

            // Add label for bright stars (magnitude < 2)
            if (star.getMagnitude() < 2.0f && star.getName() != null) {
                labels.add(new Object[]{screenPos[0], screenPos[1] - 0.02f, star.getName()});
            }
        }

        Log.d(TAG, "Rendered " + stars.size() + " visible stars");
        invalidate();
    }

    /**
     * Compute the local sidereal time for the view's current observation time and longitude.
     *
     * Uses the view's observationTime (UTC) and observerLongitude to calculate LST.
     *
     * @return Local sidereal time in degrees within [0, 360).
     */
    private double calculateLocalSiderealTime() {
        // Use observation time (set by time travel) instead of current system time
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(observationTime);

        // Julian Date calculation
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);

        double ut = hour + minute / 60.0 + second / 3600.0;

        if (month <= 2) {
            year -= 1;
            month += 12;
        }

        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        double jd = Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;

        // Days since J2000.0
        double d = jd - 2451545.0;

        // Greenwich Sidereal Time
        double gst = 280.46061837 + 360.98564736629 * d + 0.000387933 * Math.pow(d / 36525.0, 2);
        gst = gst % 360;
        if (gst < 0) gst += 360;

        // Local Sidereal Time
        double lst = gst + observerLongitude;
        lst = lst % 360;
        if (lst < 0) lst += 360;

        // Log LST for time travel debugging (only log occasionally to avoid spam)
        if (lastLoggedLst < 0 || Math.abs(lst - lastLoggedLst) > 1.0) {
            Log.d(TAG, "TIME_TRAVEL: LST = " + String.format("%.2f", lst) + "° for time " +
                new java.util.Date(observationTime));
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
         * Convert an altitude/azimuth direction to normalized screen coordinates using the view's orientation and field of view.
         *
         * @param altitude Altitude in degrees (angle above the horizon).
         * @param azimuth  Azimuth in degrees.
         * @return         A two-element float array [x, y] with coordinates normalized to the range [0, 1], or `null` if the direction is behind the viewer or outside the current field of view.
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
     * Compute the display size in pixels for a star based on its apparent magnitude.
     *
     * @param magnitude the star's apparent magnitude (smaller values are brighter)
     * @return the pixel size to use when drawing the star; larger for brighter stars, clamped to the range 1–10
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

    /**
     * Render the sky view into the provided Canvas using the current state, mode, and data.
     *
     * When simple star map mode is enabled and real star data is available this draws, in order:
     * background, optional grid, optional constellations, simple star map, optional planets, then center crosshair.
     * Otherwise this draws: background, projected constellation lines, projected stars, labels, then center crosshair.
     *
     * @param canvas the Canvas to draw the view onto
     */
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
                starPaint.setColor((int) star[3]);
                if (nightMode) {
                    starPaint.setColor(Color.rgb(255, 100, 100)); // Red tint
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
         * Renders the full-sky star layer centered on the device pointing using each star's RA/Dec.
         *
         * Converts star RA/Dec to altitude/azimuth using the current observation time and observer
         * location, projects those coordinates into screen space with a spherical (gnomonic-like)
         * projection around the view azimuth/altitude, and paints star markers and optional name
         * labels for bright stars. Off-screen and behind-view objects are skipped; rendering
         * respects the current field of view and nightMode coloring.
         *
         * @param canvas the Canvas to draw onto
         * @param width  the view width in pixels
         * @param height the view height in pixels
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

            if (nightMode) {
                starPaint.setColor(Color.rgb(255, 100, 100)); // Red tint for night mode
            } else {
                starPaint.setColor(color);
            }

            canvas.drawCircle(x, y, size, starPaint);
            starsDrawn++;

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
     * Renders visible planets onto the canvas using the view's current time, location, and orientation.
     *
     * For each planet in the internal planetData map this method:
     * - converts RA/Dec to altitude/azimuth using the current Local Sidereal Time,
     * - skips bodies below ~5° below the horizon,
     * - projects visible bodies to screen coordinates with the view projection,
     * - skips objects behind the viewer or well outside the viewport,
     * - draws a circular marker and a text label with colors chosen for night/day mode.
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

            // Skip planets below horizon
            if (planetAlt < -5) {
                continue;
            }

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

            // Draw planet point (larger than stars)
            if (nightMode) {
                planetPaint.setColor(Color.rgb(255, 100, 100)); // Red tint for night mode
            } else {
                planetPaint.setColor(color);
            }
            canvas.drawCircle(x, y, size, planetPaint);

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
            List<String> starIds = constellation.getStarIds();
            List<int[]> lineIndices = constellation.getLineIndices();

            // Build position map for this constellation's stars (screen coords)
            // Format: [x, y, visible, starAz] where visible is 1.0 if in front
            java.util.Map<Integer, float[]> starPositions = new HashMap<>();

            for (int i = 0; i < starIds.size(); i++) {
                String starId = starIds.get(i);
                StarData star = findStarForConstellation(starId);

                if (star != null) {
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

                    // Store screen position, visibility flag, and azimuth
                    starPositions.put(i, new float[]{screenPos[0], screenPos[1], screenPos[2], (float)starAz});
                }
            }

            // Draw lines between stars
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
         * Locate a StarData entry for a constellation reference, trying direct, case-insensitive,
         * and coordinate-based fallbacks.
         *
         * <p>If {@code starId} starts with {@code "cstar_"} the suffix is parsed as
         * {@code RA_Dec} in thousandths (e.g. {@code "cstar_12345_67890"} -> RA=12.345, Dec=67.890)
         * and the nearest catalog star within 1.0° is returned.
         *
         * @param starId the star identifier, which may be an exact ID, a name (case-insensitive),
         *               or a coordinate-based ID starting with {@code "cstar_"}
         * @return the matching {@code StarData} if found, `null` otherwise
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

                    // Find nearest star within 1 degree
                    return findNearestStarByCoords(ra, dec, 1.0f);
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }

        return null;
    }

    /**
         * Locate the nearest star within an angular radius around the specified RA/Dec.
         *
         * @param ra     target right ascension in degrees
         * @param dec    target declination in degrees
         * @param radius maximum angular distance in degrees
         * @return the nearest StarData within the radius, or `null` if none found
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
     * Draws a centered crosshair overlay on the provided canvas.
     *
     * @param canvas the Canvas to draw into
     * @param width  the view width in pixels (used to compute center)
     * @param height the view height in pixels (used to compute center)
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
     * Indicates whether planets are shown on the view.
     *
     * @return true if planets are shown, false otherwise.
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
     * Remove all stored planet entries and request a redraw of the view.
     *
     * Clears the internal planet data so no planets will be rendered and schedules the view to be redrawn.
     */
    public void clearPlanets() {
        planetData.clear();
        invalidate();
    }

    /**
     * Enable or disable night mode for the view's rendering.
     *
     * @param enabled true to enable the night-mode color scheme, false to use the day-mode color scheme
     */
    public void setNightMode(boolean enabled) {
        this.nightMode = enabled;
        invalidate();
    }

    /**
     * Set the view's field of view used for projecting sky coordinates and refresh star positions.
     *
     * @param fov horizontal field of view in degrees; larger values include more of the sky
     */
    public void setFieldOfView(float fov) {
        this.fieldOfView = fov;
        updateStarPositions();
    }

    /**
     * Replace the current rendered star list with the provided list and schedule a redraw.
     *
     * @param newStars list of star entries where each float[] is formatted as [x, y, size, color];
     *                 x and y are screen coordinates, size is the visual radius, and color is the
     *                 star color encoded as a float value used by the renderer.
     */
    public void setStars(List<float[]> newStars) {
        stars.clear();
        stars.addAll(newStars);
        invalidate();
    }

    /**
     * Replace the currently rendered line segments and schedule a redraw of the view.
     *
     * @param newLines a list of float arrays representing line segments; each array must contain
     *                 five elements in this order: [x1, y1, x2, y2, color].
     */
    public void setLines(List<float[]> newLines) {
        lines.clear();
        lines.addAll(newLines);
        invalidate();
    }

    /**
     * Replace the current rendered labels with the provided list and invalidate the view.
     *
     * @param newLabels list of label entries; each entry must be an Object[] containing
     *                  {Float x, Float y, String text} where x and y are label screen
     *                  coordinates (pixels) and text is the label to draw
     */
    public void setLabels(List<Object[]> newLabels) {
        labels.clear();
        labels.addAll(newLabels);
        invalidate();
    }

    /**
     * Toggle the simple star map rendering mode.
     *
     * When disabled, recomputes star positions using the complex Alt/Az projection for the current observer/time; when enabled, stars are rendered using RA/Dec mapping.
     *
     * @param enabled true to enable simple RA/Dec-based rendering, false to use Alt/Az projection
     */
    public void setSimpleStarMapMode(boolean enabled) {
        this.useSimpleStarMap = enabled;
        if (!enabled) {
            updateStarPositions();
        }
        invalidate();
    }

    /**
     * Indicates whether the view is currently using the simple star map rendering mode.
     *
     * @return `true` if the simple star map rendering mode is enabled, `false` otherwise.
     */
    public boolean isSimpleStarMapMode() {
        return useSimpleStarMap;
    }

    /**
     * Get the number of loaded stars.
     *
     * @return the number of stars currently loaded, or 0 if no star data is present
     */
    public int getStarCount() {
        return realStarData != null ? realStarData.size() : 0;
    }

    /**
     * Sets the listener for star selection events.
     *
     * @param listener The listener to notify when a star is tapped
     */
    public void setOnStarSelectedListener(OnStarSelectedListener listener) {
        this.starSelectedListener = listener;
    }

    /**
     * Handle touch events on the sky view and notify the registered listener when a star is tapped.
     *
     * <p>On ACTION_UP, searches for the nearest star within a 50-pixel radius of the touch point and,
     * if a star is found and a listener is registered, invokes {@code OnStarSelectedListener.onStarSelected}
     * with that star.</p>
     *
     * @param event the MotionEvent describing the touch
     * @return `true` indicating the touch event was consumed
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("TOUCH", "Touch at " + event.getX() + ", " + event.getY() + " action=" + event.getAction());

        if (event.getAction() == MotionEvent.ACTION_UP) {
            float touchX = event.getX();
            float touchY = event.getY();

            // Find nearest star within tap radius (50 pixels for easier selection)
            StarData nearestStar = findNearestStar(touchX, touchY, 50f);
            Log.d("TOUCH", "Nearest star: " + (nearestStar != null ? nearestStar.getName() : "null"));
            if (nearestStar != null && starSelectedListener != null) {
                Log.d("TOUCH", "Calling star selected listener for: " + nearestStar.getName());
                starSelectedListener.onStarSelected(nearestStar);
                return true;
            }
        }
        // Return true to indicate we handled the touch event
        return true;
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