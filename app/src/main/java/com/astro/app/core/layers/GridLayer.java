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
     * Creates a GridLayer with resources for localized labels.
     *
     * @param numRaLines  Number of RA lines
     * @param numDecLines Number of Dec lines
     * @param resources   Resources for string lookup (optional)
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
     * Creates a Declination line (parallel) at the given declination.
     *
     * @param dec   Declination in degrees
     * @param color Color for this line
     * @return A line primitive for the Dec parallel
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
     * Creates the ecliptic line.
     *
     * <p>The ecliptic is the path the Sun takes through the sky over a year.
     * It is tilted relative to the celestial equator by about 23.4 degrees.</p>
     *
     * @return A line primitive for the ecliptic
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
     * Formats a declination value for display.
     *
     * @param dec Declination in degrees
     * @return Formatted string (e.g., "+30deg" or "-45deg")
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
     * Formats Right Ascension hours for display.
     *
     * @param hours RA in hours (0-24)
     * @return Formatted string (e.g., "6h")
     */
    @NonNull
    private String formatRaHours(int hours) {
        return hours + "h";
    }

    /**
     * Gets the label for the north celestial pole.
     *
     * @return Localized string or default
     */
    @NonNull
    private String getNorthPoleLabel() {
        // TODO: Use resources for localization when available
        return "N";
    }

    /**
     * Gets the label for the south celestial pole.
     *
     * @return Localized string or default
     */
    @NonNull
    private String getSouthPoleLabel() {
        // TODO: Use resources for localization when available
        return "S";
    }

    // ==================== Configuration Methods ====================

    /**
     * Sets the number of RA lines (meridians).
     *
     * <p>Changing this will require calling {@link #redraw()} to take effect.</p>
     *
     * @param numRaLines Number of RA lines (e.g., 12 for 2-hour spacing, 24 for 1-hour)
     */
    public void setNumRaLines(int numRaLines) {
        this.numRaLines = numRaLines;
    }

    /**
     * Returns the number of RA lines.
     *
     * @return Number of RA lines
     */
    public int getNumRaLines() {
        return numRaLines;
    }

    /**
     * Sets the number of Dec lines on each side of the equator.
     *
     * @param numDecLines Number of Dec lines (e.g., 9 for 10-degree spacing)
     */
    public void setNumDecLines(int numDecLines) {
        this.numDecLines = numDecLines;
    }

    /**
     * Returns the number of Dec lines.
     *
     * @return Number of Dec lines on each side of equator
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
     * Returns the grid line color.
     *
     * @return ARGB color value
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
     * Returns the equator line color.
     *
     * @return ARGB color value
     */
    public int getEquatorColor() {
        return equatorColor;
    }

    /**
     * Sets the color for the ecliptic line.
     *
     * @param eclipticColor ARGB color value
     */
    public void setEclipticColor(int eclipticColor) {
        this.eclipticColor = eclipticColor;
    }

    /**
     * Returns the ecliptic line color.
     *
     * @return ARGB color value
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
     * Returns the label color.
     *
     * @return ARGB color value
     */
    public int getLabelColor() {
        return labelColor;
    }

    /**
     * Sets the line width for grid lines.
     *
     * @param lineWidth Width in pixels
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Returns the grid line width.
     *
     * @return Width in pixels
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Sets whether to show the ecliptic line.
     *
     * @param showEcliptic true to show the ecliptic
     */
    public void setShowEcliptic(boolean showEcliptic) {
        this.showEcliptic = showEcliptic;
    }

    /**
     * Returns whether the ecliptic is shown.
     *
     * @return true if ecliptic is shown
     */
    public boolean isShowEcliptic() {
        return showEcliptic;
    }

    /**
     * Sets whether to show labels.
     *
     * @param showLabels true to show labels
     */
    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    /**
     * Returns whether labels are shown.
     *
     * @return true if labels are shown
     */
    public boolean isShowLabels() {
        return showLabels;
    }

    /**
     * Sets the Resources for localized strings.
     *
     * @param resources Android Resources
     */
    public void setResources(@Nullable Resources resources) {
        this.resources = resources;
    }
}
