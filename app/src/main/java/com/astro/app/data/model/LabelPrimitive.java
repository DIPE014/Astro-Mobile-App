package com.astro.app.data.model;

import androidx.annotation.NonNull;

/**
 * A rendering primitive representing a text label in the sky.
 *
 * <p>LabelPrimitives are used to render star names, constellation names,
 * coordinate labels, and other text annotations on the sky map. Each label
 * has a celestial position, text content, and visual styling properties.</p>
 *
 * <h3>Rendering Properties:</h3>
 * <ul>
 *   <li><b>location</b> - Celestial coordinates (RA/Dec) for label placement</li>
 *   <li><b>text</b> - The text content to display</li>
 *   <li><b>color</b> - ARGB color for the text</li>
 *   <li><b>fontSize</b> - Font size in scaled pixels (sp)</li>
 * </ul>
 *
 * <h3>Positioning:</h3>
 * <p>Labels are positioned at the specified celestial coordinates. The text
 * is typically rendered with a small offset from the point to avoid overlapping
 * with the associated celestial object.</p>
 *
 * @see PointPrimitive
 * @see LinePrimitive
 * @see GeocentricCoords
 */
public class LabelPrimitive {

    /** Default white color */
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    /** Default font size in scaled pixels */
    public static final int DEFAULT_FONT_SIZE = 15;

    /** Default offset from the location */
    public static final float DEFAULT_OFFSET = 0.02f;

    /** The celestial location for this label */
    @NonNull
    private final GeocentricCoords location;

    /** Text content of the label */
    @NonNull
    private String text;

    /** ARGB color of the text */
    private final int color;

    /** Font size in scaled pixels */
    private final int fontSize;

    /** Offset from the location for positioning */
    private final float offset;

    /**
     * Creates a label primitive from Right Ascension and Declination.
     *
     * @param ra    Right Ascension in degrees (0-360)
     * @param dec   Declination in degrees (-90 to +90)
     * @param text  Text content
     * @param color ARGB color
     */
    public LabelPrimitive(float ra, float dec, @NonNull String text, int color) {
        this(GeocentricCoords.fromDegrees(ra, dec), text, color, DEFAULT_OFFSET, DEFAULT_FONT_SIZE);
    }

    /**
     * Creates a label primitive from geocentric coordinates.
     *
     * @param location Geocentric coordinates
     * @param text     Text content
     * @param color    ARGB color
     */
    public LabelPrimitive(@NonNull GeocentricCoords location, @NonNull String text, int color) {
        this(location, text, color, DEFAULT_OFFSET, DEFAULT_FONT_SIZE);
    }

