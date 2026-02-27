#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>
#include <unistd.h>

#include "astrometry/starxy.h"
#include "astrometry/kdtree.h"
#include "gsl/gsl_matrix.h"
#include "gsl/gsl_vector.h"
#include "gsl/gsl_linalg.h"
#include "gsl/gsl_blas.h"

#define LOG_TAG "StackingNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Configuration constants
#define MAX_TRIANGLES_PER_STAR 10  // C(5,2) = 10 triangles from 5 nearest neighbors
#define NUM_NEIGHBORS 5            // Use 5 nearest neighbors per star
#define TRIANGLE_RATIO_TOLERANCE 0.01  // Match tolerance for side ratios (tight: rotation is isometric)
#define RANSAC_ITERATIONS 500      // Number of RANSAC iterations
#define RANSAC_INLIER_THRESHOLD 3.0  // 3 pixels reprojection error threshold
#define MAX_STACKING_STARS 50      // Use top 50 brightest stars for alignment

// Triangle descriptor: scale-invariant side-length ratios
typedef struct {
    float ratio1;  // s1/s0 (sorted sides s0 <= s1 <= s2)
    float ratio2;  // s2/s0
    int star_indices[3];  // which 3 stars form this triangle
} triangle_t;

// Star correspondence for RANSAC
typedef struct {
    float ref_x, ref_y;
    float new_x, new_y;
} correspondence_t;

// Affine transform: [x'] = [a b tx] [x]
//                    [y']   [c d ty] [y]
//                                    [1]
typedef struct {
    double a, b, c, d, tx, ty;
} affine_t;

// Stacking context (accumulator + reference frame info)
typedef struct {
    int width;
    int height;
    int is_color;
    int frame_count;

    // Accumulator (grayscale only for now)
    float* sum_r;     // Running sum of pixel values
    int* count;       // Per-pixel frame count

    // Reference frame info (first frame's stars)
    triangle_t* ref_triangles;
    int num_ref_triangles;
    float* ref_stars;  // [x, y, flux] * N
    int num_ref_stars;
} stacking_context_t;

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

// Euclidean distance squared between two points
static inline float dist2(float x1, float y1, float x2, float y2) {
    float dx = x2 - x1;
    float dy = y2 - y1;
    return dx*dx + dy*dy;
}

// Sort 3 side lengths (in-place)
static void sort3(float* s0, float* s1, float* s2) {
    if (*s0 > *s1) { float tmp = *s0; *s0 = *s1; *s1 = tmp; }
    if (*s1 > *s2) { float tmp = *s1; *s1 = *s2; *s2 = tmp; }
    if (*s0 > *s1) { float tmp = *s0; *s0 = *s1; *s1 = tmp; }
}

// Apply affine transform to a point
static void apply_affine(const affine_t* aff, float x, float y, float* out_x, float* out_y) {
    *out_x = aff->a * x + aff->b * y + aff->tx;
    *out_y = aff->c * x + aff->d * y + aff->ty;
}

// Compute inverse affine transform
static int invert_affine(const affine_t* aff, affine_t* inv) {
    double det = aff->a * aff->d - aff->b * aff->c;
    if (fabs(det) < 1e-10) {
        return 0;  // Singular matrix
    }
    inv->a = aff->d / det;
    inv->b = -aff->b / det;
    inv->c = -aff->c / det;
    inv->d = aff->a / det;
    inv->tx = (aff->b * aff->ty - aff->d * aff->tx) / det;
    inv->ty = (aff->c * aff->tx - aff->a * aff->ty) / det;
    return 1;
}

// ============================================================================
// TRIANGLE FORMATION
// ============================================================================

