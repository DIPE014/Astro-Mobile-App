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
 * Provide the current pointing direction in celestial coordinates.
 *
 * The returned Pointing represents the observer's current line-of-sight and orientation;
 * callers must not modify the returned instance.
 *
 * @return the current pointing in celestial coordinates
 */
    Pointing getPointing();

    /**
 * Set the current pointing using explicit direction vectors in the phone coordinate frame.
 *
 * @param lineOfSight a unit vector representing the device's viewing direction (line of sight)
 * @param perpendicular a unit vector on the screen representing the upward direction (perpendicular to the line of sight)
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
 * Current observation time in milliseconds since the Unix epoch (1970-01-01T00:00:00Z, UTC).
 *
 * @return the current time in milliseconds since the Unix epoch
 */
    long getTimeMillis();

    /**
 * Set the Clock instance used for all time calculations performed by this model.
 *
 * @param clock the Clock to use for time calculations (commonly set for testing or simulating different times)
 */
    void setClock(Clock clock);

    /**
 * Set the model's current time to the specified instant by replacing the clock with a temporary clock fixed at that time.
 *
 * @param timeMillis the instant in milliseconds since the Unix epoch (UTC)
 */
    void setTime(long timeMillis);

    // ==================== Location Methods ====================

    /**
 * Observer's current geographic location on Earth.
 *
 * @return the observer's latitude and longitude as a LatLong
 */
    LatLong getLocation();

    /**
 * Update the observer's geographic location used for celestial coordinate calculations.
 *
 * @param location observer geographic coordinates (latitude and longitude)
 */
    void setLocation(LatLong location);

    // ==================== Sensor Methods ====================

    /**
 * Update the model with accelerometer and magnetometer readings expressed in the phone coordinate frame.
 * The phone frame uses x along the short side, y along the long side, and z coming out of the screen.
 *
 * @param acceleration the acceleration vector in phone coordinates (x: short side, y: long side, z: out of screen)
 * @param magneticField the magnetic field vector in phone coordinates (x: short side, y: long side, z: out of screen)
 */
    void setPhoneSensorValues(Vector3 acceleration, Vector3 magneticField);

    /**
 * Update the phone's orientation using a rotation vector provided by the fused sensors.
 *
 * @param rotationVector the rotation vector reported by the device's fused rotation sensor
 *                       (device coordinate frame, typically Sensor.TYPE_ROTATION_VECTOR); array
 *                       length is usually 3 or 4. 
 */
    void setPhoneSensorValues(float[] rotationVector);

    /**
 * Phone up direction in phone coordinates (direction opposite gravity).
 *
 * @return the up direction vector in phone coordinates
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
 * Set how the device's phone coordinate frame is interpreted for display and pointing.
 *
 * @param mode the view orientation to apply:
 *             STANDARD — phone held vertically (normal portrait viewing);
 *             ROTATE90 — rotated 90 degrees for landscape orientation;
 *             TELESCOPE — phone mounted in a telescope-style orientation
 */
    void setViewDirectionMode(ViewDirectionMode mode);

    // ==================== Magnetic Correction Methods ====================

    /**
 * Provides the magnetic declination correction in degrees.
 *
 * @return the magnetic declination in degrees — the difference between magnetic north and true north
 */
    float getMagneticCorrection();

    /**
 * Sets the calculator used to determine magnetic declination applied as a correction to device orientation.
 *
 * @param calculator the MagneticDeclinationCalculator used to compute magnetic declination values
 */
    void setMagneticDeclinationCalculator(MagneticDeclinationCalculator calculator);

    // ==================== Celestial Direction Methods ====================

    /**
 * North direction expressed in celestial coordinates.
 *
 * @return a unit Vector3 pointing toward celestial north
 */
    Vector3 getNorth();

    /**
 * Provide the celestial south direction as a unit vector.
 *
 * @return Unit vector pointing toward celestial south in celestial coordinates.
 */
    Vector3 getSouth();

    /**
 * East unit vector in celestial coordinates.
 *
 * @return Unit vector pointing toward celestial east for the current observer/frame.
 */
    Vector3 getEast();

    /**
 * West direction in celestial coordinates.
 *
 * @return the West unit vector expressed in celestial coordinates
 */
    Vector3 getWest();

    /**
 * Zenith direction (straight up) expressed in celestial coordinates.
 *
 * @return the unit vector pointing toward the zenith in celestial coordinates
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
 * Provide the 3x3 transformation matrix that converts vectors from the phone coordinate frame to the celestial coordinate frame.
 *
 * @return the 3x3 matrix transforming phone-frame vectors into celestial-frame vectors
 */
    Matrix3x3 getTransformationMatrix();

    // ==================== Listener Methods ====================

    /**
 * Register a listener to receive notifications when the pointing direction changes.
 *
 * @param listener the PointingListener invoked with the new Pointing when the model's pointing updates
 */
    void addPointingListener(PointingListener listener);

    /**
 * Unregisters a previously added listener so it no longer receives pointing updates.
 *
 * @param listener the listener to remove; if the listener is not registered this method has no effect
 */
    void removePointingListener(PointingListener listener);
}