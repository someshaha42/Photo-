package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.roundToInt

@Serializable
data class CanvasDimensions(
    val isCustom: Boolean = false,
    val ratioWidth: Int = 1,
    val ratioHeight: Int = 1,
    val exportWidth: Int = 1080,
    val exportHeight: Int = 1080
) {
    val label: String
        get() = "$ratioWidth:$ratioHeight"

    val ratioFloat: Float
        get() = ratioWidth.coerceAtLeast(1).toFloat() / ratioHeight.coerceAtLeast(1).toFloat()

    val pixelLabel: String
        get() = "${exportWidth}x${exportHeight}"

    companion object {
        val SQUARE_1_1 = CanvasDimensions(
            isCustom = false,
            ratioWidth = 1,
            ratioHeight = 1,
            exportWidth = 1080,
            exportHeight = 1080
        )

        fun fromRatioInput(raw: String): CanvasDimensions? {
            val parts = raw.trim()
                .replace('/', ':')
                .replace('x', ':')
                .replace('X', ':')
                .split(':')
                .mapNotNull { it.trim().toFloatOrNull() }
            if (parts.size != 2) return null
            val rwRaw = parts[0]
            val rhRaw = parts[1]
            if (rwRaw <= 0f || rhRaw <= 0f) return null

            val scaleToInt = if (rwRaw % 1f != 0f || rhRaw % 1f != 0f) 100f else 1f
            val intW = (rwRaw * scaleToInt).roundToInt().coerceAtLeast(1)
            val intH = (rhRaw * scaleToInt).roundToInt().coerceAtLeast(1)
            val divisor = gcd(intW, intH)
            val rw = (intW / divisor).coerceAtLeast(1)
            val rh = (intH / divisor).coerceAtLeast(1)

            val (expW, expH) = computeExportPixelsFromRatio(rw, rh)
            return CanvasDimensions(
                isCustom = !(rw == 1 && rh == 1),
                ratioWidth = rw,
                ratioHeight = rh,
                exportWidth = expW,
                exportHeight = expH
            )
        }

        fun fromPixelInput(raw: String): CanvasDimensions? {
            val parts = raw.trim()
                .split(Regex("[xX×*,\\s]+"))
                .mapNotNull { it.trim().toIntOrNull() }
            if (parts.size != 2) return null
            val w = parts[0].coerceIn(64, 4096)
            val h = parts[1].coerceIn(64, 4096)
            if (parts[0] <= 0 || parts[1] <= 0) return null

            val divisor = gcd(w, h)
            val rw = (w / divisor).coerceAtLeast(1)
            val rh = (h / divisor).coerceAtLeast(1)
            return CanvasDimensions(
                isCustom = !(rw == 1 && rh == 1 && w == 1080 && h == 1080),
                ratioWidth = rw,
                ratioHeight = rh,
                exportWidth = w,
                exportHeight = h
            )
        }

        fun computeExportPixelsFromRatio(rw: Int, rh: Int): Pair<Int, Int> {
            val baseShort = 1080
            val ratio = rw.toDouble() / rh.toDouble()
            val (w, h) = if (rw >= rh) {
                (baseShort * ratio).roundToInt() to baseShort
            } else {
                baseShort to (baseShort / ratio).roundToInt()
            }
            val maxDim = 4096
            return if (w > maxDim || h > maxDim) {
                val scale = maxDim.toDouble() / maxOf(w, h)
                (w * scale).roundToInt().coerceAtLeast(64) to (h * scale).roundToInt().coerceAtLeast(64)
            } else {
                w.coerceAtLeast(64) to h.coerceAtLeast(64)
            }
        }

        private fun gcd(a: Int, b: Int): Int {
            var x = kotlin.math.abs(a)
            var y = kotlin.math.abs(b)
            while (y != 0) {
                val t = x % y
                x = y
                y = t
            }
            return x.coerceAtLeast(1)
        }
    }
}

@Serializable
enum class EmojiSourceMode(val label: String) {
    SYSTEM("System"),
    APPLICATION("Application")
}

enum class HorizontalAlignOption(val label: String) {
    LEFT("Align Left"),
    CENTER("Align Center"),
    RIGHT("Align Right")
}

enum class TextCaseMode {
    LOWERCASE,
    UPPERCASE,
    TITLE_CASE;