// Form triangles from a set of stars using nearest neighbors
// For each star, find 5 nearest neighbors, form C(5,2)=10 triangles
static triangle_t* form_triangles(float* stars, int num_stars, int* out_num_triangles) {
    if (num_stars < 3) {
        *out_num_triangles = 0;
        return NULL;
    }

    int max_use_stars = (num_stars < MAX_STACKING_STARS) ? num_stars : MAX_STACKING_STARS;
    int max_triangles = max_use_stars * MAX_TRIANGLES_PER_STAR;
    triangle_t* triangles = (triangle_t*)malloc(max_triangles * sizeof(triangle_t));
    if (!triangles) {
        LOGE("Failed to allocate triangles");
        *out_num_triangles = 0;
        return NULL;
    }

    int tri_idx = 0;

    // For each star (only use top MAX_STACKING_STARS)
    for (int i = 0; i < max_use_stars; i++) {
        float xi = stars[i * 3];
        float yi = stars[i * 3 + 1];

        // Find NUM_NEIGHBORS nearest neighbors (brute force is fast for 50 stars)
        typedef struct { float d2; int idx; } neighbor_t;
        neighbor_t neighbors[MAX_STACKING_STARS];
        int num_neighbors = 0;

        for (int j = 0; j < max_use_stars; j++) {
            if (i == j) continue;
            float xj = stars[j * 3];
            float yj = stars[j * 3 + 1];
            float d2 = dist2(xi, yi, xj, yj);
            neighbors[num_neighbors].d2 = d2;
            neighbors[num_neighbors].idx = j;
            num_neighbors++;
        }

        // Sort by distance (simple insertion sort, N is small)
        for (int a = 1; a < num_neighbors; a++) {
            neighbor_t key = neighbors[a];
            int b = a - 1;
            while (b >= 0 && neighbors[b].d2 > key.d2) {
                neighbors[b + 1] = neighbors[b];
                b--;
            }
            neighbors[b + 1] = key;
        }

        // Take first NUM_NEIGHBORS
        int use_neighbors = (num_neighbors < NUM_NEIGHBORS) ? num_neighbors : NUM_NEIGHBORS;

        // Form triangles: C(use_neighbors, 2) pairs with star i
        for (int a = 0; a < use_neighbors; a++) {
            for (int b = a + 1; b < use_neighbors; b++) {
                if (tri_idx >= max_triangles) break;

                int idx_a = neighbors[a].idx;
                int idx_b = neighbors[b].idx;

                // Compute side lengths.
                // Each side is "opposite" one vertex:
                //   sa = dist(i, idx_a)  → opposite idx_b
                //   sb = dist(i, idx_b)  → opposite idx_a
                //   sc = dist(idx_a, idx_b) → opposite i
                float sa = sqrtf(dist2(xi, yi, stars[idx_a * 3], stars[idx_a * 3 + 1]));
                float sb = sqrtf(dist2(xi, yi, stars[idx_b * 3], stars[idx_b * 3 + 1]));
                float sc = sqrtf(dist2(stars[idx_a * 3], stars[idx_a * 3 + 1],
                                      stars[idx_b * 3], stars[idx_b * 3 + 1]));

                if (sa < 1e-6f || sb < 1e-6f || sc < 1e-6f) continue;  // Degenerate

                // Sort (side, opposite_vertex) pairs by side length so that
                // star_indices[k] is always the vertex opposite the k-th shortest side.
                // This canonical ordering ensures that when two triangles match by
                // ratio, their star_indices[k] arrays are truly corresponding stars.
                float sides[3] = { sa, sb, sc };
                int   verts[3] = { idx_b, idx_a, i };  // opposite to sa, sb, sc
                // Insertion sort (3 elements)
                for (int p = 1; p < 3; p++) {
                    float ks = sides[p]; int kv = verts[p];
                    int q = p - 1;
                    while (q >= 0 && sides[q] > ks) {
                        sides[q+1] = sides[q]; verts[q+1] = verts[q]; q--;
                    }
                    sides[q+1] = ks; verts[q+1] = kv;
                }
                // sides[0] ≤ sides[1] ≤ sides[2]; verts[k] opposite sides[k]

                // Compute scale-invariant ratios
                triangles[tri_idx].ratio1 = sides[1] / sides[0];
                triangles[tri_idx].ratio2 = sides[2] / sides[0];
                triangles[tri_idx].star_indices[0] = verts[0];
                triangles[tri_idx].star_indices[1] = verts[1];
                triangles[tri_idx].star_indices[2] = verts[2];
                tri_idx++;
            }
        }
    }

    *out_num_triangles = tri_idx;
    return triangles;
}

// ============================================================================
// TRIANGLE MATCHING
// ============================================================================

