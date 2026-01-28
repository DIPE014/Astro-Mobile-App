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

        ObjectType(String displayName) {
            this.displayName = displayName;
        }

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
     * Creates a search result.
     *
     * @param name       Display name
     * @param ra         Right Ascension in degrees
     * @param dec        Declination in degrees
     * @param objectType Type of celestial object
     */
    public SearchResult(@NonNull String name, float ra, float dec, @NonNull ObjectType objectType) {
        this(name, ra, dec, objectType, null);
    }

    /**
     * Creates a search result with an object ID.
     *
     * @param name       Display name
     * @param ra         Right Ascension in degrees
     * @param dec        Declination in degrees
     * @param objectType Type of celestial object
     * @param objectId   Optional object ID for further lookup
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
     * Returns the display name of the object.
     *
     * @return Object name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the Right Ascension in degrees.
     *
     * @return RA in degrees (0-360)
     */
    public float getRa() {
        return ra;
    }

    /**
     * Returns the Declination in degrees.
     *
     * @return Dec in degrees (-90 to +90)
     */
    public float getDec() {
        return dec;
    }

    /**
     * Returns the type of celestial object.
     *
     * @return Object type
     */
    @NonNull
    public ObjectType getObjectType() {
        return objectType;
    }

    /**
     * Returns the optional object ID.
     *
     * @return Object ID, or null if not set
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Converts the coordinates to a 3D unit vector.
     *
     * @return float array [x, y, z]
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

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + Float.floatToIntBits(ra);
        result = 31 * result + Float.floatToIntBits(dec);
        result = 31 * result + objectType.hashCode();
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("SearchResult{name='%s', ra=%.2f, dec=%.2f, type=%s}",
                name, ra, dec, objectType);
    }
}
