package com.example.data

import android.graphics.Camera
import android.graphics.Matrix
import com.example.model.CornerPinOffsets
import com.example.model.Layer3DTransform
import kotlin.math.max
import kotlin.math.tan

object LayerTransformMatrix {

    fun computeLocalEffectMatrix(
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets
    ): Matrix {
        val w = boxW.coerceAtLeast(1f)
        val h = boxH.coerceAtLeast(1f)
        val matrix = Matrix()

        // 1. Free 4-corner Corner Pin perspective homography
        if (!cornerPin.isIdentity()) {
            val src = floatArrayOf(
                0f, 0f,
                w, 0f,
                w, h,
                0f, h
            )
            val dst = floatArrayOf(
                cornerPin.tlDx * w, cornerPin.tlDy * h,
                w + cornerPin.trDx * w, cornerPin.trDy * h,
                w + cornerPin.brDx * w, h + cornerPin.brDy * h,
                cornerPin.blDx * w, h + cornerPin.blDy * h
            )
            matrix.setPolyToPoly(src, 0, dst, 0, 4)
        }

        // 2. X / Y Skew around local center
        if (transform3D.skewXDeg != 0f || transform3D.skewYDeg != 0f) {
            val kx = tan(Math.toRadians(transform3D.skewXDeg.coerceIn(-75f, 75f).toDouble())).toFloat()
            val ky = tan(Math.toRadians(transform3D.skewYDeg.coerceIn(-75f, 75f).toDouble())).toFloat()
            matrix.postTranslate(-w / 2f, -h / 2f)
            matrix.postSkew(kx, ky)
            matrix.postTranslate(w / 2f, h / 2f)
        }

        // 3. 3D Perspective (Z depth + X/Y rotation) normalized to box size so preview == export
        if (transform3D.positionZ != 0f ||
            transform3D.rotationXDeg != 0f ||
            transform3D.rotationYDeg != 0f
        ) {
            val maxDim = max(w, h)
            val camDistPx = max(maxDim * 2.4f, 576f)
            val camZInches = -(camDistPx / 72f)
            val camera = Camera()
            camera.setLocation(0f, 0f, camZInches)

            val zShiftPx = -(transform3D.positionZ * (maxDim / 380f))
                .coerceIn(-camDistPx * 0.72f, camDistPx * 2.5f)
            camera.translate(0f, 0f, zShiftPx)
            if (transform3D.rotationXDeg != 0f) {
                camera.rotateX(transform3D.rotationXDeg)
            }
            if (transform3D.rotationYDeg != 0f) {
                camera.rotateY(transform3D.rotationYDeg)
            }

            val camMatrix = Matrix()
            camera.getMatrix(camMatrix)
            camMatrix.preTranslate(-w / 2f, -h / 2f)
            camMatrix.postTranslate(w / 2f, h / 2f)
            matrix.postConcat(camMatrix)
        }

        return matrix
    }

    fun buildBitmapDrawMatrix(
        bitmapWidth: Int,
        bitmapHeight: Int,
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets
    ): Matrix {
        val effect = computeLocalEffectMatrix(boxW, boxH, transform3D, cornerPin)
        val sx = boxW / bitmapWidth.coerceAtLeast(1).toFloat()
        val sy = boxH / bitmapHeight.coerceAtLeast(1).toFloat()
        effect.preScale(sx, sy)
        return effect
    }

    fun mapCornerPoints(
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets
    ): FloatArray {
        val w = boxW.coerceAtLeast(1f)
        val h = boxH.coerceAtLeast(1f)
        val pts = floatArrayOf(
            0f, 0f,
            w, 0f,
            w, h,
            0f, h
        )
        val m = computeLocalEffectMatrix(w, h, transform3D, cornerPin)
        m.mapPoints(pts)
        return pts
    }

    /**
     * Produces a lightweight soft blurred copy of a bitmap for the active Corner Pin background ghost.
     */
    fun createFastBlurPreview(source: android.graphics.Bitmap): android.graphics.Bitmap {
        val maxDim = 120
        val scale = minOf(1f, maxDim.toFloat() / maxOf(source.width, source.height).coerceAtLeast(1))
        val sw = (source.width * scale).toInt().coerceIn(8, maxDim)
        val sh = (source.height * scale).toInt().coerceIn(8, maxDim)
        val tiny = android.graphics.Bitmap.createScaledBitmap(source, sw, sh, true)
        val w = tiny.width
        val h = tiny.height
        val srcPx = IntArray(w * h)
        val dstPx = IntArray(w * h)
        tiny.getPixels(srcPx, 0, w, 0, 0, w, h)
        val radius = 3
        val div = radius * 2 + 1
        // Horizontal + vertical box blur passes for smooth Gaussian-like softness
        repeat(2) {
            for (y in 0 until h) {
                val row = y * w
                for (x in 0 until w) {
                    var aSum = 0; var rSum = 0; var gSum = 0; var bSum = 0
                    for (k in -radius..radius) {
                        val cx = (x + k).coerceIn(0, w - 1)
                        val p = srcPx[row + cx]
                        aSum += (p ushr 24) and 0xFF
                        rSum += (p ushr 16) and 0xFF
                        gSum += (p ushr 8) and 0xFF
                        bSum += p and 0xFF
                    }
                    dstPx[row + x] = ((aSum / div) shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                }
            }
            for (x in 0 until w) {
                for (y in 0 until h) {
                    var aSum = 0; var rSum = 0; var gSum = 0; var bSum = 0
                    for (k in -radius..radius) {
                        val cy = (y + k).coerceIn(0, h - 1)
                        val p = dstPx[cy * w + x]
                        aSum += (p ushr 24) and 0xFF
                        rSum += (p ushr 16) and 0xFF
                        gSum += (p ushr 8) and 0xFF
                        bSum += p and 0xFF
                    }
                    srcPx[y * w + x] = ((aSum / div) shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                }
            }
        }
        tiny.setPixels(srcPx, 0, w, 0, 0, w, h)
        return tiny
    }
}
