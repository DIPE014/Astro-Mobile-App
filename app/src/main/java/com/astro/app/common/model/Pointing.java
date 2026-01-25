package com.astro.app.common.model;

import com.astro.app.core.math.RaDec;
import com.astro.app.core.math.Vector3;

/**
 * SHARED MODEL - Used by both Frontend and Backend
 *
 * Represents where the phone camera is pointing in the sky.
 * Contains both the line of sight direction (where the phone is pointing)
 * and a perpendicular vector (the "up" direction on screen).
 *
 * Both vectors are expressed in geocentric celestial coordinates.
 */
public class Pointing {

    // Line of sight in geocentric celestial coordinates (unit vector)
    private final Vector3 lineOfSight;

    // Perpendicular "up" direction in geocentric celestial coordinates (unit vector)
    private final Vector3 perpendicular;

    // Cached RA/Dec values for convenience
    private float rightAscension;  // RA in degrees
    private float declination;     // Dec in degrees

    // Field of view
    private float fieldOfView = 45f;  // Degrees visible

    // Device orientation (for display purposes)
    private float azimuth;         // Compass direction (0-360)
    private float altitude;        // Angle above horizon (-90 to 90)
    private float roll;            // Phone rotation

    /**
     * Creates a default Pointing oriented to RA=0°, Dec=0°.
     *
     * Initializes the internal lineOfSight to (1, 0, 0), the perpendicular to (0, 1, 0),
     * and updates the cached right ascension and declination from the line of sight.
     */
    public Pointing() {
        this.lineOfSight = new Vector3(1f, 0f, 0f);
        this.perpendicular = new Vector3(0f, 1f, 0f);
        updateRaDecFromLineOfSight();
    }

    /**
     * Create a Pointing using specified geocentric line-of-sight and perpendicular (screen up) directions.
     *
     * Copies of the provided vectors are stored; right ascension and declination are computed from the line-of-sight and cached.
     *
     * @param lineOfSight the unit direction the device is pointing in geocentric celestial coordinates
     * @param perpendicular the unit perpendicular ("screen up") direction in geocentric celestial coordinates
     */
    public Pointing(Vector3 lineOfSight, Vector3 perpendicular) {
        this.lineOfSight = lineOfSight.copyForJ();
        this.perpendicular = perpendicular.copyForJ();
        updateRaDecFromLineOfSight();
    }

    /**
     * Create a Pointing positioned at the specified right ascension and declination with the given field of view.
     *
     * The internal line-of-sight vector is computed from the provided RA/Dec (degrees). The perpendicular vector
     * is initialized to point toward the celestial north pole projection.
     *
     * @param ra  right ascension in degrees
     * @param dec declination in degrees
     * @param fov field of view in degrees
     */
    public Pointing(float ra, float dec, float fov) {
        this.rightAscension = ra;
        this.declination = dec;
        this.fieldOfView = fov;

        // Calculate line of sight from RA/Dec
        float raRad = (float) (ra * Math.PI / 180.0);
        float decRad = (float) (dec * Math.PI / 180.0);
        float cosDec = (float) Math.cos(decRad);
        this.lineOfSight = new Vector3(
                (float) Math.cos(raRad) * cosDec,
                (float) Math.sin(raRad) * cosDec,
                (float) Math.sin(decRad)
        );

        // Default perpendicular pointing toward celestial north pole projected
        this.perpendicular = new Vector3(0f, 0f, 1f);
    }

    /**
     * Retrieve the pointing's line-of-sight vector.
     *
     * @return a copy of the line-of-sight Vector3 in geocentric celestial coordinates (modifying it will not affect this Pointing)
     */
    public Vector3 getLineOfSight() {
        return lineOfSight.copyForJ();
    }

    /**
     * Get the perpendicular (screen-up) unit vector in geocentric celestial coordinates.
     *
     * @return a copy of the perpendicular Vector3; modifying the returned vector does not affect this Pointing
     */
    public Vector3 getPerpendicular() {
        return perpendicular.copyForJ();
    }

    /**
     * X component of the pointing direction in geocentric celestial coordinates.
     *
     * @return the X component of the unit line-of-sight vector.
     */
    public float getLineOfSightX() {
        return lineOfSight.x;
    }

