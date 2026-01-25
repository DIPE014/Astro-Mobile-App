package com.astro.app.core.layers;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.ConstellationData;
import com.astro.app.data.model.GeocentricCoords;
import com.astro.app.data.model.LabelPrimitive;
import com.astro.app.data.model.LinePrimitive;

import java.util.List;
import java.util.Map;

/**
 * Layer for rendering constellation lines and labels in the sky map.
 *
 * <p>This layer converts {@link ConstellationData} objects from a
 * {@link ConstellationRepository} into line and label primitives for rendering.
 * It supports independent control over line visibility and name display.</p>
 *
 * <h3>Line Rendering:</h3>
 * <p>Constellation lines are drawn between stars that form the traditional
 * constellation patterns. Each line connects two stars using their coordinates.</p>
 *
 * <h3>Label Positioning:</h3>
 * <p>Constellation names are displayed at the center of each constellation,
 * typically calculated as the average position of all stars in the constellation.</p>
 *
 * @see ConstellationRepository
 * @see ConstellationData
 */
public class ConstellationsLayer extends AbstractLayer {

    private static final String TAG = "ConstellationsLayer";

    /** Layer ID constant */
    public static final String LAYER_ID = "layer_constellations";

    /** Layer name for display */
    public static final String LAYER_NAME = "Constellations";

    /** Depth order - constellations render behind stars */
    public static final int DEPTH_ORDER = 10;

    /** Default color for constellation lines */
    public static final int DEFAULT_LINE_COLOR = 0x40FFFFFF; // Semi-transparent white

    /** Default color for constellation names */
    public static final int DEFAULT_LABEL_COLOR = 0xFF87CEEB; // Sky blue

    /** Default line width */
    public static final float DEFAULT_LINE_WIDTH = 1.5f;

    /** The constellation repository providing constellation data */
    @NonNull
    private final ConstellationRepository constellationRepository;

    /** Color for constellation lines */
    private int lineColor;

    /** Color for constellation labels */
    private int labelColor;

    /** Line width for constellation lines */
    private float lineWidth;

    /** Whether to show constellation lines */
    private boolean showLines;

    /** Whether to show constellation names */
    private boolean showNames;

    /**
     * Interface for providing constellation data to the layer.
     *
     * <p>Implementations should handle loading constellation data and
     * resolving star coordinates for drawing lines.</p>
     */
    public interface ConstellationRepository {

        /**
         * Retrieves all available constellations.
         *
         * @return a non-null list of ConstellationData objects; may be empty if no constellations are available
         */
        @NonNull
        List<ConstellationData> getConstellations();

        /**
         * Finds a constellation by its ID.
         *
         * @param constellationId The constellation ID (e.g., "Ori")
         * @return The constellation data, or null if not found
         */
        @Nullable
        ConstellationData findById(@NonNull String constellationId);

        /**
         * Finds constellations whose names contain the given query string.
         *
         * @param name query string to match against constellation names; partial matches are allowed
         * @return a list of constellations whose names match the query, or an empty list if none match
         */
        @NonNull
        List<ConstellationData> findByName(@NonNull String name);

        /**
         * Get star coordinates for the specified constellation.
         *
         * @param constellation the constellation whose star coordinates to retrieve
         * @return a non-null map from star ID to corresponding GeocentricCoords (may be empty)
         */
        @NonNull
        Map<String, GeocentricCoords> getStarCoordinates(@NonNull ConstellationData constellation);
    }

    /**
     * Constructs a ConstellationsLayer using the provided repository with constellation lines and names shown by default.
     *
     * @param constellationRepository repository providing constellation data; must not be null
     */
    public ConstellationsLayer(@NonNull ConstellationRepository constellationRepository) {
        this(constellationRepository, true, true);
    }

    /**
     * Constructs a ConstellationsLayer configured with the given data source and visibility settings for lines and names.
     *
     * @param constellationRepository the non-null repository that supplies constellation data and coordinates
     * @param showLines               true to render constellation connecting lines, false to hide them
     * @param showNames               true to render constellation name labels, false to hide them
     */
    public ConstellationsLayer(@NonNull ConstellationRepository constellationRepository,
                               boolean showLines,
                               boolean showNames) {
        super(LAYER_ID, LAYER_NAME, DEPTH_ORDER);
        this.constellationRepository = constellationRepository;
        this.lineColor = DEFAULT_LINE_COLOR;
        this.labelColor = DEFAULT_LABEL_COLOR;
        this.lineWidth = DEFAULT_LINE_WIDTH;
        this.showLines = showLines;
        this.showNames = showNames;
    }

    /**
     * Initializes the layer by loading constellations from the repository and creating line and label primitives according to visibility flags.
     *
     * If `showLines` is true, adds line primitives for each constellation's defined line pairs. If `showNames` is true, adds label primitives positioned at the constellation center (or the average of its star coordinates when a center is not provided). Primitives are added to this layer's internal collections.
     */
    @Override
    protected void initializeLayer() {
        Log.d(TAG, "Initializing constellations layer");

        List<ConstellationData> constellations = constellationRepository.getConstellations();
        Log.d(TAG, "Found " + constellations.size() + " constellations");

        for (ConstellationData constellation : constellations) {
            // Get star coordinates for this constellation
            Map<String, GeocentricCoords> starCoords =
                    constellationRepository.getStarCoordinates(constellation);

            // Create constellation lines
            if (showLines) {
                createConstellationLines(constellation, starCoords);
            }

            // Create constellation label
            if (showNames) {
                createConstellationLabel(constellation, starCoords);
            }
        }

        Log.d(TAG, "Created " + lines.size() + " constellation lines and " +
                labels.size() + " labels");
    }

