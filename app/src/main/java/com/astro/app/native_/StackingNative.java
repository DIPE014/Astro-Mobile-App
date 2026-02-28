package com.astro.app.native_;

import android.util.Log;

/**
 * JNI interface to native image stacking library.
 * Provides triangle asterism matching, RANSAC alignment, and mean accumulation.
 */
public class StackingNative {
    private static final String TAG = "StackingNative";
    private static boolean libraryLoaded = false;

    static {
        try {
            System.loadLibrary("astrometry_native");
            libraryLoaded = true;
            Log.i(TAG, "Stacking native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library: " + e.getMessage());
            libraryLoaded = false;
        }
    }

    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }

    /**
     * Initialize stacking session.
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @param isColor True for RGB, false for grayscale (currently only grayscale supported)
     * @return Native handle (jlong), or 0 on failure
     */
    public static native long initStackingNative(int width, int height, boolean isColor);

    /**
     * Add frame to stack.
     * @param handle Native stacking context handle
     * @param imageData Grayscale image byte array (width*height)
     * @param stars Detected stars [x,y,flux, x,y,flux, ...] for new frame
     * @param refStars Reference stars (null for first frame, or previous refStars for subsequent)
     * @return Array [success, inliers, rmsError, frameCount], or null on failure
     */
    public static native double[] addFrameNative(long handle, byte[] imageData,
                                                  float[] stars, float[] refStars);

    /**
     * Get stacked result image.
     * @param handle Native stacking context handle
     * @return Averaged grayscale byte array, or null on failure
     */
    public static native byte[] getStackedImageNative(long handle);

    /**
     * Get current frame count.
     * @param handle Native stacking context handle
     * @return Number of frames successfully stacked
     */
    public static native int getFrameCountNative(long handle);

    /**
     * Release native stacking context and free memory.
     * @param handle Native stacking context handle
     */
    public static native void releaseNative(long handle);

    /**
     * Result of frame alignment operation.
     */
    public static class AlignmentResult {
        public final boolean success;
        public final int inliers;
        public final double rmsError;
        public final int frameCount;

        public AlignmentResult(double[] result) {
            if (result == null || result.length < 4) {
                this.success = false;
                this.inliers = 0;
                this.rmsError = 0.0;
                this.frameCount = 0;
            } else {
                this.success = result[0] > 0.5;
                this.inliers = (int) result[1];
                this.rmsError = result[2];
                this.frameCount = (int) result[3];
            }
        }

        public static AlignmentResult failed() {
            return new AlignmentResult(null);
        }
    }

    /**
     * Initialize stacking session.
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @param isColor True for RGB, false for grayscale (currently only grayscale supported)
     * @return Native handle (jlong), or 0 on failure
     */
    public static long initStacking(int width, int height, boolean isColor) {
        if (!libraryLoaded) {
            Log.e(TAG, "Native library not loaded");
            return 0;
        }

        long handle = initStackingNative(width, height, isColor);
        if (handle == 0) {
            Log.e(TAG, "Failed to initialize stacking context");
        } else {
            Log.i(TAG, "Stacking context initialized: " + width + "x" + height +
                  (isColor ? " (color)" : " (grayscale)"));
        }

        return handle;
    }

    /**
     * Add frame to stack with alignment.
     * @param handle Native stacking context handle
     * @param imageData Grayscale image byte array
     * @param stars Detected stars [x,y,flux, x,y,flux, ...] for new frame
     * @param refStars Reference stars (null for first frame)
     * @return AlignmentResult with success status, inliers, RMS error, and frame count
     */
    public static AlignmentResult addFrame(long handle, byte[] imageData,
                                           float[] stars, float[] refStars) {
        if (!libraryLoaded) {
            Log.e(TAG, "Native library not loaded");
            return AlignmentResult.failed();
        }

        if (handle == 0) {
            Log.e(TAG, "Invalid stacking context handle");
            return AlignmentResult.failed();
        }

        if (imageData == null) {
            Log.e(TAG, "Image data is null");
            return AlignmentResult.failed();
        }

        if (stars == null || stars.length < 3) {
            Log.e(TAG, "Invalid stars array");
            return AlignmentResult.failed();
        }

        double[] result = addFrameNative(handle, imageData, stars, refStars);
        if (result == null) {
            Log.e(TAG, "Native addFrame failed");
            return AlignmentResult.failed();
        }

        AlignmentResult alignResult = new AlignmentResult(result);
        if (alignResult.success) {
            Log.i(TAG, "Frame added: inliers=" + alignResult.inliers +
                  ", rms=" + String.format("%.2f", alignResult.rmsError) +
                  ", total=" + alignResult.frameCount);
        } else {
            Log.w(TAG, "Frame alignment failed");
        }

        return alignResult;
    }

    /**
     * Get stacked result image.
     * @param handle Native stacking context handle
     * @return Averaged grayscale byte array, or null on failure
     */
    public static byte[] getStackedImage(long handle) {
        if (!libraryLoaded) {
            Log.e(TAG, "Native library not loaded");
            return null;
        }

        if (handle == 0) {
            Log.e(TAG, "Invalid stacking context handle");
            return null;
        }

        byte[] result = getStackedImageNative(handle);
        if (result == null) {
            Log.e(TAG, "Failed to get stacked image");
        } else {
            Log.i(TAG, "Retrieved stacked image: " + result.length + " bytes");
        }

        return result;
    }

    /**
     * Get current frame count.
     * @param handle Native stacking context handle
     * @return Number of frames successfully stacked, or 0 on failure
     */
    public static int getFrameCount(long handle) {
        if (!libraryLoaded) {
            Log.e(TAG, "Native library not loaded");
            return 0;
        }

        if (handle == 0) {
            Log.e(TAG, "Invalid stacking context handle");
            return 0;
        }

        return getFrameCountNative(handle);
    }

    /**
     * Release native stacking context and free memory.
     * Should be called when stacking session is complete.
     * @param handle Native stacking context handle
     */
    public static void release(long handle) {
        if (!libraryLoaded) {
            Log.e(TAG, "Native library not loaded");
            return;
        }

        if (handle == 0) {
            Log.w(TAG, "Attempted to release null handle");
            return;
        }

        releaseNative(handle);
        Log.i(TAG, "Stacking context released");
    }
}
