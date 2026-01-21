package com.astro.app.common.model;

/**
 * SHARED MODEL - Used by both Frontend and Backend
 *
 * Represents where the phone camera is pointing in the sky.
 */
public class Pointing {

    // Direction the phone is looking (celestial coordinates)
    public float rightAscension;  // RA in degrees
    public float declination;     // Dec in degrees

    // Field of view
    public float fieldOfView;     // Degrees visible

    // Device orientation
    public float azimuth;         // Compass direction (0-360)
    public float altitude;        // Angle above horizon (-90 to 90)
    public float roll;            // Phone rotation

    public Pointing() {}

    public Pointing(float ra, float dec, float fov) {
        this.rightAscension = ra;
        this.declination = dec;
        this.fieldOfView = fov;
    }
}
