package com.astro.app.ui.skybrightness;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.media.ExifInterface;

import com.astro.app.data.model.SkyBrightnessResult;
import com.astro.app.native_.AstrometryNative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analyses a sky photograph to estimate sky surface brightness and Bortle class.
 *
 * <h3>Three analysis paths (in order of preference):</h3>
 * <ol>
 *   <li><b>Calibrated (with WCS):</b> Uses plate-solve WCS + catalogue stars for
 *       photometric zero-point calibration. Outputs calibrated surface brightness
 *       in mag/arcsec².</li>
 *   <li><b>EXIF fallback:</b> Uses camera EXIF (ISO, shutter, f-number) to
 *       compute Exposure Value and normalise the background level.</li>
 *   <li><b>Raw fallback:</b> Uses raw median pixel value only. Least reliable.</li>
 * </ol>
 *
 * <h3>Algorithm (calibrated path):</h3>
 * <ol>
 *   <li>Convert image to grayscale.</li>
 *   <li>Use WCS to convert detected-star pixel positions to RA/Dec.</li>
 *   <li>Match against {@link BrightStarCatalogue} (stars &lt; mag 6.0).</li>
 *   <li>Aperture photometry on matched stars → instrumental magnitudes.</li>
 *   <li>Least-squares zero-point: ZP = mean(m_cat − m_instr).</li>
 *   <li>Cloud detection: if σ(residuals) &gt; 0.5 mag, warn.</li>
 *   <li>Mask pixels within 10 px of every detected star.</li>
 *   <li>Vignetting correction via edge/centre background ratio.</li>
 *   <li>Background mode = 3 × median − 2 × mean (star-masked pixels).</li>
 *   <li>SB = −2.5 log₁₀(F_sky / Ω_pix) + ZP  [mag/arcsec²].</li>
 *   <li>Map SB → Bortle class.</li>
 * </ol>
 */
public class SkyBrightnessAnalyzer {

    private static final String TAG = "SkyBrightnessAnalyzer";

    /** Aperture radius in pixels for star photometry. */
    private static final int APERTURE_RADIUS = 5;
    /** Inner radius of sky annulus for local background estimation. */
    private static final int SKY_ANNULUS_INNER = 8;
    /** Outer radius of sky annulus. */
    private static final int SKY_ANNULUS_OUTER = 12;
    /** Exclusion radius around detected stars for background measurement. */
    private static final int STAR_MASK_RADIUS = 10;
    /** Maximum catalogue magnitude for zero-point calibration. */
    private static final double MAX_CAT_MAG = 4.0;
    /** Pixel value threshold above which a star is considered saturated (0–1 linear). */
    private static final float SATURATION_THRESHOLD = 0.97f;
    /** Residual scatter threshold for cloud warning (magnitudes). */
    private static final double CLOUD_SCATTER_THRESHOLD = 0.5;
    /** Minimum calibration stars required for a reliable zero-point. */
    private static final int MIN_CALIBRATION_STARS = 3;

    private SkyBrightnessAnalyzer() { }

    // =================================================================
    // Public API
    // =================================================================