// Match triangles between reference and new frame, build correspondence list
static correspondence_t* match_triangles(
    triangle_t* ref_tri, int num_ref_tri, float* ref_stars,
    triangle_t* new_tri, int num_new_tri, float* new_stars,
    int* out_num_correspondences)
{
    // Brute force matching (fast for ~500 triangles each)
    // Each matching triangle pair produces 3 correspondences; cap for memory safety
    int max_corr = num_ref_tri * num_new_tri * 3;
    if (max_corr > 10000) max_corr = 10000;
    correspondence_t* corr = (correspondence_t*)malloc(max_corr * sizeof(correspondence_t));
    if (!corr) {
        LOGE("Failed to allocate correspondences");
        *out_num_correspondences = 0;
        return NULL;
    }

    int corr_idx = 0;
    int total_tri_matches = 0;  // diagnostic: total triangle pairs that match by ratio

    for (int i = 0; i < num_new_tri; i++) {
        for (int j = 0; j < num_ref_tri; j++) {
            // Check if ratios match within tolerance
            if (fabsf(new_tri[i].ratio1 - ref_tri[j].ratio1) < TRIANGLE_RATIO_TOLERANCE &&
                fabsf(new_tri[i].ratio2 - ref_tri[j].ratio2) < TRIANGLE_RATIO_TOLERANCE)
            {
                total_tri_matches++;
                // Triangle match found - add 3 star correspondences
                for (int k = 0; k < 3; k++) {
                    if (corr_idx >= max_corr) break;

                    int new_idx = new_tri[i].star_indices[k];
                    int ref_idx = ref_tri[j].star_indices[k];

                    corr[corr_idx].new_x = new_stars[new_idx * 3];
                    corr[corr_idx].new_y = new_stars[new_idx * 3 + 1];
                    corr[corr_idx].ref_x = ref_stars[ref_idx * 3];
                    corr[corr_idx].ref_y = ref_stars[ref_idx * 3 + 1];
                    corr_idx++;
                }
            }
        }
    }

    *out_num_correspondences = corr_idx;
    LOGI("Found %d star correspondences from %d triangle matches (cap=%d)",
         corr_idx, total_tri_matches, max_corr);
    return corr;
}

// ============================================================================
// RANSAC AFFINE ESTIMATION
// ============================================================================

// Solve affine transform from 3 correspondences using GSL
static int solve_affine_3pt(correspondence_t* corr, affine_t* aff) {
    // System: [x'] = [a b tx] [x]
    //         [y']   [c d ty] [y]
    //                         [1]
    // For 3 points, we get 6 equations:
    // x0' = a*x0 + b*y0 + tx
    // y0' = c*x0 + d*y0 + ty
    // ... (same for points 1, 2)

    gsl_matrix* A = gsl_matrix_alloc(6, 6);
    gsl_vector* b = gsl_vector_alloc(6);
    gsl_vector* x = gsl_vector_alloc(6);
    gsl_permutation* p = gsl_permutation_alloc(6);

    if (!A || !b || !x || !p) {
        if (A) gsl_matrix_free(A);
        if (b) gsl_vector_free(b);
        if (x) gsl_vector_free(x);
        if (p) gsl_permutation_free(p);
        return 0;
    }

    // Fill matrix (each correspondence gives 2 rows)
    for (int i = 0; i < 3; i++) {
        int row_x = i * 2;
        int row_y = i * 2 + 1;

        // Row for x': a*x + b*y + tx = x'
        gsl_matrix_set(A, row_x, 0, corr[i].new_x);  // a coefficient
        gsl_matrix_set(A, row_x, 1, corr[i].new_y);  // b coefficient
        gsl_matrix_set(A, row_x, 2, 0.0);            // c coefficient
        gsl_matrix_set(A, row_x, 3, 0.0);            // d coefficient
        gsl_matrix_set(A, row_x, 4, 1.0);            // tx coefficient
        gsl_matrix_set(A, row_x, 5, 0.0);            // ty coefficient
        gsl_vector_set(b, row_x, corr[i].ref_x);

        // Row for y': c*x + d*y + ty = y'
        gsl_matrix_set(A, row_y, 0, 0.0);
        gsl_matrix_set(A, row_y, 1, 0.0);
        gsl_matrix_set(A, row_y, 2, corr[i].new_x);
        gsl_matrix_set(A, row_y, 3, corr[i].new_y);
        gsl_matrix_set(A, row_y, 4, 0.0);
        gsl_matrix_set(A, row_y, 5, 1.0);
        gsl_vector_set(b, row_y, corr[i].ref_y);
    }

    // Solve Ax = b via LU decomposition
    int signum;
    int result = gsl_linalg_LU_decomp(A, p, &signum);
    if (result != 0) {
        gsl_matrix_free(A);
        gsl_vector_free(b);
        gsl_vector_free(x);
        gsl_permutation_free(p);
        return 0;
    }

    result = gsl_linalg_LU_solve(A, p, b, x);
    if (result != 0) {
        gsl_matrix_free(A);
        gsl_vector_free(b);
        gsl_vector_free(x);
        gsl_permutation_free(p);
        return 0;
    }

    // Extract solution
    aff->a = gsl_vector_get(x, 0);
    aff->b = gsl_vector_get(x, 1);
    aff->c = gsl_vector_get(x, 2);
    aff->d = gsl_vector_get(x, 3);
    aff->tx = gsl_vector_get(x, 4);
    aff->ty = gsl_vector_get(x, 5);

    gsl_matrix_free(A);
    gsl_vector_free(b);
    gsl_vector_free(x);
    gsl_permutation_free(p);

    return 1;
}

