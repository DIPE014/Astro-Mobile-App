package com.astro.app.core.control;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * BACKEND - Person B
 *
 * Handles device orientation sensors (accelerometer, magnetometer, gyroscope).
 * Provides rotation data to AstronomerModel.
 */
public class SensorController implements SensorEventListener {

    public interface SensorListener {
        void onOrientationChanged(float[] rotationVector);
    }

    private final SensorManager sensorManager;
    private final Sensor rotationSensor;
    private SensorListener listener;

    public SensorController(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // Use rotation vector sensor (fused accelerometer + magnetometer + gyroscope)
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    public void setListener(SensorListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (rotationSensor != null) {
            sensorManager.registerListener(
                    this,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_GAME  // Fast updates for smooth AR
            );
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && listener != null) {
            listener.onOrientationChanged(event.values);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO: Handle accuracy changes (e.g., prompt compass calibration)
    }
}
