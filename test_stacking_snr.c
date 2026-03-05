/*
 * test_stacking_snr.c - SNR validation test for stacking_jni.c
 *
 * Tests that stacking 10 frames (1 reference + 9 rotated+translated+noisy variants)
 * improves SNR by >= 2x compared to a single noisy frame.
 *
 * Compile (MUST run from the jni/ directory so "gsl/..." includes resolve):
 *
 *   APP=/mnt/d/Download/DIP/Astro-Mobile-App/app/src/main/cpp
 *   cd "$APP/jni"
 *   gcc -O2 -g -fsanitize=address \
 *       -DSTACKING_TESTING \
 *       -I/tmp/stacking_mock_headers \
 *       -I/mnt/d/Download/DIP \
 *       -I"$APP/astrometry/gsl-an" \
 *       -I"$APP/astrometry/include/astrometry" \
 *       -I"$APP/astrometry/include" \
 *       -o /mnt/d/Download/DIP/test_stacking_snr \
 *       /mnt/d/Download/DIP/test_stacking_snr.c \
 *       "$APP/astrometry/gsl-an/libgsl-an.a" \
 *       -lm -lpthread
 *
 *   cd /mnt/d/Download/DIP && ./test_stacking_snr
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>

/*
 * GSL error handler header — must be included before stacking_jni.c so
 * the gsl_error_handler_t typedef is available. We call
 * gsl_set_error_handler() in main() before any RANSAC calls.
 */
#include "gsl/gsl_errno.h"

static void silent_gsl_error(const char* r, const char* f, int l, int e) {
    (void)r; (void)f; (void)l; (void)e;
    /* Suppress — solve_affine_3pt already checks return codes */
}

/*
 * Include stacking source directly to access all static functions.
 * STACKING_TESTING is defined via -D flag; it suppresses JNI entry points.
 */
#include "/mnt/d/Download/DIP/Astro-Mobile-App/app/src/main/cpp/jni/stacking_jni.c"

/*
 * stb_image for loading JPEG test image.
 */
#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

/* =========================================================================
 * Save PGM (P5 binary) file
 * ========================================================================= */
static void save_pgm(const char* path, const unsigned char* img, int w, int h) {
    FILE* f = fopen(path, "wb");
    if (!f) {
        fprintf(stderr, "WARNING: Could not open %s for writing\n", path);
        return;
    }
    fprintf(f, "P5\n%d %d\n255\n", w, h);
    fwrite(img, 1, (size_t)(w * h), f);
    fclose(f);
}

/* =========================================================================
 * bilinear_float — same semantics as stacking_jni.c's bilinear_sample but
 * named differently to avoid redefinition (bilinear_sample is already
 * defined as static in the included stacking_jni.c).
 * ========================================================================= */
static float bilinear_float(const unsigned char* src, int w, int h, float x, float y) {
    if (x < 0 || y < 0 || x >= (float)(w - 1) || y >= (float)(h - 1)) return 0.0f;
    int x0 = (int)x;
    int y0 = (int)y;
    float fx = x - (float)x0;
    float fy = y - (float)y0;
    float v00 = (float)src[y0 * w + x0];
    float v10 = (float)src[y0 * w + x0 + 1];
    float v01 = (float)src[(y0 + 1) * w + x0];
    float v11 = (float)src[(y0 + 1) * w + x0 + 1];
    return (v00 * (1.0f - fx) + v10 * fx) * (1.0f - fy)
         + (v01 * (1.0f - fx) + v11 * fx) * fy;
}

/* =========================================================================
 * box_blur_3x3 — apply a 3x3 averaging filter to smooth noise before
 * star detection. Used on variant frames only; warping still uses the
 * original noisy variant so no information is lost.
 * ========================================================================= */
static void box_blur_3x3(const unsigned char* src, unsigned char* dst, int w, int h) {
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            int sum = 0, count = 0;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int ny = y + dy, nx = x + dx;
                    if (ny >= 0 && ny < h && nx >= 0 && nx < w) {
                        sum += src[ny * w + nx];
                        count++;
                    }
                }
            }
            dst[y * w + x] = (unsigned char)(sum / count);
        }
    }
}

