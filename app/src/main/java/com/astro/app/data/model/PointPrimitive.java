package com.astro.app.data.model;

import androidx.annotation.NonNull;

/**
 * A rendering primitive representing a single point in the sky.
 *
 * <p>PointPrimitives are used to render stars and other point-like celestial
 * objects. They contain all the information needed to draw the point at the
 * correct location with the appropriate visual properties.</p>
 *
 * <h3>Rendering Properties:</h3>
 * <ul>
 *   <li><b>location</b> - Celestial coordinates (RA/Dec)</li>
 *   <li><b>color</b> - ARGB color for the point</li>
 *   <li><b>size</b> - Diameter in pixels at default zoom</li>
 *   <li><b>shape</b> - Visual shape (circle, star, diamond, etc.)</li>
 * </ul>
 *
 * <p>This class is immutable and thread-safe.</p>
 *
 * @see LinePrimitive
 * @see LabelPrimitive
 * @see Shape
 */
public final class PointPrimitive {

    /** Default white color */
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    /** Default size in pixels */
    private static final int DEFAULT_SIZE = 3;

    /** The celestial location of this point */
    @NonNull
    private final GeocentricCoords location;

    /** ARGB color for rendering */
    private final int color;

    /** Size in pixels at default zoom level */
    private final int size;

    /** Shape to use for rendering */
    @NonNull
    private final Shape shape;

    /**
     * Private constructor. Use {@link Builder} or factory methods.
     *
     * @param location The celestial coordinates
     * @param color    ARGB color value
     * @param size     Size in pixels
     * @param shape    Shape for rendering
     */
    private PointPrimitive(@NonNull GeocentricCoords location, int color, int size, @NonNull Shape shape) {
        this.location = location;
        this.color = color;
        this.size = size;
        this.shape = shape;
    }

    /**
     * Creates a point primitive from Right Ascension and Declination.
     *
     * @param ra    Right Ascension in degrees (0-360)
     * @param dec   Declination in degrees (-90 to +90)
     * @param color ARGB color
     * @param size  Size in pixels
     * @return A new PointPrimitive
     */
    @NonNull
    public static PointPrimitive create(float ra, float dec, int color, int size) {
        return new PointPrimitive(GeocentricCoords.fromDegrees(ra, dec), color, size, Shape.CIRCLE);
    }

    /**
     * Creates a simple point primitive with default styling.
     *
     * @param location The celestial coordinates
     * @return A new PointPrimitive with default color, size, and shape
     */
    @NonNull
    public static PointPrimitive create(@NonNull GeocentricCoords location) {
        return new PointPrimitive(location, DEFAULT_COLOR, DEFAULT_SIZE, Shape.CIRCLE);
    }

    /**
     * Creates a point primitive with specified color.
     *
     * @param location The celestial coordinates
     * @param color    ARGB color value
     * @return A new PointPrimitive
     */
    @NonNull
    public static PointPrimitive create(@NonNull GeocentricCoords location, int color) {
        return new PointPrimitive(location, color, DEFAULT_SIZE, Shape.CIRCLE);
    }

    /**
     * Creates a point primitive with specified color and size.
     *
     * @param location The celestial coordinates
     * @param color    ARGB color value
     * @param size     Size in pixels
     * @return A new PointPrimitive
     */
    @NonNull
    public static PointPrimitive create(@NonNull GeocentricCoords location, int color, int size) {
        return new PointPrimitive(location, color, size, Shape.CIRCLE);
    }

    /**
     * Creates a point primitive with all parameters.
     *
     * @param location The celestial coordinates
     * @param color    ARGB color value
     * @param size     Size in pixels
     * @param shape    Shape for rendering
     * @return A new PointPrimitive
     */
    @NonNull
    public static PointPrimitive create(@NonNull GeocentricCoords location, int color, int size,
                                         @NonNull Shape shape) {
        return new PointPrimitive(location, color, size, shape);
    }

    /**
     * Creates a point primitive from RA/Dec coordinates with defaults.
     *
     * @param ra  Right Ascension in degrees
     * @param dec Declination in degrees
     * @return A new PointPrimitive
     */
    @NonNull
    public static PointPrimitive fromRaDec(float ra, float dec) {
        return new PointPrimitive(GeocentricCoords.fromDegrees(ra, dec),
                DEFAULT_COLOR, DEFAULT_SIZE, Shape.CIRCLE);
    }

    /**
     * Creates a point primitive from a StarData object.
     *
     * <p>The shape is automatically set to STAR for bright stars (magnitude less than 2)
     * and CIRCLE for dimmer stars.</p>
     *
     * @param star The star data to create a primitive for
     * @return A new PointPrimitive configured for the star
     */
    @NonNull
    public static PointPrimitive fromStar(@NonNull StarData star) {
        Shape starShape = star.getMagnitude() < 2.0f ? Shape.STAR : Shape.CIRCLE;
        return new PointPrimitive(
                star.getCoordinates(),
                star.getColor(),
                star.getSize(),
                starShape
        );
    }

    /**
     * Returns the celestial location of this point.
     *
     * @return The geocentric coordinates
     */
    @NonNull
    public GeocentricCoords getLocation() {
        return location;
    }

    /**
     * Returns the Right Ascension in degrees.
     *
     * @return RA in degrees (0-360)
     */
    public float getRa() {
        return location.getRa();
    }

