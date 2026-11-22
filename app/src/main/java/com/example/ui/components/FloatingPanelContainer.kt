package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fonts.FontRenderEngine
import com.example.model.ProjectDocument
import com.example.model.TextLayer
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import kotlin.math.roundToInt

/**
 * Universal freely-movable (unrestricted X & Y across screen) and 4-corner-resizable floating panel.
 *
 * Notice the outer wrapper intentionally carries NO tap/pointer interceptor so taps outside the
 * floating Surface pass straight through to the background / canvas.
 */
@Composable
fun FloatingDraggableResizablePanel(
    panelTag: String,
    dragHandleTag: String,
    resizeTagPrefix: String,
    defaultWidthDp: Dp? = null,
    defaultWidthFraction: Float = 0.94f,
    defaultHeightDp: Dp = 220.dp,
    minWidthDp: Dp = 140.dp,
    minHeightDp: Dp = 96.dp,
    defaultAlignment: Alignment = Alignment.BottomCenter,
    topClearanceDp: Dp = 8.dp,
    bottomClearanceDp: Dp = 6.dp,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
    cornerAccentColor: Color = StudioAzure,
    showTopDragHandleStrip: Boolean = true,
    dragStripColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
    onDismissSwipeDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    customTopStrip: (@Composable BoxScope.(dragModifier: Modifier) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availWpx = constraints.maxWidth.toFloat().coerceAtLeast(with(density) { minWidthDp.toPx() })
        val availHpx = constraints.maxHeight.toFloat().coerceAtLeast(with(density) { minHeightDp.toPx() })

        val minWpx = minOf(with(density) { minWidthDp.toPx() }, availWpx)
        val minHpx = minOf(with(density) { minHeightDp.toPx() }, availHpx)
        val maxWpx = availWpx.coerceAtLeast(minWpx)
        val maxHpx = availHpx.coerceAtLeast(minHpx)

        val initWpx = with(density) {
            val rawW = defaultWidthDp?.toPx() ?: (availWpx * defaultWidthFraction)
            rawW.coerceIn(minWpx, (availWpx - 12.dp.toPx()).coerceAtLeast(minWpx))
        }
        val topClearPx = with(density) { topClearanceDp.toPx() }
        val bottomClearPx = with(density) { bottomClearanceDp.toPx() }
        val initHpx = with(density) {
            defaultHeightDp.toPx().coerceIn(
                minHpx,
                (availHpx - bottomClearPx - 8.dp.toPx()).coerceAtLeast(minHpx)
            )
        }

        val initX = when (defaultAlignment) {
            Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> with(density) { 8.dp.toPx() }
            Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> (availWpx - initWpx - with(density) { 8.dp.toPx() })
            else -> (availWpx - initWpx) * 0.5f
        }
        val initY = when (defaultAlignment) {
            Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd -> topClearPx
            Alignment.CenterStart, Alignment.Center, Alignment.CenterEnd -> (availHpx - initHpx) * 0.5f
            else -> (availHpx - initHpx - bottomClearPx)
        }

        var initialized by remember { mutableStateOf(false) }
        var panelX by remember { mutableFloatStateOf(initX) }
        var panelY by remember { mutableFloatStateOf(initY) }
        var panelWpx by remember { mutableFloatStateOf(initWpx) }
        var panelHpx by remember { mutableFloatStateOf(initHpx) }

        if (!initialized && availWpx > 0f && availHpx > 0f) {
            panelX = initX
            panelY = initY
            panelWpx = initWpx
            panelHpx = initHpx
            initialized = true
        }

        // Adapt cleanly if viewport shrinks dynamically (e.g. IME opens)
        val effectiveWpx = panelWpx.coerceIn(minWpx, maxWpx)
        val effectiveHpx = panelHpx.coerceIn(minHpx, maxHpx)
        val minKeepVisiblePx = with(density) { 48.dp.toPx() }
        val clampedX = panelX.coerceIn(-effectiveWpx + minKeepVisiblePx, availWpx - minKeepVisiblePx)
        val clampedY = panelY.coerceIn(-effectiveHpx + minKeepVisiblePx, availHpx - minKeepVisiblePx)

        val panelWdp = with(density) { effectiveWpx.toDp() }
        val panelHdp = with(density) { effectiveHpx.toDp() }

        val shape = RoundedCornerShape(16.dp)
        val dragModifier = Modifier.pointerInput(Unit) {
            var swipeAccumY = 0f
            detectDragGestures(
                onDragStart = { swipeAccumY = 0f }
            ) { change, dragAmount ->
                change.consume()
                panelX = (panelX + dragAmount.x).coerceIn(-effectiveWpx + minKeepVisiblePx, availWpx - minKeepVisiblePx)
                panelY = (panelY + dragAmount.y).coerceIn(-effectiveHpx + minKeepVisiblePx, availHpx - minKeepVisiblePx)
                if (onDismissSwipeDown != null) {
                    swipeAccumY += dragAmount.y
                    if (clampedY >= availHpx - effectiveHpx - 4f && swipeAccumY > with(density) { 64.dp.toPx() }) {
                        swipeAccumY = 0f
                        onDismissSwipeDown()
                    }
                }
            }
        }

        Surface(
            color = surfaceColor,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            shape = shape,
            modifier = Modifier
                .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                .size(width = panelWdp, height = panelHdp)
                .border(1.dp, borderColor, shape)
                .pointerInput(Unit) { detectTapGestures { } }
                .testTag(panelTag)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (customTopStrip != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(dragModifier)
                                .testTag(dragHandleTag)
                        ) {
                            customTopStrip(dragModifier)
                        }
                    } else if (showTopDragHandleStrip) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .then(dragModifier)
                                .testTag(dragHandleTag),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 42.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(dragStripColor)
                            )
                        }
                        HorizontalDivider(color = borderColor.copy(alpha = 0.55f))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        content()
                    }
                }

                // 4 Corner Crop-Style Resize Handles (TL, TR, BL, BR)
                CornerPanelResizeHandle(
                    corner = PanelCorner.TL,
                    accentColor = cornerAccentColor,
                    tag = "${resizeTagPrefix}_tl",
                    modifier = Modifier.align(Alignment.TopStart),
                    onDragResize = { dx, dy ->
                        val newW = (panelWpx - dx).coerceIn(minWpx, maxWpx)
                        val actualDx = panelWpx - newW
                        panelX = (panelX + actualDx).coerceIn(-newW + minKeepVisiblePx, availWpx - minKeepVisiblePx)
                        panelWpx = newW

                        val newH = (panelHpx - dy).coerceIn(minHpx, maxHpx)
                        val actualDy = panelHpx - newH
                        panelY = (panelY + actualDy).coerceIn(-newH + minKeepVisiblePx, availHpx - minKeepVisiblePx)
                        panelHpx = newH
                    }
                )
                CornerPanelResizeHandle(
                    corner = PanelCorner.TR,
                    accentColor = cornerAccentColor,
                    tag = "${resizeTagPrefix}_tr",
                    modifier = Modifier.align(Alignment.TopEnd),
                    onDragResize = { dx, dy ->
                        panelWpx = (panelWpx + dx).coerceIn(minWpx, maxWpx)

                        val newH = (panelHpx - dy).coerceIn(minHpx, maxHpx)
                        val actualDy = panelHpx - newH
                        panelY = (panelY + actualDy).coerceIn(-newH + minKeepVisiblePx, availHpx - minKeepVisiblePx)
                        panelHpx = newH
                    }
                )
                CornerPanelResizeHandle(
                    corner = PanelCorner.BL,
                    accentColor = cornerAccentColor,
                    tag = "${resizeTagPrefix}_bl",
                    modifier = Modifier.align(Alignment.BottomStart),
                    onDragResize = { dx, dy ->
                        val newW = (panelWpx - dx).coerceIn(minWpx, maxWpx)
                        val actualDx = panelWpx - newW
                        panelX = (panelX + actualDx).coerceIn(-newW + minKeepVisiblePx, availWpx - minKeepVisiblePx)
                        panelWpx = newW

                        panelHpx = (panelHpx + dy).coerceIn(minHpx, maxHpx)
                    }
                )
                CornerPanelResizeHandle(
                    corner = PanelCorner.BR,
                    accentColor = cornerAccentColor,
                    tag = "${resizeTagPrefix}_br",
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onDragResize = { dx, dy ->
                        panelWpx = (panelWpx + dx).coerceIn(minWpx, maxWpx)
                        panelHpx = (panelHpx + dy).coerceIn(minHpx, maxHpx)
                    }
                )
            }
        }
    }
}

