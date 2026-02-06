#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#include "astrometry/simplexy.h"
#include "astrometry/image2xy.h"
#include "astrometry/log.h"
#include "astrometry/solver.h"
#include "astrometry/index.h"
#include "astrometry/starxy.h"
#include "astrometry/sip.h"
#include "astrometry/sip-utils.h"
#include "astrometry/matchobj.h"

#define LOG_TAG "AstrometryNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    log_init(LOG_MSG);
    LOGI("Astrometry native library loaded");
    return JNI_VERSION_1_6;
}

JNIEXPORT jfloatArray JNICALL
Java_com_astro_app_native_1_AstrometryNative_detectStarsNative(
    JNIEnv *env,
    jclass clazz,
    jbyteArray imageData,
    jint width,
    jint height,
    jfloat plim,
    jfloat dpsf,
    jint downsample
) {
    jbyte* pixels = (*env)->GetByteArrayElements(env, imageData, NULL);
    if (!pixels) {
        LOGE("Failed to get image data");
        return NULL;
    }

    // Convert u8 grayscale to float, matching solve-field's code path.
    // solve-field reads BITPIX=8 FITS as TFLOAT via cfitsio, so image2xy_run
    // always receives float data. Using image_u8 produces different detection
    // results (different star count and order).
    int npix = width * height;
    float* image_f = (float*)malloc(npix * sizeof(float));
    if (!image_f) {
        LOGE("Failed to allocate float image (%dx%d)", width, height);
        (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);
        return NULL;
    }
    for (int i = 0; i < npix; i++) {
        image_f[i] = (float)((unsigned char)pixels[i]);
    }
    (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);

    // Set up simplexy parameters
    simplexy_t params;
    memset(&params, 0, sizeof(simplexy_t));
    simplexy_fill_in_defaults(&params);

    params.image = image_f;
    params.nx = width;
    params.ny = height;
    params.dpsf = dpsf;
    params.plim = plim;
    params.dlim = 1.0;
    params.saddle = 5.0;
    params.maxper = 1000;
    params.maxnpeaks = 100000;
    params.maxsize = 2000;
    params.halfbox = 100;

    LOGI("Running image2xy on %dx%d image (float), downsample=%d, plim=%.1f, dpsf=%.1f",
         width, height, downsample, plim, dpsf);

    // Run detection with downsampling
    int result = image2xy_run(&params, downsample, 0);

    if (result != 0 || params.npeaks == 0) {
        LOGE("Star detection failed (result=%d) or no stars found (npeaks=%d)", result, params.npeaks);
        simplexy_free_contents(&params);
        return NULL;
    }

    LOGI("Detected %d stars", params.npeaks);

    int N = params.npeaks;

    // Resort stars using solve-field's interleaved merge algorithm
    // (from resort-xylist.c). This interleaves two orderings:
    // 1. Sorted by background-subtracted flux (descending)
    // 2. Sorted by raw flux (flux + background) (descending)
    // This ensures the brightest stars appear first in the list,
    // which is critical for the solver's depth iteration.
    int* perm1 = malloc(N * sizeof(int));  // flux-sorted indices
    int* perm2 = malloc(N * sizeof(int));  // raw-signal-sorted indices
    float* rawsignal = malloc(N * sizeof(float));
    unsigned char* used = calloc(N, 1);
    int* output_order = malloc(N * sizeof(int));

    if (!perm1 || !perm2 || !rawsignal || !used || !output_order) {
        LOGE("Failed to allocate resort buffers");
        free(perm1); free(perm2); free(rawsignal); free(used); free(output_order);
        simplexy_free_contents(&params);
        return NULL;
    }

    // Initialize permutation arrays as identity
    for (int i = 0; i < N; i++) {
        perm1[i] = i;
        perm2[i] = i;
        rawsignal[i] = params.flux[i] + params.background[i];
    }

    // Sort perm1 by flux descending (simple insertion sort, N is small ~700)
    for (int i = 1; i < N; i++) {
        int key = perm1[i];
        float keyval = params.flux[key];
        int j = i - 1;
        while (j >= 0 && params.flux[perm1[j]] < keyval) {
            perm1[j + 1] = perm1[j];
            j--;
        }
        perm1[j + 1] = key;
    }

    // Sort perm2 by rawsignal descending
    for (int i = 1; i < N; i++) {
        int key = perm2[i];
        float keyval = rawsignal[key];
        int j = i - 1;
        while (j >= 0 && rawsignal[perm2[j]] < keyval) {
            perm2[j + 1] = perm2[j];
            j--;
        }
        perm2[j + 1] = key;
    }

    // Interleave: for each rank, emit perm1[i] then perm2[i] (skip used)
    int out_idx = 0;
    for (int i = 0; i < N && out_idx < N; i++) {
        if (!used[perm1[i]]) {
            used[perm1[i]] = 1;
            output_order[out_idx++] = perm1[i];
        }
        if (out_idx < N && !used[perm2[i]]) {
            used[perm2[i]] = 1;
            output_order[out_idx++] = perm2[i];
        }
    }

    LOGI("Resorted %d stars (interleaved flux/rawsignal)", out_idx);

    // Uniformize: spatially distribute stars across grid bins (matching
    // solve-field's uniformize.py). Round-robin interleaves bins so that
    // early stars span the entire field, enabling the solver to form
    // field-spanning quads immediately instead of clustering in one area.
    {
        float xmin = params.x[output_order[0]], xmax = xmin;
        float ymin = params.y[output_order[0]], ymax = ymin;
        for (int i = 1; i < N; i++) {
            int s = output_order[i];
            if (params.x[s] < xmin) xmin = params.x[s];
            if (params.x[s] > xmax) xmax = params.x[s];
            if (params.y[s] < ymin) ymin = params.y[s];
            if (params.y[s] > ymax) ymax = params.y[s];
        }
        float Wf = xmax - xmin;
        float Hf = ymax - ymin;

        if (Wf > 0 && Hf > 0) {
            int UNIFORMIZE_N = 10;
            int NX = (int)(Wf / sqrtf(Wf * Hf / (float)UNIFORMIZE_N) + 0.5f);
            if (NX < 1) NX = 1;
            int NY = (int)((float)UNIFORMIZE_N / (float)NX + 0.5f);
            if (NY < 1) NY = 1;
            int nbins = NX * NY;

            LOGI("Uniformize: %dx%d bins", NX, NY);

            int* bin_counts = calloc(nbins, sizeof(int));
            int* bin_assign = malloc(N * sizeof(int));

            for (int i = 0; i < N; i++) {
                int s = output_order[i];
                int ix = (int)((params.x[s] - xmin) / Wf * NX);
                int iy = (int)((params.y[s] - ymin) / Hf * NY);
                if (ix >= NX) ix = NX - 1;
                if (iy >= NY) iy = NY - 1;
                if (ix < 0) ix = 0;
                if (iy < 0) iy = 0;
                bin_assign[i] = iy * NX + ix;
                bin_counts[bin_assign[i]]++;
            }

            int maxlen = 0;
            for (int b = 0; b < nbins; b++)
                if (bin_counts[b] > maxlen) maxlen = bin_counts[b];

            int** bin_lists = malloc(nbins * sizeof(int*));
            int* bin_pos = calloc(nbins, sizeof(int));
            for (int b = 0; b < nbins; b++)
                bin_lists[b] = malloc(bin_counts[b] * sizeof(int));
            for (int i = 0; i < N; i++) {
                int b = bin_assign[i];
                bin_lists[b][bin_pos[b]++] = i;
            }

            int* uniform_order = malloc(N * sizeof(int));
            int u_idx = 0;
            int* thisrow = malloc(nbins * sizeof(int));
            for (int round = 0; round < maxlen; round++) {
                int rowlen = 0;
                for (int b = 0; b < nbins; b++) {
                    if (round < bin_counts[b])
                        thisrow[rowlen++] = bin_lists[b][round];
                }
                // Sort by resort index (preserves brightness ordering within round)
                for (int i = 1; i < rowlen; i++) {
                    int key = thisrow[i];
                    int j = i - 1;
                    while (j >= 0 && thisrow[j] > key) { thisrow[j+1] = thisrow[j]; j--; }
                    thisrow[j+1] = key;
                }
                for (int i = 0; i < rowlen; i++)
                    uniform_order[u_idx++] = output_order[thisrow[i]];
            }

            for (int i = 0; i < N; i++)
                output_order[i] = uniform_order[i];

            free(uniform_order);
            free(thisrow);
            for (int b = 0; b < nbins; b++) free(bin_lists[b]);
            free(bin_lists);
            free(bin_pos);
            free(bin_counts);
            free(bin_assign);
        }
    }

    // Create result array: [x0, y0, flux0, x1, y1, flux1, ...]
    jfloatArray resultArray = (*env)->NewFloatArray(env, N * 3);
    if (!resultArray) {
        free(perm1); free(perm2); free(rawsignal); free(used); free(output_order);
        simplexy_free_contents(&params);
        return NULL;
    }

    jfloat* buffer = malloc(N * 3 * sizeof(jfloat));
    for (int i = 0; i < N; i++) {
        int src = output_order[i];
        buffer[i * 3] = params.x[src];
        buffer[i * 3 + 1] = params.y[src];
        buffer[i * 3 + 2] = params.flux[src];
    }

    (*env)->SetFloatArrayRegion(env, resultArray, 0, N * 3, buffer);

    free(buffer);
    free(perm1);
    free(perm2);
    free(rawsignal);
    free(used);
    free(output_order);
    simplexy_free_contents(&params);

    return resultArray;
}

