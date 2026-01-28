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
     * Creates a PlanetsLayer with the given Universe for position calculations.
     *
     * @param universe The Universe object for calculating planet positions
     */
    public PlanetsLayer(@NonNull Universe universe) {
        super(LAYER_ID, LAYER_NAME, DEPTH_ORDER);
        this.universe = universe;
        this.observationTimeMillis = System.currentTimeMillis();
    }

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
     * Adds a single planet to the layer.
     *
     * @param body The solar system body to add
     * @param date The observation date for position calculation
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
     * Returns the current observation time.
     *
     * @return Time in milliseconds since epoch
     */
    public long getObservationTime() {
        return observationTimeMillis;
    }

    /**
     * Sets whether to show planet name labels.
     *
     * @param showLabels true to show labels, false to hide
     */
    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    /**
     * Returns whether planet labels are shown.
     *
     * @return true if labels are shown
     */
    public boolean isShowLabels() {
        return showLabels;
    }

    /**
     * Gets the display color for a solar system body.
     *
     * @param body The solar system body
     * @return ARGB color value
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
     * Gets the display size for a solar system body.
     *
     * @param body The solar system body
     * @return Size in pixels
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
     * Gets the display name for a solar system body.
     *
     * @param body The solar system body
     * @return Human-readable name
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
