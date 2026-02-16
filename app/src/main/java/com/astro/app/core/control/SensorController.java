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
        void onOrientationChanged(float[] rotationVector);
    }

    private final SensorManager sensorManager;
    private final Sensor rotationSensor;
    private SensorListener listener;
    private boolean isRegistered = false;

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

    public void setListener(SensorListener listener) {
        this.listener = listener;
    }

    /**
     * Checks if the rotation sensor is available on this device.
     *
     * @return true if the device has a rotation sensor
     */
    public boolean isSensorAvailable() {
        return rotationSensor != null;
    }

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

    public void stop() {
        if (isRegistered) {
            sensorManager.unregisterListener(this);
            isRegistered = false;
            Log.d(TAG, "Sensor listener unregistered");
        }
    }

    /**
     * Temporarily pause sensor updates without fully unregistering.
     * Used when manual mode is active to prevent conflicting updates.
     */
    public void pause() {
        if (isRegistered) {
            sensorManager.unregisterListener(this);
            isRegistered = false;
            Log.d(TAG, "Sensor listener paused (manual mode)");
        }
    }

    /**
     * Resume sensor updates after pause.
     * Restores sensor tracking when returning from manual mode.
     */
    public void resume() {
        if (rotationSensor != null && !isRegistered) {
            boolean registered = sensorManager.registerListener(
                    this,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_GAME
            );
            isRegistered = registered;
            if (registered) {
                Log.d(TAG, "Sensor listener resumed from pause");
            } else {
                Log.e(TAG, "Failed to resume sensor listener");
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && listener != null) {
            // Clone the array to prevent issues with mutable sensor data being reused
            listener.onOrientationChanged(event.values.clone());
        }
    }

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