/*
 * Plate solver JNI
 *
 * solveFieldNative takes detected stars and index file paths, returns WCS result.
 * Result array format: [solved (0/1), ra, dec, crpixX, crpixY, cd11, cd12, cd21, cd22, pixelScale, rotation, logOdds]
 *
 * This implements depth iteration like solve-field does:
 * - Tries stars 1-10, then 11-20, then 21-30, etc.
 * - Stops when solution is found
 * - solve-field found solution at "field objects 21-30" for test image
 */
JNIEXPORT jdoubleArray JNICALL
Java_com_astro_app_native_1_AstrometryNative_solveFieldNative(
    JNIEnv *env,
    jclass clazz,
    jfloatArray starXY,          // [x0, y0, flux0, x1, y1, flux1, ...]
    jint numStars,
    jint imageWidth,
    jint imageHeight,
    jobjectArray indexPaths,
    jdouble scaleLow,            // arcsec/pixel
    jdouble scaleHigh,           // arcsec/pixel
    jdouble logOddsThreshold
) {
    LOGI("solveFieldNative: %d stars, image %dx%d, scale %.1f-%.1f",
         numStars, imageWidth, imageHeight, scaleLow, scaleHigh);

    // Get star data
    jfloat* stars = (*env)->GetFloatArrayElements(env, starXY, NULL);
    if (!stars) {
        LOGE("Failed to get star data");
        return NULL;
    }

    // Create solver
    solver_t* solver = solver_new();
    if (!solver) {
        LOGE("Failed to create solver");
        (*env)->ReleaseFloatArrayElements(env, starXY, stars, JNI_ABORT);
        return NULL;
    }

    // Create field with stars
    starxy_t* field = starxy_new(numStars, TRUE, FALSE);
    for (int i = 0; i < numStars; i++) {
        starxy_set(field, i, stars[i * 3], stars[i * 3 + 1]);
        starxy_set_flux(field, i, stars[i * 3 + 2]);
    }
    (*env)->ReleaseFloatArrayElements(env, starXY, stars, JNI_ABORT);

    // Stars arrive pre-sorted from detectStarsNative: resort + uniformize.
    // Do NOT re-sort here. The ordering ensures bright stars are spatially
    // distributed across the field for effective quad formation.

    // Configure solver
    solver->funits_lower = scaleLow;
    solver->funits_upper = scaleHigh;
    solver_set_quad_size_fraction(solver, 0.1, 1.0);
    solver_set_field_bounds(solver, 0, imageWidth, 0, imageHeight);
    solver_set_field(solver, field);

    solver->maxquads = 0;      // No limit - let solver try all combinations
    solver->maxmatches = 0;    // No limit
    solver->verify_pix = 1.0;  // Match solve-field default (DEFAULT_VERIFY_PIX)
    solver->distractor_ratio = 0.25;
    solver->codetol = 0.01;
    solver->parity = PARITY_BOTH;
    solver->logratio_tokeep = logOddsThreshold;
    solver->logratio_totune = log(1e6);  // ~13.8, same as solve-field
    solver->do_tweak = TRUE;             // Enable WCS refinement like solve-field
    solver->distance_from_quad_bonus = TRUE;  // Explicit (default, but for clarity)
    solver->tweak_aborder = 2;           // Match solve-field default
    solver->tweak_abporder = 2;          // Match solve-field default

    // Load index files
    int numIndexes = (*env)->GetArrayLength(env, indexPaths);
    LOGI("Loading %d index files...", numIndexes);

    for (int i = 0; i < numIndexes; i++) {
        jstring jpath = (jstring)(*env)->GetObjectArrayElement(env, indexPaths, i);
        const char* path = (*env)->GetStringUTFChars(env, jpath, NULL);

        index_t* idx = index_load(path, 0, NULL);
        if (idx) {
            solver_add_index(solver, idx);
            LOGI("Loaded index: %s", path);
        } else {
            LOGE("Failed to load index: %s", path);
        }

        (*env)->ReleaseStringUTFChars(env, jpath, path);
    }

    // Depth iteration - same as solve-field default depths
    // "10 20 30 40 50 60 70 80 90 100 110 120 130 140 150 160 170 180 190 200"
    // This means: try stars 1-10, then 11-20, then 21-30, etc.
    int depths[] = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100,
                    110, 120, 130, 140, 150, 160, 170, 180, 190, 200};
    int num_depths = sizeof(depths) / sizeof(depths[0]);

    LOGI("Running solver with depth iteration (like solve-field)...");

    int solved = 0;
    int lasthi = 0;

    for (int d = 0; d < num_depths && !solved; d++) {
        int startobj = lasthi;           // 0-indexed start
        int endobj = depths[d];          // 1-indexed end (exclusive in solver)
        lasthi = depths[d];

        // Don't try depths beyond our star count
        if (startobj >= numStars) {
            LOGI("Depth %d-%d: skipping (only have %d stars)", startobj + 1, endobj, numStars);
            break;
        }

        // Clamp endobj to actual star count
        if (endobj > numStars) {
            endobj = numStars;
        }

        LOGI("Trying depth: field objects %d-%d", startobj + 1, endobj);

        // Set the depth range
        solver->startobj = startobj;
        solver->endobj = endobj;

        // Reset and run solver for this depth
        solver_reset_counters(solver);
        solver_reset_best_match(solver);
        solver_run(solver);

        if (solver_did_solve(solver)) {
            solved = 1;
            LOGI("SOLVED at depth %d-%d!", startobj + 1, endobj);
        }
    }

    // Create result array
    jdoubleArray resultArray = (*env)->NewDoubleArray(env, 12);
    jdouble result[12] = {0};

    if (solved) {
        MatchObj* mo = solver_get_best_match(solver);
        tan_t* tan = &mo->wcstan;

        double pixscale = tan_pixel_scale(tan);
        double rotation = atan2(tan->cd[0][1], tan->cd[0][0]) * 180.0 / M_PI;

        result[0] = 1.0;  // solved
        result[1] = tan->crval[0];  // RA
        result[2] = tan->crval[1];  // Dec
        result[3] = tan->crpix[0];  // crpix X
        result[4] = tan->crpix[1];  // crpix Y
        result[5] = tan->cd[0][0];  // CD matrix
        result[6] = tan->cd[0][1];
        result[7] = tan->cd[1][0];
        result[8] = tan->cd[1][1];
        result[9] = pixscale;
        result[10] = rotation;
        result[11] = mo->logodds;

        LOGI("SOLVED! RA=%.4f, Dec=%.4f, scale=%.2f arcsec/pix, rotation=%.1f deg",
             tan->crval[0], tan->crval[1], pixscale, rotation);
    } else {
        LOGI("NOT SOLVED after all depths");
    }

    (*env)->SetDoubleArrayRegion(env, resultArray, 0, 12, result);

    // Cleanup
    solver_free(solver);

    return resultArray;
}
