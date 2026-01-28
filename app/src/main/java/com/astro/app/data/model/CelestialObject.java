package com.astro.app.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class representing any celestial object in the sky.
 *
 * <p>This is the abstract base for all celestial objects such as stars, planets,
 * deep sky objects, and other astronomical entities. It contains common properties
 * shared by all celestial objects.</p>
 *
 * <h3>Coordinate System:</h3>
 * <p>Positions are specified using the equatorial coordinate system:
 * <ul>
 *   <li><b>Right Ascension (RA)</b>: 0-360 degrees, measured eastward</li>
 *   <li><b>Declination (Dec)</b>: -90 to +90 degrees</li>
 * </ul>
 * </p>
 *
 * <p>This class uses the Builder pattern for object construction to handle
 * the many optional parameters.</p>
 *
 * @see StarData
 * @see GeocentricCoords
 */
public class CelestialObject {

    /** Unique identifier for this celestial object */
    @NonNull
    private final String id;

    /** Primary display name */
    @NonNull
    private final String name;

    /** Alternative names (e.g., catalog designations, historical names) */
    @NonNull
    private final List<String> alternateNames;

    /** Right Ascension in degrees (0-360) */
    private final float ra;

    /** Declination in degrees (-90 to +90) */
    private final float dec;

    /** ARGB color for rendering this object */
    private final int color;

    /**
     * Protected constructor for use by Builder and subclasses.
     *
     * @param builder The builder containing the object's properties
     */
    protected CelestialObject(@NonNull Builder<?> builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.alternateNames = Collections.unmodifiableList(new ArrayList<>(builder.alternateNames));
        this.ra = builder.ra;
        this.dec = builder.dec;
        this.color = builder.color;
    }

    /**
     * Returns the unique identifier for this celestial object.
     *
     * @return The object's unique ID
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Returns the primary display name of this object.
     *
     * @return The object's name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the list of alternate names for this object.
     *
     * <p>This may include catalog designations (e.g., "HD 48915"),
     * Bayer designations (e.g., "Alpha Canis Majoris"), or
     * historical names.</p>
     *
     * @return An unmodifiable list of alternate names
     */
    @NonNull
    public List<String> getAlternateNames() {
        return alternateNames;
    }

    /**
     * Returns the Right Ascension in degrees.
     *
     * @return Right Ascension in degrees (0-360)
     */
    public float getRa() {
        return ra;
    }

    /**
     * Returns the Declination in degrees.
     *
     * @return Declination in degrees (-90 to +90)
     */
    public float getDec() {
        return dec;
    }

    /**
     * Returns the geocentric coordinates of this object.
     *
     * @return GeocentricCoords representing this object's position
     */
    @NonNull
    public GeocentricCoords getCoordinates() {
        return GeocentricCoords.fromDegrees(ra, dec);
    }

    /**
     * Returns the ARGB color for rendering this object.
     *
     * @return ARGB color value
     */
    public int getColor() {
        return color;
    }

    /**
     * Checks if this object has any alternate names.
     *
     * @return true if the object has at least one alternate name
     */
    public boolean hasAlternateNames() {
        return !alternateNames.isEmpty();
    }

    /**
     * Calculates the angular distance from this object to another celestial object.
     *
     * @param other The other celestial object
     * @return Angular distance in degrees
     */
    public float angularDistanceTo(@NonNull CelestialObject other) {
        return getCoordinates().angularDistanceTo(other.getCoordinates());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CelestialObject that = (CelestialObject) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("CelestialObject{id='%s', name='%s', ra=%.4f, dec=%.4f}",
                id, name, ra, dec);
    }

    /**
     * Builder for creating CelestialObject instances.
     *
     * <p>Uses a generic self-referential type parameter to allow subclass
     * builders to return the correct builder type for method chaining.</p>
     *
     * @param <T> The concrete builder type for method chaining
     */
    public static class Builder<T extends Builder<T>> {

        /** Default white color for celestial objects */
        private static final int DEFAULT_COLOR = 0xFFFFFFFF;

        @NonNull
        private String id = "";

        @NonNull
        private String name = "";

        @NonNull
        private List<String> alternateNames = new ArrayList<>();

        private float ra = 0.0f;
        private float dec = 0.0f;
        private int color = DEFAULT_COLOR;

        /**
         * Creates a new Builder instance.
         */
        public Builder() {
        }

        /**
         * Returns this builder instance for method chaining.
         *
         * @return This builder instance
         */
        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        /**
         * Sets the unique identifier.
         *
         * @param id The unique identifier
         * @return This builder for method chaining
         */
        @NonNull
        public T setId(@NonNull String id) {
            this.id = id;
            return self();
        }

        /**
         * Sets the primary name.
         *
         * @param name The display name
         * @return This builder for method chaining
         */
        @NonNull
        public T setName(@NonNull String name) {
            this.name = name;
            return self();
        }

        /**
         * Sets the list of alternate names.
         *
         * @param alternateNames The list of alternate names
         * @return This builder for method chaining
         */
        @NonNull
        public T setAlternateNames(@Nullable List<String> alternateNames) {
            this.alternateNames = alternateNames != null ? alternateNames : new ArrayList<>();
            return self();
        }

        /**
         * Adds an alternate name.
         *
         * @param alternateName An alternate name to add
         * @return This builder for method chaining
         */
        @NonNull
        public T addAlternateName(@NonNull String alternateName) {
            this.alternateNames.add(alternateName);
            return self();
        }

        /**
         * Sets the Right Ascension in degrees.
         *
         * @param ra Right Ascension in degrees (0-360)
         * @return This builder for method chaining
         */
        @NonNull
        public T setRa(float ra) {
            this.ra = ra;
            return self();
        }

        /**
         * Sets the Declination in degrees.
         *
         * @param dec Declination in degrees (-90 to +90)
         * @return This builder for method chaining
         */
        @NonNull
        public T setDec(float dec) {
            this.dec = dec;
            return self();
        }

        /**
         * Sets the position using geocentric coordinates.
         *
         * @param coords The geocentric coordinates
         * @return This builder for method chaining
         */
        @NonNull
        public T setCoordinates(@NonNull GeocentricCoords coords) {
            this.ra = coords.getRa();
            this.dec = coords.getDec();
            return self();
        }

        /**
         * Sets the ARGB color for rendering.
         *
         * @param color ARGB color value
         * @return This builder for method chaining
         */
        @NonNull
        public T setColor(int color) {
            this.color = color;
            return self();
        }

        /**
         * Builds the CelestialObject instance.
         *
         * @return A new CelestialObject instance
         * @throws IllegalStateException if required fields are not set
         */
        @NonNull
        public CelestialObject build() {
            validate();
            return new CelestialObject(this);
        }

        /**
         * Validates that required fields are properly set.
         *
         * @throws IllegalStateException if validation fails
         */
        protected void validate() {
            if (id.isEmpty()) {
                throw new IllegalStateException("CelestialObject requires a non-empty id");
            }
            if (name.isEmpty()) {
                throw new IllegalStateException("CelestialObject requires a non-empty name");
            }
        }
    }
}