    fun next(): TextCaseMode = when (this) {
        LOWERCASE -> UPPERCASE
        UPPERCASE -> TITLE_CASE
        TITLE_CASE -> LOWERCASE
    }
}

object TextCaseTransformer {
    fun apply(text: String, mode: TextCaseMode): String {
        return when (mode) {
            TextCaseMode.LOWERCASE -> text.lowercase()
            TextCaseMode.UPPERCASE -> text.uppercase()
            TextCaseMode.TITLE_CASE -> toTitleCaseWords(text)
        }
    }

    private fun toTitleCaseWords(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        var capitalizeNext = true
        var i = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            val charCount = Character.charCount(cp)
            if (Character.isLetter(cp)) {
                val mapped = if (capitalizeNext) {
                    Character.toTitleCase(cp)
                } else {
                    Character.toLowerCase(cp)
                }
                sb.appendCodePoint(mapped)
                capitalizeNext = false
            } else {
                sb.appendCodePoint(cp)
                if (Character.isWhitespace(cp) || cp == '-'.code || cp == '/'.code || cp == '_'.code) {
                    capitalizeNext = true
                } else if (!Character.isDigit(cp) && cp != '\''.code && cp != '\u2019'.code) {
                    capitalizeNext = true
                }
            }
            i += charCount
        }
        return sb.toString()
    }
}

@Serializable
data class ImageAdjustments(
    val exposure: Float = 0f,     // -1f..1f
    val contrast: Float = 0f,     // -1f..1f
    val saturation: Float = 0f,   // -1f..1f
    val hue: Float = 0f,          // -180f..180f
    val vibrance: Float = 0f,     // -1f..1f
    val sharpness: Float = 0f,    // 0f..1f
    val vignette: Float = 0f,     // 0f..1f
    val brightness: Float = 0f,   // -1f..1f
    val warmth: Float = 0f        // -1f..1f
) {
    fun isIdentity(): Boolean {
        return exposure == 0f &&
            contrast == 0f &&
            saturation == 0f &&
            hue == 0f &&
            vibrance == 0f &&
            sharpness == 0f &&
            vignette == 0f &&
            brightness == 0f &&
            warmth == 0f
    }
}

@Serializable
data class ImageCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    fun isFull(): Boolean = left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f

    fun normalized(): ImageCropRect {
        val l = left.coerceIn(0f, 0.85f)
        val t = top.coerceIn(0f, 0.85f)
        val r = right.coerceIn((l + 0.15f).coerceAtMost(1f), 1f)
        val b = bottom.coerceIn((t + 0.15f).coerceAtMost(1f), 1f)
        return ImageCropRect(l, t, r, b)
    }
}

@Serializable
enum class ImageCropShape {
    RECTANGLE,
    CIRCLE
}

@Serializable
enum class BackgroundTab(val title: String) {
    TRANSPARENT("Transparent"),
    BLUR("Blur"),
    COLORS("Colors"),
    IMAGE("Image")
}

@Serializable
enum class EditorBlendMode(val label: String) {
    NORMAL("Normal"),
    MULTIPLY("Multiply"),
    SCREEN("Screen"),
    OVERLAY("Overlay"),
    SOFT_LIGHT("Soft Light"),
    DARKEN("Darken")
}

@Serializable
data class BackgroundConfig(
    val activeTab: BackgroundTab = BackgroundTab.TRANSPARENT,
    val checkerboardPreviewEnabled: Boolean = true,
    val checkerboardOpacity: Float = 1f,
    val blurStrength: Float = 0.35f,
    val blurBackgroundOpacity: Float = 1f,
    val colorArgb: Long = 0xFFFF4D4F,
    val colorOpacity: Float = 1f,
    val backgroundImageUri: String? = null,
    val backgroundImageOpacity: Float = 1f,
    val blendMode: EditorBlendMode = EditorBlendMode.NORMAL
)

