package com.astro.app.core.control;

import android.hardware.SensorManager;
import android.util.Log;

import com.astro.app.common.model.Pointing;
import com.astro.app.core.math.LatLong;
import com.astro.app.core.math.Matrix3x3;
import com.astro.app.core.math.RaDec;
import com.astro.app.core.math.Vector3;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.astro.app.core.math.AstronomyKt.calculateRADecOfZenith;
import static com.astro.app.core.math.CoordinateManipulationsKt.getGeocentricCoords;
import static com.astro.app.core.math.GeometryKt.calculateRotationMatrix;

/**
 * The model of the astronomer.
 *
 * Stores all the data about where and when the user is and where they're looking,
 * and handles translations between three frames of reference:
 *
 * 1. Celestial - a frame fixed against the background stars with
 *    x, y, z axes pointing to (RA=90, DEC=0), (RA=0, DEC=0), DEC=90
 *
 * 2. Phone - a frame fixed in the phone with x across the short side, y across
 *    the long side, and z coming out of the phone screen.
 *
 * 3. Local - a frame fixed in the astronomer's local position. x is due east
 *    along the ground, y is due north along the ground, and z points towards the zenith.
 *
 * The transformation from phone to celestial coordinates works as follows:
 * - Calculate local frame (North, East, Up) in phone coords
 * - Calculate local frame (North, East, Up) in celestial coords
 * - Build transformation matrix T such that: celestial = T * phone
 *
 * Adapted from stardroid's AstronomerModelImpl for educational purposes.
 */
@Singleton
public class AstronomerModelImpl implements AstronomerModel {

    private static final String TAG = "AstronomerModelImpl";

    // ==================== Constants ====================

    // Pointing direction in standard phone coordinates (looking through screen)
    private static final Vector3 POINTING_DIR_IN_STANDARD_PHONE_COORDS =
            new Vector3(0f, 0f, -1f);  // -Z axis (into screen)

    // Screen up direction in standard phone coordinates
    private static final Vector3 SCREEN_UP_STANDARD_IN_PHONE_COORDS =
            new Vector3(0f, 1f, 0f);   // +Y axis (toward top of phone)

    // Screen up rotated 90 degrees (for landscape mode)
    private static final Vector3 SCREEN_UP_ROTATED_IN_PHONE_COORDS =
            new Vector3(1f, 0f, 0f);   // +X axis

    // Pointing direction for telescope mode (sighting along phone's long edge)
    private static final Vector3 POINTING_DIR_FOR_TELESCOPES =
            new Vector3(0f, 1f, 0f);   // +Y axis

    // Screen up for telescope mode
    private static final Vector3 SCREEN_UP_FOR_TELESCOPES =
            new Vector3(0f, 0f, 1f);   // +Z axis

    // Earth's rotation axis in celestial coordinates (points to celestial north pole)
    private static final Vector3 AXIS_OF_EARTHS_ROTATION = Vector3.unitZ();

    // Minimum time between celestial coordinate updates (1 minute)
    private static final long MINIMUM_TIME_BETWEEN_CELESTIAL_COORD_UPDATES_MILLIS = 60000L;

    // Tolerance for sensor value validation
    private static final float TOL = 0.01f;

    // Initial "down" direction (default gravity vector)
    private static final Vector3 INITIAL_DOWN = new Vector3(0f, -1f, 0f);

    // Initial "south" direction (default magnetic field)
    private static final Vector3 INITIAL_SOUTH = new Vector3(0f, 0f, -1f);

    // ==================== State Variables ====================

    // Current view direction mode
    private Vector3 pointingInPhoneCoords = POINTING_DIR_IN_STANDARD_PHONE_COORDS;
    private Vector3 screenUpInPhoneCoords = SCREEN_UP_STANDARD_IN_PHONE_COORDS;

    // Magnetic declination calculator
    private MagneticDeclinationCalculator magneticDeclinationCalculator;

    // Whether to auto-update pointing from sensors
    private boolean autoUpdatePointing = true;

