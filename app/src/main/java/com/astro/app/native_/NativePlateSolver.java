package com.astro.app.native_;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level API for plate solving astronomical images.
 * Combines star detection and plate solving using native astrometry.net code.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * NativePlateSolver solver = new NativePlateSolver(context);
 * solver.setScaleBounds(0.5, 2.0);  // arcsec/pixel
 * solver.solve(bitmap, new NativePlateSolver.SolveCallback() {
 *     @Override
 *     public void onProgress(String message) {
 *         // Update UI
 *     }
 *
 *     @Override
 *     public void onSuccess(SolveResult result) {
 *         Log.i("Solve", "RA=" + result.ra + ", Dec=" + result.dec);
 *     }
 *
 *     @Override
 *     public void onFailure(String error) {
 *         // Handle failure
 *     }
 * });
 * }</pre>
 */
public class NativePlateSolver {
    private static final String TAG = "NativePlateSolver";

    private final Context context;

    // Detection parameters
    private float plim = 8.0f;          // Detection threshold
    private float dpsf = 1.0f;          // PSF sigma
    private int downsample = 2;          // Downsample factor

    // Solver parameters
    private double scaleLow = 10.0;      // Lower pixel scale bound (arcsec/pixel) - matches solve-field --scale-low 10
    private double scaleHigh = 180.0;    // Upper pixel scale bound (arcsec/pixel)
    private double logOddsThreshold = 20.0;

    // Index files
    private List<String> indexPaths = new ArrayList<>();

    /**
     * Callback interface for solve operation progress and results.
     */
    public interface SolveCallback {
        /**
         * Called to report progress.
         * @param message Progress message
         */
        void onProgress(String message);

        /**
         * Called when solving succeeds.
         * @param result The plate solve result
         */
        void onSuccess(AstrometryNative.SolveResult result);

        /**
         * Called when solving fails.
         * @param error Error message
         */
        void onFailure(String error);
    }