@Serializable
enum class TextAlignOption(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

@Serializable
enum class LayerShapeType(val label: String) {
    RECTANGLE("Rect"),
    ROUNDED_RECT("Rounded"),
    CIRCLE("Circle"),
    STAR("Star"),
    BADGE("Badge")
}

@Serializable
sealed interface EditorLayer {
    val id: String
    val name: String
    val offsetXNorm: Float
    val offsetYNorm: Float
    val scale: Float
    val stretchX: Float
    val stretchY: Float
    val rotationDeg: Float
    val opacity: Float
    val visible: Boolean
    val locked: Boolean

    fun withBaseTransform(
        offsetXNorm: Float = this.offsetXNorm,
        offsetYNorm: Float = this.offsetYNorm,
        scale: Float = this.scale,
        stretchX: Float = this.stretchX,
        stretchY: Float = this.stretchY,
        rotationDeg: Float = this.rotationDeg,
        opacity: Float = this.opacity,
        visible: Boolean = this.visible,
        locked: Boolean = this.locked
    ): EditorLayer
}

@Serializable
@SerialName("image")
data class ImageLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Photo",
    val uriString: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val adjustments: ImageAdjustments = ImageAdjustments(),
    val crop: ImageCropRect = ImageCropRect(),
    val cropShape: ImageCropShape = ImageCropShape.RECTANGLE,
    val tintColorArgb: Long? = null,
    override val offsetXNorm: Float = 0f,
    override val offsetYNorm: Float = 0f,
    override val scale: Float = 1f,
    override val stretchX: Float = 1f,
    override val stretchY: Float = 1f,
    override val rotationDeg: Float = 0f,
    override val opacity: Float = 1f,
    override val visible: Boolean = true,
    override val locked: Boolean = false
) : EditorLayer {
    override fun withBaseTransform(
        offsetXNorm: Float,
        offsetYNorm: Float,
        scale: Float,
        stretchX: Float,
        stretchY: Float,
        rotationDeg: Float,
        opacity: Float,
        visible: Boolean,
        locked: Boolean
    ) = copy(
        offsetXNorm = offsetXNorm,
        offsetYNorm = offsetYNorm,
        scale = scale,
        stretchX = stretchX,
        stretchY = stretchY,
        rotationDeg = rotationDeg,
        opacity = opacity,
        visible = visible,
        locked = locked
    )
}

@Serializable
@SerialName("text")
data class TextLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Text",
    val text: String = "New Text",
    val fontId: String = "bundled:Roboto.ttf",
    val fontSizeSp: Float = 36f,
    val colorArgb: Long = 0xFFFFFFFF,
    val strokeColorArgb: Long = 0xFF000000,
    val strokeWidth: Float = 0f,
    val shadowEnabled: Boolean = false,
    val shadowColorArgb: Long = 0xAA000000,
    val shadowRadius: Float = 8f,
    val letterSpacing: Float = 0f,
    val align: TextAlignOption = TextAlignOption.CENTER,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val bgPillEnabled: Boolean = false,
    val bgPillColorArgb: Long = 0xAA161B26,
    override val offsetXNorm: Float = 0f,
    override val offsetYNorm: Float = 0f,
    override val scale: Float = 1f,
    override val stretchX: Float = 1f,
    override val stretchY: Float = 1f,
    override val rotationDeg: Float = 0f,
    override val opacity: Float = 1f,
    override val visible: Boolean = true,
    override val locked: Boolean = false
) : EditorLayer {
    override fun withBaseTransform(
        offsetXNorm: Float,
        offsetYNorm: Float,
        scale: Float,
        stretchX: Float,
        stretchY: Float,
        rotationDeg: Float,
        opacity: Float,
        visible: Boolean,
        locked: Boolean
    ) = copy(
        offsetXNorm = offsetXNorm,
        offsetYNorm = offsetYNorm,
        scale = scale,
        stretchX = stretchX,
        stretchY = stretchY,
        rotationDeg = rotationDeg,
        opacity = opacity,
        visible = visible,
        locked = locked
    )
}