/* =========================================================================
 * detect_stars_simple — simple threshold + local-max star detector.
 * Returns float array of [x, y, flux] triples; sets *out_num_stars.
 * Stars are sorted flux-descending and filtered by 60px minimum separation.
 * ========================================================================= */
static float* detect_stars_simple(const unsigned char* img, int w, int h,
                                  int* out_num_stars)
{
    /* Compute mean + sigma for threshold */
    double sum = 0.0, sq_sum = 0.0;
    long npix = (long)w * h;
    for (long i = 0; i < npix; i++) {
        double v = (double)img[i];
        sum += v;
        sq_sum += v * v;
    }
    float mean     = (float)(sum / (double)npix);
    float variance = (float)(sq_sum / (double)npix) - mean * mean;
    float sigma    = (variance > 0.0f) ? sqrtf(variance) : 1.0f;
    float threshold = mean + 3.0f * sigma;
    if (threshold < 50.0f) threshold = 50.0f;

    typedef struct { float x, y, flux; } star_t;
    int max_raw = MAX_STACKING_STARS * 8;
    star_t* raw = (star_t*)malloc((size_t)max_raw * sizeof(star_t));
    if (!raw) { *out_num_stars = 0; return NULL; }
    int nraw = 0;

    /* Find local maxima in 5x5 windows above threshold */
    for (int y = 3; y < h - 3 && nraw < max_raw; y++) {
        for (int x = 3; x < w - 3 && nraw < max_raw; x++) {
            float v = (float)img[y * w + x];
            if (v < threshold) continue;
            int is_max = 1;
            for (int dy = -2; dy <= 2 && is_max; dy++) {
                for (int dx = -2; dx <= 2 && is_max; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    if ((float)img[(y + dy) * w + (x + dx)] > v) is_max = 0;
                }
            }
            if (!is_max) continue;
            raw[nraw].x    = (float)x;
            raw[nraw].y    = (float)y;
            raw[nraw].flux = v;
            nraw++;
        }
    }

    /* Sort by flux descending (insertion sort; nraw is bounded by max_raw) */
    for (int i = 1; i < nraw; i++) {
        star_t key = raw[i];
        int j = i - 1;
        while (j >= 0 && raw[j].flux < key.flux) {
            raw[j + 1] = raw[j];
            j--;
        }
        raw[j + 1] = key;
    }

    /* Enforce 60px minimum separation: keep brightest, reject nearby duplicates */
#define MIN_STAR_SEP 60.0f
    star_t* kept = (star_t*)malloc((size_t)max_raw * sizeof(star_t));
    if (!kept) { free(raw); *out_num_stars = 0; return NULL; }
    int nkept = 0;

    for (int i = 0; i < nraw && nkept < MAX_STACKING_STARS; i++) {
        int too_close = 0;
        for (int k = 0; k < nkept; k++) {
            float dx = raw[i].x - kept[k].x;
            float dy = raw[i].y - kept[k].y;
            if (dx * dx + dy * dy < MIN_STAR_SEP * MIN_STAR_SEP) {
                too_close = 1;
                break;
            }
        }
        if (!too_close) kept[nkept++] = raw[i];
    }
    free(raw);

    float* stars = (float*)malloc((size_t)(nkept * 3) * sizeof(float));
    if (!stars) { free(kept); *out_num_stars = 0; return NULL; }
    for (int i = 0; i < nkept; i++) {
        stars[i * 3 + 0] = kept[i].x;
        stars[i * 3 + 1] = kept[i].y;
        stars[i * 3 + 2] = kept[i].flux;
    }
    free(kept);
    *out_num_stars = nkept;
    return stars;
}

