package com.astro.app.core.control;

import com.astro.app.common.model.Pointing;

/**
 * BACKEND - Person B
 *
 * Core astronomy engine - transforms device orientation to sky coordinates.
 * Adapted from stardroid's AstronomerModelImpl.
 *
 * Key responsibility:
 * - Combine GPS location + time + sensor data
 * - Calculate where phone is pointing in celestial coordinates (RA/Dec)
 */
public class AstronomerModel {

    // User location
    private float latitude = 0f;
    private float longitude = 0f;

    // Device orientation from sensors
    private float[] rotationMatrix = new float[9];

    // Calculated pointing direction
    private Pointing currentPointing = new Pointing();

    public AstronomerModel() {
        // TODO: Initialize from stardroid math utilities
    }

    /**
     * Set user's geographic location.
     * Called by LocationController when GPS updates.
     */
    public void setLocation(float lat, float lon) {
        this.latitude = lat;
        this.longitude = lon;
        recalculate();
    }

    /**
     * Set device orientation from sensors.
     * Called by SensorController when sensors update.
     */
    public void setSensorValues(float[] rotationVector) {
        // TODO: Convert rotation vector to rotation matrix
        // android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
        recalculate();
    }

    /**
     * Get current pointing direction.
     * Called by UI to know where to display stars.
     */
    public Pointing getPointing() {
        return currentPointing;
    }

    /**
     * Recalculate pointing based on current location + sensors.
     * This is the core astronomy calculation.
     *
     * TODO: Implement using stardroid math:
     * 1. Calculate local sidereal time from UTC + longitude
     * 2. Calculate zenith RA/Dec from sidereal time + latitude
     * 3. Transform phone orientation to celestial coordinates
     */
    private void recalculate() {
        // TODO: Implement coordinate transformation
        // See stardroid's AstronomerModelImpl.calculatePointing()
    }
}
