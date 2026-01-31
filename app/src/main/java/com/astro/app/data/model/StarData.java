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

    /** Represents unknown Hipparcos ID */
    public static final int HIPPARCOS_UNKNOWN = -1;

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

    /** Hipparcos catalog ID, or HIPPARCOS_UNKNOWN if not in the catalog */
    private final int hipparcosId;

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
        this.hipparcosId = builder.hipparcosId;
    }

    /**
     * Returns the apparent magnitude (brightness) of this star.
     *
     * <p>Lower values indicate brighter stars. Typical visible range is
     * approximately -1.5 to +6.0.</p>
     *
     * @return The apparent magnitude
     */
    public float getMagnitude() {
        return magnitude;
    }

    /**
     * Returns the spectral type classification.
     *
     * <p>Examples: "O5", "B2", "A0", "F5", "G2V", "K5", "M0"</p>
     *
     * @return The spectral type, or null if unknown
     */
    @Nullable
    public String getSpectralType() {
        return spectralType;
    }

    /**
     * Returns the distance from Earth in light years.
     *
     * @return Distance in light years, or {@link #DISTANCE_UNKNOWN} if not known
     */
    public float getDistance() {
        return distance;
    }

    /**
     * Checks if the distance is known for this star.
     *
     * @return true if distance is known, false otherwise
     */
    public boolean hasKnownDistance() {
        return distance != DISTANCE_UNKNOWN && distance > 0;
    }

    /**
     * Returns the ID of the constellation this star belongs to.
     *
     * @return The constellation ID, or null if not assigned
     */
    @Nullable
    public String getConstellationId() {
        return constellationId;
    }

    /**
     * Checks if this star is assigned to a constellation.
     *
     * @return true if the star has a constellation assignment
     */
    public boolean hasConstellation() {
        return constellationId != null && !constellationId.isEmpty();
    }

    /**
     * Returns the rendering size for this star.
     *
     * <p>This is typically calculated based on magnitude - brighter stars
     * appear larger.</p>
     *
     * @return Size in pixels at default zoom level
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the Hipparcos catalog ID for this star.
     *
     * <p>The Hipparcos catalog contains about 118,000 stars with precise
     * positions and parallax measurements.</p>
     *
     * @return The Hipparcos ID, or {@link #HIPPARCOS_UNKNOWN} if not in the catalog
     */
    public int getHipparcosId() {
        return hipparcosId;
    }

    /**
     * Checks if this star has a Hipparcos catalog ID.
     *
     * @return true if the star has a valid Hipparcos ID
     */
    public boolean hasHipparcosId() {
        return hipparcosId != HIPPARCOS_UNKNOWN;
    }

    /**
     * Calculates the luminosity relative to a reference magnitude.
     *
     * <p>Uses the formula: ratio = 10^((refMag - thisMag) / 2.5)</p>
     *
     * @param referenceMagnitude The reference magnitude to compare against
     * @return The luminosity ratio
     */
    public float getLuminosityRatio(float referenceMagnitude) {
        return (float) Math.pow(10, (referenceMagnitude - magnitude) / 2.5);
    }

    /**
     * Checks if this star is visible to the naked eye.
     *
     * <p>Generally, stars with magnitude less than or equal to 6.0 are
     * visible under ideal conditions.</p>
     *
     * @return true if the star is visible to naked eye
     */
    public boolean isNakedEyeVisible() {
        return magnitude <= 6.0f;
    }

    /**
     * Returns a color based on the spectral type.
     *
     * <p>If no spectral type is set, returns a default white color.</p>
     *
     * @return ARGB color representing the star's spectral color
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
        private int hipparcosId = HIPPARCOS_UNKNOWN;

        /**
         * Creates a new Builder instance.
         */
        public Builder() {
            super();
        }

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Sets the apparent magnitude (brightness).
         *
         * @param magnitude The apparent magnitude (lower = brighter)
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setMagnitude(float magnitude) {
            this.magnitude = magnitude;
            return this;
        }

        /**
         * Sets the spectral type classification.
         *
         * @param spectralType The spectral type (e.g., "G2V")
         * @return This builder for method chaining
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
         * Sets the constellation ID this star belongs to.
         *
         * @param constellationId The constellation ID (e.g., "Ori" for Orion)
         * @return This builder for method chaining
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
         * Sets the Hipparcos catalog ID.
         *
         * @param hipparcosId The Hipparcos ID, or {@link StarData#HIPPARCOS_UNKNOWN}
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setHipparcosId(int hipparcosId) {
            this.hipparcosId = hipparcosId;
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

        @Override
        @NonNull
        public StarData build() {
            validate();
            return new StarData(this);
        }
    }
}