    /**
     * Analyse with plate-solve calibration (preferred path).
     *
     * @param bitmap       The sky image.
     * @param exif         EXIF metadata (may be null).
     * @param solveResult  Plate-solve result with WCS.
     * @param detectedStars Stars from {@code detectStarsNative} (pixel coords + flux).
     * @return Analysis result with calibrated surface brightness.
     */
    @NonNull
    public static SkyBrightnessResult analyze(
            @NonNull Bitmap bitmap,
            @Nullable ExifInterface exif,
            @NonNull AstrometryNative.SolveResult solveResult,
            @NonNull List<AstrometryNative.NativeStar> detectedStars) {

        if (!solveResult.solved) {
            return analyze(bitmap, exif);
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Convert to grayscale float array (linearised)
        float[] grayLinear = bitmapToLinearGray(bitmap);

        // --- Step 1: WCS pixel → RA/Dec for detected stars ---
        double[] cd = solveResult.cd;
        double crpixX = solveResult.crpixX;
        double crpixY = solveResult.crpixY;
        double crvalRA = solveResult.ra;
        double crvalDec = solveResult.dec;
        double pixelScale = solveResult.pixelScale;  // arcsec/pixel

        // --- Step 2: Find catalogue stars in field ---
        double fieldRadiusDeg = pixelScale * Math.max(width, height) / 2.0 / 3600.0;
        List<BrightStarCatalogue.CatStar> catStars =
                BrightStarCatalogue.findStarsInField(crvalRA, crvalDec, fieldRadiusDeg * 1.2);

        // --- Step 3: Match detected stars ↔ catalogue ---
        // Adaptive tolerance: 3 pixels at the image's pixel scale
        double matchTolDeg = 3.0 * pixelScale / 3600.0;
        List<double[]> matches = new ArrayList<>();  // {m_cat, m_instr}
        Set<String> usedCatKeys = new HashSet<>();

        for (AstrometryNative.NativeStar det : detectedStars) {
            // Pixel → intermediate world coordinates (TAN projection)
            double dx = det.x - crpixX;
            double dy = det.y - crpixY;
            double xi = cd[0] * dx + cd[1] * dy;   // degrees
            double eta = cd[2] * dx + cd[3] * dy;

            // TAN deprojection
            double[] raDec = tanDeproject(xi, eta, crvalRA, crvalDec);
            double starRA = raDec[0];
            double starDec = raDec[1];

            // Find nearest unmatched catalogue star within adaptive tolerance
            BrightStarCatalogue.CatStar bestMatch = null;
            String bestKey = null;
            double bestSep = matchTolDeg;
            for (BrightStarCatalogue.CatStar cat : catStars) {
                if (cat.vmag > MAX_CAT_MAG) continue;
                String key = cat.ra + "," + cat.dec;
                if (usedCatKeys.contains(key)) continue;  // already claimed
                double sep = angularSeparation(starRA, starDec, cat.ra, cat.dec);
                if (sep < bestSep) {
                    bestSep = sep;
                    bestMatch = cat;
                    bestKey = key;
                }
            }

            if (bestMatch != null && det.flux > 0) {
                // Aperture photometry
                double instrFlux = aperturePhotometry(grayLinear, width, height,
                        det.x, det.y);
                if (instrFlux > 0) {
                    double mInstr = -2.5 * Math.log10(instrFlux);
                    matches.add(new double[]{bestMatch.vmag, mInstr});
                    usedCatKeys.add(bestKey);
                }
            }
        }

        Log.d(TAG, "Catalogue matches: " + matches.size() + " / " + catStars.size()
                + " catalogue stars in field");

        // --- Step 4: Iterative 2σ sigma-clipped zero-point ---
        if (matches.size() < MIN_CALIBRATION_STARS) {
            Log.w(TAG, "Too few calibration stars (" + matches.size()
                    + "), falling back to EXIF path");
            return analyze(bitmap, exif);
        }

        double[] zpVals = new double[matches.size()];
        for (int i = 0; i < matches.size(); i++) {
            zpVals[i] = matches.get(i)[0] - matches.get(i)[1];  // m_cat - m_instr
        }
        boolean[] accepted = new boolean[zpVals.length];
        Arrays.fill(accepted, true);
        double zeroPoint = 0;
        for (int iter = 0; iter < 3; iter++) {
            double sum = 0; int n = 0;
            for (int i = 0; i < zpVals.length; i++) {
                if (accepted[i]) { sum += zpVals[i]; n++; }
            }
            if (n == 0) break;
            zeroPoint = sum / n;
            double sq = 0;
            for (int i = 0; i < zpVals.length; i++) {
                if (accepted[i]) { double d = zpVals[i] - zeroPoint; sq += d * d; }
            }
            double sigma = Math.sqrt(sq / Math.max(1, n - 1));
            boolean changed = false;
            for (int i = 0; i < zpVals.length; i++) {
                if (accepted[i] && Math.abs(zpVals[i] - zeroPoint) > 2.0 * sigma) {
                    accepted[i] = false; changed = true;
                }
            }
            if (!changed) break;
        }
        int finalMatchCount = 0;
        for (boolean a : accepted) if (a) finalMatchCount++;
        if (finalMatchCount < MIN_CALIBRATION_STARS) {
            Log.w(TAG, "Too few stars after sigma-clipping, falling back");
            return analyze(bitmap, exif);
        }

        // --- Step 5: Cloud detection from clipped residual scatter ---
        double residualSumSq = 0;
        for (int i = 0; i < zpVals.length; i++) {
            if (accepted[i]) { double d = zpVals[i] - zeroPoint; residualSumSq += d * d; }
        }
        double residualStd = Math.sqrt(residualSumSq / Math.max(1, finalMatchCount - 1));
        boolean cloudWarning = residualStd > CLOUD_SCATTER_THRESHOLD;
        if (cloudWarning) {
            Log.w(TAG, "Cloud warning: ZP residual σ = " + residualStd);
        }

        // --- Step 6: Build star mask ---
        boolean[] starMask = buildStarMask(width, height, detectedStars);

        // --- Step 7: Vignetting correction ---
        float[] corrected = applyVignettingCorrection(grayLinear, width, height, starMask);

        // --- Step 8: Background measurement (mode = 3*median - 2*mean) ---
        double skyFlux = measureBackground(corrected, width, height, starMask);

        // --- Step 9: Surface brightness in mag/arcsec² ---
        double pixelAreaArcsec2 = pixelScale * pixelScale;
        double surfaceBrightness;
        if (skyFlux > 0 && pixelAreaArcsec2 > 0) {
            surfaceBrightness = -2.5 * Math.log10(skyFlux / pixelAreaArcsec2) + zeroPoint;
        } else {
            surfaceBrightness = 22.0;  // fallback: assume dark sky
        }

        Log.d(TAG, String.format("Calibrated: ZP=%.2f, skyFlux=%.4f, pixScale=%.2f\"/px, "
                        + "SB=%.2f mag/arcsec², σ=%.3f, stars=%d",
                zeroPoint, skyFlux, pixelScale, surfaceBrightness,
                residualStd, finalMatchCount));

        // --- Step 10: Bortle class from mag/arcsec² ---
        int bortleClass = bortleFromSurfaceBrightness(surfaceBrightness);

        // Extract EXIF for display
        int iso = 0;
        double exposureTime = 0;
        double fNumber = 0;
        boolean hasExif = false;
        if (exif != null) {
            iso = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0);
            exposureTime = parseExposureTime(exif);
            fNumber = parseFNumber(exif);
            hasExif = iso > 0 && exposureTime > 0 && fNumber > 0;
        }

        // Median pixel (0-255) for display
        double medianPixel = computeMedianRaw(bitmap);

        return SkyBrightnessResult.createCalibrated(
                bortleClass, surfaceBrightness, medianPixel,
                iso, exposureTime, fNumber, hasExif,
                finalMatchCount, zeroPoint, cloudWarning);
    }

