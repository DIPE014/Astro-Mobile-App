package com.astro.app.ui.skybrightness;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.media.ExifInterface;

import com.astro.app.data.model.SkyBrightnessResult;

import java.util.Arrays;

/**
 * Analyzes a sky photograph to estimate the Bortle dark-sky class.
 *
 * <p>The algorithm works as follows:</p>
 * <ol>
 *   <li>Crop to the center 50 % of the image to avoid lens vignetting.</li>
 *   <li>Convert to grayscale and compute the median pixel value.</li>
 *   <li>Linearize (undo sRGB gamma): {@code linear = pow(pixel / 255.0, 2.2)}.</li>
 *   <li>If EXIF exposure data is available, normalize:
 *       {@code normalized = linear * (f^2) / (ISO * exposure)}.</li>
 *   <li>Map the normalized (or raw) brightness to a Bortle class 1-9.</li>
 * </ol>
 */
public class SkyBrightnessAnalyzer {

    private static final String TAG = "SkyBrightnessAnalyzer";

    private SkyBrightnessAnalyzer() {
        // Utility class
    }

    /**
     * Analyze a sky image and return a {@link SkyBrightnessResult}.
     *
     * @param bitmap The sky image bitmap (must not be recycled).
     * @param exif   Optional EXIF data extracted from the original file/URI.
     *               May be {@code null} if unavailable.
     * @return The analysis result including estimated Bortle class.
     */
    @NonNull
    public static SkyBrightnessResult analyze(@NonNull Bitmap bitmap,
                                              @Nullable ExifInterface exif) {

        // --- Step 1: Extract center 50 % crop as grayscale values ---
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int startX = width / 4;
        int endX = 3 * width / 4;
        int startY = height / 4;
        int endY = 3 * height / 4;
        int cropW = endX - startX;
        int cropH = endY - startY;
        int totalPixels = cropW * cropH;

        int[] grayValues = new int[totalPixels];
        int[] rowPixels = new int[cropW];
        int idx = 0;

        for (int y = startY; y < endY; y++) {
            bitmap.getPixels(rowPixels, 0, cropW, startX, y, cropW, 1);
            for (int i = 0; i < cropW; i++) {
                int pixel = rowPixels[i];
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                // Standard luminance weights (ITU-R BT.601)
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                grayValues[idx++] = gray;
            }
        }

        // --- Step 2: Compute median ---
        double medianValue = computeMedian(grayValues);

        // --- Step 3: Linearize (undo sRGB gamma) ---
        double medianNormalized = medianValue / 255.0;
        double linearMedian = Math.pow(medianNormalized, 2.2);

        // --- Step 4 & 5: Normalize with EXIF or fall back to raw median ---
        int iso = 0;
        double exposureTime = 0.0;
        double fNumber = 0.0;
        boolean hasExif = false;
        double normalizedBrightness = linearMedian;
        int bortleClass;

        if (exif != null) {
            // Try standard EXIF ISO tag
            @SuppressWarnings("deprecation")
            int isoValue = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0);
            iso = isoValue;
            exposureTime = parseExposureTime(exif);
            fNumber = parseFNumber(exif);
        }

        if (iso > 0 && exposureTime > 0.0 && fNumber > 0.0) {
            hasExif = true;
            normalizedBrightness = linearMedian * (fNumber * fNumber) / (iso * exposureTime);
            bortleClass = estimateBortle(normalizedBrightness);
            Log.d(TAG, String.format("EXIF: ISO=%d, exp=%.4fs, f/%.1f  normalized=%.6f  bortle=%d",
                    iso, exposureTime, fNumber, normalizedBrightness, bortleClass));
        } else {
            hasExif = false;
            normalizedBrightness = linearMedian;
            bortleClass = estimateBortleFromRawMedian(medianNormalized);
            Log.d(TAG, String.format("No EXIF. rawMedian=%.4f  linearMedian=%.6f  bortle=%d",
                    medianNormalized, linearMedian, bortleClass));
        }

        return SkyBrightnessResult.create(bortleClass, normalizedBrightness,
                medianValue, iso, exposureTime, fNumber, hasExif);
    }

    // -------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------

    /**
     * Computes the median of an integer array by sorting and picking the
     * middle value (or averaging the two middle values for even-length arrays).
     */
    static double computeMedian(@NonNull int[] values) {
        if (values.length == 0) return 0.0;
        Arrays.sort(values);
        int mid = values.length / 2;
        if (values.length % 2 == 0) {
            return (values[mid - 1] + values[mid]) / 2.0;
        } else {
            return values[mid];
        }
    }

    /**
     * Maps EXIF-normalized brightness to Bortle class 1-9.
     * Lower normalized brightness means darker sky.
     */
    static int estimateBortle(double normalizedBrightness) {
        if (normalizedBrightness < 0.0001) return 1;
        if (normalizedBrightness < 0.0003) return 2;
        if (normalizedBrightness < 0.001)  return 3;
        if (normalizedBrightness < 0.003)  return 4;
        if (normalizedBrightness < 0.01)   return 5;
        if (normalizedBrightness < 0.03)   return 6;
        if (normalizedBrightness < 0.1)    return 7;
        if (normalizedBrightness < 0.3)    return 8;
        return 9;
    }

    /**
     * Fallback mapping when EXIF data is not available.
     * Uses the raw sRGB-space median (0-1 range) directly.
     * Less accurate because camera settings are unknown.
     */
    static int estimateBortleFromRawMedian(double medianNormalized) {
        if (medianNormalized < 0.02) return 1;
        if (medianNormalized < 0.04) return 2;
        if (medianNormalized < 0.06) return 3;
        if (medianNormalized < 0.10) return 4;
        if (medianNormalized < 0.15) return 5;
        if (medianNormalized < 0.25) return 6;
        if (medianNormalized < 0.40) return 7;
        if (medianNormalized < 0.60) return 8;
        return 9;
    }

    /**
     * Parses exposure time from EXIF. Handles both rational (e.g. "1/60")
     * and decimal string formats.
     */
    private static double parseExposureTime(@NonNull ExifInterface exif) {
        String raw = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
        if (raw == null || raw.isEmpty()) return 0.0;
        try {
            if (raw.contains("/")) {
                String[] parts = raw.split("/");
                double num = Double.parseDouble(parts[0].trim());
                double den = Double.parseDouble(parts[1].trim());
                return den != 0 ? num / den : 0.0;
            }
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse exposure time: " + raw, e);
            return 0.0;
        }
    }

    /**
     * Parses f-number from EXIF. Handles rational and decimal formats.
     */
    private static double parseFNumber(@NonNull ExifInterface exif) {
        String raw = exif.getAttribute(ExifInterface.TAG_F_NUMBER);
        if (raw == null || raw.isEmpty()) return 0.0;
        try {
            if (raw.contains("/")) {
                String[] parts = raw.split("/");
                double num = Double.parseDouble(parts[0].trim());
                double den = Double.parseDouble(parts[1].trim());
                return den != 0 ? num / den : 0.0;
            }
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse f-number: " + raw, e);
            return 0.0;
        }
    }
}
