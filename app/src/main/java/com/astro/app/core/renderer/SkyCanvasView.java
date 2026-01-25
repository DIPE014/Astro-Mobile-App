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

    public SkyCanvasView(Context context) {
        super(context);
        init();
    }

    public SkyCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
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
     * Sets the real star data from the repository.
     * This replaces any demo data and converts coordinates for display.
     *
     * @param starList List of StarData objects from the repository
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
        this.azimuthOffset = azimuth;
        this.altitudeOffset = altitude;
        updateStarPositions();
    }

    /**
     * Gets the current view RA (approximation based on azimuth and LST).
     *
     * @return Current view RA in degrees
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
     * Gets the current view Dec (approximation based on altitude and latitude).
     *
     * @return Current view Dec in degrees
     */
    public float getViewDec() {
        // Simplified approximation for view declination
        // When looking at altitude 90 (zenith), Dec = latitude
        // When looking at horizon (alt 0), Dec varies with azimuth
        return altitudeOffset;
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
     * Sets the time for sky calculations (for time travel feature).
     *
     * @param timeMillis time in milliseconds since epoch
     */
    public void setTime(long timeMillis) {
        this.observationTime = timeMillis;
        updateStarPositions();
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
     * Calculates the Local Sidereal Time for the observer's location.
     *
     * @return LST in degrees (0-360)
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
        double jd0 = jd - ut / 24.0;

        // Days since J2000.0
        double d = jd - 2451545.0;
        double d0 = jd0 - 2451545.0;

        // Greenwich Sidereal Time
        double gst = 280.46061837 + 360.98564736629 * d + 0.000387933 * Math.pow(d / 36525.0, 2);
        gst = gst % 360;
        if (gst < 0) gst += 360;

        // Local Sidereal Time
        double lst = gst + observerLongitude;
        lst = lst % 360;
        if (lst < 0) lst += 360;

        return lst;
    }

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
            Log.d(TAG, "STARS: Drawing " + realStarData.size() + " stars in simple map mode");
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
     * Draws stars using simple RA/Dec to screen coordinate mapping.
     * This guarantees all stars are visible on screen.
     *
     * RA (0-360 degrees) maps to X (0 to width)
     * Dec (-90 to +90 degrees) maps to Y (height to 0) - north is up
     */
    private void drawSimpleStarMap(Canvas canvas, int width, int height) {
        int starsDrawn = 0;

        for (StarData star : realStarData) {
            // Get RA and Dec
            float ra = star.getRa();    // 0-360 degrees (or 0-24 hours converted)
            float dec = star.getDec();  // -90 to +90 degrees

            // Debug logging for first few stars
            if (starsDrawn < 5) {
                Log.d("STARS", "Star: name=" + star.getName() + ", ra=" + ra + ", dec=" + dec + ", mag=" + star.getMagnitude());
            }

            // Normalize RA to 0-360 range
            if (ra < 0) ra += 360f;
            if (ra >= 360) ra -= 360f;

            // Simple projection: RA to X, Dec to Y
            float x = (ra / 360f) * width;
            // Flip so north (dec +90) is at top (y=0)
            float y = ((90f - dec) / 180f) * height;

            // Star size based on magnitude (brighter = lower magnitude = larger)
            float magnitude = star.getMagnitude();
            float size = Math.max(1f, 6f - magnitude);  // mag 1 = size 5, mag 5 = size 1

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
                // Show actual name if available, otherwise show formatted coordinates
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

        Log.d(TAG, "STARS: Drew " + starsDrawn + " stars on screen");
    }

    /**
     * Draws planets on the sky map.
     * Uses the same RA/Dec to screen coordinate mapping as stars.
     */
    private void drawPlanets(Canvas canvas, int width, int height) {
        if (planetData.isEmpty()) {
            return;
        }

        int planetsDrawn = 0;
        for (java.util.Map.Entry<String, float[]> entry : planetData.entrySet()) {
            String name = entry.getKey();
            float[] data = entry.getValue();
            float ra = data[0];
            float dec = data[1];
            int color = Float.floatToIntBits(data[2]);
            float size = data[3];

            // Normalize RA to 0-360 range
            if (ra < 0) ra += 360f;
            if (ra >= 360) ra -= 360f;

            // Simple projection: RA to X, Dec to Y (same as stars)
            float x = (ra / 360f) * width;
            float y = ((90f - dec) / 180f) * height;

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

        Log.d(TAG, "PLANETS: Drew " + planetsDrawn + " planets on screen");
    }

    /**
     * Draws constellation lines on the sky map.
     * Uses the same RA/Dec to screen coordinate mapping as stars.
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

        int linesDrawn = 0;
        int labelsDrawn = 0;

        for (ConstellationData constellation : constellations) {
            List<String> starIds = constellation.getStarIds();
            List<int[]> lineIndices = constellation.getLineIndices();

            // Build position map for this constellation's stars
            java.util.Map<Integer, float[]> starPositions = new HashMap<>();

            for (int i = 0; i < starIds.size(); i++) {
                String starId = starIds.get(i);
                StarData star = findStarForConstellation(starId);

                if (star != null) {
                    float ra = star.getRa();
                    float dec = star.getDec();

                    // Normalize RA
                    if (ra < 0) ra += 360f;
                    if (ra >= 360) ra -= 360f;

                    // Convert to screen coordinates (same as stars)
                    float x = (ra / 360f) * width;
                    float y = ((90f - dec) / 180f) * height;

                    starPositions.put(i, new float[]{x, y, ra, dec});
                }
            }

            // Draw lines between stars
            for (int[] indices : lineIndices) {
                if (indices.length >= 2) {
                    float[] start = starPositions.get(indices[0]);
                    float[] end = starPositions.get(indices[1]);

                    if (start != null && end != null) {
                        // Check for RA wraparound (line crosses 0/360 boundary)
                        float raDiff = Math.abs(start[2] - end[2]);
                        if (raDiff > 180) {
                            // Skip lines that would wrap around the screen
                            // (This is a simplified handling - could draw two segments instead)
                            continue;
                        }

                        canvas.drawLine(start[0], start[1], end[0], end[1], constellationLinePaint);
                        linesDrawn++;
                    }
                }
            }

            // Draw constellation label at center
            if (showConstellationLabels && constellation.hasCenterPoint()) {
                float centerRa = constellation.getCenterRa();
                float centerDec = constellation.getCenterDec();

                // Normalize RA
                if (centerRa < 0) centerRa += 360f;
                if (centerRa >= 360) centerRa -= 360f;

                float labelX = (centerRa / 360f) * width;
                float labelY = ((90f - centerDec) / 180f) * height;

                canvas.drawText(constellation.getName(), labelX, labelY, constellationLabelPaint);
                labelsDrawn++;
            }
        }

        Log.d(TAG, "CONSTELLATIONS: Drew " + linesDrawn + " lines, " + labelsDrawn + " labels");
    }

    /**
     * Draws the coordinate grid (RA/Dec lines) on the sky map.
     * Uses the same RA/Dec to screen coordinate mapping as stars.
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

        // Draw RA lines (vertical lines, every 15 degrees = 1 hour)
        for (int ra = 0; ra < 360; ra += 15) {
            float x = (ra / 360f) * width;
            canvas.drawLine(x, 0, x, height, gridLinePaint);

            // Draw RA label at top
            int raHours = ra / 15;
            String label = raHours + "h";
            canvas.drawText(label, x + 4, 20, gridLabelPaint);
        }

        // Draw Dec lines (horizontal lines, every 15 degrees)
        for (int dec = -75; dec <= 75; dec += 15) {
            float y = ((90f - dec) / 180f) * height;
            canvas.drawLine(0, y, width, y, gridLinePaint);

            // Draw Dec label at left
            String label = (dec >= 0 ? "+" : "") + dec + "\u00b0";
            canvas.drawText(label, 4, y - 4, gridLabelPaint);
        }

        // Draw celestial equator (Dec = 0) with slightly stronger line
        float equatorY = ((90f - 0) / 180f) * height;
        Paint equatorPaint = new Paint(gridLinePaint);
        equatorPaint.setStrokeWidth(1.5f);
        equatorPaint.setColor(nightMode ? Color.argb(100, 180, 100, 100) : Color.argb(100, 150, 255, 150));
        canvas.drawLine(0, equatorY, width, equatorY, equatorPaint);

        // Draw ecliptic approximation (dashed, at roughly 23.5 degrees tilt)
        // The ecliptic is the path the Sun appears to follow through the year
        Paint eclipticPaint = new Paint(gridLinePaint);
        eclipticPaint.setStrokeWidth(1.5f);
        eclipticPaint.setColor(nightMode ? Color.argb(80, 200, 150, 100) : Color.argb(80, 255, 200, 100));
        eclipticPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 10}, 0));

        // Draw ecliptic as a sine wave (simplified approximation)
        android.graphics.Path eclipticPath = new android.graphics.Path();
        float obliquity = 23.44f; // Earth's axial tilt in degrees
        boolean firstPoint = true;
        for (int ra = 0; ra <= 360; ra += 5) {
            // Simplified ecliptic calculation: Dec varies sinusoidally with RA
            float dec = (float) (obliquity * Math.sin(Math.toRadians(ra - 90)));
            float x = (ra / 360f) * width;
            float y = ((90f - dec) / 180f) * height;
            if (firstPoint) {
                eclipticPath.moveTo(x, y);
                firstPoint = false;
            } else {
                eclipticPath.lineTo(x, y);
            }
        }
        canvas.drawPath(eclipticPath, eclipticPaint);

        Log.d(TAG, "GRID: Drew coordinate grid");
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
     * Draws a crosshair at the center of the screen.
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
    public void setOnStarSelectedListener(OnStarSelectedListener listener) {
        this.starSelectedListener = listener;
    }

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

        for (StarData star : realStarData) {
            float ra = star.getRa();
            float dec = star.getDec();

            // Normalize RA to 0-360 range (same as in drawSimpleStarMap)
            if (ra < 0) ra += 360f;
            if (ra >= 360) ra -= 360f;

            float x = (ra / 360f) * width;
            float y = ((90f - dec) / 180f) * height;

            float dist = (float) Math.sqrt(Math.pow(touchX - x, 2) + Math.pow(touchY - y, 2));
            if (dist < minDist) {
                minDist = dist;
                nearest = star;
            }
        }
        return nearest;
    }
}
