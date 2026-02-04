package com.astro.app.native_;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * JNI interface to astrometry.net C library for star detection and plate solving.
 */
public class AstrometryNative {
    private static final String TAG = "AstrometryNative";
    private static boolean libraryLoaded = false;

    static {
        try {
            System.loadLibrary("astrometry_native");
            libraryLoaded = true;
            Log.i(TAG, "Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library: " + e.getMessage());
            libraryLoaded = false;
        }
    }

    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }

    /**
     * Detect stars in grayscale image.
     * @param imageData Grayscale pixels (row-major, 0-255)
     * @param width Image width
     * @param height Image height
     * @param plim Detection threshold (default: 8.0)
     * @param dpsf PSF sigma (default: 1.0)
     * @param downsample Downsample factor (default: 2)
     * @return Array [x0,y0,flux0, x1,y1,flux1, ...] or null
     */
    public static native float[] detectStarsNative(
        byte[] imageData, int width, int height,
        float plim, float dpsf, int downsample
    );

    /**
     * Solve field using detected stars and index files.
     * @param starXY Array of [x0,y0,flux0, x1,y1,flux1, ...]
     * @param numStars Number of stars
     * @param imageWidth Image width
     * @param imageHeight Image height
     * @param indexPaths Paths to index files
     * @param scaleLow Lower pixel scale bound (arcsec/pixel)
     * @param scaleHigh Upper pixel scale bound (arcsec/pixel)
     * @param logOddsThreshold Minimum log-odds to accept solution
     * @return Array [solved, ra, dec, crpixX, crpixY, cd11, cd12, cd21, cd22, pixelScale, rotation, logOdds]
     */
    public static native double[] solveFieldNative(
        float[] starXY, int numStars,
        int imageWidth, int imageHeight,
        String[] indexPaths,
        double scaleLow, double scaleHigh,
        double logOddsThreshold
    );

    /**
     * Represents a detected star with position and flux.
     */
    public static class NativeStar {
        public final float x;
        public final float y;
        public final float flux;

        public NativeStar(float x, float y, float flux) {
            this.x = x;
            this.y = y;
            this.flux = flux;
        }
    }

    /**
     * Detect stars in a bitmap image.
     * @param bitmap Input bitmap (will be converted to grayscale)
     * @param plim Detection threshold (default: 8.0)
     * @param dpsf PSF sigma (default: 1.0)
     * @param downsample Downsample factor (default: 2)
     * @return List of detected stars, or null on failure
     */
    public static List<NativeStar> detectStars(Bitmap bitmap, float plim, float dpsf, int downsample) {
        if (!libraryLoaded) {
            Log.e(TAG, "Native library not loaded");
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Convert bitmap to grayscale bytes
        byte[] grayscale = bitmapToGrayscale(bitmap);

        // Call native detection
        float[] result = detectStarsNative(grayscale, width, height, plim, dpsf, downsample);

        if (result == null || result.length == 0) {
            return null;
        }

        // Convert to list of NativeStar
        List<NativeStar> stars = new ArrayList<>();
        for (int i = 0; i < result.length; i += 3) {
            stars.add(new NativeStar(result[i], result[i + 1], result[i + 2]));
        }

        return stars;
    }

    /**
     * Convert bitmap to grayscale byte array.
     */
    public static byte[] bitmapToGrayscale(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        byte[] grayscale = new byte[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            // Standard luminance formula
            grayscale[i] = (byte) ((0.299 * r + 0.587 * g + 0.114 * b));
        }

        return grayscale;
    }

    /**
     * Result of plate solving operation.
     */
    public static class SolveResult {
        public final boolean solved;
        public final double ra;           // Right ascension in degrees
        public final double dec;          // Declination in degrees
        public final double crpixX;       // Reference pixel X
        public final double crpixY;       // Reference pixel Y
        public final double[] cd;         // CD matrix [4] = {cd11, cd12, cd21, cd22}
        public final double pixelScale;   // Arcseconds per pixel
        public final double rotation;     // Field rotation in degrees
        public final double logOdds;      // Confidence measure

        public SolveResult(double[] result) {
            this.solved = result[0] > 0.5;
            this.ra = result[1];
            this.dec = result[2];
            this.crpixX = result[3];
            this.crpixY = result[4];
            this.cd = new double[] {result[5], result[6], result[7], result[8]};
            this.pixelScale = result[9];
            this.rotation = result[10];
            this.logOdds = result[11];
        }

        public static SolveResult failed() {
            return new SolveResult(new double[12]);
        }
    }

    /**
     * Solve field given detected stars and index files.
     * @param stars List of detected stars
     * @param imageWidth Image width
     * @param imageHeight Image height
     * @param indexPaths Paths to index files
     * @param scaleLow Lower pixel scale bound (arcsec/pixel)
     * @param scaleHigh Upper pixel scale bound (arcsec/pixel)
     * @return SolveResult with solution or failed status
     */
    public static SolveResult solveField(
            List<NativeStar> stars,
            int imageWidth,
            int imageHeight,
            String[] indexPaths,
            double scaleLow,
            double scaleHigh) {

        if (!libraryLoaded) {
            Log.e(TAG, "Native library not loaded");
            return SolveResult.failed();
        }

        if (stars == null || stars.isEmpty()) {
            Log.e(TAG, "No stars provided");
            return SolveResult.failed();
        }

        // Convert stars to flat array
        float[] starXY = new float[stars.size() * 3];
        for (int i = 0; i < stars.size(); i++) {
            NativeStar s = stars.get(i);
            starXY[i * 3] = s.x;
            starXY[i * 3 + 1] = s.y;
            starXY[i * 3 + 2] = s.flux;
        }

        // Call native solver
        double[] result = solveFieldNative(starXY, stars.size(),
                imageWidth, imageHeight, indexPaths, scaleLow, scaleHigh, 20.0);

        if (result == null) {
            return SolveResult.failed();
        }

        return new SolveResult(result);
    }
}
