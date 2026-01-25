package com.astro.app.data.model;

/**
 * Enumeration of shapes used for rendering celestial objects.
 *
 * <p>Different celestial object types are typically rendered with different
 * shapes to help users distinguish between them at a glance.</p>
 *
 * <h3>Common Usage:</h3>
 * <ul>
 *   <li>{@link #CIRCLE} - Stars, general points</li>
 *   <li>{@link #STAR} - Bright or notable stars</li>
 *   <li>{@link #DIAMOND} - Galaxies, nebulae</li>
 *   <li>{@link #SQUARE} - Clusters</li>
 *   <li>{@link #TRIANGLE} - Special objects, markers</li>
 *   <li>{@link #CROSS} - Reference points, grid intersections</li>
 * </ul>
 */
public enum Shape {

    /**
     * A filled circle - the default shape for most stars.
     */
    CIRCLE,

    /**
     * A star shape with radiating points - used for bright or notable stars.
     */
    STAR,

    /**
     * A diamond (rotated square) - often used for galaxies.
     */
    DIAMOND,

    /**
     * A square shape - used for clusters or special markers.
     */
    SQUARE,

    /**
     * A triangle shape - used for special objects or directional markers.
     */
    TRIANGLE,

    /**
     * A cross/plus shape - used for reference points or grid intersections.
     */
    CROSS;

    /**
     * Default shape used for rendering celestial objects.
     *
     * @return the default Shape, CIRCLE
     */
    public static Shape getDefault() {
        return CIRCLE;
    }

    /**
     * Indicates whether the shape requires special rendering beyond a simple filled shape.
     *
     * @return true if the shape is STAR or CROSS, false otherwise.
     */
    public boolean isComplex() {
        return this == STAR || this == CROSS;
    }
}