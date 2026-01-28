package com.astro.app.core.layers;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.data.model.LabelPrimitive;
import com.astro.app.data.model.PointPrimitive;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.StarRepository;

import java.util.List;

/**
 * Layer for rendering stars in the sky map.
 *
 * <p>This layer converts {@link StarData} objects from a
 * {@link com.astro.app.data.repository.StarRepository} into point and label
 * primitives for rendering. It supports magnitude-based filtering to improve
 * performance by limiting the number of rendered stars.</p>
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
 * @see com.astro.app.data.repository.StarRepository
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
     * Creates a StarsLayer with the given repository.
     *
     * @param starRepository The repository providing star data
     */
    public StarsLayer(@NonNull StarRepository starRepository) {
        this(starRepository, DEFAULT_MAGNITUDE_LIMIT, DEFAULT_LABEL_MAGNITUDE_LIMIT, true);
    }

    /**
     * Creates a StarsLayer with custom settings.
     *
     * @param starRepository       The repository providing star data
     * @param magnitudeLimit       Maximum magnitude for stars to render
     * @param labelMagnitudeLimit  Maximum magnitude for showing labels
     * @param showLabels           Whether to show star labels
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

    @Override
    protected void initializeLayer() {
        Log.d(TAG, "Initializing stars layer with magnitude limit: " + magnitudeLimit);

        // Get stars filtered by magnitude
        List<StarData> stars = starRepository.getStarsByMagnitude(magnitudeLimit);
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
     * Creates a point primitive from star data.
     *
     * @param star The star data
     * @return A point primitive representing the star
     */
    @NonNull
    private PointPrimitive createPointFromStar(@NonNull StarData star) {
        // Use the factory method which handles all the conversion
        return PointPrimitive.fromStar(star);
    }

    /**
     * Creates a label primitive from star data.
     *
     * @param star The star data
     * @return A label primitive, or null if the star has no name
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
     * Determines if a label should be shown for a star.
     *
     * @param star The star data
     * @return true if a label should be shown
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
     * Sets the magnitude limit for rendering stars.
     *
     * <p>Changing this will require calling {@link #redraw()} to take effect.</p>
     *
     * @param magnitudeLimit Maximum magnitude (lower = only brighter stars)
     */
    public void setMagnitudeLimit(float magnitudeLimit) {
        this.magnitudeLimit = magnitudeLimit;
    }

    /**
     * Returns the current magnitude limit.
     *
     * @return Maximum magnitude for rendered stars
     */
    public float getMagnitudeLimit() {
        return magnitudeLimit;
    }

    /**
     * Sets the magnitude limit for showing star labels.
     *
     * @param labelMagnitudeLimit Maximum magnitude for labeled stars
     */
    public void setLabelMagnitudeLimit(float labelMagnitudeLimit) {
        this.labelMagnitudeLimit = labelMagnitudeLimit;
    }

    /**
     * Returns the current label magnitude limit.
     *
     * @return Maximum magnitude for labeled stars
     */
    public float getLabelMagnitudeLimit() {
        return labelMagnitudeLimit;
    }

    /**
     * Sets whether to show star labels.
     *
     * @param showLabels true to show labels, false to hide them
     */
    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    /**
     * Returns whether star labels are shown.
     *
     * @return true if labels are shown
     */
    public boolean isShowLabels() {
        return showLabels;
    }

    /**
     * Returns the star repository.
     *
     * @return The star repository
     */
    @NonNull
    public StarRepository getStarRepository() {
        return starRepository;
    }
}
