package com.astro.app.data.model;

import androidx.annotation.NonNull;

/**
 * Represents geocentric celestial coordinates using Right Ascension and Declination.
 *
 * <p>This is a fundamental coordinate system used in astronomy to specify the position
 * of celestial objects as seen from the center of the Earth.</p>
 *
 * <h3>Coordinate System:</h3>
 * <ul>
 *   <li><b>Right Ascension (RA)</b>: Angular distance measured eastward along the
 *       celestial equator from the vernal equinox (0-360 degrees or 0-24 hours)</li>
 *   <li><b>Declination (Dec)</b>: Angular distance north or south of the celestial
 *       equator (-90 to +90 degrees)</li>
 * </ul>
 *
 * <p>This class is immutable and thread-safe.</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Equatorial_coordinate_system">
 *      Equatorial Coordinate System</a>
 */
public final class GeocentricCoords {

    /** Right Ascension in degrees (0-360) */
    private final float ra;

    /** Declination in degrees (-90 to +90) */
    private final float dec;

    /**
     * Constructs a GeocentricCoords instance after normalizing RA and clamping Dec.
     *
     * @param ra  Right Ascension in degrees; will be normalized to the range [0, 360).
     * @param dec Declination in degrees; will be clamped to the range [-90, 90].
     */
    private GeocentricCoords(float ra, float dec) {
        this.ra = normalizeRa(ra);
        this.dec = clampDec(dec);
    }

    /**
     * Constructs a GeocentricCoords from Right Ascension and Declination specified in degrees.
     *
     * @param ra  Right Ascension in degrees; will be normalized to [0, 360).
     * @param dec Declination in degrees; will be clamped to [-90, 90].
     * @return    a GeocentricCoords with RA in degrees (0–360) and Dec in degrees (−90–90)
     */
    @NonNull
    public static GeocentricCoords fromDegrees(float ra, float dec) {
        return new GeocentricCoords(ra, dec);
    }

    /**
         * Create a GeocentricCoords from Right Ascension given in hours and Declination in degrees.
         *
         * @param raHours Right Ascension in hours (expected range: 0–24)
         * @param dec     Declination in degrees (expected range: -90 to +90)
         * @return the GeocentricCoords representing the given RA and Dec (RA interpreted as hours and converted to degrees, then normalized; Dec clamped to [-90, +90])
         */
    @NonNull
    public static GeocentricCoords fromHoursAndDegrees(float raHours, float dec) {
        float raDegrees = raHours * 15.0f; // Convert hours to degrees
        return new GeocentricCoords(raDegrees, dec);
    }

    /**
     * Create a GeocentricCoords from Right Ascension and Declination expressed in radians.
     *
     * @param raRadians  Right Ascension in radians
     * @param decRadians Declination in radians
     * @return a GeocentricCoords whose RA and Dec are converted to degrees (RA normalized to [0,360), Dec clamped to [-90,90])
     */
    @NonNull
    public static GeocentricCoords fromRadians(float raRadians, float decRadians) {
        float raDegrees = (float) Math.toDegrees(raRadians);
        float decDegrees = (float) Math.toDegrees(decRadians);
        return new GeocentricCoords(raDegrees, decDegrees);
    }

    /**
     * Create a GeocentricCoords from a 3D Cartesian unit vector.
     *
     * <p>The vector is interpreted in a right-handed celestial frame where:
     * <ul>
     *   <li>X points toward RA = 0h, Dec = 0°</li>
     *   <li>Y points toward RA = 6h, Dec = 0°</li>
     *   <li>Z points toward Dec = +90° (north celestial pole)</li>
     * </ul>
     * </p>
     *
     * @param x X component of the vector (unit length not required but expected)
     * @param y Y component of the vector
     * @param z Z component of the vector
     * @return a GeocentricCoords with RA normalized to [0, 360) degrees and Dec clamped to [-90, 90] degrees
     */
    @NonNull
    public static GeocentricCoords fromVector3(float x, float y, float z) {
        float dec = (float) Math.toDegrees(Math.asin(z));
        float ra = (float) Math.toDegrees(Math.atan2(y, x));
        if (ra < 0) {
            ra += 360.0f;
        }
        return new GeocentricCoords(ra, dec);
    }

