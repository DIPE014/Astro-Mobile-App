package com.astro.app.data.model;

/**
 * Holds the result of a sky brightness analysis, including the estimated
 * Bortle class, surface brightness in mag/arcsec², calibration metadata,
 * EXIF data, and an observing tip.
 */
public class SkyBrightnessResult {

    private final int bortleClass;
    private final String label;
    private final String description;
    private final double surfaceBrightness;    // mag/arcsec² (0 if uncalibrated)
    private final double normalizedBrightness; // EV-normalised proxy (fallback path)
    private final double medianPixelValue;
    private final int iso;
    private final double exposureTime;
    private final double fNumber;
    private final boolean hasExifData;
    private final boolean hasCalibration;      // true when star-based zero-point was used
    private final int calibrationStarCount;    // number of catalogue stars matched
    private final double zeroPoint;            // photometric ZP (0 if uncalibrated)
    private final boolean cloudWarning;        // true if star residual scatter is high
    private final String tip;

    private SkyBrightnessResult(int bortleClass, String label, String description,
                                double surfaceBrightness, double normalizedBrightness,
                                double medianPixelValue,
                                int iso, double exposureTime, double fNumber,
                                boolean hasExifData, boolean hasCalibration,
                                int calibrationStarCount, double zeroPoint,
                                boolean cloudWarning, String tip) {
        this.bortleClass = bortleClass;
        this.label = label;
        this.description = description;
        this.surfaceBrightness = surfaceBrightness;
        this.normalizedBrightness = normalizedBrightness;
        this.medianPixelValue = medianPixelValue;
        this.iso = iso;
        this.exposureTime = exposureTime;
        this.fNumber = fNumber;
        this.hasExifData = hasExifData;
        this.hasCalibration = hasCalibration;
        this.calibrationStarCount = calibrationStarCount;
        this.zeroPoint = zeroPoint;
        this.cloudWarning = cloudWarning;
        this.tip = tip;
    }

    // --- Getters ---

    public int getBortleClass() { return bortleClass; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    /** Surface brightness in mag/arcsec². Only meaningful when {@link #hasCalibration()} is true. */
    public double getSurfaceBrightness() { return surfaceBrightness; }
    public double getNormalizedBrightness() { return normalizedBrightness; }
    public double getMedianPixelValue() { return medianPixelValue; }
    public int getIso() { return iso; }
    public double getExposureTime() { return exposureTime; }
    public double getFNumber() { return fNumber; }
    public boolean hasExifData() { return hasExifData; }
    /** True when a star-based photometric zero-point was computed. */
    public boolean hasCalibration() { return hasCalibration; }
    public int getCalibrationStarCount() { return calibrationStarCount; }
    public double getZeroPoint() { return zeroPoint; }
    /** True if calibration star residuals suggest cloud or haze contamination. */
    public boolean hasCloudWarning() { return cloudWarning; }
    public String getTip() { return tip; }

    // --- Static helpers ---

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
     * Factory for the calibrated path (star-based zero-point, mag/arcsec² output).
     */
    public static SkyBrightnessResult createCalibrated(
            int bortleClass, double surfaceBrightness, double medianPixelValue,
            int iso, double exposureTime, double fNumber, boolean hasExifData,
            int calibrationStarCount, double zeroPoint, boolean cloudWarning) {
        int clamped = Math.max(1, Math.min(9, bortleClass));
        return new SkyBrightnessResult(
                clamped,
                labelForBortle(clamped),
                descriptionForBortle(clamped),
                surfaceBrightness,
                0.0,  // normalizedBrightness not used in calibrated path
                medianPixelValue,
                iso, exposureTime, fNumber, hasExifData,
                true, calibrationStarCount, zeroPoint, cloudWarning,
                tipForBortle(clamped));
    }

    /**
     * Factory for the EXIF fallback path (EV-normalised, no mag/arcsec²).
     */
    public static SkyBrightnessResult createFromExif(
            int bortleClass, double normalizedBrightness, double medianPixelValue,
            int iso, double exposureTime, double fNumber) {
        int clamped = Math.max(1, Math.min(9, bortleClass));
        return new SkyBrightnessResult(
                clamped,
                labelForBortle(clamped),
                descriptionForBortle(clamped),
                0.0,  // no calibrated surface brightness
                normalizedBrightness,
                medianPixelValue,
                iso, exposureTime, fNumber, true,
                false, 0, 0.0, false,
                tipForBortle(clamped));
    }

    /**
     * Factory for the raw fallback path (no EXIF, no calibration).
     */
    public static SkyBrightnessResult createFromRawMedian(
            int bortleClass, double medianPixelValue) {
        int clamped = Math.max(1, Math.min(9, bortleClass));
        return new SkyBrightnessResult(
                clamped,
                labelForBortle(clamped),
                descriptionForBortle(clamped),
                0.0, 0.0,
                medianPixelValue,
                0, 0.0, 0.0, false,
                false, 0, 0.0, false,
                tipForBortle(clamped));
    }

    /**
     * Legacy factory retained for backward compatibility with existing callers.
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
                0.0,
                normalizedBrightness,
                medianPixelValue,
                iso, exposureTime, fNumber, hasExifData,
                false, 0, 0.0, false,
                tipForBortle(clamped));
    }

    @Override
    public String toString() {
        return "SkyBrightnessResult{bortle=" + bortleClass +
                ", label='" + label + '\'' +
                (hasCalibration ? ", SB=" + surfaceBrightness + " mag/arcsec²"
                                : ", normalized=" + normalizedBrightness) +
                ", median=" + medianPixelValue +
                ", hasExif=" + hasExifData +
                ", calibrated=" + hasCalibration +
                (cloudWarning ? ", CLOUD_WARNING" : "") + '}';
    }
}
