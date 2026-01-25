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
 * Provides the current time as milliseconds since the Unix epoch.
 *
 * @return the current time in milliseconds since January 1, 1970 UTC
 */
    long getTimeInMillisSinceEpoch();
}