package com.astro.app.core.control;

/**
 * Clock implementation that supports time travel.
 *
 * <p>This clock can either return real time or a frozen/offset time,
 * allowing users to see the sky at any date/time in history or future.</p>
 *
 * <p>Time travel modes:</p>
 * <ul>
 *   <li><b>Real time</b>: Returns actual current time (default)</li>
 *   <li><b>Frozen time</b>: Returns a fixed time that doesn't change</li>
 *   <li><b>Running from offset</b>: Returns time running from a different starting point</li>
 * </ul>
 */
public class TimeTravelClock implements Clock {

    /** Real time clock for reference */
    private final RealClock realClock = new RealClock();

    /** Offset from real time in milliseconds (can be negative for past) */
    private long timeOffsetMillis = 0;

    /** If true, time is frozen at the target time and doesn't advance */
    private boolean isFrozen = false;

    /** The frozen time value (only used when isFrozen is true) */
    private long frozenTimeMillis = 0;

    /** Whether time travel mode is active */
    private boolean isTimeTravelActive = false;

    /** Listener for time travel state changes */
    private TimeTravelListener listener;

    /**
     * Listener interface for time travel state changes.
     */
    public interface TimeTravelListener {
        /**
         * Called when time travel mode is activated or deactivated.
         *
         * @param active true if time travel is now active
         */
        void onTimeTravelStateChanged(boolean active);

        /**
         * Called when the time travel time changes.
         *
         * @param timeMillis the new time in milliseconds
         */
        void onTimeTravelTimeChanged(long timeMillis);
    }

    /**
     * Get the current time in milliseconds since the Unix epoch according to the clock's mode.
     *
     * If time travel is inactive this returns the underlying real clock time, if frozen it
     * returns the stored frozen time, otherwise it returns the real clock time plus the
     * configured time offset.
     *
     * @return the current time in milliseconds since the Unix epoch (real time when time travel is
     *         inactive, frozen time when frozen, or real time plus offset when time travel is active)
     */
    @Override
    public long getTimeInMillisSinceEpoch() {
        if (!isTimeTravelActive) {
            return realClock.getTimeInMillisSinceEpoch();
        }

        if (isFrozen) {
            return frozenTimeMillis;
        }

        return realClock.getTimeInMillisSinceEpoch() + timeOffsetMillis;
    }

    /**
     * Registers a TimeTravelListener to receive time-travel state and time change events.
     *
     * @param listener the listener to register, or {@code null} to remove the current listener
     */
    public void setListener(TimeTravelListener listener) {
        this.listener = listener;
    }

    /**
     * Activates time travel mode with a specific target time.
     *
     * @param targetTimeMillis the target time to travel to
     * @param frozen if true, time freezes at target; if false, time runs from target
     */
    public void travelToTime(long targetTimeMillis, boolean frozen) {
        this.isTimeTravelActive = true;
        this.isFrozen = frozen;

        if (frozen) {
            this.frozenTimeMillis = targetTimeMillis;
        } else {
            // Calculate offset so current real time + offset = target time
            this.timeOffsetMillis = targetTimeMillis - realClock.getTimeInMillisSinceEpoch();
        }

        notifyStateChanged(true);
        notifyTimeChanged(targetTimeMillis);
    }

    /**
     * Set the clock to the specified local date and time and freeze time there.
     *
     * The specified values are interpreted in the system default time zone; seconds and milliseconds are cleared.
     *
     * @param year  the year
     * @param month the month (1-12)
     * @param day   the day of month
     * @param hour  the hour of day (0-23)
     * @param minute the minute (0-59)
     */
    public void travelToDateTime(int year, int month, int day, int hour, int minute) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(year, month - 1, day, hour, minute, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        travelToTime(cal.getTimeInMillis(), true);
    }

    /**
     * Shift the clock's current time-travel target by the given millisecond delta.
     *
     * If time travel is not active, this activates time travel in frozen mode at
     * (real current time + deltaMillis). If time travel is active and frozen, the
     * frozen reference time is moved by deltaMillis; if active and running, the
     * running offset is adjusted by deltaMillis.
     *
     * @param deltaMillis milliseconds to shift the time-travel target; positive moves forward, negative moves backward
     */
    public void adjustTime(long deltaMillis) {
        if (!isTimeTravelActive) {
            // Start time travel from current time + delta
            travelToTime(realClock.getTimeInMillisSinceEpoch() + deltaMillis, true);
            return;
        }

        if (isFrozen) {
            frozenTimeMillis += deltaMillis;
            notifyTimeChanged(frozenTimeMillis);
        } else {
            timeOffsetMillis += deltaMillis;
            notifyTimeChanged(realClock.getTimeInMillisSinceEpoch() + timeOffsetMillis);
        }
    }

    /**
     * Exits time-travel mode and restores the clock to real time.
     *
     * Resets internal time-travel state (active flag, frozen flag, offsets) and notifies the listener
     * that time travel has been deactivated and that the current time is the real current time.
     */
    public void returnToRealTime() {
        this.isTimeTravelActive = false;
        this.isFrozen = false;
        this.timeOffsetMillis = 0;
        this.frozenTimeMillis = 0;
        notifyStateChanged(false);
        notifyTimeChanged(realClock.getTimeInMillisSinceEpoch());
    }

    /**
     * Toggle between frozen and running time travel modes.
     *
     * If time travel is not active this method does nothing. When switching from frozen to running,
     * the running offset is adjusted so the clock continues from the current frozen time. When
     * switching from running to frozen, the clock is frozen at the current effective time.
     */
    public void toggleFrozen() {
        if (!isTimeTravelActive) return;

        if (isFrozen) {
            // Start running from frozen time
            timeOffsetMillis = frozenTimeMillis - realClock.getTimeInMillisSinceEpoch();
            isFrozen = false;
        } else {
            // Freeze at current offset time
            frozenTimeMillis = realClock.getTimeInMillisSinceEpoch() + timeOffsetMillis;
            isFrozen = true;
        }
    }

    /**
         * Indicates whether time travel mode is active.
         *
         * @return `true` if time travel mode is active, `false` otherwise
         */
    public boolean isTimeTravelActive() {
        return isTimeTravelActive;
    }

    /**
     * Indicates whether time travel is currently frozen.
     *
     * @return `true` if time travel is frozen, `false` otherwise.
     */
    public boolean isFrozen() {
        return isFrozen;
    }

    /**
     * Returns the clock's current effective time in milliseconds since the Unix epoch.
     *
     * @return the current effective time in milliseconds since epoch
     */
    public long getCurrentTimeMillis() {
        return getTimeInMillisSinceEpoch();
    }

    /**
     * Notify the registered TimeTravelListener that time travel mode has been activated or deactivated.
     *
     * @param active `true` if time travel is now active, `false` if it has been deactivated
     */
    private void notifyStateChanged(boolean active) {
        if (listener != null) {
            listener.onTimeTravelStateChanged(active);
        }
    }

    /**
     * Notifies the registered TimeTravelListener that the current time-travel time has changed.
     *
     * @param timeMillis the new time in milliseconds since epoch to report to the listener
     */
    private void notifyTimeChanged(long timeMillis) {
        if (listener != null) {
            listener.onTimeTravelTimeChanged(timeMillis);
        }
    }
}