package com.astro.app.core.layers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.astro.app.core.control.SolarSystemBody;
import com.astro.app.core.control.space.Universe;
import com.astro.app.core.math.RaDec;
import com.astro.app.data.model.GeocentricCoords;
import com.astro.app.data.model.LabelPrimitive;
import com.astro.app.data.model.PointPrimitive;
import com.astro.app.data.model.Shape;

import java.util.Date;

/**
 * Layer for rendering solar system planets in the sky map.
 *
 * <p>This layer calculates and displays the positions of the visible planets
 * (Mercury, Venus, Mars, Jupiter, Saturn, Uranus, Neptune, and Pluto) as well as
 * the Sun and Moon. Positions are calculated using orbital mechanics based on
 * JPL ephemeris data.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Real-time planet position calculation</li>
 *   <li>Support for time travel (historical/future sky views)</li>
 *   <li>Color-coded planets for easy identification</li>
 *   <li>Planet name labels for bright planets</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * Universe universe = new Universe();
 * PlanetsLayer planetsLayer = new PlanetsLayer(universe);
 * planetsLayer.initialize();
 * planetsLayer.setTime(System.currentTimeMillis()); // Update positions
 * }</pre>
 *
 * @see Universe
 * @see SolarSystemBody
 */
public class PlanetsLayer extends AbstractLayer {

    private static final String TAG = "PlanetsLayer";

    /** Layer ID constant */
    public static final String LAYER_ID = "layer_planets";

    /** Layer name for display */
    public static final String LAYER_NAME = "Planets";

    /** Depth order - planets render on top of stars */
    public static final int DEPTH_ORDER = 40;

    /** Default label color for planet names */
    private static final int DEFAULT_LABEL_COLOR = 0xFFFFB74D; // Warm orange

    /** Size for planet points (larger than stars) */
    private static final int PLANET_POINT_SIZE = 6;

    /** Size for Sun and Moon (largest) */
    private static final int SUN_MOON_POINT_SIZE = 10;

    // Planet colors based on their visual appearance
    private static final int COLOR_SUN = 0xFFFFD700;      // Gold
    private static final int COLOR_MOON = 0xFFF4F4F4;     // Near white
    private static final int COLOR_MERCURY = 0xFFB0B0B0;  // Gray
    private static final int COLOR_VENUS = 0xFFE6E6CC;    // Pale yellow
    private static final int COLOR_MARS = 0xFFFF6347;     // Red-orange
    private static final int COLOR_JUPITER = 0xFFD4A574;  // Tan/brown
    private static final int COLOR_SATURN = 0xFFF4D59E;   // Pale gold
    private static final int COLOR_URANUS = 0xFFAFDBF5;   // Pale blue
    private static final int COLOR_NEPTUNE = 0xFF5B5DDF; // Deep blue
    private static final int COLOR_PLUTO = 0xFFCCBBAA;    // Brownish gray

    /** The Universe object for calculating planet positions */
    @NonNull
    private final Universe universe;

    /** Current observation time in milliseconds */
    private long observationTimeMillis;

    /** Whether to show planet labels */
    private boolean showLabels = true;

    /**
     * Create a PlanetsLayer that renders solar system bodies using the provided Universe for position calculations.
     *
     * @param universe the Universe used to compute right ascension and declination for solar system bodies
     */
    public PlanetsLayer(@NonNull Universe universe) {
        super(LAYER_ID, LAYER_NAME, DEPTH_ORDER);
        this.universe = universe;
        this.observationTimeMillis = System.currentTimeMillis();
    }

    /**
     * Initializes the planets layer by computing and adding solar system bodies for the current observation time.
     *
     * Computes positions for every SolarSystemBody except Earth using the layer's observation time, creates and
     * adds the corresponding point (and optional label) primitives to the layer, and logs initialization progress.
     * Errors encountered while adding individual bodies are caught and logged without aborting the overall initialization.
     */
    @Override
    protected void initializeLayer() {
        Log.d(TAG, "Initializing planets layer for time: " + new Date(observationTimeMillis));

        Date observationDate = new Date(observationTimeMillis);

        // Add all visible solar system bodies (excluding Earth)
        for (SolarSystemBody body : SolarSystemBody.values()) {
            if (body == SolarSystemBody.Earth) {
                continue; // Skip Earth - we're observing from Earth
            }

            try {
                addPlanetToLayer(body, observationDate);
            } catch (Exception e) {
                Log.e(TAG, "Error adding planet " + body.name() + ": " + e.getMessage());
            }
        }

        Log.d(TAG, "Planets layer initialized with " + points.size() + " planets");
    }