    /**
     * Analyse without WCS — uses EXIF or raw median fallback.
     *
     * @param bitmap The sky image.
     * @param exif   EXIF metadata (may be null).
     * @return Analysis result (uncalibrated).
     */
    @NonNull
    public static SkyBrightnessResult analyze(@NonNull Bitmap bitmap,
                                              @Nullable ExifInterface exif) {

        // --- Centre crop (50%) to reduce vignetting ---
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
        long graySum = 0;

        for (int y = startY; y < endY; y++) {
            bitmap.getPixels(rowPixels, 0, cropW, startX, y, cropW, 1);
            for (int i = 0; i < cropW; i++) {
                int pixel = rowPixels[i];
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                grayValues[idx++] = gray;
                graySum += gray;
            }
        }

        // Mode estimation: 3*median - 2*mean (robust against stars)
        double median = computeMedian(grayValues);
        double mean = (double) graySum / totalPixels;
        double mode = 3.0 * median - 2.0 * mean;
        if (mode < 0) mode = median;  // safety clamp

        // Parse EXIF
        int iso = 0;
        double exposureTime = 0.0;
        double fNumber = 0.0;
        if (exif != null) {
            @SuppressWarnings("deprecation")
            int isoValue = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0);
            iso = isoValue;
            exposureTime = parseExposureTime(exif);
            fNumber = parseFNumber(exif);
        }

