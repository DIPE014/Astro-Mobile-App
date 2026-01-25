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
         * Initialize a CelestialObject using values from a Builder.
         *
         * The instance's fields are populated from the provided builder. The list
         * of alternate names is copied and exposed as an unmodifiable list.
         *
         * @param builder the Builder that provides the object's properties; must be non-null
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
     * Get the unique identifier of this celestial object.
     *
     * @return the object's unique identifier (non-null)
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Primary display name of this celestial object.
     *
     * @return the primary display name (non-null)
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
     * The object's Right Ascension in degrees.
     *
     * @return the Right Ascension in degrees, in the range 0 to 360
     */
    public float getRa() {
        return ra;
    }

    /**
     * Declination in degrees.
     *
     * @return Declination in degrees, between -90 and +90
     */
    public float getDec() {
        return dec;
    }

    /**
         * Obtain the geocentric coordinate representation of this object.
         *
         * @return the geocentric coordinates corresponding to this object's right ascension and declination (in degrees)
         */
    @NonNull
    public GeocentricCoords getCoordinates() {
        return GeocentricCoords.fromDegrees(ra, dec);
    }

    /**
     * Get the ARGB color used to render this celestial object.
     *
     * @return the ARGB color value used for rendering
     */
    public int getColor() {
        return color;
    }

    /**
     * Returns whether the object has any alternate names.
     *
     * @return `true` if the object has at least one alternate name, `false` otherwise
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

    /**
     * Determine whether another object represents the same celestial object by id.
     *
     * @param o the object to compare with
     * @return `true` if {@code o} is a CelestialObject of the same runtime class and has an identical id, `false` otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CelestialObject that = (CelestialObject) o;
        return id.equals(that.id);
    }

    /**
     * Compute a hash code for this object using its identifier.
     *
     * @return the hash code derived from {@code id}
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Provide a concise string representation of the celestial object.
     *
     * @return a string containing the object's id, name, RA, and Dec in the format
     *         "CelestialObject{id='...', name='...', ra=%.4f, dec=%.4f}".
     */
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
                 * Return the current builder cast to the concrete builder type for fluent chaining.
                 *
                 * @return `this` cast to the concrete builder type `T`
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
         * Sets the primary display name.
         *
         * @param name primary display name
         * @return this builder instance for chaining
         */
        @NonNull
        public T setName(@NonNull String name) {
            this.name = name;
            return self();
        }

        /**
         * Replace the builder's alternate names with the given list.
         *
         * If `alternateNames` is `null`, the builder's alternate names are set to an empty list.
         *
         * @param alternateNames list of alternate names, or `null` to clear to an empty list
         * @return this builder for method chaining
         */
        @NonNull
        public T setAlternateNames(@Nullable List<String> alternateNames) {
            this.alternateNames = alternateNames != null ? alternateNames : new ArrayList<>();
            return self();
        }

        /**
         * Appends an alternate name to the builder's list of alternate names.
         *
         * @param alternateName the alternate name to append
         * @return this builder instance for chaining
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
         * Set the ARGB color used to render the celestial object.
         *
         * @param color ARGB color value (0xAARRGGBB)
         * @return this builder for method chaining
         */
        @NonNull
        public T setColor(int color) {
            this.color = color;
            return self();
        }

        /**
         * Create a CelestialObject from the builder's current state.
         *
         * @return the constructed CelestialObject
         * @throws IllegalStateException if required fields (such as id or name) are empty
         */
        @NonNull
        public CelestialObject build() {
            validate();
            return new CelestialObject(this);
        }

        /**
         * Ensure required builder fields are present before building a CelestialObject.
         *
         * @throws IllegalStateException if `id` is empty or `name` is empty
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