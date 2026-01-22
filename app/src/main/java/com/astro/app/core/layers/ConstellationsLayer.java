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
         * Returns the list of all constellations.
         *
         * @return List of constellation data (never null, may be empty)
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
         * Finds constellations by name (partial match).
         *
         * @param name The name to search for
         * @return List of matching constellations
         */
        @NonNull
        List<ConstellationData> findByName(@NonNull String name);

        /**
         * Returns star coordinates for the given constellation.
         *
         * <p>The returned map contains star IDs as keys and their
         * coordinates as values. This is used for drawing constellation
         * lines between stars.</p>
         *
         * @param constellation The constellation to get star coordinates for
         * @return Map of star ID to coordinates
         */
        @NonNull
        Map<String, GeocentricCoords> getStarCoordinates(@NonNull ConstellationData constellation);
    }

    /**
     * Creates a ConstellationsLayer with the given repository.
     *
     * @param constellationRepository The repository providing constellation data
     */
    public ConstellationsLayer(@NonNull ConstellationRepository constellationRepository) {
        this(constellationRepository, true, true);
    }

    /**
     * Creates a ConstellationsLayer with custom visibility settings.
     *
     * @param constellationRepository The repository providing constellation data
     * @param showLines               Whether to show constellation lines
     * @param showNames               Whether to show constellation names
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
     * Creates line primitives for a constellation's pattern.
     *
     * @param constellation The constellation data
     * @param starCoords    Map of star ID to coordinates
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
     * Creates a label primitive for the constellation name.
     *
     * @param constellation The constellation data
     * @param starCoords    Map of star ID to coordinates
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
     * Calculates the center point of a set of star coordinates.
     *
     * @param starCoords Map of star ID to coordinates
     * @return The center point, or null if the map is empty
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
     * Returns whether constellation lines are shown.
     *
     * @return true if lines are shown
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
     * Returns whether constellation names are shown.
     *
     * @return true if names are shown
     */
    public boolean isShowNames() {
        return showNames;
    }

    /**
     * Sets the color for constellation lines.
     *
     * @param lineColor ARGB color value
     */
    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * Returns the constellation line color.
     *
     * @return ARGB color value
     */
    public int getLineColor() {
        return lineColor;
    }

    /**
     * Sets the color for constellation labels.
     *
     * @param labelColor ARGB color value
     */
    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    /**
     * Returns the constellation label color.
     *
     * @return ARGB color value
     */
    public int getLabelColor() {
        return labelColor;
    }

    /**
     * Sets the line width for constellation lines.
     *
     * @param lineWidth Width in pixels
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * Returns the constellation line width.
     *
     * @return Width in pixels
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Returns the constellation repository.
     *
     * @return The constellation repository
     */
    @NonNull
    public ConstellationRepository getConstellationRepository() {
        return constellationRepository;
    }
}
