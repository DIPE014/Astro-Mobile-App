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
     * Create a PointPrimitive with the specified location, ARGB color, size in pixels, and shape.
     *
     * @param location celestial coordinates (non-null)
     * @param color    ARGB color value (0xAARRGGBB)
     * @param size     diameter in pixels at default zoom
     * @param shape    rendering shape (non-null)
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
         * Create a PointPrimitive at the given celestial location with the specified ARGB color.
         *
         * @param location celestial coordinates (non-null)
         * @param color    ARGB color value
         * @return the PointPrimitive with the given location and color, using the default size and a circular shape
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
     * Create a PointPrimitive with the specified location, ARGB color, size, and shape.
     *
     * @param location the celestial coordinates (non-null)
     * @param color    ARGB color packed into an int (0xAARRGGBB)
     * @param size     diameter in pixels at default zoom
     * @param shape    rendering shape (non-null)
     * @return         the created PointPrimitive
     */
    @NonNull
    public static PointPrimitive create(@NonNull GeocentricCoords location, int color, int size,
                                         @NonNull Shape shape) {
        return new PointPrimitive(location, color, size, shape);
    }

    /**
         * Create a PointPrimitive at the given celestial coordinates using the default color, size, and circle shape.
         *
         * @param ra  Right Ascension in degrees
         * @param dec Declination in degrees
         * @return a PointPrimitive positioned at the specified RA/Dec with default styling
         */
    @NonNull
    public static PointPrimitive fromRaDec(float ra, float dec) {
        return new PointPrimitive(GeocentricCoords.fromDegrees(ra, dec),
                DEFAULT_COLOR, DEFAULT_SIZE, Shape.CIRCLE);
    }

    /**
         * Creates a PointPrimitive representing the given star.
         *
         * <p>The primitive uses the star's coordinates, color, and size. If the star's magnitude
         * is less than 2.0 the shape is set to {@link Shape#STAR}; otherwise the shape is
         * {@link Shape#CIRCLE}.</p>
         *
         * @param star the star data to convert (non-null)
         * @return the PointPrimitive corresponding to the provided star
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
     * The celestial (geocentric) coordinates of this point.
     *
     * @return the non-null {@link GeocentricCoords} representing the point's celestial location
     */
    @NonNull
    public GeocentricCoords getLocation() {
        return location;
    }

    /**
     * Get the right ascension in degrees.
     *
     * @return the right ascension in degrees in the range [0, 360)
     */
    public float getRa() {
        return location.getRa();
    }

    /**
     * Gets the celestial Declination in degrees.
     *
     * @return the Declination in degrees, from -90 to +90
     */
    public float getDec() {
        return location.getDec();
    }

    /**
     * Get the point's color as a packed ARGB integer.
     *
     * @return the color encoded as an ARGB-packed int (alpha in bits 24–31, red in 16–23, green in 8–15, blue in 0–7)
     */
    public int getColor() {
        return color;
    }

    /**
     * Alpha component of the ARGB color.
     *
     * @return the alpha component (0-255)
     */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
     * Get the red component of this primitive's ARGB color.
     *
     * @return the red component value in the range 0-255
     */
    public int getRed() {
        return (color >> 16) & 0xFF;
    }

    /**
     * Gets the green component of the ARGB color.
     *
     * @return the green component (0-255)
     */
    public int getGreen() {
        return (color >> 8) & 0xFF;
    }

    /**
     * Gets the blue component of this primitive's ARGB color.
     *
     * @return the blue component (0–255)
     */
    public int getBlue() {
        return color & 0xFF;
    }

    /**
     * Provides the rendering diameter in pixels at the default zoom level.
     *
     * @return the diameter in pixels at default zoom.
     */
    public int getSize() {
        return size;
    }

    /**
         * The rendering shape of this point primitive.
         *
         * @return the shape used to render the point
         */
    @NonNull
    public Shape getShape() {
        return shape;
    }

    /**
     * Converts the point's celestial coordinates to a 3D unit vector on the celestial unit sphere.
     *
     * <p>Axes: X points toward RA=0h, Dec=0; Y points toward RA=6h, Dec=0; Z points toward Dec=+90° (north celestial pole).</p>
     *
     * @return a length-3 float array {x, y, z} representing the unit vector
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
         * Create a PointPrimitive with the same location, color, and shape but a different size.
         *
         * @param newSize the diameter in pixels at default zoom
         * @return a new PointPrimitive with the specified size
         */
    @NonNull
    public PointPrimitive withSize(int newSize) {
        return new PointPrimitive(location, color, newSize, shape);
    }

    /**
         * Creates a copy of this primitive with the specified shape.
         *
         * @param newShape the shape to set on the returned primitive
         * @return a PointPrimitive with the same location, color, and size, and the specified shape
         */
    @NonNull
    public PointPrimitive withShape(@NonNull Shape newShape) {
        return new PointPrimitive(location, color, size, newShape);
    }

    /**
         * Return a copy of this primitive with its alpha channel set to the given value.
         *
         * @param alpha the new alpha value in the range 0–255
         * @return a PointPrimitive with the same location, size, and shape and with RGB channels preserved but alpha set to `alpha`
         */
    @NonNull
    public PointPrimitive withAlpha(int alpha) {
        int newColor = (alpha << 24) | (color & 0x00FFFFFF);
        return new PointPrimitive(location, newColor, size, shape);
    }

    /**
     * Determines whether this PointPrimitive is equal to another object based on its identifying fields.
     *
     * Two PointPrimitive instances are equal if they are the same class and have equal location, color,
     * size, and shape.
     *
     * @param o the object to compare with this instance
     * @return {@code true} if the given object is a PointPrimitive with the same location, color, size,
     *         and shape; {@code false} otherwise
     */
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

    /**
     * Compute a hash code for this PointPrimitive.
     *
     * @return the hash code derived from the location, color, size, and shape
     */
    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + color;
        result = 31 * result + size;
        result = 31 * result + shape.hashCode();
        return result;
    }

    /**
     * Provides a concise textual representation of this point primitive including RA, Dec, color, size, and shape.
     *
     * @return the string formatted as PointPrimitive{ra=%.4f, dec=%.4f, color=0x%08X, size=%d, shape=%s}
     */
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
         * Creates a new Builder for constructing a PointPrimitive.
         *
         * The builder is initialized with default color, size, and shape; the location is unset
         * and must be provided before calling {@link Builder#build()}.
         */
        public Builder() {
        }

        /**
         * Set the celestial location used when building the PointPrimitive.
         *
         * @param location the geocentric celestial coordinates to use
         * @return this Builder instance for method chaining
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
                 * Set the ARGB color for the PointPrimitive being built.
                 *
                 * @param color ARGB color in 0xAARRGGBB format
                 * @return this builder for method chaining
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
         * Constructs a PointPrimitive from the builder's configured properties.
         *
         * @return a new PointPrimitive with the builder's location, color, size, and shape
         * @throws IllegalStateException if location has not been set
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