@Serializable
@SerialName("shape")
data class ShapeLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Shape",
    val shapeType: LayerShapeType = LayerShapeType.ROUNDED_RECT,
    val fillColorArgb: Long = 0xFF2D8CFF,
    val strokeColorArgb: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 4f,
    override val offsetXNorm: Float = 0f,
    override val offsetYNorm: Float = 0f,
    override val scale: Float = 1f,
    override val stretchX: Float = 1f,
    override val stretchY: Float = 1f,
    override val rotationDeg: Float = 0f,
    override val opacity: Float = 1f,
    override val visible: Boolean = true,
    override val locked: Boolean = false
) : EditorLayer {
    override fun withBaseTransform(
        offsetXNorm: Float,
        offsetYNorm: Float,
        scale: Float,
        stretchX: Float,
        stretchY: Float,
        rotationDeg: Float,
        opacity: Float,
        visible: Boolean,
        locked: Boolean
    ) = copy(
        offsetXNorm = offsetXNorm,
        offsetYNorm = offsetYNorm,
        scale = scale,
        stretchX = stretchX,
        stretchY = stretchY,
        rotationDeg = rotationDeg,
        opacity = opacity,
        visible = visible,
        locked = locked
    )
}

@Serializable
@SerialName("group")
data class GroupLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Group",
    val children: List<EditorLayer> = emptyList(),
    val isExpanded: Boolean = false,
    override val offsetXNorm: Float = 0f,
    override val offsetYNorm: Float = 0f,
    override val scale: Float = 1f,
    override val stretchX: Float = 1f,
    override val stretchY: Float = 1f,
    override val rotationDeg: Float = 0f,
    override val opacity: Float = 1f,
    override val visible: Boolean = true,
    override val locked: Boolean = false
) : EditorLayer {
    override fun withBaseTransform(
        offsetXNorm: Float,
        offsetYNorm: Float,
        scale: Float,
        stretchX: Float,
        stretchY: Float,
        rotationDeg: Float,
        opacity: Float,
        visible: Boolean,
        locked: Boolean
    ) = copy(
        offsetXNorm = offsetXNorm,
        offsetYNorm = offsetYNorm,
        scale = scale,
        stretchX = stretchX,
        stretchY = stretchY,
        rotationDeg = rotationDeg,
        opacity = opacity,
        visible = visible,
        locked = locked
    )
}

@Serializable
data class DrawPoint(
    val xNorm: Float,
    val yNorm: Float
)

@Serializable
data class DrawStroke(
    val colorArgb: Long = 0xFFFF4D4FL,
    val strokeWidthNorm: Float = 0.012f,
    val points: List<DrawPoint> = emptyList()
)

@Serializable
@SerialName("draw")
data class DrawLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Draw",
    val strokes: List<DrawStroke> = emptyList(),
    override val offsetXNorm: Float = 0f,
    override val offsetYNorm: Float = 0f,
    override val scale: Float = 1f,
    override val stretchX: Float = 1f,
    override val stretchY: Float = 1f,
    override val rotationDeg: Float = 0f,
    override val opacity: Float = 1f,
    override val visible: Boolean = true,
    override val locked: Boolean = false
) : EditorLayer {
    override fun withBaseTransform(
        offsetXNorm: Float,
        offsetYNorm: Float,
        scale: Float,
        stretchX: Float,
        stretchY: Float,
        rotationDeg: Float,
        opacity: Float,
        visible: Boolean,
        locked: Boolean
    ) = copy(
        offsetXNorm = offsetXNorm,
        offsetYNorm = offsetYNorm,
        scale = scale,
        stretchX = stretchX,
        stretchY = stretchY,
        rotationDeg = rotationDeg,
        opacity = opacity,
        visible = visible,
        locked = locked
    )
}

enum class ExportImageFormat(val label: String, val ext: String, val mimeType: String) {
    PNG("PNG", "png", "image/png"),
    JPG("JPG", "jpg", "image/jpeg")
}

enum class ExportResolutionPreset(val label: String) {
    RES_4K("4K"),
    RES_2K("2K"),
    CUSTOM("Custom")
}

@Serializable
data class ProjectDocument(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Project",
    val canvas: CanvasDimensions = CanvasDimensions.SQUARE_1_1,
    val background: BackgroundConfig = BackgroundConfig(),
    val layers: List<EditorLayer> = emptyList(),
    val selectedLayerId: String? = null,
    val selectionOrder: List<String> = emptyList(),
    val emojiSourceMode: EmojiSourceMode = EmojiSourceMode.SYSTEM,
    val appEmojiFontId: String? = null
) {
    val effectiveSelectionOrder: List<String>
        get() = if (selectionOrder.isNotEmpty()) selectionOrder else listOfNotNull(selectedLayerId)
}
