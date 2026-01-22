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
    private Matrix3x3 axesPhoneInverseMatrix = Matrix3x3.getIdentity();
    private Matrix3x3 axesMagneticCelestialMatrix = Matrix3x3.getIdentity();
    private Matrix3x3 transformationMatrix = Matrix3x3.getIdentity();

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

    // ==================== View Mode ====================

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

    // ==================== Auto Update ====================

    @Override
    public void setAutoUpdatePointing(boolean autoUpdatePointing) {
        this.autoUpdatePointing = autoUpdatePointing;
    }

    // ==================== Field of View ====================

    @Override
    public float getFieldOfView() {
        return fieldOfView;
    }

    @Override
    public void setFieldOfView(float degrees) {
        this.fieldOfView = degrees;
        pointing.setFieldOfView(degrees);
    }

    // ==================== Magnetic Correction ====================

    @Override
    public float getMagneticCorrection() {
        return magneticDeclinationCalculator != null ?
                magneticDeclinationCalculator.getDeclination() : 0f;
    }

    @Override
    public void setMagneticDeclinationCalculator(MagneticDeclinationCalculator calculator) {
        this.magneticDeclinationCalculator = calculator;
        calculateLocalNorthAndUpInCelestialCoords(true);
    }

    // ==================== Time Methods ====================

    @Override
    public Date getTime() {
        return new Date(clock.getTimeInMillisSinceEpoch());
    }

    @Override
    public long getTimeMillis() {
        return clock.getTimeInMillisSinceEpoch();
    }

    @Override
    public void setClock(Clock clock) {
        this.clock = clock;
        calculateLocalNorthAndUpInCelestialCoords(true);
    }

    // ==================== Location Methods ====================

    @Override
    public LatLong getLocation() {
        return location;
    }

    @Override
    public void setLocation(LatLong location) {
        this.location = location;
        calculateLocalNorthAndUpInCelestialCoords(true);
    }

    // ==================== Sensor Methods ====================

    @Override
    public Vector3 getPhoneUpDirection() {
        return upPhone.copyForJ();
    }

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

    @Override
    public void setPhoneSensorValues(float[] rotationVector) {
        // Copy rotation vector (truncate to 4 elements if necessary)
        int length = Math.min(rotationVector.length, 4);
        System.arraycopy(rotationVector, 0, this.rotationVector, 0, length);
        useRotationVector = true;
    }

    // ==================== Celestial Directions ====================

    @Override
    public Vector3 getNorth() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        return trueNorthCelestial.copyForJ();
    }

    @Override
    public Vector3 getSouth() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        Vector3 south = trueNorthCelestial.copyForJ();
        south.timesAssign(-1f);
        return south;
    }

    @Override
    public Vector3 getZenith() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        return upCelestial.copyForJ();
    }

    @Override
    public Vector3 getNadir() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        Vector3 nadir = upCelestial.copyForJ();
        nadir.timesAssign(-1f);
        return nadir;
    }

    @Override
    public Vector3 getEast() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        return trueEastCelestial.copyForJ();
    }

    @Override
    public Vector3 getWest() {
        calculateLocalNorthAndUpInCelestialCoords(false);
        Vector3 west = trueEastCelestial.copyForJ();
        west.timesAssign(-1f);
        return west;
    }

    // ==================== Pointing Methods ====================

    @Override
    public Pointing getPointing() {
        calculatePointing();
        return pointing;
    }

    @Override
    public void setPointing(Vector3 lineOfSight, Vector3 perpendicular) {
        pointing.updateLineOfSight(lineOfSight);
        pointing.updatePerpendicular(perpendicular);
        notifyPointingListeners();
    }

    // ==================== Transformation Matrix ====================

    @Override
    public Matrix3x3 getTransformationMatrix() {
        calculatePointing();  // Ensure matrix is up to date
        return transformationMatrix.copyForJ();
    }

    // ==================== Listener Methods ====================

    @Override
    public void addPointingListener(PointingListener listener) {
        if (listener != null && !pointingListeners.contains(listener)) {
            pointingListeners.add(listener);
        }
    }

    @Override
    public void removePointingListener(PointingListener listener) {
        pointingListeners.remove(listener);
    }

    private void notifyPointingListeners() {
        for (PointingListener listener : pointingListeners) {
            listener.onPointingChanged(pointing);
        }
    }

    // ==================== Core Calculation Methods ====================

    /**
     * Updates the astronomer's 'pointing', that is, the direction the phone is
     * facing in celestial coordinates and also the 'up' vector along the
     * screen (also in celestial coordinates).
     *
     * This method requires that axesMagneticCelestialMatrix and
     * axesPhoneInverseMatrix are currently up to date.
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
     * Calculates local North, East and Up vectors in terms of the celestial
     * coordinate frame.
     *
     * @param forceUpdate if true, forces recalculation even if recently updated
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
     * Calculates local North and Up vectors in terms of the phone's coordinate
     * frame from the magnetic field and accelerometer sensors (or rotation vector).
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
