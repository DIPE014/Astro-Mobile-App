package com.astro.app.core.control;

/**
 * Provides a time reading.
 *
 * This interface allows for time to be mocked for testing,
 * or for implementing "time travel" features where users can
 * see the sky at different times.
 */
public interface Clock {

    /**
     * Gets the current time in milliseconds since the Unix epoch (January 1, 1970).
     *
     * @return time in milliseconds since epoch
     */
    long getTimeInMillisSinceEpoch();
}
