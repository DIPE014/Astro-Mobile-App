package com.astro.app.core.layers;

import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.core.math.RaDec;
import com.astro.app.core.math.Vector3;
import com.astro.app.data.model.LabelPrimitive;
import com.astro.app.data.model.LinePrimitive;

/**
 * Layer for rendering the celestial coordinate grid.
 *
 * <p>This layer creates a grid of lines showing Right Ascension and Declination
 * coordinates. It includes:</p>
 * <ul>
 *   <li>RA lines (meridians) - vertical lines at fixed Right Ascension values</li>
 *   <li>Dec lines (parallels) - horizontal lines at fixed Declination values</li>
 *   <li>The celestial equator (Dec = 0)</li>
 *   <li>Optional ecliptic line</li>
 *   <li>Labels for hour marks and degree marks</li>
 * </ul>
 *
 * <h3>Grid Spacing:</h3>
 * <p>The spacing of grid lines is configurable. Default values are:</p>
 * <ul>
 *   <li>RA: 12 lines (every 2 hours / 30 degrees)</li>
 *   <li>Dec: 9 lines (every 10 degrees from equator)</li>
 * </ul>
 *
 * @see Layer
 * @see AbstractLayer
 */
public class GridLayer extends AbstractLayer {

    private static final String TAG = "GridLayer";

    /** Layer ID constant */
    public static final String LAYER_ID = "layer_grid";

    /** Layer name for display */
    public static final String LAYER_NAME = "Coordinate Grid";

    /** Depth order - grid renders behind everything */
    public static final int DEPTH_ORDER = 0;

    /** Default number of RA lines (12 = every 2 hours) */
    public static final int DEFAULT_RA_LINES = 12;

    /** Default number of Dec lines on each side of equator (9 = every 10 degrees) */
    public static final int DEFAULT_DEC_LINES = 9;

    /** Default color for grid lines (semi-transparent yellow-ish) */
    public static final int DEFAULT_LINE_COLOR = 0x14F8EFBC;

    /** Default color for equator line (brighter) */
    public static final int DEFAULT_EQUATOR_COLOR = 0x40F8EFBC;

    /** Default color for ecliptic line (orange-ish) */
    public static final int DEFAULT_ECLIPTIC_COLOR = 0x40FFB347;

    /** Default color for labels */
    public static final int DEFAULT_LABEL_COLOR = 0x80F8EFBC;

    /** Default line width */
    public static final float DEFAULT_LINE_WIDTH = 1.0f;

    /** Number of vertices for declination circles (every 10 degrees in RA) */
    private static final int NUM_RA_VERTICES = 36;

    /** Number of vertices for RA meridians (from pole to pole) */
    private static final int NUM_DEC_VERTICES = 19;

    /** Obliquity of the ecliptic (degrees) */
    private static final float OBLIQUITY = 23.439281f;

    /** Number of RA lines to draw */
    private int numRaLines;

    /** Number of Dec lines to draw (on each side of equator) */
    private int numDecLines;

    /** Color for normal grid lines */
    private int lineColor;

    /** Color for the celestial equator */
    private int equatorColor;

    /** Color for the ecliptic */
    private int eclipticColor;

    /** Color for grid labels */
    private int labelColor;

    /** Line width for grid lines */
    private float lineWidth;

    /** Whether to show the ecliptic line */
    private boolean showEcliptic;

    /** Whether to show labels */
    private boolean showLabels;

    /** Resources for accessing strings (optional) */
    @Nullable
    private Resources resources;

    /**
     * Creates a GridLayer with default settings.
     */
    public GridLayer() {
        this(DEFAULT_RA_LINES, DEFAULT_DEC_LINES);
    }

    /**
     * Creates a GridLayer with custom grid density.
     *
     * @param numRaLines  Number of RA lines (meridians) - 12 gives 2-hour spacing
     * @param numDecLines Number of Dec lines on each side of equator - 9 gives 10-degree spacing
     */
    public GridLayer(int numRaLines, int numDecLines) {
        this(numRaLines, numDecLines, null);
    }