    /**
     * Creates a label primitive with all parameters.
     *
     * @param location Geocentric coordinates
     * @param text     Text content
     * @param color    ARGB color
     * @param offset   Offset from location
     * @param fontSize Font size in scaled pixels
     * @throws IllegalArgumentException if text is null or empty
     */
    public LabelPrimitive(@NonNull GeocentricCoords location, @NonNull String text, int color,
                          float offset, int fontSize) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Label text cannot be null or empty");
        }
        this.location = location;
        this.text = text;
        this.color = color;
        this.offset = offset;
        this.fontSize = fontSize;
    }

    /**
     * Creates a label primitive with default styling.
     *
     * @param location Geocentric coordinates
     * @param text     Text content
     * @return A new LabelPrimitive with default color and font size
     */
    @NonNull
    public static LabelPrimitive create(@NonNull GeocentricCoords location, @NonNull String text) {
        return new LabelPrimitive(location, text, DEFAULT_COLOR);
    }

    /**
     * Creates a label primitive with specified color.
     *
     * @param location Geocentric coordinates
     * @param text     Text content
     * @param color    ARGB color value
     * @return A new LabelPrimitive with the specified color
     */
    @NonNull
    public static LabelPrimitive create(@NonNull GeocentricCoords location, @NonNull String text, int color) {
        return new LabelPrimitive(location, text, color);
    }

    /**
     * Creates a label primitive from RA/Dec with default styling.
     *
     * @param ra   Right Ascension in degrees
     * @param dec  Declination in degrees
     * @param text Text content
     * @return A new LabelPrimitive
     */
    @NonNull
    public static LabelPrimitive fromRaDec(float ra, float dec, @NonNull String text) {
        return new LabelPrimitive(ra, dec, text, DEFAULT_COLOR);
    }

    /**
     * Creates a label primitive from a CelestialObject.
     *
     * @param object The celestial object to create a label for
     * @return A new LabelPrimitive using the object's name and position
     */
    @NonNull
    public static LabelPrimitive fromCelestialObject(@NonNull CelestialObject object) {
        return new LabelPrimitive(
                object.getCoordinates(),
                object.getName(),
                object.getColor(),
                DEFAULT_OFFSET,
                DEFAULT_FONT_SIZE
        );
    }

    /**
     * Creates a label primitive from a StarData object.
     *
     * @param star The star to create a label for
     * @return A new LabelPrimitive using the star's name and position
     */
    @NonNull
    public static LabelPrimitive fromStar(@NonNull StarData star) {
        return fromCelestialObject(star);
    }

    /**
     * Returns the celestial location of this label.
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
     * Returns the text content of the label.
     *
     * @return Label text
     */
    @NonNull
    public String getText() {
        return text;
    }

    /**
     * Returns the text content (alias for getText).
     *
     * @return Label text
     */
    @NonNull
    public String getLabel() {
        return text;
    }

    /**
     * Sets the text content of the label.
     *
     * @param text New text content
     * @throws IllegalArgumentException if text is null or empty
     */
    public void setText(@NonNull String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Label text cannot be null or empty");
        }
        this.text = text;
    }

    /**
     * Returns the ARGB color.
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
     * Returns the font size in scaled pixels.
     *
     * @return Font size
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * Returns the offset from the location.
     *
     * <p>This offset is used to position the label slightly away from
     * its associated point to prevent overlap.</p>
     *
     * @return Offset value
     */
    public float getOffset() {
        return offset;
    }

    /**
     * Returns the location as a 3D unit vector.
     *
     * @return float array [x, y, z]
     */
    @NonNull
    public float[] toVector3() {
        return location.toVector3();
    }

    /**
     * Returns the length of the text content.
     *
     * @return Number of characters in the label
     */
    public int getTextLength() {
        return text.length();
    }

    /**
     * Checks if this label has non-trivial content.
     *
     * @return true if the label has visible text content
     */
    public boolean hasContent() {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Creates a copy of this label with a different text.
     *
     * @param newText The new text content
     * @return A new LabelPrimitive with the specified text
     */
    @NonNull
    public LabelPrimitive withText(@NonNull String newText) {
        return new LabelPrimitive(location, newText, color, offset, fontSize);
    }

    /**
     * Creates a copy of this label with a different color.
     *
     * @param newColor The new ARGB color
     * @return A new LabelPrimitive with the specified color
     */
    @NonNull
    public LabelPrimitive withColor(int newColor) {
        return new LabelPrimitive(location, text, newColor, offset, fontSize);
    }

    /**
     * Creates a copy of this label with a different font size.
     *
     * @param newFontSize The new font size in scaled pixels
     * @return A new LabelPrimitive with the specified font size
     */
    @NonNull
    public LabelPrimitive withFontSize(int newFontSize) {
        return new LabelPrimitive(location, text, color, offset, newFontSize);
    }

    /**
     * Creates a copy of this label with a different offset.
     *
     * @param newOffset The new offset value
     * @return A new LabelPrimitive with the specified offset
     */
    @NonNull
    public LabelPrimitive withOffset(float newOffset) {
        return new LabelPrimitive(location, text, color, newOffset, fontSize);
    }

    /**
     * Creates a copy of this label with modified alpha.
     *
     * @param alpha The new alpha value (0-255)
     * @return A new LabelPrimitive with the specified alpha
     */
    @NonNull
    public LabelPrimitive withAlpha(int alpha) {
        int newColor = (alpha << 24) | (color & 0x00FFFFFF);
        return new LabelPrimitive(location, text, newColor, offset, fontSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelPrimitive that = (LabelPrimitive) o;
        return color == that.color &&
               fontSize == that.fontSize &&
               Float.compare(that.offset, offset) == 0 &&
               location.equals(that.location) &&
               text.equals(that.text);
    }

    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + text.hashCode();
        result = 31 * result + color;
        result = 31 * result + fontSize;
        result = 31 * result + (offset != +0.0f ? Float.floatToIntBits(offset) : 0);
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("LabelPrimitive{text='%s', ra=%.4f, dec=%.4f, color=0x%08X, fontSize=%d}",
                text, location.getRa(), location.getDec(), color, fontSize);
    }

    /**
     * Builder for creating LabelPrimitive instances.
     *
     * <p>Example usage:
     * <pre>{@code
     * LabelPrimitive label = new LabelPrimitive.Builder()
     *     .setLocation(GeocentricCoords.fromDegrees(45.0f, 30.0f))
     *     .setText("Sirius")
     *     .setColor(0xFFFFFF00)
     *     .setFontSize(18)
     *     .build();
     * }</pre>
     * </p>
     */
    public static class Builder {

        private GeocentricCoords location;
        private String text;
        private int color = DEFAULT_COLOR;
        private int fontSize = DEFAULT_FONT_SIZE;
        private float offset = DEFAULT_OFFSET;

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
         * Sets the text content.
         *
         * @param text The label text
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setText(@NonNull String text) {
            this.text = text;
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
         * Sets the font size.
         *
         * @param fontSize Font size in scaled pixels
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setFontSize(int fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        /**
         * Sets the offset from the location.
         *
         * @param offset Offset value
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setOffset(float offset) {
            this.offset = offset;
            return this;
        }

        /**
         * Builds the LabelPrimitive instance.
         *
         * @return A new LabelPrimitive
         * @throws IllegalStateException if location or text is not set
         */
        @NonNull
        public LabelPrimitive build() {
            if (location == null) {
                throw new IllegalStateException("LabelPrimitive requires a location");
            }
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalStateException("LabelPrimitive requires non-empty text");
            }
            return new LabelPrimitive(location, text, color, offset, fontSize);
        }
    }
}