private enum class PanelCorner { TL, TR, BL, BR }

@Composable
private fun CornerPanelResizeHandle(
    corner: PanelCorner,
    accentColor: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onDragResize: (dx: Float, dy: Float) -> Unit
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .pointerInput(tag) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragResize(dragAmount.x, dragAmount.y)
                }
            }
            .testTag(tag)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 4.dp.toPx()
            val arm = 8.dp.toPx()
            val strokeW = 2.4.dp.toPx()
            val path = Path().apply {
                when (corner) {
                    PanelCorner.TL -> {
                        moveTo(inset, inset + arm)
                        lineTo(inset, inset)
                        lineTo(inset + arm, inset)
                    }
                    PanelCorner.TR -> {
                        moveTo(size.width - inset - arm, inset)
                        lineTo(size.width - inset, inset)
                        lineTo(size.width - inset, inset + arm)
                    }
                    PanelCorner.BL -> {
                        moveTo(inset, size.height - inset - arm)
                        lineTo(inset, size.height - inset)
                        lineTo(inset + arm, size.height - inset)
                    }
                    PanelCorner.BR -> {
                        moveTo(size.width - inset - arm, size.height - inset)
                        lineTo(size.width - inset, size.height - inset)
                        lineTo(size.width - inset, size.height - inset - arm)
                    }
                }
            }
            drawPath(
                path = path,
                color = accentColor.copy(alpha = 0.85f),
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Floating live-sync Preview Window enabled from the 3-Dot Menu -> Preview toggle.
 * Freely movable in X/Y and 4-corner resizable; renders live non-interactive canvas preview
 * whenever an editor option panel is open.
 */
@Composable
fun FloatingPreviewWindow(
    document: ProjectDocument,
    imageBitmaps: Map<String, Bitmap>,
    fontRenderEngine: FontRenderEngine,
    canvasZoom: Float,
    canvasPan: Pair<Float, Float>,
    draftText: String,
    draftTextStyle: TextLayer,
    isDraftTextPreview: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingDraggableResizablePanel(
        panelTag = "floating_preview_window",
        dragHandleTag = "preview_window_drag_handle",
        resizeTagPrefix = "preview_window_resize",
        defaultWidthDp = 166.dp,
        defaultHeightDp = 166.dp,
        minWidthDp = 112.dp,
        minHeightDp = 112.dp,
        defaultAlignment = Alignment.TopCenter,
        topClearanceDp = 8.dp,
        surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        borderColor = StudioAzure.copy(alpha = 0.65f),
        cornerAccentColor = StudioAzure,
        showTopDragHandleStrip = false,
        modifier = modifier,
        customTopStrip = { _ ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = StudioAzure
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            EditorCanvasViewport(
                document = document,
                imageBitmaps = imageBitmaps,
                fontRenderEngine = fontRenderEngine,
                isZoomMode = false,
                isCropMode = false,
                isCornerPinMode = false,
                isCornerPinAddPointMode = false,
                isEditingFillMedia = false,
                isPreviewMirror = true,
                canvasZoom = canvasZoom,
                canvasPan = canvasPan,
                isEyedropperActive = false,
                draftText = draftText,
                draftTextStyle = draftTextStyle,
                isDraftTextPreview = isDraftTextPreview,
                onCanvasViewportTransform = { _, _, _ -> },
                onSelectLayer = {},
                onTransformLayer = { _, _, _, _, _, _, _ -> },
                onEyedropperTap = { _, _ -> }
            )

            // Bottom-Right × Close Button (offset slightly inward from the bottom-right corner resize bracket)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(StudioCoral.copy(alpha = 0.92f))
                    .border(1.5.dp, Color.White, CircleShape)
                    .clickable(onClick = onClose)
                    .testTag("preview_window_close_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Preview Window",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
