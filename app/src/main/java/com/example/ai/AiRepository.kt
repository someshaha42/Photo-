package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import kotlinx.coroutines.delay
import java.util.UUID

enum class AiImageSize(val label: String, val pixels: Int) {
    SIZE_1K("1K", 1024),
    SIZE_2K("2K", 2048),
    SIZE_4K("4K", 4096)
}

enum class AiChatModel(val modelId: String, val displayName: String, val tagline: String) {
    HYDROGEN("gemini-4-hydrogen", "Gemini 4 Hydrogen", "General creative & design tasks"),
    CARBON("gemini-4-carbon", "Gemini 4 Carbon", "Deep reasoning & layout planning"),
    NEON("gemini-4-neon", "Gemini 4 Neon", "Complex interactive workflows"),
    FLASH_LITE("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite", "Fast suggestions & captions")
}

enum class AiSystemRole(val title: String, val systemInstruction: String) {
    DESIGN_DIRECTOR(
        "Design Director",
        "You are an expert graphic designer helping craft PixelLab-style posters, thumbnails, and typography compositions."
    ),
    VIDEO_COACH(
        "Video Editor Coach",
        "You are a mobile video editing specialist advising on cuts, pacing, speed ramps, and text overlays."
    ),
    TYPOGRAPHY_EXPERT(
        "Typography & Emoji Expert",
        "You specialize in pairing fonts, color emoji styling, contrast, stroke, and shadow hierarchy."
    ),
    CAPTION_WRITER(
        "Caption & Hook Writer",
        "You write concise, punchy headlines, quotes, and social media hooks."
    )
}

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val modelLabel: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class AiGeneratedImageResult(
    val bitmap: Bitmap,
    val size: AiImageSize,
    val modelId: String,
    val notice: String
)

interface AiRepository {
    val isLiveFirebaseConnected: Boolean
    suspend fun generateImage(prompt: String, size: AiImageSize): AiGeneratedImageResult
    suspend fun sendChatMessage(
        model: AiChatModel,
        role: AiSystemRole,
        history: List<AiChatMessage>,
        userMessage: String
    ): AiChatMessage
}

/**
 * Placeholder AiRepository used because Firebase setup was declined.
 * All responses are clearly labeled sample outputs with an in-app Firebase notice.
 */
class PlaceholderAiRepository : AiRepository {
    override val isLiveFirebaseConnected: Boolean = false

    override suspend fun generateImage(prompt: String, size: AiImageSize): AiGeneratedImageResult {
        delay(550L)
        val previewSide = when (size) {
            AiImageSize.SIZE_1K -> 1024
            AiImageSize.SIZE_2K -> 1280
            AiImageSize.SIZE_4K -> 1536
        }
        val bmp = Bitmap.createBitmap(previewSide, previewSide, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, previewSide.toFloat(), previewSide.toFloat(),
                intArrayOf(Color.parseColor("#141E30"), Color.parseColor("#243B55"), Color.parseColor("#0F2027")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, previewSide.toFloat(), previewSide.toFloat(), bgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = previewSide * 0.045f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80D8FF")
            textSize = previewSide * 0.028f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Sample AI Preview (${size.label})",
            previewSide / 2f,
            previewSide * 0.42f,
            titlePaint
        )
        canvas.drawText(
            prompt.take(36).ifBlank { "Creative Poster Concept" },
            previewSide / 2f,
            previewSide * 0.50f,
            subPaint
        )
        canvas.drawText(
            "Requires Firebase AI Logic (gemini-3-pro-image-preview)",
            previewSide / 2f,
            previewSide * 0.58f,
            subPaint
        )

        return AiGeneratedImageResult(
            bitmap = bmp,
            size = size,
            modelId = "gemini-3-pro-image-preview",
            notice = "Sample placeholder (${size.label} / ${size.pixels}px) — enable Firebase AI Logic for live gemini-3-pro-image-preview generation."
        )
    }

    override suspend fun sendChatMessage(
        model: AiChatModel,
        role: AiSystemRole,
        history: List<AiChatMessage>,
        userMessage: String
    ): AiChatMessage {
        delay(350L)
        val reply = buildString {
            appendLine("[Sample AI Response • ${model.displayName} • Role: ${role.title}]")
            appendLine()
            appendLine("For your request: \"${userMessage.trim()}\"")
            when (role) {
                AiSystemRole.DESIGN_DIRECTOR -> appendLine(
                    "Try pairing a bold Display headline at 48sp with subtle 4px dark stroke and a 9:16 or 1:1 canvas ratio, using a Blurred Duplicate background at 35% blur strength for depth."
                )
                AiSystemRole.VIDEO_COACH -> appendLine(
                    "Keep your hook within the first 1.8 seconds, trim dead frames from the start/end, and place high-contrast text overlays in the center safe zone."
                )
                AiSystemRole.TYPOGRAPHY_EXPERT -> appendLine(
                    "Use Roboto or Playfair Display for clean hierarchy, and apply a Color Emoji font layer with independent scale so emojis stay razor-sharp at 400%-800% zoom."
                )
                AiSystemRole.CAPTION_WRITER -> appendLine(
                    "Hook idea: \"Create Bold. Edit Without Limits.\" — short, rhythmic, and legible over high-contrast visuals."
                )
            }
            appendLine()
            append("Note: Firebase is not connected. Enable Firebase AI Logic to chat with live Gemini models.")
        }
        return AiChatMessage(
            isUser = false,
            text = reply,
            modelLabel = model.modelId
        )
    }
}
