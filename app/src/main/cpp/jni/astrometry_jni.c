#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>

#include "astrometry/simplexy.h"
#include "astrometry/image2xy.h"
#include "astrometry/log.h"

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
