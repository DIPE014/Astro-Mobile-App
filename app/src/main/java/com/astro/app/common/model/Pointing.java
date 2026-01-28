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
     * Default constructor - points to RA=0, Dec=0
     */
    public Pointing() {
        this.lineOfSight = new Vector3(1f, 0f, 0f);
        this.perpendicular = new Vector3(0f, 1f, 0f);
        updateRaDecFromLineOfSight();
    }

    /**
     * Construct pointing from line of sight and perpendicular vectors.
     *
     * @param lineOfSight the direction the phone is pointing (geocentric coordinates)
     * @param perpendicular the "up" direction on screen (geocentric coordinates)
     */
    public Pointing(Vector3 lineOfSight, Vector3 perpendicular) {
        this.lineOfSight = lineOfSight.copyForJ();
        this.perpendicular = perpendicular.copyForJ();
        updateRaDecFromLineOfSight();
    }

    /**
     * Construct pointing from RA/Dec values.
     *
     * @param ra right ascension in degrees
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
     * Gets the line of sight component of the pointing.
     * Warning: creates a copy.
     */
    public Vector3 getLineOfSight() {
        return lineOfSight.copyForJ();
    }

    /**
     * Gets the perpendicular component of the pointing.
     * Warning: creates a copy.
     */
    public Vector3 getPerpendicular() {
        return perpendicular.copyForJ();
    }

    public float getLineOfSightX() {
        return lineOfSight.x;
    }

    public float getLineOfSightY() {
        return lineOfSight.y;
    }

    public float getLineOfSightZ() {
        return lineOfSight.z;
    }

    public float getPerpendicularX() {
        return perpendicular.x;
    }

    public float getPerpendicularY() {
        return perpendicular.y;
    }

    public float getPerpendicularZ() {
        return perpendicular.z;
    }

    /**
     * Updates the line of sight direction.
     * Note: This method is intended for use by AstronomerModel only.
     * Do not call directly from UI code.
     *
     * @param newLineOfSight the new line of sight vector
     */
    public void updateLineOfSight(Vector3 newLineOfSight) {
        lineOfSight.assign(newLineOfSight);
        updateRaDecFromLineOfSight();
    }

    /**
     * Updates the perpendicular (screen up) direction.
     * Note: This method is intended for use by AstronomerModel only.
     * Do not call directly from UI code.
     *
     * @param newPerpendicular the new perpendicular vector
     */
    public void updatePerpendicular(Vector3 newPerpendicular) {
        perpendicular.assign(newPerpendicular);
    }

    /**
     * Updates cached RA/Dec from the line of sight vector.
     */
    private void updateRaDecFromLineOfSight() {
        RaDec raDec = RaDec.fromGeocentricCoords(lineOfSight);
        this.rightAscension = raDec.getRa();
        this.declination = raDec.getDec();
    }

    // Getters and setters for convenience fields

    public float getRightAscension() {
        return rightAscension;
    }

    public float getDeclination() {
        return declination;
    }

    public float getFieldOfView() {
        return fieldOfView;
    }

    public void setFieldOfView(float fieldOfView) {
        this.fieldOfView = fieldOfView;
    }

    public float getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
    }

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }
}
