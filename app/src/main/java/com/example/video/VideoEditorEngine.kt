package com.example.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.fonts.FontRenderEngine
import com.example.model.ImageLayer
import com.example.model.ProjectDocument
import com.example.model.TextLayer
import com.example.model.VideoLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed interface VideoExportState {
    data object Idle : VideoExportState
    data class Exporting(val progressPercent: Int) : VideoExportState
    data class Completed(val outputFile: File, val galleryUri: Uri?) : VideoExportState
    data class Error(val message: String) : VideoExportState
}

@OptIn(UnstableApi::class)
class VideoEditorEngine(private val context: Context) {

    private val _exportState = MutableStateFlow<VideoExportState>(VideoExportState.Idle)
    val exportState: StateFlow<VideoExportState> = _exportState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeTransformer: Transformer? = null

    fun resetExportState() {
        _exportState.value = VideoExportState.Idle
    }

    fun cancelExport() {
        mainHandler.post {
            activeTransformer?.cancel()
            activeTransformer = null
            _exportState.value = VideoExportState.Idle
        }
    }

    suspend fun exportVideo(
        document: ProjectDocument,
        videoLayer: VideoLayer,
        fontRenderEngine: FontRenderEngine
    ) {
        _exportState.value = VideoExportState.Exporting(0)
        val overlayBitmap = withContext(Dispatchers.Default) {
            renderTextOverlaysBitmap(document, fontRenderEngine)
        }

        withContext(Dispatchers.Main) {
            val exportsDir = File(context.filesDir, "exports")
            if (!exportsDir.exists()) exportsDir.mkdirs()
            val fileName = "PixelForge_Video_${System.currentTimeMillis()}.mp4"
            val outputFile = File(exportsDir, fileName)

            val uri = if (videoLayer.uriString.startsWith("/")) {
                Uri.fromFile(File(videoLayer.uriString))
            } else {
                Uri.parse(videoLayer.uriString)
            }

            val startMs = videoLayer.trimStartMs.coerceAtLeast(0L)
            val endMs = videoLayer.trimEndMs.coerceAtLeast(startMs + 200L)

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()

            val videoEffects = mutableListOf<androidx.media3.common.Effect>()
            if (videoLayer.rotationDeg != 0f) {
                videoEffects.add(
                    ScaleAndRotateTransformation.Builder()
                        .setRotationDegrees(-videoLayer.rotationDeg)
                        .build()
                )
            }
            if (overlayBitmap != null) {
                val textureOverlays = listOf<TextureOverlay>(
                    BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
                )
                videoEffects.add(OverlayEffect(textureOverlays))
            }

            val audioProcessors = mutableListOf<AudioProcessor>()
            if (!videoLayer.isMuted && kotlin.math.abs(videoLayer.speed - 1f) > 0.05f) {
                val sonic = SonicAudioProcessor().apply {
                    setSpeed(videoLayer.speed.coerceIn(0.5f, 2f))
                    setPitch(1.0f)
                }
                audioProcessors.add(sonic)
            }

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(videoLayer.isMuted)
                .setEffects(Effects(audioProcessors, videoEffects))
                .build()

            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        activeTransformer = null
                        overlayBitmap?.recycle()
                        val galleryUri = saveVideoToMediaStore(outputFile, fileName)
                        _exportState.value = VideoExportState.Completed(outputFile, galleryUri)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        activeTransformer = null
                        overlayBitmap?.recycle()
                        _exportState.value = VideoExportState.Error(
                            exportException.localizedMessage ?: "Video export failed."
                        )
                    }
                })
                .build()

            activeTransformer = transformer
            transformer.start(editedMediaItem, outputFile.absolutePath)

            val progressHolder = ProgressHolder()
            val progressRunnable = object : Runnable {
                override fun run() {
                    val current = activeTransformer ?: return
                    val state = current.getProgress(progressHolder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        _exportState.value = VideoExportState.Exporting(progressHolder.progress)
                    }
                    if (state != Transformer.PROGRESS_STATE_NOT_STARTED && activeTransformer != null) {
                        mainHandler.postDelayed(this, 250L)
                    }
                }
            }
            mainHandler.post(progressRunnable)
        }
    }

    private fun renderTextOverlaysBitmap(
        document: ProjectDocument,
        fontRenderEngine: FontRenderEngine
    ): Bitmap? {
        val textLayers = document.layers.filter { it.visible && it is TextLayer }
        if (textLayers.isEmpty()) return null
        val w = document.aspectRatio.exportWidth
        val h = document.aspectRatio.exportHeight
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        for (layer in textLayers) {
            if (layer is TextLayer) {
                canvas.save()
                canvas.translate(w * (0.5f + layer.offsetXNorm), h * (0.5f + layer.offsetYNorm))
                canvas.rotate(layer.rotationDeg)
                canvas.scale(layer.scale, layer.scale)
                fontRenderEngine.drawTextLayerOnCanvas(
                    canvas = canvas,
                    layer = layer,
                    effectiveFontSizePx = layer.fontSizeSp * (w / 360f),
                    maxWidthPx = w * 0.9f
                )
                canvas.restore()
            }
        }
        return bmp
    }

    private fun saveVideoToMediaStore(file: File, displayName: String): Uri? {
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/PixelForge")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                    resolver.update(uri, done, null, null)
                }
            }
            uri
        }.getOrNull()
    }
}
