#include "pixelforge_native.h"

#include <jni.h>
#include <math.h>
#include <stddef.h>
#include <string.h>

namespace {

inline int32_t clamp255(float value) {
    if (value <= 0.0f) return 0;
    if (value >= 255.0f) return 255;
    return static_cast<int32_t>(value + 0.5f);
}

inline float clampf(float value, float lo, float hi) {
    if (value < lo) return lo;
    if (value > hi) return hi;
    return value;
}

} // namespace

extern "C" {

const char* pf_native_abi_name(void) {
#if defined(__aarch64__) || defined(PIXELFORGE_ABI_ARM64)
    return "arm64-v8a";
#elif defined(__arm__) || defined(PIXELFORGE_ABI_ARM32)
    return "armeabi-v7a";
#elif defined(__x86_64__) || defined(PIXELFORGE_ABI_X86_64)
    return "x86_64";
#elif defined(__i386__) || defined(PIXELFORGE_ABI_X86)
    return "x86";
#else
    return "generic";
#endif
}

void pf_adjust_argb8888(
    uint32_t* pixels,
    int32_t count,
    float exposure,
    float brightness,
    float contrast,
    float saturation
) {
    if (pixels == nullptr || count <= 0) {
        return;
    }

    const float expFactor = powf(2.0f, clampf(exposure, -2.0f, 2.0f));
    const float cFactor = 1.0f + clampf(contrast, -0.85f, 1.5f);
    const float satFactor = clampf(1.0f + saturation, 0.0f, 2.5f);
    const float bias = 128.0f * (1.0f - cFactor) + (brightness * 90.0f);

    for (int32_t i = 0; i < count; ++i) {
        const uint32_t px = pixels[i];
        const uint32_t a = (px >> 24U) & 0xFFU;
        if (a == 0U) {
            continue;
        }
        float r = static_cast<float>((px >> 16U) & 0xFFU) * expFactor * cFactor + bias;
        float g = static_cast<float>((px >> 8U) & 0xFFU) * expFactor * cFactor + bias;
        float b = static_cast<float>(px & 0xFFU) * expFactor * cFactor + bias;

        if (fabsf(satFactor - 1.0f) > 0.001f) {
            const float luma = 0.2126f * r + 0.7152f * g + 0.0722f * b;
            r = luma + (r - luma) * satFactor;
            g = luma + (g - luma) * satFactor;
            b = luma + (b - luma) * satFactor;
        }

        const uint32_t nr = static_cast<uint32_t>(clamp255(r));
        const uint32_t ng = static_cast<uint32_t>(clamp255(g));
        const uint32_t nb = static_cast<uint32_t>(clamp255(b));
        pixels[i] = (a << 24U) | (nr << 16U) | (ng << 8U) | nb;
    }
}

void pf_sharpen_argb8888(
    const uint32_t* src,
    uint32_t* dst,
    int32_t width,
    int32_t height,
    float amount
) {
    if (src == nullptr || dst == nullptr || width <= 0 || height <= 0) {
        return;
    }

    const size_t total = static_cast<size_t>(width) * static_cast<size_t>(height);
    memcpy(dst, src, total * sizeof(uint32_t));
    if (width < 3 || height < 3 || amount <= 0.001f) {
        return;
    }

    const float clampedAmount = clampf(amount, 0.0f, 3.0f);
    for (int32_t y = 1; y < height - 1; ++y) {
        const int32_t row = y * width;
        for (int32_t x = 1; x < width - 1; ++x) {
            const uint32_t c = src[row + x];
            const uint32_t a = (c >> 24U) & 0xFFU;
            const uint32_t t = src[row - width + x];
            const uint32_t b = src[row + width + x];
            const uint32_t l = src[row + x - 1];
            const uint32_t r = src[row + x + 1];

            const float cr = static_cast<float>((c >> 16U) & 0xFFU);
            const float cg = static_cast<float>((c >> 8U) & 0xFFU);
            const float cb = static_cast<float>(c & 0xFFU);

            const float avgR = 0.25f * static_cast<float>(
                ((t >> 16U) & 0xFFU) + ((b >> 16U) & 0xFFU) + ((l >> 16U) & 0xFFU) + ((r >> 16U) & 0xFFU)
            );
            const float avgG = 0.25f * static_cast<float>(
                ((t >> 8U) & 0xFFU) + ((b >> 8U) & 0xFFU) + ((l >> 8U) & 0xFFU) + ((r >> 8U) & 0xFFU)
            );
            const float avgB = 0.25f * static_cast<float>(
                (t & 0xFFU) + (b & 0xFFU) + (l & 0xFFU) + (r & 0xFFU)
            );

            const uint32_t nr = static_cast<uint32_t>(clamp255(cr + (cr - avgR) * clampedAmount));
            const uint32_t ng = static_cast<uint32_t>(clamp255(cg + (cg - avgG) * clampedAmount));
            const uint32_t nb = static_cast<uint32_t>(clamp255(cb + (cb - avgB) * clampedAmount));
            dst[row + x] = (a << 24U) | (nr << 16U) | (ng << 8U) | nb;
        }
    }
}

int32_t pf_scan_opaque_bounds(
    const uint32_t* pixels,
    int32_t width,
    int32_t height,
    int32_t alphaThreshold,
    int32_t* outBounds4
) {
    if (pixels == nullptr || outBounds4 == nullptr || width <= 0 || height <= 0) {
        return 0;
    }

    int32_t minX = width;
    int32_t minY = height;
    int32_t maxX = -1;
    int32_t maxY = -1;

    for (int32_t y = 0; y < height; ++y) {
        const int32_t row = y * width;
        for (int32_t x = 0; x < width; ++x) {
            const int32_t alpha = static_cast<int32_t>((pixels[row + x] >> 24U) & 0xFFU);
            if (alpha > alphaThreshold) {
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }
    }

    if (maxX < minX || maxY < minY) {
        return 0;
    }

    outBounds4[0] = minX;
    outBounds4[1] = minY;
    outBounds4[2] = maxX;
    outBounds4[3] = maxY;
    return 1;
}

JNIEXPORT jstring JNICALL
Java_com_example_compat_NativePixelEngine_nativeGetAbiName(
    JNIEnv* env,
    jobject /* thiz */
) {
    return env->NewStringUTF(pf_native_abi_name());
}

JNIEXPORT void JNICALL
Java_com_example_compat_NativePixelEngine_nativeAdjustPixels(
    JNIEnv* env,
    jobject /* thiz */,
    jintArray pixels,
    jfloat exposure,
    jfloat brightness,
    jfloat contrast,
    jfloat saturation
) {
    if (pixels == nullptr) return;
    const jsize count = env->GetArrayLength(pixels);
    if (count <= 0) return;

    jint* buffer = env->GetIntArrayElements(pixels, nullptr);
    if (buffer == nullptr) return;

    pf_adjust_argb8888(
        reinterpret_cast<uint32_t*>(buffer),
        static_cast<int32_t>(count),
        static_cast<float>(exposure),
        static_cast<float>(brightness),
        static_cast<float>(contrast),
        static_cast<float>(saturation)
    );

    env->ReleaseIntArrayElements(pixels, buffer, 0);
}

JNIEXPORT void JNICALL
Java_com_example_compat_NativePixelEngine_nativeSharpenPixels(
    JNIEnv* env,
    jobject /* thiz */,
    jintArray srcPixels,
    jintArray dstPixels,
    jint width,
    jint height,
    jfloat amount
) {
    if (srcPixels == nullptr || dstPixels == nullptr || width <= 0 || height <= 0) return;
    const jsize expected = width * height;
    if (env->GetArrayLength(srcPixels) < expected || env->GetArrayLength(dstPixels) < expected) {
        return;
    }

    jint* srcPtr = env->GetIntArrayElements(srcPixels, nullptr);
    jint* dstPtr = env->GetIntArrayElements(dstPixels, nullptr);
    if (srcPtr != nullptr && dstPtr != nullptr) {
        pf_sharpen_argb8888(
            reinterpret_cast<const uint32_t*>(srcPtr),
            reinterpret_cast<uint32_t*>(dstPtr),
            static_cast<int32_t>(width),
            static_cast<int32_t>(height),
            static_cast<float>(amount)
        );
    }

    if (srcPtr != nullptr) env->ReleaseIntArrayElements(srcPixels, srcPtr, JNI_ABORT);
    if (dstPtr != nullptr) env->ReleaseIntArrayElements(dstPixels, dstPtr, 0);
}

JNIEXPORT jboolean JNICALL
Java_com_example_compat_NativePixelEngine_nativeScanOpaqueBounds(
    JNIEnv* env,
    jobject /* thiz */,
    jintArray pixels,
    jint width,
    jint height,
    jint alphaThreshold,
    jintArray outBounds4
) {
    if (pixels == nullptr || outBounds4 == nullptr || env->GetArrayLength(outBounds4) < 4) {
        return JNI_FALSE;
    }

    jint* pxPtr = env->GetIntArrayElements(pixels, nullptr);
    jint bounds[4] = {0, 0, 0, 0};
    int32_t found = 0;

    if (pxPtr != nullptr) {
        found = pf_scan_opaque_bounds(
            reinterpret_cast<const uint32_t*>(pxPtr),
            static_cast<int32_t>(width),
            static_cast<int32_t>(height),
            static_cast<int32_t>(alphaThreshold),
            bounds
        );
        env->ReleaseIntArrayElements(pixels, pxPtr, JNI_ABORT);
    }

    if (found != 0) {
        env->SetIntArrayRegion(outBounds4, 0, 4, bounds);
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

} // extern "C"
