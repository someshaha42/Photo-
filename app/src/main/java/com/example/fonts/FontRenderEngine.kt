package com.example.fonts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import com.example.model.TextAlignOption
import com.example.model.TextLayer
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.roundToInt

data class GlyphCacheKey(
    val fontId: String,
    val text: String,
    val targetBucketPx: Int,
    val colorArgb: Long,
    val strokeColorArgb: Long,
    val strokeWidth: Float,
    val isBold: Boolean,
    val isItalic: Boolean,
    val shadowEnabled: Boolean
)

data class CachedGlyph(
    val bitmap: Bitmap,
    val renderedPx: Int,
    val byteCount: Int
)

/**
 * High-resolution RGBA8888 font & Color Emoji rendering engine with intelligent resolution-aware LRU cache.
 * - Never upscales a lower-resolution cached bitmap when higher zoom/export resolution is needed.
 * - Never strips colors or forces monochrome onto Color Emoji fonts (COLR/CPAL, CBDT/CBLC, sbix).
 * - Preserves full 8-bit alpha channel in ARGB_8888.
 */
class FontRenderEngine(private val context: Context) {

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()
    private val metadataIndex = ConcurrentHashMap<String, FontMetadata>()

    private val maxCacheBytes = 32 * 1024 * 1024 // 32 MB bounded memory
    private val glyphCache = object : LruCache<GlyphCacheKey, CachedGlyph>(maxCacheBytes) {
        override fun sizeOf(key: GlyphCacheKey, value: CachedGlyph): Int = value.byteCount
    }

    @Volatile private var cacheHits = 0L
    @Volatile private var cacheMisses = 0L
    @Volatile private var lastRasterMs = 0.0
    @Volatile private var totalRasterMs = 0.0
    @Volatile private var rasterCount = 0L
    @Volatile private var lastRequestedPx = 0
    @Volatile private var lastRenderedPx = 0

    fun registerFonts(fonts: List<FontMetadata>) {
        fonts.forEach { meta ->
            metadataIndex[meta.id] = meta
        }
    }

    fun getMetadata(fontId: String): FontMetadata? = metadataIndex[fontId]

    fun getTypeface(fontId: String): Typeface {
        typefaceCache[fontId]?.let { return it }
        val meta = metadataIndex[fontId]
        val loaded = loadTypefaceInternal(meta)
        typefaceCache[fontId] = loaded
        return loaded
    }

    fun evictFont(fontId: String) {
        typefaceCache.remove(fontId)
        metadataIndex.remove(fontId)
        glyphCache.snapshot().keys.filter { it.fontId == fontId }.forEach { glyphCache.remove(it) }
    }

    private fun loadTypefaceInternal(meta: FontMetadata?): Typeface {
        if (meta == null) return Typeface.DEFAULT
        return runCatching {
            when (meta.source) {
                FontSource.BUNDLED -> {
                    Typeface.createFromAsset(context.assets, meta.storagePath)
                }
                FontSource.IMPORTED -> {
                    val path = meta.renderablePath ?: meta.storagePath
                    val file = File(path)
                    if (file.exists()) Typeface.createFromFile(file) else Typeface.DEFAULT
                }
            }
        }.getOrElse { Typeface.DEFAULT }
    }

    /**
     * Renders text/emoji directly onto a target Canvas (e.g. live zoomed canvas or full-res PNG export)
     * from the original Typeface vector/strike source — zero intermediate low-res downsampling!
     */
    fun drawTextLayerOnCanvas(
        canvas: Canvas,
        layer: TextLayer,
        effectiveFontSizePx: Float,
        maxWidthPx: Float
    ) {
        val startNs = System.nanoTime()
        val meta = metadataIndex[layer.fontId]
        val typeface = getTypeface(layer.fontId)
        val isColorEmojiFont = meta?.classification == FontClassification.COLOR_EMOJI ||
            meta?.colorTableType != null && meta.colorTableType != ColorTableType.NONE

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            this.typeface = applyStyle(typeface, layer.isBold, layer.isItalic)
            this.textSize = effectiveFontSizePx.coerceAtLeast(8f)
            this.letterSpacing = layer.letterSpacing
            this.alpha = (layer.opacity * 255f).roundToInt().coerceIn(0, 255)
            // CRITICAL: Preserve original Color Emoji colors, gradients, and alpha.
            // Never attach a monochrome ColorFilter or override RGB on Color Emoji fonts!
            if (!isColorEmojiFont) {
                this.color = layer.colorArgb.toInt()
                this.alpha = ((android.graphics.Color.alpha(layer.colorArgb) / 255f) * layer.opacity * 255f)
                    .roundToInt().coerceIn(0, 255)
            } else {
                // Keep white base with full layer alpha so Android Skia draws native COLR/CBDT/sbix colors untouched
                this.color = android.graphics.Color.WHITE
                this.alpha = (layer.opacity * 255f).roundToInt().coerceIn(0, 255)
            }
            if (layer.shadowEnabled) {
                setShadowLayer(
                    layer.shadowRadius.coerceAtLeast(1f) * (effectiveFontSizePx / 36f),
                    0f,
                    2f * (effectiveFontSizePx / 36f),
                    layer.shadowColorArgb.toInt()
                )
            }
        }