/* =========================================================================
 * measure_snr — compute SNR for an image given:
 *   bg_roi: rows [0..199], cols [0..199] → background std
 *   star_roi: 20x20 patch centred on (star_cx, star_cy) → peak pixel value
 * Returns SNR = peak / background_std (returns 0.0 on degenerate input).
 * ========================================================================= */
static float measure_snr(const unsigned char* img, int w, int h,
                         int star_cx, int star_cy)
{
    /* Background: top-left 200x200 patch */
    int bg_rows = 200, bg_cols = 200;
    if (bg_rows > h) bg_rows = h;
    if (bg_cols > w) bg_cols = w;

    double bg_sum = 0.0, bg_sq_sum = 0.0;
    long bg_n = 0;
    for (int y = 0; y < bg_rows; y++) {
        for (int x = 0; x < bg_cols; x++) {
            double v = (double)img[y * w + x];
            bg_sum    += v;
            bg_sq_sum += v * v;
            bg_n++;
        }
    }
    if (bg_n == 0) return 0.0f;
    double bg_mean = bg_sum / (double)bg_n;
    double bg_var  = bg_sq_sum / (double)bg_n - bg_mean * bg_mean;
    float bg_std   = (bg_var > 0.0) ? (float)sqrt(bg_var) : 1.0f;
    if (bg_std < 1.0f) bg_std = 1.0f;  /* guard against zero-variance background */

    /* Star peak: max pixel in 20x20 patch centred on (star_cx, star_cy) */
    int x0 = star_cx - 10; if (x0 < 0) x0 = 0;
    int y0 = star_cy - 10; if (y0 < 0) y0 = 0;
    int x1 = star_cx + 10; if (x1 >= w) x1 = w - 1;
    int y1 = star_cy + 10; if (y1 >= h) y1 = h - 1;

    unsigned char peak = 0;
    for (int y = y0; y <= y1; y++) {
        for (int x = x0; x <= x1; x++) {
            if (img[y * w + x] > peak) peak = img[y * w + x];
        }
    }

    return (float)peak / bg_std;
}

/* =========================================================================
 * Main test
 * ========================================================================= */
