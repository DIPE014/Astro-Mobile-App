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
    log_init(LOG_VERB);
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

    // Set up simplexy parameters
    simplexy_t params;
    memset(&params, 0, sizeof(simplexy_t));

    params.image_u8 = (unsigned char*)pixels;
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

    LOGI("Running image2xy on %dx%d image, downsample=%d, plim=%.1f, dpsf=%.1f",
         width, height, downsample, plim, dpsf);

    // Run detection with downsampling
    int result = image2xy_run(&params, downsample, 0);

    (*env)->ReleaseByteArrayElements(env, imageData, pixels, JNI_ABORT);

    if (result != 0 || params.npeaks == 0) {
        LOGE("Star detection failed (result=%d) or no stars found (npeaks=%d)", result, params.npeaks);
        simplexy_free_contents(&params);
        return NULL;
    }

    LOGI("Detected %d stars", params.npeaks);

    // Create result array: [x0, y0, flux0, x1, y1, flux1, ...]
    jfloatArray resultArray = (*env)->NewFloatArray(env, params.npeaks * 3);
    if (!resultArray) {
        simplexy_free_contents(&params);
        return NULL;
    }

    jfloat* buffer = malloc(params.npeaks * 3 * sizeof(jfloat));
    for (int i = 0; i < params.npeaks; i++) {
        buffer[i * 3] = params.x[i];
        buffer[i * 3 + 1] = params.y[i];
        buffer[i * 3 + 2] = params.flux[i];
    }

    (*env)->SetFloatArrayRegion(env, resultArray, 0, params.npeaks * 3, buffer);

    free(buffer);
    simplexy_free_contents(&params);

    return resultArray;
}

/*
 * Plate solver JNI
 *
 * solveFieldNative takes detected stars and index file paths, returns WCS result.
 * Result array format: [solved (0/1), ra, dec, crpixX, crpixY, cd11, cd12, cd21, cd22, pixelScale, rotation, logOdds]
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

    // Sort by flux
    starxy_sort_by_flux(field);

    // Configure solver
    solver->funits_lower = scaleLow;
    solver->funits_upper = scaleHigh;
    solver_set_quad_size_fraction(solver, 0.1, 1.0);
    solver_set_field_bounds(solver, 0, imageWidth - 1, 0, imageHeight - 1);
    solver_set_field(solver, field);

    solver->maxquads = 10000;
    solver->maxmatches = 1000;
    solver->verify_pix = 1.0;
    solver->distractor_ratio = 0.25;
    solver->codetol = 0.01;
    solver->parity = PARITY_BOTH;
    solver->logratio_tokeep = logOddsThreshold;
    solver->logratio_totune = logOddsThreshold;

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

    // Run solver
    solver_reset_counters(solver);
    solver_reset_best_match(solver);

    LOGI("Running solver...");
    solver_run(solver);

    // Create result array
    jdoubleArray resultArray = (*env)->NewDoubleArray(env, 12);
    jdouble result[12] = {0};

    if (solver_did_solve(solver)) {
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
        LOGI("NOT SOLVED");
    }

    (*env)->SetDoubleArrayRegion(env, resultArray, 0, 12, result);

    // Cleanup
    solver_free(solver);

    return resultArray;
}
