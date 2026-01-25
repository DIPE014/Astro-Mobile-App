package com.astro.app.core.control;

/**
 * Provides the actual current time from the system.
 *
 * This is the default Clock implementation used in production.
 */
public class RealClock implements Clock {

    /**
     * Gets the current time in milliseconds since the Unix epoch.
     *
     * @return the current time in milliseconds since January 1, 1970 UTC
     */
    @Override
    public long getTimeInMillisSinceEpoch() {
        return System.currentTimeMillis();
    }
}