    /**
     * Creates a new NativePlateSolver.
     * @param context Application context
     */
    public NativePlateSolver(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Sets the detection threshold (plim).
     * Lower values detect fainter stars but may include noise.
     * @param plim Detection threshold (default: 8.0)
     */
    public void setDetectionThreshold(float plim) {
        this.plim = plim;
    }

    /**
     * Sets the PSF sigma for detection.
     * @param dpsf PSF sigma (default: 1.0)
     */
    public void setPsfSigma(float dpsf) {
        this.dpsf = dpsf;
    }

    /**
     * Sets the downsample factor for detection.
     * Higher values speed up detection but may miss small stars.
     * @param downsample Downsample factor (default: 2)
     */
    public void setDownsample(int downsample) {
        this.downsample = downsample;
    }

    /**
     * Sets the pixel scale search bounds.
     * @param scaleLow Lower bound in arcsec/pixel
     * @param scaleHigh Upper bound in arcsec/pixel
     */
    public void setScaleBounds(double scaleLow, double scaleHigh) {
        this.scaleLow = scaleLow;
        this.scaleHigh = scaleHigh;
    }

    /**
     * Sets the log-odds threshold for accepting solutions.
     * Higher values require more confident solutions.
     * @param threshold Log-odds threshold (default: 20.0)
     */
    public void setLogOddsThreshold(double threshold) {
        this.logOddsThreshold = threshold;
    }

    /**
     * Adds an index file path for solving.
     * @param path Path to index file (.fits)
     */
    public void addIndexPath(String path) {
        indexPaths.add(path);
    }

    /**
     * Clears all index file paths.
     */
    public void clearIndexPaths() {
        indexPaths.clear();
    }

    /**
     * Loads index files from assets to internal storage.
     * @param assetDir Asset directory containing index files
     * @return Number of index files loaded
     */
    public int loadIndexesFromAssets(String assetDir) throws IOException {
        File indexDir = new File(context.getFilesDir(), "indexes");
        if (!indexDir.exists()) {
            boolean created = indexDir.mkdirs();
            Log.i(TAG, "Created index directory: " + indexDir.getAbsolutePath() + " success=" + created);
        }

        String[] assets = context.getAssets().list(assetDir);
        if (assets == null || assets.length == 0) {
            Log.e(TAG, "No assets found in directory: " + assetDir);
            return 0;
        }

        Log.i(TAG, "Found " + assets.length + " items in assets/" + assetDir);

        int count = 0;
        for (String asset : assets) {
            Log.d(TAG, "Checking asset: " + asset);
            if (asset.endsWith(".fits")) {
                File outFile = new File(indexDir, asset);

                // Copy if not exists
                if (!outFile.exists()) {
                    Log.i(TAG, "Copying " + asset + " to " + outFile.getAbsolutePath());
                    copyAssetFile(assetDir + "/" + asset, outFile);
                } else {
                    Log.i(TAG, "Index already exists: " + outFile.getAbsolutePath() + " size=" + outFile.length());
                }

                addIndexPath(outFile.getAbsolutePath());
                Log.i(TAG, "Added index path: " + outFile.getAbsolutePath());
                count++;
            }
        }

        Log.i(TAG, "Loaded " + count + " index files. Total paths: " + indexPaths.size());
        return count;
    }

    private void copyAssetFile(String assetPath, File outFile) throws IOException {
        try (InputStream in = context.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    /**
     * Runs plate solving on a bitmap image.
     * This runs synchronously - call from a background thread.
     *
     * @param bitmap Input image
     * @param callback Callback for progress and results
     */
    public void solve(Bitmap bitmap, SolveCallback callback) {
        if (!AstrometryNative.isLibraryLoaded()) {
            callback.onFailure("Native library not loaded");
            return;
        }

        if (indexPaths.isEmpty()) {
            callback.onFailure("No index files configured");
            return;
        }

        // Step 1: Detect stars
        callback.onProgress("Detecting stars...");
        List<AstrometryNative.NativeStar> stars = AstrometryNative.detectStars(
                bitmap, plim, dpsf, downsample);

        if (stars == null || stars.isEmpty()) {
            callback.onFailure("No stars detected in image");
            return;
        }

        callback.onProgress("Detected " + stars.size() + " stars");
        Log.i(TAG, "Detected " + stars.size() + " stars");

        // Step 2: Solve field
        callback.onProgress("Solving field...");
        String[] indexArray = indexPaths.toArray(new String[0]);

        AstrometryNative.SolveResult result = AstrometryNative.solveField(
                stars,
                bitmap.getWidth(),
                bitmap.getHeight(),
                indexArray,
                scaleLow,
                scaleHigh
        );

        if (result.solved) {
            callback.onProgress("Solved!");
            callback.onSuccess(result);
        } else {
            callback.onFailure("Could not find astrometric solution");
        }
    }

    /**
     * Synchronous solve that returns the result directly.
     * Call from a background thread.
     *
     * @param bitmap Input image
     * @return SolveResult or null on failure
     */
    public AstrometryNative.SolveResult solveSync(Bitmap bitmap) {
        if (!AstrometryNative.isLibraryLoaded()) {
            Log.e(TAG, "Native library not loaded");
            return null;
        }

        if (indexPaths.isEmpty()) {
            Log.e(TAG, "No index files configured");
            return null;
        }

        // Detect stars
        List<AstrometryNative.NativeStar> stars = AstrometryNative.detectStars(
                bitmap, plim, dpsf, downsample);

        if (stars == null || stars.isEmpty()) {
            Log.e(TAG, "No stars detected");
            return null;
        }

        Log.i(TAG, "Detected " + stars.size() + " stars");

        // Solve
        String[] indexArray = indexPaths.toArray(new String[0]);
        AstrometryNative.SolveResult result = AstrometryNative.solveField(
                stars,
                bitmap.getWidth(),
                bitmap.getHeight(),
                indexArray,
                scaleLow,
                scaleHigh
        );

        if (result.solved) {
            Log.i(TAG, String.format("Solved: RA=%.4f, Dec=%.4f, scale=%.2f arcsec/pix",
                    result.ra, result.dec, result.pixelScale));
        }

        return result.solved ? result : null;
    }

    /**
     * Detects stars only without solving.
     * @param bitmap Input image
     * @return List of detected stars or null
     */
    public List<AstrometryNative.NativeStar> detectStars(Bitmap bitmap) {
        return AstrometryNative.detectStars(bitmap, plim, dpsf, downsample);
    }
}
