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
        if (cornerPin.hasCornerOffsets()) {
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

    /**
     * Smooth Inverse-Distance-Weighted / Gaussian puppet deformation field in normalized [0..1] object space.
     * Pinned points act as smooth attractors & anchors; stationary edge anchors keep unpinned corners softly grounded
     * when only 1-2 pins exist so the object bends organically like a rubber sheet.
     */
    fun applyPuppetDisplacementNorm(
        u: Float,
        v: Float,
        pins: List<com.example.model.PuppetPinPoint>
    ): Pair<Float, Float> {
        if (pins.isEmpty()) return u to v
        var sumW = 0.0
        var sumDx = 0.0
        var sumDy = 0.0
        val sigma2 = 0.095 // ~0.31 normalized falloff radius
        for (pin in pins) {
            val du = (u - pin.anchorX).toDouble()
            val dv = (v - pin.anchorY).toDouble()
            val d2 = du * du + dv * dv
            if (d2 < 1e-6) {
                return (u + pin.dx) to (v + pin.dy)
            }
            val w = kotlin.math.exp(-d2 / sigma2) / (d2 + 0.008)
            sumW += w
            sumDx += w * pin.dx
            sumDy += w * pin.dy
        }
        // Soft far-field damping so dragging a single pin bends locally around that pin rather than rigidly translating
        val damp = 0.35
        val totalW = sumW + damp
        return (u + (sumDx / totalW).toFloat()) to (v + (sumDy / totalW).toFloat())
    }

    fun buildWarpedMeshVerts(
        cols: Int,
        rows: Int,
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets,
        padRatioX: Float = 1f,
        padRatioY: Float = 1f
    ): FloatArray {
        val w = boxW.coerceAtLeast(1f)
        val h = boxH.coerceAtLeast(1f)
        val prX = padRatioX.coerceAtLeast(1f)
        val prY = padRatioY.coerceAtLeast(1f)
        val verts = FloatArray((cols + 1) * (rows + 1) * 2)
        val pins = cornerPin.puppetPins
        var idx = 0
        for (r in 0..rows) {
            val tv = r.toFloat() / rows.toFloat()
            val vNom = (tv - 0.5f) * prY + 0.5f
            for (c in 0..cols) {
                val tu = c.toFloat() / cols.toFloat()
                val uNom = (tu - 0.5f) * prX + 0.5f
                val (wu, wv) = applyPuppetDisplacementNorm(uNom, vNom, pins)
                verts[idx++] = wu * w
                verts[idx++] = wv * h
            }
        }
        val m = computeLocalEffectMatrix(w, h, transform3D, cornerPin)
        m.mapPoints(verts)
        return verts
    }

    fun mapPuppetPinToBoxPx(
        pin: com.example.model.PuppetPinPoint,
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets
    ): Pair<Float, Float> {
        val w = boxW.coerceAtLeast(1f)
        val h = boxH.coerceAtLeast(1f)
        val pts = floatArrayOf(
            (pin.anchorX + pin.dx) * w,
            (pin.anchorY + pin.dy) * h
        )
        computeLocalEffectMatrix(w, h, transform3D, cornerPin).mapPoints(pts)
        return pts[0] to pts[1]
    }

    fun mapBoxPointToLocalNorm(
        localX: Float,
        localY: Float,
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets
    ): Pair<Float, Float> {
        val w = boxW.coerceAtLeast(1f)
        val h = boxH.coerceAtLeast(1f)
        val effect = computeLocalEffectMatrix(w, h, transform3D, cornerPin)
        val inv = Matrix()
        val pts = floatArrayOf(localX, localY)
        if (effect.invert(inv)) {
            inv.mapPoints(pts)
        }
        return (pts[0] / w).coerceIn(0.02f, 0.98f) to (pts[1] / h).coerceIn(0.02f, 0.98f)
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

    /**
     * Maps an oversized padded offscreen bitmap (of nominal boxW * padRatioX by boxH * padRatioY,
     * centered on the nominal box) through the same Corner Pin & 3D effect matrix as buildBitmapDrawMatrix
     * so stretched vertices or child layers extending beyond nominal bounds are never clipped.
     */
    fun buildPaddedBitmapDrawMatrix(
        bitmapWidth: Int,
        bitmapHeight: Int,
        padRatioX: Float,
        padRatioY: Float,
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets
    ): Matrix {
        val w = boxW.coerceAtLeast(1f)
        val h = boxH.coerceAtLeast(1f)
        val prX = padRatioX.coerceAtLeast(1f)
        val prY = padRatioY.coerceAtLeast(1f)
        val effect = computeLocalEffectMatrix(w, h, transform3D, cornerPin)
        effect.preTranslate(-w * (prX - 1f) * 0.5f, -h * (prY - 1f) * 0.5f)
        val sx = (w * prX) / bitmapWidth.coerceAtLeast(1).toFloat()
        val sy = (h * prY) / bitmapHeight.coerceAtLeast(1).toFloat()
        effect.preScale(sx, sy)
        return effect
    }

    fun mapCornerPoints(
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets
    ): FloatArray {
        val quad = mapQuadForRect(
            left = 0f,
            top = 0f,
            right = boxW,
            bottom = boxH,
            boxW = boxW,
            boxH = boxH,
            transform3D = transform3D,
            cornerPin = cornerPin
        )
        if (cornerPin.puppetPins.isEmpty()) return quad
        val w = boxW.coerceAtLeast(1f)
        val h = boxH.coerceAtLeast(1f)
        val mesh = buildWarpedMeshVerts(8, 8, w, h, transform3D, cornerPin)
        var minX = quad[0]; var maxX = quad[0]
        var minY = quad[1]; var maxY = quad[1]
        for (i in 0 until mesh.size step 2) {
            if (mesh[i] < minX) minX = mesh[i]
            if (mesh[i] > maxX) maxX = mesh[i]
            if (mesh[i + 1] < minY) minY = mesh[i + 1]
            if (mesh[i + 1] > maxY) maxY = mesh[i + 1]
        }
        return quad
    }

    fun mapQuadForRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets
    ): FloatArray {
        val w = boxW.coerceAtLeast(1f)
        val h = boxH.coerceAtLeast(1f)
        val pts = floatArrayOf(
            left, top,
            right, top,
            right, bottom,
            left, bottom
        )
        val m = computeLocalEffectMatrix(w, h, transform3D, cornerPin)
        m.mapPoints(pts)
        return pts
    }

    fun mapPoints(
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets,
        pts: FloatArray
    ): FloatArray {
        val out = pts.copyOf()
        val m = computeLocalEffectMatrix(boxW.coerceAtLeast(1f), boxH.coerceAtLeast(1f), transform3D, cornerPin)
        m.mapPoints(out)
        return out
    }

    fun mapPath(
        source: android.graphics.Path,
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets
    ): android.graphics.Path {
        val out = android.graphics.Path(source)
        val m = computeLocalEffectMatrix(boxW.coerceAtLeast(1f), boxH.coerceAtLeast(1f), transform3D, cornerPin)
        out.transform(m)
        return out
    }

    data class LocalBounds(
        val minX: Float,
        val minY: Float,
        val maxX: Float,
        val maxY: Float
    ) {
        val width: Float get() = (maxX - minX).coerceAtLeast(1f)
        val height: Float get() = (maxY - minY).coerceAtLeast(1f)
    }

    fun computeQuadBoundingBox(
        quadPts: FloatArray,
        paddingPx: Float = 0f
    ): LocalBounds {
        var minX = quadPts[0]
        var maxX = quadPts[0]
        var minY = quadPts[1]
        var maxY = quadPts[1]
        for (i in 1 until 4) {
            val x = quadPts[i * 2]
            val y = quadPts[i * 2 + 1]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        return LocalBounds(
            minX = minX - paddingPx,
            minY = minY - paddingPx,
            maxX = maxX + paddingPx,
            maxY = maxY + paddingPx
        )
    }

    fun computeMeshBoundingBox(
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets,
        paddingPx: Float = 0f
    ): LocalBounds {
        if (cornerPin.puppetPins.isEmpty()) {
            val q = mapCornerPoints(boxW, boxH, transform3D, cornerPin)
            return computeQuadBoundingBox(q, paddingPx)
        }
        val mesh = buildWarpedMeshVerts(10, 10, boxW, boxH, transform3D, cornerPin)
        var minX = mesh[0]
        var maxX = mesh[0]
        var minY = mesh[1]
        var maxY = mesh[1]
        for (i in 2 until mesh.size step 2) {
            val x = mesh[i]
            val y = mesh[i + 1]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        return LocalBounds(
            minX = minX - paddingPx,
            minY = minY - paddingPx,
            maxX = maxX + paddingPx,
            maxY = maxY + paddingPx
        )
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