        val layoutWidth = maxWidthPx.roundToInt().coerceAtLeast(64)
        val alignment = when (layer.align) {
            TextAlignOption.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignOption.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val staticLayout = StaticLayout.Builder
            .obtain(layer.text, 0, layer.text.length, textPaint, layoutWidth)
            .setAlignment(alignment)
            .setIncludePad(true)
            .build()

        val textW = (0 until staticLayout.lineCount).maxOfOrNull { staticLayout.getLineWidth(it) } ?: 100f
        val textH = staticLayout.height.toFloat()

        canvas.save()
        canvas.translate(-layoutWidth / 2f, -textH / 2f)

        if (layer.bgPillEnabled) {
            val padX = effectiveFontSizePx * 0.45f
            val padY = effectiveFontSizePx * 0.25f
            val left = when (layer.align) {
                TextAlignOption.LEFT -> -padX
                TextAlignOption.CENTER -> (layoutWidth - textW) / 2f - padX
                TextAlignOption.RIGHT -> (layoutWidth - textW) - padX
            }
            val pillRect = android.graphics.RectF(
                left,
                -padY,
                left + textW + padX * 2f,
                textH + padY
            )
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = layer.bgPillColorArgb.toInt()
                alpha = ((android.graphics.Color.alpha(layer.bgPillColorArgb) / 255f) * layer.opacity * 255f)
                    .roundToInt().coerceIn(0, 255)
            }
            canvas.drawRoundRect(pillRect, effectiveFontSizePx * 0.35f, effectiveFontSizePx * 0.35f, pillPaint)
        }

        if (layer.strokeWidth > 0f && !isColorEmojiFont) {
            val strokePaint = TextPaint(textPaint).apply {
                style = Paint.Style.STROKE
                color = layer.strokeColorArgb.toInt()
                strokeWidth = layer.strokeWidth * (effectiveFontSizePx / 36f)
                strokeJoin = Paint.Join.ROUND
                clearShadowLayer()
            }
            val strokeLayout = StaticLayout.Builder
                .obtain(layer.text, 0, layer.text.length, strokePaint, layoutWidth)
                .setAlignment(alignment)
                .setIncludePad(true)
                .build()
            strokeLayout.draw(canvas)
        }