    // Field of view in degrees
    private float fieldOfView = 45f;

    // User's location
    private LatLong location = new LatLong(0f, 0f);

    // Clock for time calculations
    private Clock clock = new RealClock();

    // Timestamp of last celestial coordinate update
    private long celestialCoordsLastUpdated = -1;

    // The current pointing direction
    private final Pointing pointing = new Pointing();

    // Sensor values in phone coordinates
    private final Vector3 acceleration = INITIAL_DOWN.copyForJ();
    private Vector3 upPhone;

    private final Vector3 magneticField = INITIAL_SOUTH.copyForJ();

    // Whether to use rotation vector sensor (vs accelerometer + magnetometer)
    private boolean useRotationVector = false;
    private final float[] rotationVector = new float[]{1f, 0f, 0f, 0f};

    // Local frame directions in celestial coordinates
    private Vector3 trueNorthCelestial = Vector3.unitX();
    private Vector3 upCelestial = Vector3.unitY();
    private Vector3 trueEastCelestial = AXIS_OF_EARTHS_ROTATION.copyForJ();

    // Transformation matrices
    private Matrix3x3 axesPhoneInverseMatrix = Matrix3x3.identity.copyForJ();
    private Matrix3x3 axesMagneticCelestialMatrix = Matrix3x3.identity.copyForJ();
    private Matrix3x3 transformationMatrix = Matrix3x3.identity.copyForJ();

    // Listeners for pointing changes
    private final List<PointingListener> pointingListeners = new CopyOnWriteArrayList<>();

    // ==================== Constructor ====================

    /**
     * Constructs a new AstronomerModelImpl with the given magnetic declination calculator.
     *
     * @param magneticDeclinationCalculator calculator for magnetic declination correction
     */
    @Inject
    public AstronomerModelImpl(MagneticDeclinationCalculator magneticDeclinationCalculator) {
        this.upPhone = acceleration.copyForJ();
        this.upPhone.timesAssign(-1f);
        setMagneticDeclinationCalculator(magneticDeclinationCalculator);
    }

    /**
     * Default constructor using real magnetic declination calculator.
     */
    public AstronomerModelImpl() {
        this(new RealMagneticDeclinationCalculator());
    }

    /**
     * Set the phone's canonical pointing and screen-up vectors based on the selected view direction mode.
     *
     * For STANDARD the pointing is the screen-forward vector and screen-up is top of screen;
     * for ROTATE90 the pointing is screen-forward and screen-up is rotated 90°;
     * for TELESCOPE the pointing and screen-up use the telescope orientation convention.
     *
     * @param mode the view direction mode determining which predefined phone-coordinate vectors to use
     */

    @Override
    public void setViewDirectionMode(ViewDirectionMode mode) {
        switch (mode) {
            case STANDARD:
                pointingInPhoneCoords = POINTING_DIR_IN_STANDARD_PHONE_COORDS;
                screenUpInPhoneCoords = SCREEN_UP_STANDARD_IN_PHONE_COORDS;
                break;
            case ROTATE90:
                pointingInPhoneCoords = POINTING_DIR_IN_STANDARD_PHONE_COORDS;
                screenUpInPhoneCoords = SCREEN_UP_ROTATED_IN_PHONE_COORDS;
                break;
            case TELESCOPE:
                pointingInPhoneCoords = POINTING_DIR_FOR_TELESCOPES;
                screenUpInPhoneCoords = SCREEN_UP_FOR_TELESCOPES;
                break;
        }
    }

    /**
     * Enable or disable automatic updating of the model's pointing from sensors, time, and location changes.
     *
     * @param autoUpdatePointing true to enable automatic updates of pointing; false to disable them
     */

    @Override
    public void setAutoUpdatePointing(boolean autoUpdatePointing) {
        this.autoUpdatePointing = autoUpdatePointing;
    }

    /**
     * Get the current field of view in degrees.
     *
     * @return the field of view, in degrees
     */

    @Override
    public float getFieldOfView() {
        return fieldOfView;
    }

