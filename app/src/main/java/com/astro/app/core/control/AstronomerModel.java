package com.astro.app.core.control;

import com.astro.app.common.model.Pointing;
import com.astro.app.core.math.LatLong;
import com.astro.app.core.math.Matrix3x3;
import com.astro.app.core.math.Vector3;

import java.util.Date;

/**
 * The interface for the astronomer model.
 *
 * The AstronomerModel is the core astronomy engine that transforms device orientation
 * to celestial coordinates. It combines GPS location, time, and sensor data to calculate
 * where the phone is pointing in the sky (RA/Dec).
 *
 * The model manages three coordinate frames:
 * 1. Celestial - fixed against background stars (x,y,z pointing to specific RA/Dec)
 * 2. Phone - fixed in the phone (x across short side, y along long side, z out of screen)
 * 3. Local - fixed at observer's position (x=East, y=North, z=Zenith)
 *
 * Adapted from stardroid's AstronomerModel for educational purposes.
 */
public interface AstronomerModel {

    /**
     * Listener interface for pointing changes.
     */
    interface PointingListener {
        /**
         * Called when the pointing direction changes.
         *
         * @param pointing the new pointing direction
         */
        void onPointingChanged(Pointing pointing);
    }

    /**
     * View direction modes for different device orientations.
     */
    enum ViewDirectionMode {
        /** Standard mode - phone held vertically, looking through screen */
        STANDARD,
        /** Rotated 90 degrees - for landscape mode */
        ROTATE90,
        /** Telescope mode - phone strapped to telescope tube */
        TELESCOPE
    }

    // ==================== Pointing Methods ====================

    /**
     * Gets the current pointing direction.
     * Note: clients should not modify the returned object.
     *
     * @return the current pointing in celestial coordinates
     */
    Pointing getPointing();

    /**
     * Sets the pointing direction manually.
     * Use this for manual control mode (e.g., dragging the sky view).
     *
     * @param lineOfSight the direction the phone is pointing
     * @param perpendicular the "up" direction on screen
     */
    void setPointing(Vector3 lineOfSight, Vector3 perpendicular);

    /**
     * Sets whether pointing should auto-update from sensors.
     *
     * @param autoUpdate true to auto-update, false for manual control
     */
    void setAutoUpdatePointing(boolean autoUpdate);

    // ==================== Time Methods ====================

    /**
     * Gets the current observation time.
     *
     * @return the current time as UTC
     */
    Date getTime();

    /**
     * Gets the current time in milliseconds since epoch.
     *
     * @return time in milliseconds
     */
    long getTimeMillis();

    /**
     * Sets the clock used for time calculations.
     * Useful for time travel feature or testing.
     *
     * @param clock the clock to use
     */
    void setClock(Clock clock);

    /**
     * Sets the time directly (convenience method for time travel).
     * Creates a temporary clock returning this specific time.
     *
     * @param timeMillis time in milliseconds since epoch
     */
    void setTime(long timeMillis);

    // ==================== Location Methods ====================

    /**
     * Gets the observer's current location on Earth.
     *
     * @return the current latitude/longitude
     */
    LatLong getLocation();

    /**
     * Sets the observer's current location on Earth.
     * Called by LocationController when GPS updates.
     *
     * @param location the new latitude/longitude
     */
    void setLocation(LatLong location);

    // ==================== Sensor Methods ====================

    /**
     * Sets the phone's sensor values from accelerometer and magnetometer.
     * The phone frame has x along the short side, y along the long side,
     * and z coming out of the screen.
     *
     * @param acceleration the acceleration vector in phone coordinates
     * @param magneticField the magnetic field vector in phone coordinates
     */
    void setPhoneSensorValues(Vector3 acceleration, Vector3 magneticField);

    /**
     * Sets the phone's rotation vector from the fused sensor.
     * Alternative to setPhoneSensorValues(Vector3, Vector3).
     *
     * @param rotationVector the rotation vector from the sensor
     */
    void setPhoneSensorValues(float[] rotationVector);

    /**
     * Gets the phone's up direction (opposite of gravity).
     *
     * @return the up direction in phone coordinates
     */
    Vector3 getPhoneUpDirection();

    // ==================== Field of View Methods ====================

    /**
     * Gets the current field of view.
     *
     * @return field of view in degrees
     */
    float getFieldOfView();

    /**
     * Sets the field of view.
     *
     * @param degrees the field of view in degrees
     */
    void setFieldOfView(float degrees);

    // ==================== View Mode Methods ====================

    /**
     * Sets the view direction mode.
     *
     * @param mode the view direction mode
     */
    void setViewDirectionMode(ViewDirectionMode mode);

    // ==================== Magnetic Correction Methods ====================

    /**
     * Gets the magnetic declination correction in degrees.
     * This is the difference between magnetic north and true north.
     *
     * @return magnetic declination in degrees
     */
    float getMagneticCorrection();

    /**
     * Sets the magnetic declination calculator.
     *
     * @param calculator the calculator for magnetic declination
     */
    void setMagneticDeclinationCalculator(MagneticDeclinationCalculator calculator);

    // ==================== Celestial Direction Methods ====================

    /**
     * Gets the North direction in celestial coordinates.
     *
     * @return North direction as a unit vector
     */
    Vector3 getNorth();

    /**
     * Gets the South direction in celestial coordinates.
     *
     * @return South direction as a unit vector
     */
    Vector3 getSouth();

    /**
     * Gets the East direction in celestial coordinates.
     *
     * @return East direction as a unit vector
     */
    Vector3 getEast();

    /**
     * Gets the West direction in celestial coordinates.
     *
     * @return West direction as a unit vector
     */
    Vector3 getWest();

    /**
     * Gets the Zenith direction in celestial coordinates.
     *
     * @return Zenith (straight up) direction as a unit vector
     */
    Vector3 getZenith();

    /**
     * Gets the Nadir direction in celestial coordinates.
     *
     * @return Nadir (straight down) direction as a unit vector
     */
    Vector3 getNadir();

    // ==================== Transformation Matrix Methods ====================

    /**
     * Gets the transformation matrix from phone coordinates to celestial coordinates.
     *
     * @return the 3x3 transformation matrix
     */
    Matrix3x3 getTransformationMatrix();

    // ==================== Listener Methods ====================

    /**
     * Adds a listener for pointing changes.
     *
     * @param listener the listener to add
     */
    void addPointingListener(PointingListener listener);

    /**
     * Removes a pointing listener.
     *
     * @param listener the listener to remove
     */
    void removePointingListener(PointingListener listener);
}
