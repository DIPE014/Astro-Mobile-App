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
     * Sets the listener for time travel events.
     *
     * @param listener the listener, or null to remove
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
     * Travels to a specific date/time and freezes there.
     *
     * @param year the year
     * @param month the month (1-12)
     * @param day the day of month
     * @param hour the hour (0-23)
     * @param minute the minute (0-59)
     */
    public void travelToDateTime(int year, int month, int day, int hour, int minute) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(year, month - 1, day, hour, minute, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        travelToTime(cal.getTimeInMillis(), true);
    }

    /**
     * Adjusts the current time travel time by a delta.
     *
     * @param deltaMillis milliseconds to add (negative to go back)
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
     * Returns to real time.
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
     * Toggles between frozen and running time travel.
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
     * Checks if time travel is currently active.
     *
     * @return true if in time travel mode
     */
    public boolean isTimeTravelActive() {
        return isTimeTravelActive;
    }

    /**
     * Checks if time is currently frozen.
     *
     * @return true if time is frozen
     */
    public boolean isFrozen() {
        return isFrozen;
    }

    /**
     * Gets the current time travel time.
     *
     * @return the current time in milliseconds
     */
    public long getCurrentTimeMillis() {
        return getTimeInMillisSinceEpoch();
    }

    private void notifyStateChanged(boolean active) {
        if (listener != null) {
            listener.onTimeTravelStateChanged(active);
        }
    }

    private void notifyTimeChanged(long timeMillis) {
        if (listener != null) {
            listener.onTimeTravelTimeChanged(timeMillis);
        }
    }
}
