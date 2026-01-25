package com.astro.app.core.control;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * BACKEND - Person B
 *
 * Handles device orientation sensors (accelerometer, magnetometer, gyroscope).
 * Provides rotation data to the sky view for sensor-based sky movement.
 *
 * Uses the rotation vector sensor which fuses accelerometer, magnetometer, and
 * gyroscope data for smooth, accurate orientation tracking.
 */
public class SensorController implements SensorEventListener {
    private static final String TAG = "SensorController";

    public interface SensorListener {
        /**
 * Called when a new device rotation vector is available.
 *
 * @param rotationVector the rotation vector reported by the sensor (SensorEvent.values).
 *                       Typically a 3- or 4-element array in device coordinates representing
 *                       orientation; when 4 elements are present the fourth is the scalar component.
 */
void onOrientationChanged(float[] rotationVector);
    }

    private final SensorManager sensorManager;
    private final Sensor rotationSensor;
    private SensorListener listener;
    private boolean isRegistered = false;

    /**
     * Create a SensorController and initialize the rotation vector sensor used for orientation.
     *
     * Initializes the SensorManager from the given context and attempts to obtain the device's
     * rotation vector sensor (TYPE_ROTATION_VECTOR). Logs a warning if the sensor is unavailable
     * or a debug message with the sensor name when initialization succeeds.
     *
     * @param context the Android Context used to acquire the system SensorManager
     */
    public SensorController(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // Use rotation vector sensor (fused accelerometer + magnetometer + gyroscope)
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor == null) {
            Log.w(TAG, "Rotation vector sensor not available on this device");
        } else {
            Log.d(TAG, "Rotation vector sensor initialized: " + rotationSensor.getName());
        }
    }

    /**
     * Sets the listener to receive orientation updates from the rotation vector sensor.
     *
     * @param listener the SensorListener to notify when a new rotation vector is available;
     *                 may be null to clear the current listener
     */
    public void setListener(SensorListener listener) {
        this.listener = listener;
    }

    /**
     * Determine whether the device provides a rotation vector sensor.
     *
     * @return `true` if the rotation vector sensor is present on the device, `false` otherwise.
     */
    public boolean isSensorAvailable() {
        return rotationSensor != null;
    }

    /**
     * Registers this controller as a listener for the rotation vector sensor when the sensor
     * is available and not already registered.
     *
     * Updates the `isRegistered` flag to reflect registration success and logs whether
     * registration succeeded or failed. If the rotation sensor is not present, logs a warning.
     */
    public void start() {
        if (rotationSensor != null && !isRegistered) {
            boolean registered = sensorManager.registerListener(
                    this,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_GAME  // Fast updates for smooth AR
            );
            isRegistered = registered;
            if (registered) {
                Log.d(TAG, "Sensor listener registered successfully");
            } else {
                Log.e(TAG, "Failed to register sensor listener");
            }
        } else if (rotationSensor == null) {
            Log.w(TAG, "Cannot start - rotation sensor not available");
        }
    }

    /**
     * Stops sensor updates by unregistering this listener when it is currently registered.
     *
     * If the listener was registered, it will be unregistered, `isRegistered` will be set to
     * false, and an informational log entry will be emitted.
     */
    public void stop() {
        if (isRegistered) {
            sensorManager.unregisterListener(this);
            isRegistered = false;
            Log.d(TAG, "Sensor listener unregistered");
        }
    }

    /**
     * Forwards rotation vector sensor events to the registered listener.
     *
     * If the event originates from the rotation vector sensor and a listener is set,
     * passes the event's rotation vector values to the listener's onOrientationChanged method.
     *
     * @param event the SensorEvent to handle; when its sensor type is TYPE_ROTATION_VECTOR,
     *              this method forwards event.values to the registered listener
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && listener != null) {
            listener.onOrientationChanged(event.values);
        }
    }

    /**
     * Handles sensor accuracy updates by converting the numeric accuracy code to a human-readable label and logging the result.
     *
     * Translates the `accuracy` constant into one of: NO_CONTACT, UNRELIABLE, LOW, MEDIUM, HIGH, or UNKNOWN. Logs the current accuracy state and emits a warning when the accuracy is `SENSOR_STATUS_UNRELIABLE` (suggesting compass calibration).
     *
     * @param sensor   the sensor whose accuracy has changed
     * @param accuracy one of the SensorManager accuracy constants (e.g. SENSOR_STATUS_NO_CONTACT, SENSOR_STATUS_UNRELIABLE, SENSOR_STATUS_ACCURACY_LOW, SENSOR_STATUS_ACCURACY_MEDIUM, SENSOR_STATUS_ACCURACY_HIGH)
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String accuracyStr;
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_NO_CONTACT:
                accuracyStr = "NO_CONTACT";
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                accuracyStr = "UNRELIABLE";
                Log.w(TAG, "Sensor accuracy is UNRELIABLE - consider calibrating compass");
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                accuracyStr = "LOW";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                accuracyStr = "MEDIUM";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                accuracyStr = "HIGH";
                break;
            default:
                accuracyStr = "UNKNOWN";
        }
        Log.d(TAG, "Sensor accuracy changed: " + accuracyStr);
    }
}