package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.fonts.FontRenderEngine
import com.example.model.BackgroundTab
import com.example.model.EditorBlendMode
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.model.VideoLayer
import com.example.ui.theme.CheckerDark1
import com.example.ui.theme.CheckerDark2
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import java.io.File
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun EditorCanvasViewport(
    document: ProjectDocument,
    imageBitmaps: Map<String, Bitmap>,
    blurredPreview: Bitmap?,
    fontRenderEngine: FontRenderEngine,
    canvasZoom: Float,
    showGrid: Boolean,
    isEyedropperActive: Boolean,
    isVideoPlaying: Boolean,
    onSelectLayer: (String?) -> Unit,
    onTransformLayer: (layerId: String, dxNorm: Float, dyNorm: Float, scale: Float, rotation: Float) -> Unit,
    onEyedropperTap: (normX: Float, normY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = document.aspectRatio.ratioFloat
    val density = LocalDensity.current.density

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val availW = maxWidth
        val availH = maxHeight
        val boxModifier = if (availW / availH > ratio) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(ratio)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
        }

        Box(
            modifier = boxModifier
                .graphicsLayer {
                    scaleX = canvasZoom
                    scaleY = canvasZoom
                }
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width = 1.5.dp,
                    color = if (isEyedropperActive) StudioAmber else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .clipToBounds()
                .pointerInput(isEyedropperActive) {
                    detectTapGestures { offset ->
                        if (isEyedropperActive) {
                            val nx = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val ny = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            onEyedropperTap(nx, ny)
                        } else {
                            onSelectLayer(null)
                        }
                    }
                }
        ) {
            // 1. Non-exportable Dark Checkerboard Transparency Preview (#202020 & #303030)
            if (document.background.checkerboardPreviewEnabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cell = 12.dp.toPx()
                    val cols = (size.width / cell).toInt() + 1
                    val rows = (size.height / cell).toInt() + 1
                    val alpha = document.background.checkerboardOpacity.coerceIn(0f, 1f)
                    for (r in 0..rows) {
                        for (c in 0..cols) {
                            val col = if ((r + c) % 2 == 0) CheckerDark1 else CheckerDark2
                            drawRect(
                                color = col.copy(alpha = alpha),
                                topLeft = Offset(c * cell, r * cell),
                                size = Size(cell, cell)
                            )
                        }
                    }
                }
            }

            // 2. Background Layer (Blend Mode applies ONLY to background layer)
            val bg = document.background
            val composeBlend = remember(bg.blendMode) { mapComposeBlendMode(bg.blendMode) }
            when (bg.activeTab) {
                BackgroundTab.TRANSPARENT -> Unit
                BackgroundTab.COLORS -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = Color(bg.colorArgb.toInt()).copy(alpha = bg.colorOpacity),
                            blendMode = composeBlend
                        )
                    }
                }
                BackgroundTab.BLUR -> {
                    val blurBmp = blurredPreview
                    if (blurBmp != null) {
                        Image(
                            bitmap = blurBmp.asImageBitmap(),
                            contentDescription = "Blurred background",
                            contentScale = ContentScale.Crop,
                            alpha = bg.blurBackgroundOpacity,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                BackgroundTab.IMAGE -> {
                    val bgBmp = bg.backgroundImageUri?.let { imageBitmaps[it] }
                    if (bgBmp != null) {
                        Image(
                            bitmap = bgBmp.asImageBitmap(),
                            contentDescription = "Background image",
                            contentScale = ContentScale.Crop,
                            alpha = bg.backgroundImageOpacity,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 3. Layers Stack (Images, Videos, Shapes, Text/Color Emojis)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val canvasWpx = with(LocalDensity.current) { maxWidth.toPx() }
                val canvasHpx = with(LocalDensity.current) { maxHeight.toPx() }
                val baseUnitPx = min(canvasWpx, canvasHpx)

                document.layers.forEach { layer ->
                    if (!layer.visible) return@forEach
                    val isSelected = layer.id == document.selectedLayerId && !isEyedropperActive

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = layer.offsetXNorm * canvasWpx
                                translationY = layer.offsetYNorm * canvasHpx
                                rotationZ = layer.rotationDeg
                                scaleX = layer.scale
                                scaleY = layer.scale
                                alpha = layer.opacity
                            }
                            .pointerInput(layer.id, layer.locked, isEyedropperActive) {
                                if (isEyedropperActive) return@pointerInput
                                detectTapGestures(
                                    onTap = { onSelectLayer(layer.id) }
                                )
                            }
                            .pointerInput(layer.id, layer.locked, isEyedropperActive, canvasWpx, canvasHpx) {
                                if (isEyedropperActive || layer.locked) return@pointerInput
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    onSelectLayer(layer.id)
                                    onTransformLayer(
                                        layer.id,
                                        pan.x / canvasWpx.coerceAtLeast(1f),
                                        pan.y / canvasHpx.coerceAtLeast(1f),
                                        zoom,
                                        rotation
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (layer) {
                            is ImageLayer -> {
                                val bmp = imageBitmaps[layer.uriString]
                                if (bmp != null) {
                                    // Preserve original image proportions & resolution; changing canvas aspect ratio never stretches/crops!
                                    val imgRatio = bmp.width.toFloat() / bmp.height.coerceAtLeast(1)
                                    val fitModifier = if (imgRatio >= 1f) {
                                        Modifier.size(
                                            width = with(LocalDensity.current) { (baseUnitPx * 0.85f).toDp() },
                                            height = with(LocalDensity.current) { (baseUnitPx * 0.85f / imgRatio).toDp() }
                                        )
                                    } else {
                                        Modifier.size(
                                            width = with(LocalDensity.current) { (baseUnitPx * 0.85f * imgRatio).toDp() },
                                            height = with(LocalDensity.current) { (baseUnitPx * 0.85f).toDp() }
                                        )
                                    }
                                    Box(
                                        modifier = fitModifier
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, StudioAzure, RoundedCornerShape(4.dp))
                                                else Modifier
                                            )
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = layer.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                            is VideoLayer -> {
                                val vidRatio = layer.originalWidth.toFloat() / layer.originalHeight.coerceAtLeast(1)
                                val fitW = if (vidRatio >= 1f) baseUnitPx * 0.85f else baseUnitPx * 0.85f * vidRatio
                                val fitH = if (vidRatio >= 1f) baseUnitPx * 0.85f / vidRatio else baseUnitPx * 0.85f
                                Box(
                                    modifier = Modifier
                                        .size(
                                            width = with(LocalDensity.current) { fitW.toDp() },
                                            height = with(LocalDensity.current) { fitH.toDp() }
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, StudioAzure, RoundedCornerShape(4.dp))
                                            else Modifier
                                        )
                                ) {
                                    VideoLayerPlayer(
                                        layer = layer,
                                        isPlaying = isVideoPlaying,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            is ShapeLayer -> {
                                val shapePx = baseUnitPx * 0.34f
                                val shapeDp = with(LocalDensity.current) { shapePx.toDp() }
                                Canvas(
                                    modifier = Modifier
                                        .size(shapeDp)
                                        .then(
                                            if (isSelected) Modifier.border(1.5.dp, StudioAzure, RoundedCornerShape(4.dp))
                                            else Modifier
                                        )
                                ) {
                                    val fill = Color(layer.fillColorArgb.toInt())
                                    val stroke = Color(layer.strokeColorArgb.toInt())
                                    val strokeW = layer.strokeWidth.dp.toPx()
                                    when (layer.shapeType) {
                                        LayerShapeType.RECTANGLE -> {
                                            drawRect(color = fill)
                                            if (strokeW > 0f) drawRect(color = stroke, style = Stroke(strokeW))
                                        }
                                        LayerShapeType.ROUNDED_RECT -> {
                                            val r = size.minDimension * 0.18f
                                            drawRoundRect(color = fill, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
                                            if (strokeW > 0f) drawRoundRect(color = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r), style = Stroke(strokeW))
                                        }
                                        LayerShapeType.CIRCLE -> {
                                            drawCircle(color = fill)
                                            if (strokeW > 0f) drawCircle(color = stroke, style = Stroke(strokeW))
                                        }
                                        LayerShapeType.STAR, LayerShapeType.BADGE -> {
                                            val pts = if (layer.shapeType == LayerShapeType.STAR) 5 else 8
                                            val inner = if (layer.shapeType == LayerShapeType.STAR) 0.45f else 0.78f
                                            val path = Path()
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            val outerR = size.minDimension / 2f
                                            val total = pts * 2
                                            for (i in 0 until total) {
                                                val rad = if (i % 2 == 0) outerR else outerR * inner
                                                val ang = Math.toRadians((i * 360.0 / total) - 90.0)
                                                val px = cx + (rad * cos(ang)).toFloat()
                                                val py = cy + (rad * sin(ang)).toFloat()
                                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                            }
                                            path.close()
                                            drawPath(path, color = fill)
                                            if (strokeW > 0f) drawPath(path, color = stroke, style = Stroke(strokeW))
                                        }
                                    }
                                }
                            }

                            is TextLayer -> {
                                // Render Text & Color Emoji directly from original Typeface onto nativeCanvas
                                // at high effective resolution so zoom/scale never pixelates vector emojis!
                                Canvas(
                                    modifier = Modifier
                                        .size(
                                            width = with(LocalDensity.current) { (canvasWpx * 0.9f).toDp() },
                                            height = with(LocalDensity.current) { (baseUnitPx * 0.48f).toDp() }
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(1.5.dp, StudioAzure, RoundedCornerShape(6.dp))
                                            else Modifier
                                        )
                                ) {
                                    drawIntoCanvas { canvas ->
                                        canvas.nativeCanvas.save()
                                        canvas.nativeCanvas.translate(size.width / 2f, size.height / 2f)
                                        fontRenderEngine.drawTextLayerOnCanvas(
                                            canvas = canvas.nativeCanvas,
                                            layer = layer,
                                            effectiveFontSizePx = layer.fontSizeSp * density,
                                            maxWidthPx = size.width
                                        )
                                        canvas.nativeCanvas.restore()
                                    }
                                }
                            }
                        }

                        if (isSelected && layer.locked) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Locked",
                                tint = StudioAmber,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            }

            // 4. Optional Alignment Grid Overlay
            if (showGrid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 1.dp.toPx()
                    val gridColor = Color.White.copy(alpha = 0.25f)
                    for (i in 1..2) {
                        val x = size.width * (i / 3f)
                        val y = size.height * (i / 3f)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
                    }
                }
            }

            // 5. Eyedropper Badge
            if (isEyedropperActive) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = StudioAmber,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Tap canvas to sample color",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoLayerPlayer(
    layer: VideoLayer,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(layer.id) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(layer.uriString, layer.trimStartMs, layer.trimEndMs) {
        val uri = if (layer.uriString.startsWith("/")) {
            Uri.fromFile(File(layer.uriString))
        } else {
            Uri.parse(layer.uriString)
        }
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(layer.trimStartMs.coerceAtLeast(0L))
                    .setEndPositionMs(layer.trimEndMs.coerceAtLeast(layer.trimStartMs + 200L))
                    .build()
            )
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    LaunchedEffect(layer.speed, layer.isMuted, isPlaying) {
        player.playbackParameters = PlaybackParameters(layer.speed.coerceIn(0.5f, 2f))
        player.volume = if (layer.isMuted) 0f else 1f
        player.playWhenReady = isPlaying
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.player = player
            }
        },
        update = { pv ->
            if (pv.player !== player) pv.player = player
        },
        modifier = modifier
    )
}

private fun mapComposeBlendMode(mode: EditorBlendMode): BlendMode {
    return when (mode) {
        EditorBlendMode.NORMAL -> BlendMode.SrcOver
        EditorBlendMode.MULTIPLY -> BlendMode.Multiply
        EditorBlendMode.SCREEN -> BlendMode.Screen
        EditorBlendMode.OVERLAY -> BlendMode.Overlay
        EditorBlendMode.SOFT_LIGHT -> BlendMode.Softlight
        EditorBlendMode.DARKEN -> BlendMode.Darken
    }
}
