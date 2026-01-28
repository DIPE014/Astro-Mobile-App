package com.astro.app.core.control;

/**
 * Provides the actual current time from the system.
 *
 * This is the default Clock implementation used in production.
 */
public class RealClock implements Clock {

    @Override
    public long getTimeInMillisSinceEpoch() {
        return System.currentTimeMillis();
    }
}