    /**
     * The Y component of the pointing direction's line-of-sight unit vector in geocentric celestial coordinates.
     *
     * @return the Y component of the line-of-sight unit vector in geocentric celestial coordinates
     */
    public float getLineOfSightY() {
        return lineOfSight.y;
    }

    /**
     * Get the Z component of the cached unit line-of-sight vector in geocentric celestial coordinates.
     *
     * @return the Z component of the unit line-of-sight vector.
     */
    public float getLineOfSightZ() {
        return lineOfSight.z;
    }

    /**
     * Get the X component of the perpendicular (screen up) direction in geocentric celestial coordinates.
     *
     * @return the X component of the perpendicular vector
     */
    public float getPerpendicularX() {
        return perpendicular.x;
    }

    /**
     * Retrieve the Y component of the perpendicular (screen up) unit vector in geocentric celestial coordinates.
     *
     * @return the Y component of the perpendicular vector
     */
    public float getPerpendicularY() {
        return perpendicular.y;
    }

    /**
     * Get the Z component of the perpendicular (screen-up) vector in geocentric celestial coordinates.
     *
     * @return the Z component of the perpendicular vector
     */
    public float getPerpendicularZ() {
        return perpendicular.z;
    }

    /**
     * Update the stored line-of-sight direction and refresh cached right ascension and declination.
     *
     * Intended for internal use by AstronomerModel; UI code should not call this method.
     *
     * @param newLineOfSight unit vector in geocentric celestial coordinates representing the new line-of-sight direction
     */
    public void updateLineOfSight(Vector3 newLineOfSight) {
        lineOfSight.assign(newLineOfSight);
        updateRaDecFromLineOfSight();
    }

    /**
     * Update the perpendicular (screen-up) direction used by this Pointing.
     *
     * @param newPerpendicular vector whose components will replace the internal perpendicular direction
     */
    public void updatePerpendicular(Vector3 newPerpendicular) {
        perpendicular.assign(newPerpendicular);
    }

    /**
     * Refreshes the cached rightAscension and declination values from the current lineOfSight vector.
     *
     * The cached values are stored in degrees.
     */
    private void updateRaDecFromLineOfSight() {
        RaDec raDec = RaDec.fromGeocentricCoords(lineOfSight);
        this.rightAscension = raDec.getRa();
        this.declination = raDec.getDec();
    }

    /**
     * Cached right ascension of the pointing in degrees.
     *
     * @return the cached right ascension in degrees
     */

    public float getRightAscension() {
        return rightAscension;
    }

    /**
     * Get the cached declination of the pointing in degrees.
     *
     * @return the cached declination in degrees
     */
    public float getDeclination() {
        return declination;
    }

    /**
     * Get the pointing's angular field of view in degrees.
     *
     * @return the field of view in degrees (defaults to 45.0)
     */
    public float getFieldOfView() {
        return fieldOfView;
    }

    /**
     * Set the pointing field of view in degrees.
     *
     * @param fieldOfView the field of view angle, in degrees
     */
    public void setFieldOfView(float fieldOfView) {
        this.fieldOfView = fieldOfView;
    }

    /**
     * Gets the current compass azimuth of the pointing.
     *
     * @return the azimuth in degrees measured clockwise from north, in the range [0, 360)
     */
    public float getAzimuth() {
        return azimuth;
    }

    /**
     * Set the device's compass azimuth used for orientation and display.
     *
     * @param azimuth compass direction in degrees, measured clockwise from north (0–360)
     */
    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
    }

    /**
     * Provide the device's altitude above the horizon in degrees.
     *
     * @return the altitude in degrees (angle above the horizon); range is -90 to 90
     */
    public float getAltitude() {
        return altitude;
    }

    /**
     * Set the device altitude (angle above the horizon).
     *
     * @param altitude angle in degrees; positive values are above the horizon, negative below (valid range: -90 to 90)
     */
    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    /**
     * Get the device roll angle in degrees.
     *
     * @return the roll angle in degrees
     */
    public float getRoll() {
        return roll;
    }

    /**
     * Sets the device rotation (roll) in degrees.
     *
     * @param roll the rotation angle in degrees
     */
    public void setRoll(float roll) {
        this.roll = roll;
    }
}