// Count inliers and compute RMS error for an affine transform
static void evaluate_affine(affine_t* aff, correspondence_t* corr, int num_corr,
                           int* out_inliers, double* out_rms)
{
    int inliers = 0;
    double sum_sq_error = 0.0;

    for (int i = 0; i < num_corr; i++) {
        float proj_x, proj_y;
        apply_affine(aff, corr[i].new_x, corr[i].new_y, &proj_x, &proj_y);

        float error = sqrtf(dist2(proj_x, proj_y, corr[i].ref_x, corr[i].ref_y));
        sum_sq_error += error * error;

        if (error < RANSAC_INLIER_THRESHOLD) {
            inliers++;
        }
    }

    *out_inliers = inliers;
    *out_rms = (num_corr > 0) ? sqrt(sum_sq_error / num_corr) : 0.0;
}

// RANSAC: find best affine transform from correspondences
static int ransac_affine(correspondence_t* corr, int num_corr, affine_t* best_aff,
                        int* out_inliers, double* out_rms)
{
    if (num_corr < 3) {
        LOGE("Not enough correspondences for RANSAC (%d < 3)", num_corr);
        return 0;
    }

    int best_inliers = 0;
    double best_rms = 1e9;
    affine_t best;

    for (int iter = 0; iter < RANSAC_ITERATIONS; iter++) {
        // Pick 3 random correspondences
        correspondence_t sample[3];
        int indices[3];
        for (int i = 0; i < 3; i++) {
            int retry = 0;
            do {
                indices[i] = rand() % num_corr;
                // Check for duplicates
                int dup = 0;
                for (int j = 0; j < i; j++) {
                    if (indices[i] == indices[j]) {
                        dup = 1;
                        break;
                    }
                }
                if (!dup) break;
                retry++;
            } while (retry < 10);

            sample[i] = corr[indices[i]];
        }

        // Solve affine
        affine_t aff;
        if (!solve_affine_3pt(sample, &aff)) {
            continue;
        }

        // Evaluate on all correspondences
        int inliers;
        double rms;
        evaluate_affine(&aff, corr, num_corr, &inliers, &rms);

        // Keep if better
        if (inliers > best_inliers || (inliers == best_inliers && rms < best_rms)) {
            best_inliers = inliers;
            best_rms = rms;
            best = aff;
        }
    }

    if (best_inliers == 0) {
        LOGE("RANSAC failed to find any inliers");
        return 0;
    }

    *best_aff = best;
    *out_inliers = best_inliers;
    *out_rms = best_rms;

    LOGI("RANSAC: %d inliers, RMS=%.2f px", best_inliers, best_rms);
    return 1;
}

// ============================================================================
// BILINEAR INTERPOLATION & WARPING
// ============================================================================

