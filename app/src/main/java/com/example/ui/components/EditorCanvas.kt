package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.MediaStorageManager
import com.example.fonts.FontRenderEngine
import com.example.model.BackgroundTab
import com.example.model.EditorBlendMode
import com.example.model.EditorLayer
import com.example.model.GroupLayer
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.ui.theme.CheckerDark1
import com.example.ui.theme.CheckerDark2
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioEmerald
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EditorCanvasViewport(
    document: ProjectDocument,
    imageBitmaps: Map<String, Bitmap>,
    blurredPreview: Bitmap?,
    fontRenderEngine: FontRenderEngine,
    isZoomMode: Boolean,
    canvasZoom: Float,
    canvasPan: Pair<Float, Float>,
    isEyedropperActive: Boolean,
    onCanvasViewportTransform: (zoomDelta: Float, panDx: Float, panDy: Float) -> Unit,
    onSelectLayer: (String?) -> Unit,
    onLayerTapped: (String) -> Unit = onSelectLayer,
    onEmptyCanvasTap: () -> Unit = { onSelectLayer(null) },
    onTransformLayer: (
        layerId: String,
        dxNorm: Float,
        dyNorm: Float,
        scale: Float,
        rotation: Float,
        stretchX: Float,
        stretchY: Float
    ) -> Unit,
    onEyedropperTap: (normX: Float, normY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = document.canvas.ratioFloat
    val selectedSet = remember(document.selectionOrder, document.selectedLayerId) {
        document.effectiveSelectionOrder.toSet()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .then(
                if (isZoomMode) {
                    Modifier.pointerInput(isZoomMode) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            onCanvasViewportTransform(zoom, pan.x, pan.y)
                        }
                    }
                } else if (!isEyedropperActive) {
                    Modifier.pointerInput(isZoomMode, isEyedropperActive) {
                        detectTapGestures { onEmptyCanvasTap() }
                    }
                } else Modifier
            )
            .padding(8.dp),
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
                    translationX = canvasPan.first
                    translationY = canvasPan.second
                }
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width = 1.5.dp,
                    color = when {
                        isEyedropperActive -> StudioAmber
                        isZoomMode -> StudioAzure
                        else -> MaterialTheme.colorScheme.outline
                    },
                    shape = RoundedCornerShape(4.dp)
                )
                .clipToBounds()
                .pointerInput(isEyedropperActive, isZoomMode) {
                    if (isZoomMode) return@pointerInput
                    detectTapGestures { offset ->
                        if (isEyedropperActive) {
                            val nx = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val ny = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                            onEyedropperTap(nx, ny)
                        } else {
                            onEmptyCanvasTap()
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

            // 2. Background Layer
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
                            contentDescription = "Blurred Background",
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
                            contentDescription = "Background Photo",
                            contentScale = ContentScale.Crop,
                            alpha = bg.backgroundImageOpacity,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 3. Layers Stack (Photos, Shapes, Text/Color Emojis, Groups)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val canvasWpx = with(LocalDensity.current) { maxWidth.toPx() }
                val canvasHpx = with(LocalDensity.current) { maxHeight.toPx() }
                val baseUnitPx = min(canvasWpx, canvasHpx)

                document.layers.forEach { layer ->
                    RenderCanvasLayerNode(
                        layer = layer,
                        document = document,
                        selectedSet = selectedSet,
                        primarySelectedId = document.selectedLayerId,
                        parentGroup = null,
                        parentLocked = false,
                        canvasWpx = canvasWpx,
                        canvasHpx = canvasHpx,
                        baseUnitPx = baseUnitPx,
                        imageBitmaps = imageBitmaps,
                        fontRenderEngine = fontRenderEngine,
                        isZoomMode = isZoomMode,
                        isEyedropperActive = isEyedropperActive,
                        onSelectLayer = onSelectLayer,
                        onLayerTapped = onLayerTapped,
                        onTransformLayer = onTransformLayer
                    )
                }
            }

            if (isEyedropperActive) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = StudioAmber,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Tap color",
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

@Composable
private fun RenderCanvasLayerNode(
    layer: EditorLayer,
    document: ProjectDocument,
    selectedSet: Set<String>,
    primarySelectedId: String?,
    parentGroup: GroupLayer?,
    parentLocked: Boolean,
    canvasWpx: Float,
    canvasHpx: Float,
    baseUnitPx: Float,
    imageBitmaps: Map<String, Bitmap>,
    fontRenderEngine: FontRenderEngine,
    isZoomMode: Boolean,
    isEyedropperActive: Boolean,
    onSelectLayer: (String?) -> Unit,
    onLayerTapped: (String) -> Unit,
    onTransformLayer: (
        layerId: String,
        dxNorm: Float,
        dyNorm: Float,
        scale: Float,
        rotation: Float,
        stretchX: Float,
        stretchY: Float
    ) -> Unit
) {
    if (!layer.visible) return
    val density = LocalDensity.current.density
    val interactiveTarget = parentGroup ?: layer
    val interactiveTargetId = interactiveTarget.id
    val effectiveLocked = parentLocked || layer.locked || (parentGroup?.locked == true)
    val isPrimarySelected = (interactiveTargetId == primarySelectedId) && !isEyedropperActive && !isZoomMode
    val isMultiSelected = (interactiveTargetId in selectedSet) && !isEyedropperActive && !isZoomMode
    val showDots = isPrimarySelected || isMultiSelected
    val dotAccent = if (isPrimarySelected) StudioAzure else StudioEmerald

    val liveTargetLayer by rememberUpdatedState(interactiveTarget)
    val liveChildLayer by rememberUpdatedState(layer)

    // Compensates pointer pan from rotated+scaled local Box space back into canvas space so
    // finger drag tracks 1:1 even when deeply zoomed in!
    val bodyGestureModifier = if (!isZoomMode && !isEyedropperActive) {
        if (layer is GroupLayer && layer.isExpanded && parentGroup == null) {
            Modifier
        } else {
            Modifier
                .pointerInput(interactiveTargetId, isEyedropperActive) {
                    detectTapGestures(onTap = { onLayerTapped(interactiveTargetId) })
                }
                .pointerInput(interactiveTargetId, effectiveLocked, canvasWpx, canvasHpx) {
                    if (effectiveLocked) return@pointerInput
                    detectTransformGestures { _, pan, zoom, rotation ->
                        onSelectLayer(interactiveTargetId)
                        val t = liveTargetLayer
                        val c = liveChildLayer
                        val sx = t.scale * t.stretchX * if (parentGroup != null) c.scale * c.stretchX else 1f
                        val sy = t.scale * t.stretchY * if (parentGroup != null) c.scale * c.stretchY else 1f
                        val totalRotDeg = t.rotationDeg + if (parentGroup != null) c.rotationDeg else 0f
                        val unscaledX = pan.x * sx
                        val unscaledY = pan.y * sy
                        val rad = Math.toRadians(totalRotDeg.toDouble())
                        val dxCanvasPx = (unscaledX * cos(rad) - unscaledY * sin(rad)).toFloat()
                        val dyCanvasPx = (unscaledX * sin(rad) + unscaledY * cos(rad)).toFloat()
                        onTransformLayer(
                            interactiveTargetId,
                            dxCanvasPx / canvasWpx.coerceAtLeast(1f),
                            dyCanvasPx / canvasHpx.coerceAtLeast(1f),
                            zoom,
                            rotation,
                            1f,
                            1f
                        )
                    }
                }
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = layer.offsetXNorm * canvasWpx
                translationY = layer.offsetYNorm * canvasHpx
                rotationZ = layer.rotationDeg
                scaleX = layer.scale * layer.stretchX
                scaleY = layer.scale * layer.stretchY
                alpha = layer.opacity
            },
        contentAlignment = Alignment.Center
    ) {
        when (layer) {
            is GroupLayer -> {
                val childForwardParent = if (layer.isExpanded) null else layer
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    layer.children.forEach { child ->
                        RenderCanvasLayerNode(
                            layer = child,
                            document = document,
                            selectedSet = selectedSet,
                            primarySelectedId = primarySelectedId,
                            parentGroup = childForwardParent,
                            parentLocked = effectiveLocked,
                            canvasWpx = canvasWpx,
                            canvasHpx = canvasHpx,
                            baseUnitPx = baseUnitPx,
                            imageBitmaps = imageBitmaps,
                            fontRenderEngine = fontRenderEngine,
                            isZoomMode = isZoomMode,
                            isEyedropperActive = isEyedropperActive,
                            onSelectLayer = onSelectLayer,
                            onLayerTapped = onLayerTapped,
                            onTransformLayer = onTransformLayer
                        )
                    }

                    // Show 4 corner dots around collapsed Group frame when group is selected
                    if (!layer.isExpanded && showDots && !effectiveLocked) {
                        SelectionDotHandlesOverlay(
                            boxModifier = Modifier.size(with(LocalDensity.current) { (baseUnitPx * 0.64f).toDp() }),
                            dotColor = dotAccent,
                            targetLayer = layer,
                            onUniformZoom = { zoomFactor ->
                                onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                            },
                            onStretch = { sxFactor, syFactor ->
                                onTransformLayer(layer.id, 0f, 0f, 1f, 0f, sxFactor, syFactor)
                            }
                        )
                    }
                }
            }

            is ImageLayer -> {
                val bmp = imageBitmaps[layer.uriString]
                if (bmp != null) {
                    val cropNorm = layer.crop.normalized()
                    val srcW = ((cropNorm.right - cropNorm.left) * bmp.width).roundToInt().coerceAtLeast(1)
                    val srcH = ((cropNorm.bottom - cropNorm.top) * bmp.height).roundToInt().coerceAtLeast(1)
                    val imgRatio = srcW.toFloat() / srcH.toFloat()
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
                    val adjColorFilter = remember(layer.adjustments) {
                        if (layer.adjustments.isIdentity()) null
                        else {
                            val androidMatrix = MediaStorageManager.buildColorMatrix(layer.adjustments)
                            ColorFilter.colorMatrix(ColorMatrix(androidMatrix.array))
                        }
                    }

                    Box(modifier = fitModifier.then(bodyGestureModifier)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = if (layer.tintColorArgb != null) {
                                        CompositingStrategy.Offscreen
                                    } else {
                                        CompositingStrategy.Auto
                                    }
                                }
                        ) {
                            if (cropNorm.isFull()) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = layer.name,
                                    contentScale = ContentScale.Fit,
                                    colorFilter = adjColorFilter,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val imageBmp = remember(bmp) { bmp.asImageBitmap() }
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val srcOffset = IntOffset(
                                        (cropNorm.left * bmp.width).roundToInt().coerceIn(0, (bmp.width - 1).coerceAtLeast(0)),
                                        (cropNorm.top * bmp.height).roundToInt().coerceIn(0, (bmp.height - 1).coerceAtLeast(0))
                                    )
                                    drawImage(
                                        image = imageBmp,
                                        srcOffset = srcOffset,
                                        srcSize = IntSize(srcW.coerceAtMost(bmp.width - srcOffset.x), srcH.coerceAtMost(bmp.height - srcOffset.y)),
                                        dstOffset = IntOffset.Zero,
                                        dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
                                        colorFilter = adjColorFilter
                                    )
                                }
                            }

                            layer.tintColorArgb?.let { tintLong ->
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawRect(
                                        color = Color(tintLong.toInt()),
                                        blendMode = BlendMode.SrcAtop
                                    )
                                }
                            }
                        }

                        if (layer.adjustments.vignette > 0.01f) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val vAlpha = layer.adjustments.vignette.coerceIn(0f, 1f) * 0.8f
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = vAlpha * 0.35f),
                                            Color.Black.copy(alpha = vAlpha)
                                        ),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = size.maxDimension * 0.72f
                                    )
                                )
                            }
                        }

                        if (showDots && parentGroup == null) {
                            SelectionDotHandlesOverlay(
                                boxModifier = Modifier.fillMaxSize(),
                                dotColor = dotAccent,
                                targetLayer = layer,
                                onUniformZoom = { zoomFactor ->
                                    if (!effectiveLocked) {
                                        onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                    }
                                },
                                onStretch = { sxFactor, syFactor ->
                                    if (!effectiveLocked) {
                                        onTransformLayer(layer.id, 0f, 0f, 1f, 0f, sxFactor, syFactor)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            is ShapeLayer -> {
                val shapePx = baseUnitPx * 0.34f
                val shapeDp = with(LocalDensity.current) { shapePx.toDp() }
                Box(modifier = Modifier.size(shapeDp).then(bodyGestureModifier)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
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
                                drawRoundRect(color = fill, cornerRadius = CornerRadius(r, r))
                                if (strokeW > 0f) drawRoundRect(color = stroke, cornerRadius = CornerRadius(r, r), style = Stroke(strokeW))
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

                    if (showDots && parentGroup == null) {
                        SelectionDotHandlesOverlay(
                            boxModifier = Modifier.fillMaxSize(),
                            dotColor = dotAccent,
                            targetLayer = layer,
                            onUniformZoom = { zoomFactor ->
                                if (!effectiveLocked) {
                                    onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                }
                            },
                            onStretch = { sxFactor, syFactor ->
                                if (!effectiveLocked) {
                                    onTransformLayer(layer.id, 0f, 0f, 1f, 0f, sxFactor, syFactor)
                                }
                            }
                        )
                    }
                }
            }

            is TextLayer -> {
                val effPx = layer.fontSizeSp * density
                val maxBoxW = (canvasWpx * 0.92f).coerceAtLeast(64f)
                val measured = remember(
                    layer.text,
                    layer.fontId,
                    layer.fontSizeSp,
                    layer.isBold,
                    layer.isItalic,
                    layer.letterSpacing,
                    layer.align,
                    document.emojiSourceMode,
                    document.appEmojiFontId,
                    maxBoxW
                ) {
                    fontRenderEngine.measureTextLayerBounds(
                        layer = layer,
                        effectiveFontSizePx = effPx,
                        maxWidthPx = maxBoxW,
                        emojiSourceMode = document.emojiSourceMode,
                        appEmojiFontId = document.appEmojiFontId
                    )
                }
                val boxWdp = with(LocalDensity.current) { measured.widthPx.toDp() }
                val boxHdp = with(LocalDensity.current) { measured.heightPx.toDp() }

                Box(
                    modifier = Modifier
                        .size(width = boxWdp, height = boxHdp)
                        .then(bodyGestureModifier)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.save()
                            canvas.nativeCanvas.translate(size.width / 2f, size.height / 2f)
                            fontRenderEngine.drawTextLayerOnCanvas(
                                canvas = canvas.nativeCanvas,
                                layer = layer,
                                effectiveFontSizePx = effPx,
                                maxWidthPx = (measured.widthPx + 4f).coerceAtMost(maxBoxW),
                                emojiSourceMode = document.emojiSourceMode,
                                appEmojiFontId = document.appEmojiFontId
                            )
                            canvas.nativeCanvas.restore()
                        }
                    }

                    if (showDots && parentGroup == null) {
                        SelectionDotHandlesOverlay(
                            boxModifier = Modifier.fillMaxSize(),
                            dotColor = dotAccent,
                            targetLayer = layer,
                            onUniformZoom = { zoomFactor ->
                                if (!effectiveLocked) {
                                    onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                }
                            },
                            onStretch = { sxFactor, syFactor ->
                                if (!effectiveLocked) {
                                    onTransformLayer(layer.id, 0f, 0f, 1f, 0f, sxFactor, syFactor)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (isPrimarySelected && effectiveLocked && layer !is GroupLayer) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Locked",
                tint = StudioAmber,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp)
            )
        }
    }
}

/**
 * Renders 4 Corner Dots (uniform zoom) + 4 Edge Midpoint Handles (gentle horizontal & vertical stretch)
 * with NO solid rectangle border around the object.
 */
@Composable
private fun SelectionDotHandlesOverlay(
    boxModifier: Modifier,
    dotColor: Color,
    targetLayer: EditorLayer,
    onUniformZoom: (scaleFactor: Float) -> Unit,
    onStretch: (stretchXFactor: Float, stretchYFactor: Float) -> Unit
) {
    var boxW by remember { mutableFloatStateOf(160f) }
    var boxH by remember { mutableFloatStateOf(160f) }
    val liveLayer by rememberUpdatedState(targetLayer)

    // Keep handle visual size reasonably consistent even when layer is scaled
    val invScaleX = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.25f, 2.2f)
    val invScaleY = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.25f, 2.2f)
    val invUniform = ((invScaleX + invScaleY) * 0.5f).coerceIn(0.25f, 2.0f)

    Box(
        modifier = boxModifier.onSizeChanged { sz ->
            boxW = sz.width.toFloat().coerceAtLeast(48f)
            boxH = sz.height.toFloat().coerceAtLeast(48f)
        }
    ) {
        // 4 Corner Dots: Uniform Zoom on drag
        val corners = listOf(
            Alignment.TopStart to (-1f to -1f),
            Alignment.TopEnd to (1f to -1f),
            Alignment.BottomStart to (-1f to 1f),
            Alignment.BottomEnd to (1f to 1f)
        )
        corners.forEachIndexed { idx, (align, signs) ->
            val (sxSign, sySign) = signs
            Box(
                modifier = Modifier
                    .align(align)
                    .offset(x = (sxSign * 6).dp, y = (sySign * 6).dp)
                    .size(28.dp)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .pointerInput(liveLayer.id, idx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val radialDelta = (dragAmount.x * sxSign + dragAmount.y * sySign) * 0.5f
                            val span = min(boxW, boxH).coerceAtLeast(60f)
                            val zoomFactor = (1f + (radialDelta / span) * 0.85f).coerceIn(0.92f, 1.09f)
                            onUniformZoom(zoomFactor)
                        }
                    }
                    .testTag("corner_zoom_dot_$idx"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, dotColor, CircleShape)
                )
            }
        }

        // Top & Bottom Edge Handles: Controlled Vertical Stretch (up/down)
        listOf(Alignment.TopCenter to -1f, Alignment.BottomCenter to 1f).forEachIndexed { idx, (align, signY) ->
            Box(
                modifier = Modifier
                    .align(align)
                    .offset(y = (signY * 5).dp)
                    .size(width = 32.dp, height = 22.dp)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .pointerInput(liveLayer.id, "v_$idx") {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Damped gentle speed so stretching never explodes to extreme sizes
                            val deltaPx = dragAmount.y * signY
                            val stretchFactor = (1f + (deltaPx / boxH.coerceAtLeast(64f)) * 0.55f)
                                .coerceIn(0.95f, 1.06f)
                            onStretch(1f, stretchFactor)
                        }
                    }
                    .testTag("vertical_stretch_handle_$idx"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White)
                        .border(1.2.dp, dotColor, RoundedCornerShape(999.dp))
                )
            }
        }

        // Left & Right Edge Handles: Controlled Horizontal Stretch (left/right)
        listOf(Alignment.CenterStart to -1f, Alignment.CenterEnd to 1f).forEachIndexed { idx, (align, signX) ->
            Box(
                modifier = Modifier
                    .align(align)
                    .offset(x = (signX * 5).dp)
                    .size(width = 22.dp, height = 32.dp)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .pointerInput(liveLayer.id, "h_$idx") {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaPx = dragAmount.x * signX
                            val stretchFactor = (1f + (deltaPx / boxW.coerceAtLeast(64f)) * 0.55f)
                                .coerceIn(0.95f, 1.06f)
                            onStretch(stretchFactor, 1f)
                        }
                    }
                    .testTag("horizontal_stretch_handle_$idx"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 5.dp, height = 16.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White)
                        .border(1.2.dp, dotColor, RoundedCornerShape(999.dp))
                )
            }
        }
    }
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