    /**
     * Create a GridLayer configured with the given counts for RA meridians and declination parallels and optional localization resources.
     *
     * @param numRaLines  number of RA meridians to draw (evenly spaced around 360°)
     * @param numDecLines number of declination divisions between equator and pole (controls parallels per hemisphere)
     * @param resources   optional Android Resources used to localize label strings; may be null
     */
    public GridLayer(int numRaLines, int numDecLines, @Nullable Resources resources) {
        super(LAYER_ID, LAYER_NAME, DEPTH_ORDER);
        this.numRaLines = numRaLines;
        this.numDecLines = numDecLines;
        this.resources = resources;
        this.lineColor = DEFAULT_LINE_COLOR;
        this.equatorColor = DEFAULT_EQUATOR_COLOR;
        this.eclipticColor = DEFAULT_ECLIPTIC_COLOR;
        this.labelColor = DEFAULT_LABEL_COLOR;
        this.lineWidth = DEFAULT_LINE_WIDTH;
        this.showEcliptic = true;
        this.showLabels = true;
    }

    /**
     * Builds and populates the grid layer by constructing RA meridians, Declination parallels,
     * the celestial equator, optional ecliptic, and optional labels, then adding those primitives
     * to the layer.
     *
     * The constructed primitives reflect the layer's current configuration (number of RA/Dec
     * lines, colors, line width, and the showEcliptic/showLabels flags) and are appended to the
     * layer's internal collections of lines and labels.
     */
    @Override
    protected void initializeLayer() {
        Log.d(TAG, "Initializing grid layer with " + numRaLines + " RA lines and " +
                numDecLines + " Dec lines");

        // Create RA lines (meridians - from north to south pole)
        for (int i = 0; i < numRaLines; i++) {
            LinePrimitive raLine = createRaLine(i);
            addLine(raLine);
        }

        // Create Dec lines (parallels)
        // Start with the equator
        LinePrimitive equator = createDecLine(0.0f, equatorColor);
        addLine(equator);

        // Create lines above and below equator
        for (int i = 1; i < numDecLines; i++) {
            float dec = i * 90.0f / numDecLines;

            // Northern hemisphere
            LinePrimitive northLine = createDecLine(dec, lineColor);
            addLine(northLine);

            // Southern hemisphere
            LinePrimitive southLine = createDecLine(-dec, lineColor);
            addLine(southLine);

            // Add labels for Dec lines
            if (showLabels) {
                addLabel(new LabelPrimitive(0f, dec, formatDec(dec), labelColor));
                addLabel(new LabelPrimitive(0f, -dec, formatDec(-dec), labelColor));
            }
        }

        // Add pole labels
        if (showLabels) {
            addLabel(new LabelPrimitive(0f, 90f, getNorthPoleLabel(), labelColor));
            addLabel(new LabelPrimitive(0f, -90f, getSouthPoleLabel(), labelColor));

            // Add hour marks along the equator
            for (int i = 0; i < 12; i++) {
                float ra = i * 30.0f; // Every 2 hours (30 degrees)
                String label = formatRaHours(i * 2);
                addLabel(new LabelPrimitive(ra, 0.0f, label, labelColor));
            }
        }

        // Add ecliptic line if enabled
        if (showEcliptic) {
            LinePrimitive ecliptic = createEclipticLine();
            addLine(ecliptic);
        }

        Log.d(TAG, "Created " + lines.size() + " grid lines and " + labels.size() + " labels");
    }

    /**
     * Creates a Right Ascension line (meridian) from north to south pole.
     *
     * @param index Line index (0 to numRaLines-1)
     * @return A line primitive for the RA meridian
     */
    @NonNull
    private LinePrimitive createRaLine(int index) {
        LinePrimitive line = new LinePrimitive(lineColor, lineWidth);
        float ra = index * 360.0f / numRaLines;

        // Create vertices from north pole to south pole
        for (int i = 0; i < NUM_DEC_VERTICES; i++) {
            float dec = 90.0f - i * 180.0f / (NUM_DEC_VERTICES - 1);
            line.addVertex(ra, dec);
        }

        return line;
    }