// Bilinear interpolation at (x, y) in image
static float bilinear_sample(unsigned char* image, int width, int height, float x, float y) {
    if (x < 0 || y < 0 || x >= width - 1 || y >= height - 1) {
        return 0.0f;  // Out of bounds
    }

    int x0 = (int)x;
    int y0 = (int)y;
    int x1 = x0 + 1;
    int y1 = y0 + 1;

    float fx = x - x0;
    float fy = y - y0;

    float v00 = (float)image[y0 * width + x0];
    float v10 = (float)image[y0 * width + x1];
    float v01 = (float)image[y1 * width + x0];
    float v11 = (float)image[y1 * width + x1];

    float v0 = v00 * (1.0f - fx) + v10 * fx;
    float v1 = v01 * (1.0f - fx) + v11 * fx;

    return v0 * (1.0f - fy) + v1 * fy;
}

// Warp image to reference frame using affine transform and accumulate
static void warp_and_accumulate(stacking_context_t* ctx, unsigned char* image,
                               affine_t* aff)
{
    // Compute inverse affine (map reference pixels to new frame)
    affine_t inv_aff;
    if (!invert_affine(aff, &inv_aff)) {
        LOGE("Failed to invert affine transform");
        return;
    }

    int npix = ctx->width * ctx->height;

    // For each pixel in reference frame
    for (int y = 0; y < ctx->height; y++) {
        for (int x = 0; x < ctx->width; x++) {
            int idx = y * ctx->width + x;

            // Map to new frame coordinates
            float src_x, src_y;
            apply_affine(&inv_aff, (float)x, (float)y, &src_x, &src_y);

            // Sample new frame with bilinear interpolation
            if (src_x >= 0 && src_y >= 0 && src_x < ctx->width - 1 && src_y < ctx->height - 1) {
                float value = bilinear_sample(image, ctx->width, ctx->height, src_x, src_y);
                ctx->sum_r[idx] += value;
                ctx->count[idx]++;
            }
        }
    }

    ctx->frame_count++;
}

// ============================================================================
// JNI ENTRY POINTS
// ============================================================================

#ifndef STACKING_TESTING  /* Skip JNI entry points when unit-testing static functions */

