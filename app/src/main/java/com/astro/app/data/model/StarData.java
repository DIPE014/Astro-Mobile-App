package com.astro.app.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Represents a star with its astronomical properties and rendering attributes.
 *
 * <p>Extends {@link CelestialObject} with star-specific properties such as
 * magnitude (brightness), spectral type, and distance.</p>
 *
 * <h3>Magnitude System:</h3>
 * <p>The magnitude scale is logarithmic and inverted - brighter stars have
 * lower (or negative) magnitudes:
 * <ul>
 *   <li>-1.46: Sirius (brightest star)</li>
 *   <li>0.0: Vega (reference star)</li>
 *   <li>+6.0: Faintest visible to naked eye</li>
 * </ul>
 * </p>
 *
 * <h3>Spectral Types:</h3>
 * <p>The spectral classification indicates the star's temperature and color:
 * <ul>
 *   <li>O, B: Blue/blue-white (hot)</li>
 *   <li>A, F: White/yellow-white</li>
 *   <li>G: Yellow (like our Sun)</li>
 *   <li>K, M: Orange/red (cool)</li>
 * </ul>
 * </p>
 *
 * @see CelestialObject
 */
public class StarData extends CelestialObject {

    /** Represents unknown distance */
    public static final float DISTANCE_UNKNOWN = -1.0f;

    /** Default rendering size for stars */
    private static final int DEFAULT_SIZE = 3;

    /** Apparent magnitude (brightness) - lower values are brighter */
    private final float magnitude;

    /** Spectral classification (e.g., "G2V" for the Sun) */
    @Nullable
    private final String spectralType;

    /** Distance from Earth in light years, or DISTANCE_UNKNOWN if not known */
    private final float distance;

    /** ID of the constellation this star belongs to (if any) */
    @Nullable
    private final String constellationId;

    /** Size for rendering (in pixels at default zoom) */
    private final int size;

    /**
     * Private constructor. Use {@link Builder} to create instances.
     *
     * @param builder The builder containing star properties
     */
    private StarData(@NonNull Builder builder) {
        super(builder);
        this.magnitude = builder.magnitude;
        this.spectralType = builder.spectralType;
        this.distance = builder.distance;
        this.constellationId = builder.constellationId;
        this.size = builder.size;
    }

    /**
     * The apparent magnitude (brightness) of this star.
     *
     * Lower values indicate brighter stars. Typical visible range is approximately -1.5 to +6.0.
     *
     * @return the apparent magnitude
     */
    public float getMagnitude() {
        return magnitude;
    }

    /**
         * Spectral classification string for the star.
         *
         * <p>Examples: "O5", "B2", "A0", "F5", "G2V", "K5", "M0"</p>
         *
         * @return the spectral type (e.g., "G2V"), or null if unknown
         */
    @Nullable
    public String getSpectralType() {
        return spectralType;
    }

    /**
     * The distance from Earth to this star in light years.
     *
     * @return the distance in light years, or {@link #DISTANCE_UNKNOWN} if unknown
     */
    public float getDistance() {
        return distance;
    }

    /**
     * Indicates whether the star's distance is known.
     *
     * @return `true` if distance is greater than zero and not `DISTANCE_UNKNOWN`, `false` otherwise
     */
    public boolean hasKnownDistance() {
        return distance != DISTANCE_UNKNOWN && distance > 0;
    }

    /**
     * Identifier of the constellation that contains this star.
     *
     * @return the constellation ID, or null if the star has no assigned constellation
     */
    @Nullable
    public String getConstellationId() {
        return constellationId;
    }

    /**
     * Indicates whether the star has a constellation identifier.
     *
     * @return `true` if the constellation ID is non-null and not empty, `false` otherwise
     */
    public boolean hasConstellation() {
        return constellationId != null && !constellationId.isEmpty();
    }

    /**
     * Rendering size of the star in pixels at the default zoom level.
     *
     * <p>Brighter stars are typically assigned larger sizes for rendering.</p>
     *
     * @return Size in pixels at the default zoom level.
     */
    public int getSize() {
        return size;
    }

    /**
     * Calculates the luminosity ratio relative to an object with the given reference magnitude.
     *
     * <p>Uses the formula {@code 10^((referenceMagnitude - magnitude) / 2.5)}.</p>
     *
     * @param referenceMagnitude the reference magnitude to compare against
     * @return the multiplicative luminosity ratio — `>1` if this star is more luminous than the reference magnitude, `1` if equal, `<1` otherwise
     */
    public float getLuminosityRatio(float referenceMagnitude) {
        return (float) Math.pow(10, (referenceMagnitude - magnitude) / 2.5);
    }

    /**
     * Determines whether this star is visible to the naked eye.
     *
     * @return true if the star's apparent magnitude is less than or equal to 6.0, false otherwise.
     */
    public boolean isNakedEyeVisible() {
        return magnitude <= 6.0f;
    }

