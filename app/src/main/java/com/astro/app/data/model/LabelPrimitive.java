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
     * Constructs a LabelPrimitive from right ascension and declination.
     *
     * @param ra    Right ascension in degrees (0–360)
     * @param dec   Declination in degrees (−90 to +90)
     * @param text  Label text; must be non-null and non-empty
     * @param color ARGB color
     * @throws IllegalArgumentException if {@code text} is null or empty
     */
    public LabelPrimitive(float ra, float dec, @NonNull String text, int color) {
        this(GeocentricCoords.fromDegrees(ra, dec), text, color, DEFAULT_OFFSET, DEFAULT_FONT_SIZE);
    }

    /**
     * Constructs a LabelPrimitive at the specified geocentric coordinates with the given text and color.
     *
     * @param location geocentric position for the label; must not be null
     * @param text     label text; must not be null or empty
     * @param color    ARGB color value for the label
     * @throws IllegalArgumentException if {@code text} is null or empty
     */
    public LabelPrimitive(@NonNull GeocentricCoords location, @NonNull String text, int color) {
        this(location, text, color, DEFAULT_OFFSET, DEFAULT_FONT_SIZE);
    }

    /**
     * Constructs a LabelPrimitive positioned at the given celestial location with the specified
     * text, color, offset, and font size.
     *
     * @param location Geocentric coordinates of the label's position (must be non-null)
     * @param text     Label content; must be non-null and not empty after trimming
     * @param color    ARGB color value for the text
     * @param offset   Positional offset from the location
     * @param fontSize Font size in scaled pixels
     * @throws IllegalArgumentException if {@code text} is null or empty
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
     * Create a label primitive positioned at the given celestial coordinates using default color and font size.
     *
     * @return a new LabelPrimitive positioned at the provided location with the given text and default styling
     */
    @NonNull
    public static LabelPrimitive create(@NonNull GeocentricCoords location, @NonNull String text) {
        return new LabelPrimitive(location, text, DEFAULT_COLOR);
    }

    /**
     * Creates a label primitive positioned at the given geocentric coordinates with the specified text and color.
     *
     * @param location geocentric coordinates for the label's position
     * @param text     label text content
     * @param color    ARGB color value for the label
     * @return the label primitive configured with the given location, text, and color
     */
    @NonNull
    public static LabelPrimitive create(@NonNull GeocentricCoords location, @NonNull String text, int color) {
        return new LabelPrimitive(location, text, color);
    }

    /**
     * Create a label positioned at the specified celestial coordinates using the default styling.
     *
     * @param ra   Right Ascension in degrees.
     * @param dec  Declination in degrees.
     * @param text Label text (must be non-null and non-empty).
     * @return      a LabelPrimitive positioned at the given RA/Dec using the default color, offset, and font size.
     */
    @NonNull
    public static LabelPrimitive fromRaDec(float ra, float dec, @NonNull String text) {
        return new LabelPrimitive(ra, dec, text, DEFAULT_COLOR);
    }

    /**
         * Create a label primitive representing the given celestial object.
         *
         * @param object the celestial object whose name, coordinates, and color are used for the label
         * @return a LabelPrimitive positioned at the object's coordinates with the object's name and color
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
         * The celestial location of this label.
         *
         * @return the geocentric coordinates representing the label's celestial position
         */
    @NonNull
    public GeocentricCoords getLocation() {
        return location;
    }

    /**
     * Get the right ascension in degrees.
     *
     * @return the right ascension in degrees, in the range 0–360
     */
    public float getRa() {
        return location.getRa();
    }

    /**
     * Declination of the label's location in degrees.
     *
     * @return Declination in degrees, between -90 and +90.
     */
    public float getDec() {
        return location.getDec();
    }

    /**
         * Retrieve the label's text content.
         *
         * @return the label's text
         */
    @NonNull
    public String getText() {
        return text;
    }

    /**
     * Alias for getText that provides the label's text content.
     *
     * @return the label's text content
     */
    @NonNull
    public String getLabel() {
        return text;
    }

    /**
     * Update the label's displayed text.
     *
     * @param text the new text for the label; must be non-null and not empty after trimming
     * @throws IllegalArgumentException if {@code text} is null or empty after trimming
     */
    public void setText(@NonNull String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Label text cannot be null or empty");
        }
        this.text = text;
    }

    /**
     * The ARGB color used to render the label.
     *
     * @return the color as an ARGB-packed int
     */
    public int getColor() {
        return color;
    }

    /**
     * Retrieves the alpha component of the label's ARGB color.
     *
     * @return the alpha component (0-255)
     */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
     * Font size used to render the label, expressed in scaled pixels.
     *
     * @return the font size in scaled pixels
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * Positional offset applied to the label to separate it from its anchored location.
     *
     * @return the offset value used to displace the label from its location
     */
    public float getOffset() {
        return offset;
    }

    /**
         * Get the location as a 3D unit vector.
         *
         * @return a float array of length 3 containing the unit vector components in order: x, y, z
         */
    @NonNull
    public float[] toVector3() {
        return location.toVector3();
    }

    /**
     * Gets the number of characters in the label's text.
     *
     * @return the number of characters in the label's text
     */
    public int getTextLength() {
        return text.length();
    }

    /**
     * Determines whether the label contains non-empty text (ignoring surrounding whitespace).
     *
     * @return true if the label contains one or more non-whitespace characters, false otherwise.
     */
    public boolean hasContent() {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Return a new label identical to this one but using the provided text.
     *
     * @param newText the text for the new label (must be non-null and non-empty)
     * @return a new LabelPrimitive with the same location, color, offset, and font size, but with `newText`
     * @throws IllegalArgumentException if `newText` is null or empty
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
     * Create a label identical to this one but using the specified positional offset.
     *
     * @param newOffset the positional offset from the label's location
     * @return a new LabelPrimitive identical to this one except with the given offset
     */
    @NonNull
    public LabelPrimitive withOffset(float newOffset) {
        return new LabelPrimitive(location, text, color, newOffset, fontSize);
    }

    /**
     * Creates a copy of this label with its color alpha component set to the given value.
     *
     * @param alpha the new alpha value in the range 0–255
     * @return a LabelPrimitive identical to this one except the color's alpha is set to the given value
     */
    @NonNull
    public LabelPrimitive withAlpha(int alpha) {
        int newColor = (alpha << 24) | (color & 0x00FFFFFF);
        return new LabelPrimitive(location, text, newColor, offset, fontSize);
    }

    /**
     * Determines whether the given object represents the same label primitive.
     *
     * @param o the object to compare with this label
     * @return `true` if {@code o} is a {@code LabelPrimitive} whose location, text, color,
     *         font size, and offset are equal to this instance's; `false` otherwise
     */
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

    /**
     * Compute a hash code based on the label's location, text, color, font size, and offset.
     *
     * @return an int hash code derived from the label's `location`, `text`, `color`, `fontSize`, and `offset`
     */
    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + text.hashCode();
        result = 31 * result + color;
        result = 31 * result + fontSize;
        result = 31 * result + (offset != +0.0f ? Float.floatToIntBits(offset) : 0);
        return result;
    }

    /**
     * Provide a concise textual representation of the label including its text, celestial coordinates, color, and font size.
     *
     * @return a {@code String} formatted as {@code "LabelPrimitive{text='...', ra=..., dec=..., color=0xAARRGGBB, fontSize=...}"} containing the label's content, RA, Dec, ARGB color in hex, and font size.
     */
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
         * Set the positional offset applied when rendering the label relative to its location.
         *
         * @param offset the offset distance to apply from the label's celestial location
         * @return this builder for method chaining
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