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
                } else if (!isEyedropperActive && !isDrawMode && !isCropMode && !isCornerPinMode) {
                    Modifier
                        // Viewport-wide 2-finger pinch/rotate/move assist so tiny or heavily zoomed Text/Photos transform effortlessly!
                        .pointerInput(isZoomMode, isEyedropperActive, isDrawMode, isCropMode, isCornerPinMode) {
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
                        // Unified Main-pass: 1-finger remote pan anywhere on empty canvas when a layer is selected, or quick-tap to deselect
                        .pointerInput(isZoomMode, isEyedropperActive, isDrawMode, isCropMode, isCornerPinMode) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                                var totalPan = Offset.Zero
                                var pastSlop = false
                                var draggingRemote = false
                                var childConsumedOrMulti = false
                                var pointerReleasedCleanly = false
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
                                                pointerReleasedCleanly = true
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
                                        if (pastSlop) {
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
                                    if (!childConsumedOrMulti && !pastSlop && pointerReleasedCleanly) {
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
    val isBeingCornerPinned = isCornerPinMode && isPrimarySelected && parentGroup == null &&
        (layer is ImageLayer || layer is TextLayer)
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

                    if (!layer.isExpanded && showDots && !effectiveLocked) {
                        SelectionDotHandlesOverlay(
                            boxModifier = Modifier.size(with(LocalDensity.current) { (baseUnitPx * 0.64f).toDp() }),
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

                            Box(modifier = fitModifier.then(bodyGestureModifier)) {
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

                                if (showDots && parentGroup == null) {
                                    SelectionDotHandlesOverlay(
                                        boxModifier = Modifier.fillMaxSize(),
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
                        } else {
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

                                if (showDots && parentGroup == null) {
                                    SelectionDotHandlesOverlay(
                                        boxModifier = Modifier.fillMaxSize(),
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
                Box(modifier = Modifier.size(shapeDp).then(bodyGestureModifier)) {
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

                    if (showDots && parentGroup == null && (!isShapeEditPointsMode || !isPrimarySelected)) {
                        SelectionDotHandlesOverlay(
                            boxModifier = Modifier.fillMaxSize(),
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

                    Box(modifier = Modifier.size(width = boxWdp, height = boxHdp).then(bodyGestureModifier)) {
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

                        if (showDots && parentGroup == null) {
                            SelectionDotHandlesOverlay(
                                boxModifier = Modifier.fillMaxSize(),
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
                } else {
                    Box(
                        modifier = Modifier
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

                        if (showDots && parentGroup == null) {
                            SelectionDotHandlesOverlay(
                                boxModifier = Modifier.fillMaxSize(),
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
                // Compute tight stroke bounding box so selection handles & gestures fit ONLY the drawn area (not whole canvas)
                val allPts = remember(layer.strokes) { layer.strokes.flatMap { it.points } }
                val minXNorm = (allPts.minOfOrNull { it.xNorm } ?: 0.25f).let { (it - 0.04f).coerceIn(0f, 0.92f) }
                val minYNorm = (allPts.minOfOrNull { it.yNorm } ?: 0.25f).let { (it - 0.04f).coerceIn(0f, 0.92f) }
                val maxXNorm = (allPts.maxOfOrNull { it.xNorm } ?: 0.75f).let { (it + 0.04f).coerceIn((minXNorm + 0.08f).coerceAtMost(1f), 1f) }
                val maxYNorm = (allPts.maxOfOrNull { it.yNorm } ?: 0.75f).let { (it + 0.04f).coerceIn((minYNorm + 0.08f).coerceAtMost(1f), 1f) }

                val boxLeftDp = with(LocalDensity.current) { (minXNorm * canvasWpx).toDp() }
                val boxTopDp = with(LocalDensity.current) { (minYNorm * canvasHpx).toDp() }
                val boxWidthDp = with(LocalDensity.current) { ((maxXNorm - minXNorm) * canvasWpx).coerceAtLeast(48f).toDp() }
                val boxHeightDp = with(LocalDensity.current) { ((maxYNorm - minYNorm) * canvasHpx).coerceAtLeast(48f).toDp() }

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
                    ) {
                        if (showDots && parentGroup == null) {
                            SelectionDotHandlesOverlay(
                                boxModifier = Modifier.fillMaxSize(),
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
    onBeginTransaction: () -> Unit,
    onEndTransaction: () -> Unit,
    onDragCorner: (cornerIndex: Int, deltaDxRatio: Float, deltaDyRatio: Float) -> Unit
) {
    val liveLayer by rememberUpdatedState(targetLayer)
    val liveCanvasZoom by rememberUpdatedState(canvasZoom)
    val safeW = boxWpx.coerceAtLeast(1f)
    val safeH = boxHpx.coerceAtLeast(1f)
    val originX = (canvasWpx - safeW) / 2f
    val originY = (canvasHpx - safeH) / 2f

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
        // White Alight Motion outline + small center crosshair circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outlinePath = VectorShapeEngine.buildShapePath(
                shapeType = liveLayer.shapeType,
                params = liveLayer.vectorParams,
                baseSizePx = shapePx,
                cx = canvasWpx * 0.5f,
                cy = canvasHpx * 0.5f
            ).asComposePath()
            drawPath(
                path = outlinePath,
                color = Color.White.copy(alpha = 0.9f),
                style = Stroke(width = 1.6.dp.toPx() * min(invScaleX, invScaleY))
            )
            // Small center dot / ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = 4.dp.toPx() * min(invScaleX, invScaleY),
                center = Offset(canvasWpx * 0.5f, canvasHpx * 0.5f),
                style = Stroke(width = 1.2.dp.toPx() * min(invScaleX, invScaleY))
            )
        }

        handles.forEach { handle ->
            val hx = originX + handle.xPx
            val hy = originY + handle.yPx
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
                            onHandleDrag(handle.id, realDx, realDy)
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

/**
 * Selection Handle Layout:
 * - Top-Left: Cross (✕) Delete icon button to immediately remove selected object.
 * - Bottom-Right: Larger diagonal Zoom icon handle for easy finger zooming.
 * - Top-Right & Bottom-Left: Corner zoom dots.
 * - 4 Edge Midpoints: Small localized anchored-stretch dots (touching only the small dot stretches strictly from that side with opposite edge pinned).
 */
@Composable
private fun SelectionDotHandlesOverlay(
    boxModifier: Modifier,
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
    var boxW by remember { mutableFloatStateOf(160f) }
    var boxH by remember { mutableFloatStateOf(160f) }
    val liveLayer by rememberUpdatedState(targetLayer)
    val liveLocks by rememberUpdatedState(locks)
    val liveCanvasZoom by rememberUpdatedState(canvasZoom)

    val invScaleX = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
    val invScaleY = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)

    Box(
        modifier = boxModifier
            .border(1.dp, dotColor.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
            .onSizeChanged { sz ->
                boxW = sz.width.toFloat().coerceAtLeast(48f)
                boxH = sz.height.toFloat().coerceAtLeast(48f)
            }
    ) {
        // 1. Top-Left Cross (✕) Delete Button
        if (showDeleteCross) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-10).dp, y = (-10).dp)
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

        // 2. Top-Right & Bottom-Left Corner Zoom Dots (respects Zoom / Position locks)
        listOf(
            Alignment.TopEnd to (1f to -1f),
            Alignment.BottomStart to (-1f to 1f)
        ).forEachIndexed { idx, (align, signs) ->
            val (sxSign, sySign) = signs
            Box(
                modifier = Modifier
                    .align(align)
                    .offset(x = (sxSign * 6).dp, y = (sySign * 6).dp)
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
                                val radialDelta = (realDeltaX * sxSign + realDeltaY * sySign) * 0.55f
                                val curSpan = min(
                                    boxW * liveLayer.scale * liveLayer.stretchX,
                                    boxH * liveLayer.scale * liveLayer.stretchY
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

        // 3. Bottom-Right Corner: Larger Zoom Handle Icon
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 12.dp, y = 12.dp)
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
                            val radialDelta = (realDeltaX + realDeltaY) * 0.55f
                            val curSpan = min(
                                boxW * liveLayer.scale * liveLayer.stretchX,
                                boxH * liveLayer.scale * liveLayer.stretchY
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

        // 4. Side Midpoint Handles:
        // - When Stretch Lock (S) is OFF (unlocked): Anchored one-side stretch ONLY (opposite side pinned, no zoom/rotate/pan).
        // - When Stretch Lock (S) is ON (default): Loses stretch ability and instead performs Uniform Zoom (if Zoom unlocked) or Position Move (if Zoom locked & Position unlocked).
        listOf(Alignment.TopCenter to -1f, Alignment.BottomCenter to 1f).forEachIndexed { idx, (align, signY) ->
            Box(
                modifier = Modifier
                    .align(align)
                    .offset(y = (signY * 5).dp)
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
                                // Stretch unlocked -> Pure anchored stretch only!
                                onAnchoredEdgeStretch(0f, signY, 0f, screenLocalDy, boxW, boxH)
                            } else if (!curLocks.zoomLocked) {
                                val radialDelta = screenLocalDy * signY
                                val curSpan = (boxH * liveLayer.scale * liveLayer.stretchY).coerceAtLeast(40f)
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

        listOf(Alignment.CenterStart to -1f, Alignment.CenterEnd to 1f).forEachIndexed { idx, (align, signX) ->
            Box(
                modifier = Modifier
                    .align(align)
                    .offset(x = (signX * 5).dp)
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
                                // Stretch unlocked -> Pure anchored stretch only!
                                onAnchoredEdgeStretch(signX, 0f, screenLocalDx, 0f, boxW, boxH)
                            } else if (!curLocks.zoomLocked) {
                                val radialDelta = screenLocalDx * signX
                                val curSpan = (boxW * liveLayer.scale * liveLayer.stretchX).coerceAtLeast(40f)
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
