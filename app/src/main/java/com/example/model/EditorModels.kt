package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class CanvasAspectRatio(
    val label: String,
    val widthUnits: Int,
    val heightUnits: Int,
    val exportWidth: Int,
    val exportHeight: Int
) {
    RATIO_1_1("1:1", 1, 1, 1080, 1080),
    RATIO_16_9("16:9", 16, 9, 1920, 1080),
    RATIO_9_16("9:16", 9, 16, 1080, 1920),
    RATIO_4_5("4:5", 4, 5, 1080, 1350);

    val ratioFloat: Float
        get() = widthUnits.toFloat() / heightUnits.toFloat()

    companion object {
        fun closestForDimensions(width: Int, height: Int): CanvasAspectRatio {
            if (width <= 0 || height <= 0) return RATIO_1_1
            val target = width.toFloat() / height.toFloat()
            return entries.minByOrNull { kotlin.math.abs(it.ratioFloat - target) } ?: RATIO_1_1
        }
    }
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
    RECTANGLE("Rectangle"),
    ROUNDED_RECT("Rounded Box"),
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
    val rotationDeg: Float
    val opacity: Float
    val visible: Boolean
    val locked: Boolean

    fun withBaseTransform(
        offsetXNorm: Float = this.offsetXNorm,
        offsetYNorm: Float = this.offsetYNorm,
        scale: Float = this.scale,
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
    override val name: String = "Image",
    val uriString: String,
    val originalWidth: Int,
    val originalHeight: Int,
    override val offsetXNorm: Float = 0f,
    override val offsetYNorm: Float = 0f,
    override val scale: Float = 1f,
    override val rotationDeg: Float = 0f,
    override val opacity: Float = 1f,
    override val visible: Boolean = true,
    override val locked: Boolean = false
) : EditorLayer {
    override fun withBaseTransform(
        offsetXNorm: Float,
        offsetYNorm: Float,
        scale: Float,
        rotationDeg: Float,
        opacity: Float,
        visible: Boolean,
        locked: Boolean
    ) = copy(
        offsetXNorm = offsetXNorm,
        offsetYNorm = offsetYNorm,
        scale = scale,
        rotationDeg = rotationDeg,
        opacity = opacity,
        visible = visible,
        locked = locked
    )
}

@Serializable
@SerialName("video")
data class VideoLayer(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Video",
    val uriString: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val speed: Float = 1f,
    val isMuted: Boolean = false,
    override val offsetXNorm: Float = 0f,
    override val offsetYNorm: Float = 0f,
    override val scale: Float = 1f,
    override val rotationDeg: Float = 0f,
    override val opacity: Float = 1f,
    override val visible: Boolean = true,
    override val locked: Boolean = false
) : EditorLayer {
    override fun withBaseTransform(
        offsetXNorm: Float,
        offsetYNorm: Float,
        scale: Float,
        rotationDeg: Float,
        opacity: Float,
        visible: Boolean,
        locked: Boolean
    ) = copy(
        offsetXNorm = offsetXNorm,
        offsetYNorm = offsetYNorm,
        scale = scale,
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
    override val rotationDeg: Float = 0f,
    override val opacity: Float = 1f,
    override val visible: Boolean = true,
    override val locked: Boolean = false
) : EditorLayer {
    override fun withBaseTransform(
        offsetXNorm: Float,
        offsetYNorm: Float,
        scale: Float,
        rotationDeg: Float,
        opacity: Float,
        visible: Boolean,
        locked: Boolean
    ) = copy(
        offsetXNorm = offsetXNorm,
        offsetYNorm = offsetYNorm,
        scale = scale,
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
    override val rotationDeg: Float = 0f,
    override val opacity: Float = 1f,
    override val visible: Boolean = true,
    override val locked: Boolean = false
) : EditorLayer {
    override fun withBaseTransform(
        offsetXNorm: Float,
        offsetYNorm: Float,
        scale: Float,
        rotationDeg: Float,
        opacity: Float,
        visible: Boolean,
        locked: Boolean
    ) = copy(
        offsetXNorm = offsetXNorm,
        offsetYNorm = offsetYNorm,
        scale = scale,
        rotationDeg = rotationDeg,
        opacity = opacity,
        visible = visible,
        locked = locked
    )
}

@Serializable
data class ProjectDocument(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Project",
    val aspectRatio: CanvasAspectRatio = CanvasAspectRatio.RATIO_1_1,
    val autoImageDetectionEnabled: Boolean = true,
    val manualAspectOverride: Boolean = false,
    val background: BackgroundConfig = BackgroundConfig(),
    val layers: List<EditorLayer> = emptyList(),
    val selectedLayerId: String? = null
)
