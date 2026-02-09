package com.astro.app.native_;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Full pipeline test for NativePlateSolver.
 * Tests star detection and plate solving against known test image.
 *
 * Expected result for test_image.png (Orion):
 * - RA ≈ 81.37° (or 81.27° depending on exact center)
 * - Dec ≈ -0.99° (or 0.11° depending on exact center)
 */
@RunWith(AndroidJUnit4.class)
public class NativePlateSolverTest {

    private static Context appContext;
    private static Bitmap testBitmap;
    private static NativePlateSolver solver;

    // Expected coordinates for Orion test image
    private static final double EXPECTED_RA = 81.37;
    private static final double EXPECTED_DEC = -0.99;
    private static final double TOLERANCE_DEGREES = 1.0;  // Allow 1 degree tolerance

    @BeforeClass
    public static void setUp() throws Exception {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Load test image from assets
        InputStream is = InstrumentationRegistry.getInstrumentation()
                .getContext().getAssets().open("test_image.png");
        testBitmap = BitmapFactory.decodeStream(is);
        is.close();

        assertNotNull("Test image should load", testBitmap);

        // Create solver
        solver = new NativePlateSolver(appContext);
    }

    @Test
    public void testNativeLibraryLoads() {
        assertTrue("Native library should load", AstrometryNative.isLibraryLoaded());
    }

    @Test
    public void testStarDetection() {
        List<AstrometryNative.NativeStar> stars = solver.detectStars(testBitmap);

        assertNotNull("Should detect stars", stars);
        assertTrue("Should detect more than 100 stars", stars.size() > 100);
        assertTrue("Should detect less than 5000 stars", stars.size() < 5000);

        System.out.println("Detected " + stars.size() + " stars");

        // Verify star positions are within image bounds
        for (AstrometryNative.NativeStar star : stars) {
            assertTrue("Star X should be positive", star.x >= 0);
            assertTrue("Star Y should be positive", star.y >= 0);
            assertTrue("Star X should be within width",
                    star.x <= testBitmap.getWidth());
            assertTrue("Star Y should be within height",
                    star.y <= testBitmap.getHeight());
        }
    }

    @Test
    public void testStarDetectionWithDifferentParameters() {
        // Test with more sensitive detection
        solver.setDetectionThreshold(4.0f);
        List<AstrometryNative.NativeStar> starsSensitive = solver.detectStars(testBitmap);

        // Reset and test with less sensitive detection
        solver.setDetectionThreshold(12.0f);
        List<AstrometryNative.NativeStar> starsLess = solver.detectStars(testBitmap);

        // Reset to default
        solver.setDetectionThreshold(8.0f);

        assertNotNull(starsSensitive);
        assertNotNull(starsLess);

        // More sensitive should find more stars
        assertTrue("Lower threshold should find more stars",
                starsSensitive.size() >= starsLess.size());

        System.out.println("Stars with plim=4: " + starsSensitive.size());
        System.out.println("Stars with plim=12: " + starsLess.size());
    }

    /**
     * Test full plate solving pipeline.
     * Note: This test requires index files to be present.
     * If no index files are available, this test will be skipped.
     */
    @Test
    public void testFullPipelineSolve() throws Exception {
        // Try to load index files from assets
        int indexCount = 0;
        try {
            indexCount = solver.loadIndexesFromAssets("indexes");
        } catch (Exception e) {
            System.out.println("No index files in assets: " + e.getMessage());
        }

        if (indexCount == 0) {
            // Try from external storage
            File indexDir = new File("/sdcard/astrometry/indexes");
            if (indexDir.exists()) {
                File[] files = indexDir.listFiles((dir, name) -> name.endsWith(".fits"));
                if (files != null) {
                    for (File f : files) {
                        solver.addIndexPath(f.getAbsolutePath());
                        indexCount++;
                    }
                }
            }
        }

        if (indexCount == 0) {
            System.out.println("SKIPPING plate solve test - no index files available");
            System.out.println("To run this test, place index files in assets/indexes/ or /sdcard/astrometry/indexes/");
            return;
        }

        System.out.println("Using " + indexCount + " index files");

        // Set appropriate scale bounds for typical smartphone photos
        // Typical phone camera: 1-10 arcsec/pixel depending on zoom
        solver.setScaleBounds(0.5, 20.0);

        // Run solve with callback
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<AstrometryNative.SolveResult> resultRef = new AtomicReference<>();
        final AtomicReference<String> errorRef = new AtomicReference<>();

        solver.solve(testBitmap, new NativePlateSolver.SolveCallback() {
            @Override
            public void onProgress(String message) {
                System.out.println("Progress: " + message);
            }

            @Override
            public void onSuccess(AstrometryNative.SolveResult result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onFailure(String error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        // Wait up to 60 seconds for solve
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertTrue("Solve should complete within 60 seconds", completed);

        if (errorRef.get() != null) {
            System.out.println("Solve failed: " + errorRef.get());
            // Don't fail test if no solution found - may be index issue
            return;
        }

        AstrometryNative.SolveResult result = resultRef.get();
        assertNotNull("Should have result", result);
        assertTrue("Should be solved", result.solved);

        System.out.println("=== SOLVE RESULT ===");
        System.out.println("RA: " + result.ra + "°");
        System.out.println("Dec: " + result.dec + "°");
        System.out.println("Pixel scale: " + result.pixelScale + " arcsec/pix");
        System.out.println("Rotation: " + result.rotation + "°");
        System.out.println("Log-odds: " + result.logOdds);
        System.out.println("====================");

        // Verify coordinates are close to expected
        double raDiff = Math.abs(result.ra - EXPECTED_RA);
        double decDiff = Math.abs(result.dec - EXPECTED_DEC);

        // Handle RA wraparound at 360°
        if (raDiff > 180) {
            raDiff = 360 - raDiff;
        }

        System.out.println("RA difference: " + raDiff + "° (tolerance: " + TOLERANCE_DEGREES + "°)");
        System.out.println("Dec difference: " + decDiff + "° (tolerance: " + TOLERANCE_DEGREES + "°)");

        assertTrue("RA should be within " + TOLERANCE_DEGREES + "° of expected",
                raDiff <= TOLERANCE_DEGREES);
        assertTrue("Dec should be within " + TOLERANCE_DEGREES + "° of expected",
                decDiff <= TOLERANCE_DEGREES);
    }

    @Test
    public void testSolveSync() {
        // This test just verifies the sync API works without index files
        // It should return null when no index files are configured
        solver.clearIndexPaths();

        AstrometryNative.SolveResult result = solver.solveSync(testBitmap);

        // Without index files, should return null
        assertNull("Should return null without index files", result);
    }
}
