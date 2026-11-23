package com.example.data

import android.graphics.Bitmap
import android.graphics.BlendMode as AndroidBlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import com.example.model.CornerPinOffsets
import com.example.model.ImageCropRect
import com.example.model.ImageCropShape
import com.example.model.Layer3DTransform
import com.example.model.LayerBlendMode
import com.example.model.LayerEffectStyle
import com.example.model.LayerFillStyle
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shared rendering helpers for Layer Blend Modes, Fill Color/Media (SRC_ATOP),
 * outer Stroke & Drop Shadow, Puppet Mesh deformation (drawBitmapMesh),
 * and Universal Live-State Crop clipping.
 * Ensures live preview canvas and exported PNG/JPG output match identically.
 */
object LayerEffectsHelper {

    data class EffectBitmapResult(
        val bitmap: Bitmap,
        val padPx: Float,
        val recycledSource: Boolean
    )

    /**
     * Scans opaque pixels (alpha > 8) inside [cropNorm] + [cropShape] of [bmp] on a bounded sample grid
     * and returns the tight normalized bounding rect inside [0f..1f]. Falls back cleanly to full 0..1
     * when the bitmap is uniform/fully transparent.
     */
    fun computeTightOpaqueBoundsNorm(
        bmp: Bitmap,
        cropNorm: ImageCropRect = ImageCropRect(),
        cropShape: ImageCropShape = ImageCropShape.RECTANGLE
    ): RectF {
        if (bmp.isRecycled || bmp.width <= 0 || bmp.height <= 0) return RectF(0f, 0f, 1f, 1f)
        val norm = cropNorm.normalized()
        val srcL = (norm.left * bmp.width).roundToInt().coerceIn(0, bmp.width - 1)
        val srcT = (norm.top * bmp.height).roundToInt().coerceIn(0, bmp.height - 1)
        val srcR = (norm.right * bmp.width).roundToInt().coerceIn(srcL + 1, bmp.width)
        val srcB = (norm.bottom * bmp.height).roundToInt().coerceIn(srcT + 1, bmp.height)
        val cw = (srcR - srcL).coerceAtLeast(1)
        val ch = (srcB - srcT).coerceAtLeast(1)

        val grid = 96
        val stepX = max(1, cw / grid)
        val stepY = max(1, ch / grid)
        var minNx = 1f
        var minNy = 1f
        var maxNx = 0f
        var maxNy = 0f
        var found = false

        var y = srcT
        while (y < srcB) {
            val ny = (y - srcT).toFloat() / ch.toFloat()
            var x = srcL
            while (x < srcR) {
                val nx = (x - srcL).toFloat() / cw.toFloat()
                val insideShape = if (cropShape == ImageCropShape.CIRCLE) {
                    val dx = nx - 0.5f
                    val dy = ny - 0.5f
                    (dx * dx + dy * dy) <= 0.255f
                } else true
                if (insideShape && android.graphics.Color.alpha(bmp.getPixel(x, y)) > 8) {
                    found = true
                    if (nx < minNx) minNx = nx
                    if (nx > maxNx) maxNx = nx
                    if (ny < minNy) minNy = ny
                    if (ny > maxNy) maxNy = ny
                }
                x += stepX
            }
            y += stepY
        }
        if (!found) return RectF(0f, 0f, 1f, 1f)
        val padX = (stepX.toFloat() / cw.toFloat()).coerceAtLeast(1f / grid.toFloat())
        val padY = (stepY.toFloat() / ch.toFloat()).coerceAtLeast(1f / grid.toFloat())
        val l = (minNx - padX).coerceIn(0f, 0.96f)
        val t = (minNy - padY).coerceIn(0f, 0.96f)
        val r = (maxNx + padX).coerceIn(l + 0.04f, 1f)
        val b = (maxNy + padY).coerceIn(t + 0.04f, 1f)
        return RectF(l, t, r, b)
    }

    fun toPorterDuffMode(blend: LayerBlendMode): PorterDuff.Mode {
        return when (blend) {
            LayerBlendMode.NORMAL -> PorterDuff.Mode.SRC_OVER
            LayerBlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
            LayerBlendMode.SCREEN -> PorterDuff.Mode.SCREEN
            LayerBlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
            LayerBlendMode.DARKEN -> PorterDuff.Mode.DARKEN
            LayerBlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
            LayerBlendMode.ADD -> PorterDuff.Mode.ADD
            LayerBlendMode.DIFFERENCE -> PorterDuff.Mode.XOR
        }
    }