        staticLayout.draw(canvas)
        canvas.restore()

        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
        lastRasterMs = elapsedMs
        totalRasterMs += elapsedMs
        rasterCount++
        lastRequestedPx = effectiveFontSizePx.roundToInt()
        lastRenderedPx = effectiveFontSizePx.roundToInt()
    }

    /**
     * Resolution-aware cached glyph/text bitmap generator used for Zoom Quality inspection (100%-800%)
     * and high-res bitmap overlays (e.g., Media3 video export overlays).
     * Never returns an upscaled low-res cache entry if targetPx exceeds cached resolution.
     */
    fun getOrRenderGlyphBitmap(
        fontId: String,
        text: String,
        targetFontPx: Int,
        colorArgb: Long = 0xFFFFFFFF,
        strokeColorArgb: Long = 0xFF000000,
        strokeWidth: Float = 0f,
        isBold: Boolean = false,
        isItalic: Boolean = false,
        shadowEnabled: Boolean = false
    ): Bitmap {
        val clampedPx = targetFontPx.coerceIn(16, 640)
        // Quantize to clean high-res buckets (16px steps) so small pinch fluctuations reuse cache cleanly,
        // while larger zoom increases trigger fresh high-res rasterization!
        val bucketPx = ((clampedPx + 15) / 16) * 16
        val key = GlyphCacheKey(
            fontId = fontId,
            text = text,
            targetBucketPx = bucketPx,
            colorArgb = colorArgb,
            strokeColorArgb = strokeColorArgb,
            strokeWidth = strokeWidth,
            isBold = isBold,
            isItalic = isItalic,
            shadowEnabled = shadowEnabled
        )

        lastRequestedPx = clampedPx
        val existing = glyphCache.get(key)
        if (existing != null && existing.renderedPx >= clampedPx) {
            cacheHits++
            lastRenderedPx = existing.renderedPx
            return existing.bitmap
        }

        cacheMisses++
        val startNs = System.nanoTime()
        val meta = metadataIndex[fontId]
        val isColorEmoji = meta?.classification == FontClassification.COLOR_EMOJI ||
            (meta?.colorTableType != null && meta.colorTableType != ColorTableType.NONE)
        val typeface = applyStyle(getTypeface(fontId), isBold, isItalic)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            this.typeface = typeface
            this.textSize = bucketPx.toFloat()
            this.color = if (isColorEmoji) android.graphics.Color.WHITE else colorArgb.toInt()
        }

        val fm = paint.fontMetrics
        val lineHeight = ceil((fm.descent - fm.ascent).toDouble()).toInt().coerceAtLeast(bucketPx)
        val lines = text.split("\n").ifEmpty { listOf(" ") }
        val maxLineW = lines.maxOfOrNull { ceil(paint.measureText(it).toDouble()).toInt() }
            ?.coerceAtLeast(bucketPx) ?: bucketPx
        val pad = (bucketPx * 0.25f).roundToInt().coerceAtLeast(12)
        val bmpW = (maxLineW + pad * 2).coerceIn(32, 1600)
        val bmpH = (lineHeight * lines.size + pad * 2).coerceIn(32, 1600)

        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        var y = pad - fm.ascent
        for (line in lines) {
            if (strokeWidth > 0f && !isColorEmoji) {
                val sp = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    color = strokeColorArgb.toInt()
                    this.strokeWidth = strokeWidth * (bucketPx / 36f)
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawText(line, pad.toFloat(), y, sp)
            }
            canvas.drawText(line, pad.toFloat(), y, paint)
            y += lineHeight
        }

        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
        lastRasterMs = elapsedMs
        totalRasterMs += elapsedMs
        rasterCount++
        lastRenderedPx = bucketPx

        val entry = CachedGlyph(bitmap, bucketPx, bitmap.allocationByteCount)
        glyphCache.put(key, entry)
        return bitmap
    }

    fun getDiagnosticsSnapshot(): FontDiagnosticsSnapshot {
        val all = metadataIndex.values.toList()
        return FontDiagnosticsSnapshot(
            totalFonts = all.size,
            bundledCount = all.count { it.source == FontSource.BUNDLED },
            importedCount = all.count { it.source == FontSource.IMPORTED },
            colorEmojiCount = all.count {
                it.classification == FontClassification.COLOR_EMOJI ||
                    it.classification == FontClassification.MIXED_TEXT_COLOR_EMOJI
            },
            cacheHitCount = cacheHits,
            cacheMissCount = cacheMisses,
            cacheMemoryBytes = glyphCache.size(),
            cacheMaxBytes = maxCacheBytes,
            cacheEntryCount = glyphCache.snapshot().size,
            lastRasterizationMs = lastRasterMs,
            avgRasterizationMs = if (rasterCount > 0) totalRasterMs / rasterCount else 0.0,
            lastRequestedPx = lastRequestedPx,
            lastRenderedPx = lastRenderedPx,
            activeFontDiagnostics = all.map { f ->
                val strikes = if (f.bitmapStrikePpem.isNotEmpty()) " • strikes=${f.bitmapStrikePpem.joinToString("/")}px" else ""
                "${f.displayName}: ${f.classification.badgeName} (${f.colorTableType.label}, ${f.glyphCount} glyphs$strikes)"
            }
        )
    }

    private fun applyStyle(base: Typeface, isBold: Boolean, isItalic: Boolean): Typeface {
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        return if (style == Typeface.NORMAL) base else Typeface.create(base, style)
    }
}