JNIEXPORT jlong JNICALL
Java_com_astro_app_native_1_StackingNative_initStackingNative(
    JNIEnv *env,
    jclass clazz,
    jint width,
    jint height,
    jboolean isColor)
{
    LOGI("initStackingNative: %dx%d, color=%d", width, height, isColor);

    stacking_context_t* ctx = (stacking_context_t*)calloc(1, sizeof(stacking_context_t));
    if (!ctx) {
        LOGE("Failed to allocate context");
        return 0;
    }

    // Initialize random seed once per session (combines timestamp + process ID)
    srand((unsigned int)time(NULL) ^ (unsigned int)getpid());
    LOGI("Initialized random seed for stacking session");

    ctx->width = width;
    ctx->height = height;
    ctx->is_color = isColor;
    ctx->frame_count = 0;

    int npix = width * height;

    // Allocate accumulator (grayscale only for now)
    ctx->sum_r = (float*)calloc(npix, sizeof(float));
    ctx->count = (int*)calloc(npix, sizeof(int));

    if (!ctx->sum_r || !ctx->count) {
        LOGE("Failed to allocate accumulator");
        free(ctx->sum_r);
        free(ctx->count);
        free(ctx);
        return 0;
    }

    ctx->ref_triangles = NULL;
    ctx->num_ref_triangles = 0;
    ctx->ref_stars = NULL;
    ctx->num_ref_stars = 0;

    LOGI("Stacking context initialized");
    return (jlong)(intptr_t)ctx;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_astro_app_native_1_StackingNative_addFrameNative(
    JNIEnv *env,
    jclass clazz,
    jlong handle,
    jbyteArray imageData,
    jfloatArray stars,
    jfloatArray refStars)
{
    stacking_context_t* ctx = (stacking_context_t*)(intptr_t)handle;
    if (!ctx) {
        LOGE("Invalid context handle");
        return NULL;
    }

    // Get image data
    jbyte* pixels = (*env)->GetByteArrayElements(env, imageData, NULL);
    if (!pixels) {
        LOGE("Failed to get image data");
        return NULL;
    }

    // Get star arrays
    jfloat* stars_arr = (*env)->GetFloatArrayElements(env, stars, NULL);
    jfloat* ref_stars_arr = (refStars != NULL) ? (*env)->GetFloatArrayElements(env, refStars, NULL) : NULL;

    if (!stars_arr) {
        LOGE("Failed to get stars array");
        (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);
        return NULL;
    }

    int num_stars = (*env)->GetArrayLength(env, stars) / 3;

    LOGI("addFrame: %d stars detected", num_stars);

    // Check if this is the first frame (reference)
    if (ctx->frame_count == 0) {
        // First frame - use as reference, no alignment needed
        LOGI("First frame - initializing reference");

        // Store reference stars
        ctx->num_ref_stars = (num_stars < MAX_STACKING_STARS) ? num_stars : MAX_STACKING_STARS;
        ctx->ref_stars = (float*)malloc(ctx->num_ref_stars * 3 * sizeof(float));
        if (!ctx->ref_stars) {
            LOGE("Failed to allocate ref_stars");
            (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);
            (*env)->ReleaseFloatArrayElements(env, stars, stars_arr, JNI_ABORT);
            return NULL;
        }
        memcpy(ctx->ref_stars, stars_arr, ctx->num_ref_stars * 3 * sizeof(float));

        // Form reference triangles
        ctx->ref_triangles = form_triangles(ctx->ref_stars, ctx->num_ref_stars,
                                           &ctx->num_ref_triangles);
        if (!ctx->ref_triangles) {
            LOGE("Failed to form reference triangles");
            (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);
            (*env)->ReleaseFloatArrayElements(env, stars, stars_arr, JNI_ABORT);
            return NULL;
        }

        LOGI("Formed %d reference triangles from %d stars", ctx->num_ref_triangles,
             ctx->num_ref_stars);

        // Add first frame directly to accumulator (identity transform)
        int npix = ctx->width * ctx->height;
        for (int i = 0; i < npix; i++) {
            ctx->sum_r[i] += (float)((unsigned char)pixels[i]);
            ctx->count[i]++;
        }
        ctx->frame_count++;

        (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);
        (*env)->ReleaseFloatArrayElements(env, stars, stars_arr, JNI_ABORT);

        // Return success
        jdoubleArray result = (*env)->NewDoubleArray(env, 4);
        jdouble buffer[4] = {1.0, 0.0, 0.0, 1.0};  // success, inliers=0, rms=0, frameCount=1
        (*env)->SetDoubleArrayRegion(env, result, 0, 4, buffer);
        return result;
    }

    // Subsequent frames - align to reference
    LOGI("Aligning frame %d to reference", ctx->frame_count + 1);

    // Form triangles from new frame
    int num_new_tri;
    int use_stars = (num_stars < MAX_STACKING_STARS) ? num_stars : MAX_STACKING_STARS;
    triangle_t* new_tri = form_triangles(stars_arr, use_stars, &num_new_tri);

    if (!new_tri || num_new_tri == 0) {
        LOGE("Failed to form new frame triangles");
        if (new_tri) free(new_tri);
        (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);
        (*env)->ReleaseFloatArrayElements(env, stars, stars_arr, JNI_ABORT);
        if (ref_stars_arr) (*env)->ReleaseFloatArrayElements(env, refStars, ref_stars_arr, JNI_ABORT);

        // Return failure
        jdoubleArray result = (*env)->NewDoubleArray(env, 4);
        jdouble buffer[4] = {0.0, 0.0, 0.0, (double)ctx->frame_count};
        (*env)->SetDoubleArrayRegion(env, result, 0, 4, buffer);
        return result;
    }

    // Match triangles
    int num_corr;
    correspondence_t* corr = match_triangles(ctx->ref_triangles, ctx->num_ref_triangles,
                                            ctx->ref_stars,
                                            new_tri, num_new_tri, stars_arr,
                                            &num_corr);
    free(new_tri);

    if (!corr || num_corr < 3) {
        LOGE("Triangle matching failed (only %d correspondences)", num_corr);
        (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);
        (*env)->ReleaseFloatArrayElements(env, stars, stars_arr, JNI_ABORT);
        if (ref_stars_arr) (*env)->ReleaseFloatArrayElements(env, refStars, ref_stars_arr, JNI_ABORT);
        if (corr) free(corr);

        // Return failure
        jdoubleArray result = (*env)->NewDoubleArray(env, 4);
        jdouble buffer[4] = {0.0, 0.0, 0.0, (double)ctx->frame_count};
        (*env)->SetDoubleArrayRegion(env, result, 0, 4, buffer);
        return result;
    }

    // RANSAC affine estimation
    affine_t aff;
    int inliers;
    double rms;
    if (!ransac_affine(corr, num_corr, &aff, &inliers, &rms)) {
        LOGE("RANSAC failed");
        free(corr);
        (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);
        (*env)->ReleaseFloatArrayElements(env, stars, stars_arr, JNI_ABORT);
        if (ref_stars_arr) (*env)->ReleaseFloatArrayElements(env, refStars, ref_stars_arr, JNI_ABORT);

        // Return failure
        jdoubleArray result = (*env)->NewDoubleArray(env, 4);
        jdouble buffer[4] = {0.0, 0.0, 0.0, (double)ctx->frame_count};
        (*env)->SetDoubleArrayRegion(env, result, 0, 4, buffer);
        return result;
    }
    free(corr);

    // Warp and accumulate
    warp_and_accumulate(ctx, (unsigned char*)pixels, &aff);

    (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, stars, stars_arr, JNI_ABORT);
    if (ref_stars_arr) (*env)->ReleaseFloatArrayElements(env, refStars, ref_stars_arr, JNI_ABORT);

    LOGI("Frame %d added successfully", ctx->frame_count);

    // Return success
    jdoubleArray result = (*env)->NewDoubleArray(env, 4);
    jdouble buffer[4] = {1.0, (double)inliers, rms, (double)ctx->frame_count};
    (*env)->SetDoubleArrayRegion(env, result, 0, 4, buffer);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_astro_app_native_1_StackingNative_getStackedImageNative(
    JNIEnv *env,
    jclass clazz,
    jlong handle)
{
    stacking_context_t* ctx = (stacking_context_t*)(intptr_t)handle;
    if (!ctx) {
        LOGE("Invalid context handle");
        return NULL;
    }

    if (ctx->frame_count == 0) {
        LOGE("No frames stacked yet");
        return NULL;
    }

    int npix = ctx->width * ctx->height;
    jbyteArray result = (*env)->NewByteArray(env, npix);
    if (!result) {
        LOGE("Failed to allocate result array");
        return NULL;
    }

    jbyte* pixels = (*env)->GetByteArrayElements(env, result, NULL);
    if (!pixels) {
        LOGE("Failed to get result array elements");
        return NULL;
    }

    // Average the accumulated values
    for (int i = 0; i < npix; i++) {
        if (ctx->count[i] > 0) {
            float avg = ctx->sum_r[i] / (float)ctx->count[i];
            int val = (int)(avg + 0.5f);
            if (val < 0) val = 0;
            if (val > 255) val = 255;
            pixels[i] = (jbyte)val;
        } else {
            pixels[i] = 0;
        }
    }

    (*env)->ReleaseByteArrayElements(env, result, pixels, 0);

    LOGI("Generated stacked image from %d frames", ctx->frame_count);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_astro_app_native_1_StackingNative_getFrameCountNative(
    JNIEnv *env,
    jclass clazz,
    jlong handle)
{
    stacking_context_t* ctx = (stacking_context_t*)(intptr_t)handle;
    if (!ctx) {
        return 0;
    }
    return ctx->frame_count;
}

JNIEXPORT void JNICALL
Java_com_astro_app_native_1_StackingNative_releaseNative(
    JNIEnv *env,
    jclass clazz,
    jlong handle)
{
    stacking_context_t* ctx = (stacking_context_t*)(intptr_t)handle;
    if (!ctx) {
        return;
    }

    free(ctx->sum_r);
    free(ctx->count);
    free(ctx->ref_stars);
    free(ctx->ref_triangles);
    free(ctx);

    LOGI("Stacking context released");
}

#endif /* STACKING_TESTING */
