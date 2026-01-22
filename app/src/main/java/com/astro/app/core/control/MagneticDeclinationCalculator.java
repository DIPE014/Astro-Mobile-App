package com.astro.app.core.control;

import com.astro.app.core.math.LatLong;

/**
 * Calculator for magnetic declination.
 *
 * Magnetic declination is the angle between magnetic north (where a compass points)
 * and true north (geographic north). This varies based on location and time.
 *
 * The AstronomerModel uses this to correct for the difference between
 * the magnetometer reading and true north when calculating celestial coordinates.
 */
public interface MagneticDeclinationCalculator {

    /**
     * Gets the magnetic declination in degrees.
     * Positive values indicate that magnetic north is east of true north.
     * Negative values indicate that magnetic north is west of true north.
     *
     * @return the magnetic declination in degrees
     */
    float getDeclination();

    /**
     * Sets the user's location and time for declination calculation.
     * This should be called when the user's location or time changes.
     *
     * @param location the user's latitude/longitude
     * @param timeInMillis the current time in milliseconds since epoch
     */
    void setLocationAndTime(LatLong location, long timeInMillis);
}