    /**
     * Maps the star's spectral class to a representative ARGB color.
     *
     * <p>Spectral classes O, B, A, F, G, K, and M are mapped to typical blue-to-red star colors;
     * if the spectral type is unset or unrecognized, the star's default color is returned.</p>
     *
     * @return ARGB integer encoding the color for the star's spectral class, or the object's default color if unset or unknown
     */
    public int getSpectralColor() {
        if (spectralType == null || spectralType.isEmpty()) {
            return getColor();
        }

        char spectralClass = spectralType.charAt(0);
        switch (spectralClass) {
            case 'O':
                return 0xFF9BB0FF; // Blue
            case 'B':
                return 0xFFAABFFF; // Blue-white
            case 'A':
                return 0xFFCAD7FF; // White
            case 'F':
                return 0xFFF8F7FF; // Yellow-white
            case 'G':
                return 0xFFFFF4EA; // Yellow
            case 'K':
                return 0xFFFFD2A1; // Orange
            case 'M':
                return 0xFFFFCC6F; // Orange-red
            default:
                return getColor();
        }
    }

    /**
     * Provides a concise one-line representation of the star's identifying and observational fields.
     *
     * @return a formatted string containing the star's id, name, right ascension (ra), declination (dec), magnitude, and spectral type, e.g. "StarData{id='...', name='...', ra=..., dec=..., mag=..., type='...'}".
     */
    @Override
    @NonNull
    public String toString() {
        return String.format("StarData{id='%s', name='%s', ra=%.4f, dec=%.4f, mag=%.2f, type='%s'}",
                getId(), getName(), getRa(), getDec(), magnitude, spectralType);
    }

    /**
     * Creates a new Builder instance.
     *
     * @return A new Builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating StarData instances.
     *
     * <p>Example usage:
     * <pre>{@code
     * StarData sirius = StarData.builder()
     *     .setId("sirius")
     *     .setName("Sirius")
     *     .setRa(101.2875f)
     *     .setDec(-16.7161f)
     *     .setMagnitude(-1.46f)
     *     .setSpectralType("A1V")
     *     .setDistance(8.6f)
     *     .setConstellationId("CMa")
     *     .build();
     * }</pre>
     * </p>
     */
    public static class Builder extends CelestialObject.Builder<Builder> {

        private float magnitude = 0.0f;
        @Nullable
        private String spectralType = null;
        private float distance = DISTANCE_UNKNOWN;
        @Nullable
        private String constellationId = null;
        private int size = DEFAULT_SIZE;

        /**
         * Creates a new Builder instance.
         */
        public Builder() {
            super();
        }

        /**
         * Return the current Builder to enable fluent method chaining.
         *
         * @return this Builder instance
         */
        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Set the apparent magnitude (brightness) for the star.
         *
         * @param magnitude apparent magnitude; lower values indicate a brighter star
         * @return this Builder for method chaining
         */
        @NonNull
        public Builder setMagnitude(float magnitude) {
            this.magnitude = magnitude;
            return this;
        }

        /**
                 * Set the star's spectral type classification.
                 *
                 * @param spectralType the spectral type (e.g., "G2V"); may be {@code null} to unset
                 * @return this builder for method chaining
                 */
        @NonNull
        public Builder setSpectralType(@Nullable String spectralType) {
            this.spectralType = spectralType;
            return this;
        }

        /**
         * Sets the distance from Earth in light years.
         *
         * @param distance Distance in light years, or {@link StarData#DISTANCE_UNKNOWN}
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setDistance(float distance) {
            this.distance = distance;
            return this;
        }

        /**
                 * Set the constellation identifier for the star.
                 *
                 * @param constellationId the constellation abbreviation (e.g., "Ori" for Orion), or {@code null} to unset
                 * @return this Builder instance for chaining
                 */
        @NonNull
        public Builder setConstellationId(@Nullable String constellationId) {
            this.constellationId = constellationId;
            return this;
        }

        /**
         * Sets the rendering size.
         *
         * @param size Size in pixels at default zoom
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setSize(int size) {
            this.size = size;
            return this;
        }

        /**
         * Calculates and sets the size based on magnitude.
         *
         * <p>Brighter stars (lower magnitude) get larger sizes.</p>
         *
         * @return This builder for method chaining
         */
        @NonNull
        public Builder calculateSizeFromMagnitude() {
            // Map magnitude range [-1.5, 6.5] to size range [8, 1]
            float normalizedMag = Math.max(-1.5f, Math.min(6.5f, magnitude));
            this.size = (int) (8 - ((normalizedMag + 1.5f) / 8.0f) * 7);
            this.size = Math.max(1, Math.min(8, this.size));
            return this;
        }

        /**
         * Create a StarData instance from this builder's configured values.
         *
         * @return a validated {@link StarData} constructed from this builder
         */
        @Override
        @NonNull
        public StarData build() {
            validate();
            return new StarData(this);
        }
    }
}