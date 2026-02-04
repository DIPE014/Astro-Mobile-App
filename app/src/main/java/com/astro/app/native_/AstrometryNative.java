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
}
