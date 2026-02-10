package com.astro.app.native_;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Orchestrates multi-frame image stacking pipeline.
 * Combines star detection, triangle asterism matching, and averaging to improve SNR.
 *
 * Usage:
 * 1. startSession(firstFrame, callback) - initialize with reference frame
 * 2. addFrame(frame) - align and stack additional frames
 * 3. getResult() - retrieve current stacked image
 * 4. release() - clean up native resources
 */
public class ImageStackingManager {
    private static final String TAG = "ImageStackingManager";

    // Star detection parameters (match plate solving defaults)
    private static final float PLIM = 8.0f;
    private static final float DPSF = 1.0f;
    private static final int DOWNSAMPLE = 2;
    private static final int MIN_STARS = 20;  // Minimum stars needed for alignment

    // Reasonable image size limits
    private static final int MIN_DIMENSION = 100;
    private static final int MAX_DIMENSION = 8192;

    // Session state
    private long nativeHandle = 0;
    private int width = 0;
    private int height = 0;
    private float[] referenceStars = null;  // First frame's stars (x, y, flux triplets)
    private int frameCount = 0;
    private StackingCallback callback = null;

    /**
     * Callback interface for stacking progress and errors
     */
    public interface StackingCallback {
        /**
         * Called when a frame is successfully stacked
         * @param frameNumber 1-based frame number
         * @param totalFrames current total frame count
         * @param inliers number of matched stars
         * @param rmsError alignment error in pixels
         */
        void onFrameStacked(int frameNumber, int totalFrames, int inliers, double rmsError);

        /**
         * Called when frame alignment fails
         * @param frameNumber 1-based frame number
         * @param reason error message
         */
        void onAlignmentFailed(int frameNumber, String reason);

        /**
         * Called when star detection fails
         * @param frameNumber 1-based frame number
         * @param starCount detected star count (might be too low)
         */
        void onStarDetectionFailed(int frameNumber, int starCount);
    }

    /**
     * Start a new stacking session with the first frame
     * @param firstFrame first frame bitmap (grayscale or color)
     * @param callback progress callback (nullable)
     * @return true if session started successfully
     */
    public boolean startSession(Bitmap firstFrame, StackingCallback callback) {
        // Check libraries loaded
        if (!AstrometryNative.isLibraryLoaded()) {
            Log.e(TAG, "startSession failed: AstrometryNative library not loaded");
            return false;
        }
        if (!StackingNative.isLibraryLoaded()) {
            Log.e(TAG, "startSession failed: StackingNative library not loaded");
            return false;
        }

        // Validate bitmap
        if (firstFrame == null) {
            Log.e(TAG, "startSession failed: bitmap is null");
            return false;
        }

        int w = firstFrame.getWidth();
        int h = firstFrame.getHeight();

        if (w < MIN_DIMENSION || h < MIN_DIMENSION || w > MAX_DIMENSION || h > MAX_DIMENSION) {
            Log.e(TAG, "startSession failed: invalid dimensions " + w + "x" + h);
            return false;
        }

        // Convert to grayscale
        byte[] grayData = AstrometryNative.bitmapToGrayscale(firstFrame);
        if (grayData == null) {
            Log.e(TAG, "startSession failed: grayscale conversion failed");
            return false;
        }

        // Detect stars
        float[] stars = AstrometryNative.detectStarsNative(grayData, w, h, PLIM, DPSF, DOWNSAMPLE);
        if (stars == null) {
            Log.e(TAG, "startSession failed: star detection returned null");
            if (callback != null) {
                callback.onStarDetectionFailed(1, 0);
            }
            return false;
        }

        int starCount = stars.length / 3;
        if (starCount < MIN_STARS) {
            Log.e(TAG, "startSession failed: too few stars detected (" + starCount + " < " + MIN_STARS + ")");
            if (callback != null) {
                callback.onStarDetectionFailed(1, starCount);
            }
            return false;
        }

        Log.i(TAG, "Reference frame: " + w + "x" + h + ", " + starCount + " stars detected");

        // Initialize native stacking context (grayscale only)
        long handle = StackingNative.initStacking(w, h, false);
        if (handle == 0) {
            Log.e(TAG, "startSession failed: native initialization failed");
            return false;
        }

        // Add first frame (null refStars for reference frame)
        StackingNative.AlignmentResult result = StackingNative.addFrame(handle, grayData, stars, null);
        if (result == null || !result.success) {
            Log.e(TAG, "startSession failed: could not add reference frame");
            StackingNative.release(handle);
            return false;
        }

        // Success - store session state
        this.nativeHandle = handle;
        this.width = w;
        this.height = h;
        this.referenceStars = stars;
        this.frameCount = 1;
        this.callback = callback;

        Log.i(TAG, "Stacking session started: " + w + "x" + h);

        // Notify callback
        if (callback != null) {
            callback.onFrameStacked(1, 1, starCount, 0.0);
        }

        return true;
    }

