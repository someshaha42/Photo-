package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MergeType
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.ProjectEntity
import com.example.data.VectorShapeEngine
import com.example.fonts.FontClassification
import com.example.fonts.FontMetadata
import com.example.fonts.FontRenderEngine
import com.example.model.DrawLayer
import com.example.model.EditorLayer
import com.example.model.EmojiSourceMode
import com.example.model.GroupLayer
import com.example.model.ImageAdjustments
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal val LAYER_COLORS = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFF4D4F, 0xFFFFB020,
    0xFF22C55E, 0xFF2D8CFF, 0xFF00D4B4, 0xFF9333EA
)

/**
 * Pure, distraction-free outer bottom Horizontal Layer Strip:
 * - No hold/long-press options or multi-select.
 * - Gentle, animated 1-step-at-a-time horizontal swipe reordering so layers glide smoothly without jumping 10 slots.
 */
@Composable
fun HorizontalLayerStrip(
    layers: List<EditorLayer>,
    selectedLayerId: String?,
    imageBitmaps: Map<String, Bitmap>,
    onSelectLayer: (String) -> Unit,
    onMoveLayerOrder: (layerId: String, direction: Int) -> Unit
) {
    val displayLayers = remember(layers) { layers.reversed() }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .testTag("horizontal_layer_strip")
    ) {
        if (layers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Layers • Tap Photo / Text / Shapes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(displayLayers, key = { it.id }) { layer ->
                    val isSelected = layer.id == selectedLayerId
                    val dragThresholdPx = with(LocalDensity.current) { 74.dp.toPx() }
                    var dragAccumulator by remember(layer.id) { mutableFloatStateOf(0f) }
                    var lastSwapTimeMs by remember(layer.id) { mutableLongStateOf(0L) }
                    var isDragging by remember(layer.id) { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .animateItem(
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            .zIndex(if (isDragging) 10f else 0f)
                            .graphicsLayer {
                                translationX = (dragAccumulator * 0.55f).coerceIn(-42f, 42f)
                                scaleX = if (isDragging) 1.06f else 1f
                                scaleY = if (isDragging) 1.06f else 1f
                            }
                            .size(width = 62.dp, height = 62.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (layer is GroupLayer) StudioPurple.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    isSelected -> StudioAzure
                                    layer is GroupLayer -> StudioPurple.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .pointerInput(layer.id, dragThresholdPx) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        dragAccumulator = 0f
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        dragAccumulator = 0f
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        dragAccumulator = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        // Smoothly damped accumulator + 420ms step cooldown so
                                        // a swipe moves strictly 1 layer slot at a time cleanly!
                                        dragAccumulator += dragAmount
                                        val now = System.currentTimeMillis()
                                        if (now - lastSwapTimeMs >= 420L) {
                                            if (dragAccumulator <= -dragThresholdPx) {
                                                onMoveLayerOrder(layer.id, +1)
                                                lastSwapTimeMs = now
                                                dragAccumulator = 0f
                                            } else if (dragAccumulator >= dragThresholdPx) {
                                                onMoveLayerOrder(layer.id, -1)
                                                lastSwapTimeMs = now
                                                dragAccumulator = 0f
                                            }
                                        }
                                    }
                                )
                            }
                            .clickable { onSelectLayer(layer.id) }
                            .testTag("layer_thumb_${layer.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        LayerMiniThumbnail(layer = layer, imageBitmaps = imageBitmaps)
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerMiniThumbnail(
    layer: EditorLayer,
    imageBitmaps: Map<String, Bitmap>
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (layer) {
            is GroupLayer -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderCopy,
                        contentDescription = layer.name,
                        tint = StudioPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Grp ${layer.children.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = StudioPurple,
                        maxLines = 1
                    )
                }
            }
            is ImageLayer -> {
                val bmp = imageBitmaps[layer.uriString]
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = layer.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Photo", style = MaterialTheme.typography.labelSmall)
                }
            }
            is TextLayer -> {
                Text(
                    text = layer.text.ifBlank { "T" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(4.dp)
                )
            }
            is ShapeLayer -> {
                Canvas(modifier = Modifier.size(30.dp)) {
                    val fill = Color(layer.fillColorArgb.toInt())
                    val shapePath = VectorShapeEngine.buildShapePath(
                        shapeType = layer.shapeType,
                        params = layer.vectorParams,
                        baseSizePx = size.minDimension * 0.85f,
                        cx = size.width * 0.5f,
                        cy = size.height * 0.5f
                    ).asComposePath()
                    drawPath(shapePath, color = fill)
                }
            }
            is DrawLayer -> {
                Icon(
                    imageVector = Icons.Rounded.Brush,
                    contentDescription = layer.name,
                    tint = Color((layer.strokes.lastOrNull()?.colorArgb ?: 0xFFFF4D4FL).toInt()),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (!layer.visible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(2.dp)
            ) {
                Icon(Icons.Rounded.VisibilityOff, contentDescription = "Hidden", tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
        if (layer.locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(2.dp)
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = StudioAmber, modifier = Modifier.size(10.dp))
            }
        }
    }
}

/**
 * Right-Half Side Layers Drawer opened from the top "Layers" button:
 * - Covers the right ~52% of the canvas area top-to-bottom, vertically scrollable.
 * - Each row shows Hide & Lock controls on its right side.
 * - Holding a row for ~1.8s-2s selects it into Multi-Select mode (tapping others adds to selection).
 * - When 2+ layers are selected, offers a "Group / Merge" button to join them so they move together.
 * - No Align button anywhere.
 */
@Composable
fun LayersSideDrawer(
    layers: List<EditorLayer>,
    selectedLayerId: String?,
    selectionOrder: List<String>,
    isMultiSelectMode: Boolean,
    imageBitmaps: Map<String, Bitmap>,
    onSelectLayer: (String) -> Unit,
    onHoldSelectMulti: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onMoveLayerOrder: (String, Int) -> Unit,
    onGroupSelected: () -> Unit,
    onToggleGroupExpanded: (String) -> Unit,
    onUngroupLayer: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayLayers = remember(layers) { layers.reversed() }

    var drawerDragX by remember { mutableFloatStateOf(0f) }
    val drawerDismissPx = with(LocalDensity.current) { 48.dp.toPx() }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.58f)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { drawerDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        drawerDragX += dragAmount
                        if (drawerDragX > drawerDismissPx) {
                            drawerDragX = 0f
                            onClose()
                        }
                    }
                )
            }
            .testTag("layers_side_drawer")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Layers, contentDescription = null, tint = StudioAzure, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Layers (${layers.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Layers", modifier = Modifier.size(18.dp))
                }
            }

            if (selectionOrder.size >= 2) {
                Button(
                    onClick = onGroupSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPurple, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("drawer_merge_group_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.MergeType, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Merge / Group (${selectionOrder.size})", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            } else {
                Text(
                    text = if (isMultiSelectMode) "Tap layers to select & merge" else "Hold 0.5s to select • Drag ≡ to reorder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            if (layers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Layers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displayLayers, key = { it.id }) { layer ->
                        DrawerLayerRow(
                            layer = layer,
                            selectedLayerId = selectedLayerId,
                            selectionOrder = selectionOrder,
                            imageBitmaps = imageBitmaps,
                            onSelectLayer = onSelectLayer,
                            onHoldSelectMulti = onHoldSelectMulti,
                            onToggleVisibility = onToggleVisibility,
                            onToggleLock = onToggleLock,
                            onDeleteLayer = onDeleteLayer,
                            onMoveUp = { onMoveLayerOrder(layer.id, +1) },
                            onMoveDown = { onMoveLayerOrder(layer.id, -1) },
                            onToggleGroupExpand = if (layer is GroupLayer) {
                                { onToggleGroupExpanded(layer.id) }
                            } else null,
                            onUngroup = if (layer is GroupLayer) {
                                { onUngroupLayer(layer.id) }
                            } else null
                        )

                        if (layer is GroupLayer && layer.isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                layer.children.reversed().forEach { child ->
                                    DrawerLayerRow(
                                        layer = child,
                                        selectedLayerId = selectedLayerId,
                                        selectionOrder = selectionOrder,
                                        imageBitmaps = imageBitmaps,
                                        onSelectLayer = onSelectLayer,
                                        onHoldSelectMulti = onHoldSelectMulti,
                                        onToggleVisibility = onToggleVisibility,
                                        onToggleLock = onToggleLock,
                                        onDeleteLayer = onDeleteLayer,
                                        onMoveUp = { onMoveLayerOrder(child.id, +1) },
                                        onMoveDown = { onMoveLayerOrder(child.id, -1) },
                                        onToggleGroupExpand = null,
                                        onUngroup = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerLayerRow(
    layer: EditorLayer,
    selectedLayerId: String?,
    selectionOrder: List<String>,
    imageBitmaps: Map<String, Bitmap>,
    onSelectLayer: (String) -> Unit,
    onHoldSelectMulti: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleGroupExpand: (() -> Unit)?,
    onUngroup: (() -> Unit)?
) {
    val scope = rememberCoroutineScope()
    val selIndex = selectionOrder.indexOf(layer.id)
    val isSelected = layer.id == selectedLayerId || selIndex >= 0
    var holdProgress by remember(layer.id) { mutableFloatStateOf(0f) }
    val dragStepPx = with(LocalDensity.current) { 38.dp.toPx() }
    var dragAccumY by remember(layer.id) { mutableFloatStateOf(0f) }
    var lastReorderTime by remember(layer.id) { mutableLongStateOf(0L) }
    var isReordering by remember(layer.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isReordering) 5f else 0f)
            .graphicsLayer {
                translationY = (dragAccumY * 0.45f).coerceIn(-24f, 24f)
            }
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    selIndex >= 0 && selectionOrder.size > 1 -> StudioPurple.copy(alpha = 0.16f)
                    isSelected -> StudioAzure.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                }
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = when {
                    selIndex >= 0 && selectionOrder.size > 1 -> StudioPurple
                    isSelected -> StudioAzure
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .pointerInput(layer.id) {
                // Quick tap or 0.5s (500ms) long hold to enter multi-select mode immediately
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var triggeredHold = false
                    val progressJob = scope.launch {
                        val totalMs = 500L
                        val stepMs = 40L
                        var elapsed = 0L
                        while (elapsed < totalMs) {
                            delay(stepMs)
                            elapsed += stepMs
                            holdProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                        }
                        triggeredHold = true
                        holdProgress = 0f
                        onHoldSelectMulti(layer.id)
                    }
                    val up = waitForUpOrCancellation()
                    progressJob.cancel()
                    holdProgress = 0f
                    if (up != null && !triggeredHold) {
                        onSelectLayer(layer.id)
                    }
                }
            }
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("drawer_layer_row_${layer.id}")
    ) {
        if (holdProgress > 0f) {
            LinearProgressIndicator(
                progress = { holdProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .padding(bottom = 2.dp),
                color = StudioPurple
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Vertical Hand-Gesture Drag Grip to reorder layers smoothly without arrows
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 34.dp)
                    .pointerInput(layer.id, dragStepPx) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isReordering = true
                                dragAccumY = 0f
                            },
                            onDragEnd = {
                                isReordering = false
                                dragAccumY = 0f
                            },
                            onDragCancel = {
                                isReordering = false
                                dragAccumY = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumY += dragAmount
                                val now = System.currentTimeMillis()
                                if (now - lastReorderTime >= 320L) {
                                    // Top of drawer list is front (+1), dragging UP lifts layer forward
                                    if (dragAccumY <= -dragStepPx) {
                                        onMoveUp()
                                        lastReorderTime = now
                                        dragAccumY = 0f
                                    } else if (dragAccumY >= dragStepPx) {
                                        onMoveDown()
                                        lastReorderTime = now
                                        dragAccumY = 0f
                                    }
                                }
                            }
                        )
                    }
                    .testTag("drawer_drag_handle_${layer.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragIndicator,
                    contentDescription = "Drag to Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                LayerMiniThumbnail(layer = layer, imageBitmaps = imageBitmaps)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (selIndex >= 0 && selectionOrder.size > 1) "${selIndex + 1}. ${layer.name}" else layer.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (onToggleGroupExpand != null || onUngroup != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onToggleGroupExpand != null) {
                            Icon(
                                imageVector = if ((layer as GroupLayer).isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = "Expand/Collapse",
                                tint = StudioPurple,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable(onClick = onToggleGroupExpand)
                            )
                        }
                        if (onUngroup != null) {
                            Icon(
                                imageVector = Icons.Rounded.LayersClear,
                                contentDescription = "Ungroup",
                                tint = StudioPurple,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onUngroup)
                            )
                        }
                    }
                }
            }

            // Right-side Hide, Lock, and Delete controls inside the Layers drawer
            IconButton(
                onClick = { onToggleVisibility(layer.id) },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("drawer_hide_${layer.id}")
            ) {
                Icon(
                    imageVector = if (layer.visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = "Hide",
                    tint = if (!layer.visible) StudioAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = { onToggleLock(layer.id) },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("drawer_lock_${layer.id}")
            ) {
                Icon(
                    imageVector = if (layer.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = "Lock",
                    tint = if (layer.locked) StudioAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = { onDeleteLayer(layer.id) },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("drawer_delete_${layer.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = StudioCoral,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CropToolPanel(
    selectedImageLayer: ImageLayer?,
    onDone: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedImageLayer == null) {
            Text(
                text = "Select a Photo to Crop",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Drag crop edges on canvas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("crop_done_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Done", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private enum class TextSubOption {
    NONE,
    SHADOW,
    STROKE
}

@Composable
fun TextToolPanel(
    selectedTextLayer: TextLayer?,
    draftText: String,
    draftStyle: TextLayer,
    activeFont: FontMetadata?,
    onTextChangeLive: (String) -> Unit,
    onConfirmText: () -> Unit,
    onCycleTextCase: () -> Unit,
    onOpenFontLibrary: () -> Unit,
    onUpdateTextLayer: ((TextLayer) -> TextLayer) -> Unit,
    onUpdateTextLayerTransient: ((TextLayer) -> TextLayer) -> Unit = onUpdateTextLayer,
    onEndTransaction: () -> Unit = {},
    isCompactImeMode: Boolean = false
) {
    val activeStyle = selectedTextLayer ?: draftStyle
    val currentBoxValue = selectedTextLayer?.text ?: draftText
    var activeSubOption by remember { mutableStateOf(TextSubOption.NONE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Immediate Text Box + ✓ Button right beside it (always stays mounted across IME open/close)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = currentBoxValue,
                onValueChange = onTextChangeLive,
                placeholder = { Text("Enter text...") },
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_layer_editor_input")
            )
            FilledIconButton(
                onClick = onConfirmText,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = StudioEmerald),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("text_confirm_button")
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Confirm Text", tint = Color.White)
            }
        }

        // Collapse formatting controls while typing so canvas remains visible without unmounting the focused OutlinedTextField
        AnimatedVisibility(
            visible = !isCompactImeMode,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Font | Shadow | Stroke | B | Aa | Italic | Pill | New (no Size row here — Size lives inside Font tab)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
            FilledTonalButton(
                onClick = onOpenFontLibrary,
                modifier = Modifier.testTag("text_panel_font_button")
            ) {
                Icon(Icons.Rounded.FontDownload, contentDescription = "Font", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(activeFont?.displayName?.take(10) ?: "Font", maxLines = 1)
            }

            FilterChip(
                selected = activeStyle.shadowEnabled || activeSubOption == TextSubOption.SHADOW,
                onClick = {
                    if (!activeStyle.shadowEnabled) {
                        onUpdateTextLayer { it.copy(shadowEnabled = true) }
                        activeSubOption = TextSubOption.SHADOW
                    } else if (activeSubOption != TextSubOption.SHADOW) {
                        activeSubOption = TextSubOption.SHADOW
                    } else {
                        onUpdateTextLayer { it.copy(shadowEnabled = false) }
                        activeSubOption = TextSubOption.NONE
                    }
                },
                label = { Text("Shadow") },
                modifier = Modifier.testTag("text_shadow_chip")
            )

            FilterChip(
                selected = activeStyle.strokeWidth > 0f || activeSubOption == TextSubOption.STROKE,
                onClick = {
                    if (activeStyle.strokeWidth <= 0f) {
                        onUpdateTextLayer { it.copy(strokeWidth = 4f) }
                        activeSubOption = TextSubOption.STROKE
                    } else if (activeSubOption != TextSubOption.STROKE) {
                        activeSubOption = TextSubOption.STROKE
                    } else {
                        onUpdateTextLayer { it.copy(strokeWidth = 0f) }
                        activeSubOption = TextSubOption.NONE
                    }
                },
                label = { Text("Stroke") },
                modifier = Modifier.testTag("text_stroke_chip")
            )

            FilterChip(
                selected = activeStyle.isBold,
                onClick = { onUpdateTextLayer { it.copy(isBold = !it.isBold) } },
                label = { Icon(Icons.Rounded.FormatBold, contentDescription = "Bold") },
                modifier = Modifier.testTag("text_bold_chip")
            )

            FilledTonalButton(
                onClick = onCycleTextCase,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("text_case_aa_button")
            ) {
                Text("Aa", fontWeight = FontWeight.Bold)
            }

            FilterChip(
                selected = activeStyle.isItalic,
                onClick = { onUpdateTextLayer { it.copy(isItalic = !it.isItalic) } },
                label = { Icon(Icons.Rounded.FormatItalic, contentDescription = "Italic") }
            )

            FilterChip(
                selected = activeStyle.bgPillEnabled,
                onClick = { onUpdateTextLayer { it.copy(bgPillEnabled = !it.bgPillEnabled) } },
                label = { Text("Pill") }
            )
        }

        // Contextual Slider when Shadow or Stroke is active
        when (activeSubOption) {
            TextSubOption.SHADOW -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shadow Radius: ${activeStyle.shadowRadius.roundToInt()}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = {
                            onUpdateTextLayer { it.copy(shadowEnabled = false) }
                            activeSubOption = TextSubOption.NONE
                        }) {
                            Text("Off")
                        }
                    }
                    Slider(
                        value = activeStyle.shadowRadius.coerceIn(1f, 28f),
                        onValueChange = { r -> onUpdateTextLayerTransient { it.copy(shadowEnabled = true, shadowRadius = r) } },
                        onValueChangeFinished = onEndTransaction,
                        valueRange = 1f..28f,
                        modifier = Modifier.testTag("text_shadow_slider")
                    )
                }
            }
            TextSubOption.STROKE -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stroke Width: ${activeStyle.strokeWidth.roundToInt()}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = {
                            onUpdateTextLayer { it.copy(strokeWidth = 0f) }
                            activeSubOption = TextSubOption.NONE
                        }) {
                            Text("Off")
                        }
                    }
                    Slider(
                        value = activeStyle.strokeWidth.coerceIn(0f, 16f),
                        onValueChange = { w -> onUpdateTextLayerTransient { it.copy(strokeWidth = w) } },
                        onValueChangeFinished = onEndTransaction,
                        valueRange = 0f..16f,
                        modifier = Modifier.testTag("text_stroke_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LAYER_COLORS.forEach { argb ->
                            val selected = activeStyle.strokeColorArgb == argb
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb.toInt()))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) StudioAzure else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdateTextLayer { it.copy(strokeColorArgb = argb) } }
                            )
                        }
                    }
                }
            }
            TextSubOption.NONE -> {
                // Quick text color swatches when not tuning Shadow/Stroke slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LAYER_COLORS.forEach { argb ->
                        val selected = activeStyle.colorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) StudioAzure else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onUpdateTextLayer { it.copy(colorArgb = argb) } }
                        )
                    }
                }
            }
        }
            }
        }
    }
}

