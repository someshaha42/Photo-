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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.example.data.LayerEffectsHelper
import com.example.data.LayerTransformMatrix
import com.example.data.MediaStorageManager
import com.example.fonts.FontRenderEngine
import com.example.model.EditorLayer
import com.example.model.GroupLayer
import com.example.model.ImageCropRect
import com.example.model.ImageCropShape
import com.example.model.ImageLayer
import com.example.model.LayerBlendMode
import com.example.model.ProjectDocument
import com.example.model.TextLayer
import com.example.ui.StudioViewModel
import com.example.ui.TransformLockState
import com.example.ui.theme.CheckerDark1
import com.example.ui.theme.CheckerDark2
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioTeal
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
    fontRenderEngine: FontRenderEngine,
    isZoomMode: Boolean,
    isCropMode: Boolean = false,
    isCornerPinMode: Boolean = false,
    isCornerPinAddPointMode: Boolean = false,
    isEditingFillMedia: Boolean = false,
    isPreviewMirror: Boolean = false,
    transformLocks: TransformLockState = TransformLockState(),
    canvasZoom: Float,
    canvasPan: Pair<Float, Float>,
    isEyedropperActive: Boolean,
    draftText: String = "",
    draftTextStyle: TextLayer? = null,
    isDraftTextPreview: Boolean = false,
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
    onTextWidthReflow: (
        layerId: String,
        signX: Float,
        screenLocalDeltaX: Float,
        boxWpx: Float,
        canvasWpx: Float,
        canvasHpx: Float
    ) -> Unit = { _, _, _, _, _, _ -> },
    onCornerPinDrag: (layerId: String, cornerIndex: Int, deltaDxRatio: Float, deltaDyRatio: Float) -> Unit = { _, _, _, _ -> },
    onAddPuppetPin: (layerId: String, anchorXNorm: Float, anchorYNorm: Float) -> Unit = { _, _, _ -> },
    onPuppetPinDrag: (layerId: String, pinId: String, deltaDxRatio: Float, deltaDyRatio: Float) -> Unit = { _, _, _, _ -> },
    onTransformFillMedia: (dxRatio: Float, dyRatio: Float, scaleFactor: Float, rotDeltaDeg: Float) -> Unit = { _, _, _, _ -> },
    onBeginTransformTransaction: () -> Unit = {},
    onEndTransformTransaction: () -> Unit = {},
    onUpdateCropTransient: ((ImageCropRect) -> ImageCropRect) -> Unit = {},
    onCommitCrop: ((ImageCropRect) -> ImageCropRect) -> Unit = {},
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
    val liveEditingFillMedia by rememberUpdatedState(isEditingFillMedia)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .then(
                if (isPreviewMirror) {
                    Modifier
                } else if (isZoomMode) {
                    Modifier.pointerInput(isZoomMode) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            onCanvasViewportTransform(zoom, pan.x, pan.y)
                        }
                    }
                } else if (!isEyedropperActive) {
                    val allowViewportTransform = !isCropMode && !isCornerPinMode
                    Modifier
                        // Viewport-wide 2-finger pinch/rotate/move assist when transform editing is active
                        .pointerInput(isZoomMode, isEyedropperActive, isCropMode, isCornerPinMode) {
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
                                                if (liveEditingFillMedia && !targetLayer.fillStyle.fillMediaUri.isNullOrBlank()) {
                                                    val span = min(safeW, safeH).coerceAtLeast(48f) * targetLayer.scale.coerceAtLeast(0.2f)
                                                    onTransformFillMedia(
                                                        (pan.x / safeCanvasZ) / span,
                                                        (pan.y / safeCanvasZ) / span,
                                                        zoom,
                                                        rot
                                                    )
                                                } else {
                                                    onTransformLayer(
                                                        targetId,
                                                        if (locks.positionLocked) 0f else (pan.x / safeCanvasZ) / safeW,
                                                        if (locks.positionLocked) 0f else (pan.y / safeCanvasZ) / safeH,
                                                        if (locks.zoomLocked) 1f else zoom,
                                                        if (locks.rotationLocked) 0f else rot,
                                                        1f,
                                                        1f
                                                    )
                                                }
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
                        .pointerInput(isZoomMode, isEyedropperActive, isCropMode, isCornerPinMode) {
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
                                        if (pastSlop && allowViewportTransform && !liveEditingFillMedia) {
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
                        isCropMode -> StudioEmerald
                        isCornerPinMode -> StudioAzure
                        isZoomMode -> StudioAzure
                        else -> MaterialTheme.colorScheme.outline
                    },
                    shape = RoundedCornerShape(4.dp)
                )
                .clipToBounds()
                .then(
                    if (!isPreviewMirror && isEyedropperActive) {
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

            // 2. Layers Stack (Photos, Text/Color Emojis, Groups)
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
                        isCornerPinMode = isCornerPinMode,
                        isCornerPinAddPointMode = isCornerPinAddPointMode,
                        isEditingFillMedia = isEditingFillMedia,
                        isPreviewMirror = isPreviewMirror,
                        transformLocks = transformLocks,
                        isEyedropperActive = isEyedropperActive,
                        onSelectLayer = onSelectLayer,
                        onLayerTapped = onLayerTapped,
                        onDeleteLayer = onDeleteLayer,
                        onTransformLayer = onTransformLayer,
                        onAnchoredStretch = onAnchoredStretch,
                        onTextWidthReflow = onTextWidthReflow,
                        onCornerPinDrag = onCornerPinDrag,
                        onAddPuppetPin = onAddPuppetPin,
                        onPuppetPinDrag = onPuppetPinDrag,
                        onTransformFillMedia = onTransformFillMedia,
                        onBeginTransformTransaction = onBeginTransformTransaction,
                        onEndTransformTransaction = onEndTransformTransaction,
                        onUpdateCropTransient = onUpdateCropTransient,
                        onCommitCrop = onCommitCrop
                    )
                }

                // Live Unsaved Draft Text Preview (renders directly over canvas while typing without adding a layer)
                if (isDraftTextPreview && document.selectedLayerId == null && draftText.isNotEmpty() && draftTextStyle != null) {
                    val draftPreviewLayer = remember(draftText, draftTextStyle) {
                        draftTextStyle.copy(id = "__draft_text_preview__", text = draftText)
                    }
                    Box(modifier = Modifier.fillMaxSize().testTag("draft_text_live_preview")) {
                        RenderCanvasLayerNode(
                            layer = draftPreviewLayer,
                            document = document,
                            selectedSet = emptySet(),
                            primarySelectedId = null,
                            parentGroup = null,
                            parentLocked = true,
                            parentScale = 1f,
                            canvasZoom = canvasZoom,
                            canvasWpx = canvasWpx,
                            canvasHpx = canvasHpx,
                            baseUnitPx = baseUnitPx,
                            imageBitmaps = imageBitmaps,
                            fontRenderEngine = fontRenderEngine,
                            isZoomMode = true,
                            isCropMode = false,
                            isCornerPinMode = false,
                            isCornerPinAddPointMode = false,
                            isEditingFillMedia = false,
                            isPreviewMirror = true,
                            transformLocks = transformLocks,
                            isEyedropperActive = false,
                            onSelectLayer = {},
                            onLayerTapped = {},
                            onDeleteLayer = {},
                            onTransformLayer = { _, _, _, _, _, _, _ -> },
                            onAnchoredStretch = { _, _, _, _, _, _, _, _, _ -> },
                            onTextWidthReflow = { _, _, _, _, _, _ -> },
                            onCornerPinDrag = { _, _, _, _ -> },
                            onAddPuppetPin = { _, _, _ -> },
                            onPuppetPinDrag = { _, _, _, _ -> },
                            onTransformFillMedia = { _, _, _, _ -> },
                            onBeginTransformTransaction = {},
                            onEndTransformTransaction = {},
                            onUpdateCropTransient = {},
                            onCommitCrop = {}
                        )
                    }
                }
            }

            if (!isPreviewMirror && isEyedropperActive) {
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
            } else if (!isPreviewMirror && isEditingFillMedia) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = StudioAzure,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                        .testTag("fill_media_edit_indicator")
                ) {
                    Text(
                        text = "Fill Media • Drag / Pinch / Rotate",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
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
    isCornerPinMode: Boolean,
    isCornerPinAddPointMode: Boolean,
    isEditingFillMedia: Boolean,
    isPreviewMirror: Boolean,
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
    onTextWidthReflow: (
        layerId: String,
        signX: Float,
        screenLocalDeltaX: Float,
        boxWpx: Float,
        canvasWpx: Float,
        canvasHpx: Float
    ) -> Unit,
    onCornerPinDrag: (layerId: String, cornerIndex: Int, deltaDxRatio: Float, deltaDyRatio: Float) -> Unit,
    onAddPuppetPin: (layerId: String, anchorXNorm: Float, anchorYNorm: Float) -> Unit,
    onPuppetPinDrag: (layerId: String, pinId: String, deltaDxRatio: Float, deltaDyRatio: Float) -> Unit,
    onTransformFillMedia: (dxRatio: Float, dyRatio: Float, scaleFactor: Float, rotDeltaDeg: Float) -> Unit,
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
    val isPrimarySelected = !isPreviewMirror &&
        (interactiveTargetId == primarySelectedId) &&
        !isEyedropperActive && !isZoomMode
    val isMultiSelected = !isPreviewMirror &&
        (interactiveTargetId in selectedSet) &&
        !isEyedropperActive && !isZoomMode
    val isBeingCornerPinned = !isPreviewMirror && isCornerPinMode && isPrimarySelected && parentGroup == null
    val isBeingCropped = !isPreviewMirror && isCropMode && isPrimarySelected && parentGroup == null
    val isFillMediaEditActive = !isPreviewMirror &&
        isEditingFillMedia &&
        isPrimarySelected &&
        parentGroup == null &&
        !interactiveTarget.fillStyle.fillMediaUri.isNullOrBlank()
    val showDots = (isPrimarySelected || isMultiSelected) &&
        !isBeingCropped &&
        !isBeingCornerPinned &&
        !isFillMediaEditActive
    val dotAccent = if (isPrimarySelected) StudioAzure else StudioEmerald

    val liveTargetLayer by rememberUpdatedState(interactiveTarget)
    val liveChildLayer by rememberUpdatedState(layer)
    val liveLocks by rememberUpdatedState(transformLocks)
    val liveCanvasZoom by rememberUpdatedState(canvasZoom)
    val liveParentScale by rememberUpdatedState(parentScale)

    // Scale-aware gesture handler: touchSlop is inversely scaled by zoom so high-zoom dragging/zooming/rotating responds immediately!
    val bodyGestureModifier = if (!isPreviewMirror && !isZoomMode && !isEyedropperActive && !isBeingCornerPinned) {
        if ((layer is GroupLayer && layer.isExpanded && parentGroup == null) || isBeingCropped) {
            Modifier
        } else if (isFillMediaEditActive) {
            Modifier.pointerInput(interactiveTargetId, effectiveLocked, baseUnitPx) {
                if (effectiveLocked) return@pointerInput
                detectScaleAwareTransformGestures(
                    effectiveScaleProvider = {
                        liveTargetLayer.scale * liveParentScale * liveCanvasZoom
                    },
                    onGestureStart = { onBeginTransformTransaction() },
                    onGestureEnd = { onEndTransformTransaction() }
                ) { _, pan, zoom, rotation ->
                    val span = (baseUnitPx * 0.72f * liveTargetLayer.scale.coerceAtLeast(0.15f)).coerceAtLeast(32f)
                    onTransformFillMedia(pan.x / span, pan.y / span, zoom, rotation)
                }
            }
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

    // When blendMode != NORMAL, apply layer.opacity inside paint alpha so Compose won't force an implicit offscreen layer that breaks BlendMode
    val deferAlphaToPaint = layer.blendMode != LayerBlendMode.NORMAL

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = layer.offsetXNorm * canvasWpx
                translationY = layer.offsetYNorm * canvasHpx
                rotationZ = layer.rotationDeg
                scaleX = layer.scale * layer.stretchX
                scaleY = layer.scale * layer.stretchY
                alpha = if (deferAlphaToPaint) 1f else layer.opacity
            },
        contentAlignment = Alignment.Center
    ) {
        val paintAlphaInt = if (deferAlphaToPaint) {
            (layer.opacity.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        } else 255

        when (layer) {
            is GroupLayer -> {
                val childForwardParent = if (layer.isExpanded) null else layer
                val useOffscreenGroup = !layer.isExpanded && (
                    !layer.cornerPin.isIdentity() ||
                        !layer.transform3D.isIdentity() ||
                        isBeingCornerPinned ||
                        !layer.liveCrop.isFull() ||
                        isBeingCropped ||
                        layer.blendMode != LayerBlendMode.NORMAL ||
                        layer.fillStyle.hasFill() ||
                        layer.effectStyle.hasEffects()
                    )
                val boxPx = baseUnitPx * 0.64f
                val boxDp = with(LocalDensity.current) { boxPx.toDp() }

                val groupOriginX = (canvasWpx - boxPx) * 0.5f
                val groupOriginY = (canvasHpx - boxPx) * 0.5f

                if (useOffscreenGroup) {
                    val padRatio = 2.2f
                    val uniformZoom = (layer.scale * parentScale * canvasZoom).coerceAtLeast(1f)
                    val texScale = uniformZoom.coerceIn(1f, 3f)
                        .coerceAtMost(1024f / (boxPx * padRatio).coerceAtLeast(1f))
                        .coerceAtLeast(0.5f)
                    val fillMediaBmp = layer.fillStyle.fillMediaUri?.let { imageBitmaps[it] }
                    val preparedGroupPair = remember(
                        layer.children,
                        document.emojiSourceMode,
                        document.appEmojiFontId,
                        imageBitmaps,
                        canvasWpx,
                        canvasHpx,
                        baseUnitPx,
                        texScale,
                        density,
                        layer.fillStyle,
                        fillMediaBmp,
                        layer.effectStyle
                    ) {
                        val tw = (boxPx * padRatio * texScale).roundToInt().coerceIn(48, 1200)
                        val th = (boxPx * padRatio * texScale).roundToInt().coerceIn(48, 1200)
                        val rawGroupBmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888).also { bmp ->
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
                        val eff = LayerEffectsHelper.applyFillAndOuterEffects(
                            sourceBmp = rawGroupBmp,
                            fillStyle = layer.fillStyle,
                            fillMediaBmp = fillMediaBmp,
                            effectStyle = layer.effectStyle,
                            applyOuterStrokeAndShadow = true,
                            effectScale = texScale
                        )
                        if (eff.bitmap !== rawGroupBmp) {
                            rawGroupBmp.recycle()
                        }
                        val pxRatioX = padRatio * (eff.bitmap.width.toFloat() / tw.toFloat())
                        val pxRatioY = padRatio * (eff.bitmap.height.toFloat() / th.toFloat())
                        Triple(eff.bitmap, pxRatioX, pxRatioY)
                    }
                    val styledGroupBmp = preparedGroupPair.first
                    val effPadRatioX = preparedGroupPair.second
                    val effPadRatioY = preparedGroupPair.third

                    val blurredGhost = remember(styledGroupBmp, isBeingCornerPinned) {
                        if (isBeingCornerPinned) LayerTransformMatrix.createFastBlurPreview(styledGroupBmp) else null
                    }
                    val meshBounds = remember(boxPx, layer.transform3D, layer.cornerPin) {
                        LayerTransformMatrix.computeMeshBoundingBox(boxPx, boxPx, layer.transform3D, layer.cornerPin)
                    }
                    val hitBounds = remember(meshBounds) {
                        LayerTransformMatrix.LocalBounds(
                            minX = meshBounds.minX - 20f,
                            minY = meshBounds.minY - 20f,
                            maxX = meshBounds.maxX + 20f,
                            maxY = meshBounds.maxY + 20f
                        )
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
                                                -size.width * (effPadRatioX - 1f) * 0.5f,
                                                -size.height * (effPadRatioY - 1f) * 0.5f,
                                                size.width * (effPadRatioX + 1f) * 0.5f,
                                                size.height * (effPadRatioY + 1f) * 0.5f
                                            ),
                                            paint
                                        )
                                        paint.alpha = 255
                                    }
                                    val localBounds = LayerTransformMatrix.computeMeshBoundingBox(
                                        size.width,
                                        size.height,
                                        layer.transform3D,
                                        layer.cornerPin
                                    )
                                    val clipped = if (!isBeingCropped) {
                                        nc.save()
                                        LayerEffectsHelper.clipLiveCropRect(nc, layer.liveCrop, localBounds)
                                    } else false
                                    LayerEffectsHelper.drawBitmapWithEffectsAndWarp(
                                        canvas = nc,
                                        bmp = styledGroupBmp,
                                        boxW = size.width,
                                        boxH = size.height,
                                        transform3D = layer.transform3D,
                                        cornerPin = layer.cornerPin,
                                        blendMode = layer.blendMode,
                                        alphaInt = paintAlphaInt,
                                        padRatioX = effPadRatioX,
                                        padRatioY = effPadRatioY
                                    )
                                    if (clipped) {
                                        nc.restore()
                                    } else if (!isBeingCropped) {
                                        nc.restore()
                                    }
                                }
                            }
                        }

                        if (isBeingCropped) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (groupOriginX + meshBounds.minX).roundToInt(),
                                            (groupOriginY + meshBounds.minY).roundToInt()
                                        )
                                    }
                                    .size(
                                        width = with(LocalDensity.current) { meshBounds.width.toDp() },
                                        height = with(LocalDensity.current) { meshBounds.height.toDp() }
                                    )
                            ) {
                                InteractiveCropOverlay(
                                    crop = layer.liveCrop.normalized(),
                                    onBeginTransaction = onBeginTransformTransaction,
                                    onUpdateTransient = onUpdateCropTransient,
                                    onCommit = onCommitCrop
                                )
                            }
                        } else {
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
                        }

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
                                isAddPointMode = isCornerPinAddPointMode,
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onDragCorner = { cornerIdx, dDxRatio, dDyRatio ->
                                    onCornerPinDrag(layer.id, cornerIdx, dDxRatio, dDyRatio)
                                },
                                onAddPuppetPin = { ax, ay -> onAddPuppetPin(layer.id, ax, ay) },
                                onDragPuppetPin = { pinId, dDxR, dDyR -> onPuppetPinDrag(layer.id, pinId, dDxR, dDyR) }
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
                                    isCornerPinMode = isCornerPinMode,
                                    isCornerPinAddPointMode = isCornerPinAddPointMode,
                                    isEditingFillMedia = isEditingFillMedia,
                                    isPreviewMirror = isPreviewMirror,
                                    transformLocks = transformLocks,
                                    isEyedropperActive = isEyedropperActive,
                                    onSelectLayer = onSelectLayer,
                                    onLayerTapped = onLayerTapped,
                                    onDeleteLayer = onDeleteLayer,
                                    onTransformLayer = onTransformLayer,
                                    onAnchoredStretch = onAnchoredStretch,
                                    onTextWidthReflow = onTextWidthReflow,
                                    onCornerPinDrag = onCornerPinDrag,
                                    onAddPuppetPin = onAddPuppetPin,
                                    onPuppetPinDrag = onPuppetPinDrag,
                                    onTransformFillMedia = onTransformFillMedia,
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
                    val needsOffscreenEffect = !layer.cornerPin.isIdentity() ||
                        !layer.transform3D.isIdentity() ||
                        isBeingCornerPinned ||
                        !layer.liveCrop.isFull() ||
                        isBeingCropped ||
                        layer.blendMode != LayerBlendMode.NORMAL ||
                        layer.fillStyle.hasFill() ||
                        layer.effectStyle.hasEffects()

                    if (needsOffscreenEffect) {
                        val fillMediaBmp = layer.fillStyle.fillMediaUri?.let { imageBitmaps[it] }
                        val effectScale = (900f / maxOf(boxWpx, boxHpx).coerceAtLeast(1f)).coerceIn(0.6f, 2.5f)
                        val preparedPair = remember(
                            bmp,
                            cropNorm,
                            layer.cropShape,
                            layer.adjustments,
                            layer.tintColorArgb,
                            layer.fillStyle,
                            fillMediaBmp,
                            layer.effectStyle,
                            effectScale
                        ) {
                            val rawImg = buildPreparedImageBitmap(
                                bmp = bmp,
                                cropNorm = cropNorm,
                                cropShape = layer.cropShape,
                                adjustments = layer.adjustments,
                                tintColorArgb = layer.tintColorArgb,
                                maxDim = 900
                            )
                            val eff = LayerEffectsHelper.applyFillAndOuterEffects(
                                sourceBmp = rawImg,
                                fillStyle = layer.fillStyle,
                                fillMediaBmp = fillMediaBmp,
                                effectStyle = layer.effectStyle,
                                applyOuterStrokeAndShadow = true,
                                effectScale = effectScale
                            )
                            if (eff.bitmap !== rawImg) {
                                rawImg.recycle()
                            }
                            val prX = eff.bitmap.width.toFloat() / rawImg.width.coerceAtLeast(1).toFloat()
                            val prY = eff.bitmap.height.toFloat() / rawImg.height.coerceAtLeast(1).toFloat()
                            Triple(eff.bitmap, prX, prY)
                        }
                        val styledBmp = preparedPair.first
                        val prX = preparedPair.second
                        val prY = preparedPair.third

                        val blurredGhost = remember(styledBmp, isBeingCornerPinned) {
                            if (isBeingCornerPinned) LayerTransformMatrix.createFastBlurPreview(styledBmp) else null
                        }
                        val meshBounds = remember(boxWpx, boxHpx, layer.transform3D, layer.cornerPin) {
                            LayerTransformMatrix.computeMeshBoundingBox(boxWpx, boxHpx, layer.transform3D, layer.cornerPin)
                        }
                        val hitBounds = remember(meshBounds) {
                            LayerTransformMatrix.LocalBounds(
                                minX = meshBounds.minX - 20f,
                                minY = meshBounds.minY - 20f,
                                maxX = meshBounds.maxX + 20f,
                                maxY = meshBounds.maxY + 20f
                            )
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
                                                RectF(
                                                    -size.width * (prX - 1f) * 0.5f,
                                                    -size.height * (prY - 1f) * 0.5f,
                                                    size.width * (prX + 1f) * 0.5f,
                                                    size.height * (prY + 1f) * 0.5f
                                                ),
                                                paint
                                            )
                                            paint.alpha = 255
                                        }
                                        val localBounds = LayerTransformMatrix.computeMeshBoundingBox(
                                            size.width,
                                            size.height,
                                            layer.transform3D,
                                            layer.cornerPin
                                        )
                                        val sc = if (!isBeingCropped && !layer.liveCrop.isFull()) {
                                            nc.save().also {
                                                LayerEffectsHelper.clipLiveCropRect(nc, layer.liveCrop, localBounds)
                                            }
                                        } else null
                                        LayerEffectsHelper.drawBitmapWithEffectsAndWarp(
                                            canvas = nc,
                                            bmp = styledBmp,
                                            boxW = size.width,
                                            boxH = size.height,
                                            transform3D = layer.transform3D,
                                            cornerPin = layer.cornerPin,
                                            blendMode = layer.blendMode,
                                            alphaInt = paintAlphaInt,
                                            padRatioX = prX,
                                            padRatioY = prY
                                        )
                                        if (sc != null) {
                                            nc.restoreToCount(sc)
                                        }
                                    }
                                }
                            }

                            if (isBeingCropped) {
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (imgOriginX + meshBounds.minX).roundToInt(),
                                                (imgOriginY + meshBounds.minY).roundToInt()
                                            )
                                        }
                                        .size(
                                            width = with(LocalDensity.current) { meshBounds.width.toDp() },
                                            height = with(LocalDensity.current) { meshBounds.height.toDp() }
                                        )
                                ) {
                                    InteractiveCropOverlay(
                                        crop = layer.liveCrop.normalized(),
                                        onBeginTransaction = onBeginTransformTransaction,
                                        onUpdateTransient = onUpdateCropTransient,
                                        onCommit = onCommitCrop
                                    )
                                }
                            } else {
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

                            if (isBeingCornerPinned && !effectiveLocked) {
                                CornerPinHandlesOverlay(
                                    targetLayer = layer,
                                    boxWpx = boxWpx,
                                    boxHpx = boxHpx,
                                    canvasWpx = canvasWpx,
                                    canvasHpx = canvasHpx,
                                    canvasZoom = canvasZoom,
                                    isAddPointMode = isCornerPinAddPointMode,
                                    onBeginTransaction = onBeginTransformTransaction,
                                    onEndTransaction = onEndTransformTransaction,
                                    onDragCorner = { cornerIdx, dDxRatio, dDyRatio ->
                                        onCornerPinDrag(layer.id, cornerIdx, dDxRatio, dDyRatio)
                                    },
                                    onAddPuppetPin = { ax, ay -> onAddPuppetPin(layer.id, ax, ay) },
                                    onDragPuppetPin = { pinId, dDxR, dDyR -> onPuppetPinDrag(layer.id, pinId, dDxR, dDyR) }
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

            is TextLayer -> {
                val effPx = layer.fontSizeSp * density
                val maxBoxW = (canvasWpx * 0.92f).coerceAtLeast(64f)
                val exactWrapPx = layer.wrapWidthNorm?.let { (it * canvasWpx).coerceIn(44f, maxBoxW) }
                val measured = remember(
                    layer.text,
                    layer.fontId,
                    layer.fontSizeSp,
                    layer.isBold,
                    layer.isItalic,
                    layer.letterSpacing,
                    layer.align,
                    layer.wrapWidthNorm,
                    document.emojiSourceMode,
                    document.appEmojiFontId,
                    maxBoxW
                ) {
                    fontRenderEngine.measureTextLayerBounds(
                        layer = layer,
                        effectiveFontSizePx = effPx,
                        maxWidthPx = maxBoxW,
                        emojiSourceMode = document.emojiSourceMode,
                        appEmojiFontId = document.appEmojiFontId,
                        exactWrapWidthPx = exactWrapPx
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
                val useOffscreenText = !layer.cornerPin.isIdentity() ||
                    !layer.transform3D.isIdentity() ||
                    isBeingCornerPinned ||
                    layer.blendMode != LayerBlendMode.NORMAL ||
                    layer.fillStyle.hasFill() ||
                    isBeingCropped

                if (useOffscreenText) {
                    val texScale = uniformZoom.coerceIn(1f, 4f)
                        .coerceAtMost(960f / maxOf(boxWpx, boxHpx).coerceAtLeast(1f))
                        .coerceAtLeast(1f)
                    val fillMediaBmp = layer.fillStyle.fillMediaUri?.let { imageBitmaps[it] }
                    val preparedTextPair = remember(
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
                        layer.wrapWidthNorm,
                        layer.fillStyle,
                        fillMediaBmp,
                        document.emojiSourceMode,
                        document.appEmojiFontId,
                        boxWpx,
                        boxHpx,
                        texScale
                    ) {
                        val tw = (boxWpx * texScale).roundToInt().coerceIn(24, 1200)
                        val th = (boxHpx * texScale).roundToInt().coerceIn(24, 1200)
                        val rawTxtBmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888).also { bmp ->
                            val tc = AndroidCanvas(bmp)
                            tc.translate(tw / 2f, th / 2f)
                            fontRenderEngine.drawTextLayerOnCanvas(
                                canvas = tc,
                                layer = layer.copy(opacity = 1f),
                                effectiveFontSizePx = effPx * texScale,
                                maxWidthPx = ((measured.widthPx + 4f).coerceAtMost(maxBoxW)) * texScale,
                                emojiSourceMode = document.emojiSourceMode,
                                appEmojiFontId = document.appEmojiFontId,
                                exactWrapWidthPx = exactWrapPx?.let { it * texScale }
                            )
                        }
                        val eff = LayerEffectsHelper.applyFillAndOuterEffects(
                            sourceBmp = rawTxtBmp,
                            fillStyle = layer.fillStyle,
                            fillMediaBmp = fillMediaBmp,
                            effectStyle = layer.effectStyle,
                            applyOuterStrokeAndShadow = false,
                            effectScale = texScale
                        )
                        if (eff.bitmap !== rawTxtBmp) {
                            rawTxtBmp.recycle()
                        }
                        val prX = eff.bitmap.width.toFloat() / tw.toFloat()
                        val prY = eff.bitmap.height.toFloat() / th.toFloat()
                        Triple(eff.bitmap, prX, prY)
                    }
                    val styledTextBmp = preparedTextPair.first
                    val prX = preparedTextPair.second
                    val prY = preparedTextPair.third

                    val blurredGhost = remember(styledTextBmp, isBeingCornerPinned) {
                        if (isBeingCornerPinned) LayerTransformMatrix.createFastBlurPreview(styledTextBmp) else null
                    }
                    val meshBounds = remember(boxWpx, boxHpx, layer.transform3D, layer.cornerPin) {
                        LayerTransformMatrix.computeMeshBoundingBox(boxWpx, boxHpx, layer.transform3D, layer.cornerPin)
                    }
                    val hitBounds = remember(meshBounds) {
                        LayerTransformMatrix.LocalBounds(
                            minX = meshBounds.minX - 20f,
                            minY = meshBounds.minY - 20f,
                            maxX = meshBounds.maxX + 20f,
                            maxY = meshBounds.maxY + 20f
                        )
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
                                    val localBounds = LayerTransformMatrix.computeMeshBoundingBox(
                                        size.width,
                                        size.height,
                                        layer.transform3D,
                                        layer.cornerPin
                                    )
                                    val sc = if (!isBeingCropped && !layer.liveCrop.isFull()) {
                                        nc.save().also {
                                            LayerEffectsHelper.clipLiveCropRect(nc, layer.liveCrop, localBounds)
                                        }
                                    } else null
                                    LayerEffectsHelper.drawBitmapWithEffectsAndWarp(
                                        canvas = nc,
                                        bmp = styledTextBmp,
                                        boxW = size.width,
                                        boxH = size.height,
                                        transform3D = layer.transform3D,
                                        cornerPin = layer.cornerPin,
                                        blendMode = layer.blendMode,
                                        alphaInt = paintAlphaInt,
                                        padRatioX = prX,
                                        padRatioY = prY
                                    )
                                    if (sc != null) {
                                        nc.restoreToCount(sc)
                                    }
                                }
                            }
                        }

                        if (isBeingCropped) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (textOriginX + meshBounds.minX).roundToInt(),
                                            (textOriginY + meshBounds.minY).roundToInt()
                                        )
                                    }
                                    .size(
                                        width = with(LocalDensity.current) { meshBounds.width.toDp() },
                                        height = with(LocalDensity.current) { meshBounds.height.toDp() }
                                    )
                            ) {
                                InteractiveCropOverlay(
                                    crop = layer.liveCrop.normalized(),
                                    onBeginTransaction = onBeginTransformTransaction,
                                    onUpdateTransient = onUpdateCropTransient,
                                    onCommit = onCommitCrop
                                )
                            }
                        } else {
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
                                isTextWidthReflow = true,
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
                                },
                                onTextWidthReflowDrag = { sx, sdx, bw ->
                                    if (!effectiveLocked) {
                                        onTextWidthReflow(layer.id, sx, sdx, bw, canvasWpx, canvasHpx)
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
                                isAddPointMode = isCornerPinAddPointMode,
                                onBeginTransaction = onBeginTransformTransaction,
                                onEndTransaction = onEndTransformTransaction,
                                onDragCorner = { cornerIdx, dDxRatio, dDyRatio ->
                                    onCornerPinDrag(layer.id, cornerIdx, dDxRatio, dDyRatio)
                                },
                                onAddPuppetPin = { ax, ay -> onAddPuppetPin(layer.id, ax, ay) },
                                onDragPuppetPin = { pinId, dDxR, dDyR -> onPuppetPinDrag(layer.id, pinId, dDxR, dDyR) }
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
                                    val nc = canvas.nativeCanvas
                                    nc.save()
                                    if (!layer.liveCrop.isFull()) {
                                        LayerEffectsHelper.clipLiveCropRect(
                                            nc,
                                            layer.liveCrop,
                                            LayerTransformMatrix.LocalBounds(0f, 0f, size.width, size.height)
                                        )
                                    }
                                    nc.translate(size.width / 2f, size.height / 2f)
                                    if (renderScale > 1.01f) {
                                        nc.scale(1f / renderScale, 1f / renderScale)
                                    }
                                    fontRenderEngine.drawTextLayerOnCanvas(
                                        canvas = nc,
                                        layer = layer,
                                        effectiveFontSizePx = effPx * renderScale,
                                        maxWidthPx = ((measured.widthPx + 4f).coerceAtMost(maxBoxW)) * renderScale,
                                        emojiSourceMode = document.emojiSourceMode,
                                        appEmojiFontId = document.appEmojiFontId,
                                        exactWrapWidthPx = exactWrapPx?.let { it * renderScale }
                                    )
                                    nc.restore()
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
                                isTextWidthReflow = true,
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
                                },
                                onTextWidthReflowDrag = { sx, sdx, bw ->
                                    if (!effectiveLocked) {
                                        onTextWidthReflow(layer.id, sx, sdx, bw, canvasWpx, canvasHpx)
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
    isAddPointMode: Boolean = false,
    onBeginTransaction: () -> Unit,
    onEndTransaction: () -> Unit,
    onDragCorner: (cornerIndex: Int, deltaDxRatio: Float, deltaDyRatio: Float) -> Unit,
    onAddPuppetPin: (anchorXNorm: Float, anchorYNorm: Float) -> Unit = { _, _ -> },
    onDragPuppetPin: (pinId: String, deltaDxRatio: Float, deltaDyRatio: Float) -> Unit = { _, _, _ -> }
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
    val meshBounds = LayerTransformMatrix.computeMeshBoundingBox(
        boxW = safeW,
        boxH = safeH,
        transform3D = liveLayer.transform3D,
        cornerPin = liveLayer.cornerPin
    )
    val invScaleX = (1f / (liveLayer.scale * liveLayer.stretchX)).coerceIn(0.01f, 4f)
    val invScaleY = (1f / (liveLayer.scale * liveLayer.stretchY)).coerceIn(0.01f, 4f)
    val handleSizeDp = 36.dp
    val handleHalfPx = with(LocalDensity.current) { (handleSizeDp / 2f).toPx() }
    val pinHandleSizeDp = 30.dp
    val pinHalfPx = with(LocalDensity.current) { (pinHandleSizeDp / 2f).toPx() }

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

        // Layer-scoped tap target when Add Point mode is active
        if (isAddPointMode) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (originX + meshBounds.minX).roundToInt(),
                            (originY + meshBounds.minY).roundToInt()
                        )
                    }
                    .size(
                        width = with(LocalDensity.current) { meshBounds.width.coerceAtLeast(32f).toDp() },
                        height = with(LocalDensity.current) { meshBounds.height.coerceAtLeast(32f).toDp() }
                    )
                    .pointerInput(liveLayer.id, safeW, safeH, meshBounds.minX, meshBounds.minY) {
                        detectTapGestures { localInBounds ->
                            val boxLocalX = meshBounds.minX + localInBounds.x
                            val boxLocalY = meshBounds.minY + localInBounds.y
                            val (ax, ay) = LayerTransformMatrix.mapBoxPointToLocalNorm(
                                localX = boxLocalX,
                                localY = boxLocalY,
                                boxW = safeW,
                                boxH = safeH,
                                transform3D = liveLayer.transform3D,
                                cornerPin = liveLayer.cornerPin
                            )
                            onAddPuppetPin(ax, ay)
                        }
                    }
                    .testTag("corner_pin_add_point_surface")
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

        // Interactive Puppet Deformation Pins added via "Add Point"
        liveLayer.cornerPin.puppetPins.forEachIndexed { idx, pin ->
            val (px, py) = LayerTransformMatrix.mapPuppetPinToBoxPx(
                pin = pin,
                boxW = safeW,
                boxH = safeH,
                transform3D = liveLayer.transform3D,
                cornerPin = liveLayer.cornerPin
            )
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (originX + px - pinHalfPx).roundToInt(),
                            (originY + py - pinHalfPx).roundToInt()
                        )
                    }
                    .size(pinHandleSizeDp)
                    .graphicsLayer { scaleX = invScaleX; scaleY = invScaleY }
                    .pointerInput(liveLayer.id, pin.id) {
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
                            onDragPuppetPin(pin.id, realDx / safeW, realDy / safeH)
                        }
                    }
                    .testTag("puppet_pin_$idx"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(StudioTeal)
                        .border(2.dp, Color.White, CircleShape)
                )
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
    val alphaInt = (effectiveOpacity * 255f).roundToInt().coerceIn(0, 255)
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
                val fillMediaBmp = layer.fillStyle.fillMediaUri?.let { imageBitmaps[it] }
                val eff = LayerEffectsHelper.applyFillAndOuterEffects(
                    sourceBmp = prepared,
                    fillStyle = layer.fillStyle,
                    fillMediaBmp = fillMediaBmp,
                    effectStyle = layer.effectStyle,
                    applyOuterStrokeAndShadow = true,
                    effectScale = 1f
                )
                val prX = eff.bitmap.width.toFloat() / prepared.width.coerceAtLeast(1).toFloat()
                val prY = eff.bitmap.height.toFloat() / prepared.height.coerceAtLeast(1).toFloat()

                canvas.save()
                canvas.translate(-drawW / 2f, -drawH / 2f)
                if (!layer.liveCrop.isFull()) {
                    LayerEffectsHelper.clipLiveCropRect(
                        canvas,
                        layer.liveCrop,
                        LayerTransformMatrix.computeMeshBoundingBox(drawW, drawH, layer.transform3D, layer.cornerPin)
                    )
                }
                LayerEffectsHelper.drawBitmapWithEffectsAndWarp(
                    canvas = canvas,
                    bmp = eff.bitmap,
                    boxW = drawW,
                    boxH = drawH,
                    transform3D = layer.transform3D,
                    cornerPin = layer.cornerPin,
                    blendMode = layer.blendMode,
                    alphaInt = alphaInt,
                    padRatioX = prX,
                    padRatioY = prY
                )
                canvas.restore()
                if (eff.bitmap !== prepared) eff.bitmap.recycle()
                prepared.recycle()
            }
        }
        is TextLayer -> {
            val effPx = layer.fontSizeSp * density
            val maxBoxW = (canvasWpx * 0.92f).coerceAtLeast(64f)
            val exactWrapPx = layer.wrapWidthNorm?.let { (it * canvasWpx).coerceIn(44f, maxBoxW) }
            val measured = fontRenderEngine.measureTextLayerBounds(
                layer = layer.copy(opacity = effectiveOpacity),
                effectiveFontSizePx = effPx,
                maxWidthPx = maxBoxW,
                emojiSourceMode = document.emojiSourceMode,
                appEmojiFontId = document.appEmojiFontId,
                exactWrapWidthPx = exactWrapPx
            )
            val useOffscreenChildText = !layer.cornerPin.isIdentity() ||
                !layer.transform3D.isIdentity() ||
                layer.blendMode != LayerBlendMode.NORMAL ||
                layer.fillStyle.hasFill() ||
                !layer.liveCrop.isFull()

            if (!useOffscreenChildText) {
                fontRenderEngine.drawTextLayerOnCanvas(
                    canvas = canvas,
                    layer = layer.copy(opacity = effectiveOpacity),
                    effectiveFontSizePx = effPx,
                    maxWidthPx = (measured.widthPx + 4f).coerceAtMost(maxBoxW),
                    emojiSourceMode = document.emojiSourceMode,
                    appEmojiFontId = document.appEmojiFontId,
                    exactWrapWidthPx = exactWrapPx
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
                    layer = layer.copy(opacity = 1f),
                    effectiveFontSizePx = effPx,
                    maxWidthPx = (measured.widthPx + 4f).coerceAtMost(maxBoxW),
                    emojiSourceMode = document.emojiSourceMode,
                    appEmojiFontId = document.appEmojiFontId,
                    exactWrapWidthPx = exactWrapPx
                )
                val fillMediaBmp = layer.fillStyle.fillMediaUri?.let { imageBitmaps[it] }
                val eff = LayerEffectsHelper.applyFillAndOuterEffects(
                    sourceBmp = tb,
                    fillStyle = layer.fillStyle,
                    fillMediaBmp = fillMediaBmp,
                    effectStyle = layer.effectStyle,
                    applyOuterStrokeAndShadow = false,
                    effectScale = 1f
                )
                val prX = eff.bitmap.width.toFloat() / tw.toFloat()
                val prY = eff.bitmap.height.toFloat() / th.toFloat()
                canvas.save()
                canvas.translate(-bw / 2f, -bh / 2f)
                if (!layer.liveCrop.isFull()) {
                    LayerEffectsHelper.clipLiveCropRect(
                        canvas,
                        layer.liveCrop,
                        LayerTransformMatrix.computeMeshBoundingBox(bw, bh, layer.transform3D, layer.cornerPin)
                    )
                }
                LayerEffectsHelper.drawBitmapWithEffectsAndWarp(
                    canvas = canvas,
                    bmp = eff.bitmap,
                    boxW = bw,
                    boxH = bh,
                    transform3D = layer.transform3D,
                    cornerPin = layer.cornerPin,
                    blendMode = layer.blendMode,
                    alphaInt = alphaInt,
                    padRatioX = prX,
                    padRatioY = prY
                )
                canvas.restore()
                if (eff.bitmap !== tb) eff.bitmap.recycle()
                tb.recycle()
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

/**
 * Warped & Deforming Selection Handle Layout:
 * - Follows any Layer3DTransform and CornerPinOffsets geometry tightly.
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
    isTextWidthReflow: Boolean = false,
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
    ) -> Unit,
    onTextWidthReflowDrag: (
        signX: Float,
        screenLocalDeltaX: Float,
        boxWpx: Float
    ) -> Unit = { _, _, _ -> }
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
        // For TextLayer, Left & Right handles ALWAYS adjust line-wrapping width (wrapWidthNorm) without altering stretchX.
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
                    .pointerInput(liveLayer.id, "anchored_h_$idx", isTextWidthReflow) {
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
                            if (isTextWidthReflow) {
                                onTextWidthReflowDrag(signX, screenLocalDx, rectW)
                            } else if (!curLocks.stretchLocked) {
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
                if (isTextWidthReflow) {
                    Box(
                        modifier = Modifier
                            .size(width = 6.dp, height = 14.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(StudioAzure)
                            .border(1.2.dp, Color.White, RoundedCornerShape(999.dp))
                    )
                } else if (!locks.stretchLocked) {
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