    /**
     * Create a declination parallel as a closed RA circle at the specified declination.
     *
     * The returned line contains vertices covering RA 0°–360° (inclusive) forming a closed loop.
     *
     * @param dec   declination in degrees
     * @param color color to use for the line
     * @return      a LinePrimitive representing the closed declination parallel at {@code dec}
     */
    @NonNull
    private LinePrimitive createDecLine(float dec, int color) {
        LinePrimitive line = new LinePrimitive(color, lineWidth);

        // Create vertices around the full circle of RA
        for (int i = 0; i <= NUM_RA_VERTICES; i++) {
            float ra = i * 360.0f / NUM_RA_VERTICES;
            // Close the circle by repeating the first point
            if (i == NUM_RA_VERTICES) {
                ra = 0.0f;
            }
            line.addVertex(ra, dec);
        }

        return line;
    }

    /**
         * Constructs a LinePrimitive that traces the ecliptic in right ascension and declination.
         *
         * The ecliptic is the Sun's apparent annual path on the celestial sphere, tilted relative
         * to the celestial equator by the obliquity angle.
         *
         * @return a LinePrimitive whose vertices follow the ecliptic in RA/Dec coordinates
         */
    @NonNull
    private LinePrimitive createEclipticLine() {
        LinePrimitive line = new LinePrimitive(eclipticColor, lineWidth);

        // The ecliptic is a great circle tilted at the obliquity angle
        // Parametric equations for ecliptic:
        // Dec = OBLIQUITY * sin(ecliptic_longitude)
        // RA = ecliptic_longitude (approximately, for small obliquity)
        // More accurately:
        // tan(RA) = tan(lon) * cos(OBLIQUITY)
        // sin(Dec) = sin(lon) * sin(OBLIQUITY)

        for (int i = 0; i <= NUM_RA_VERTICES; i++) {
            float lonDeg = i * 360.0f / NUM_RA_VERTICES;
            float lonRad = (float) Math.toRadians(lonDeg);
            float obliqRad = (float) Math.toRadians(OBLIQUITY);

            // Calculate RA and Dec from ecliptic longitude
            float ra = (float) Math.toDegrees(Math.atan2(
                    Math.sin(lonRad) * Math.cos(obliqRad),
                    Math.cos(lonRad)
            ));
            if (ra < 0) ra += 360.0f;

            float dec = (float) Math.toDegrees(Math.asin(
                    Math.sin(lonRad) * Math.sin(obliqRad)
            ));

            line.addVertex(ra, dec);
        }

        return line;
    }

    /**
         * Format a declination value as an integer-degrees string with a leading '+' for positive values.
         *
         * @param dec Declination in degrees.
         * @return `+DD°` for positive values, `DD°` for zero or negative values; degrees are rounded to the nearest integer.
         */
    @NonNull
    private String formatDec(float dec) {
        int degInt = Math.round(dec);
        if (degInt > 0) {
            return "+" + degInt + "\u00B0";
        } else {
            return degInt + "\u00B0";
        }
    }

    /**
         * Convert an RA hour value into a short hour label.
         *
         * @param hours hour value in the range 0–24
         * @return the hour followed by "h" (for example, "6h")
         */
    @NonNull
    private String formatRaHours(int hours) {
        return hours + "h";
    }

    /**
         * Provides the label for the north celestial pole.
         *
         * @return the localized label for the north celestial pole; currently returns "N"
         */
    @NonNull
    private String getNorthPoleLabel() {
        // TODO: Use resources for localization when available
        return "N";
    }

    /**
         * Provides the label for the south celestial pole.
         *
         * @return the label string for the south celestial pole; returns "S" when no localization is available
         */
    @NonNull
    private String getSouthPoleLabel() {
        // TODO: Use resources for localization when available
        return "S";
    }