    /**
     * Set the model's field of view in degrees and apply it to the current pointing.
     *
     * @param degrees the field of view angle in degrees
     */
    @Override
    public void setFieldOfView(float degrees) {
        this.fieldOfView = degrees;
        pointing.setFieldOfView(degrees);
    }

    /**
     * Provides the magnetic declination used to correct local north for celestial calculations.
     *
     * @return the magnetic declination in degrees; positive values indicate eastward declination,
     *         negative values indicate westward declination, or 0.0 if no declination calculator is available.
     */

    @Override
    public float getMagneticCorrection() {
        return magneticDeclinationCalculator != null ?
                magneticDeclinationCalculator.getDeclination() : 0f;
    }

    /**
     * Sets the magnetic declination calculator and recomputes the local north and up axes in celestial coordinates.
     *
     * @param calculator the MagneticDeclinationCalculator to use for magnetic correction, or {@code null} to disable magnetic correction
     */
    @Override
    public void setMagneticDeclinationCalculator(MagneticDeclinationCalculator calculator) {
        this.magneticDeclinationCalculator = calculator;
        calculateLocalNorthAndUpInCelestialCoords(true);
    }

    /**
     * Get the current instant from the model's clock.
     *
     * @return a Date representing the current time according to the model's clock
     */

    @Override
    public Date getTime() {
        return new Date(clock.getTimeInMillisSinceEpoch());
    }

    /**
     * Retrieve the model's current time from its clock.
     *
     * @return Current time in milliseconds since the Unix epoch.
     */
    @Override
    public long getTimeMillis() {
        return clock.getTimeInMillisSinceEpoch();
    }

    /**
     * Set the time source used for celestial calculations and force immediate recomputation of the local
     * north and up axes in celestial coordinates.
     *
     * @param clock the Clock to use for time queries; will be used immediately to refresh cached celestial axes
     */
    @Override
    public void setClock(Clock clock) {
        this.clock = clock;
        calculateLocalNorthAndUpInCelestialCoords(true);
    }

    /**
     * Set the model's current time to a specific instant and refresh time-dependent celestial axes.
     *
     * @param timeMillis the target time expressed as milliseconds since the Unix epoch (UTC)
     */
    @Override
    public void setTime(long timeMillis) {
        // Create a fixed-time clock for this specific time
        this.clock = () -> timeMillis;
        calculateLocalNorthAndUpInCelestialCoords(true);
    }

    /**
     * Get the geographic location used for celestial calculations and magnetic correction.
     *
     * @return the current latitude/longitude location used by the model
     */

    @Override
    public LatLong getLocation() {
        return location;
    }

    /**
     * Set the observer's geographic location and force an immediate refresh of the local
     * celestial North/East/Up axes.
     *
     * @param location the observer's geographic latitude and longitude used to compute
     *                 local celestial directions (North, East, Up)
     */
    @Override
    public void setLocation(LatLong location) {
        this.location = location;
        calculateLocalNorthAndUpInCelestialCoords(true);
    }

    /**
     * Get the device's up direction expressed in the phone coordinate frame.
     *
     * @return a copy of the phone up direction vector (phone-coordinate unit vector)
     */

    @Override
    public Vector3 getPhoneUpDirection() {
        return upPhone.copyForJ();
    }

    /**
     * Updates the model with accelerometer and magnetometer readings from the phone.
     *
     * If either vector's magnitude is below the internal tolerance, the values are ignored
     * and the model is not changed. On success, stores the provided vectors and disables
     * rotation-vector-based orientation.
     *
     * @param acceleration the accelerometer reading in phone coordinates
     * @param magneticField the magnetic field (magnetometer) reading in phone coordinates
     */
    @Override
    public void setPhoneSensorValues(Vector3 acceleration, Vector3 magneticField) {
        if (magneticField.getLength2() < TOL || acceleration.getLength2() < TOL) {
            Log.w(TAG, "Invalid sensor values - ignoring");
            return;
        }
        this.acceleration.assign(acceleration);
        this.magneticField.assign(magneticField);
        useRotationVector = false;
    }

