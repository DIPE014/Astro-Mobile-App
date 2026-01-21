package com.astro.app.common;

import com.astro.app.common.model.Pointing;
import com.astro.app.common.model.StarData;
import java.util.List;

/**
 * SHARED INTERFACE - Contract between Frontend and Backend
 *
 * Backend implements this, Frontend calls it.
 */
public interface SkyDataProvider {

    /**
     * Get current pointing direction of the phone.
     * Backend: calculates from sensors + location
     * Frontend: uses to know where user is looking
     */
    Pointing getCurrentPointing();

    /**
     * Get list of stars visible in current view.
     * Backend: filters stars based on pointing + field of view
     * Frontend: renders these on screen
     */
    List<StarData> getVisibleStars();

    /**
     * Get all stars (for search, etc.)
     */
    List<StarData> getAllStars();

    /**
     * Start receiving sensor/location updates.
     */
    void start();

    /**
     * Stop updates (save battery).
     */
    void stop();
}
