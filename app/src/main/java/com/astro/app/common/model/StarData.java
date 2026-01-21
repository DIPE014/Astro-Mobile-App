package com.astro.app.common.model;

/**
 * SHARED MODEL - Used by both Frontend and Backend
 *
 * Represents a star with its celestial and screen coordinates.
 */
public class StarData {

    // Celestial coordinates (Backend calculates these)
    public float rightAscension;  // RA in degrees (0-360)
    public float declination;     // Dec in degrees (-90 to +90)

    // Screen coordinates (Frontend uses these for rendering)
    public float screenX;
    public float screenY;

    // Display info
    public String name;
    public float magnitude;       // Brightness (-1 to 6, lower = brighter)
    public int color;             // ARGB color

    // Visibility
    public boolean isVisible;

    public StarData() {}

    public StarData(String name, float ra, float dec, float magnitude) {
        this.name = name;
        this.rightAscension = ra;
        this.declination = dec;
        this.magnitude = magnitude;
        this.isVisible = true;
    }
}