        if (iso > 0 && exposureTime > 0.0 && fNumber > 0.0) {
            // --- EXIF path: proper EV normalisation (report §9.6) ---
            // Linearise (undo sRGB gamma)
            double modeNorm = mode / 255.0;
            double linearMode = srgbToLinear(modeNorm);

            // EV = log2(N²/t) + log2(ISO/100)
            double ev = Math.log(fNumber * fNumber / exposureTime) / Math.log(2.0)
                      + Math.log(iso / 100.0) / Math.log(2.0);
            // Normalise to reference exposure (EV_ref = 0: f/1, 1s, ISO 100)
            double bNorm = linearMode * Math.pow(2.0, -ev);

            int bortleClass = estimateBortleFromEV(bNorm);

            Log.d(TAG, String.format("EXIF path: ISO=%d, exp=%.4fs, f/%.1f, EV=%.2f, "
                            + "mode=%.1f, Bnorm=%.6e, bortle=%d",
                    iso, exposureTime, fNumber, ev, mode, bNorm, bortleClass));

            return SkyBrightnessResult.createFromExif(
                    bortleClass, bNorm, median,
                    iso, exposureTime, fNumber);

        } else {
            // --- Raw fallback: no EXIF ---
            double medianNorm = median / 255.0;
            int bortleClass = estimateBortleFromRawMedian(medianNorm);

            Log.d(TAG, String.format("Raw path: median=%.1f (%.4f), bortle=%d",
                    median, medianNorm, bortleClass));

            return SkyBrightnessResult.createFromRawMedian(bortleClass, median);
        }
    }

    // =================================================================
    // Bortle classification
    // =================================================================

    /**
     * Maps calibrated surface brightness (mag/arcsec²) to Bortle class.
     * Thresholds from Bortle (2001) refined by Cinzano et al.
     */
    static int bortleFromSurfaceBrightness(double sb) {
        if (sb >= 21.99) return 1;
        if (sb >= 21.89) return 2;
        if (sb >= 21.69) return 3;
        if (sb >= 21.25) return 4;
        if (sb >= 20.49) return 5;
        if (sb >= 19.50) return 6;
        if (sb >= 18.94) return 7;
        if (sb >= 18.38) return 8;
        return 9;
    }

    /**
     * Maps EV-normalised background brightness to Bortle class.
     * Lower values = darker sky.
     */
    static int estimateBortleFromEV(double bNorm) {
        if (bNorm < 1e-7) return 1;
        if (bNorm < 3e-7) return 2;
        if (bNorm < 1e-6) return 3;
        if (bNorm < 3e-6) return 4;
        if (bNorm < 1e-5) return 5;
        if (bNorm < 3e-5) return 6;
        if (bNorm < 1e-4) return 7;
        if (bNorm < 3e-4) return 8;
        return 9;
    }

    /**
     * Fallback when no EXIF data is available.
     * Uses raw sRGB median (0–1). Least reliable.
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

    // =================================================================
    // Calibrated-path helpers
    // =================================================================

    /**
     * Converts full bitmap to a linearised grayscale float array.
     * Applies proper sRGB inverse transfer function.
     */
    private static float[] bitmapToLinearGray(@NonNull Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float[] out = new float[w * h];
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1);
            int base = y * w;
            for (int x = 0; x < w; x++) {
                int pixel = row[x];
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                double gray = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
                out[base + x] = (float) srgbToLinear(gray);
            }
        }
        return out;
    }

    /**
     * Proper sRGB → linear conversion (IEC 61966-2-1).
     * Uses the piecewise transfer function rather than a simple gamma.
     */
    private static double srgbToLinear(double srgb) {
        if (srgb <= 0.04045) {
            return srgb / 12.92;
        } else {
            return Math.pow((srgb + 0.055) / 1.055, 2.4);
        }
    }

    /**
     * TAN (gnomonic) deprojection: intermediate world coords → RA/Dec.
     */
    private static double[] tanDeproject(double xiDeg, double etaDeg,
                                          double crvalRA, double crvalDec) {
        double xi = Math.toRadians(xiDeg);
        double eta = Math.toRadians(etaDeg);
        double dec0 = Math.toRadians(crvalDec);
        double ra0 = Math.toRadians(crvalRA);

        double denom = Math.cos(dec0) - eta * Math.sin(dec0);
        double ra = ra0 + Math.atan2(xi, denom);
        // Use sqrt(xi²+denom²) in denominator — always positive, avoids circular
        // dependency on ra and handles denom < 0 correctly (near-pole sources).
        double dec = Math.atan2(
                eta * Math.cos(dec0) + Math.sin(dec0),
                Math.sqrt(xi * xi + denom * denom));

        return new double[]{Math.toDegrees(ra), Math.toDegrees(dec)};
    }

    /**
     * Angular separation between two sky positions, in degrees.
     */
    private static double angularSeparation(double ra1, double dec1,
                                             double ra2, double dec2) {
        double dRA = Math.toRadians(ra1 - ra2);
        double d1 = Math.toRadians(dec1);
        double d2 = Math.toRadians(dec2);
        double cosSep = Math.sin(d1) * Math.sin(d2)
                      + Math.cos(d1) * Math.cos(d2) * Math.cos(dRA);
        cosSep = Math.max(-1.0, Math.min(1.0, cosSep));
        return Math.toDegrees(Math.acos(cosSep));
    }

    /**
     * Background-subtracted aperture photometry.
     * Sums linearised flux within {@link #APERTURE_RADIUS} pixels of (cx, cy),
     * subtracts local sky estimated from an annulus.
     */
    private static double aperturePhotometry(float[] gray, int w, int h,
                                              float cx, float cy) {
        int icx = Math.round(cx);
        int icy = Math.round(cy);

        double apertureSum = 0;
        int apertureCount = 0;
        float[] annulusVals = new float[
                (2 * SKY_ANNULUS_OUTER + 1) * (2 * SKY_ANNULUS_OUTER + 1)];
        int annulusCount = 0;

        int outerR = SKY_ANNULUS_OUTER;
        for (int dy = -outerR; dy <= outerR; dy++) {
            for (int dx = -outerR; dx <= outerR; dx++) {
                int px = icx + dx;
                int py = icy + dy;
                if (px < 0 || px >= w || py < 0 || py >= h) continue;

                double r = Math.sqrt(dx * dx + dy * dy);
                float val = gray[py * w + px];

                if (r <= APERTURE_RADIUS) {
                    if (val >= SATURATION_THRESHOLD) return -1;  // saturated — reject star
                    apertureSum += val;
                    apertureCount++;
                } else if (r >= SKY_ANNULUS_INNER && r <= SKY_ANNULUS_OUTER) {
                    annulusVals[annulusCount++] = val;
                }
            }
        }

        if (apertureCount == 0 || annulusCount == 0) return 0;

        // Median sky — robust against a neighbouring star landing in the annulus
        Arrays.sort(annulusVals, 0, annulusCount);
        double skyPerPixel = annulusVals[annulusCount / 2];
        double netFlux = apertureSum - skyPerPixel * apertureCount;
        return Math.max(netFlux, 0);
    }

    /**
     * Builds a boolean mask that is {@code true} for pixels within
     * {@link #STAR_MASK_RADIUS} of any detected star.
     */
    private static boolean[] buildStarMask(int w, int h,
                                            @NonNull List<AstrometryNative.NativeStar> stars) {
        boolean[] mask = new boolean[w * h];
        int r = STAR_MASK_RADIUS;
        for (AstrometryNative.NativeStar s : stars) {
            int cx = Math.round(s.x);
            int cy = Math.round(s.y);
            int yMin = Math.max(0, cy - r);
            int yMax = Math.min(h - 1, cy + r);
            int xMin = Math.max(0, cx - r);
            int xMax = Math.min(w - 1, cx + r);
            int r2 = r * r;
            for (int y = yMin; y <= yMax; y++) {
                for (int x = xMin; x <= xMax; x++) {
                    int dy = y - cy;
                    int dx = x - cx;
                    if (dx * dx + dy * dy <= r2) {
                        mask[y * w + x] = true;
                    }
                }
            }
        }
        return mask;
    }

    /**
     * Applies a simple radial vignetting correction.
     * Estimates the background ratio between edge and centre, then applies
     * a radial gain map to flatten the background.
     */
    private static float[] applyVignettingCorrection(float[] gray, int w, int h,
                                                      boolean[] starMask) {
        double cxImg = w / 2.0;
        double cyImg = h / 2.0;
        double maxR = Math.sqrt(cxImg * cxImg + cyImg * cyImg);

        // Sample background in centre ring (r < 0.2*maxR) and edge ring (0.7 < r < 1.0)
        double centreSum = 0;
        int centreCount = 0;
        double edgeSum = 0;
        int edgeCount = 0;

        // Sample every 4th pixel for speed
        for (int y = 0; y < h; y += 4) {
            for (int x = 0; x < w; x += 4) {
                if (starMask[y * w + x]) continue;
                double dx = x - cxImg;
                double dy = y - cyImg;
                double r = Math.sqrt(dx * dx + dy * dy) / maxR;
                float val = gray[y * w + x];
                if (r < 0.2) {
                    centreSum += val;
                    centreCount++;
                } else if (r > 0.7) {
                    edgeSum += val;
                    edgeCount++;
                }
            }
        }

        if (centreCount == 0 || edgeCount == 0) {
            return gray;  // can't correct
        }

        double centreMean = centreSum / centreCount;
        double edgeMean = edgeSum / edgeCount;

        if (edgeMean <= 0 || centreMean <= 0 || edgeMean >= centreMean) {
            return gray;  // no vignetting or inverted — skip
        }

        // Model: gain(r) = 1 / (1 - k * r²), where k is estimated from edge/centre ratio
        double ratio = edgeMean / centreMean;
        // At r = ~0.85 (mean edge radius), gain should make edgeMean → centreMean
        // So: edgeMean * gain = centreMean → gain = centreMean / edgeMean
        // gain(r) = 1 / (1 - k * r²), at r=0.85: 1/(1-k*0.7225) = 1/ratio
        // → 1 - k*0.7225 = ratio → k = (1-ratio)/0.7225
        double k = (1.0 - ratio) / 0.7225;
        k = Math.min(k, 0.8);  // safety clamp

        float[] corrected = new float[gray.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double dx = x - cxImg;
                double dy = y - cyImg;
                double rNorm = Math.sqrt(dx * dx + dy * dy) / maxR;
                double gain = 1.0 / (1.0 - k * rNorm * rNorm);
                corrected[y * w + x] = (float) (gray[y * w + x] * gain);
            }
        }
        return corrected;
    }

    /**
     * Measures the sky background level from star-masked pixels.
     * Uses the mode estimator: mode ≈ 3 × median − 2 × mean.
     */
    private static double measureBackground(float[] gray, int w, int h,
                                             boolean[] starMask) {
        // Collect unmasked pixels (centre 80% to avoid edges)
        int marginX = w / 10;
        int marginY = h / 10;
        List<Float> bgPixels = new ArrayList<>();
        double sum = 0;

        for (int y = marginY; y < h - marginY; y++) {
            for (int x = marginX; x < w - marginX; x++) {
                if (!starMask[y * w + x]) {
                    float val = gray[y * w + x];
                    bgPixels.add(val);
                    sum += val;
                }
            }
        }

        if (bgPixels.isEmpty()) return 0;

        float[] sorted = new float[bgPixels.size()];
        for (int i = 0; i < sorted.length; i++) sorted[i] = bgPixels.get(i);
        Arrays.sort(sorted);

        int mid = sorted.length / 2;
        double median = (sorted.length % 2 == 0)
                ? (sorted[mid - 1] + sorted[mid]) / 2.0
                : sorted[mid];
        double mean = sum / sorted.length;
        double mode = 3.0 * median - 2.0 * mean;
        // Clamp to median (not 0) so we never return a physically meaningless flux
        return Math.max(mode, median);
    }

    // =================================================================
    // Shared helpers
    // =================================================================

    /**
     * Computes the median of an integer array (sorts in-place).
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
     * Quick median of the full bitmap (grayscale, 0–255) for display purposes.
     */
    private static double computeMedianRaw(@NonNull Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        // Use centre crop for speed
        int sx = w / 4, ex = 3 * w / 4;
        int sy = h / 4, ey = 3 * h / 4;
        int cw = ex - sx;
        int[] vals = new int[cw * (ey - sy)];
        int[] row = new int[cw];
        int idx = 0;
        for (int y = sy; y < ey; y++) {
            bitmap.getPixels(row, 0, cw, sx, y, cw, 1);
            for (int x = 0; x < cw; x++) {
                int p = row[x];
                vals[idx++] = (int) (0.299 * Color.red(p) + 0.587 * Color.green(p)
                                     + 0.114 * Color.blue(p));
            }
        }
        return computeMedian(vals);
    }

    /**
     * Parses exposure time from EXIF. Handles rational ("1/60") and decimal formats.
     */
    static double parseExposureTime(@NonNull ExifInterface exif) {
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
    static double parseFNumber(@NonNull ExifInterface exif) {
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