    /**
     * Computes the body's apparent position for the given observation date and adds a point primitive
     * (and an optional label) representing that body to the layer.
     *
     * @param body the solar system body to add
     * @param date the observation date used to compute the body's position
     */
    private void addPlanetToLayer(@NonNull SolarSystemBody body, @NonNull Date date) {
        // Calculate planet position using Universe
        RaDec raDec = universe.getRaDec(body, date);

        // Convert RaDec to GeocentricCoords
        GeocentricCoords coords = GeocentricCoords.fromDegrees(raDec.getRa(), raDec.getDec());

        // Get planet color and size
        int color = getPlanetColor(body);
        int size = getPlanetSize(body);

        // Create point primitive for the planet
        PointPrimitive point = PointPrimitive.create(coords, color, size, Shape.CIRCLE);
        addPoint(point);

        // Add label for the planet
        if (showLabels) {
            String name = getPlanetDisplayName(body);
            LabelPrimitive label = LabelPrimitive.create(coords, name, DEFAULT_LABEL_COLOR);
            addLabel(label);
        }

        Log.d(TAG, "Added " + body.name() + " at RA=" + raDec.getRa() + ", Dec=" + raDec.getDec());
    }

    /**
     * Sets the observation time and updates planet positions.
     *
     * @param timeMillis The observation time in milliseconds since epoch
     */
    public void setTime(long timeMillis) {
        this.observationTimeMillis = timeMillis;
        redraw(); // Recalculate positions for new time
    }

    /**
     * Current observation time used to compute planetary positions.
     *
     * @return The observation time in milliseconds since the Unix epoch.
     */
    public long getObservationTime() {
        return observationTimeMillis;
    }

    /**
     * Enable or disable rendering of planet name labels.
     *
     * @param showLabels true to enable planet name labels, false to disable
     */
    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    /**
     * Indicates whether planet labels are shown.
     *
     * @return `true` if labels are shown, `false` otherwise.
     */
    public boolean isShowLabels() {
        return showLabels;
    }

    /**
     * Provide the display color associated with the given solar system body.
     *
     * @param body the solar system body whose display color is requested
     * @return the ARGB color value for the body; white (0xFFFFFFFF) if the body is unrecognized
     */
    private int getPlanetColor(@NonNull SolarSystemBody body) {
        switch (body) {
            case Sun:
                return COLOR_SUN;
            case Moon:
                return COLOR_MOON;
            case Mercury:
                return COLOR_MERCURY;
            case Venus:
                return COLOR_VENUS;
            case Mars:
                return COLOR_MARS;
            case Jupiter:
                return COLOR_JUPITER;
            case Saturn:
                return COLOR_SATURN;
            case Uranus:
                return COLOR_URANUS;
            case Neptune:
                return COLOR_NEPTUNE;
            case Pluto:
                return COLOR_PLUTO;
            default:
                return 0xFFFFFFFF; // White default
        }
    }

    /**
     * Determine the point size in pixels used to render the specified solar system body.
     *
     * Sun and Moon use a distinct larger size; brighter planets are rendered slightly larger than dimmer ones.
     *
     * @param body the solar system body to size
     * @return the point size in pixels for the given body
     */
    private int getPlanetSize(@NonNull SolarSystemBody body) {
        switch (body) {
            case Sun:
            case Moon:
                return SUN_MOON_POINT_SIZE;
            case Venus:
            case Jupiter:
                return PLANET_POINT_SIZE + 2; // Brightest planets
            case Mars:
            case Saturn:
                return PLANET_POINT_SIZE;
            default:
                return PLANET_POINT_SIZE - 1; // Dimmer planets
        }
    }

    /**
     * Provides the user-facing name for a solar system body.
     *
     * @param body the solar system body
     * @return the capitalized display name for the body; for unexpected values returns {@code body.name()}
     */
    @NonNull
    private String getPlanetDisplayName(@NonNull SolarSystemBody body) {
        // Return proper capitalized names
        switch (body) {
            case Sun:
                return "Sun";
            case Moon:
                return "Moon";
            case Mercury:
                return "Mercury";
            case Venus:
                return "Venus";
            case Mars:
                return "Mars";
            case Jupiter:
                return "Jupiter";
            case Saturn:
                return "Saturn";
            case Uranus:
                return "Uranus";
            case Neptune:
                return "Neptune";
            case Pluto:
                return "Pluto";
            default:
                return body.name();
        }
    }
}