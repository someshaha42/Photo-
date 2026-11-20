package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.fonts.FontRenderEngine
import com.example.model.BackgroundTab
import com.example.model.EditorBlendMode
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextAlignOption
import com.example.model.TextLayer
import com.example.model.VideoLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

data class ImportedImageMeta(
    val uriString: String,
    val width: Int,
    val height: Int,
    val displayName: String
)

data class ImportedVideoMeta(
    val uriString: String,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val displayName: String
)

class MediaStorageManager(private val context: Context) {

    suspend fun importImage(
        sourceUri: Uri,
        projectId: String,
        keepLocalCopy: Boolean
    ): ImportedImageMeta = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(sourceUri) ?: "image_${System.currentTimeMillis()}.png"
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

    suspend fun importVideo(
        sourceUri: Uri,
        projectId: String,
        keepLocalCopy: Boolean
    ): ImportedVideoMeta = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(sourceUri) ?: "video_${System.currentTimeMillis()}.mp4"
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

        var width = 1080
        var height = 1920
        var durationMs = 5000L
        val retriever = MediaMetadataRetriever()
        try {
            if (resolvedUriString.startsWith("/")) {
                retriever.setDataSource(resolvedUriString)
            } else {
                retriever.setDataSource(context, Uri.parse(resolvedUriString))
            }
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 1080
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 1920
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(500L) ?: 5000L
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }

        ImportedVideoMeta(
            uriString = resolvedUriString,
            width = width,
            height = height,
            durationMs = durationMs,
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

    suspend fun extractVideoThumbnail(uriOrPath: String, timeUs: Long = 0L): Bitmap? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                if (uriOrPath.startsWith("/")) {
                    retriever.setDataSource(uriOrPath)
                } else {
                    retriever.setDataSource(context, Uri.parse(uriOrPath))
                }
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.copy(Bitmap.Config.ARGB_8888, false)
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }

