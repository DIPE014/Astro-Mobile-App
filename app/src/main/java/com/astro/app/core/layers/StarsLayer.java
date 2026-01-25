package com.astro.app.core.layers;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.LabelPrimitive;
import com.astro.app.data.model.PointPrimitive;
import com.astro.app.data.model.StarData;

import java.util.List;

/**
 * Layer for rendering stars in the sky map.
 *
 * <p>This layer converts {@link StarData} objects from a {@link StarRepository}
 * into point and label primitives for rendering. It supports magnitude-based
 * filtering to improve performance by limiting the number of rendered stars.</p>
 *
 * <h3>Magnitude Filtering:</h3>
 * <p>Stars can be filtered by magnitude to reduce rendering load. Only stars
 * brighter than (i.e., with magnitude less than) the configured limit will be
 * rendered. The default limit of 6.5 includes all stars visible to the naked eye.</p>
 *
 * <h3>Label Display:</h3>
 * <p>Labels can be shown for named stars. The magnitude limit for labels can be
 * set independently to show names only for brighter stars.</p>
 *
 * @see StarRepository
 * @see StarData
 */
public class StarsLayer extends AbstractLayer {

    private static final String TAG = "StarsLayer";

    /** Layer ID constant */
    public static final String LAYER_ID = "layer_stars";

    /** Layer name for display */
    public static final String LAYER_NAME = "Stars";

    /** Depth order - stars render on top of most other layers */
    public static final int DEPTH_ORDER = 30;

    /** Default magnitude limit (includes all naked-eye visible stars) */
    public static final float DEFAULT_MAGNITUDE_LIMIT = 6.5f;

    /** Default magnitude limit for showing star names */
    public static final float DEFAULT_LABEL_MAGNITUDE_LIMIT = 3.0f;

    /** Default color for star labels */
    private static final int DEFAULT_LABEL_COLOR = 0xFFCCCCCC; // Light gray

    /** The star repository providing star data */
    @NonNull
    private final StarRepository starRepository;

    /** Maximum magnitude of stars to render (lower = only brighter stars) */
    private float magnitudeLimit;

    /** Maximum magnitude of stars to show labels for */
    private float labelMagnitudeLimit;

    /** Whether to show star labels */
    private boolean showLabels;

    /**
     * Interface for providing star data to the layer.
     *
     * <p>Implementations should handle loading star data from files,
     * databases, or other sources.</p>
     */
    public interface StarRepository {

        /**
         * Retrieve all available stars from the repository.
         *
         * @return a non-null list of StarData objects containing all known stars; may be empty
         */
        @NonNull
        List<StarData> getStars();

        /**
         * Retrieve stars brighter than the specified magnitude.
         *
         * @param maxMagnitude maximum magnitude; lower values indicate brighter stars
         * @return non-null list of stars with magnitude less than `maxMagnitude` (may be empty)
         */
        @NonNull
        List<StarData> getStarsBrighterThan(float maxMagnitude);

        /**
         * Finds the star with the specified identifier.
         *
         * @param starId non-null identifier of the star to look up
         * @return the matching StarData, or null if no star has the given identifier
         */
        @Nullable
        StarData findById(@NonNull String starId);

        /**
         * Find stars whose name contains the given text (partial match).
         *
         * @param name substring to match against star names; matching is partial
         * @return a non-null list of stars whose names match the provided text (may be empty)
         */
        @NonNull
        List<StarData> findByName(@NonNull String name);
    }

    /**
     * Constructs a StarsLayer backed by the provided StarRepository using default magnitude and label settings.
     *
     * @param starRepository the non-null repository supplying star data for this layer
     */
    public StarsLayer(@NonNull StarRepository starRepository) {
        this(starRepository, DEFAULT_MAGNITUDE_LIMIT, DEFAULT_LABEL_MAGNITUDE_LIMIT, true);
    }

    /**
     * Constructs a StarsLayer backed by the given StarRepository and configured with
     * magnitude and label display limits.
     *
     * @param starRepository      non-null repository that provides star data
     * @param magnitudeLimit      maximum star magnitude to render (higher values include fainter stars)
     * @param labelMagnitudeLimit maximum star magnitude eligible for labels (labels shown for stars with magnitude <= this)
     * @param showLabels          true to enable label creation for eligible stars, false to disable labels
     */
    public StarsLayer(@NonNull StarRepository starRepository,
                      float magnitudeLimit,
                      float labelMagnitudeLimit,
                      boolean showLabels) {
        super(LAYER_ID, LAYER_NAME, DEPTH_ORDER);
        this.starRepository = starRepository;
        this.magnitudeLimit = magnitudeLimit;
        this.labelMagnitudeLimit = labelMagnitudeLimit;
        this.showLabels = showLabels;
    }