    /**
     * Create line primitives for the given constellation by connecting its defined star pairs.
     *
     * For each valid pair of star IDs in the constellation's line pairs, a LinePrimitive is
     * created using coordinates looked up from the provided map and added to the layer. Pairs
     * with missing coordinates are skipped and a warning is logged.
     *
     * @param constellation the constellation whose line pairs will be rendered
     * @param starCoords    mapping from star ID to its geocentric coordinates used to position line endpoints
     */
    private void createConstellationLines(@NonNull ConstellationData constellation,
                                          @NonNull Map<String, GeocentricCoords> starCoords) {
        List<String[]> linePairs = constellation.getLinePairs();

        for (String[] pair : linePairs) {
            if (pair.length < 2) continue;

            String startStarId = pair[0];
            String endStarId = pair[1];

            GeocentricCoords startCoord = starCoords.get(startStarId);
            GeocentricCoords endCoord = starCoords.get(endStarId);

            if (startCoord == null || endCoord == null) {
                Log.w(TAG, "Missing star coordinates for line in " + constellation.getId() +
                        ": " + startStarId + " -> " + endStarId);
                continue;
            }

            // Create a line primitive connecting the two stars
            LinePrimitive line = new LinePrimitive(lineColor, lineWidth);
            line.addVertex(startCoord.getRa(), startCoord.getDec());
            line.addVertex(endCoord.getRa(), endCoord.getDec());

            addLine(line);
        }
    }

    /**
     * Create and add a label for the constellation positioned at its center.
     *
     * If the constellation provides an explicit center, that position is used; otherwise the
     * center is computed as the average of the provided star coordinates. If no center can be
     * determined, no label is created.
     *
     * @param constellation the constellation data containing id, name, and optional center point
     * @param starCoords    map of star ID to geocentric coordinates used to compute a center if needed
     */
    private void createConstellationLabel(@NonNull ConstellationData constellation,
                                          @NonNull Map<String, GeocentricCoords> starCoords) {
        // Get center point - either from constellation data or calculate it
        GeocentricCoords center = constellation.getCenterPoint();

        if (center == null && !starCoords.isEmpty()) {
            // Calculate center as average of star positions
            center = calculateCenter(starCoords);
        }

        if (center == null) {
            Log.w(TAG, "Cannot create label for " + constellation.getId() + ": no position");
            return;
        }

        LabelPrimitive label = new LabelPrimitive(center, constellation.getName(), labelColor);
        addLabel(label);
    }

    /**
         * Computes the arithmetic mean of right ascension and declination across the given star coordinates.
         *
         * @param starCoords map from star identifier to its geocentric coordinates (RA/Dec in degrees)
         * @return a GeocentricCoords whose RA and Dec are the averages of the input values, or `null` if `starCoords` is empty
         */
    @Nullable
    private GeocentricCoords calculateCenter(@NonNull Map<String, GeocentricCoords> starCoords) {
        if (starCoords.isEmpty()) {
            return null;
        }

        float sumRa = 0;
        float sumDec = 0;
        int count = 0;

        for (GeocentricCoords coord : starCoords.values()) {
            sumRa += coord.getRa();
            sumDec += coord.getDec();
            count++;
        }

        return GeocentricCoords.fromDegrees(sumRa / count, sumDec / count);
    }

    // ==================== Configuration Methods ====================

    /**
     * Sets whether to show constellation lines.
     *
     * <p>Changing this will require calling {@link #redraw()} to take effect.</p>
     *
     * @param showLines true to show lines, false to hide them
     */
    public void setShowLines(boolean showLines) {
        this.showLines = showLines;
    }

    /**
     * Indicates whether constellation lines are visible.
     *
     * @return `true` if constellation lines are visible, `false` otherwise
     */
    public boolean isShowLines() {
        return showLines;
    }

    /**
     * Sets whether to show constellation names.
     *
     * <p>Changing this will require calling {@link #redraw()} to take effect.</p>
     *
     * @param showNames true to show names, false to hide them
     */
    public void setShowNames(boolean showNames) {
        this.showNames = showNames;
    }

    /**
     * Indicates whether constellation names are visible.
     *
     * @return `true` if constellation names are shown, `false` otherwise
     */
    public boolean isShowNames() {
        return showNames;
    }

    /**
     * Updates the color used to draw constellation lines.
     *
     * @param lineColor ARGB color value to use for lines
     */
    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * Get the color used to draw constellation lines.
     *
     * @return the ARGB color value used for constellation lines
     */
    public int getLineColor() {
        return lineColor;
    }

    /**
     * Sets the color used to draw constellation labels.
     *
     * @param labelColor ARGB color value to use for labels
     */
    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    /**
         * The current ARGB color used for constellation labels.
         *
         * @return the ARGB color value used for constellation labels
         */
    public int getLabelColor() {
        return labelColor;
    }

    /**
         * Set the width used to draw constellation lines.
         *
         * <p>The width is specified in pixels; changes take effect only after the layer is redrawn.</p>
         *
         * @param lineWidth the line width in pixels
         */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Get the width used to draw constellation lines.
     *
     * @return the line width in pixels
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
         * Provide access to the ConstellationRepository used by this layer.
         *
         * @return the ConstellationRepository instance backing this layer
         */
    @NonNull
    public ConstellationRepository getConstellationRepository() {
        return constellationRepository;
    }
}