    /**
     * Sets the phone's rotation-vector sensor values and enables the rotation-vector input path.
     *
     * Copies up to the first four elements of the provided Android rotation-vector array into the
     * internal rotation vector and marks the model to use the rotation-vector sensor for subsequent
     * orientation calculations.
     *
     * @param rotationVector Android rotation-vector sensor values; only the first four elements are used
     */
    @Override
    public void setPhoneSensorValues(float[] rotationVector) {
        // Copy rotation vector (truncate to 4 elements if necessary)
        int length = Math.min(rotationVector.length, 4);
        System.arraycopy(rotationVector, 0, this.rotationVector, 0, length);
        useRotationVector = true;
    }

    /**
     * The local true north direction expressed in celestial coordinates.
     *
     * @return a new {@code Vector3} unit vector pointing toward local true north in the celestial coordinate frame
     */

    @Override
    public Vector3 getNorth() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        return trueNorthCelestial.copyForJ();
    }

    /**
     * Local south direction in celestial coordinates.
     *
     * @return a unit Vector3 pointing toward local geographic south in the celestial frame
     */
    @Override
    public Vector3 getSouth() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        Vector3 south = trueNorthCelestial.copyForJ();
        south.timesAssign(-1f);
        return south;
    }

    /**
     * Provides the local zenith direction in celestial coordinates.
     *
     * Ensures the model's cached local celestial axes are refreshed before returning.
     *
     * @return a copy of the local zenith (unit `Vector3`) expressed in celestial coordinates
     */
    @Override
    public Vector3 getZenith() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        return upCelestial.copyForJ();
    }

    /**
     * Provide the local nadir direction in celestial coordinates (the direction pointing toward the ground).
     *
     * Recomputes local celestial axes if they are out of date before returning the nadir.
     *
     * @return the nadir direction as a unit Vector3 in celestial coordinates (points opposite the zenith)
     */
    @Override
    public Vector3 getNadir() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        Vector3 nadir = upCelestial.copyForJ();
        nadir.timesAssign(-1f);
        return nadir;
    }

    /**
     * Gets the local east direction expressed in celestial coordinates.
     *
     * Ensures local celestial axes are up-to-date before returning.
     *
     * @return a copy of the local east unit vector in celestial coordinates
     */
    @Override
    public Vector3 getEast() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        return trueEastCelestial.copyForJ();
    }

    /**
     * Provides the local west direction expressed in celestial coordinates.
     *
     * @return the unit Vector3 pointing toward local West in celestial coordinates (a new copy)
     */
    @Override
    public Vector3 getWest() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        Vector3 west = trueEastCelestial.copyForJ();
        west.timesAssign(-1f);
        return west;
    }

    /**
     * Provide the current pointing state, recalculating it beforehand to ensure it reflects the latest sensor, time, and location data.
     *
     * @return the current {@code Pointing} containing the up-to-date line-of-sight and perpendicular vectors (and field of view)
     */

    @Override
    public Pointing getPointing() {
        calculatePointing();
        return pointing;
    }

    /**
     * Update the current pointing using an explicit line-of-sight and a perpendicular orientation vector.
     *
     * @param lineOfSight a vector representing the pointing direction in the coordinate frame used by the model
     * @param perpendicular a vector perpendicular to {@code lineOfSight} that defines the orientation (screen-up) in the same frame
     * <p>
     * This updates the internal pointing state and notifies registered PointingListeners of the change.
     */
    @Override
    public void setPointing(Vector3 lineOfSight, Vector3 perpendicular) {
        pointing.updateLineOfSight(lineOfSight);
        pointing.updatePerpendicular(perpendicular);
        notifyPointingListeners();
    }

    /**
     * Provide the current 3x3 transformation that maps vectors from phone coordinates into celestial coordinates.
     *
     * The implementation ensures the matrix is recalculated if needed before returning.
     *
     * @return a copy of the transformation matrix mapping phone-coordinate vectors to celestial-coordinate vectors
     */

    @Override
    public Matrix3x3 getTransformationMatrix() {
        calculatePointing();  // Ensure matrix is up to date
        return transformationMatrix.copyForJ();
    }

    /**
     * Registers a listener to be notified when the pointing changes.
     *
     * If `listener` is null or already registered, this method has no effect.
     *
     * @param listener the PointingListener to add
     */

    @Override
    public void addPointingListener(PointingListener listener) {
        if (listener != null && !pointingListeners.contains(listener)) {
            pointingListeners.add(listener);
        }
    }

    /**
     * Unregisters a pointing listener so it will no longer receive pointing updates.
     *
     * @param listener the listener to remove; no-op if the listener is null or not registered
     */
    @Override
    public void removePointingListener(PointingListener listener) {
        pointingListeners.remove(listener);
    }

    /**
     * Notify all registered PointingListener instances that the current pointing has changed.
     *
     * Each listener's onPointingChanged(pointing) method is invoked synchronously on the calling thread.
     */
    private void notifyPointingListeners() {
        for (PointingListener listener : pointingListeners) {
            listener.onPointingChanged(pointing);
        }
    }

    // ==================== Core Calculation Methods ====================

    /**
     * Update the current pointing in celestial coordinates and notify listeners.
     *
     * Recomputes the phone→celestial transformation, transforms the stored phone
     * pointing and screen-up vectors into celestial coordinates, updates the
     * internal Pointing (line-of-sight, perpendicular, and field of view), and
     * notifies registered PointingListeners. Requires that
     * axesMagneticCelestialMatrix and axesPhoneInverseMatrix are current.
     */
    private void calculatePointing() {
        if (!autoUpdatePointing) {
            return;
        }

        calculateLocalNorthAndUpInCelestialCoords(false);
        calculateLocalNorthAndUpInPhoneCoordsFromSensors();

        // Calculate transformation matrix: celestial = T * phone
        // T = axesMagneticCelestialMatrix * axesPhoneInverseMatrix
        transformationMatrix = axesMagneticCelestialMatrix.times(axesPhoneInverseMatrix);

        // Transform pointing direction from phone to celestial coordinates
        Vector3 viewInCelestialCoords = transformationMatrix.times(pointingInPhoneCoords);
        Vector3 screenUpInCelestialCoords = transformationMatrix.times(screenUpInPhoneCoords);

        pointing.updateLineOfSight(viewInCelestialCoords);
        pointing.updatePerpendicular(screenUpInCelestialCoords);
        pointing.setFieldOfView(fieldOfView);

        notifyPointingListeners();
    }

    /**
     * Recomputes the local North, East, and Up unit vectors expressed in celestial coordinates.
     *
     * <p>When not forced, this method will return immediately if the most recent computation
     * occurred within the configured minimum update interval. On execution it updates the
     * cached timestamp, refreshes the magnetic-declination calculator state, computes the
     * zenith (Up) and the true North/East directions, applies the magnetic-declination
     * correction if available, and stores the results in the instance fields:
     * {@code upCelestial}, {@code trueNorthCelestial}, {@code trueEastCelestial}, and
     * {@code axesMagneticCelestialMatrix}.</p>
     *
     * @param forceUpdate if true, bypasses the time-based throttle and forces a recalculation
     */
    private void calculateLocalNorthAndUpInCelestialCoords(boolean forceUpdate) {
        long currentTime = clock.getTimeInMillisSinceEpoch();

        // Only update if forced or enough time has passed
        if (!forceUpdate &&
                Math.abs(currentTime - celestialCoordsLastUpdated) <
                        MINIMUM_TIME_BETWEEN_CELESTIAL_COORD_UPDATES_MILLIS) {
            return;
        }

        celestialCoordsLastUpdated = currentTime;
        updateMagneticCorrection();

        // Calculate zenith direction in celestial coordinates
        RaDec zenithRaDec = calculateRADecOfZenith(getTime(), location);
        upCelestial = getGeocentricCoords(zenithRaDec);

        // Calculate true North direction (along the ground)
        // North is the projection of Earth's rotation axis onto the local horizontal plane
        Vector3 z = AXIS_OF_EARTHS_ROTATION.copyForJ();
        float zDotUp = upCelestial.dot(z);

        // trueNorth = z - upCelestial * (z dot upCelestial)
        // This is the vector rejection of z from upCelestial
        trueNorthCelestial = z.minus(upCelestial.times(zDotUp));
        trueNorthCelestial.normalize();

        // East is the cross product of North and Up
        trueEastCelestial = trueNorthCelestial.times(upCelestial);

        // Apply magnetic declination correction
        // Rather than correcting the phone's axes, we rotate the celestial axes
        // by the same amount in the opposite direction
        float declination = magneticDeclinationCalculator != null ?
                magneticDeclinationCalculator.getDeclination() : 0f;

        Matrix3x3 rotationMatrix = calculateRotationMatrix(declination, upCelestial);
        Vector3 magneticNorthCelestial = rotationMatrix.times(trueNorthCelestial);
        Vector3 magneticEastCelestial = magneticNorthCelestial.times(upCelestial);

        // Build the celestial axes matrix [North, Up, East] as column vectors
        axesMagneticCelestialMatrix = new Matrix3x3(
                magneticNorthCelestial,
                upCelestial,
                magneticEastCelestial,
                true  // column vectors
        );
    }

    /**
     * Compute the local North, East, and Up axes expressed in phone coordinates and update
     * the cached inverse phone-axes matrix used for frame transformations.
     *
     * <p>If a rotation-vector sensor is in use, the axes are extracted from the rotation
     * matrix derived from that vector. Otherwise, the accelerometer and magnetometer are
     * used: the accelerometer determines Up and the magnetometer (projected onto the
     * horizontal plane) determines magnetic North; East is computed from North and Up.
     *
     * <p>Side effects: updates the fields {@code upPhone} and {@code axesPhoneInverseMatrix}
     * (row vectors: magnetic north, up, magnetic east).
     */
    private void calculateLocalNorthAndUpInPhoneCoordsFromSensors() {
        Vector3 magneticNorthPhone;
        Vector3 magneticEastPhone;

        if (useRotationVector) {
            // Use rotation vector sensor (fused accelerometer + magnetometer + gyroscope)
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, this.rotationVector);

            // The up and north vectors are the 2nd and 3rd rows of this matrix
            magneticNorthPhone = new Vector3(
                    rotationMatrix[3], rotationMatrix[4], rotationMatrix[5]);
            upPhone = new Vector3(
                    rotationMatrix[6], rotationMatrix[7], rotationMatrix[8]);
            magneticEastPhone = new Vector3(
                    rotationMatrix[0], rotationMatrix[1], rotationMatrix[2]);
        } else {
            // Use raw accelerometer and magnetometer
            // Up is opposite of gravity (normalized acceleration)
            upPhone = acceleration.normalizedCopy();

            // Magnetic field points toward magnetic north (but also has vertical component)
            Vector3 magneticFieldNormalized = magneticField.normalizedCopy();

            // Get the horizontal component of magnetic field (vector rejection)
            // This is magnetic North along the ground
            float magDotUp = magneticFieldNormalized.dot(upPhone);
            magneticNorthPhone = magneticFieldNormalized.minus(upPhone.times(magDotUp));
            magneticNorthPhone.normalize();

            // East is the cross product of North and Up
            magneticEastPhone = magneticNorthPhone.times(upPhone);
        }

        // The matrix is orthogonal, so transpose it to find its inverse
        // We construct it from row vectors instead of column vectors to get the transpose
        axesPhoneInverseMatrix = new Matrix3x3(
                magneticNorthPhone,
                upPhone,
                magneticEastPhone,
                false  // row vectors (transpose)
        );
    }

    /**
     * Updates the magnetic declination based on current location and time.
     */
    private void updateMagneticCorrection() {
        if (magneticDeclinationCalculator != null) {
            magneticDeclinationCalculator.setLocationAndTime(location, getTimeMillis());
        }
    }
}