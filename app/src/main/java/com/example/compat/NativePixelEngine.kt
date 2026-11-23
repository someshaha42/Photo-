package com.example.compat

import android.graphics.Bitmap
import android.os.Build

/**
 * JNI bridge to C/C++ pixel processing library (`libpixelforge_native.so`) for
 * ARMv8 (`arm64-v8a`), ARMv7 (`armeabi-v7a`), `x86_64`, and `x86`, with automatic
 * Java/JVM fallback when running without an NDK `.so` (e.g. unit tests / Robolectric).
 */
object NativePixelEngine {

    val isNativeLoaded: Boolean by lazy {
        try {
            System.loadLibrary("pixelforge_native")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private external fun nativeGetAbiName(): String
    private external fun nativeAdjustPixels(
        pixels: IntArray,
        exposure: Float,
        brightness: Float,
        contrast: Float,
        saturation: Float
    )

    private external fun nativeSharpenPixels(
        srcPixels: IntArray,
        dstPixels: IntArray,
        width: Int,
        height: Int,
        amount: Float
    )

    private external fun nativeScanOpaqueBounds(
        pixels: IntArray,
        width: Int,
        height: Int,
        alphaThreshold: Int,
        outBounds4: IntArray
    ): Boolean

    fun activeAbi(): String {
        if (isNativeLoaded) {
            val abi = runCatching { nativeGetAbiName() }.getOrNull()
            if (!abi.isNullOrBlank()) return abi
        }
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "jvm-compat"
    }

    fun backendSummary(): String =
        if (isNativeLoaded) "C++ NDK (${activeAbi()})"
        else "Java Compat (${activeAbi()})"

    fun adjustPixels(
        pixels: IntArray,
        exposure: Float,
        brightness: Float,
        contrast: Float,
        saturation: Float
    ) {
        if (pixels.isEmpty()) return
        if (isNativeLoaded) {
            nativeAdjustPixels(pixels, exposure, brightness, contrast, saturation)
        } else {
            LegacyCompatBridge.adjustPixelsFallback(pixels, exposure, brightness, contrast, saturation)
        }
    }

    fun sharpenBitmap(bitmap: Bitmap, amount: Float): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 3 || h < 3 || amount <= 0.01f) return bitmap

        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)

        if (isNativeLoaded) {
            nativeSharpenPixels(src, dst, w, h, amount)
        } else {
            LegacyCompatBridge.sharpenPixelsFallback(src, dst, w, h, amount)
        }
        return Bitmap.createBitmap(dst, w, h, Bitmap.Config.ARGB_8888)
    }

    fun scanOpaqueBounds(
        pixels: IntArray,
        width: Int,
        height: Int,
        alphaThreshold: Int = 0
    ): IntArray? {
        val out = IntArray(4)
        val ok = if (isNativeLoaded) {
            nativeScanOpaqueBounds(pixels, width, height, alphaThreshold, out)
        } else {
            LegacyCompatBridge.scanOpaqueBoundsFallback(pixels, width, height, alphaThreshold, out)
        }
        return if (ok) out else null
    }
}