    // ==================== Configuration Methods ====================

    /**
     * Configure how many right-ascension meridians the layer will render.
     *
     * <p>Changing this value does not take effect until {@link #redraw()} is called.</p>
     *
     * @param numRaLines the number of RA lines (e.g., 12 for 2-hour spacing, 24 for 1-hour)
     */
    public void setNumRaLines(int numRaLines) {
        this.numRaLines = numRaLines;
    }

    /**
     * Get the number of right ascension (RA) meridian lines configured for this layer.
     *
     * @return the number of RA lines
     */
    public int getNumRaLines() {
        return numRaLines;
    }

    /**
     * Set the number of declination divisions between the celestial equator and a pole.
     *
     * @param numDecLines the number of divisions; declination lines are placed at dec = i * 90 / numDecLines
     *                    for i = 1..(numDecLines - 1) on each side of the equator (e.g., 9 yields 10° spacing)
     */
    public void setNumDecLines(int numDecLines) {
        this.numDecLines = numDecLines;
    }

    /**
     * Get the number of declination (Dec) lines per hemisphere.
     *
     * @return the number of declination (Dec) lines on each side of the celestial equator
     */
    public int getNumDecLines() {
        return numDecLines;
    }

    /**
     * Sets the color for normal grid lines.
     *
     * @param lineColor ARGB color value
     */
    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * The color used for grid lines.
     *
     * @return the ARGB color value for grid lines
     */
    public int getLineColor() {
        return lineColor;
    }

    /**
     * Sets the color for the celestial equator.
     *
     * @param equatorColor ARGB color value
     */
    public void setEquatorColor(int equatorColor) {
        this.equatorColor = equatorColor;
    }

    /**
     * Gets the color used to draw the celestial equator.
     *
     * @return the equator color as an ARGB color value
     */
    public int getEquatorColor() {
        return equatorColor;
    }

    /**
     * Set the color used for drawing the ecliptic line.
     *
     * @param eclipticColor ARGB color value to use for the ecliptic line
     */
    public void setEclipticColor(int eclipticColor) {
        this.eclipticColor = eclipticColor;
    }

    /**
     * Gets the color used to draw the ecliptic line.
     *
     * @return the ARGB color value used for the ecliptic line
     */
    public int getEclipticColor() {
        return eclipticColor;
    }

    /**
     * Sets the color for grid labels.
     *
     * @param labelColor ARGB color value
     */
    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    /**
     * The color used for label rendering.
     *
     * @return the ARGB color value used for labels
     */
    public int getLabelColor() {
        return labelColor;
    }

    /**
     * Sets the width used to draw grid lines.
     *
     * @param lineWidth the line width in pixels; values &gt;= 0 draw with the specified thickness
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Provides the configured grid line width in pixels.
     *
     * @return the line width in pixels
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Set whether the layer displays the ecliptic line.
     *
     * @param showEcliptic true to display the ecliptic line, false to hide it
     */
    public void setShowEcliptic(boolean showEcliptic) {
        this.showEcliptic = showEcliptic;
    }

    /**
     * Indicates whether the ecliptic line is enabled for rendering.
     *
     * @return `true` if the ecliptic line is enabled, `false` otherwise.
     */
    public boolean isShowEcliptic() {
        return showEcliptic;
    }

    /**
     * Enable or disable rendering of textual labels (declination, hour marks, and pole labels) on the grid.
     *
     * @param showLabels true to enable labels, false to disable them
     */
    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    /**
     * Indicates whether labels are shown.
     *
     * @return true if labels are shown, false otherwise.
     */
    public boolean isShowLabels() {
        return showLabels;
    }

    /**
     * Sets the Android Resources used to localize labels produced by this layer.
     *
     * @param resources Android Resources used for localization, or null to use built-in defaults
     */
    public void setResources(@Nullable Resources resources) {
        this.resources = resources;
    }
}