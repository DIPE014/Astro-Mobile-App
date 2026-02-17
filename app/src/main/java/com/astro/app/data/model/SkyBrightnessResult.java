package com.astro.app.data.model;

/**
 * Holds the result of a sky brightness analysis, including the estimated
 * Bortle class, descriptive label, EXIF metadata, and an observing tip.
 */
public class SkyBrightnessResult {

    private final int bortleClass;
    private final String label;
    private final String description;
    private final double normalizedBrightness;
    private final double medianPixelValue;
    private final int iso;
    private final double exposureTime;
    private final double fNumber;
    private final boolean hasExifData;
    private final String tip;

    private SkyBrightnessResult(int bortleClass, String label, String description,
                                double normalizedBrightness, double medianPixelValue,
                                int iso, double exposureTime, double fNumber,
                                boolean hasExifData, String tip) {
        this.bortleClass = bortleClass;
        this.label = label;
        this.description = description;
        this.normalizedBrightness = normalizedBrightness;
        this.medianPixelValue = medianPixelValue;
        this.iso = iso;
        this.exposureTime = exposureTime;
        this.fNumber = fNumber;
        this.hasExifData = hasExifData;
        this.tip = tip;
    }

    // --- Getters ---

    public int getBortleClass() {
        return bortleClass;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public double getNormalizedBrightness() {
        return normalizedBrightness;
    }

    public double getMedianPixelValue() {
        return medianPixelValue;
    }

    public int getIso() {
        return iso;
    }

    public double getExposureTime() {
        return exposureTime;
    }

    public double getFNumber() {
        return fNumber;
    }

    public boolean hasExifData() {
        return hasExifData;
    }

    public String getTip() {
        return tip;
    }

    // --- Static helpers ---

    /**
     * Returns the human-readable label for a given Bortle class (1-9).
     */
    public static String labelForBortle(int bortle) {
        switch (bortle) {
            case 1:  return "Excellent Dark Site";
            case 2:  return "Typical Dark Site";
            case 3:  return "Rural Sky";
            case 4:  return "Rural/Suburban Transition";
            case 5:  return "Suburban Sky";
            case 6:  return "Bright Suburban";
            case 7:  return "Suburban/Urban Transition";
            case 8:  return "City Sky";
            case 9:  return "Inner City";
            default: return "Unknown";
        }
    }

    /**
     * Returns a short description of observing conditions for the given Bortle class.
     */
    public static String descriptionForBortle(int bortle) {
        switch (bortle) {
            case 1:  return "Perfect for deep sky imaging and faintest objects";
            case 2:  return "Great for visual observation and astrophotography";
            case 3:  return "Milky Way visible with good detail";
            case 4:  return "Milky Way visible but lacks detail";
            case 5:  return "Milky Way only visible near zenith";
            case 6:  return "Only brightest Milky Way visible";
            case 7:  return "Milky Way invisible, major constellations visible";
            case 8:  return "Only bright planets and few stars visible";
            case 9:  return "Only Moon, planets, and few bright stars visible";
            default: return "";
        }
    }

    /**
     * Returns an observing tip appropriate for the given Bortle class range.
     */
    public static String tipForBortle(int bortle) {
        if (bortle <= 3) {
            return "Excellent conditions for deep sky photography!";
        } else if (bortle <= 5) {
            return "Good for bright nebulae and star clusters. Use longer exposures.";
        } else if (bortle <= 7) {
            return "Try narrowband filters to cut through light pollution.";
        } else {
            return "Consider traveling to a darker site for better results.";
        }
    }

    /**
     * Factory method that builds a complete {@link SkyBrightnessResult} from raw
     * analysis values. The Bortle class is used to look up label, description, and tip.
     */
    public static SkyBrightnessResult create(int bortleClass, double normalizedBrightness,
                                             double medianPixelValue, int iso,
                                             double exposureTime, double fNumber,
                                             boolean hasExifData) {
        int clamped = Math.max(1, Math.min(9, bortleClass));
        return new SkyBrightnessResult(
                clamped,
                labelForBortle(clamped),
                descriptionForBortle(clamped),
                normalizedBrightness,
                medianPixelValue,
                iso,
                exposureTime,
                fNumber,
                hasExifData,
                tipForBortle(clamped)
        );
    }

    @Override
    public String toString() {
        return "SkyBrightnessResult{bortle=" + bortleClass +
                ", label='" + label + '\'' +
                ", normalized=" + normalizedBrightness +
                ", median=" + medianPixelValue +
                ", hasExif=" + hasExifData + '}';
    }
}
