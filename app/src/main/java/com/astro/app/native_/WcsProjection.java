package com.astro.app.native_;

/**
 * WCS TAN (gnomonic) projection for converting RA/Dec to pixel coordinates.
 * Uses the CD matrix and reference point from a plate solve result.
 */
public class WcsProjection {
    private final double crpixX, crpixY;
    private final double crvalRA, crvalDec;
    private final double[] cd;
    // Inverse CD matrix elements
    private final double invCD00, invCD01, invCD10, invCD11;

    public WcsProjection(AstrometryNative.SolveResult result) {
        this.crpixX = result.crpixX;
        this.crpixY = result.crpixY;
        this.crvalRA = Math.toRadians(result.ra);
        this.crvalDec = Math.toRadians(result.dec);
        this.cd = result.cd;

        // Compute inverse CD matrix
        double det = cd[0] * cd[3] - cd[1] * cd[2];
        invCD00 =  cd[3] / det;
        invCD01 = -cd[1] / det;
        invCD10 = -cd[2] / det;
        invCD11 =  cd[0] / det;
    }

    /**
     * Project RA/Dec (in degrees) to pixel coordinates.
     * Returns null if the point is on the opposite side of the sky.
     */
    public double[] radecToPixel(double raDeg, double decDeg) {
        double ra = Math.toRadians(raDeg);
        double dec = Math.toRadians(decDeg);

        double sinDec0 = Math.sin(crvalDec);
        double cosDec0 = Math.cos(crvalDec);
        double sinDec = Math.sin(dec);
        double cosDec = Math.cos(dec);
        double dRA = ra - crvalRA;

        // Gnomonic (TAN) projection
        double D = sinDec0 * sinDec + cosDec0 * cosDec * Math.cos(dRA);
        if (D <= 0) {
            return null; // Behind the tangent plane
        }

        // Intermediate world coordinates in radians
        double xi  = cosDec * Math.sin(dRA) / D;
        double eta = (cosDec0 * sinDec - sinDec0 * cosDec * Math.cos(dRA)) / D;

        // Convert to degrees (CD matrix expects degrees)
        xi  = Math.toDegrees(xi);
        eta = Math.toDegrees(eta);

        // Apply inverse CD matrix to get pixel offsets
        double dx = invCD00 * xi + invCD01 * eta;
        double dy = invCD10 * xi + invCD11 * eta;

        // FITS 1-based to 0-based pixel coordinates
        double pixelX = crpixX + dx - 1.0;
        double pixelY = crpixY + dy - 1.0;

        return new double[]{pixelX, pixelY};
    }

    /**
     * Check if pixel coordinates are within the image bounds (with margin).
     */
    public static boolean isOnImage(double x, double y, int width, int height) {
        double margin = -50; // Allow slightly off-screen for line clipping
        return x >= margin && x < width - margin && y >= margin && y < height - margin;
    }
}
