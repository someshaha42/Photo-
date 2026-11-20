package com.example.fonts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.util.LruCache
import com.example.model.EmojiSourceMode
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

class EmojiSpanSegment(
    val start: Int,
    val end: Int,
    val isEmoji: Boolean
)

private class CustomEmojiFontSpan(
    private val emojiTypeface: Typeface,
    private val preserveOriginalColor: Boolean,
    private val transparentForStrokePass: Boolean,
    private val layerAlpha: Int
) : MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = emojiTypeface
        if (transparentForStrokePass) {
            tp.color = android.graphics.Color.TRANSPARENT
            tp.clearShadowLayer()
        } else if (preserveOriginalColor) {
            tp.color = android.graphics.Color.WHITE
            tp.alpha = layerAlpha
        }
    }

    override fun updateMeasureState(tp: TextPaint) {
        tp.typeface = emojiTypeface
    }
}

/**
 * High-resolution RGBA8888 font & Color Emoji rendering engine with intelligent resolution-aware LRU cache.
 * Supports dual-font rendering:
 * - SYSTEM emoji mode: letters use layer's chosen font; emojis render in native Android system emoji style.
 * - APPLICATION emoji mode: letters use layer's chosen font; emojis/symbols render using the selected app font.
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

    fun drawTextLayerOnCanvas(
        canvas: Canvas,
        layer: TextLayer,
        effectiveFontSizePx: Float,
        maxWidthPx: Float,
        emojiSourceMode: EmojiSourceMode = EmojiSourceMode.SYSTEM,
        appEmojiFontId: String? = null
    ) {
        val startNs = System.nanoTime()
        val meta = metadataIndex[layer.fontId]
        val typeface = getTypeface(layer.fontId)
        val isColorEmojiFont = meta?.classification == FontClassification.COLOR_EMOJI ||
            (meta?.colorTableType != null && meta.colorTableType != ColorTableType.NONE)

        val layerAlpha = (layer.opacity * 255f).roundToInt().coerceIn(0, 255)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            this.typeface = applyStyle(typeface, layer.isBold, layer.isItalic)
            this.textSize = effectiveFontSizePx.coerceAtLeast(8f)
            this.letterSpacing = layer.letterSpacing
            if (!isColorEmojiFont) {
                this.color = layer.colorArgb.toInt()
                this.alpha = ((android.graphics.Color.alpha(layer.colorArgb) / 255f) * layer.opacity * 255f)
                    .roundToInt().coerceIn(0, 255)
            } else {
                this.color = android.graphics.Color.WHITE
                this.alpha = layerAlpha
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

        val styledText = buildStyledSpannable(
            text = layer.text,
            layerFontId = layer.fontId,
            layerAlpha = layerAlpha,
            emojiSourceMode = emojiSourceMode,
            appEmojiFontId = appEmojiFontId,
            forStrokePass = false
        )

        val layoutWidth = maxWidthPx.roundToInt().coerceAtLeast(64)
        val alignment = when (layer.align) {
            TextAlignOption.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignOption.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val staticLayout = StaticLayout.Builder
            .obtain(styledText, 0, styledText.length, textPaint, layoutWidth)
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
            val strokeText = buildStyledSpannable(
                text = layer.text,
                layerFontId = layer.fontId,
                layerAlpha = layerAlpha,
                emojiSourceMode = emojiSourceMode,
                appEmojiFontId = appEmojiFontId,
                forStrokePass = true
            )
            val strokeLayout = StaticLayout.Builder
                .obtain(strokeText, 0, strokeText.length, strokePaint, layoutWidth)
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

    fun buildStyledSpannable(
        text: String,
        layerFontId: String,
        layerAlpha: Int = 255,
        emojiSourceMode: EmojiSourceMode = EmojiSourceMode.SYSTEM,
        appEmojiFontId: String? = null,
        forStrokePass: Boolean = false
    ): SpannableStringBuilder {
        val builder = SpannableStringBuilder(text)
        if (text.isEmpty()) return builder

        val segments = segmentEmojiRuns(text)
        val useAppFont = emojiSourceMode == EmojiSourceMode.APPLICATION && !appEmojiFontId.isNullOrBlank()
        val resolvedEmojiFontId = if (useAppFont) appEmojiFontId!! else null
        val emojiTypeface = if (resolvedEmojiFontId != null) getTypeface(resolvedEmojiFontId) else Typeface.DEFAULT
        val emojiMeta = resolvedEmojiFontId?.let { metadataIndex[it] }
        val preserveColor = if (!useAppFont) {
            // System emoji font is always color emoji
            true
        } else {
            emojiMeta?.classification == FontClassification.COLOR_EMOJI ||
                (emojiMeta?.colorTableType != null && emojiMeta.colorTableType != ColorTableType.NONE)
        }

        // If layer itself is already a dedicated color emoji font and emoji mode is APPLICATION on the same font,
        // no extra override is needed. Otherwise, isolate emoji runs cleanly!
        for (seg in segments) {
            if (!seg.isEmoji) continue
            if (useAppFont && resolvedEmojiFontId == layerFontId) continue
            builder.setSpan(
                CustomEmojiFontSpan(
                    emojiTypeface = emojiTypeface,
                    preserveOriginalColor = preserveColor,
                    transparentForStrokePass = forStrokePass,
                    layerAlpha = layerAlpha
                ),
                seg.start,
                seg.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return builder
    }

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

    companion object {
        fun segmentEmojiRuns(text: String): List<EmojiSpanSegment> {
            if (text.isEmpty()) return emptyList()
            val segments = mutableListOf<EmojiSpanSegment>()
            var idx = 0
            var currentStart = 0
            var currentEmoji: Boolean? = null

            while (idx < text.length) {
                val cp = text.codePointAt(idx)
                val charCount = Character.charCount(cp)
                val isModOrJoin = cp == 0x200D || cp == 0xFE0F || cp == 0xFE0E ||
                    cp in 0x1F3FB..0x1F3FF || cp == 0x20E3
                val isEmojiCp = isEmojiCodePoint(cp) || (isModOrJoin && currentEmoji == true)

                if (currentEmoji == null) {
                    currentEmoji = isEmojiCp
                    currentStart = idx
                } else if (currentEmoji != isEmojiCp) {
                    segments.add(EmojiSpanSegment(currentStart, idx, currentEmoji))
                    currentEmoji = isEmojiCp
                    currentStart = idx
                }
                idx += charCount
            }
            if (currentEmoji != null && currentStart < text.length) {
                segments.add(EmojiSpanSegment(currentStart, text.length, currentEmoji))
            }
            return segments
        }

        private fun isEmojiCodePoint(cp: Int): Boolean {
            return cp in 0x1F000..0x1FAFF ||
                cp in 0x2600..0x27BF ||
                cp in 0x2300..0x23FF ||
                cp in 0x2B00..0x2BFF ||
                cp in 0x1F1E6..0x1F1FF ||
                cp == 0x00A9 || cp == 0x00AE || cp == 0x203C || cp == 0x2049 ||
                cp == 0x2122 || cp == 0x2139 || cp in 0x2194..0x21AA ||
                cp in 0x25AA..0x25FE || cp in 0x2934..0x2935 || cp in 0x3030..0x3299
        }
    }
}
