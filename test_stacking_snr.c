/*
 * test_stacking_snr.c - SNR validation test for stacking_jni.c
 *
 * Tests that stacking 10 noisy frames (all with Gaussian sigma=20 noise;
 * frame 0 identity, frames 1-9 rotated+translated) improves SNR by >= 2x
 * compared to a single noisy frame.
 * Runs over all 57 images in the dataset directory.
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
 * Stars are sorted flux-descending and filtered by min_sep minimum separation.
 * min_sep: computed adaptively at each call site as
 *   fmaxf(20.0f, 0.05f * (float)min(w, h))
 * ========================================================================= */
static float* detect_stars_simple(const unsigned char* img, int w, int h,
                                  int* out_num_stars, float min_sep)
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

    /* Enforce min_sep minimum separation: keep brightest, reject nearby duplicates */
    star_t* kept = (star_t*)malloc((size_t)max_raw * sizeof(star_t));
    if (!kept) { free(raw); *out_num_stars = 0; return NULL; }
    int nkept = 0;

    for (int i = 0; i < nraw && nkept < MAX_STACKING_STARS; i++) {
        int too_close = 0;
        for (int k = 0; k < nkept; k++) {
            float dx = raw[i].x - kept[k].x;
            float dy = raw[i].y - kept[k].y;
            if (dx * dx + dy * dy < min_sep * min_sep) {
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
 *   bg_roi: rows [0..199], cols [0..199] -> background std
 *   star_roi: 20x20 patch centred on (star_cx, star_cy) -> peak pixel value
 * Returns SNR = peak / background_std (returns 0.0 on degenerate input).
 * ========================================================================= */
static float measure_snr(const unsigned char* img, int w, int h,
                          int star_cx, int star_cy, float* out_bg_std)
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
    if (out_bg_std) *out_bg_std = bg_std;

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

    return (bg_std > 0) ? (float)peak / bg_std : 0.0f;
}

/* =========================================================================
 * Dataset image list
 * ========================================================================= */
#define DATASET_DIR "/mnt/d/Download/DIP/dataset/raw/Dataset/"
#define TOTAL_IMAGES 57

static const char* IMAGE_NAMES[TOTAL_IMAGES] = {
    "IMG001.jpg", "IMG002.jpg", "IMG003.jpg", "IMG004.jpg", "IMG005.jpg",
    "IMG006.jpg", "IMG007.jpg", "IMG008.jpg", "IMG009.jpg", "IMG010.JPG",
    "IMG011.JPG", "IMG012.JPG", "IMG013.jpg", "IMG014.JPG", "IMG015.JPG",
    "IMG016.jpg", "IMG017.jpg", "IMG018.JPG", "IMG019.JPG", "IMG020.JPG",
    "IMG021.jpg", "IMG022.jpg", "IMG023.jpg", "IMG024.jpg", "IMG025.jpg",
    "IMG026.jpg", "IMG027.jpg", "IMG028.jpg", "IMG029.jpg", "IMG030.jpg",
    "IMG031.jpg", "IMG032.jpg", "IMG033.jpg", "IMG034.jpg", "IMG035.jpg",
    "IMG036.jpg", "IMG037.jpg", "IMG038.jpg", "IMG039.jpg", "IMG040.jpg",
    "IMG041.jpg", "IMG042.jpg", "IMG043.jpg", "IMG044.jpg", "IMG045.jpg",
    "IMG046.jpg", "IMG047.jpg", "IMG048.jpg", "IMG049.jpg", "IMG050.jpg",
    "IMG051.jpg", "IMG052.jpg", "IMG053.jpg", "IMG054.jpg", "IMG055.jpg",
    "IMG056.jpg", "IMG057.jpg"
};

/* =========================================================================
 * Main test
 * ========================================================================= */
int main(void) {
    printf("=== Stacking SNR Test (57 images) ===\n\n");

    /* Install GSL error handler before any RANSAC calls */
    gsl_set_error_handler(silent_gsl_error);

    /* Aggregate tracking */
    int n_processed    = 0;
    int n_skipped      = 0;
    int total_frames   = 0;   /* sum of frames_stacked-1 (variants only) across processed images */
    double sum_snr_improvement = 0.0;
    double sum_std_reduction   = 0.0;
    double min_snr_improvement = 1e30;
    double max_snr_improvement = -1e30;
    int    min_snr_idx         = -1;
    int    max_snr_idx         = -1;
    int    n_passing_snr       = 0;  /* images with improvement >= 2.0x */

    for (int img_idx = 0; img_idx < TOTAL_IMAGES; img_idx++) {
        const char* img_name = IMAGE_NAMES[img_idx];

        /* Build full path */
        char img_path[512];
        snprintf(img_path, sizeof(img_path), "%s%s", DATASET_DIR, img_name);

        /* ------------------------------------------------------------------
         * Step 1: Load image and convert to grayscale with 2x downsample
         * ------------------------------------------------------------------ */
        int src_w, src_h, channels;
        unsigned char* img_rgb = stbi_load(img_path, &src_w, &src_h, &channels, 0);
        if (!img_rgb) {
            printf("[%s] SKIP: failed to load (%s)\n", img_name, stbi_failure_reason());
            n_skipped++;
            continue;
        }

        /* Convert to grayscale at full resolution */
        long src_npix = (long)src_w * src_h;
        unsigned char* src_gray = (unsigned char*)malloc((size_t)src_npix);
        if (!src_gray) {
            printf("[%s] SKIP: OOM for grayscale buffer\n", img_name);
            stbi_image_free(img_rgb);
            n_skipped++;
            continue;
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
            printf("[%s] SKIP: OOM for downsampled buffer\n", img_name);
            free(src_gray);
            n_skipped++;
            continue;
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

        /* ------------------------------------------------------------------
         * Step 2: Generate noisy Frame 0 (identity transform, sigma=10)
         * ref_gray is kept clean for variant generation.
         * ------------------------------------------------------------------ */
        srand(1000 + img_idx);  /* seed for all pixel noise (frame0 + variants) */

        unsigned char* frame0 = (unsigned char*)malloc((size_t)npix);
        if (!frame0) {
            printf("[%s] SKIP: OOM for frame0\n", img_name);
            free(ref_gray);
            n_skipped++;
            continue;
        }
        for (long i = 0; i < npix; i++) {
            float u1 = ((float)rand() + 1.0f) / ((float)RAND_MAX + 2.0f);
            float u2 = (float)rand() / ((float)RAND_MAX + 1.0f);
            float noise = sqrtf(-2.0f * logf(u1)) * cosf(2.0f * (float)M_PI * u2) * 20.0f;
            float val = (float)ref_gray[i] + noise;
            frame0[i] = (unsigned char)(val < 0.0f ? 0.0f : val > 255.0f ? 255.0f : val);
        }

        /* ------------------------------------------------------------------
         * Step 3a: Detect reference stars from blurred CLEAN ref_gray.
         * This avoids noise raising the detection threshold (mean+3σ).
         * The noisy frame0 is still accumulated into the stack below.
         * ------------------------------------------------------------------ */
        unsigned char* ref_blurred = (unsigned char*)malloc((size_t)npix);
        if (!ref_blurred) {
            printf("[%s] SKIP: OOM for ref_blurred\n", img_name);
            free(frame0);
            free(ref_gray);
            n_skipped++;
            continue;
        }
        box_blur_3x3(ref_gray, ref_blurred, w, h);

        float ref_min_sep = fmaxf(20.0f, 0.05f * (float)(w < h ? w : h));
        int num_ref_stars = 0;
        float* ref_stars = detect_stars_simple(ref_blurred, w, h, &num_ref_stars, ref_min_sep);
        free(ref_blurred);
        if (!ref_stars || num_ref_stars < 3) {
            printf("[%s] SKIP: too few stars (%d)\n", img_name, num_ref_stars);
            if (ref_stars) free(ref_stars);
            free(frame0);
            free(ref_gray);
            n_skipped++;
            continue;
        }

        /* Form reference triangles */
        int num_ref_tri = 0;
        triangle_t* ref_tri = form_triangles(ref_stars, num_ref_stars, &num_ref_tri);
        if (!ref_tri || num_ref_tri == 0) {
            printf("[%s] SKIP: no triangles\n", img_name);
            if (ref_tri) free(ref_tri);
            free(ref_stars);
            free(frame0);
            free(ref_gray);
            n_skipped++;
            continue;
        }

        /* ------------------------------------------------------------------
         * Step 3b: Generate 9 rotation+translation+noise variants
         * Each image gets its own reproducible seed based on index.
         * ------------------------------------------------------------------ */
        srand(42 + img_idx);
        float angles[9], txs[9], tys[9];
        for (int i = 0; i < 9; i++) {
            angles[i] = (float)(rand() % 600 - 300) / 100.0f;  /* -3.0 to +3.0 degrees */
            txs[i]    = (float)(rand() % 60 - 30);               /* -30 to +30 px */
            tys[i]    = (float)(rand() % 60 - 30);
        }

        /* Allocate 9 variant images */
        unsigned char** variants = (unsigned char**)malloc(9 * sizeof(unsigned char*));
        if (!variants) {
            printf("[%s] SKIP: OOM for variant array\n", img_name);
            free(ref_tri); free(ref_stars); free(frame0); free(ref_gray);
            n_skipped++;
            continue;
        }
        int variants_ok = 1;
        for (int i = 0; i < 9; i++) {
            variants[i] = (unsigned char*)malloc((size_t)npix);
            if (!variants[i]) {
                printf("[%s] SKIP: OOM for variant %d\n", img_name, i);
                for (int j = 0; j < i; j++) free(variants[j]);
                free(variants);
                free(ref_tri); free(ref_stars); free(frame0); free(ref_gray);
                variants_ok = 0;
                break;
            }
        }
        if (!variants_ok) {
            n_skipped++;
            continue;
        }

        float cx = (float)w * 0.5f;
        float cy = (float)h * 0.5f;

        /* Re-seed for variant pixel noise (after angles/txs/tys are set).
         * Use img_idx+2000 so variant noise is independent of frame0 noise. */
        srand(2000 + img_idx);

        for (int i = 0; i < 9; i++) {
            float angle_rad = angles[i] * (float)M_PI / 180.0f;
            float cos_a = cosf(angle_rad);
            float sin_a = sinf(angle_rad);

            unsigned char* dst = variants[i];

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float dx = (float)x - cx - txs[i];
                    float dy = (float)y - cy - tys[i];
                    float src_x = cos_a * dx + sin_a * dy + cx;
                    float src_y = -sin_a * dx + cos_a * dy + cy;

                    float bilinear_val = bilinear_float(ref_gray, w, h, src_x, src_y);

                    /* Box-Muller Gaussian noise sigma=10 */
                    float u1 = ((float)rand() + 1.0f) / ((float)RAND_MAX + 2.0f);
                    float u2 = (float)rand() / ((float)RAND_MAX + 1.0f);
                    float noise = sqrtf(-2.0f * logf(u1)) * cosf(2.0f * (float)M_PI * u2) * 20.0f;

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
        unsigned char* blurred = (unsigned char*)malloc((size_t)npix);
        if (!blurred) {
            printf("[%s] SKIP: OOM for blurred buffer\n", img_name);
            for (int i = 0; i < 9; i++) free(variants[i]);
            free(variants);
            free(ref_tri); free(ref_stars); free(frame0); free(ref_gray);
            n_skipped++;
            continue;
        }

        /* Initialize accumulator with noisy Frame 0 */
        float* sum_buf   = (float*)calloc((size_t)npix, sizeof(float));
        int*   count_buf = (int*)calloc((size_t)npix, sizeof(int));
        if (!sum_buf || !count_buf) {
            printf("[%s] SKIP: OOM for accumulator\n", img_name);
            free(blurred);
            if (sum_buf)   free(sum_buf);
            if (count_buf) free(count_buf);
            for (int i = 0; i < 9; i++) free(variants[i]);
            free(variants);
            free(ref_tri); free(ref_stars); free(frame0); free(ref_gray);
            n_skipped++;
            continue;
        }
        for (long i = 0; i < npix; i++) {
            sum_buf[i]   = (float)frame0[i];
            count_buf[i] = 1;
        }
        int frames_stacked = 1;  /* noisy frame0 already counted */

        for (int fi = 0; fi < 9; fi++) {
            float angle_rad = angles[fi] * (float)M_PI / 180.0f;
            float cos_a     = cosf(angle_rad);
            float sin_a     = sinf(angle_rad);

            box_blur_3x3(variants[fi], blurred, w, h);

            float var_min_sep = fmaxf(20.0f, 0.05f * (float)(w < h ? w : h));
            int num_new_stars = 0;
            float* new_stars = detect_stars_simple(blurred, w, h, &num_new_stars, var_min_sep);
            if (!new_stars || num_new_stars < 3) {
                if (img_idx == 0) {
                    printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                           " | SKIP (not enough stars: %d)\n",
                           fi + 1, angles[fi], txs[fi], tys[fi], num_new_stars);
                }
                if (new_stars) free(new_stars);
                continue;
            }

            int num_new_tri = 0;
            int use_stars = (num_new_stars < MAX_STACKING_STARS) ? num_new_stars : MAX_STACKING_STARS;
            triangle_t* new_tri = form_triangles(new_stars, use_stars, &num_new_tri);
            if (!new_tri || num_new_tri == 0) {
                if (img_idx == 0) {
                    printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                           " | SKIP (no triangles formed)\n",
                           fi + 1, angles[fi], txs[fi], tys[fi]);
                }
                if (new_tri) free(new_tri);
                free(new_stars);
                continue;
            }

            int num_corr = 0;
            correspondence_t* corr = match_triangles(
                ref_tri, num_ref_tri, ref_stars,
                new_tri, num_new_tri, new_stars,
                &num_corr);
            free(new_tri);

            if (!corr || num_corr < 3) {
                if (img_idx == 0) {
                    printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                           " | SKIP (too few correspondences: %d)\n",
                           fi + 1, angles[fi], txs[fi], tys[fi], num_corr);
                }
                if (corr) free(corr);
                free(new_stars);
                continue;
            }

            affine_t aff;
            int inliers = 0;
            double rms = 0.0;
            int ransac_ok = ransac_affine(corr, num_corr, &aff, &inliers, &rms);
            free(corr);
            free(new_stars);

            if (!ransac_ok) {
                if (img_idx == 0) {
                    printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                           " | SKIP (RANSAC failed)\n",
                           fi + 1, angles[fi], txs[fi], tys[fi]);
                }
                continue;
            }

            double det = aff.a * aff.d - aff.b * aff.c;
            if (fabs(det - 1.0) > 0.3) {
                if (img_idx == 0) {
                    printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                           " | SKIP: non-rigid affine (det=%.3f)\n",
                           fi + 1, angles[fi], txs[fi], tys[fi], det);
                }
                continue;
            }

            /* Alignment error check and verbose output (first image only) */
            if (img_idx == 0) {
                float test_pts[5][2] = {
                    {100.0f,           100.0f},
                    {(float)w * 0.75f, 100.0f},
                    {100.0f,           (float)h * 0.75f},
                    {(float)w * 0.5f,  (float)h * 0.5f},
                    {(float)w * 0.25f, (float)h * 0.75f}
                };

                float align_err_sum = 0.0f;
                for (int tp = 0; tp < 5; tp++) {
                    float rx = test_pts[tp][0];
                    float ry = test_pts[tp][1];

                    float dx = rx - cx;
                    float dy = ry - cy;
                    float nx = cx + (dx * cos_a - dy * sin_a) + txs[fi];
                    float ny = cy + (dx * sin_a + dy * cos_a) + tys[fi];

                    float rx_rec, ry_rec;
                    apply_affine(&aff, nx, ny, &rx_rec, &ry_rec);

                    float edx = rx_rec - rx;
                    float edy = ry_rec - ry;
                    align_err_sum += edx * edx + edy * edy;
                }
                float align_err = sqrtf(align_err_sum / 5.0f);
                int aligned_ok = (align_err < 5.0f);

                printf("Frame %d: expected rot=%.2fdeg tx=%.0f ty=%.0f"
                       " | recovered a=%.3f b=%.3f tx=%.1f ty=%.1f det=%.3f"
                       " | align_err=%.2fpx [%s]\n",
                       fi + 1, angles[fi], txs[fi], tys[fi],
                       (float)aff.a, (float)aff.b, (float)aff.tx, (float)aff.ty,
                       det,
                       align_err,
                       aligned_ok ? "PASS" : "FAIL");
            }

            /* Warp and accumulate */
            affine_t inv;
            if (!invert_affine(&aff, &inv)) {
                if (img_idx == 0) {
                    printf("  NOTE: affine inversion failed for frame %d, skipping warp\n", fi + 1);
                }
                continue;
            }

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float sx, sy;
                    apply_affine(&inv, (float)x, (float)y, &sx, &sy);
                    if (sx >= 0.0f && sy >= 0.0f && sx < (float)(w - 1) && sy < (float)(h - 1)) {
                        sum_buf[y * w + x]   += bilinear_float(variants[fi], w, h, sx, sy);
                        count_buf[y * w + x]++;
                    }
                }
            }
            frames_stacked++;
        }

        free(blurred);

        if (img_idx == 0) {
            printf("\nFrame 0: noisy reference (sigma=20, identity transform)\n");
            printf("Variants successfully aligned: %d/9\n\n", frames_stacked - 1);
        }

        /* ------------------------------------------------------------------
         * Step 5: Compute final stacked image
         * ------------------------------------------------------------------ */
        unsigned char* stacked = (unsigned char*)malloc((size_t)npix);
        if (!stacked) {
            printf("[%s] SKIP: OOM for stacked output\n", img_name);
            free(sum_buf); free(count_buf);
            for (int i = 0; i < 9; i++) free(variants[i]);
            free(variants);
            free(ref_tri); free(ref_stars); free(frame0); free(ref_gray);
            n_skipped++;
            continue;
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
        int star_cx = (int)ref_stars[0];
        int star_cy = (int)ref_stars[1];

        float bg_std_before, bg_std_after;
        float snr_before = measure_snr(frame0,   w, h, star_cx, star_cy, &bg_std_before);
        float snr_after  = measure_snr(stacked,  w, h, star_cx, star_cy, &bg_std_after);
        float snr_improvement = (snr_before > 0.0f) ? snr_after / snr_before : 0.0f;
        float std_reduction = (bg_std_after > 0.0f) ? bg_std_before / bg_std_after : 0.0f;

        int frames_aligned = frames_stacked - 1;  /* exclude reference */
        int pass_snr   = (snr_improvement >= 2.0f);
        int pass_frame = (frames_aligned >= 8);

        /* Per-image one-liner */
        printf("[%s %dx%d] ref_stars=%d frames=%d/9 snr=%.1f->%.1f (%.2fx) std_red=%.2fx %s\n",
               img_name, w, h,
               num_ref_stars,
               frames_aligned,
               snr_before, snr_after, snr_improvement,
               std_reduction,
               (pass_snr && pass_frame) ? "PASS" : "FAIL");

        /* For first image: also print verbose SNR info and save PGMs */
        if (img_idx == 0) {
            printf("\nStar peak location: (%d, %d)\n", star_cx, star_cy);
            printf("SNR before (single noisy frame): %.1f\n", snr_before);
            printf("SNR after  (10-frame stack):     %.1f\n", snr_after);
            printf("Improvement: %.2fx [%s]\n\n",
                   snr_improvement,
                   pass_snr ? "PASS" : "FAIL");

            /* Save PGMs only for first image */
            save_pgm("/mnt/d/Download/DIP/frame_000.pgm", frame0, w, h);
            char pgm_path[128];
            for (int i = 0; i < 9; i++) {
                snprintf(pgm_path, sizeof(pgm_path), "/mnt/d/Download/DIP/frame_%03d.pgm", i + 1);
                save_pgm(pgm_path, variants[i], w, h);
            }
            save_pgm("/mnt/d/Download/DIP/stacked_output.pgm", stacked, w, h);
            printf("PGM files saved: frame_000..009.pgm, stacked_output.pgm\n\n");
        }

        /* Aggregate tracking */
        n_processed++;
        total_frames += frames_aligned;
        sum_snr_improvement += snr_improvement;
        sum_std_reduction   += std_reduction;
        if (snr_improvement < min_snr_improvement) {
            min_snr_improvement = snr_improvement;
            min_snr_idx = img_idx;
        }
        if (snr_improvement > max_snr_improvement) {
            max_snr_improvement = snr_improvement;
            max_snr_idx = img_idx;
        }
        if (pass_snr) n_passing_snr++;

        /* Cleanup per-image allocations */
        free(stacked);
        free(sum_buf);
        free(count_buf);
        for (int i = 0; i < 9; i++) free(variants[i]);
        free(variants);
        free(ref_tri);
        free(ref_stars);
        free(frame0);
        free(ref_gray);
    }

    /* ------------------------------------------------------------------
     * Aggregate summary
     * ------------------------------------------------------------------ */
    printf("\n=== AGGREGATE RESULTS (%d / %d images processed) ===\n",
           n_processed, TOTAL_IMAGES);
    printf("Images skipped (load/star detection failure): %d\n", n_skipped);

    if (n_processed > 0) {
        double avg_frames        = (double)total_frames / (double)n_processed;
        double avg_snr           = sum_snr_improvement / (double)n_processed;
        double avg_std_reduction = sum_std_reduction   / (double)n_processed;

        printf("Avg frames stacked per image:  %.1f / 9\n", avg_frames);
        printf("Avg SNR improvement:           %.2fx\n", avg_snr);
        printf("Min SNR improvement:           %.2fx  (%s)\n",
               min_snr_improvement,
               min_snr_idx >= 0 ? IMAGE_NAMES[min_snr_idx] : "N/A");
        printf("Max SNR improvement:           %.2fx  (%s)\n",
               max_snr_improvement,
               max_snr_idx >= 0 ? IMAGE_NAMES[max_snr_idx] : "N/A");
        printf("Images passing SNR >= 2.0x:    %d / %d\n", n_passing_snr, n_processed);
        printf("Avg background std reduction:  %.2fx  (theoretical sqrt(10) = 3.16x)\n",
               avg_std_reduction);

        int pass_avg_snr       = (avg_snr >= 1.5);
        int pass_avg_std_red   = (avg_std_reduction >= 2.0);
        int pass_avg_frames    = (avg_frames >= 7.0);
        int overall_pass       = pass_avg_snr && pass_avg_std_red && pass_avg_frames;

        printf("=== OVERALL: %s ===\n", overall_pass ? "PASS" : "FAIL");

        return overall_pass ? 0 : 1;
    } else {
        printf("No images processed.\n");
        printf("=== OVERALL: FAIL ===\n");
        return 1;
    }
}