private enum class AdjustParam(val label: String, val min: Float, val max: Float) {
    EXPOSURE("Exposure", -1f, 1f),
    CONTRAST("Contrast", -1f, 1f),
    SATURATION("Saturation", -1f, 1f),
    OPACITY("Opacity", 0f, 1f),
    HUE("Hue", -180f, 180f),
    VIBRANCE("HSL", -1f, 1f),
    SHARPNESS("Sharpness", 0f, 1f),
    VIGNETTE("Vignette", 0f, 1f),
    BRIGHTNESS("Brightness", -1f, 1f),
    WARMTH("Warmth", -1f, 1f)
}

@Composable
fun AdjustToolPanel(
    selectedImageLayer: ImageLayer?,
    onUpdateAdjustments: ((ImageAdjustments) -> ImageAdjustments) -> Unit,
    onSetImageOpacity: (Float) -> Unit,
    onResetAdjustments: () -> Unit,
    onEndTransaction: () -> Unit = {}
) {
    var activeParam by remember { mutableStateOf(AdjustParam.EXPOSURE) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedImageLayer == null) {
            Text(
                text = "Add or select a Photo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val adj = selectedImageLayer.adjustments
            val currentVal = when (activeParam) {
                AdjustParam.EXPOSURE -> adj.exposure
                AdjustParam.CONTRAST -> adj.contrast
                AdjustParam.SATURATION -> adj.saturation
                AdjustParam.OPACITY -> selectedImageLayer.opacity
                AdjustParam.HUE -> adj.hue
                AdjustParam.VIBRANCE -> adj.vibrance
                AdjustParam.SHARPNESS -> adj.sharpness
                AdjustParam.VIGNETTE -> adj.vignette
                AdjustParam.BRIGHTNESS -> adj.brightness
                AdjustParam.WARMTH -> adj.warmth
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AdjustParam.entries.forEach { param ->
                    FilterChip(
                        selected = activeParam == param,
                        onClick = { activeParam = param },
                        label = { Text(param.label) },
                        modifier = Modifier.testTag("adjust_param_${param.name.lowercase()}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formatted = when (activeParam) {
                    AdjustParam.HUE -> "${currentVal.roundToInt()}°"
                    AdjustParam.OPACITY -> "${(currentVal * 100).roundToInt()}%"
                    else -> "${(currentVal * 100).roundToInt()}"
                }
                Text(
                    text = "${activeParam.label}: $formatted",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = {
                    onSetImageOpacity(1f)
                    onResetAdjustments()
                }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            Slider(
                value = currentVal,
                onValueChange = { v ->
                    if (activeParam == AdjustParam.OPACITY) {
                        onSetImageOpacity(v)
                    } else {
                        onUpdateAdjustments { prev ->
                            when (activeParam) {
                                AdjustParam.EXPOSURE -> prev.copy(exposure = v)
                                AdjustParam.CONTRAST -> prev.copy(contrast = v)
                                AdjustParam.SATURATION -> prev.copy(saturation = v)
                                AdjustParam.OPACITY -> prev
                                AdjustParam.HUE -> prev.copy(hue = v)
                                AdjustParam.VIBRANCE -> prev.copy(vibrance = v)
                                AdjustParam.SHARPNESS -> prev.copy(sharpness = v)
                                AdjustParam.VIGNETTE -> prev.copy(vignette = v)
                                AdjustParam.BRIGHTNESS -> prev.copy(brightness = v)
                                AdjustParam.WARMTH -> prev.copy(warmth = v)
                            }
                        }
                    }
                },
                onValueChangeFinished = onEndTransaction,
                valueRange = activeParam.min..activeParam.max,
                modifier = Modifier.testTag("adjust_slider")
            )
        }
    }
}

@Composable
fun RotateTransformPanel(
    selectedLayer: EditorLayer?,
    onRotateBy: (Float) -> Unit,
    onSetRotation: (Float) -> Unit,
    onSetScale: (Float) -> Unit,
    onSetOpacity: (Float) -> Unit,
    onMoveOrder: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onEndTransaction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (selectedLayer == null) {
            Text(
                text = "Select a layer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = { onRotateBy(-90f) }) {
                    Icon(Icons.AutoMirrored.Rounded.RotateLeft, contentDescription = "-90", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("-90°")
                }
                FilledTonalButton(onClick = { onRotateBy(90f) }) {
                    Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = "+90", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+90°")
                }
                FilledTonalButton(onClick = {
                    onSetRotation(0f)
                    onEndTransaction()
                }) {
                    Text("0°")
                }
                IconButton(onClick = { onMoveOrder(1) }) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Forward")
                }
                IconButton(onClick = { onMoveOrder(-1) }) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Backward")
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Angle", style = MaterialTheme.typography.labelLarge)
                Text("${selectedLayer.rotationDeg.roundToInt()}°", style = MaterialTheme.typography.labelLarge)
            }
            Slider(
                value = selectedLayer.rotationDeg.coerceIn(-180f, 180f),
                onValueChange = onSetRotation,
                onValueChangeFinished = onEndTransaction,
                valueRange = -180f..180f
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Size", style = MaterialTheme.typography.labelLarge)
                Text("${(selectedLayer.scale * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            Slider(
                value = selectedLayer.scale.coerceIn(0.1f, 50f),
                onValueChange = onSetScale,
                onValueChangeFinished = onEndTransaction,
                valueRange = 0.1f..50f
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Opacity", style = MaterialTheme.typography.labelLarge)
                Text("${(selectedLayer.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            Slider(
                value = selectedLayer.opacity.coerceIn(0f, 1f),
                onValueChange = onSetOpacity,
                onValueChangeFinished = onEndTransaction,
                valueRange = 0f..1f
            )
        }
    }
}

@Composable
fun EmojiSettingsPanel(
    emojiSourceMode: EmojiSourceMode,
    appEmojiFontId: String?,
    fontsRecentToOld: List<FontMetadata>,
    renderEngine: FontRenderEngine,
    onSelectMode: (EmojiSourceMode) -> Unit,
    onSelectAppEmojiFont: (String) -> Unit
) {
    val previewColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Emoji",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmojiSourceMode.entries.forEachIndexed { idx, mode ->
                    val selected = emojiSourceMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) StudioAzure.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) StudioAzure else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectMode(mode) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("emoji_mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}. ${mode.label}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) StudioAzure else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (emojiSourceMode == EmojiSourceMode.APPLICATION) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = fontsRecentToOld,
                        key = { it.id },
                        contentType = { "emoji_font" }
                    ) { font ->
                        val isSelected = font.id == appEmojiFontId
                        val sample = if (font.classification == FontClassification.COLOR_EMOJI || font.hasEmojiCoverage) {
                            "😀 🚀 🌈 ✨"
                        } else {
                            "★ ✦ ♥ ♫ 😀"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) StudioAzure.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectAppEmojiFont(font.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("emoji_app_font_${font.fileName}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = font.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                FontSpecimenText(
                                    fontId = font.id,
                                    text = sample,
                                    renderEngine = renderEngine,
                                    color = previewColor,
                                    fontSize = 18.sp,
                                    maxLines = 1
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Active", tint = StudioAzure)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Right-side vertical My Projects Drawer opened from the 3-dot Menu -> "4. My Projects".
 * Ordered oldest-to-newest (1, 2, 3...) so tapping any saved project auto-saves the
 * current project first and switches cleanly in-place without losing any work.
 */
@Composable
fun MyProjectsSideDrawer(
    projects: List<ProjectEntity>,
    currentProjectId: String,
    onSelectProject: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()) }
    var drawerDragX by remember { mutableFloatStateOf(0f) }
    val drawerDismissPx = with(LocalDensity.current) { 48.dp.toPx() }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.64f)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { drawerDragX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        drawerDragX += dragAmount
                        if (drawerDragX > drawerDismissPx) {
                            drawerDragX = 0f
                            onClose()
                        }
                    }
                )
            }
            .testTag("my_projects_side_drawer")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderCopy,
                        contentDescription = null,
                        tint = StudioAzure,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "My Projects (${projects.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp).testTag("close_projects_drawer_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close My Projects", modifier = Modifier.size(18.dp))
                }
            }

            Text(
                text = "Tap any project to save current & switch",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved projects yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(projects, key = { _, p -> p.id }) { idx, project ->
                        val number = idx + 1
                        val isActive = project.id == currentProjectId
                        val thumbBmp = remember(project.thumbnailPath) {
                            project.thumbnailPath?.takeIf { File(it).exists() }?.let { path ->
                                BitmapFactory.decodeFile(path)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) StudioEmerald.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                                )
                                .border(
                                    width = if (isActive) 1.5.dp else 1.dp,
                                    color = if (isActive) StudioEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectProject(project.id) }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .testTag("drawer_project_card_$number"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isActive) StudioEmerald else StudioAzure),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$number",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumbBmp != null) {
                                    Image(
                                        bitmap = thumbBmp.asImageBitmap(),
                                        contentDescription = project.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.FolderCopy,
                                        contentDescription = project.title,
                                        tint = StudioAzure,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${project.aspectLabel} • ${project.layerCount} layers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = dateFormat.format(Date(project.createdAtMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isActive) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = StudioEmerald
                                ) {
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

