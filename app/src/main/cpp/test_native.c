/*
 * Native test for astrometry star detection - runs without JNI/Android
 * Compile: gcc -o test_native test_native.c ... -lm
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

#include "astrometry/include/astrometry/simplexy.h"
#include "astrometry/include/astrometry/image2xy.h"
#include "astrometry/include/astrometry/log.h"

int main(int argc, char** argv) {
    const char* image_path = "/mnt/d/Download/DIP/img.png";

    if (argc > 1) {
        image_path = argv[1];
    }

    printf("Loading image: %s\n", image_path);

    // Load image using stb_image
    int width, height, channels;
    unsigned char* rgb = stbi_load(image_path, &width, &height, &channels, 0);

    if (!rgb) {
        fprintf(stderr, "Failed to load image: %s\n", image_path);
        return 1;
    }

    printf("Image loaded: %dx%d, %d channels\n", width, height, channels);

    // Convert to grayscale
    unsigned char* grayscale = malloc(width * height);
    for (int i = 0; i < width * height; i++) {
        int idx = i * channels;
        int r = rgb[idx];
        int g = (channels > 1) ? rgb[idx + 1] : r;
        int b = (channels > 2) ? rgb[idx + 2] : r;
        // Standard luminance formula
        grayscale[i] = (unsigned char)(0.299 * r + 0.587 * g + 0.114 * b);
    }
    stbi_image_free(rgb);

    // Calculate grayscale stats
    double sum = 0;
    for (int i = 0; i < width * height; i++) {
        sum += grayscale[i];
    }
    double mean = sum / (width * height);
    printf("Grayscale mean: %.2f\n", mean);

    // Initialize logging
    log_init(LOG_VERB);

    // Set up simplexy parameters (same as solve-field defaults)
    simplexy_t params;
    memset(&params, 0, sizeof(simplexy_t));

    params.image_u8 = grayscale;
    params.nx = width;
    params.ny = height;
    params.dpsf = 1.0;      // PSF sigma
    params.plim = 8.0;      // Detection threshold
    params.dlim = 1.0;
    params.saddle = 5.0;
    params.maxper = 1000;
    params.maxnpeaks = 100000;
    params.maxsize = 2000;
    params.halfbox = 100;

    int downsample = 2;

    printf("\nRunning star detection with:\n");
    printf("  plim=%.1f, dpsf=%.1f, downsample=%d\n", params.plim, params.dpsf, downsample);

    // Run detection
    int result = image2xy_run(&params, downsample, 0);

    if (result != 0) {
        fprintf(stderr, "Star detection failed with code %d\n", result);
        free(grayscale);
        return 1;
    }

    printf("\n=== RESULTS ===\n");
    printf("Stars detected: %d\n", params.npeaks);

    // Print first 10 stars
    printf("\nFirst 10 stars (x, y, flux):\n");
    for (int i = 0; i < params.npeaks && i < 10; i++) {
        printf("  %3d: (%8.2f, %8.2f) flux=%10.2f\n",
               i+1, params.x[i], params.y[i], params.flux[i]);
    }

    // Compare with expected
    printf("\n=== COMPARISON ===\n");
    printf("Expected (solve-field): ~677 stars\n");
    printf("Actual (our code):      %d stars\n", params.npeaks);

    int diff = abs(params.npeaks - 677);
    float pct = (float)diff / 677 * 100;
    printf("Difference: %d stars (%.1f%%)\n", diff, pct);

    if (diff < 100) {
        printf("STATUS: PASS - within 100 stars of reference\n");
    } else {
        printf("STATUS: CHECK - more than 100 stars difference\n");
    }

    // Cleanup
    simplexy_free_contents(&params);
    free(grayscale);

    return 0;
}