    /**
     * Add a frame to the stack
     * @param frame bitmap to add (must match session dimensions)
     * @return true if frame was successfully stacked
     */
    public boolean addFrame(Bitmap frame) {
        // Check session active
        if (!isActive()) {
            Log.e(TAG, "addFrame failed: no active session");
            return false;
        }

        // Validate bitmap
        if (frame == null) {
            Log.e(TAG, "addFrame failed: bitmap is null");
            return false;
        }

        int w = frame.getWidth();
        int h = frame.getHeight();

        if (w != width || h != height) {
            Log.e(TAG, "addFrame failed: dimension mismatch (expected " + width + "x" + height + ", got " + w + "x" + h + ")");
            return false;
        }

        int nextFrameNumber = frameCount + 1;

        // Convert to grayscale
        byte[] grayData = AstrometryNative.bitmapToGrayscale(frame);
        if (grayData == null) {
            Log.e(TAG, "addFrame failed: grayscale conversion failed");
            return false;
        }

        // Detect stars
        float[] stars = AstrometryNative.detectStarsNative(grayData, w, h, PLIM, DPSF, DOWNSAMPLE);
        if (stars == null) {
            Log.e(TAG, "addFrame failed: star detection returned null");
            if (callback != null) {
                callback.onStarDetectionFailed(nextFrameNumber, 0);
            }
            return false;
        }

        int starCount = stars.length / 3;
        if (starCount < MIN_STARS) {
            Log.e(TAG, "addFrame failed: too few stars detected (" + starCount + " < " + MIN_STARS + ")");
            if (callback != null) {
                callback.onStarDetectionFailed(nextFrameNumber, starCount);
            }
            return false;
        }

        // Align and stack
        StackingNative.AlignmentResult result = StackingNative.addFrame(nativeHandle, grayData, stars, referenceStars);
        if (result == null) {
            Log.e(TAG, "addFrame failed: native addFrame returned null");
            if (callback != null) {
                callback.onAlignmentFailed(nextFrameNumber, "Native call failed");
            }
            return false;
        }

        if (!result.success) {
            String errorMsg = "Insufficient inliers (" + result.inliers + ") or high RMS error (" + 
                              String.format("%.2f", result.rmsError) + " px)";
            Log.w(TAG, "addFrame alignment failed (frame " + nextFrameNumber + "): " + errorMsg);
            
            if (callback != null) {
                callback.onAlignmentFailed(nextFrameNumber, errorMsg);
            }
            return false;
        }

        // Success
        frameCount++;
        Log.i(TAG, "Frame " + frameCount + " stacked: " + result.inliers + " inliers, RMS=" + String.format("%.2f", result.rmsError) + "px");

        // Notify callback
        if (callback != null) {
            callback.onFrameStacked(frameCount, frameCount, result.inliers, result.rmsError);
        }

        return true;
    }

        /**
     * Get the current stacked result
     * @return stacked image as bitmap, or null if no frames stacked
     */
    public Bitmap getResult() {
        if (!isActive()) {
            Log.e(TAG, "getResult failed: no active session");
            return null;
        }

        if (frameCount == 0) {
            Log.e(TAG, "getResult failed: no frames stacked");
            return null;
        }

        // Get stacked data from native
        byte[] result = StackingNative.getStackedImage(nativeHandle);
        if (result == null) {
            Log.e(TAG, "getResult failed: native getStackedImage returned null");
            return null;
        }

        if (result.length != width * height) {
            Log.e(TAG, "getResult failed: size mismatch (expected " + (width * height) + ", got " + result.length + ")");
            return null;
        }

        // Convert grayscale byte array to ARGB bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];

        for (int i = 0; i < pixels.length; i++) {
            int gray = result[i] & 0xFF;  // Unsigned byte
            pixels[i] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;  // ARGB
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        Log.i(TAG, "Generated result bitmap: " + width + "x" + height + ", " + frameCount + " frames");
        return bitmap;
    }

    /**
     * Get current frame count
     * @return number of frames successfully stacked
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * Check if session is active
     * @return true if session is initialized
     */
    public boolean isActive() {
        return nativeHandle != 0;
    }

    /**
     * Release native resources and reset session state
     */
    public void release() {
        if (nativeHandle != 0) {
            StackingNative.release(nativeHandle);
            Log.i(TAG, "Released stacking session (" + frameCount + " frames)");
        }

        // Clear state
        nativeHandle = 0;
        width = 0;
        height = 0;
        referenceStars = null;
        frameCount = 0;
        callback = null;
    }
}
