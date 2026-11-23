#ifndef PIXELFORGE_NATIVE_H
#define PIXELFORGE_NATIVE_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Returns the compiled target CPU ABI identifier ("arm64-v8a", "armeabi-v7a", "x86_64", "x86", etc.).
 */
const char* pf_native_abi_name(void);

/**
 * Applies in-place exposure, brightness, contrast, and saturation to packed ARGB_8888 pixels.
 */
void pf_adjust_argb8888(
    uint32_t* pixels,
    int32_t count,
    float exposure,
    float brightness,
    float contrast,
    float saturation
);

/**
 * Applies a 3x3 unsharp-mask sharpen kernel from src into dst over ARGB_8888 buffers.
 */
void pf_sharpen_argb8888(
    const uint32_t* src,
    uint32_t* dst,
    int32_t width,
    int32_t height,
    float amount
);

/**
 * Scans packed ARGB_8888 pixels and writes tight bounding box [minX, minY, maxX, maxY]
 * of non-transparent pixels (alpha > threshold). Returns 1 if any opaque pixel exists, else 0.
 */
int32_t pf_scan_opaque_bounds(
    const uint32_t* pixels,
    int32_t width,
    int32_t height,
    int32_t alphaThreshold,
    int32_t* outBounds4
);

#ifdef __cplusplus
}
#endif

#endif // PIXELFORGE_NATIVE_H