    /**
     * Populates the layer with star point primitives and optional name labels according to the
     * layer's magnitude and label visibility settings.
     *
     * The method loads stars filtered by the current magnitude limit, creates a point primitive
     * for each loaded star, and—when label display is enabled—adds a label for named stars whose
     * magnitude is less than or equal to the label magnitude limit.
     */
    @Override
    protected void initializeLayer() {
        Log.d(TAG, "Initializing stars layer with magnitude limit: " + magnitudeLimit);

        // Get stars filtered by magnitude
        List<StarData> stars = starRepository.getStarsBrighterThan(magnitudeLimit);
        Log.d(TAG, "Found " + stars.size() + " stars brighter than magnitude " + magnitudeLimit);

        for (StarData star : stars) {
            // Create point primitive for the star
            PointPrimitive point = createPointFromStar(star);
            addPoint(point);

            // Create label if this star is bright enough and has a name
            if (showLabels && shouldShowLabel(star)) {
                LabelPrimitive label = createLabelFromStar(star);
                if (label != null) {
                    addLabel(label);
                }
            }
        }

        Log.d(TAG, "Created " + points.size() + " star points and " + labels.size() + " labels");
    }

    /**
     * Create a PointPrimitive used to render the given star.
     *
     * @param star the star data to convert into a point primitive
     * @return the PointPrimitive representing the star's visual point
     */
    @NonNull
    private PointPrimitive createPointFromStar(@NonNull StarData star) {
        // Use the factory method which handles all the conversion
        return PointPrimitive.fromStar(star);
    }

    /**
         * Create a label primitive for the given star if the star has a name.
         *
         * @return the `LabelPrimitive` for the star, or `null` if the star has no name
         */
    @Nullable
    private LabelPrimitive createLabelFromStar(@NonNull StarData star) {
        String name = star.getName();
        if (name == null || name.isEmpty()) {
            return null;
        }

        // Use the factory method for creating labels from stars
        return LabelPrimitive.fromStar(star).withColor(DEFAULT_LABEL_COLOR);
    }

    /**
         * Determines whether a label should be displayed for the given star.
         *
         * A label is shown only if the star has a non-empty name and its magnitude
         * is less than or equal to the layer's label magnitude limit.
         *
         * @param star the star to evaluate
         * @return `true` if the star has a non-empty name and its magnitude is less than or equal to the label magnitude limit, `false` otherwise
         */
    private boolean shouldShowLabel(@NonNull StarData star) {
        // Only show labels for bright stars with names
        if (star.getName() == null || star.getName().isEmpty()) {
            return false;
        }
        return star.getMagnitude() <= labelMagnitudeLimit;
    }

    // ==================== Configuration Methods ====================

    /**
     * Update the maximum magnitude used to determine which stars are rendered.
     *
     * <p>Changing this value requires calling {@link #redraw()} for the change to take effect.</p>
     *
     * @param magnitudeLimit maximum magnitude to include; lower values restrict rendering to brighter stars
     */
    public void setMagnitudeLimit(float magnitudeLimit) {
        this.magnitudeLimit = magnitudeLimit;
    }

    /**
     * Get the current magnitude limit used to filter which stars are rendered.
     *
     * @return the maximum magnitude; stars with magnitude greater than this value are excluded from rendering
     */
    public float getMagnitudeLimit() {
        return magnitudeLimit;
    }

    /**
     * Sets the maximum star magnitude eligible for displaying labels.
     *
     * @param labelMagnitudeLimit maximum magnitude (inclusive) for which named stars may receive labels
     */
    public void setLabelMagnitudeLimit(float labelMagnitudeLimit) {
        this.labelMagnitudeLimit = labelMagnitudeLimit;
    }

    /**
     * Gets the maximum star magnitude eligible for showing labels.
     *
     * @return the maximum magnitude for which a star will receive a label
     */
    public float getLabelMagnitudeLimit() {
        return labelMagnitudeLimit;
    }

    /**
     * Enable or disable rendering of star labels in this layer.
     *
     * @param showLabels `true` to display labels, `false` to hide them
     */
    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    /**
     * Indicates whether star labels are displayed.
     *
     * @return true if labels are shown, false otherwise.
     */
    public boolean isShowLabels() {
        return showLabels;
    }

    /**
         * Accesses the StarRepository backing this layer.
         *
         * @return the StarRepository used by this layer (never {@code null})
         */
    @NonNull
    public StarRepository getStarRepository() {
        return starRepository;
    }
}