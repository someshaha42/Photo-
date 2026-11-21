package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.fonts.FontRenderEngine
import com.example.model.BackgroundTab
import com.example.model.CanvasDimensions
import com.example.model.DrawLayer
import com.example.model.EditorBlendMode
import com.example.model.EditorLayer
import com.example.model.ExportImageFormat
import com.example.model.ExportResolutionPreset
import com.example.model.GroupLayer
import com.example.model.ImageAdjustments
import com.example.model.ImageCropShape
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

data class ImportedImageMeta(
    val uriString: String,
    val width: Int,
    val height: Int,
    val displayName: String
)

class MediaStorageManager(private val context: Context) {

    suspend fun importImage(
        sourceUri: Uri,
        projectId: String,
        keepLocalCopy: Boolean
    ): ImportedImageMeta = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(sourceUri) ?: "photo_${System.currentTimeMillis()}.png"
        val resolvedUriString = if (keepLocalCopy) {
            copyUriToAppStorage(sourceUri, projectId, displayName)
        } else {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            sourceUri.toString()
        }

        val (width, height) = decodeImageDimensions(resolvedUriString)
        ImportedImageMeta(
            uriString = resolvedUriString,
            width = width.coerceAtLeast(1),
            height = height.coerceAtLeast(1),
            displayName = displayName
        )
    }

    private fun copyUriToAppStorage(uri: Uri, projectId: String, fallbackName: String): String {
        val safeName = fallbackName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val dir = File(context.filesDir, "projects_media/$projectId")
        if (!dir.exists()) dir.mkdirs()
        val destFile = File(dir, "${UUID.randomUUID().toString().take(8)}_$safeName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output, bufferSize = 64 * 1024)
            }
        }
        return destFile.absolutePath
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
    }

    fun decodeImageDimensions(uriOrPath: String): Pair<Int, Int> {
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        openBitmapStream(uriOrPath, opts)
        return opts.outWidth to opts.outHeight
    }

    suspend fun decodeRgba8888Bitmap(
        uriOrPath: String,
        maxDimension: Int? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openBitmapStream(uriOrPath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        val sampleSize = if (maxDimension != null && maxDimension > 0) {
            var s = 1
            while ((bounds.outWidth / s) > maxDimension * 2 || (bounds.outHeight / s) > maxDimension * 2) {
                s *= 2
            }
            s
        } else 1

        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inPremultiplied = true
            inSampleSize = sampleSize
            inMutable = false
        }
        openBitmapStream(uriOrPath, opts)
    }

    private fun openBitmapStream(uriOrPath: String, opts: BitmapFactory.Options): Bitmap? {
        return runCatching {
            if (uriOrPath.startsWith("/")) {
                BitmapFactory.decodeFile(uriOrPath, opts)
            } else {
                context.contentResolver.openInputStream(Uri.parse(uriOrPath))?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                }
            }
        }.getOrNull()
    }

    suspend fun createBlurredBitmap(source: Bitmap, blurFraction: Float): Bitmap =
        withContext(Dispatchers.Default) {
            val clamped = blurFraction.coerceIn(0f, 1f)
            val radius = (clamped * 36f).roundToInt().coerceIn(0, 36)
            val working = source.copy(Bitmap.Config.ARGB_8888, true)
            if (radius <= 0) return@withContext working
            stackBlurInPlace(working, radius)
            working
        }

    /**
     * Renders the complete project into an RGBA8888 Bitmap WITHOUT the checkerboard preview layer.
     */
    suspend fun renderProjectToBitmap(
        document: ProjectDocument,
        fontRenderEngine: FontRenderEngine,
        targetWidth: Int = document.canvas.exportWidth,
        targetHeight: Int = document.canvas.exportHeight
    ): Bitmap = withContext(Dispatchers.IO) {
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val bg = document.background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = blendModeToPorterDuff(bg.blendMode)
        }

        when (bg.activeTab) {
            BackgroundTab.TRANSPARENT -> Unit
            BackgroundTab.COLORS -> {
                val bgColorInt = bg.colorArgb.toInt()
                val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = bgColorInt
                    alpha = ((android.graphics.Color.alpha(bgColorInt) / 255f) * bg.colorOpacity * 255f)
                        .roundToInt().coerceIn(0, 255)
                    xfermode = blendModeToPorterDuff(bg.blendMode)
                }
                canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), colorPaint)
            }
            BackgroundTab.BLUR -> {
                val firstImageUri = bg.backgroundImageUri
                    ?: findFirstImageUri(document.layers)
                if (firstImageUri != null) {
                    decodeRgba8888Bitmap(firstImageUri, maxDimension = 1280)?.let { src ->
                        val blurred = createBlurredBitmap(src, bg.blurStrength)
                        val coverMatrix = computeCoverMatrix(
                            blurred.width,
                            blurred.height,
                            targetWidth.toFloat(),
                            targetHeight.toFloat()
                        )
                        bgPaint.alpha = (bg.blurBackgroundOpacity * 255f).roundToInt().coerceIn(0, 255)
                        canvas.drawBitmap(blurred, coverMatrix, bgPaint)
                        if (blurred !== src) blurred.recycle()
                        src.recycle()
                    }
                }
            }
            BackgroundTab.IMAGE -> {
                val imgUri = bg.backgroundImageUri
                if (imgUri != null) {
                    decodeRgba8888Bitmap(imgUri, maxDimension = targetWidth.coerceAtLeast(targetHeight))?.let { src ->
                        val coverMatrix = computeCoverMatrix(
                            src.width,
                            src.height,
                            targetWidth.toFloat(),
                            targetHeight.toFloat()
                        )
                        bgPaint.alpha = (bg.backgroundImageOpacity * 255f).roundToInt().coerceIn(0, 255)
                        canvas.drawBitmap(src, coverMatrix, bgPaint)
                        src.recycle()
                    }
                }
            }
        }

        for (layer in document.layers) {
            if (!layer.visible) continue
            renderLayerToCanvas(
                canvas = canvas,
                layer = layer,
                document = document,
                fontRenderEngine = fontRenderEngine,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                isNestedInGroup = false,
                parentOpacity = 1f,
                parentScale = 1f
            )
        }

        output
    }

    private fun findFirstImageUri(layers: List<EditorLayer>): String? {
        for (layer in layers) {
            when (layer) {
                is ImageLayer -> return layer.uriString
                is GroupLayer -> findFirstImageUri(layer.children)?.let { return it }
                else -> Unit
            }
        }
        return null
    }

    private suspend fun renderLayerToCanvas(
        canvas: Canvas,
        layer: EditorLayer,
        document: ProjectDocument,
        fontRenderEngine: FontRenderEngine,
        targetWidth: Int,
        targetHeight: Int,
        isNestedInGroup: Boolean,
        parentOpacity: Float,
        parentScale: Float
    ) {
        if (!layer.visible) return
        val cx = if (isNestedInGroup) targetWidth * layer.offsetXNorm else targetWidth * (0.5f + layer.offsetXNorm)
        val cy = if (isNestedInGroup) targetHeight * layer.offsetYNorm else targetHeight * (0.5f + layer.offsetYNorm)
        val effectiveOpacity = (layer.opacity * parentOpacity).coerceIn(0f, 1f)
        val cumulativeScale = (layer.scale * parentScale).coerceAtLeast(0.05f)

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(layer.rotationDeg)
        canvas.scale(layer.scale * layer.stretchX, layer.scale * layer.stretchY)

        when (layer) {
            is GroupLayer -> {
                for (child in layer.children) {
                    renderLayerToCanvas(
                        canvas = canvas,
                        layer = child,
                        document = document,
                        fontRenderEngine = fontRenderEngine,
                        targetWidth = targetWidth,
                        targetHeight = targetHeight,
                        isNestedInGroup = true,
                        parentOpacity = effectiveOpacity,
                        parentScale = cumulativeScale
                    )
                }
            }
            is ImageLayer -> {
                decodeRgba8888Bitmap(layer.uriString, maxDimension = null)?.let { rawBmp ->
                    val bmp = if (layer.adjustments.sharpness > 0.01f) {
                        applySharpenBitmap(rawBmp, layer.adjustments.sharpness)
                    } else {
                        rawBmp
                    }
                    val cropNorm = layer.crop.normalized()
                    val srcRect = android.graphics.Rect(
                        (cropNorm.left * bmp.width).roundToInt().coerceIn(0, (bmp.width - 1).coerceAtLeast(0)),
                        (cropNorm.top * bmp.height).roundToInt().coerceIn(0, (bmp.height - 1).coerceAtLeast(0)),
                        (cropNorm.right * bmp.width).roundToInt().coerceIn(1, bmp.width.coerceAtLeast(1)),
                        (cropNorm.bottom * bmp.height).roundToInt().coerceIn(1, bmp.height.coerceAtLeast(1))
                    )
                    val croppedW = srcRect.width().coerceAtLeast(1)
                    val croppedH = srcRect.height().coerceAtLeast(1)
                    val fitScale = min(
                        (targetWidth * 0.85f) / croppedW,
                        (targetHeight * 0.85f) / croppedH
                    )
                    val drawW = croppedW * fitScale
                    val drawH = croppedH * fitScale
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        if (!layer.adjustments.isIdentity()) {
                            colorFilter = ColorMatrixColorFilter(buildColorMatrix(layer.adjustments))
                        }
                    }
                    val layerAlpha = (effectiveOpacity * 255f).roundToInt().coerceIn(0, 255)
                    val tintArgb = layer.tintColorArgb

                    if (layer.cornerPin.isIdentity() && layer.transform3D.isIdentity()) {
                        val dst = android.graphics.RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
                        val shapeClipSave = if (layer.cropShape == ImageCropShape.CIRCLE) {
                            val c = canvas.save()
                            val ovalPath = android.graphics.Path().apply {
                                addOval(dst, android.graphics.Path.Direction.CW)
                            }
                            canvas.clipPath(ovalPath)
                            c
                        } else null

                        if (tintArgb != null) {
                            val saveCount = canvas.saveLayerAlpha(dst, layerAlpha)
                            canvas.drawBitmap(bmp, srcRect, dst, paint)
                            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = tintArgb.toInt()
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                            }
                            canvas.drawRect(dst, tintPaint)
                            if (layer.adjustments.vignette > 0.01f) {
                                drawVignetteOnRect(canvas, dst, layer.adjustments.vignette, 1f)
                            }
                            canvas.restoreToCount(saveCount)
                        } else {
                            paint.alpha = layerAlpha
                            canvas.drawBitmap(bmp, srcRect, dst, paint)
                            if (layer.adjustments.vignette > 0.01f) {
                                drawVignetteOnRect(canvas, dst, layer.adjustments.vignette, effectiveOpacity)
                            }
                        }

                        if (shapeClipSave != null) {
                            canvas.restoreToCount(shapeClipSave)
                        }
                    } else {
                        val maxPrep = 1440f
                        val prepScale = min(1f, maxPrep / maxOf(drawW, drawH).coerceAtLeast(1f))
                        val prepW = (drawW * prepScale).roundToInt().coerceIn(24, 1600)
                        val prepH = (drawH * prepScale).roundToInt().coerceIn(24, 1600)
                        val prepared = Bitmap.createBitmap(prepW, prepH, Bitmap.Config.ARGB_8888)
                        val pc = Canvas(prepared)
                        val prepDst = android.graphics.RectF(0f, 0f, prepW.toFloat(), prepH.toFloat())
                        if (layer.cropShape == ImageCropShape.CIRCLE) {
                            val ovalPath = android.graphics.Path().apply {
                                addOval(prepDst, android.graphics.Path.Direction.CW)
                            }
                            pc.clipPath(ovalPath)
                        }
                        if (tintArgb != null) {
                            val saveCount = pc.saveLayerAlpha(prepDst, layerAlpha)
                            pc.drawBitmap(bmp, srcRect, prepDst, paint)
                            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = tintArgb.toInt()
                                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                            }
                            pc.drawRect(prepDst, tintPaint)
                            if (layer.adjustments.vignette > 0.01f) {
                                drawVignetteOnRect(pc, prepDst, layer.adjustments.vignette, 1f)
                            }
                            pc.restoreToCount(saveCount)
                        } else {
                            paint.alpha = layerAlpha
                            pc.drawBitmap(bmp, srcRect, prepDst, paint)
                            if (layer.adjustments.vignette > 0.01f) {
                                drawVignetteOnRect(pc, prepDst, layer.adjustments.vignette, effectiveOpacity)
                            }
                        }
                        val matrix = LayerTransformMatrix.buildBitmapDrawMatrix(
                            bitmapWidth = prepW,
                            bitmapHeight = prepH,
                            boxW = drawW,
                            boxH = drawH,
                            transform3D = layer.transform3D,
                            cornerPin = layer.cornerPin
                        )
                        matrix.postTranslate(-drawW / 2f, -drawH / 2f)
                        canvas.drawBitmap(prepared, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        prepared.recycle()
                    }

                    if (bmp !== rawBmp) bmp.recycle()
                    rawBmp.recycle()
                }
            }
            is ShapeLayer -> {
                drawShapeOnCanvas(canvas, layer.copy(opacity = effectiveOpacity), targetWidth, targetHeight)
            }
            is TextLayer -> {
                val resolutionFactor = targetWidth / 360f
                val effPx = layer.fontSizeSp * resolutionFactor
                val measured = fontRenderEngine.measureTextLayerBounds(
                    layer = layer.copy(opacity = effectiveOpacity),
                    effectiveFontSizePx = effPx,
                    maxWidthPx = targetWidth * 0.9f,
                    emojiSourceMode = document.emojiSourceMode,
                    appEmojiFontId = document.appEmojiFontId
                )
                if (layer.cornerPin.isIdentity() && layer.transform3D.isIdentity()) {
                    val renderScale = cumulativeScale.coerceIn(1f, (2048f / effPx.coerceAtLeast(1f)).coerceAtLeast(1f))
                    if (renderScale > 1.01f) {
                        canvas.scale(1f / renderScale, 1f / renderScale)
                    }
                    fontRenderEngine.drawTextLayerOnCanvas(
                        canvas = canvas,
                        layer = layer.copy(opacity = effectiveOpacity),
                        effectiveFontSizePx = effPx * renderScale,
                        maxWidthPx = ((measured.widthPx + 4f).coerceAtMost(targetWidth * 0.9f)) * renderScale,
                        emojiSourceMode = document.emojiSourceMode,
                        appEmojiFontId = document.appEmojiFontId
                    )
                } else {
                    val boxW = measured.widthPx.coerceAtLeast(48f)
                    val boxH = measured.heightPx.coerceAtLeast(48f)
                    val texScale = cumulativeScale.coerceIn(1f, 6f)
                        .coerceAtMost(1440f / maxOf(boxW, boxH).coerceAtLeast(1f))
                        .coerceAtLeast(1f)
                    val texW = (boxW * texScale).roundToInt().coerceIn(24, 1600)
                    val texH = (boxH * texScale).roundToInt().coerceIn(24, 1600)
                    val textBmp = Bitmap.createBitmap(texW, texH, Bitmap.Config.ARGB_8888)
                    val tc = Canvas(textBmp)
                    tc.translate(texW / 2f, texH / 2f)
                    fontRenderEngine.drawTextLayerOnCanvas(
                        canvas = tc,
                        layer = layer.copy(opacity = effectiveOpacity),
                        effectiveFontSizePx = effPx * texScale,
                        maxWidthPx = ((measured.widthPx + 4f).coerceAtMost(targetWidth * 0.9f)) * texScale,
                        emojiSourceMode = document.emojiSourceMode,
                        appEmojiFontId = document.appEmojiFontId
                    )
                    val matrix = LayerTransformMatrix.buildBitmapDrawMatrix(
                        bitmapWidth = texW,
                        bitmapHeight = texH,
                        boxW = boxW,
                        boxH = boxH,
                        transform3D = layer.transform3D,
                        cornerPin = layer.cornerPin
                    )
                    matrix.postTranslate(-boxW / 2f, -boxH / 2f)
                    canvas.drawBitmap(textBmp, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    textBmp.recycle()
                }
            }
            is DrawLayer -> {
                drawVectorStrokesOnCanvas(
                    canvas = canvas,
                    layer = layer.copy(opacity = effectiveOpacity),
                    targetWidth = targetWidth,
                    targetHeight = targetHeight
                )
            }
        }
        canvas.restore()
    }

    private fun drawVectorStrokesOnCanvas(
        canvas: Canvas,
        layer: DrawLayer,
        targetWidth: Int,
        targetHeight: Int
    ) {
        val w = targetWidth.toFloat().coerceAtLeast(1f)
        val h = targetHeight.toFloat().coerceAtLeast(1f)
        for (stroke in layer.strokes) {
            VectorStrokeEngine.drawVectorStroke(
                canvas = canvas,
                stroke = stroke,
                areaWidthPx = w,
                areaHeightPx = h,
                layerOpacity = layer.opacity,
                originOffsetX = -w * 0.5f,
                originOffsetY = -h * 0.5f
            )
        }
    }

    suspend fun saveProjectThumbnail(
        document: ProjectDocument,
        fontRenderEngine: FontRenderEngine
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "thumbnails")
        if (!dir.exists()) dir.mkdirs()
        val thumbFile = File(dir, "${document.id}.png")
        val thumbW = 360
        val thumbH = (thumbW / document.canvas.ratioFloat).roundToInt().coerceIn(180, 640)
        val bmp = renderProjectToBitmap(document, fontRenderEngine, thumbW, thumbH)
        FileOutputStream(thumbFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bmp.recycle()
        thumbFile.absolutePath
    }

    suspend fun exportPngToAppAndGallery(
        document: ProjectDocument,
        fontRenderEngine: FontRenderEngine
    ): Result<Pair<File, Uri?>> = exportImageToAppAndGallery(
        document = document,
        fontRenderEngine = fontRenderEngine,
        format = ExportImageFormat.PNG,
        targetWidth = document.canvas.exportWidth,
        targetHeight = document.canvas.exportHeight
    )

    suspend fun exportImageToAppAndGallery(
        document: ProjectDocument,
        fontRenderEngine: FontRenderEngine,
        format: ExportImageFormat,
        targetWidth: Int,
        targetHeight: Int
    ): Result<Pair<File, Uri?>> = withContext(Dispatchers.IO) {
        runCatching {
            val safeW = targetWidth.coerceIn(64, 8192)
            val safeH = targetHeight.coerceIn(64, 8192)
            val exportsDir = File(context.filesDir, "exports")
            if (!exportsDir.exists()) exportsDir.mkdirs()
            val fileName = "PixelForge_${System.currentTimeMillis()}.${format.ext}"
            val outFile = File(exportsDir, fileName)

            val rendered = renderProjectToBitmap(document, fontRenderEngine, safeW, safeH)
            val finalBitmap = if (format == ExportImageFormat.JPG) {
                val opaque = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
                val c = Canvas(opaque)
                c.drawColor(android.graphics.Color.WHITE)
                c.drawBitmap(rendered, 0f, 0f, null)
                rendered.recycle()
                opaque
            } else {
                rendered
            }

            FileOutputStream(outFile).use { stream ->
                val compressFmt = if (format == ExportImageFormat.JPG) {
                    Bitmap.CompressFormat.JPEG
                } else {
                    Bitmap.CompressFormat.PNG
                }
                finalBitmap.compress(compressFmt, 96, stream)
            }
            finalBitmap.recycle()

            val galleryUri = saveImageToMediaStore(outFile, fileName, format.mimeType)
            outFile to galleryUri
        }
    }

    private fun saveImageToMediaStore(file: File, displayName: String, mimeType: String): Uri? {
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PixelForge")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    resolver.update(uri, done, null, null)
                }
            }
            uri
        }.getOrNull()
    }

    fun getShareUri(file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun computeCoverMatrix(srcW: Int, srcH: Int, dstW: Float, dstH: Float): Matrix {
        val scale = maxOf(dstW / srcW.coerceAtLeast(1), dstH / srcH.coerceAtLeast(1))
        val dx = (dstW - srcW * scale) / 2f
        val dy = (dstH - srcH * scale) / 2f
        return Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
    }

    private fun blendModeToPorterDuff(mode: EditorBlendMode): PorterDuffXfermode? {
        return when (mode) {
            EditorBlendMode.NORMAL -> null
            EditorBlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            EditorBlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            EditorBlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            EditorBlendMode.SOFT_LIGHT -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            EditorBlendMode.DARKEN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
        }
    }

    private fun drawShapeOnCanvas(
        canvas: Canvas,
        layer: ShapeLayer,
        targetWidth: Int,
        targetHeight: Int
    ) {
        val baseSize = min(targetWidth, targetHeight) * 0.34f
        val strokeScale = min(targetWidth, targetHeight) / 360f
        VectorShapeEngine.drawShapeOnCanvas(
            canvas = canvas,
            layer = layer,
            baseSizePx = baseSize,
            cx = 0f,
            cy = 0f,
            strokeScaleFactor = strokeScale
        )
    }

    private fun drawVignetteOnRect(
        canvas: Canvas,
        rect: android.graphics.RectF,
        vignette: Float,
        layerOpacity: Float
    ) {
        val radius = maxOf(rect.width(), rect.height()) * 0.72f
        if (radius <= 1f) return
        val edgeAlpha = (vignette.coerceIn(0f, 1f) * 0.82f * layerOpacity * 255f).roundToInt().coerceIn(0, 255)
        val gradient = RadialGradient(
            rect.centerX(),
            rect.centerY(),
            radius,
            intArrayOf(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.argb(edgeAlpha / 3, 0, 0, 0),
                android.graphics.Color.argb(edgeAlpha, 0, 0, 0)
            ),
            floatArrayOf(0.45f, 0.76f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
        canvas.drawRect(rect, p)
    }

    private fun applySharpenBitmap(src: Bitmap, strength: Float): Bitmap {
        val amount = strength.coerceIn(0f, 1f) * 1.4f
        if (amount <= 0.01f) return src
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        val out = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        System.arraycopy(pixels, 0, out, 0, pixels.size)
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val c = pixels[row + x]
                val t = pixels[row - w + x]
                val b = pixels[row + w + x]
                val l = pixels[row + x - 1]
                val r = pixels[row + x + 1]

                val a = (c ushr 24) and 0xFF
                val cr = (c ushr 16) and 0xFF
                val cg = (c ushr 8) and 0xFF
                val cb = c and 0xFF

                val avgR = (((t ushr 16) and 0xFF) + ((b ushr 16) and 0xFF) + ((l ushr 16) and 0xFF) + ((r ushr 16) and 0xFF)) / 4f
                val avgG = (((t ushr 8) and 0xFF) + ((b ushr 8) and 0xFF) + ((l ushr 8) and 0xFF) + ((r ushr 8) and 0xFF)) / 4f
                val avgB = ((t and 0xFF) + (b and 0xFF) + (l and 0xFF) + (r and 0xFF)) / 4f

                val nr = (cr + (cr - avgR) * amount).roundToInt().coerceIn(0, 255)
                val ng = (cg + (cg - avgG) * amount).roundToInt().coerceIn(0, 255)
                val nb = (cb + (cb - avgB) * amount).roundToInt().coerceIn(0, 255)
                out[row + x] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun stackBlurInPlace(bitmap: Bitmap, radius: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val a = IntArray(wh)
        var rsum: Int; var gsum: Int; var bsum: Int; var asum: Int
        var x: Int; var y: Int; var i: Int; var p: Int; var yp: Int; var yi: Int; var yw: Int
        val vmin = IntArray(maxOf(w, h))
        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in dv.indices) dv[idx] = idx / divsum

        yw = 0; yi = 0
        val stack = Array(div) { IntArray(4) }
        var stackpointer: Int; var stackstart: Int; var sir: IntArray; var rbs: Int
        val r1 = radius + 1
        var routsum: Int; var goutsum: Int; var boutsum: Int; var aoutsum: Int
        var rinsum: Int; var ginsum: Int; var binsum: Int; var ainsum: Int

        y = 0
        while (y < h) {
            rinsum = 0; ginsum = 0; binsum = 0; ainsum = 0
            routsum = 0; goutsum = 0; boutsum = 0; aoutsum = 0
            rsum = 0; gsum = 0; bsum = 0; asum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + minOf(wm, maxOf(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                sir[3] = (p ushr 24) and 0xff
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs; gsum += sir[1] * rbs; bsum += sir[2] * rbs; asum += sir[3] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; ainsum += sir[3]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; aoutsum += sir[3]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum]; a[yi] = dv[asum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum; asum -= aoutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]; aoutsum -= sir[3]
                if (y == 0) vmin[x] = minOf(x + radius + 1, wm)
                p = pix[yw + vmin[x]]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                sir[3] = (p ushr 24) and 0xff
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; ainsum += sir[3]
                rsum += rinsum; gsum += ginsum; bsum += binsum; asum += ainsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; aoutsum += sir[3]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]; ainsum -= sir[3]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            rinsum = 0; ginsum = 0; binsum = 0; ainsum = 0
            routsum = 0; goutsum = 0; boutsum = 0; aoutsum = 0
            rsum = 0; gsum = 0; bsum = 0; asum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = maxOf(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi]; sir[3] = a[yi]
                rbs = r1 - kotlin.math.abs(i)
                rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs; asum += a[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; ainsum += sir[3]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; aoutsum += sir[3]
                }
                if (i < hm) yp += w
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = (dv[asum] shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum; asum -= aoutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]; aoutsum -= sir[3]
                if (x == 0) vmin[y] = minOf(y + r1, hm) * w
                p = x + vmin[y]
                sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p]; sir[3] = a[p]
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; ainsum += sir[3]
                rsum += rinsum; gsum += ginsum; bsum += binsum; asum += ainsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; aoutsum += sir[3]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]; ainsum -= sir[3]
                yi += w
                y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
    }

    companion object {
        fun buildColorMatrix(adj: ImageAdjustments): ColorMatrix {
            val result = ColorMatrix()

            // Exposure + Contrast
            val expFactor = 2.0.pow(adj.exposure.toDouble()).toFloat()
            val contrast = 1f + adj.contrast.coerceIn(-0.85f, 1.5f)
            val translate = 128f * (1f - contrast) + adj.brightness * 90f
            val expContrast = ColorMatrix(
                floatArrayOf(
                    expFactor * contrast, 0f, 0f, 0f, translate,
                    0f, expFactor * contrast, 0f, 0f, translate,
                    0f, 0f, expFactor * contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.postConcat(expContrast)

            // Saturation + Vibrance
            val satValue = (1f + adj.saturation + adj.vibrance * 0.55f).coerceIn(0f, 2.5f)
            val satMatrix = ColorMatrix().apply { setSaturation(satValue) }
            result.postConcat(satMatrix)

            // Warmth (red/blue balance)
            if (adj.warmth != 0f) {
                val w = adj.warmth * 36f
                val warmMatrix = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, w,
                        0f, 1f, 0f, 0f, w * 0.25f,
                        0f, 0f, 1f, 0f, -w,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                result.postConcat(warmMatrix)
            }

            // Hue rotation (-180..180 deg)
            if (adj.hue != 0f) {
                val angle = Math.toRadians(adj.hue.toDouble())
                val cosVal = cos(angle).toFloat()
                val sinVal = sin(angle).toFloat()
                val lumR = 0.213f
                val lumG = 0.715f
                val lumB = 0.072f
                val hueMatrix = ColorMatrix(
                    floatArrayOf(
                        lumR + cosVal * (1f - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1f - lumB), 0f, 0f,
                        lumR + cosVal * (-lumR) + sinVal * 0.143f, lumG + cosVal * (1f - lumG) + sinVal * 0.140f, lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0f, 0f,
                        lumR + cosVal * (-lumR) + sinVal * (-(1f - lumR)), lumG + cosVal * (-lumG) + sinVal * lumG, lumB + cosVal * (1f - lumB) + sinVal * lumB, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                result.postConcat(hueMatrix)
            }

            return result
        }

        fun computeTargetResolution(
            canvas: CanvasDimensions,
            preset: ExportResolutionPreset,
            customWidth: Int,
            customHeight: Int
        ): Pair<Int, Int> {
            return when (preset) {
                ExportResolutionPreset.RES_4K -> scaleToLongEdge(canvas.ratioFloat, 3840)
                ExportResolutionPreset.RES_2K -> scaleToLongEdge(canvas.ratioFloat, 2048)
                ExportResolutionPreset.CUSTOM -> {
                    customWidth.coerceIn(64, 8192) to customHeight.coerceIn(64, 8192)
                }
            }
        }

        private fun scaleToLongEdge(ratio: Float, longEdge: Int): Pair<Int, Int> {
            val safeRatio = ratio.coerceIn(0.05f, 20f)
            return if (safeRatio >= 1f) {
                longEdge to (longEdge / safeRatio).roundToInt().coerceAtLeast(64)
            } else {
                (longEdge * safeRatio).roundToInt().coerceAtLeast(64) to longEdge
            }
        }
    }
}
