package com.astro.app.search;

import androidx.annotation.NonNull;

/**
 * Represents a search result with its name, coordinates, and category.
 *
 * <p>SearchResult pairs a celestial object's display name with its
 * celestial coordinates (RA/Dec) for navigation purposes.</p>
 */
public class SearchResult {

    /** Type of celestial object */
    public enum ObjectType {
        STAR("Star"),
        PLANET("Planet"),
        CONSTELLATION("Constellation"),
        DEEP_SKY("Deep Sky Object"),
        MOON("Moon"),
        SUN("Sun"),
        OTHER("Object");

        private final String displayName;

        /**
         * Creates an enum instance with the specified human-readable display name.
         *
         * @param displayName the human-readable name for this object type (e.g., "Star")
         */
        ObjectType(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Human-readable name for this object type.
         *
         * @return the display name (for example, "Star", "Planet")
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    /** Display name of the object */
    @NonNull
    private final String name;

    /** Right Ascension in degrees (0-360) */
    private final float ra;

    /** Declination in degrees (-90 to +90) */
    private final float dec;

    /** Type of celestial object */
    @NonNull
    private final ObjectType objectType;

    /** Optional ID for the object */
    private final String objectId;

    /**
     * Constructs a SearchResult with the given display name, celestial coordinates, and object type.
     *
     * @param name       display name of the object
     * @param ra         right ascension in degrees (0–360)
     * @param dec        declination in degrees (−90–+90)
     * @param objectType type of the celestial object
     */
    public SearchResult(@NonNull String name, float ra, float dec, @NonNull ObjectType objectType) {
        this(name, ra, dec, objectType, null);
    }

    /**
     * Constructs a SearchResult with an optional object identifier.
     *
     * @param name       display name of the object (non-null)
     * @param ra         Right Ascension in degrees, in range 0–360
     * @param dec        Declination in degrees, in range -90–90
     * @param objectType type of celestial object (non-null)
     * @param objectId   optional identifier for further lookup; may be null
     */
    public SearchResult(@NonNull String name, float ra, float dec,
                        @NonNull ObjectType objectType, String objectId) {
        this.name = name;
        this.ra = ra;
        this.dec = dec;
        this.objectType = objectType;
        this.objectId = objectId;
    }

    /**
     * The object's display name.
     *
     * @return the object's display name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Gets the Right Ascension in degrees.
     *
     * @return the Right Ascension in degrees, in the range 0 to 360
     */
    public float getRa() {
        return ra;
    }

    /**
     * Gets the Declination in degrees.
     *
     * @return Declination in degrees, in the range -90 to +90
     */
    public float getDec() {
        return dec;
    }

    /**
     * Gets the celestial object's type.
     *
     * @return the object's {@link ObjectType} (never {@code null})
     */
    @NonNull
    public ObjectType getObjectType() {
        return objectType;
    }

    /**
     * Gets the optional identifier associated with this search result.
     *
     * @return the object ID, or {@code null} if not set
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Convert the object's Right Ascension and Declination into a 3D unit Cartesian vector.
     *
     * @return a length-3 float array containing the unit vector [x, y, z] in equatorial Cartesian coordinates
     */
    @NonNull
    public float[] toVector3() {
        double raRad = Math.toRadians(ra);
        double decRad = Math.toRadians(dec);

        float x = (float) (Math.cos(decRad) * Math.cos(raRad));
        float y = (float) (Math.cos(decRad) * Math.sin(raRad));
        float z = (float) Math.sin(decRad);

        return new float[] {x, y, z};
    }

    /**
     * Determines whether another object represents the same search result.
     *
     * @param o the object to compare with this instance
     * @return `true` if {@code o} is a {@code SearchResult} with equal `name`, `ra`, `dec`, and `objectType`; `false` otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
        return Float.compare(that.ra, ra) == 0 &&
               Float.compare(that.dec, dec) == 0 &&
               name.equals(that.name) &&
               objectType == that.objectType;
    }

    /**
     * Computes a hash code derived from the search result's name, right ascension, declination, and object type.
     *
     * @return an int hash code based on the name, ra, dec, and object type
     */
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + Float.floatToIntBits(ra);
        result = 31 * result + Float.floatToIntBits(dec);
        result = 31 * result + objectType.hashCode();
        return result;
    }

    /**
     * Create a compact, human-readable representation of the search result.
     *
     * @return the string "SearchResult{name='NAME', ra=RA, dec=DEC, type=TYPE}" where NAME and TYPE are the object's name and type, and RA/DEC are formatted to two decimal places
     */
    @Override
    @NonNull
    public String toString() {
        return String.format("SearchResult{name='%s', ra=%.2f, dec=%.2f, type=%s}",
                name, ra, dec, objectType);
    }
}