package com.astro.app.core.control;

import android.hardware.GeomagneticField;

import com.astro.app.core.math.LatLong;

/**
 * Real implementation of MagneticDeclinationCalculator using Android's GeomagneticField.
 *
 * Uses the World Magnetic Model (WMM) to calculate the difference between
 * magnetic north and true north at the user's location.
 */
public class RealMagneticDeclinationCalculator implements MagneticDeclinationCalculator {

    // Cached magnetic field calculation
    private GeomagneticField geomagneticField;

    // Default to 0 until location is set
    private float declination = 0f;

    @Override
    public float getDeclination() {
        return declination;
    }

    @Override
    public void setLocationAndTime(LatLong location, long timeInMillis) {
        if (location == null) {
            declination = 0f;
            return;
        }

        // Create new geomagnetic field calculation
        // The altitude parameter (0) assumes sea level, which is accurate enough
        // for declination calculations
        geomagneticField = new GeomagneticField(
                location.getLatitude(),
                location.getLongitude(),
                0f,  // altitude in meters (sea level approximation)
                timeInMillis
        );

        // Get declination in degrees
        // Positive values mean magnetic north is east of true north
        declination = geomagneticField.getDeclination();
    }
}