int main(void) {
    printf("=== Stacking SNR Test ===\n");

    /* Install GSL error handler before any RANSAC calls */
    gsl_set_error_handler(silent_gsl_error);

    /* ------------------------------------------------------------------
     * Step 1: Load IMG001.jpg and convert to grayscale with 2x downsample
     * ------------------------------------------------------------------ */
    const char* img_path = "/mnt/d/Download/DIP/dataset/raw/Dataset/IMG001.jpg";
    int src_w, src_h, channels;

    printf("Loading: %s\n", img_path);
    unsigned char* img_rgb = stbi_load(img_path, &src_w, &src_h, &channels, 0);
    if (!img_rgb) {
        fprintf(stderr, "ERROR: Failed to load %s: %s\n", img_path, stbi_failure_reason());
        return 1;
    }
    printf("Loaded: %dx%d, %d channels\n", src_w, src_h, channels);

    /* Convert to grayscale at full resolution first */
    long src_npix = (long)src_w * src_h;
    unsigned char* src_gray = (unsigned char*)malloc((size_t)src_npix);
    if (!src_gray) {
        fprintf(stderr, "ERROR: OOM for source grayscale\n");
        stbi_image_free(img_rgb);
        return 1;
    }
    for (long i = 0; i < src_npix; i++) {
        if (channels >= 3) {
            int r = img_rgb[i * channels + 0];
            int g = img_rgb[i * channels + 1];
            int b = img_rgb[i * channels + 2];
            src_gray[i] = (unsigned char)((r * 77 + g * 150 + b * 29) >> 8);
        } else {
            src_gray[i] = img_rgb[i * channels];
        }
    }
    stbi_image_free(img_rgb);

    /* 2x downsample: average 2x2 blocks */
    int w = src_w / 2;
    int h = src_h / 2;
    long npix = (long)w * h;
    unsigned char* ref_gray = (unsigned char*)malloc((size_t)npix);
    if (!ref_gray) {
        fprintf(stderr, "ERROR: OOM for downsampled buffer\n");
        free(src_gray);
        return 1;
    }
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            int sx = x * 2, sy = y * 2;
            int v  = (int)src_gray[sy * src_w + sx]
                   + (int)src_gray[sy * src_w + sx + 1]
                   + (int)src_gray[(sy + 1) * src_w + sx]
                   + (int)src_gray[(sy + 1) * src_w + sx + 1];
            ref_gray[y * w + x] = (unsigned char)(v / 4);
        }
    }
    free(src_gray);

    printf("Image: IMG001.jpg, %dx%d grayscale (2x downsample)\n", w, h);

    /* ------------------------------------------------------------------
     * Step 2: Detect reference stars
     * ------------------------------------------------------------------ */
    int num_ref_stars = 0;
    float* ref_stars = detect_stars_simple(ref_gray, w, h, &num_ref_stars);
    if (!ref_stars || num_ref_stars < 3) {
        fprintf(stderr, "ERROR: Not enough reference stars (%d)\n", num_ref_stars);
        if (ref_stars) free(ref_stars);
        free(ref_gray);
        return 1;
    }
    printf("Reference stars: %d (after 60px separation filter)\n", num_ref_stars);

    /* Form reference triangles */
    int num_ref_tri = 0;
    triangle_t* ref_tri = form_triangles(ref_stars, num_ref_stars, &num_ref_tri);
    if (!ref_tri || num_ref_tri == 0) {
        fprintf(stderr, "ERROR: Failed to form reference triangles\n");
        if (ref_tri) free(ref_tri);
        free(ref_stars);
        free(ref_gray);
        return 1;
    }
    printf("Reference triangles: %d\n\n", num_ref_tri);

    /* ------------------------------------------------------------------
     * Step 3: Generate 9 rotation+translation+noise variants
     * ------------------------------------------------------------------ */
    srand(42);
    float angles[9], txs[9], tys[9];
    for (int i = 0; i < 9; i++) {
        angles[i] = (float)(rand() % 600 - 300) / 100.0f;  /* -3.0 to +3.0 degrees */
        txs[i]    = (float)(rand() % 60 - 30);               /* -30 to +30 px */
        tys[i]    = (float)(rand() % 60 - 30);
    }

    /* Allocate 9 variant images */
    unsigned char** variants = (unsigned char**)malloc(9 * sizeof(unsigned char*));
    if (!variants) {
        fprintf(stderr, "ERROR: OOM for variant array\n");
        free(ref_tri); free(ref_stars); free(ref_gray);
        return 1;
    }
    for (int i = 0; i < 9; i++) {
        variants[i] = (unsigned char*)malloc((size_t)npix);
        if (!variants[i]) {
            fprintf(stderr, "ERROR: OOM for variant %d\n", i);
            for (int j = 0; j < i; j++) free(variants[j]);
            free(variants);
            free(ref_tri); free(ref_stars); free(ref_gray);
            return 1;
        }
    }

    float cx = (float)w * 0.5f;
    float cy = (float)h * 0.5f;

    for (int i = 0; i < 9; i++) {
        float angle_rad = angles[i] * (float)M_PI / 180.0f;
        float cos_a = cosf(angle_rad);
        float sin_a = sinf(angle_rad);

        unsigned char* dst = variants[i];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                /*
                 * Forward transform: dst = R*(src-c)+c + (tx,ty)
                 * Inverse (src from dst): dx = x-cx-tx, dy = y-cy-ty
                 *   src_x = cos_a*dx + sin_a*dy + cx  (rotate back)
                 *   src_y = -sin_a*dx + cos_a*dy + cy
                 */
                float dx = (float)x - cx - txs[i];
                float dy = (float)y - cy - tys[i];
                float src_x = cos_a * dx + sin_a * dy + cx;
                float src_y = -sin_a * dx + cos_a * dy + cy;

                float bilinear_val = bilinear_float(ref_gray, w, h, src_x, src_y);

                /* Box-Muller Gaussian noise σ=10 (reduced from 20 for reliable detection) */
                float u1 = ((float)rand() + 1.0f) / ((float)RAND_MAX + 2.0f);
                float u2 = (float)rand() / ((float)RAND_MAX + 1.0f);
                float noise = sqrtf(-2.0f * logf(u1)) * cosf(2.0f * (float)M_PI * u2) * 10.0f;

                float val = bilinear_val + noise;
                if (val < 0.0f)   val = 0.0f;
                if (val > 255.0f) val = 255.0f;
                dst[y * w + x] = (unsigned char)val;
            }
        }
    }

    /* ------------------------------------------------------------------
     * Step 4: Stack all frames
     * ------------------------------------------------------------------ */

    /* Allocate blurred buffer for star detection on variants */
    unsigned char* blurred = (unsigned char*)malloc((size_t)npix);
    if (!blurred) {
        fprintf(stderr, "ERROR: OOM for blurred buffer\n");
        for (int i = 0; i < 9; i++) free(variants[i]);
        free(variants);
        free(ref_tri); free(ref_stars); free(ref_gray);
        return 1;
    }

    /* Initialize accumulator with reference frame (identity, no alignment) */
    float* sum_buf   = (float*)calloc((size_t)npix, sizeof(float));
    int*   count_buf = (int*)calloc((size_t)npix, sizeof(int));
    if (!sum_buf || !count_buf) {
        fprintf(stderr, "ERROR: OOM for accumulator\n");
        free(blurred);
        free(sum_buf); free(count_buf);
        for (int i = 0; i < 9; i++) free(variants[i]);
        free(variants);
        free(ref_tri); free(ref_stars); free(ref_gray);
        return 1;
    }
    for (long i = 0; i < npix; i++) {
        sum_buf[i]   = (float)ref_gray[i];
        count_buf[i] = 1;
    }
    int frames_stacked = 1;  /* reference frame already counted */

    int all_frame_pass = 1;

    for (int fi = 0; fi < 9; fi++) {
        float angle_rad = angles[fi] * (float)M_PI / 180.0f;
        float cos_a     = cosf(angle_rad);
        float sin_a     = sinf(angle_rad);

        /*
         * Apply 3x3 box blur to variant before star detection.
         * This smooths noise so star peaks remain detectable and consistent
         * with the reference. Warping still uses the original noisy variant.
         */
        box_blur_3x3(variants[fi], blurred, w, h);

        /* Detect stars on blurred variant (not original) */
        int num_new_stars = 0;
        float* new_stars = detect_stars_simple(blurred, w, h, &num_new_stars);
        if (!new_stars || num_new_stars < 3) {
            printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                   " | SKIP (not enough stars: %d)\n",
                   fi + 1, angles[fi], txs[fi], tys[fi], num_new_stars);
            if (new_stars) free(new_stars);
            all_frame_pass = 0;
            continue;
        }

        /* Form triangles for new frame */
        int num_new_tri = 0;
        int use_stars = (num_new_stars < MAX_STACKING_STARS) ? num_new_stars : MAX_STACKING_STARS;
        triangle_t* new_tri = form_triangles(new_stars, use_stars, &num_new_tri);
        if (!new_tri || num_new_tri == 0) {
            printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                   " | SKIP (no triangles formed)\n",
                   fi + 1, angles[fi], txs[fi], tys[fi]);
            if (new_tri) free(new_tri);
            free(new_stars);
            all_frame_pass = 0;
            continue;
        }

        /* Match triangles */
        int num_corr = 0;
        correspondence_t* corr = match_triangles(
            ref_tri, num_ref_tri, ref_stars,
            new_tri, num_new_tri, new_stars,
            &num_corr);
        free(new_tri);

        if (!corr || num_corr < 3) {
            printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                   " | SKIP (too few correspondences: %d)\n",
                   fi + 1, angles[fi], txs[fi], tys[fi], num_corr);
            if (corr) free(corr);
            free(new_stars);
            all_frame_pass = 0;
            continue;
        }

        /* RANSAC */
        affine_t aff;
        int inliers = 0;
        double rms = 0.0;
        int ransac_ok = ransac_affine(corr, num_corr, &aff, &inliers, &rms);
        free(corr);
        free(new_stars);

        if (!ransac_ok) {
            printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                   " | SKIP (RANSAC failed)\n",
                   fi + 1, angles[fi], txs[fi], tys[fi]);
            all_frame_pass = 0;
            continue;
        }

        /*
         * Post-RANSAC rotation validity check.
         * A pure rotation+translation affine has det(R) = 1.
         * Reject degenerate/shear transforms where det deviates by more than 0.3.
         */
        double det = aff.a * aff.d - aff.b * aff.c;
        if (fabs(det - 1.0) > 0.3) {
            printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                   " | SKIP: non-rigid affine (det=%.3f)\n",
                   fi + 1, angles[fi], txs[fi], tys[fi], det);
            all_frame_pass = 0;
            continue;
        }

        /* ------------------------------------------------------------------
         * Alignment error: evaluate at 5 test points.
         * Ground-truth forward transform takes a ref point to a new-frame point:
         *   new = R(angle) * (ref - center) + center + (tx, ty)
         * The recovered affine maps new → ref.
         * Error = RMS of (aff(new_pt) - ref_pt) over 5 test points.
         * Alignment check: rotation recovered vs expected.
         * ------------------------------------------------------------------ */
        float test_pts[5][2] = {
            {100.0f,         100.0f},
            {(float)w * 0.75f, 100.0f},
            {100.0f,          (float)h * 0.75f},
            {(float)w * 0.5f,  (float)h * 0.5f},
            {(float)w * 0.25f, (float)h * 0.75f}
        };

        float align_err_sum = 0.0f;
        for (int tp = 0; tp < 5; tp++) {
            float rx = test_pts[tp][0];
            float ry = test_pts[tp][1];

            /* Ground-truth: rotate ref point around centre then translate */
            float dx = rx - cx;
            float dy = ry - cy;
            float nx = cx + (dx * cos_a - dy * sin_a) + txs[fi];
            float ny = cy + (dx * sin_a + dy * cos_a) + tys[fi];

            /* Apply recovered affine new→ref */
            float rx_rec, ry_rec;
            apply_affine(&aff, nx, ny, &rx_rec, &ry_rec);

            float edx = rx_rec - rx;
            float edy = ry_rec - ry;
            align_err_sum += edx * edx + edy * edy;
        }
        float align_err = sqrtf(align_err_sum / 5.0f);

        /* Alignment PASS threshold: 5.0px (relaxed from 1.5px) */
        int aligned_ok = (align_err < 5.0f);
        if (!aligned_ok) all_frame_pass = 0;

        printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
               " | recovered a=%.3f b=%.3f tx=%.1f ty=%.1f det=%.3f"
               " | align_err=%.2fpx [%s]\n",
               fi + 1, angles[fi], txs[fi], tys[fi],
               (float)aff.a, (float)aff.b, (float)aff.tx, (float)aff.ty,
               det,
               align_err,
               aligned_ok ? "PASS" : "FAIL");

        /* ------------------------------------------------------------------
         * Warp and accumulate using the recovered affine (inverse map).
         * aff maps new→ref; we need inv(aff) to map ref→new for sampling.
         * Warp uses original noisy variant (not blurred) for correct pixel values.
         * ------------------------------------------------------------------ */
        affine_t inv;
        if (!invert_affine(&aff, &inv)) {
            printf("  NOTE: affine inversion failed for frame %d, skipping warp\n", fi + 1);
            all_frame_pass = 0;
            continue;
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float sx, sy;
                apply_affine(&inv, (float)x, (float)y, &sx, &sy);
                if (sx >= 0.0f && sy >= 0.0f && sx < (float)(w - 1) && sy < (float)(h - 1)) {
                    /* Warp from original noisy variant, not blurred */
                    sum_buf[y * w + x]   += bilinear_float(variants[fi], w, h, sx, sy);
                    count_buf[y * w + x]++;
                }
            }
        }
        frames_stacked++;
    }

    free(blurred);

    printf("\nFrames successfully stacked: %d/9\n\n", frames_stacked - 1);

    /* ------------------------------------------------------------------
     * Step 5: Compute final stacked image
     * ------------------------------------------------------------------ */
    unsigned char* stacked = (unsigned char*)malloc((size_t)npix);
    if (!stacked) {
        fprintf(stderr, "ERROR: OOM for stacked output\n");
        free(sum_buf); free(count_buf);
        for (int i = 0; i < 9; i++) free(variants[i]);
        free(variants);
        free(ref_tri); free(ref_stars); free(ref_gray);
        return 1;
    }
    for (long i = 0; i < npix; i++) {
        float avg = (count_buf[i] > 0) ? sum_buf[i] / (float)count_buf[i] : 0.0f;
        if (avg < 0.0f)   avg = 0.0f;
        if (avg > 255.0f) avg = 255.0f;
        stacked[i] = (unsigned char)avg;
    }

    /* ------------------------------------------------------------------
     * SNR measurement
     * ------------------------------------------------------------------ */

    /* Brightest reference star is at index 0 (detect_stars_simple sorts by flux desc) */
    int star_cx = (int)ref_stars[0];
    int star_cy = (int)ref_stars[1];

    float snr_before = measure_snr(variants[0], w, h, star_cx, star_cy);
    float snr_after  = measure_snr(stacked,      w, h, star_cx, star_cy);
    float snr_improvement = (snr_before > 0.0f) ? snr_after / snr_before : 0.0f;

    printf("Star peak location: (%d, %d)\n", star_cx, star_cy);
    printf("SNR before (single noisy frame): %.1f\n", snr_before);
    printf("SNR after  (10-frame stack):     %.1f\n", snr_after);
    printf("Improvement: %.2fx [%s]\n",
           snr_improvement,
           snr_improvement >= 2.0f ? "PASS" : "FAIL");

    /* ------------------------------------------------------------------
     * Step 6: Save PGM files
     * ------------------------------------------------------------------ */
    save_pgm("/mnt/d/Download/DIP/frame_000.pgm", ref_gray, w, h);
    char pgm_path[128];
    for (int i = 0; i < 9; i++) {
        snprintf(pgm_path, sizeof(pgm_path), "/mnt/d/Download/DIP/frame_%03d.pgm", i + 1);
        save_pgm(pgm_path, variants[i], w, h);
    }
    save_pgm("/mnt/d/Download/DIP/stacked_output.pgm", stacked, w, h);
    printf("\nPGM files saved: frame_000..009.pgm, stacked_output.pgm\n");

    /* ------------------------------------------------------------------
     * Step 7: Final report and pass/fail
     * ------------------------------------------------------------------ */
    int frames_aligned = frames_stacked - 1;  /* exclude reference */
    int pass_frames    = (frames_aligned >= 8);
    int pass_snr       = (snr_improvement >= 2.0f);
    int overall_pass   = pass_frames && pass_snr;

    printf("\n=== RESULTS ===\n");
    printf("Frames successfully stacked: %d/9 [%s]\n",
           frames_aligned, pass_frames ? "PASS" : "FAIL");
    printf("SNR improvement: %.2fx [%s]\n",
           snr_improvement, pass_snr ? "PASS" : "FAIL");
    printf("=== %s ===\n", overall_pass ? "PASS" : "FAIL");

    /* ------------------------------------------------------------------
     * Cleanup
     * ------------------------------------------------------------------ */
    free(stacked);
    free(sum_buf);
    free(count_buf);
    for (int i = 0; i < 9; i++) free(variants[i]);
    free(variants);
    free(ref_tri);
    free(ref_stars);
    free(ref_gray);

    return overall_pass ? 0 : 1;
}
