package com.astro.app.common.model;

/**
 * SHARED MODEL - Used by both Frontend and Backend
 *
 * Result from ML constellation recognition.
 */
public class ConstellationResult {

    public String name;           // e.g., "Orion"
    public float confidence;      // 0.0 to 1.0
    public String description;    // Info about the constellation

    public ConstellationResult() {}

    public ConstellationResult(String name, float confidence) {
        this.name = name;
        this.confidence = confidence;
    }
}