    /**
     * Get the Right Ascension in degrees.
     *
     * @return Right Ascension in degrees, normalized to the range [0, 360)
     */
    public float getRa() {
        return ra;
    }

    /**
     * Right Ascension expressed in hours.
     *
     * @return Right Ascension in hours, normalized to the range [0, 24).
     */
    public float getRaHours() {
        return ra / 15.0f;
    }

    /**
     * Right ascension expressed in radians.
     *
     * @return Right ascension in radians, in the range [0, 2π).
     */
    public float getRaRadians() {
        return (float) Math.toRadians(ra);
    }

    /**
     * The Declination in degrees.
     *
     * @return Declination in degrees, between -90 and +90 inclusive.
     */
    public float getDec() {
        return dec;
    }

    /**
     * Declination expressed in radians.
     *
     * @return the declination in radians, between -π/2 and +π/2
     */
    public float getDecRadians() {
        return (float) Math.toRadians(dec);
    }

    /**
     * Converts these coordinates to a 3D Cartesian unit vector.
     *
     * <p>The returned array contains [x, y, z] components where:
     * <ul>
     *   <li>X points toward RA=0h, Dec=0</li>
     *   <li>Y points toward RA=6h, Dec=0</li>
     *   <li>Z points toward Dec=+90 (north celestial pole)</li>
     * </ul>
     * </p>
     *
     * @return A float array of length 3 containing [x, y, z]
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
     * Computes the angular distance to another geocentric coordinate.
     *
     * @param other the target coordinate
     * @return the angular separation in degrees, between 0 and 180
     */
    public float angularDistanceTo(@NonNull GeocentricCoords other) {
        double ra1 = Math.toRadians(this.ra);
        double dec1 = Math.toRadians(this.dec);
        double ra2 = Math.toRadians(other.ra);
        double dec2 = Math.toRadians(other.dec);

        // Haversine formula
        double dRa = ra2 - ra1;
        double dDec = dec2 - dec1;

        double a = Math.sin(dDec / 2) * Math.sin(dDec / 2) +
                   Math.cos(dec1) * Math.cos(dec2) *
                   Math.sin(dRa / 2) * Math.sin(dRa / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (float) Math.toDegrees(c);
    }

    /**
     * Normalizes Right Ascension to the range [0, 360).
     */
    private static float normalizeRa(float ra) {
        ra = ra % 360.0f;
        if (ra < 0) {
            ra += 360.0f;
        }
        return ra;
    }

    /**
     * Clamp a declination angle to the inclusive range -90° to +90°.
     *
     * @param dec declination in degrees
     * @return the input declination constrained to the range -90.0 to 90.0 degrees
     */
    private static float clampDec(float dec) {
        return Math.max(-90.0f, Math.min(90.0f, dec));
    }

    /**
     * Determines whether this GeocentricCoords is equal to another object.
     *
     * @param o the object to compare with
     * @return `true` if `o` is a GeocentricCoords with identical RA and Dec values, `false` otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeocentricCoords that = (GeocentricCoords) o;
        return Float.compare(that.ra, ra) == 0 &&
               Float.compare(that.dec, dec) == 0;
    }

    /**
     * Computes a hash code derived from the stored Right Ascension and Declination values.
     *
     * @return an int hash code computed from this object's `ra` and `dec` fields
     */
    @Override
    public int hashCode() {
        int result = (ra != +0.0f ? Float.floatToIntBits(ra) : 0);
        result = 31 * result + (dec != +0.0f ? Float.floatToIntBits(dec) : 0);
        return result;
    }

    /**
     * Provide a compact string representation of the coordinates including RA in degrees and hours and Dec in degrees.
     *
     * @return a string in the form {@code GeocentricCoords{ra=%.4f deg (%.2fh), dec=%.4f deg}} showing RA in degrees, RA in hours, and Dec in degrees
     */
    @Override
    @NonNull
    public String toString() {
        return String.format("GeocentricCoords{ra=%.4f deg (%.2fh), dec=%.4f deg}",
                ra, getRaHours(), dec);
    }
}