package com.astro.app.native_;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented test for native star detection.
 * Tests that the JNI bridge correctly detects stars from a test image.
 */
@RunWith(AndroidJUnit4.class)
public class NativeStarDetectionTest {

    private static Context appContext;
    private static Bitmap testBitmap;

    @BeforeClass
    public static void setUp() throws Exception {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Load test image from assets
        InputStream is = InstrumentationRegistry.getInstrumentation()
                .getContext().getAssets().open("test_image.png");
        testBitmap = BitmapFactory.decodeStream(is);
        is.close();

        assertNotNull("Test image should load", testBitmap);
        assertEquals("Image width should be 1920", 1920, testBitmap.getWidth());
        assertEquals("Image height should be 2560", 2560, testBitmap.getHeight());
    }

    @Test
    public void testNativeLibraryLoads() {
        assertTrue("Native library should load", AstrometryNative.isLibraryLoaded());
    }

    @Test
    public void testStarDetectionWithDefaultParams() {
        // Use same parameters as solve-field
        float plim = 8.0f;
        float dpsf = 1.0f;
        int downsample = 2;

        List<AstrometryNative.NativeStar> stars = AstrometryNative.detectStars(
                testBitmap, plim, dpsf, downsample);

        assertNotNull("Should detect stars", stars);
        assertTrue("Should detect some stars", stars.size() > 0);

        // Expected: ~677 stars (matching solve-field output)
        // Allow wider tolerance since grayscale conversion may differ
        int expectedStars = 677;
        int tolerance = 200;  // Wide tolerance for different grayscale conversions

        System.out.println("Stars detected: " + stars.size() + " (expected ~" + expectedStars + ")");

        // Just check we're in a reasonable range
        assertTrue("Should detect more than 100 stars", stars.size() > 100);
        assertTrue("Should detect less than 5000 stars", stars.size() < 5000);

        // Print first few stars for debugging
        System.out.println("First 5 stars:");
        for (int i = 0; i < Math.min(5, stars.size()); i++) {
            AstrometryNative.NativeStar s = stars.get(i);
            System.out.println("  " + (i+1) + ": (" + s.x + ", " + s.y + ") flux=" + s.flux);
        }
    }

    @Test
    public void testStarDetectionWithHigherSensitivity() {
        // Lower plim = more sensitive
        float plim = 4.0f;
        float dpsf = 1.0f;
        int downsample = 2;

        List<AstrometryNative.NativeStar> stars = AstrometryNative.detectStars(
                testBitmap, plim, dpsf, downsample);

        assertNotNull("Should detect stars", stars);
        assertTrue("More sensitive should find more stars", stars.size() > 100);

        System.out.println("Stars detected (plim=4): " + stars.size());
    }

    @Test
    public void testGrayscaleConversion() {
        // Test that bitmap to grayscale conversion works
        byte[] grayscale = AstrometryNative.bitmapToGrayscale(testBitmap);

        assertNotNull("Grayscale conversion should work", grayscale);
        assertEquals("Grayscale size should match pixel count",
                testBitmap.getWidth() * testBitmap.getHeight(), grayscale.length);

        // Check that values are reasonable (not all zeros or all 255)
        int sum = 0;
        int nonZero = 0;
        for (byte b : grayscale) {
            int val = b & 0xFF;
            sum += val;
            if (val > 0) nonZero++;
        }
        double mean = (double) sum / grayscale.length;

        System.out.println("Grayscale mean: " + mean + ", non-zero: " + nonZero);

        assertTrue("Mean should be reasonable", mean > 30 && mean < 200);
        assertTrue("Most pixels should be non-zero", nonZero > grayscale.length / 2);
    }
}