    /**
     * Returns the Declination in degrees.
     *
     * @return Dec in degrees (-90 to +90)
     */
    public float getDec() {
        return location.getDec();
    }

    /**
     * Returns the ARGB color for this point.
     *
     * @return ARGB color value
     */
    public int getColor() {
        return color;
    }

    /**
     * Returns the alpha component of the color.
     *
     * @return Alpha value (0-255)
     */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
     * Returns the red component of the color.
     *
     * @return Red value (0-255)
     */
    public int getRed() {
        return (color >> 16) & 0xFF;
    }

    /**
     * Returns the green component of the color.
     *
     * @return Green value (0-255)
     */
    public int getGreen() {
        return (color >> 8) & 0xFF;
    }

    /**
     * Returns the blue component of the color.
     *
     * @return Blue value (0-255)
     */
    public int getBlue() {
        return color & 0xFF;
    }

    /**
     * Returns the rendering size in pixels.
     *
     * @return Size in pixels at default zoom
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the shape for rendering.
     *
     * @return The rendering shape
     */
    @NonNull
    public Shape getShape() {
        return shape;
    }

    /**
     * Returns the location as a 3D unit vector.
     *
     * <p>The vector is on a unit sphere where:
     * <ul>
     *   <li>X points toward RA=0h, Dec=0</li>
     *   <li>Y points toward RA=6h, Dec=0</li>
     *   <li>Z points toward Dec=+90 (north celestial pole)</li>
     * </ul>
     * </p>
     *
     * @return float array [x, y, z]
     */
    @NonNull
    public float[] toVector3() {
        return location.toVector3();
    }

    /**
     * Creates a copy of this primitive with a different color.
     *
     * @param newColor The new ARGB color
     * @return A new PointPrimitive with the specified color
     */
    @NonNull
    public PointPrimitive withColor(int newColor) {
        return new PointPrimitive(location, newColor, size, shape);
    }

    /**
     * Creates a copy of this primitive with a different size.
     *
     * @param newSize The new size in pixels
     * @return A new PointPrimitive with the specified size
     */
    @NonNull
    public PointPrimitive withSize(int newSize) {
        return new PointPrimitive(location, color, newSize, shape);
    }

    /**
     * Creates a copy of this primitive with a different shape.
     *
     * @param newShape The new shape
     * @return A new PointPrimitive with the specified shape
     */
    @NonNull
    public PointPrimitive withShape(@NonNull Shape newShape) {
        return new PointPrimitive(location, color, size, newShape);
    }

    /**
     * Creates a copy of this primitive with modified alpha.
     *
     * @param alpha The new alpha value (0-255)
     * @return A new PointPrimitive with the specified alpha
     */
    @NonNull
    public PointPrimitive withAlpha(int alpha) {
        int newColor = (alpha << 24) | (color & 0x00FFFFFF);
        return new PointPrimitive(location, newColor, size, shape);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointPrimitive that = (PointPrimitive) o;
        return color == that.color &&
               size == that.size &&
               location.equals(that.location) &&
               shape == that.shape;
    }

    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + color;
        result = 31 * result + size;
        result = 31 * result + shape.hashCode();
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("PointPrimitive{ra=%.4f, dec=%.4f, color=0x%08X, size=%d, shape=%s}",
                location.getRa(), location.getDec(), color, size, shape);
    }

    /**
     * Builder for creating PointPrimitive instances with custom properties.
     *
     * <p>Example usage:
     * <pre>{@code
     * PointPrimitive point = new PointPrimitive.Builder()
     *     .setLocation(GeocentricCoords.fromDegrees(45.0f, 30.0f))
     *     .setColor(0xFFFF0000)
     *     .setSize(5)
     *     .setShape(Shape.STAR)
     *     .build();
     * }</pre>
     * </p>
     */
    public static class Builder {

        private GeocentricCoords location;
        private int color = DEFAULT_COLOR;
        private int size = DEFAULT_SIZE;
        private Shape shape = Shape.CIRCLE;

        /**
         * Creates a new Builder instance.
         */
        public Builder() {
        }

        /**
         * Sets the celestial location.
         *
         * @param location The geocentric coordinates
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setLocation(@NonNull GeocentricCoords location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the location from RA/Dec values.
         *
         * @param ra  Right Ascension in degrees
         * @param dec Declination in degrees
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setLocation(float ra, float dec) {
            this.location = GeocentricCoords.fromDegrees(ra, dec);
            return this;
        }

        /**
         * Sets the ARGB color.
         *
         * @param color ARGB color value
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setColor(int color) {
            this.color = color;
            return this;
        }

        /**
         * Sets the rendering size.
         *
         * @param size Size in pixels
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setSize(int size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the rendering shape.
         *
         * @param shape The shape to use
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setShape(@NonNull Shape shape) {
            this.shape = shape;
            return this;
        }

        /**
         * Builds the PointPrimitive instance.
         *
         * @return A new PointPrimitive
         * @throws IllegalStateException if location is not set
         */
        @NonNull
        public PointPrimitive build() {
            if (location == null) {
                throw new IllegalStateException("PointPrimitive requires a location");
            }
            return new PointPrimitive(location, color, size, shape);
        }
    }
}