    /**
     * Fast in-memory StackBlur preserving ARGB_8888 channels.
     */
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
        targetWidth: Int = document.aspectRatio.exportWidth,
        targetHeight: Int = document.aspectRatio.exportHeight
    ): Bitmap = withContext(Dispatchers.IO) {
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        // Start with pure transparent RGBA8888 buffer (never export checkerboard)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val bg = document.background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = blendModeToPorterDuff(bg.blendMode)
        }

        // 1. Background Layer (Transparent tab exports transparent!)
        when (bg.activeTab) {
            BackgroundTab.TRANSPARENT -> Unit
            BackgroundTab.COLORS -> {
                val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = bg.colorArgb.toInt()
                    alpha = ((android.graphics.Color.alpha(bg.colorArgb) / 255f) * bg.colorOpacity * 255f)
                        .roundToInt().coerceIn(0, 255)
                    xfermode = blendModeToPorterDuff(bg.blendMode)
                }
                canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), colorPaint)
            }
            BackgroundTab.BLUR -> {
                val firstImageUri = bg.backgroundImageUri
                    ?: document.layers.filterIsInstance<ImageLayer>().firstOrNull()?.uriString
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

        // 2. Render all visible layers non-destructively at full target export resolution
        for (layer in document.layers) {
            if (!layer.visible) continue
            val cx = targetWidth * (0.5f + layer.offsetXNorm)
            val cy = targetHeight * (0.5f + layer.offsetYNorm)
            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(layer.rotationDeg)
            canvas.scale(layer.scale, layer.scale)

            when (layer) {
                is ImageLayer -> {
                    decodeRgba8888Bitmap(layer.uriString, maxDimension = null)?.let { bmp ->
                        val fitScale = min(
                            (targetWidth * 0.85f) / bmp.width.coerceAtLeast(1),
                            (targetHeight * 0.85f) / bmp.height.coerceAtLeast(1)
                        )
                        val drawW = bmp.width * fitScale
                        val drawH = bmp.height * fitScale
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (layer.opacity * 255f).roundToInt().coerceIn(0, 255)
                        }
                        val dst = android.graphics.RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
                        canvas.drawBitmap(bmp, null, dst, paint)
                        bmp.recycle()
                    }
                }
                is VideoLayer -> {
                    extractVideoThumbnail(layer.uriString, layer.trimStartMs * 1000L)?.let { frame ->
                        val fitScale = min(
                            (targetWidth * 0.85f) / frame.width.coerceAtLeast(1),
                            (targetHeight * 0.85f) / frame.height.coerceAtLeast(1)
                        )
                        val drawW = frame.width * fitScale
                        val drawH = frame.height * fitScale
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (layer.opacity * 255f).roundToInt().coerceIn(0, 255)
                        }
                        val dst = android.graphics.RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
                        canvas.drawBitmap(frame, null, dst, paint)
                        frame.recycle()
                    }
                }
                is ShapeLayer -> {
                    drawShapeOnCanvas(canvas, layer, targetWidth)
                }
                is TextLayer -> {
                    // Render text and Color Emojis at actual export resolution from original Typeface!
                    val resolutionFactor = targetWidth / 360f
                    fontRenderEngine.drawTextLayerOnCanvas(
                        canvas = canvas,
                        layer = layer,
                        effectiveFontSizePx = layer.fontSizeSp * resolutionFactor,
                        maxWidthPx = targetWidth * 0.9f
                    )
                }
            }
            canvas.restore()
        }

        output
    }

    suspend fun saveProjectThumbnail(
        document: ProjectDocument,
        fontRenderEngine: FontRenderEngine
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "thumbnails")
        if (!dir.exists()) dir.mkdirs()
        val thumbFile = File(dir, "${document.id}.png")
        val thumbW = 360
        val thumbH = (thumbW / document.aspectRatio.ratioFloat).roundToInt().coerceIn(180, 640)
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
    ): Result<Pair<File, Uri?>> = withContext(Dispatchers.IO) {
        runCatching {
            val exportsDir = File(context.filesDir, "exports")
            if (!exportsDir.exists()) exportsDir.mkdirs()
            val fileName = "PixelForge_${System.currentTimeMillis()}.png"
            val outFile = File(exportsDir, fileName)
            val bitmap = renderProjectToBitmap(document, fontRenderEngine)
            FileOutputStream(outFile).use { stream ->
                // Lossless PNG preserving full RGBA8888 8-bit alpha transparency
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            bitmap.recycle()

            val galleryUri = savePngToMediaStore(outFile, fileName)
            outFile to galleryUri
        }
    }

    private fun savePngToMediaStore(file: File, displayName: String): Uri? {
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
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

    private fun drawShapeOnCanvas(canvas: Canvas, layer: ShapeLayer, targetWidth: Int) {
        val baseSize = targetWidth * 0.28f
        val half = baseSize / 2f
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = layer.fillColorArgb.toInt()
            alpha = ((android.graphics.Color.alpha(layer.fillColorArgb) / 255f) * layer.opacity * 255f)
                .roundToInt().coerceIn(0, 255)
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = layer.strokeColorArgb.toInt()
            strokeWidth = layer.strokeWidth * (targetWidth / 360f)
            alpha = (layer.opacity * 255f).roundToInt().coerceIn(0, 255)
        }
        val rect = android.graphics.RectF(-half, -half, half, half)
        when (layer.shapeType) {
            LayerShapeType.RECTANGLE -> {
                canvas.drawRect(rect, fillPaint)
                if (layer.strokeWidth > 0f) canvas.drawRect(rect, strokePaint)
            }
            LayerShapeType.ROUNDED_RECT -> {
                val r = baseSize * 0.18f
                canvas.drawRoundRect(rect, r, r, fillPaint)
                if (layer.strokeWidth > 0f) canvas.drawRoundRect(rect, r, r, strokePaint)
            }
            LayerShapeType.CIRCLE -> {
                canvas.drawCircle(0f, 0f, half, fillPaint)
                if (layer.strokeWidth > 0f) canvas.drawCircle(0f, 0f, half, strokePaint)
            }
            LayerShapeType.STAR, LayerShapeType.BADGE -> {
                val points = if (layer.shapeType == LayerShapeType.STAR) 5 else 8
                val innerRatio = if (layer.shapeType == LayerShapeType.STAR) 0.45f else 0.78f
                val path = android.graphics.Path()
                val total = points * 2
                for (i in 0 until total) {
                    val radius = if (i % 2 == 0) half else half * innerRatio
                    val angle = Math.toRadians((i * 360.0 / total) - 90.0)
                    val x = (radius * kotlin.math.cos(angle)).toFloat()
                    val y = (radius * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, fillPaint)
                if (layer.strokeWidth > 0f) canvas.drawPath(path, strokePaint)
            }
        }
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
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var asum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(maxOf(w, h))
        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in dv.indices) dv[idx] = idx / divsum

        yw = 0
        yi = 0
        val stack = Array(div) { IntArray(4) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var aoutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        var ainsum: Int

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
}