    fun applyBlendModeToPaint(paint: Paint, blend: LayerBlendMode, alphaInt: Int = 255): Paint {
        paint.alpha = alphaInt.coerceIn(0, 255)
        if (blend == LayerBlendMode.NORMAL) {
            paint.xfermode = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                paint.blendMode = AndroidBlendMode.SRC_OVER
            }
            return paint
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.blendMode = when (blend) {
                LayerBlendMode.NORMAL -> AndroidBlendMode.SRC_OVER
                LayerBlendMode.MULTIPLY -> AndroidBlendMode.MULTIPLY
                LayerBlendMode.SCREEN -> AndroidBlendMode.SCREEN
                LayerBlendMode.OVERLAY -> AndroidBlendMode.OVERLAY
                LayerBlendMode.DARKEN -> AndroidBlendMode.DARKEN
                LayerBlendMode.LIGHTEN -> AndroidBlendMode.LIGHTEN
                LayerBlendMode.ADD -> AndroidBlendMode.PLUS
                LayerBlendMode.DIFFERENCE -> AndroidBlendMode.DIFFERENCE
            }
        } else {
            paint.xfermode = PorterDuffXfermode(toPorterDuffMode(blend))
        }
        return paint
    }

    /**
     * Composites [sourceBmp] with [fillStyle], optional [fillMediaBmp], and outer [effectStyle]
     * (when [applyOuterStrokeAndShadow] is true — set to false for TextLayer when crisp vector stroke/shadow
     * was already drawn by FontRenderEngine).
     * Returns an ARGB_8888 bitmap plus uniform pixel padding around its nominal rect.
     */
    fun applyFillAndOuterEffects(
        sourceBmp: Bitmap,
        fillStyle: LayerFillStyle,
        fillMediaBmp: Bitmap?,
        effectStyle: LayerEffectStyle,
        applyOuterStrokeAndShadow: Boolean,
        effectScale: Float = 1f
    ): EffectBitmapResult {
        val hasFill = fillStyle.hasFill()
        val hasStroke = applyOuterStrokeAndShadow && effectStyle.strokeWidth > 0.05f
        val hasShadow = applyOuterStrokeAndShadow && effectStyle.shadowEnabled

        if (!hasFill && !hasStroke && !hasShadow) {
            return EffectBitmapResult(sourceBmp, padPx = 0f, recycledSource = false)
        }

        val w = sourceBmp.width.coerceAtLeast(1)
        val h = sourceBmp.height.coerceAtLeast(1)
        val strokePx = if (hasStroke) (effectStyle.strokeWidth * effectScale).coerceIn(1f, 80f) else 0f
        val shadowRadPx = if (hasShadow) (effectStyle.shadowRadius * effectScale).coerceIn(1f, 90f) else 0f
        val pad = ceil(max(strokePx * 1.35f, shadowRadPx * 1.5f + 6f)).coerceIn(0f, 140f)
        val padInt = pad.roundToInt()

        val outW = (w + padInt * 2).coerceIn(1, 2400)
        val outH = (h + padInt * 2).coerceIn(1, 2400)
        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val nomLeft = padInt.toFloat()
        val nomTop = padInt.toFloat()

        // 1. Optional Filled Interior Bitmap (preserving source alpha)
        val filledSrc: Bitmap = if (hasFill) {
            val tmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val fc = Canvas(tmp)
            val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
            val sc = fc.saveLayer(rect, null)
            fc.drawBitmap(sourceBmp, 0f, 0f, null)

            fillStyle.fillColorArgb?.let { colArgb ->
                val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colArgb.toInt()
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                }
                fc.drawRect(rect, cp)
            }

            if (fillMediaBmp != null && !fillMediaBmp.isRecycled) {
                val mediaPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                }
                val mw = fillMediaBmp.width.coerceAtLeast(1).toFloat()
                val mh = fillMediaBmp.height.coerceAtLeast(1).toFloat()
                val coverScale = max(w / mw, h / mh)
                val m = Matrix().apply {
                    postTranslate(-mw * 0.5f, -mh * 0.5f)
                    postScale(coverScale * fillStyle.fillMediaScale, coverScale * fillStyle.fillMediaScale)
                    postRotate(fillStyle.fillMediaRotationDeg)
                    postTranslate(
                        w * (0.5f + fillStyle.fillMediaOffsetX),
                        h * (0.5f + fillStyle.fillMediaOffsetY)
                    )
                }
                fc.drawBitmap(fillMediaBmp, m, mediaPaint)
            }
            fc.restoreToCount(sc)
            tmp
        } else {
            sourceBmp
        }

        val alphaMask = if (hasStroke || hasShadow) filledSrc.extractAlpha() else null

        // 2. Drop Shadow behind contour
        if (hasShadow && alphaMask != null) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                color = effectStyle.shadowColorArgb.toInt()
                maskFilter = BlurMaskFilter(shadowRadPx, BlurMaskFilter.Blur.NORMAL)
            }
            val dx = 2f * effectScale
            val dy = 4f * effectScale
            canvas.drawBitmap(alphaMask, nomLeft + dx, nomTop + dy, shadowPaint)
        }

        // 3. Outer Stroke via radially stepped alpha-tinted stamps
        if (hasStroke && alphaMask != null) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = PorterDuffColorFilter(effectStyle.strokeColorArgb.toInt(), PorterDuff.Mode.SRC_IN)
            }
            val steps = 12
            for (i in 0 until steps) {
                val angle = (2.0 * Math.PI * i) / steps
                val ox = (Math.cos(angle) * strokePx).toFloat()
                val oy = (Math.sin(angle) * strokePx).toFloat()
                canvas.drawBitmap(alphaMask, nomLeft + ox, nomTop + oy, strokePaint)
            }
            if (strokePx > 4f) {
                val innerR = strokePx * 0.55f
                for (i in 0 until 8) {
                    val angle = (2.0 * Math.PI * i) / 8.0
                    val ox = (Math.cos(angle) * innerR).toFloat()
                    val oy = (Math.sin(angle) * innerR).toFloat()
                    canvas.drawBitmap(alphaMask, nomLeft + ox, nomTop + oy, strokePaint)
                }
            }
        }

        alphaMask?.recycle()

        // 4. Foreground filled content
        canvas.drawBitmap(filledSrc, nomLeft, nomTop, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        if (filledSrc !== sourceBmp) {
            filledSrc.recycle()
        }
        return EffectBitmapResult(result, padPx = padInt.toFloat(), recycledSource = false)
    }

    /**
     * Draws [bmp] mapped into [boxW]x[boxH] (with extra margin [padRatioX]/[padRatioY] >= 1f)
     * applying both 4-corner CornerPin/3D perspective and optional smooth Puppet Deformation Mesh.
     */
    fun drawBitmapWithEffectsAndWarp(
        canvas: Canvas,
        bmp: Bitmap,
        boxW: Float,
        boxH: Float,
        transform3D: Layer3DTransform,
        cornerPin: CornerPinOffsets,
        blendMode: LayerBlendMode,
        alphaInt: Int,
        padRatioX: Float = 1f,
        padRatioY: Float = 1f
    ) {
        val paint = applyBlendModeToPaint(
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            blendMode,
            alphaInt
        )
        if (cornerPin.puppetPins.isNotEmpty()) {
            val cols = 14
            val rows = 14
            val verts = LayerTransformMatrix.buildWarpedMeshVerts(
                cols = cols,
                rows = rows,
                boxW = boxW,
                boxH = boxH,
                transform3D = transform3D,
                cornerPin = cornerPin,
                padRatioX = padRatioX,
                padRatioY = padRatioY
            )
            canvas.drawBitmapMesh(bmp, cols, rows, verts, 0, null, 0, paint)
        } else if (padRatioX > 1.001f || padRatioY > 1.001f) {
            val m = LayerTransformMatrix.buildPaddedBitmapDrawMatrix(
                bitmapWidth = bmp.width,
                bitmapHeight = bmp.height,
                padRatioX = padRatioX,
                padRatioY = padRatioY,
                boxW = boxW,
                boxH = boxH,
                transform3D = transform3D,
                cornerPin = cornerPin
            )
            canvas.drawBitmap(bmp, m, paint)
        } else {
            val m = LayerTransformMatrix.buildBitmapDrawMatrix(
                bitmapWidth = bmp.width,
                bitmapHeight = bmp.height,
                boxW = boxW,
                boxH = boxH,
                transform3D = transform3D,
                cornerPin = cornerPin
            )
            canvas.drawBitmap(bmp, m, paint)
        }
    }

    fun clipLiveCropRect(
        canvas: Canvas,
        liveCrop: ImageCropRect,
        bounds: LayerTransformMatrix.LocalBounds
    ): Boolean {
        if (liveCrop.isFull()) return false
        val norm = liveCrop.normalized()
        val l = bounds.minX + norm.left * bounds.width
        val t = bounds.minY + norm.top * bounds.height
        val r = bounds.minX + norm.right * bounds.width
        val b = bounds.minY + norm.bottom * bounds.height
        canvas.clipRect(l, t, r, b)
        return true
    }
}
