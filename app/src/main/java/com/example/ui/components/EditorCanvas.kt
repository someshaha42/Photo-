package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect as AndroidRect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.LayerTransformMatrix
import com.example.data.MediaStorageManager
import com.example.data.ShapeHandleKind
import com.example.data.VectorShapeEngine
import com.example.data.VectorStrokeEngine
import com.example.fonts.FontRenderEngine
import com.example.model.BackgroundTab
import com.example.model.DrawLayer
import com.example.model.DrawPoint
import com.example.model.DrawStroke
import com.example.model.EditorBlendMode
import com.example.model.EditorLayer
import com.example.model.GroupLayer
import com.example.model.ImageCropRect
import com.example.model.ImageCropShape
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ProjectDocument
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.model.VectorStrokeStyle
import com.example.ui.StudioViewModel
import com.example.ui.TransformLockState
import com.example.ui.theme.CheckerDark1
import com.example.ui.theme.CheckerDark2
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
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
    isCropMode: Boolean = false,
    isDrawMode: Boolean = false,
    isCornerPinMode: Boolean = false,
    isShapeEditPointsMode: Boolean = false,
    transformLocks: TransformLockState = TransformLockState(),
    drawColorArgb: Long = 0xFFFF4D4FL,
    drawBrushSizeNorm: Float = 0.014f,
    drawStrokeStyle: VectorStrokeStyle = VectorStrokeStyle(),
    canvasZoom: Float,
    canvasPan: Pair<Float, Float>,
    isEyedropperActive: Boolean,
    onCanvasViewportTransform: (zoomDelta: Float, panDx: Float, panDy: Float) -> Unit,
    onSelectLayer: (String?) -> Unit,
    onLayerTapped: (String) -> Unit = onSelectLayer,
    onEmptyCanvasTap: () -> Unit = { onSelectLayer(null) },
    onDeleteLayer: (String) -> Unit = {},
    onTransformLayer: (
        layerId: String,
        dxNorm: Float,
        dyNorm: Float,
        scale: Float,
        rotation: Float,
        stretchX: Float,
        stretchY: Float
    ) -> Unit,
    onAnchoredStretch: (
        layerId: String,
        signX: Float,
        signY: Float,
        screenLocalDeltaX: Float,
        screenLocalDeltaY: Float,
        boxWpx: Float,
        boxHpx: Float,
        canvasWpx: Float,
        canvasHpx: Float
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onCornerPinDrag: (layerId: String, cornerIndex: Int, deltaDxRatio: Float, deltaDyRatio: Float) -> Unit = { _, _, _, _ -> },
    onShapeHandleDrag: (layerId: String, handleId: String, dxPx: Float, dyPx: Float, baseSizePx: Float) -> Unit = { _, _, _, _, _ -> },
    onBeginTransformTransaction: () -> Unit = {},
    onEndTransformTransaction: () -> Unit = {},
    onUpdateCropTransient: ((ImageCropRect) -> ImageCropRect) -> Unit = {},
    onCommitCrop: ((ImageCropRect) -> ImageCropRect) -> Unit = {},
    onAddDrawStroke: (DrawStroke) -> Unit = {},
    onEyedropperTap: (normX: Float, normY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = document.canvas.ratioFloat
    val selectedSet = remember(document.selectionOrder, document.selectedLayerId) {
        document.effectiveSelectionOrder.toSet()
    }
    var canvasBoxWpx by remember { mutableFloatStateOf(300f) }
    var canvasBoxHpx by remember { mutableFloatStateOf(300f) }
    val liveSelectedId by rememberUpdatedState(document.selectedLayerId)
    val liveSelectedLayer by rememberUpdatedState(
        StudioViewModel.findLayerRecursive(document.layers, document.selectedLayerId)
    )
    val liveCanvasZoom by rememberUpdatedState(canvasZoom)
    val liveLocks by rememberUpdatedState(transformLocks)

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
                } else if (!isEyedropperActive && !isDrawMode) {
                    val allowViewportTransform = !isCropMode && !isCornerPinMode
                    Modifier
                        // Viewport-wide 2-finger pinch/rotate/move assist when transform editing is active
                        .pointerInput(isZoomMode, isEyedropperActive, isDrawMode, isCropMode, isCornerPinMode) {
                            if (!allowViewportTransform) return@pointerInput
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                var twoFingerActive = false
                                try {
                                    do {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val pressedCount = event.changes.count { it.pressed }
                                        val targetId = liveSelectedId
                                        val targetLayer = liveSelectedLayer
                                        if (pressedCount >= 2 && targetId != null && targetLayer != null && !targetLayer.locked) {
                                            if (!twoFingerActive) {
                                                twoFingerActive = true
                                                onBeginTransformTransaction()
                                            }
                                            val zoom = event.calculateZoom()
                                            val pan = event.calculatePan()
                                            val rot = event.calculateRotation()
                                            val safeCanvasZ = liveCanvasZoom.coerceAtLeast(0.25f)
                                            val safeW = canvasBoxWpx.coerceAtLeast(1f)
                                            val safeH = canvasBoxHpx.coerceAtLeast(1f)
                                            val locks = liveLocks
                                            if (zoom != 1f || rot != 0f || pan != Offset.Zero) {
                                                onTransformLayer(
                                                    targetId,
                                                    if (locks.positionLocked) 0f else (pan.x / safeCanvasZ) / safeW,
                                                    if (locks.positionLocked) 0f else (pan.y / safeCanvasZ) / safeH,
                                                    if (locks.zoomLocked) 1f else zoom,
                                                    if (locks.rotationLocked) 0f else rot,
                                                    1f,
                                                    1f
                                                )
                                                event.changes.forEach { it.consume() }
                                            }
                                        } else if (twoFingerActive && pressedCount < 2) {
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while (event.changes.any { it.pressed })
                                } finally {
                                    if (twoFingerActive) {
                                        onEndTransformTransaction()
                                    }
                                }
                            }
                        }
                        // Unified Main-pass: 1-finger remote pan on empty canvas when transform-active, or quick-tap anywhere empty to commit & dismiss
                        .pointerInput(isZoomMode, isEyedropperActive, isDrawMode, isCropMode, isCornerPinMode) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                                var totalPan = Offset.Zero
                                var pastSlop = false
                                var draggingRemote = false
                                var childConsumedOrMulti = down.isConsumed
                                var releasedChange: PointerInputChange? = null
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                        if (event.changes.count { it.pressed } >= 2 || event.changes.any { it.isConsumed }) {
                                            childConsumedOrMulti = true
                                            break
                                        }
                                        val active = event.changes.firstOrNull { it.id == down.id }
                                            ?: event.changes.firstOrNull()
                                        if (active == null || !active.pressed) {
                                            if (active != null && !active.isConsumed) {
                                                releasedChange = active
                                            }
                                            break
                                        }
                                        val delta = active.position - active.previousPosition
                                        if (!pastSlop) {
                                            totalPan += delta
                                            if (totalPan.getDistance() >= viewConfiguration.touchSlop) {
                                                pastSlop = true
                                            }
                                        }
                                        if (pastSlop && allowViewportTransform) {
                                            val targetId = liveSelectedId
                                            val targetLayer = liveSelectedLayer
                                            val locks = liveLocks
                                            val moveDelta = if (!draggingRemote) {
                                                draggingRemote = true
                                                onBeginTransformTransaction()
                                                totalPan
                                            } else delta
                                            if (targetId != null && targetLayer != null && !targetLayer.locked && !locks.positionLocked && moveDelta != Offset.Zero) {
                                                val safeCanvasZ = liveCanvasZoom.coerceAtLeast(0.25f)
                                                val safeW = canvasBoxWpx.coerceAtLeast(1f)
                                                val safeH = canvasBoxHpx.coerceAtLeast(1f)
                                                onTransformLayer(
                                                    targetId,
                                                    (moveDelta.x / safeCanvasZ) / safeW,
                                                    (moveDelta.y / safeCanvasZ) / safeH,
                                                    1f,
                                                    0f,
                                                    1f,
                                                    1f
                                                )
                                                active.consume()
                                            }
                                        }
                                    }
                                    if (!childConsumedOrMulti && !pastSlop && releasedChange != null) {
                                        releasedChange.consume()
                                        onEmptyCanvasTap()
                                    }
                                } finally {
                                    if (draggingRemote) {
                                        onEndTransformTransaction()
                                    }
                                }
                            }
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
                .onSizeChanged { sz ->
                    canvasBoxWpx = sz.width.toFloat().coerceAtLeast(1f)
                    canvasBoxHpx = sz.height.toFloat().coerceAtLeast(1f)
                }
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
                        isDrawMode -> StudioCoral
                        isCropMode -> StudioEmerald
                        isCornerPinMode -> StudioAzure
                        isZoomMode -> StudioAzure
                        else -> MaterialTheme.colorScheme.outline
                    },
                    shape = RoundedCornerShape(4.dp)
                )
                .clipToBounds()
                .then(
                    if (isEyedropperActive) {
                        Modifier.pointerInput(isEyedropperActive) {
                            detectTapGestures { offset ->
                                val nx = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                val ny = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                onEyedropperTap(nx, ny)
                            }
                        }
                    } else Modifier
                )
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

            // 3. Layers Stack (Photos, Shapes, Text/Color Emojis, Draw, Groups)
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
                        parentScale = 1f,
                        canvasZoom = canvasZoom,
                        canvasWpx = canvasWpx,
                        canvasHpx = canvasHpx,
                        baseUnitPx = baseUnitPx,
                        imageBitmaps = imageBitmaps,
                        fontRenderEngine = fontRenderEngine,
                        isZoomMode = isZoomMode,
                        isCropMode = isCropMode,
                        isDrawMode = isDrawMode,
                        isCornerPinMode = isCornerPinMode,
                        isShapeEditPointsMode = isShapeEditPointsMode,
                        transformLocks = transformLocks,
                        isEyedropperActive = isEyedropperActive,
                        onSelectLayer = onSelectLayer,
                        onLayerTapped = onLayerTapped,
                        onDeleteLayer = onDeleteLayer,
                        onTransformLayer = onTransformLayer,
                        onAnchoredStretch = onAnchoredStretch,
                        onCornerPinDrag = onCornerPinDrag,
                        onShapeHandleDrag = onShapeHandleDrag,
                        onBeginTransformTransaction = onBeginTransformTransaction,
                        onEndTransformTransaction = onEndTransformTransaction,
                        onUpdateCropTransient = onUpdateCropTransient,
                        onCommitCrop = onCommitCrop
                    )
                }

                // 4. Interactive Freehand Draw Overlay (captures coordinates & pointer/stylus pressure with zero touch-slop jump)
                if (isDrawMode && !isZoomMode && !isEyedropperActive) {
                    val livePoints = remember { mutableStateListOf<DrawPoint>() }
                    val latestColor by rememberUpdatedState(drawColorArgb)
                    val latestBrushNorm by rememberUpdatedState(drawBrushSizeNorm)
                    val latestStyle by rememberUpdatedState(drawStrokeStyle)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = true)
                                    down.consume()
                                    val w = size.width.coerceAtLeast(1)
                                    val h = size.height.coerceAtLeast(1)
                                    livePoints.clear()
                                    livePoints.add(
                                        DrawPoint(
                                            xNorm = (down.position.x / w).coerceIn(0f, 1f),
                                            yNorm = (down.position.y / h).coerceIn(0f, 1f),
                                            pressure = if (down.pressure > 0.01f) down.pressure else 1f
                                        )
                                    )
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                        if (change == null || !change.pressed) {
                                            break
                                        }
                                        if (change.positionChanged()) {
                                            change.consume()
                                            livePoints.add(
                                                DrawPoint(
                                                    xNorm = (change.position.x / w).coerceIn(0f, 1f),
                                                    yNorm = (change.position.y / h).coerceIn(0f, 1f),
                                                    pressure = if (change.pressure > 0.01f) change.pressure else 1f
                                                )
                                            )
                                        }
                                    }
                                    if (livePoints.isNotEmpty()) {
                                        val capturedRaw = livePoints.toList()
                                        val effectiveStyle = latestStyle.copy(overallWidthNorm = latestBrushNorm)
                                        onAddDrawStroke(
                                            DrawStroke(
                                                colorArgb = latestColor,
                                                strokeWidthNorm = latestBrushNorm,
                                                points = capturedRaw,
                                                rawPoints = capturedRaw,
                                                style = effectiveStyle
                                            )
                                        )
                                    }
                                    livePoints.clear()
                                }
                            }
                            .testTag("canvas_draw_overlay")
                    ) {
                        if (livePoints.isNotEmpty()) {
                            val effectiveStyle = latestStyle.copy(overallWidthNorm = latestBrushNorm)
                            val previewSmoothed = VectorStrokeEngine.smoothPoints(livePoints.toList(), effectiveStyle.smoothing)
                            val liveStroke = DrawStroke(
                                colorArgb = latestColor,
                                strokeWidthNorm = latestBrushNorm,
                                points = previewSmoothed,
                                rawPoints = livePoints.toList(),
                                style = effectiveStyle
                            )
                            drawIntoCanvas { canvas ->
                                VectorStrokeEngine.drawVectorStroke(
                                    canvas = canvas.nativeCanvas,
                                    stroke = liveStroke,
                                    areaWidthPx = size.width,
                                    areaHeightPx = size.height,
                                    layerOpacity = 1f,
                                    originOffsetX = 0f,
                                    originOffsetY = 0f
                                )
                            }
                        }
                    }
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
    parentScale: Float,
    canvasZoom: Float,
    canvasWpx: Float,
    canvasHpx: Float,
    baseUnitPx: Float,
    imageBitmaps: Map<String, Bitmap>,
    fontRenderEngine: FontRenderEngine,
    isZoomMode: Boolean,
    isCropMode: Boolean,
    isDrawMode: Boolean,
    isCornerPinMode: Boolean,
    isShapeEditPointsMode: Boolean,
    transformLocks: TransformLockState,
    isEyedropperActive: Boolean,
    onSelectLayer: (String?) -> Unit,
    onLayerTapped: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onTransformLayer: (
        layerId: String,
        dxNorm: Float,
        dyNorm: Float,
        scale: Float,
        rotation: Float,
        stretchX: Float,
        stretchY: Float
    ) -> Unit,
    onAnchoredStretch: (
        layerId: String,
        signX: Float,
        signY: Float,
        screenLocalDeltaX: Float,
        screenLocalDeltaY: Float,
        boxWpx: Float,
        boxHpx: Float,
        canvasWpx: Float,
        canvasHpx: Float
    ) -> Unit,
    onCornerPinDrag: (layerId: String, cornerIndex: Int, deltaDxRatio: Float, deltaDyRatio: Float) -> Unit,
    onShapeHandleDrag: (layerId: String, handleId: String, dxPx: Float, dyPx: Float, baseSizePx: Float) -> Unit,
    onBeginTransformTransaction: () -> Unit,
    onEndTransformTransaction: () -> Unit,
    onUpdateCropTransient: ((ImageCropRect) -> ImageCropRect) -> Unit,
    onCommitCrop: ((ImageCropRect) -> ImageCropRect) -> Unit
) {
    if (!layer.visible) return
    val density = LocalDensity.current.density
    val interactiveTarget = parentGroup ?: layer
    val interactiveTargetId = interactiveTarget.id
    val effectiveLocked = parentLocked || layer.locked || (parentGroup?.locked == true)
    val isPrimarySelected = (interactiveTargetId == primarySelectedId) &&
        !isEyedropperActive && !isZoomMode && !isDrawMode
    val isMultiSelected = (interactiveTargetId in selectedSet) &&
        !isEyedropperActive && !isZoomMode && !isDrawMode
    val isBeingCornerPinned = isCornerPinMode && isPrimarySelected && parentGroup == null
    val showDots = (isPrimarySelected || isMultiSelected) &&
        !(isCropMode && layer is ImageLayer && isPrimarySelected) &&
        !isBeingCornerPinned
    val dotAccent = if (isPrimarySelected) StudioAzure else StudioEmerald

    val liveTargetLayer by rememberUpdatedState(interactiveTarget)
    val liveChildLayer by rememberUpdatedState(layer)
    val liveLocks by rememberUpdatedState(transformLocks)
    val liveCanvasZoom by rememberUpdatedState(canvasZoom)
    val liveParentScale by rememberUpdatedState(parentScale)

    // Scale-aware gesture handler: touchSlop is inversely scaled by zoom so high-zoom dragging/zooming/rotating responds immediately!
    val bodyGestureModifier = if (!isZoomMode && !isEyedropperActive && !isDrawMode && !isBeingCornerPinned) {
        if ((layer is GroupLayer && layer.isExpanded && parentGroup == null) ||
            (isCropMode && layer is ImageLayer && isPrimarySelected)
        ) {
            Modifier
        } else {
            Modifier
                .pointerInput(interactiveTargetId, isEyedropperActive) {
                    detectTapGestures(onTap = { onLayerTapped(interactiveTargetId) })
                }
                .pointerInput(interactiveTargetId, effectiveLocked, canvasWpx, canvasHpx) {
                    if (effectiveLocked) return@pointerInput
                    detectScaleAwareTransformGestures(
                        effectiveScaleProvider = {
                            val t = liveTargetLayer
                            val c = liveChildLayer
                            val sx = t.scale * t.stretchX * if (parentGroup != null) c.scale * c.stretchX else 1f
                            val sy = t.scale * t.stretchY * if (parentGroup != null) c.scale * c.stretchY else 1f
                            max(sx, sy) * liveParentScale * liveCanvasZoom
                        },
                        onGestureStart = { onBeginTransformTransaction() },
                        onGestureEnd = { onEndTransformTransaction() }
                    ) { _, pan, zoom, rotation ->
                        onSelectLayer(interactiveTargetId)
                        val t = liveTargetLayer
                        val c = liveChildLayer
                        val locks = liveLocks
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
                            if (locks.positionLocked) 0f else dxCanvasPx / canvasWpx.coerceAtLeast(1f),
                            if (locks.positionLocked) 0f else dyCanvasPx / canvasHpx.coerceAtLeast(1f),
                            if (locks.zoomLocked) 1f else zoom,
                            if (locks.rotationLocked) 0f else rotation,
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
                val hasPerspectiveOr3D = !layer.isExpanded && (
                    !layer.cornerPin.isIdentity() ||
                        !layer.transform3D.isIdentity() ||
                        isBeingCornerPinned
                    )
                val boxPx = baseUnitPx * 0.64f
                val boxDp = with(LocalDensity.current) { boxPx.toDp() }

                val groupOriginX = (canvasWpx - boxPx) * 0.5f
                val groupOriginY = (canvasHpx - boxPx) * 0.5f

                if (hasPerspectiveOr3D) {
                    val padRatio = 2.2f
                    val uniformZoom = (layer.scale * parentScale * canvasZoom).coerceAtLeast(1f)
                    val texScale = uniformZoom.coerceIn(1f, 3f)
                        .coerceAtMost(1024f / (boxPx * padRatio).coerceAtLeast(1f))
                        .coerceAtLeast(0.5f)
                    val preparedGroupBmp = remember(
                        layer.children,
                        document.emojiSourceMode,
                        document.appEmojiFontId,
                        imageBitmaps,
                        canvasWpx,
                        canvasHpx,
                        baseUnitPx,
                        texScale,
                        density
                    ) {
                        val tw = (boxPx * padRatio * texScale).roundToInt().coerceIn(48, 1200)
                        val th = (boxPx * padRatio * texScale).roundToInt().coerceIn(48, 1200)
                        Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888).also { bmp ->
                            val gc = AndroidCanvas(bmp)
                            gc.translate(tw / 2f, th / 2f)
                            gc.scale(texScale, texScale)
                            layer.children.forEach { child ->
                                renderGroupChildToAndroidCanvas(
                                    canvas = gc,
                                    layer = child,
                                    document = document,
                                    imageBitmaps = imageBitmaps,
                                    fontRenderEngine = fontRenderEngine,
                                    canvasWpx = canvasWpx,
                                    canvasHpx = canvasHpx,
                                    baseUnitPx = baseUnitPx,
                                    density = density,
                                    parentOpacity = 1f,
                                    parentScale = parentScale * layer.scale * texScale
                                )
                            }
                        }
                    }
                    val blurredGhost = remember(preparedGroupBmp, isBeingCornerPinned) {
                        if (isBeingCornerPinned) LayerTransformMatrix.createFastBlurPreview(preparedGroupBmp) else null
                    }
                    val mappedQuad = remember(boxPx, layer.transform3D, layer.cornerPin) {
                        LayerTransformMatrix.mapCornerPoints(boxPx, boxPx, layer.transform3D, layer.cornerPin)
                    }
                    val hitBounds = remember(mappedQuad) {
                        LayerTransformMatrix.computeQuadBoundingBox(mappedQuad, paddingPx = 20f)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.align(Alignment.Center).size(boxDp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawIntoCanvas { canvas ->
                                    val nc = canvas.nativeCanvas
                                    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
                                    if (isBeingCornerPinned && blurredGhost != null) {
                                        paint.alpha = 115
                                        nc.drawBitmap(
                                            blurredGhost,
                                            null,
                                            RectF(
                                                -size.width * (padRatio - 1f) * 0.5f,
                                                -size.height * (padRatio - 1f) * 0.5f,
                                                size.width * (padRatio + 1f) * 0.5f,
                                                size.height * (padRatio + 1f) * 0.5f
                                            ),
                                            paint
                                        )
                                        paint.alpha = 255
                                    }
                                    val drawMatrix = LayerTransformMatrix.buildPaddedBitmapDrawMatrix(
                                        bitmapWidth = preparedGroupBmp.width,
                                        bitmapHeight = preparedGroupBmp.height,
                                        padRatioX = padRatio,
                                        padRatioY = padRatio,
                                        boxW = size.width,
                                        boxH = size.height,
                                        transform3D = layer.transform3D,
                                        cornerPin = layer.cornerPin
                                    )
                                    nc.drawBitmap(preparedGroupBmp, drawMatrix, paint)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (groupOriginX + hitBounds.minX).roundToInt(),
                                        (groupOriginY + hitBounds.minY).roundToInt()
                                    )
                                }
                                .size(
                                    width = with(LocalDensity.current) { hitBounds.width.toDp() },
                                    height = with(LocalDensity.current) { hitBounds.height.toDp() }
                                )
                                .then(bodyGestureModifier)
                        )

                        if (showDots && !effectiveLocked) {
                            SelectionDotHandlesOverlay(
                                boxWpx = boxPx,
                                boxHpx = boxPx,
                                originX = groupOriginX,
                                originY = groupOriginY,
                                dotColor = dotAccent,
                                targetLayer = layer,
                                canvasZoom = canvasZoom,
                                locks = transformLocks,
                                showDeleteCross = isPrimarySelected,
                                onDeleteClick = { onDeleteLayer(layer.id) },
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onUniformZoom = { zoomFactor ->
                                    onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                },
                                onEdgePan = { localDx, localDy ->
                                    val sx = layer.scale * layer.stretchX
                                    val sy = layer.scale * layer.stretchY
                                    val rad = Math.toRadians(layer.rotationDeg.toDouble())
                                    val ux = localDx * sx
                                    val uy = localDy * sy
                                    val dxPx = (ux * cos(rad) - uy * sin(rad)).toFloat()
                                    val dyPx = (ux * sin(rad) + uy * cos(rad)).toFloat()
                                    onTransformLayer(layer.id, dxPx / canvasWpx.coerceAtLeast(1f), dyPx / canvasHpx.coerceAtLeast(1f), 1f, 0f, 1f, 1f)
                                },
                                onAnchoredEdgeStretch = { sx, sy, sdx, sdy, bw, bh ->
                                    onAnchoredStretch(layer.id, sx, sy, sdx, sdy, bw, bh, canvasWpx, canvasHpx)
                                }
                            )
                        }

                        if (isBeingCornerPinned && !effectiveLocked) {
                            CornerPinHandlesOverlay(
                                targetLayer = layer,
                                boxWpx = boxPx,
                                boxHpx = boxPx,
                                canvasWpx = canvasWpx,
                                canvasHpx = canvasHpx,
                                canvasZoom = canvasZoom,
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onDragCorner = { cornerIdx, dDxRatio, dDyRatio ->
                                    onCornerPinDrag(layer.id, cornerIdx, dDxRatio, dDyRatio)
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            layer.children.forEach { child ->
                                RenderCanvasLayerNode(
                                    layer = child,
                                    document = document,
                                    selectedSet = selectedSet,
                                    primarySelectedId = primarySelectedId,
                                    parentGroup = childForwardParent,
                                    parentLocked = effectiveLocked,
                                    parentScale = parentScale * layer.scale,
                                    canvasZoom = canvasZoom,
                                    canvasWpx = canvasWpx,
                                    canvasHpx = canvasHpx,
                                    baseUnitPx = baseUnitPx,
                                    imageBitmaps = imageBitmaps,
                                    fontRenderEngine = fontRenderEngine,
                                    isZoomMode = isZoomMode,
                                    isCropMode = isCropMode,
                                    isDrawMode = isDrawMode,
                                    isCornerPinMode = isCornerPinMode,
                                    isShapeEditPointsMode = isShapeEditPointsMode,
                                    transformLocks = transformLocks,
                                    isEyedropperActive = isEyedropperActive,
                                    onSelectLayer = onSelectLayer,
                                    onLayerTapped = onLayerTapped,
                                    onDeleteLayer = onDeleteLayer,
                                    onTransformLayer = onTransformLayer,
                                    onAnchoredStretch = onAnchoredStretch,
                                    onCornerPinDrag = onCornerPinDrag,
                                    onShapeHandleDrag = onShapeHandleDrag,
                                    onBeginTransformTransaction = onBeginTransformTransaction,
                                    onEndTransformTransaction = onEndTransformTransaction,
                                    onUpdateCropTransient = onUpdateCropTransient,
                                    onCommitCrop = onCommitCrop
                                )
                            }
                        }

                        if (!layer.isExpanded && showDots && !effectiveLocked) {
                            SelectionDotHandlesOverlay(
                                boxWpx = boxPx,
                                boxHpx = boxPx,
                                originX = groupOriginX,
                                originY = groupOriginY,
                                dotColor = dotAccent,
                                targetLayer = layer,
                                canvasZoom = canvasZoom,
                                locks = transformLocks,
                                showDeleteCross = isPrimarySelected,
                                onDeleteClick = { onDeleteLayer(layer.id) },
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onUniformZoom = { zoomFactor ->
                                    onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                },
                                onEdgePan = { localDx, localDy ->
                                    val sx = layer.scale * layer.stretchX
                                    val sy = layer.scale * layer.stretchY
                                    val rad = Math.toRadians(layer.rotationDeg.toDouble())
                                    val ux = localDx * sx
                                    val uy = localDy * sy
                                    val dxPx = (ux * cos(rad) - uy * sin(rad)).toFloat()
                                    val dyPx = (ux * sin(rad) + uy * cos(rad)).toFloat()
                                    onTransformLayer(layer.id, dxPx / canvasWpx.coerceAtLeast(1f), dyPx / canvasHpx.coerceAtLeast(1f), 1f, 0f, 1f, 1f)
                                },
                                onAnchoredEdgeStretch = { sx, sy, sdx, sdy, bw, bh ->
                                    onAnchoredStretch(layer.id, sx, sy, sdx, sdy, bw, bh, canvasWpx, canvasHpx)
                                }
                            )
                        }
                    }
                }
            }

            is ImageLayer -> {
                val bmp = imageBitmaps[layer.uriString]
                if (bmp != null) {
                    val cropNorm = layer.crop.normalized()
                    val isBeingCroppedNow = isCropMode && isPrimarySelected && parentGroup == null

                    if (isBeingCroppedNow) {
                        // Interactive Crop Mode: display FULL uncropped image + real draggable crop frame
                        val fullRatio = bmp.width.coerceAtLeast(1).toFloat() / bmp.height.coerceAtLeast(1)
                        val fullModifier = if (fullRatio >= 1f) {
                            Modifier.size(
                                width = with(LocalDensity.current) { (baseUnitPx * 0.85f).toDp() },
                                height = with(LocalDensity.current) { (baseUnitPx * 0.85f / fullRatio).toDp() }
                            )
                        } else {
                            Modifier.size(
                                width = with(LocalDensity.current) { (baseUnitPx * 0.85f * fullRatio).toDp() },
                                height = with(LocalDensity.current) { (baseUnitPx * 0.85f).toDp() }
                            )
                        }
                        Box(modifier = fullModifier) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = layer.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            InteractiveCropOverlay(
                                crop = cropNorm,
                                onBeginTransaction = onBeginTransformTransaction,
                                onUpdateTransient = onUpdateCropTransient,
                                onCommit = onCommitCrop
                            )
                        }
                    } else {
                        val srcW = ((cropNorm.right - cropNorm.left) * bmp.width).roundToInt().coerceAtLeast(1)
                        val srcH = ((cropNorm.bottom - cropNorm.top) * bmp.height).roundToInt().coerceAtLeast(1)
                        val imgRatio = srcW.toFloat() / srcH.toFloat()
                        val boxWpx = if (imgRatio >= 1f) baseUnitPx * 0.85f else baseUnitPx * 0.85f * imgRatio
                        val boxHpx = if (imgRatio >= 1f) baseUnitPx * 0.85f / imgRatio else baseUnitPx * 0.85f
                        val fitModifier = Modifier.size(
                            width = with(LocalDensity.current) { boxWpx.toDp() },
                            height = with(LocalDensity.current) { boxHpx.toDp() }
                        )
                        val imgOriginX = (canvasWpx - boxWpx) * 0.5f
                        val imgOriginY = (canvasHpx - boxHpx) * 0.5f
                        val hasPerspectiveOr3D = !layer.cornerPin.isIdentity() ||
                            !layer.transform3D.isIdentity() ||
                            isBeingCornerPinned

                        if (hasPerspectiveOr3D) {
                            val preparedBmp = remember(
                                bmp,
                                cropNorm,
                                layer.cropShape,
                                layer.adjustments,
                                layer.tintColorArgb
                            ) {
                                buildPreparedImageBitmap(
                                    bmp = bmp,
                                    cropNorm = cropNorm,
                                    cropShape = layer.cropShape,
                                    adjustments = layer.adjustments,
                                    tintColorArgb = layer.tintColorArgb,
                                    maxDim = 900
                                )
                            }
                            val blurredGhost = remember(preparedBmp, isBeingCornerPinned) {
                                if (isBeingCornerPinned) LayerTransformMatrix.createFastBlurPreview(preparedBmp) else null
                            }
                            val mappedQuad = remember(boxWpx, boxHpx, layer.transform3D, layer.cornerPin) {
                                LayerTransformMatrix.mapCornerPoints(boxWpx, boxHpx, layer.transform3D, layer.cornerPin)
                            }
                            val hitBounds = remember(mappedQuad) {
                                LayerTransformMatrix.computeQuadBoundingBox(mappedQuad, paddingPx = 20f)
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.align(Alignment.Center).then(fitModifier)) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawIntoCanvas { canvas ->
                                            val nc = canvas.nativeCanvas
                                            val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
                                            if (isBeingCornerPinned && blurredGhost != null) {
                                                paint.alpha = 108
                                                nc.drawBitmap(
                                                    blurredGhost,
                                                    null,
                                                    RectF(0f, 0f, size.width, size.height),
                                                    paint
                                                )
                                                paint.alpha = 255
                                            }
                                            val drawMatrix = LayerTransformMatrix.buildBitmapDrawMatrix(
                                                bitmapWidth = preparedBmp.width,
                                                bitmapHeight = preparedBmp.height,
                                                boxW = size.width,
                                                boxH = size.height,
                                                transform3D = layer.transform3D,
                                                cornerPin = layer.cornerPin
                                            )
                                            nc.drawBitmap(preparedBmp, drawMatrix, paint)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (imgOriginX + hitBounds.minX).roundToInt(),
                                                (imgOriginY + hitBounds.minY).roundToInt()
                                            )
                                        }
                                        .size(
                                            width = with(LocalDensity.current) { hitBounds.width.toDp() },
                                            height = with(LocalDensity.current) { hitBounds.height.toDp() }
                                        )
                                        .then(bodyGestureModifier)
                                )

                                if (showDots && parentGroup == null) {
                                    SelectionDotHandlesOverlay(
                                        boxWpx = boxWpx,
                                        boxHpx = boxHpx,
                                        originX = imgOriginX,
                                        originY = imgOriginY,
                                        dotColor = dotAccent,
                                        targetLayer = layer,
                                        canvasZoom = canvasZoom,
                                        locks = transformLocks,
                                        showDeleteCross = isPrimarySelected,
                                        onDeleteClick = { onDeleteLayer(layer.id) },
                                        onBeginTransaction = onBeginTransformTransaction,
                                        onEndTransaction = onEndTransformTransaction,
                                        onUniformZoom = { zoomFactor ->
                                            if (!effectiveLocked) {
                                                onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                            }
                                        },
                                        onEdgePan = { localDx, localDy ->
                                            if (!effectiveLocked) {
                                                val sx = layer.scale * layer.stretchX
                                                val sy = layer.scale * layer.stretchY
                                                val rad = Math.toRadians(layer.rotationDeg.toDouble())
                                                val ux = localDx * sx
                                                val uy = localDy * sy
                                                val dxPx = (ux * cos(rad) - uy * sin(rad)).toFloat()
                                                val dyPx = (ux * sin(rad) + uy * cos(rad)).toFloat()
                                                onTransformLayer(layer.id, dxPx / canvasWpx.coerceAtLeast(1f), dyPx / canvasHpx.coerceAtLeast(1f), 1f, 0f, 1f, 1f)
                                            }
                                        },
                                        onAnchoredEdgeStretch = { sx, sy, sdx, sdy, bw, bh ->
                                            if (!effectiveLocked) {
                                                onAnchoredStretch(layer.id, sx, sy, sdx, sdy, bw, bh, canvasWpx, canvasHpx)
                                            }
                                        }
                                    )
                                }

                                if (isBeingCornerPinned && !effectiveLocked) {
                                    CornerPinHandlesOverlay(
                                        targetLayer = layer,
                                        boxWpx = boxWpx,
                                        boxHpx = boxHpx,
                                        canvasWpx = canvasWpx,
                                        canvasHpx = canvasHpx,
                                        canvasZoom = canvasZoom,
                                        onBeginTransaction = onBeginTransformTransaction,
                                        onEndTransaction = onEndTransformTransaction,
                                        onDragCorner = { cornerIdx, dDxRatio, dDyRatio ->
                                            onCornerPinDrag(layer.id, cornerIdx, dDxRatio, dDyRatio)
                                        }
                                    )
                                }
                            }
                        } else {
                            val adjColorFilter = remember(layer.adjustments) {
                                if (layer.adjustments.isIdentity()) null
                                else {
                                    val androidMatrix = MediaStorageManager.buildColorMatrix(layer.adjustments)
                                    ColorFilter.colorMatrix(ColorMatrix(androidMatrix.array))
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.align(Alignment.Center).then(fitModifier).then(bodyGestureModifier)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(if (layer.cropShape == ImageCropShape.CIRCLE) CircleShape else RectangleShape)
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
                                                    srcSize = IntSize(
                                                        srcW.coerceAtMost(bmp.width - srcOffset.x),
                                                        srcH.coerceAtMost(bmp.height - srcOffset.y)
                                                    ),
                                                    dstOffset = IntOffset.Zero,
                                                    dstSize = IntSize(
                                                        size.width.roundToInt().coerceAtLeast(1),
                                                        size.height.roundToInt().coerceAtLeast(1)
                                                    ),
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
                                }

                                if (showDots && parentGroup == null) {
                                    SelectionDotHandlesOverlay(
                                        boxWpx = boxWpx,
                                        boxHpx = boxHpx,
                                        originX = imgOriginX,
                                        originY = imgOriginY,
                                        dotColor = dotAccent,
                                        targetLayer = layer,
                                        canvasZoom = canvasZoom,
                                        locks = transformLocks,
                                        showDeleteCross = isPrimarySelected,
                                        onDeleteClick = { onDeleteLayer(layer.id) },
                                        onBeginTransaction = onBeginTransformTransaction,
                                        onEndTransaction = onEndTransformTransaction,
                                        onUniformZoom = { zoomFactor ->
                                            if (!effectiveLocked) {
                                                onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                            }
                                        },
                                        onEdgePan = { localDx, localDy ->
                                            if (!effectiveLocked) {
                                                val sx = layer.scale * layer.stretchX
                                                val sy = layer.scale * layer.stretchY
                                                val rad = Math.toRadians(layer.rotationDeg.toDouble())
                                                val ux = localDx * sx
                                                val uy = localDy * sy
                                                val dxPx = (ux * cos(rad) - uy * sin(rad)).toFloat()
                                                val dyPx = (ux * sin(rad) + uy * cos(rad)).toFloat()
                                                onTransformLayer(layer.id, dxPx / canvasWpx.coerceAtLeast(1f), dyPx / canvasHpx.coerceAtLeast(1f), 1f, 0f, 1f, 1f)
                                            }
                                        },
                                        onAnchoredEdgeStretch = { sx, sy, sdx, sdy, bw, bh ->
                                            if (!effectiveLocked) {
                                                onAnchoredStretch(layer.id, sx, sy, sdx, sdy, bw, bh, canvasWpx, canvasHpx)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is ShapeLayer -> {
                val shapePx = baseUnitPx * 0.34f
                val shapeDp = with(LocalDensity.current) { shapePx.toDp() }
                val shapeOriginX = (canvasWpx - shapePx) * 0.5f
                val shapeOriginY = (canvasHpx - shapePx) * 0.5f
                val shapeLocalRect = remember(layer.shapeType, layer.vectorParams, shapePx) {
                    val rf = RectF()
                    VectorShapeEngine.buildShapePath(
                        shapeType = layer.shapeType,
                        params = layer.vectorParams,
                        baseSizePx = shapePx,
                        cx = shapePx * 0.5f,
                        cy = shapePx * 0.5f
                    ).computeBounds(rf, true)
                    ensureMinBoundsRect(rf, minSpan = 28f)
                }
                val mappedQuad = remember(shapeLocalRect, shapePx, layer.transform3D, layer.cornerPin) {
                    LayerTransformMatrix.mapQuadForRect(
                        left = shapeLocalRect.left,
                        top = shapeLocalRect.top,
                        right = shapeLocalRect.right,
                        bottom = shapeLocalRect.bottom,
                        boxW = shapePx,
                        boxH = shapePx,
                        transform3D = layer.transform3D,
                        cornerPin = layer.cornerPin
                    )
                }
                val hitBounds = remember(mappedQuad) {
                    LayerTransformMatrix.computeQuadBoundingBox(mappedQuad, paddingPx = 22f)
                }
                val hasPerspectiveOr3D = !layer.cornerPin.isIdentity() ||
                    !layer.transform3D.isIdentity() ||
                    isBeingCornerPinned

                Box(modifier = Modifier.fillMaxSize()) {
                    if (hasPerspectiveOr3D) {
                        val padRatio = 2.0f
                        val uniformZoom = (layer.scale * parentScale * canvasZoom).coerceAtLeast(1f)
                        val texScale = uniformZoom.coerceIn(1f, 3f)
                            .coerceAtMost(960f / (shapePx * padRatio).coerceAtLeast(1f))
                            .coerceAtLeast(1f)
                        val preparedShapeBmp = remember(
                            layer.shapeType,
                            layer.vectorParams,
                            layer.fillColorArgb,
                            layer.strokeColorArgb,
                            layer.strokeWidth,
                            shapePx,
                            texScale,
                            density
                        ) {
                            val tw = (shapePx * padRatio * texScale).roundToInt().coerceIn(48, 1200)
                            val th = (shapePx * padRatio * texScale).roundToInt().coerceIn(48, 1200)
                            Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888).also { bmp ->
                                val sc = AndroidCanvas(bmp)
                                VectorShapeEngine.drawShapeOnCanvas(
                                    canvas = sc,
                                    layer = layer.copy(opacity = 1f),
                                    baseSizePx = shapePx * texScale,
                                    cx = tw * 0.5f,
                                    cy = th * 0.5f,
                                    strokeScaleFactor = density * texScale
                                )
                            }
                        }
                        val blurredGhost = remember(preparedShapeBmp, isBeingCornerPinned) {
                            if (isBeingCornerPinned) LayerTransformMatrix.createFastBlurPreview(preparedShapeBmp) else null
                        }

                        Box(modifier = Modifier.align(Alignment.Center).size(shapeDp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawIntoCanvas { canvas ->
                                    val nc = canvas.nativeCanvas
                                    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
                                    if (isBeingCornerPinned && blurredGhost != null) {
                                        paint.alpha = 115
                                        nc.drawBitmap(
                                            blurredGhost,
                                            null,
                                            RectF(
                                                -size.width * (padRatio - 1f) * 0.5f,
                                                -size.height * (padRatio - 1f) * 0.5f,
                                                size.width * (padRatio + 1f) * 0.5f,
                                                size.height * (padRatio + 1f) * 0.5f
                                            ),
                                            paint
                                        )
                                        paint.alpha = 255
                                    }
                                    val drawMatrix = LayerTransformMatrix.buildPaddedBitmapDrawMatrix(
                                        bitmapWidth = preparedShapeBmp.width,
                                        bitmapHeight = preparedShapeBmp.height,
                                        padRatioX = padRatio,
                                        padRatioY = padRatio,
                                        boxW = size.width,
                                        boxH = size.height,
                                        transform3D = layer.transform3D,
                                        cornerPin = layer.cornerPin
                                    )
                                    nc.drawBitmap(preparedShapeBmp, drawMatrix, paint)
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.align(Alignment.Center).size(shapeDp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawIntoCanvas { canvas ->
                                    VectorShapeEngine.drawShapeOnCanvas(
                                        canvas = canvas.nativeCanvas,
                                        layer = layer.copy(opacity = 1f),
                                        baseSizePx = size.minDimension,
                                        cx = size.width * 0.5f,
                                        cy = size.height * 0.5f,
                                        strokeScaleFactor = density
                                    )
                                }
                            }
                        }
                    }

                    // Warped & parametric hit-test gesture box matching actual visual pixels
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (shapeOriginX + hitBounds.minX).roundToInt(),
                                    (shapeOriginY + hitBounds.minY).roundToInt()
                                )
                            }
                            .size(
                                width = with(LocalDensity.current) { hitBounds.width.toDp() },
                                height = with(LocalDensity.current) { hitBounds.height.toDp() }
                            )
                            .then(bodyGestureModifier)
                    )

                    if (showDots && parentGroup == null && (!isShapeEditPointsMode || !isPrimarySelected)) {
                        SelectionDotHandlesOverlay(
                            boxWpx = shapePx,
                            boxHpx = shapePx,
                            originX = shapeOriginX,
                            originY = shapeOriginY,
                            localRect = shapeLocalRect,
                            dotColor = dotAccent,
                            targetLayer = layer,
                            canvasZoom = canvasZoom,
                            locks = transformLocks,
                            showDeleteCross = isPrimarySelected,
                            onDeleteClick = { onDeleteLayer(layer.id) },
                            onBeginTransaction = onBeginTransformTransaction,
                            onEndTransaction = onEndTransformTransaction,
                            onUniformZoom = { zoomFactor ->
                                if (!effectiveLocked) {
                                    onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                }
                            },
                            onEdgePan = { localDx, localDy ->
                                if (!effectiveLocked) {
                                    val sx = layer.scale * layer.stretchX
                                    val sy = layer.scale * layer.stretchY
                                    val rad = Math.toRadians(layer.rotationDeg.toDouble())
                                    val ux = localDx * sx
                                    val uy = localDy * sy
                                    val dxPx = (ux * cos(rad) - uy * sin(rad)).toFloat()
                                    val dyPx = (ux * sin(rad) + uy * cos(rad)).toFloat()
                                    onTransformLayer(layer.id, dxPx / canvasWpx.coerceAtLeast(1f), dyPx / canvasHpx.coerceAtLeast(1f), 1f, 0f, 1f, 1f)
                                }
                            },
                            onAnchoredEdgeStretch = { sx, sy, sdx, sdy, bw, bh ->
                                if (!effectiveLocked) {
                                    onAnchoredStretch(layer.id, sx, sy, sdx, sdy, bw, bh, canvasWpx, canvasHpx)
                                }
                            }
                        )
                    }

                    if (isBeingCornerPinned && !effectiveLocked) {
                        CornerPinHandlesOverlay(
                            targetLayer = layer,
                            boxWpx = shapePx,
                            boxHpx = shapePx,
                            canvasWpx = canvasWpx,
                            canvasHpx = canvasHpx,
                            canvasZoom = canvasZoom,
                            onBeginTransaction = onBeginTransformTransaction,
                            onEndTransaction = onEndTransformTransaction,
                            onDragCorner = { cornerIdx, dDxRatio, dDyRatio ->
                                onCornerPinDrag(layer.id, cornerIdx, dDxRatio, dDyRatio)
                            }
                        )
                    }

                    if (showDots && parentGroup == null && isShapeEditPointsMode && isPrimarySelected && !effectiveLocked) {
                        VectorShapeHandlesOverlay(
                            targetLayer = layer,
                            shapePx = shapePx,
                            canvasWpx = canvasWpx,
                            canvasHpx = canvasHpx,
                            canvasZoom = canvasZoom,
                            onBeginTransaction = onBeginTransformTransaction,
                            onEndTransaction = onEndTransformTransaction,
                            onHandleDrag = { handleId, dxPx, dyPx ->
                                onShapeHandleDrag(layer.id, handleId, dxPx, dyPx, shapePx)
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
                val boxWpx = measured.widthPx.coerceAtLeast(48f)
                val boxHpx = measured.heightPx.coerceAtLeast(48f)
                val boxWdp = with(LocalDensity.current) { boxWpx.toDp() }
                val boxHdp = with(LocalDensity.current) { boxHpx.toDp() }

                val textOriginX = (canvasWpx - boxWpx) * 0.5f
                val textOriginY = (canvasHpx - boxHpx) * 0.5f

                val uniformZoom = (layer.scale * parentScale * canvasZoom).coerceAtLeast(1f)
                val renderScale = uniformZoom.coerceIn(1f, (1536f / effPx.coerceAtLeast(1f)).coerceAtLeast(1f))
                val hasPerspectiveOr3D = !layer.cornerPin.isIdentity() ||
                    !layer.transform3D.isIdentity() ||
                    isBeingCornerPinned

                if (hasPerspectiveOr3D) {
                    val texScale = uniformZoom.coerceIn(1f, 4f)
                        .coerceAtMost(960f / maxOf(boxWpx, boxHpx).coerceAtLeast(1f))
                        .coerceAtLeast(1f)
                    val preparedTextBmp = remember(
                        layer.text,
                        layer.fontId,
                        layer.fontSizeSp,
                        layer.colorArgb,
                        layer.isBold,
                        layer.isItalic,
                        layer.shadowEnabled,
                        layer.shadowRadius,
                        layer.shadowColorArgb,
                        layer.strokeWidth,
                        layer.strokeColorArgb,
                        layer.bgPillEnabled,
                        layer.bgPillColorArgb,
                        layer.letterSpacing,
                        layer.align,
                        document.emojiSourceMode,
                        document.appEmojiFontId,
                        boxWpx,
                        boxHpx,
                        texScale
                    ) {
                        val tw = (boxWpx * texScale).roundToInt().coerceIn(24, 1200)
                        val th = (boxHpx * texScale).roundToInt().coerceIn(24, 1200)
                        Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888).also { bmp ->
                            val tc = AndroidCanvas(bmp)
                            tc.translate(tw / 2f, th / 2f)
                            fontRenderEngine.drawTextLayerOnCanvas(
                                canvas = tc,
                                layer = layer.copy(opacity = 1f),
                                effectiveFontSizePx = effPx * texScale,
                                maxWidthPx = ((measured.widthPx + 4f).coerceAtMost(maxBoxW)) * texScale,
                                emojiSourceMode = document.emojiSourceMode,
                                appEmojiFontId = document.appEmojiFontId
                            )
                        }
                    }
                    val blurredGhost = remember(preparedTextBmp, isBeingCornerPinned) {
                        if (isBeingCornerPinned) LayerTransformMatrix.createFastBlurPreview(preparedTextBmp) else null
                    }
                    val mappedQuad = remember(boxWpx, boxHpx, layer.transform3D, layer.cornerPin) {
                        LayerTransformMatrix.mapCornerPoints(boxWpx, boxHpx, layer.transform3D, layer.cornerPin)
                    }
                    val hitBounds = remember(mappedQuad) {
                        LayerTransformMatrix.computeQuadBoundingBox(mappedQuad, paddingPx = 20f)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.align(Alignment.Center).size(width = boxWdp, height = boxHdp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawIntoCanvas { canvas ->
                                    val nc = canvas.nativeCanvas
                                    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
                                    if (isBeingCornerPinned && blurredGhost != null) {
                                        paint.alpha = 115
                                        nc.drawBitmap(
                                            blurredGhost,
                                            null,
                                            RectF(0f, 0f, size.width, size.height),
                                            paint
                                        )
                                        paint.alpha = 255
                                    }
                                    val drawMatrix = LayerTransformMatrix.buildBitmapDrawMatrix(
                                        bitmapWidth = preparedTextBmp.width,
                                        bitmapHeight = preparedTextBmp.height,
                                        boxW = size.width,
                                        boxH = size.height,
                                        transform3D = layer.transform3D,
                                        cornerPin = layer.cornerPin
                                    )
                                    nc.drawBitmap(preparedTextBmp, drawMatrix, paint)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (textOriginX + hitBounds.minX).roundToInt(),
                                        (textOriginY + hitBounds.minY).roundToInt()
                                    )
                                }
                                .size(
                                    width = with(LocalDensity.current) { hitBounds.width.toDp() },
                                    height = with(LocalDensity.current) { hitBounds.height.toDp() }
                                )
                                .then(bodyGestureModifier)
                        )

                        if (showDots && parentGroup == null) {
                            SelectionDotHandlesOverlay(
                                boxWpx = boxWpx,
                                boxHpx = boxHpx,
                                originX = textOriginX,
                                originY = textOriginY,
                                dotColor = dotAccent,
                                targetLayer = layer,
                                canvasZoom = canvasZoom,
                                locks = transformLocks,
                                showDeleteCross = isPrimarySelected,
                                onDeleteClick = { onDeleteLayer(layer.id) },
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onUniformZoom = { zoomFactor ->
                                    if (!effectiveLocked) {
                                        onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                    }
                                },
                                onEdgePan = { localDx, localDy ->
                                    if (!effectiveLocked) {
                                        val sx = layer.scale * layer.stretchX
                                        val sy = layer.scale * layer.stretchY
                                        val rad = Math.toRadians(layer.rotationDeg.toDouble())
                                        val ux = localDx * sx
                                        val uy = localDy * sy
                                        val dxPx = (ux * cos(rad) - uy * sin(rad)).toFloat()
                                        val dyPx = (ux * sin(rad) + uy * cos(rad)).toFloat()
                                        onTransformLayer(layer.id, dxPx / canvasWpx.coerceAtLeast(1f), dyPx / canvasHpx.coerceAtLeast(1f), 1f, 0f, 1f, 1f)
                                    }
                                },
                                onAnchoredEdgeStretch = { sx, sy, sdx, sdy, bw, bh ->
                                    if (!effectiveLocked) {
                                        onAnchoredStretch(layer.id, sx, sy, sdx, sdy, bw, bh, canvasWpx, canvasHpx)
                                    }
                                }
                            )
                        }

                        if (isBeingCornerPinned && !effectiveLocked) {
                            CornerPinHandlesOverlay(
                                targetLayer = layer,
                                boxWpx = boxWpx,
                                boxHpx = boxHpx,
                                canvasWpx = canvasWpx,
                                canvasHpx = canvasHpx,
                                canvasZoom = canvasZoom,
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onDragCorner = { cornerIdx, dDxRatio, dDyRatio ->
                                    onCornerPinDrag(layer.id, cornerIdx, dDxRatio, dDyRatio)
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(width = boxWdp, height = boxHdp)
                                .then(bodyGestureModifier)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawIntoCanvas { canvas ->
                                    canvas.nativeCanvas.save()
                                    canvas.nativeCanvas.translate(size.width / 2f, size.height / 2f)
                                    if (renderScale > 1.01f) {
                                        canvas.nativeCanvas.scale(1f / renderScale, 1f / renderScale)
                                    }
                                    fontRenderEngine.drawTextLayerOnCanvas(
                                        canvas = canvas.nativeCanvas,
                                        layer = layer,
                                        effectiveFontSizePx = effPx * renderScale,
                                        maxWidthPx = ((measured.widthPx + 4f).coerceAtMost(maxBoxW)) * renderScale,
                                        emojiSourceMode = document.emojiSourceMode,
                                        appEmojiFontId = document.appEmojiFontId
                                    )
                                    canvas.nativeCanvas.restore()
                                }
                            }
                        }

                        if (showDots && parentGroup == null) {
                            SelectionDotHandlesOverlay(
                                boxWpx = boxWpx,
                                boxHpx = boxHpx,
                                originX = textOriginX,
                                originY = textOriginY,
                                dotColor = dotAccent,
                                targetLayer = layer,
                                canvasZoom = canvasZoom,
                                locks = transformLocks,
                                showDeleteCross = isPrimarySelected,
                                onDeleteClick = { onDeleteLayer(layer.id) },
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onUniformZoom = { zoomFactor ->
                                    if (!effectiveLocked) {
                                        onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                    }
                                },
                                onEdgePan = { localDx, localDy ->
                                    if (!effectiveLocked) {
                                        val sx = layer.scale * layer.stretchX
                                        val sy = layer.scale * layer.stretchY
                                        val rad = Math.toRadians(layer.rotationDeg.toDouble())
                                        val ux = localDx * sx
                                        val uy = localDy * sy
                                        val dxPx = (ux * cos(rad) - uy * sin(rad)).toFloat()
                                        val dyPx = (ux * sin(rad) + uy * cos(rad)).toFloat()
                                        onTransformLayer(layer.id, dxPx / canvasWpx.coerceAtLeast(1f), dyPx / canvasHpx.coerceAtLeast(1f), 1f, 0f, 1f, 1f)
                                    }
                                },
                                onAnchoredEdgeStretch = { sx, sy, sdx, sdy, bw, bh ->
                                    if (!effectiveLocked) {
                                        onAnchoredStretch(layer.id, sx, sy, sdx, sdy, bw, bh, canvasWpx, canvasHpx)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            is DrawLayer -> {
                // Compute tight stroke bounding box so selection handles, 3D/Corner Pin, and gestures fit ONLY the drawn area
                val allPts = remember(layer.strokes) { layer.strokes.flatMap { it.points } }
                val minXNorm = (allPts.minOfOrNull { it.xNorm } ?: 0.25f).let { (it - 0.04f).coerceIn(0f, 0.92f) }
                val minYNorm = (allPts.minOfOrNull { it.yNorm } ?: 0.25f).let { (it - 0.04f).coerceIn(0f, 0.92f) }
                val maxXNorm = (allPts.maxOfOrNull { it.xNorm } ?: 0.75f).let { (it + 0.04f).coerceIn((minXNorm + 0.08f).coerceAtMost(1f), 1f) }
                val maxYNorm = (allPts.maxOfOrNull { it.yNorm } ?: 0.75f).let { (it + 0.04f).coerceIn((minYNorm + 0.08f).coerceAtMost(1f), 1f) }

                val boxOriginX = minXNorm * canvasWpx
                val boxOriginY = minYNorm * canvasHpx
                val boxWpx = ((maxXNorm - minXNorm) * canvasWpx).coerceAtLeast(48f)
                val boxHpx = ((maxYNorm - minYNorm) * canvasHpx).coerceAtLeast(48f)
                val boxLeftDp = with(LocalDensity.current) { boxOriginX.toDp() }
                val boxTopDp = with(LocalDensity.current) { boxOriginY.toDp() }
                val boxWidthDp = with(LocalDensity.current) { boxWpx.toDp() }
                val boxHeightDp = with(LocalDensity.current) { boxHpx.toDp() }
                val hasPerspectiveOr3D = !layer.cornerPin.isIdentity() ||
                    !layer.transform3D.isIdentity() ||
                    isBeingCornerPinned

                if (hasPerspectiveOr3D) {
                    val uniformZoom = (layer.scale * parentScale * canvasZoom).coerceAtLeast(1f)
                    val texScale = uniformZoom.coerceIn(1f, 3f)
                        .coerceAtMost(960f / maxOf(boxWpx, boxHpx).coerceAtLeast(1f))
                        .coerceAtLeast(1f)
                    val preparedDrawBmp = remember(
                        layer.strokes,
                        canvasWpx,
                        canvasHpx,
                        boxWpx,
                        boxHpx,
                        texScale
                    ) {
                        val tw = (boxWpx * texScale).roundToInt().coerceIn(24, 1200)
                        val th = (boxHpx * texScale).roundToInt().coerceIn(24, 1200)
                        Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888).also { bmp ->
                            val dc = AndroidCanvas(bmp)
                            dc.translate(-boxOriginX * texScale, -boxOriginY * texScale)
                            layer.strokes.forEach { stroke ->
                                VectorStrokeEngine.drawVectorStroke(
                                    canvas = dc,
                                    stroke = stroke,
                                    areaWidthPx = canvasWpx * texScale,
                                    areaHeightPx = canvasHpx * texScale,
                                    layerOpacity = 1f,
                                    originOffsetX = 0f,
                                    originOffsetY = 0f
                                )
                            }
                        }
                    }
                    val blurredGhost = remember(preparedDrawBmp, isBeingCornerPinned) {
                        if (isBeingCornerPinned) LayerTransformMatrix.createFastBlurPreview(preparedDrawBmp) else null
                    }

                    val mappedQuad = remember(boxWpx, boxHpx, layer.transform3D, layer.cornerPin) {
                        LayerTransformMatrix.mapCornerPoints(boxWpx, boxHpx, layer.transform3D, layer.cornerPin)
                    }
                    val hitBounds = remember(mappedQuad) {
                        LayerTransformMatrix.computeQuadBoundingBox(mappedQuad, paddingPx = 20f)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .offset(x = boxLeftDp, y = boxTopDp)
                                .size(width = boxWidthDp, height = boxHeightDp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawIntoCanvas { canvas ->
                                    val nc = canvas.nativeCanvas
                                    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
                                    if (isBeingCornerPinned && blurredGhost != null) {
                                        paint.alpha = 115
                                        nc.drawBitmap(
                                            blurredGhost,
                                            null,
                                            RectF(0f, 0f, size.width, size.height),
                                            paint
                                        )
                                        paint.alpha = 255
                                    }
                                    val drawMatrix = LayerTransformMatrix.buildBitmapDrawMatrix(
                                        bitmapWidth = preparedDrawBmp.width,
                                        bitmapHeight = preparedDrawBmp.height,
                                        boxW = size.width,
                                        boxH = size.height,
                                        transform3D = layer.transform3D,
                                        cornerPin = layer.cornerPin
                                    )
                                    nc.drawBitmap(preparedDrawBmp, drawMatrix, paint)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (boxOriginX + hitBounds.minX).roundToInt(),
                                        (boxOriginY + hitBounds.minY).roundToInt()
                                    )
                                }
                                .size(
                                    width = with(LocalDensity.current) { hitBounds.width.toDp() },
                                    height = with(LocalDensity.current) { hitBounds.height.toDp() }
                                )
                                .then(bodyGestureModifier)
                        )

                        if (showDots && parentGroup == null) {
                            SelectionDotHandlesOverlay(
                                boxWpx = boxWpx,
                                boxHpx = boxHpx,
                                originX = boxOriginX,
                                originY = boxOriginY,
                                dotColor = dotAccent,
                                targetLayer = layer,
                                canvasZoom = canvasZoom,
                                locks = transformLocks,
                                showDeleteCross = isPrimarySelected,
                                onDeleteClick = { onDeleteLayer(layer.id) },
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onUniformZoom = { zoomFactor ->
                                    if (!effectiveLocked) {
                                        onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                    }
                                },
                                onEdgePan = { localDx, localDy ->
                                    if (!effectiveLocked) {
                                        val sx = layer.scale * layer.stretchX
                                        val sy = layer.scale * layer.stretchY
                                        val rad = Math.toRadians(layer.rotationDeg.toDouble())
                                        val ux = localDx * sx
                                        val uy = localDy * sy
                                        val dxPx = (ux * cos(rad) - uy * sin(rad)).toFloat()
                                        val dyPx = (ux * sin(rad) + uy * cos(rad)).toFloat()
                                        onTransformLayer(layer.id, dxPx / canvasWpx.coerceAtLeast(1f), dyPx / canvasHpx.coerceAtLeast(1f), 1f, 0f, 1f, 1f)
                                    }
                                },
                                onAnchoredEdgeStretch = { sx, sy, sdx, sdy, bw, bh ->
                                    if (!effectiveLocked) {
                                        onAnchoredStretch(layer.id, sx, sy, sdx, sdy, bw, bh, canvasWpx, canvasHpx)
                                    }
                                }
                            )
                        }

                        if (isBeingCornerPinned && !effectiveLocked) {
                            CornerPinHandlesOverlay(
                                targetLayer = layer,
                                boxWpx = boxWpx,
                                boxHpx = boxHpx,
                                canvasWpx = canvasWpx,
                                canvasHpx = canvasHpx,
                                canvasZoom = canvasZoom,
                                originX = boxOriginX,
                                originY = boxOriginY,
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onDragCorner = { cornerIdx, dDxRatio, dDyRatio ->
                                    onCornerPinDrag(layer.id, cornerIdx, dDxRatio, dDyRatio)
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawIntoCanvas { canvas ->
                                layer.strokes.forEach { stroke ->
                                    VectorStrokeEngine.drawVectorStroke(
                                        canvas = canvas.nativeCanvas,
                                        stroke = stroke,
                                        areaWidthPx = size.width,
                                        areaHeightPx = size.height,
                                        layerOpacity = 1f,
                                        originOffsetX = 0f,
                                        originOffsetY = 0f
                                    )
                                }
                            }
                        }

                        // Interactive hit-box + handles tightly wrapping ONLY the drawn strokes!
                        Box(
                            modifier = Modifier
                                .offset(x = boxLeftDp, y = boxTopDp)
                                .size(width = boxWidthDp, height = boxHeightDp)
                                .then(bodyGestureModifier)
                        )

                        if (showDots && parentGroup == null) {
                            SelectionDotHandlesOverlay(
                                boxWpx = boxWpx,
                                boxHpx = boxHpx,
                                originX = boxOriginX,
                                originY = boxOriginY,
                                dotColor = dotAccent,
                                targetLayer = layer,
                                canvasZoom = canvasZoom,
                                locks = transformLocks,
                                showDeleteCross = isPrimarySelected,
                                onDeleteClick = { onDeleteLayer(layer.id) },
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onUniformZoom = { zoomFactor ->
                                    if (!effectiveLocked) {
                                        onTransformLayer(layer.id, 0f, 0f, zoomFactor, 0f, 1f, 1f)
                                    }
                                },
                                onEdgePan = { localDx, localDy ->
                                    if (!effectiveLocked) {
                                        val sx = layer.scale * layer.stretchX
                                        val sy = layer.scale * layer.stretchY
                                        val rad = Math.toRadians(layer.rotationDeg.toDouble())
                                        val ux = localDx * sx
                                        val uy = localDy * sy
                                        val dxPx = (ux * cos(rad) - uy * sin(rad)).toFloat()
                                        val dyPx = (ux * sin(rad) + uy * cos(rad)).toFloat()
                                        onTransformLayer(layer.id, dxPx / canvasWpx.coerceAtLeast(1f), dyPx / canvasHpx.coerceAtLeast(1f), 1f, 0f, 1f, 1f)
                                    }
                                },
                                onAnchoredEdgeStretch = { sx, sy, sdx, sdy, bw, bh ->
                                    if (!effectiveLocked) {
                                        onAnchoredStretch(layer.id, sx, sy, sdx, sdy, bw, bh, canvasWpx, canvasHpx)
                                    }
                                }
                            )
                        }
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
 * Interactive hand-gesture Crop Frame drawn directly over the selected Photo:
 * - Dragging Top, Bottom, Left, or Right edge crops cleanly from that edge only!
 * - Dragging 4 corners adjusts both adjacent edges smoothly.
 */
@Composable
private fun InteractiveCropOverlay(
    crop: ImageCropRect,
    onBeginTransaction: () -> Unit = {},
    onUpdateTransient: ((ImageCropRect) -> ImageCropRect) -> Unit,
    onCommit: ((ImageCropRect) -> ImageCropRect) -> Unit
) {
    val liveCrop by rememberUpdatedState(crop)
    var boxW by remember { mutableFloatStateOf(200f) }
    var boxH by remember { mutableFloatStateOf(200f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { sz ->
                boxW = sz.width.toFloat().coerceAtLeast(1f)
                boxH = sz.height.toFloat().coerceAtLeast(1f)
            }
            .testTag("interactive_crop_overlay")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val l = liveCrop.left * size.width
            val t = liveCrop.top * size.height
            val r = liveCrop.right * size.width
            val b = liveCrop.bottom * size.height
            val mask = Color.Black.copy(alpha = 0.58f)

            // Dim outside crop bounds
            drawRect(mask, topLeft = Offset(0f, 0f), size = Size(size.width, t))
            drawRect(mask, topLeft = Offset(0f, b), size = Size(size.width, size.height - b))
            drawRect(mask, topLeft = Offset(0f, t), size = Size(l, b - t))
            drawRect(mask, topLeft = Offset(r, t), size = Size(size.width - r, b - t))

            // Crop outline + Rule-of-thirds grid
            val cw = (r - l).coerceAtLeast(1f)
            val ch = (b - t).coerceAtLeast(1f)
            drawRect(
                color = Color.White,
                topLeft = Offset(l, t),
                size = Size(cw, ch),
                style = Stroke(width = 2.dp.toPx())
            )
            val gridColor = Color.White.copy(alpha = 0.45f)
            for (step in 1..2) {
                val gx = l + cw * (step / 3f)
                drawLine(gridColor, Offset(gx, t), Offset(gx, b), strokeWidth = 1.dp.toPx())
                val gy = t + ch * (step / 3f)
                drawLine(gridColor, Offset(l, gy), Offset(r, gy), strokeWidth = 1.dp.toPx())
            }
        }

        val lDp = with(LocalDensity.current) { (liveCrop.left * boxW).toDp() }
        val tDp = with(LocalDensity.current) { (liveCrop.top * boxH).toDp() }
        val rDp = with(LocalDensity.current) { (liveCrop.right * boxW).toDp() }
        val bDp = with(LocalDensity.current) { (liveCrop.bottom * boxH).toDp() }
        val centerXDp = with(LocalDensity.current) { (((liveCrop.left + liveCrop.right) * 0.5f) * boxW).toDp() }
        val centerYDp = with(LocalDensity.current) { (((liveCrop.top + liveCrop.bottom) * 0.5f) * boxH).toDp() }

        // Edge Crop Handles: Top, Bottom, Left, Right
        CropEdgeHandle(
            xDp = centerXDp,
            yDp = tDp,
            isHorizontalBar = true,
            tag = "crop_edge_top",
            onDragStart = onBeginTransaction,
            onDrag = { _, dy ->
                onUpdateTransient { cur -> cur.copy(top = (cur.top + dy / boxH).coerceIn(0f, cur.bottom - 0.15f)) }
            },
            onDragEnd = { onCommit { it } }
        )
        CropEdgeHandle(
            xDp = centerXDp,
            yDp = bDp,
            isHorizontalBar = true,
            tag = "crop_edge_bottom",
            onDragStart = onBeginTransaction,
            onDrag = { _, dy ->
                onUpdateTransient { cur -> cur.copy(bottom = (cur.bottom + dy / boxH).coerceIn(cur.top + 0.15f, 1f)) }
            },
            onDragEnd = { onCommit { it } }
        )
        CropEdgeHandle(
            xDp = lDp,
            yDp = centerYDp,
            isHorizontalBar = false,
            tag = "crop_edge_left",
            onDragStart = onBeginTransaction,
            onDrag = { dx, _ ->
                onUpdateTransient { cur -> cur.copy(left = (cur.left + dx / boxW).coerceIn(0f, cur.right - 0.15f)) }
            },
            onDragEnd = { onCommit { it } }
        )
        CropEdgeHandle(
            xDp = rDp,
            yDp = centerYDp,
            isHorizontalBar = false,
            tag = "crop_edge_right",
            onDragStart = onBeginTransaction,
            onDrag = { dx, _ ->
                onUpdateTransient { cur -> cur.copy(right = (cur.right + dx / boxW).coerceIn(cur.left + 0.15f, 1f)) }
            },
            onDragEnd = { onCommit { it } }
        )

        // 4 Corner Crop Handles
        CropCornerHandle(lDp, tDp, "crop_corner_tl", onDragStart = onBeginTransaction, onDrag = { dx, dy ->
            onUpdateTransient { cur ->
                cur.copy(
                    left = (cur.left + dx / boxW).coerceIn(0f, cur.right - 0.15f),
                    top = (cur.top + dy / boxH).coerceIn(0f, cur.bottom - 0.15f)
                )
            }
        }, onDragEnd = { onCommit { it } })
        CropCornerHandle(rDp, tDp, "crop_corner_tr", onDragStart = onBeginTransaction, onDrag = { dx, dy ->
            onUpdateTransient { cur ->
                cur.copy(
                    right = (cur.right + dx / boxW).coerceIn(cur.left + 0.15f, 1f),
                    top = (cur.top + dy / boxH).coerceIn(0f, cur.bottom - 0.15f)
                )
            }
        }, onDragEnd = { onCommit { it } })
        CropCornerHandle(lDp, bDp, "crop_corner_bl", onDragStart = onBeginTransaction, onDrag = { dx, dy ->
            onUpdateTransient { cur ->
                cur.copy(
                    left = (cur.left + dx / boxW).coerceIn(0f, cur.right - 0.15f),
                    bottom = (cur.bottom + dy / boxH).coerceIn(cur.top + 0.15f, 1f)
                )
            }
        }, onDragEnd = { onCommit { it } })
        CropCornerHandle(rDp, bDp, "crop_corner_br", onDragStart = onBeginTransaction, onDrag = { dx, dy ->
            onUpdateTransient { cur ->
                cur.copy(
                    right = (cur.right + dx / boxW).coerceIn(cur.left + 0.15f, 1f),
                    bottom = (cur.bottom + dy / boxH).coerceIn(cur.top + 0.15f, 1f)
                )
            }
        }, onDragEnd = { onCommit { it } })
    }
}

@Composable
private fun CropEdgeHandle(
    xDp: androidx.compose.ui.unit.Dp,
    yDp: androidx.compose.ui.unit.Dp,
    isHorizontalBar: Boolean,
    tag: String,
    onDragStart: () -> Unit = {},
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset(x = xDp - 18.dp, y = yDp - 18.dp)
            .size(36.dp)
            .pointerInput(tag) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = if (isHorizontalBar) 24.dp else 6.dp,
                    height = if (isHorizontalBar) 6.dp else 24.dp
                )
                .clip(RoundedCornerShape(999.dp))
                .background(StudioEmerald)
                .border(1.5.dp, Color.White, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun CropCornerHandle(
    xDp: androidx.compose.ui.unit.Dp,
    yDp: androidx.compose.ui.unit.Dp,
    tag: String,
    onDragStart: () -> Unit = {},
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset(x = xDp - 16.dp, y = yDp - 16.dp)
            .size(32.dp)
            .pointerInput(tag) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(StudioEmerald)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun CornerPinHandlesOverlay(
    targetLayer: EditorLayer,
    boxWpx: Float,
    boxHpx: Float,
    canvasWpx: Float,
    canvasHpx: Float,
    canvasZoom: Float,
    originX: Float = (canvasWpx - boxWpx.coerceAtLeast(1f)) / 2f,
    originY: Float = (canvasHpx - boxHpx.coerceAtLeast(1f)) / 2f,
    onBeginTransaction: () -> Unit,
    onEndTransaction: () -> Unit,
    onDragCorner: (cornerIndex: Int, deltaDxRatio: Float, deltaDyRatio: Float) -> Unit
) {
    val liveLayer by rememberUpdatedState(targetLayer)
    val liveCanvasZoom by rememberUpdatedState(canvasZoom)
    val safeW = boxWpx.coerceAtLeast(1f)
    val safeH = boxHpx.coerceAtLeast(1f)

    val mappedPts = LayerTransformMatrix.mapCornerPoints(
        boxW = safeW,
        boxH = safeH,
        transform3D = liveLayer.transform3D,
        cornerPin = liveLayer.cornerPin
    )
    val invScaleX = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
    val invScaleY = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
    val handleSizeDp = 36.dp
    val handleHalfPx = with(LocalDensity.current) { (handleSizeDp / 2f).toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dashed reference original outline + solid warped quad outline
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            drawRect(
                color = Color.White.copy(alpha = 0.45f),
                topLeft = Offset(originX, originY),
                size = Size(safeW, safeH),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = dash)
            )
            val q = Path().apply {
                moveTo(originX + mappedPts[0], originY + mappedPts[1])
                lineTo(originX + mappedPts[2], originY + mappedPts[3])
                lineTo(originX + mappedPts[4], originY + mappedPts[5])
                lineTo(originX + mappedPts[6], originY + mappedPts[7])
                close()
            }
            drawPath(
                path = q,
                color = StudioAzure,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 4 independent corner pin handles directly over canvas quad corners
        for (idx in 0 until 4) {
            val cx = originX + mappedPts[idx * 2]
            val cy = originY + mappedPts[idx * 2 + 1]
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (cx - handleHalfPx).roundToInt(),
                            (cy - handleHalfPx).roundToInt()
                        )
                    }
                    .size(handleSizeDp)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .pointerInput(liveLayer.id, idx) {
                        detectScaleAwareDragGestures(
                            effectiveScaleProvider = {
                                val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                                val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                                liveCanvasZoom * max(liveLayer.scale * liveLayer.stretchX * lx, liveLayer.scale * liveLayer.stretchY * ly)
                            },
                            onDragStart = { onBeginTransaction() },
                            onDragEnd = { onEndTransaction() }
                        ) { change, dragAmount ->
                            change.consume()
                            val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                            val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                            val realDx = dragAmount.x * lx
                            val realDy = dragAmount.y * ly
                            onDragCorner(idx, realDx / safeW, realDy / safeH)
                        }
                    }
                    .testTag("corner_pin_handle_$idx"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(StudioAmber)
                        .border(2.5.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

/**
 * Alight-Motion-Style Parametric Vector Shape Handles Overlay:
 * Renders the crisp white outline, small center origin ring, minimal cardinal dots (1..4 dots
 * for Circle/Square), corner/inner-radius `||` pills, and double-ring vertex handles for
 * Triangle, Star, Arrow, Pie, and Quad.
 */
@Composable
private fun VectorShapeHandlesOverlay(
    targetLayer: ShapeLayer,
    shapePx: Float,
    canvasWpx: Float,
    canvasHpx: Float,
    canvasZoom: Float,
    onBeginTransaction: () -> Unit,
    onEndTransaction: () -> Unit,
    onHandleDrag: (handleId: String, dxPx: Float, dyPx: Float) -> Unit
) {
    val liveLayer by rememberUpdatedState(targetLayer)
    val liveCanvasZoom by rememberUpdatedState(canvasZoom)
    val originX = (canvasWpx - shapePx) * 0.5f
    val originY = (canvasHpx - shapePx) * 0.5f
    val invScaleX = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
    val invScaleY = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
    val handleBoxSize = 40.dp
    val handleHalfPx = with(LocalDensity.current) { (handleBoxSize / 2f).toPx() }
    val handles = remember(liveLayer.shapeType, liveLayer.vectorParams, shapePx) {
        VectorShapeEngine.computeControlHandles(liveLayer, shapePx)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vector_shape_handles_overlay")
    ) {
        // Warped Alight Motion white outline + mapped center crosshair ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val localOutline = VectorShapeEngine.buildShapePath(
                shapeType = liveLayer.shapeType,
                params = liveLayer.vectorParams,
                baseSizePx = shapePx,
                cx = shapePx * 0.5f,
                cy = shapePx * 0.5f
            )
            val warpedOutline = LayerTransformMatrix.mapPath(
                source = localOutline,
                boxW = shapePx,
                boxH = shapePx,
                transform3D = liveLayer.transform3D,
                cornerPin = liveLayer.cornerPin
            )
            warpedOutline.offset(originX, originY)
            drawPath(
                path = warpedOutline.asComposePath(),
                color = Color.White.copy(alpha = 0.9f),
                style = Stroke(width = 1.6.dp.toPx() * min(invScaleX, invScaleY))
            )

            val mappedCenter = LayerTransformMatrix.mapPoints(
                boxW = shapePx,
                boxH = shapePx,
                transform3D = liveLayer.transform3D,
                cornerPin = liveLayer.cornerPin,
                pts = floatArrayOf(shapePx * 0.5f, shapePx * 0.5f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = 4.dp.toPx() * min(invScaleX, invScaleY),
                center = Offset(originX + mappedCenter[0], originY + mappedCenter[1]),
                style = Stroke(width = 1.2.dp.toPx() * min(invScaleX, invScaleY))
            )
        }

        handles.forEach { handle ->
            val mappedPt = LayerTransformMatrix.mapPoints(
                boxW = shapePx,
                boxH = shapePx,
                transform3D = liveLayer.transform3D,
                cornerPin = liveLayer.cornerPin,
                pts = floatArrayOf(handle.xPx, handle.yPx)
            )
            val hx = originX + mappedPt[0]
            val hy = originY + mappedPt[1]
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (hx - handleHalfPx).roundToInt(),
                            (hy - handleHalfPx).roundToInt()
                        )
                    }
                    .size(handleBoxSize)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .pointerInput(liveLayer.id, handle.id) {
                        detectScaleAwareDragGestures(
                            effectiveScaleProvider = {
                                val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                                val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                                liveCanvasZoom * max(
                                    liveLayer.scale * liveLayer.stretchX * lx,
                                    liveLayer.scale * liveLayer.stretchY * ly
                                )
                            },
                            onDragStart = { onBeginTransaction() },
                            onDragEnd = { onEndTransaction() }
                        ) { change, dragAmount ->
                            change.consume()
                            val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                            val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                            val realDx = dragAmount.x * lx
                            val realDy = dragAmount.y * ly
                            val effectM = LayerTransformMatrix.computeLocalEffectMatrix(
                                boxW = shapePx,
                                boxH = shapePx,
                                transform3D = liveLayer.transform3D,
                                cornerPin = liveLayer.cornerPin
                            )
                            val invM = android.graphics.Matrix()
                            if (effectM.invert(invM)) {
                                val anchor = floatArrayOf(handle.xPx, handle.yPx)
                                effectM.mapPoints(anchor)
                                val probe = floatArrayOf(anchor[0], anchor[1], anchor[0] + realDx, anchor[1] + realDy)
                                invM.mapPoints(probe)
                                onHandleDrag(
                                    handle.id,
                                    (probe[2] - probe[0]).coerceIn(-120f, 120f),
                                    (probe[3] - probe[1]).coerceIn(-120f, 120f)
                                )
                            } else {
                                onHandleDrag(handle.id, realDx, realDy)
                            }
                        }
                    }
                    .testTag("shape_handle_${handle.id}"),
                contentAlignment = Alignment.Center
            ) {
                when (handle.kind) {
                    ShapeHandleKind.CARDINAL_DOT -> {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.8.dp, Color(0xFF1F2937), CircleShape)
                        )
                    }
                    ShapeHandleKind.VERTEX_RING -> {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, StudioAzure, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StudioAzure)
                            )
                        }
                    }
                    ShapeHandleKind.RADIUS_PILL -> {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .size(width = 20.dp, height = 15.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                                .border(1.5.dp, Color(0xFF1F2937), RoundedCornerShape(4.dp)),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 2.dp, height = 8.dp)
                                    .background(Color(0xFF1F2937))
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 2.dp, height = 8.dp)
                                    .background(Color(0xFF1F2937))
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun renderGroupChildToAndroidCanvas(
    canvas: AndroidCanvas,
    layer: EditorLayer,
    document: ProjectDocument,
    imageBitmaps: Map<String, Bitmap>,
    fontRenderEngine: FontRenderEngine,
    canvasWpx: Float,
    canvasHpx: Float,
    baseUnitPx: Float,
    density: Float,
    parentOpacity: Float,
    parentScale: Float
) {
    if (!layer.visible) return
    val effectiveOpacity = (layer.opacity * parentOpacity).coerceIn(0f, 1f)
    val cumulativeScale = (layer.scale * parentScale).coerceAtLeast(0.05f)
    canvas.save()
    canvas.translate(layer.offsetXNorm * canvasWpx, layer.offsetYNorm * canvasHpx)
    canvas.rotate(layer.rotationDeg)
    canvas.scale(layer.scale * layer.stretchX, layer.scale * layer.stretchY)

    when (layer) {
        is GroupLayer -> {
            layer.children.forEach { child ->
                renderGroupChildToAndroidCanvas(
                    canvas = canvas,
                    layer = child,
                    document = document,
                    imageBitmaps = imageBitmaps,
                    fontRenderEngine = fontRenderEngine,
                    canvasWpx = canvasWpx,
                    canvasHpx = canvasHpx,
                    baseUnitPx = baseUnitPx,
                    density = density,
                    parentOpacity = effectiveOpacity,
                    parentScale = cumulativeScale
                )
            }
        }
        is ImageLayer -> {
            val bmp = imageBitmaps[layer.uriString]
            if (bmp != null) {
                val cropNorm = layer.crop.normalized()
                val srcW = ((cropNorm.right - cropNorm.left) * bmp.width).roundToInt().coerceAtLeast(1)
                val srcH = ((cropNorm.bottom - cropNorm.top) * bmp.height).roundToInt().coerceAtLeast(1)
                val imgRatio = srcW.toFloat() / srcH.toFloat()
                val drawW = if (imgRatio >= 1f) baseUnitPx * 0.85f else baseUnitPx * 0.85f * imgRatio
                val drawH = if (imgRatio >= 1f) baseUnitPx * 0.85f / imgRatio else baseUnitPx * 0.85f
                val prepared = buildPreparedImageBitmap(
                    bmp = bmp,
                    cropNorm = cropNorm,
                    cropShape = layer.cropShape,
                    adjustments = layer.adjustments,
                    tintColorArgb = layer.tintColorArgb,
                    maxDim = 640
                )
                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG).apply {
                    alpha = (effectiveOpacity * 255f).roundToInt().coerceIn(0, 255)
                }
                val m = LayerTransformMatrix.buildBitmapDrawMatrix(
                    bitmapWidth = prepared.width,
                    bitmapHeight = prepared.height,
                    boxW = drawW,
                    boxH = drawH,
                    transform3D = layer.transform3D,
                    cornerPin = layer.cornerPin
                )
                m.postTranslate(-drawW / 2f, -drawH / 2f)
                canvas.drawBitmap(prepared, m, paint)
                prepared.recycle()
            }
        }
        is ShapeLayer -> {
            val baseSize = baseUnitPx * 0.34f
            if (layer.cornerPin.isIdentity() && layer.transform3D.isIdentity()) {
                VectorShapeEngine.drawShapeOnCanvas(
                    canvas = canvas,
                    layer = layer.copy(opacity = effectiveOpacity),
                    baseSizePx = baseSize,
                    cx = 0f,
                    cy = 0f,
                    strokeScaleFactor = density
                )
            } else {
                val padRatio = 2.0f
                val tw = (baseSize * padRatio).roundToInt().coerceIn(48, 800)
                val th = (baseSize * padRatio).roundToInt().coerceIn(48, 800)
                val shapeBmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
                val sc = AndroidCanvas(shapeBmp)
                VectorShapeEngine.drawShapeOnCanvas(
                    canvas = sc,
                    layer = layer.copy(opacity = 1f),
                    baseSizePx = baseSize,
                    cx = tw * 0.5f,
                    cy = th * 0.5f,
                    strokeScaleFactor = density
                )
                val m = LayerTransformMatrix.buildPaddedBitmapDrawMatrix(
                    bitmapWidth = tw,
                    bitmapHeight = th,
                    padRatioX = padRatio,
                    padRatioY = padRatio,
                    boxW = baseSize,
                    boxH = baseSize,
                    transform3D = layer.transform3D,
                    cornerPin = layer.cornerPin
                )
                m.postTranslate(-baseSize / 2f, -baseSize / 2f)
                val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG).apply {
                    alpha = (effectiveOpacity * 255f).roundToInt().coerceIn(0, 255)
                }
                canvas.drawBitmap(shapeBmp, m, p)
                shapeBmp.recycle()
            }
        }
        is TextLayer -> {
            val effPx = layer.fontSizeSp * density
            val maxBoxW = (canvasWpx * 0.92f).coerceAtLeast(64f)
            val measured = fontRenderEngine.measureTextLayerBounds(
                layer = layer.copy(opacity = effectiveOpacity),
                effectiveFontSizePx = effPx,
                maxWidthPx = maxBoxW,
                emojiSourceMode = document.emojiSourceMode,
                appEmojiFontId = document.appEmojiFontId
            )
            if (layer.cornerPin.isIdentity() && layer.transform3D.isIdentity()) {
                fontRenderEngine.drawTextLayerOnCanvas(
                    canvas = canvas,
                    layer = layer.copy(opacity = effectiveOpacity),
                    effectiveFontSizePx = effPx,
                    maxWidthPx = (measured.widthPx + 4f).coerceAtMost(maxBoxW),
                    emojiSourceMode = document.emojiSourceMode,
                    appEmojiFontId = document.appEmojiFontId
                )
            } else {
                val bw = measured.widthPx.coerceAtLeast(48f)
                val bh = measured.heightPx.coerceAtLeast(48f)
                val tw = bw.roundToInt().coerceIn(24, 960)
                val th = bh.roundToInt().coerceIn(24, 960)
                val tb = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
                val tc = AndroidCanvas(tb)
                tc.translate(tw / 2f, th / 2f)
                fontRenderEngine.drawTextLayerOnCanvas(
                    canvas = tc,
                    layer = layer.copy(opacity = effectiveOpacity),
                    effectiveFontSizePx = effPx,
                    maxWidthPx = (measured.widthPx + 4f).coerceAtMost(maxBoxW),
                    emojiSourceMode = document.emojiSourceMode,
                    appEmojiFontId = document.appEmojiFontId
                )
                val m = LayerTransformMatrix.buildBitmapDrawMatrix(
                    bitmapWidth = tw,
                    bitmapHeight = th,
                    boxW = bw,
                    boxH = bh,
                    transform3D = layer.transform3D,
                    cornerPin = layer.cornerPin
                )
                m.postTranslate(-bw / 2f, -bh / 2f)
                canvas.drawBitmap(tb, m, AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG))
                tb.recycle()
            }
        }
        is DrawLayer -> {
            layer.strokes.forEach { stroke ->
                VectorStrokeEngine.drawVectorStroke(
                    canvas = canvas,
                    stroke = stroke,
                    areaWidthPx = canvasWpx,
                    areaHeightPx = canvasHpx,
                    layerOpacity = effectiveOpacity,
                    originOffsetX = -canvasWpx * 0.5f,
                    originOffsetY = -canvasHpx * 0.5f
                )
            }
        }
    }
    canvas.restore()
}

private fun buildPreparedImageBitmap(
    bmp: Bitmap,
    cropNorm: ImageCropRect,
    cropShape: ImageCropShape,
    adjustments: com.example.model.ImageAdjustments,
    tintColorArgb: Long?,
    maxDim: Int = 900
): Bitmap {
    val srcRect = AndroidRect(
        (cropNorm.left * bmp.width).roundToInt().coerceIn(0, (bmp.width - 1).coerceAtLeast(0)),
        (cropNorm.top * bmp.height).roundToInt().coerceIn(0, (bmp.height - 1).coerceAtLeast(0)),
        (cropNorm.right * bmp.width).roundToInt().coerceIn(1, bmp.width.coerceAtLeast(1)),
        (cropNorm.bottom * bmp.height).roundToInt().coerceIn(1, bmp.height.coerceAtLeast(1))
    )
    val cw = srcRect.width().coerceAtLeast(1)
    val ch = srcRect.height().coerceAtLeast(1)
    val scale = min(1f, maxDim.toFloat() / max(cw, ch).coerceAtLeast(1))
    val outW = (cw * scale).roundToInt().coerceIn(16, maxDim)
    val outH = (ch * scale).roundToInt().coerceIn(16, maxDim)
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(out)
    val dst = RectF(0f, 0f, outW.toFloat(), outH.toFloat())
    if (cropShape == ImageCropShape.CIRCLE) {
        val oval = android.graphics.Path().apply {
            addOval(dst, android.graphics.Path.Direction.CW)
        }
        c.clipPath(oval)
    }
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG).apply {
        if (!adjustments.isIdentity()) {
            colorFilter = ColorMatrixColorFilter(MediaStorageManager.buildColorMatrix(adjustments))
        }
    }
    if (tintColorArgb != null) {
        val save = c.saveLayer(dst, null)
        c.drawBitmap(bmp, srcRect, dst, paint)
        val tintPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = tintColorArgb.toInt()
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        c.drawRect(dst, tintPaint)
        c.restoreToCount(save)
    } else {
        c.drawBitmap(bmp, srcRect, dst, paint)
    }
    if (adjustments.vignette > 0.01f) {
        val radius = maxOf(dst.width(), dst.height()) * 0.72f
        val edgeAlpha = (adjustments.vignette.coerceIn(0f, 1f) * 0.8f * 255f).roundToInt().coerceIn(0, 255)
        val vigPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                dst.centerX(),
                dst.centerY(),
                radius,
                intArrayOf(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.argb(edgeAlpha / 3, 0, 0, 0),
                    android.graphics.Color.argb(edgeAlpha, 0, 0, 0)
                ),
                floatArrayOf(0.45f, 0.76f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        c.drawRect(dst, vigPaint)
    }
    return out
}

private fun ensureMinBoundsRect(rect: RectF, minSpan: Float = 28f): RectF {
    val cx = rect.centerX()
    val cy = rect.centerY()
    val hw = max(rect.width() * 0.5f, minSpan * 0.5f)
    val hh = max(rect.height() * 0.5f, minSpan * 0.5f)
    return RectF(cx - hw, cy - hh, cx + hw, cy + hh)
}

/**
 * Warped & Deforming Selection Handle Layout:
 * - Follows any Layer3DTransform, CornerPinOffsets, and parametric ShapeLayer geometry tightly.
 * - Top-Left mapped corner: Cross (✕) Delete icon button.
 * - Bottom-Right mapped corner: Larger diagonal Zoom icon handle.
 * - Top-Right & Bottom-Left mapped corners: Corner zoom dots.
 * - 4 Edge Midpoints: Localized anchored-stretch / zoom / pan dots.
 */
@Composable
private fun SelectionDotHandlesOverlay(
    boxWpx: Float,
    boxHpx: Float,
    originX: Float,
    originY: Float,
    localRect: RectF = RectF(0f, 0f, boxWpx, boxHpx),
    dotColor: Color,
    targetLayer: EditorLayer,
    canvasZoom: Float,
    locks: TransformLockState,
    showDeleteCross: Boolean,
    onDeleteClick: () -> Unit,
    onBeginTransaction: () -> Unit = {},
    onEndTransaction: () -> Unit = {},
    onUniformZoom: (scaleFactor: Float) -> Unit,
    onEdgePan: (localDx: Float, localDy: Float) -> Unit,
    onAnchoredEdgeStretch: (
        signX: Float,
        signY: Float,
        screenLocalDeltaX: Float,
        screenLocalDeltaY: Float,
        boxWpx: Float,
        boxHpx: Float
    ) -> Unit
) {
    val liveLayer by rememberUpdatedState(targetLayer)
    val liveLocks by rememberUpdatedState(locks)
    val liveCanvasZoom by rememberUpdatedState(canvasZoom)
    val density = LocalDensity.current

    val invScaleX = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
    val invScaleY = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
    val strokeCompensation = min(invScaleX, invScaleY)

    val rectW = localRect.width().coerceAtLeast(24f)
    val rectH = localRect.height().coerceAtLeast(24f)

    val quadPts = LayerTransformMatrix.mapQuadForRect(
        left = localRect.left,
        top = localRect.top,
        right = localRect.right,
        bottom = localRect.bottom,
        boxW = boxWpx.coerceAtLeast(1f),
        boxH = boxHpx.coerceAtLeast(1f),
        transform3D = liveLayer.transform3D,
        cornerPin = liveLayer.cornerPin
    )
    val tlX = originX + quadPts[0]; val tlY = originY + quadPts[1]
    val trX = originX + quadPts[2]; val trY = originY + quadPts[3]
    val brX = originX + quadPts[4]; val brY = originY + quadPts[5]
    val blX = originX + quadPts[6]; val blY = originY + quadPts[7]

    val topMidX = (tlX + trX) * 0.5f; val topMidY = (tlY + trY) * 0.5f
    val botMidX = (blX + brX) * 0.5f; val botMidY = (blY + brY) * 0.5f
    val leftMidX = (tlX + blX) * 0.5f; val leftMidY = (tlY + blY) * 0.5f
    val rightMidX = (trX + brX) * 0.5f; val rightMidY = (trY + brY) * 0.5f

    val centerX = (tlX + trX + brX + blX) * 0.25f
    val centerY = (tlY + trY + brY + blY) * 0.25f

    fun outwardOffsetPoint(px: Float, py: Float, fallbackX: Float, fallbackY: Float, pushDp: Float): Pair<Float, Float> {
        val dx = px - centerX
        val dy = py - centerY
        val len = hypot(dx, dy)
        val ux = if (len > 1f) dx / len else fallbackX
        val uy = if (len > 1f) dy / len else fallbackY
        val pushPx = with(density) { pushDp.dp.toPx() }
        return (px + ux * pushPx * invScaleX) to (py + uy * pushPx * invScaleY)
    }

    fun radialUnit(px: Float, py: Float, fallbackX: Float, fallbackY: Float): Pair<Float, Float> {
        val dx = px - centerX
        val dy = py - centerY
        val len = hypot(dx, dy)
        return if (len > 1f) (dx / len) to (dy / len) else (fallbackX to fallbackY)
    }

    val deleteBoxPx = with(density) { 34.dp.toPx() }
    val cornerDotBoxPx = with(density) { 26.dp.toPx() }
    val zoomHandleBoxPx = with(density) { 40.dp.toPx() }
    val edgeVBoxWpx = with(density) { 22.dp.toPx() }
    val edgeVBoxHpx = with(density) { 20.dp.toPx() }
    val edgeHBoxWpx = with(density) { 20.dp.toPx() }
    val edgeHBoxHpx = with(density) { 22.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Warped quadrilateral selection border matching exact pixel geometry
        Canvas(modifier = Modifier.fillMaxSize()) {
            val qPath = Path().apply {
                moveTo(tlX, tlY)
                lineTo(trX, trY)
                lineTo(brX, brY)
                lineTo(blX, blY)
                close()
            }
            drawPath(
                path = qPath,
                color = dotColor.copy(alpha = 0.65f),
                style = Stroke(width = 1.2.dp.toPx() * strokeCompensation)
            )
        }

        // 1. Mapped Top-Left Cross (✕) Delete Button
        if (showDeleteCross) {
            val (delCx, delCy) = outwardOffsetPoint(tlX, tlY, -0.707f, -0.707f, 8f)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (delCx - deleteBoxPx * 0.5f).roundToInt(),
                            (delCy - deleteBoxPx * 0.5f).roundToInt()
                        )
                    }
                    .size(34.dp)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .clickable(onClick = onDeleteClick)
                    .testTag("selection_delete_cross_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(StudioCoral)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Delete Layer",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 2. Mapped Top-Right & Bottom-Left Corner Zoom Dots
        listOf(
            Triple(trX, trY, 0.707f to -0.707f),
            Triple(blX, blY, -0.707f to 0.707f)
        ).forEachIndexed { idx, (px, py, fallbackDir) ->
            val (cPosX, cPosY) = outwardOffsetPoint(px, py, fallbackDir.first, fallbackDir.second, 5f)
            val (rx, ry) = radialUnit(px, py, fallbackDir.first, fallbackDir.second)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (cPosX - cornerDotBoxPx * 0.5f).roundToInt(),
                            (cPosY - cornerDotBoxPx * 0.5f).roundToInt()
                        )
                    }
                    .size(26.dp)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .pointerInput(liveLayer.id, "corner_$idx") {
                        detectScaleAwareDragGestures(
                            effectiveScaleProvider = {
                                val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                                val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                                liveCanvasZoom * max(liveLayer.scale * liveLayer.stretchX * lx, liveLayer.scale * liveLayer.stretchY * ly)
                            },
                            onDragStart = onBeginTransaction,
                            onDragEnd = onEndTransaction
                        ) { change, dragAmount ->
                            change.consume()
                            val curLocks = liveLocks
                            val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                            val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                            val realDeltaX = dragAmount.x * (liveLayer.scale * liveLayer.stretchX * lx)
                            val realDeltaY = dragAmount.y * (liveLayer.scale * liveLayer.stretchY * ly)
                            if (!curLocks.zoomLocked) {
                                val radialDelta = (realDeltaX * rx + realDeltaY * ry) * 0.78f
                                val curSpan = min(
                                    rectW * liveLayer.scale * liveLayer.stretchX,
                                    rectH * liveLayer.scale * liveLayer.stretchY
                                ).coerceAtLeast(40f)
                                val zoomFactor = (1f + radialDelta / curSpan).coerceIn(0.88f, 1.14f)
                                onUniformZoom(zoomFactor)
                            } else if (!curLocks.positionLocked) {
                                onEdgePan(realDeltaX / (liveLayer.scale * liveLayer.stretchX), realDeltaY / (liveLayer.scale * liveLayer.stretchY))
                            }
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

        // 3. Mapped Bottom-Right Corner: Larger Zoom Handle Icon
        run {
            val (brPosX, brPosY) = outwardOffsetPoint(brX, brY, 0.707f, 0.707f, 8f)
            val (rx, ry) = radialUnit(brX, brY, 0.707f, 0.707f)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (brPosX - zoomHandleBoxPx * 0.5f).roundToInt(),
                            (brPosY - zoomHandleBoxPx * 0.5f).roundToInt()
                        )
                    }
                    .size(40.dp)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .pointerInput(liveLayer.id, "bottom_right_zoom") {
                        detectScaleAwareDragGestures(
                            effectiveScaleProvider = {
                                val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                                val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                                liveCanvasZoom * max(liveLayer.scale * liveLayer.stretchX * lx, liveLayer.scale * liveLayer.stretchY * ly)
                            },
                            onDragStart = onBeginTransaction,
                            onDragEnd = onEndTransaction
                        ) { change, dragAmount ->
                            change.consume()
                            val curLocks = liveLocks
                            val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                            val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                            val realDeltaX = dragAmount.x * (liveLayer.scale * liveLayer.stretchX * lx)
                            val realDeltaY = dragAmount.y * (liveLayer.scale * liveLayer.stretchY * ly)
                            if (!curLocks.zoomLocked) {
                                val radialDelta = (realDeltaX * rx + realDeltaY * ry) * 0.78f
                                val curSpan = min(
                                    rectW * liveLayer.scale * liveLayer.stretchX,
                                    rectH * liveLayer.scale * liveLayer.stretchY
                                ).coerceAtLeast(40f)
                                val zoomFactor = (1f + radialDelta / curSpan).coerceIn(0.88f, 1.14f)
                                onUniformZoom(zoomFactor)
                            } else if (!curLocks.positionLocked) {
                                onEdgePan(realDeltaX / (liveLayer.scale * liveLayer.stretchX), realDeltaY / (liveLayer.scale * liveLayer.stretchY))
                            }
                        }
                    }
                    .testTag("corner_zoom_icon_handle"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInFull,
                        contentDescription = "Zoom Layer",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // 4. Mapped Top & Bottom Edge Midpoint Handles
        listOf(
            Triple(topMidX, topMidY, -1f),
            Triple(botMidX, botMidY, 1f)
        ).forEachIndexed { idx, (mx, my, signY) ->
            val (posX, posY) = outwardOffsetPoint(mx, my, 0f, signY, 5f)
            val (rx, ry) = radialUnit(mx, my, 0f, signY)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (posX - edgeVBoxWpx * 0.5f).roundToInt(),
                            (posY - edgeVBoxHpx * 0.5f).roundToInt()
                        )
                    }
                    .size(width = 22.dp, height = 20.dp)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .pointerInput(liveLayer.id, "anchored_v_$idx") {
                        detectScaleAwareDragGestures(
                            effectiveScaleProvider = {
                                val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                                liveCanvasZoom * liveLayer.scale * liveLayer.stretchY * ly
                            },
                            onDragStart = onBeginTransaction,
                            onDragEnd = onEndTransaction
                        ) { change, dragAmount ->
                            change.consume()
                            val curLocks = liveLocks
                            val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                            val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                            val screenLocalDx = dragAmount.x * (liveLayer.scale * liveLayer.stretchX * lx)
                            val screenLocalDy = dragAmount.y * (liveLayer.scale * liveLayer.stretchY * ly)
                            if (!curLocks.stretchLocked) {
                                onAnchoredEdgeStretch(0f, signY, 0f, screenLocalDy, rectW, rectH)
                            } else if (!curLocks.zoomLocked) {
                                val radialDelta = screenLocalDx * rx + screenLocalDy * ry
                                val curSpan = (rectH * liveLayer.scale * liveLayer.stretchY).coerceAtLeast(40f)
                                val zoomFactor = (1f + radialDelta / curSpan).coerceIn(0.88f, 1.14f)
                                onUniformZoom(zoomFactor)
                            } else if (!curLocks.positionLocked) {
                                onEdgePan(screenLocalDx / (liveLayer.scale * liveLayer.stretchX), screenLocalDy / (liveLayer.scale * liveLayer.stretchY))
                            }
                        }
                    }
                    .testTag("vertical_stretch_handle_$idx"),
                contentAlignment = Alignment.Center
            ) {
                if (!locks.stretchLocked) {
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(StudioAmber)
                            .border(1.2.dp, Color.White, RoundedCornerShape(999.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, dotColor, CircleShape)
                    )
                }
            }
        }

        // 5. Mapped Left & Right Edge Midpoint Handles
        listOf(
            Triple(leftMidX, leftMidY, -1f),
            Triple(rightMidX, rightMidY, 1f)
        ).forEachIndexed { idx, (mx, my, signX) ->
            val (posX, posY) = outwardOffsetPoint(mx, my, signX, 0f, 5f)
            val (rx, ry) = radialUnit(mx, my, signX, 0f)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (posX - edgeHBoxWpx * 0.5f).roundToInt(),
                            (posY - edgeHBoxHpx * 0.5f).roundToInt()
                        )
                    }
                    .size(width = 20.dp, height = 22.dp)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .pointerInput(liveLayer.id, "anchored_h_$idx") {
                        detectScaleAwareDragGestures(
                            effectiveScaleProvider = {
                                val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                                liveCanvasZoom * liveLayer.scale * liveLayer.stretchX * lx
                            },
                            onDragStart = onBeginTransaction,
                            onDragEnd = onEndTransaction
                        ) { change, dragAmount ->
                            change.consume()
                            val curLocks = liveLocks
                            val lx = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
                            val ly = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
                            val screenLocalDx = dragAmount.x * (liveLayer.scale * liveLayer.stretchX * lx)
                            val screenLocalDy = dragAmount.y * (liveLayer.scale * liveLayer.stretchY * ly)
                            if (!curLocks.stretchLocked) {
                                onAnchoredEdgeStretch(signX, 0f, screenLocalDx, 0f, rectW, rectH)
                            } else if (!curLocks.zoomLocked) {
                                val radialDelta = screenLocalDx * rx + screenLocalDy * ry
                                val curSpan = (rectW * liveLayer.scale * liveLayer.stretchX).coerceAtLeast(40f)
                                val zoomFactor = (1f + radialDelta / curSpan).coerceIn(0.88f, 1.14f)
                                onUniformZoom(zoomFactor)
                            } else if (!curLocks.positionLocked) {
                                onEdgePan(screenLocalDx / (liveLayer.scale * liveLayer.stretchX), screenLocalDy / (liveLayer.scale * liveLayer.stretchY))
                            }
                        }
                    }
                    .testTag("horizontal_stretch_handle_$idx"),
                contentAlignment = Alignment.Center
            ) {
                if (!locks.stretchLocked) {
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 14.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(StudioAmber)
                            .border(1.2.dp, Color.White, RoundedCornerShape(999.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, dotColor, CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Scale-aware drag gesture detector:
 * Divides Compose's viewConfiguration.touchSlop by effective zoom scale so handles & zoomed layers
 * respond immediately to 1-2px screen movements even at 50x-100x magnification.
 */
private suspend fun PointerInputScope.detectScaleAwareDragGestures(
    effectiveScaleProvider: () -> Float,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var overSlop = Offset.Zero
        var pastTouchSlop = false
        val activeId = down.id
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == activeId } ?: event.changes.firstOrNull()
                if (change == null || !change.pressed) break
                val delta = change.position - change.previousPosition
                if (!pastTouchSlop) {
                    overSlop += delta
                    val scale = effectiveScaleProvider().coerceAtLeast(1f)
                    val slop = (viewConfiguration.touchSlop / scale).coerceIn(0.25f, viewConfiguration.touchSlop)
                    if (overSlop.getDistance() >= slop) {
                        pastTouchSlop = true
                        onDragStart()
                        change.consume()
                        onDrag(change, overSlop)
                    }
                } else if (delta != Offset.Zero) {
                    change.consume()
                    onDrag(change, delta)
                }
            }
        } finally {
            if (pastTouchSlop) onDragEnd()
        }
    }
}

/**
 * Scale-aware transform gesture detector (pan, pinch-zoom, 2-finger rotate):
 * Compensates touchSlop by effective visual scale so layers zoomed up to 100x never stall on touchSlop.
 */
private suspend fun PointerInputScope.detectScaleAwareTransformGestures(
    effectiveScaleProvider: () -> Float,
    onGestureStart: () -> Unit = {},
    onGestureEnd: () -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        awaitFirstDown(requireUnconsumed = false)
        try {
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()
                    val panChange = event.calculatePan()

                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        rotation += rotationChange
                        pan += panChange

                        val centroidSize = event.calculateCentroid(useCurrent = false).let { c ->
                            if (c == Offset.Unspecified) 0f
                            else event.changes.filter { it.pressed }.maxOfOrNull { (it.position - c).getDistance() } ?: 0f
                        }
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                        val panMotion = pan.getDistance()
                        val scale = effectiveScaleProvider().coerceAtLeast(1f)
                        val touchSlop = (viewConfiguration.touchSlop / scale).coerceIn(0.25f, viewConfiguration.touchSlop)

                        if (zoomMotion > touchSlop || rotationMotion > touchSlop || panMotion > touchSlop) {
                            pastTouchSlop = true
                            onGestureStart()
                        }
                    }

                    if (pastTouchSlop) {
                        val centroid = event.calculateCentroid(useCurrent = true)
                        if (rotationChange != 0f || zoomChange != 1f || panChange != Offset.Zero) {
                            onGesture(centroid, panChange, zoomChange, rotationChange)
                        }
                        event.changes.forEach {
                            if (it.positionChanged()) it.consume()
                        }
                    }
                }
            } while (!canceled && event.changes.any { it.pressed })
        } finally {
            if (pastTouchSlop) onGestureEnd()
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
