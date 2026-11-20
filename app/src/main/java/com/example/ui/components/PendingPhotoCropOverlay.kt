package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.CropPortrait
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ImageCropRect
import com.example.model.ImageCropShape
import com.example.ui.PendingImportPhoto
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import kotlin.math.abs

enum class ImportCropMode(val label: String) {
    FREE("Free"),
    CIRCLE("Circle"),
    RATIO_9_16("9:16")
}

@Composable
fun PendingPhotoCropOverlay(
    pendingPhoto: PendingImportPhoto,
    onDelete: () -> Unit,
    onDone: (crop: ImageCropRect, shape: ImageCropShape) -> Unit
) {
    BackHandler(onBack = onDelete)

    var cropMode by remember(pendingPhoto.id) { mutableStateOf(ImportCropMode.FREE) }
    var frame by remember(pendingPhoto.id) { mutableStateOf(ImageCropRect(0f, 0f, 1f, 1f)) }
    var imgScale by remember(pendingPhoto.id) { mutableFloatStateOf(1f) }
    var panXNorm by remember(pendingPhoto.id) { mutableFloatStateOf(0f) }
    var panYNorm by remember(pendingPhoto.id) { mutableFloatStateOf(0f) }
    var boxW by remember { mutableFloatStateOf(300f) }
    var boxH by remember { mutableFloatStateOf(300f) }

    val imgRatio = remember(pendingPhoto.id) {
        pendingPhoto.previewBitmap.width.coerceAtLeast(1).toFloat() /
            pendingPhoto.previewBitmap.height.coerceAtLeast(1).toFloat()
    }
    val imageBitmap = remember(pendingPhoto.id) { pendingPhoto.previewBitmap.asImageBitmap() }

    fun applyModePreset(mode: ImportCropMode) {
        cropMode = mode
        imgScale = 1f
        panXNorm = 0f
        panYNorm = 0f
        frame = when (mode) {
            ImportCropMode.FREE -> ImageCropRect(0f, 0f, 1f, 1f)
            ImportCropMode.CIRCLE -> {
                // Start as centered circle in visual pixel space, freely stretchable into oval
                if (imgRatio >= 1f) {
                    val spanX = (0.78f / imgRatio).coerceIn(0.25f, 0.92f)
                    val marginX = (1f - spanX) * 0.5f
                    ImageCropRect(marginX, 0.11f, 1f - marginX, 0.89f)
                } else {
                    val spanY = (0.78f * imgRatio).coerceIn(0.25f, 0.92f)
                    val marginY = (1f - spanY) * 0.5f
                    ImageCropRect(0.11f, marginY, 0.89f, 1f - marginY)
                }
            }
            ImportCropMode.RATIO_9_16 -> {
                val targetRatio = 9f / 16f
                if (imgRatio > targetRatio) {
                    val w = (targetRatio / imgRatio).coerceIn(0.22f, 1f)
                    val h = 0.86f
                    val ml = (1f - w * h) * 0.5f
                    ImageCropRect(ml, 0.07f, 1f - ml, 0.93f)
                } else {
                    val w = 0.86f
                    val h = ((imgRatio / targetRatio) * w).coerceIn(0.22f, 1f)
                    val mt = (1f - h) * 0.5f
                    ImageCropRect(0.07f, mt, 0.93f, 1f - mt)
                }
            }
        }
    }

    fun computeFinalCropRect(): ImageCropRect {
        val invScale = 1f / imgScale.coerceAtLeast(1f)
        val origLeft = (0.5f + (frame.left - 0.5f - panXNorm) * invScale).coerceIn(0f, 0.85f)
        val origTop = (0.5f + (frame.top - 0.5f - panYNorm) * invScale).coerceIn(0f, 0.85f)
        val origRight = (0.5f + (frame.right - 0.5f - panXNorm) * invScale)
            .coerceIn((origLeft + 0.15f).coerceAtMost(1f), 1f)
        val origBottom = (0.5f + (frame.bottom - 0.5f - panYNorm) * invScale)
            .coerceIn((origTop + 0.15f).coerceAtMost(1f), 1f)
        return ImageCropRect(origLeft, origTop, origRight, origBottom).normalized()
    }

    Surface(
        color = Color(0xFF0F1218),
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxSize()
            .testTag("pending_photo_crop_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar: Delete (left) and Done (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioCoral),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("import_crop_delete_button")
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Crop Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Button(
                    onClick = {
                        val finalShape = if (cropMode == ImportCropMode.CIRCLE) {
                            ImageCropShape.CIRCLE
                        } else {
                            ImageCropShape.RECTANGLE
                        }
                        onDone(computeFinalCropRect(), finalShape)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("import_crop_done_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Done", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }

            // Interactive Crop Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val availRatio = maxWidth / maxHeight
                val containerMod = if (availRatio > imgRatio) {
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(imgRatio)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(imgRatio)
                }

                val liveMode by rememberUpdatedState(cropMode)
                val liveFrame by rememberUpdatedState(frame)

                Box(
                    modifier = containerMod
                        .clip(RoundedCornerShape(4.dp))
                        .clipToBounds()
                        .onSizeChanged { sz ->
                            boxW = sz.width.toFloat().coerceAtLeast(1f)
                            boxH = sz.height.toFloat().coerceAtLeast(1f)
                        }
                        .pointerInput(pendingPhoto.id) {
                            // Supports 2-finger pinch zoom & pan on the photo + 1-finger moving the crop frame
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var movingFrame = false
                                var decided = false
                                do {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.count { it.pressed }
                                    if (pressed >= 2) {
                                        decided = true
                                        movingFrame = false
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        if (zoom != 1f || pan != Offset.Zero) {
                                            val nextScale = (imgScale * zoom).coerceIn(1f, 8f)
                                            val maxPan = ((nextScale - 1f) * 0.5f).coerceAtLeast(0f)
                                            imgScale = nextScale
                                            panXNorm = (panXNorm + pan.x / size.width.coerceAtLeast(1)).coerceIn(-maxPan, maxPan)
                                            panYNorm = (panYNorm + pan.y / size.height.coerceAtLeast(1)).coerceIn(-maxPan, maxPan)
                                            event.changes.forEach { it.consume() }
                                        }
                                    } else if (pressed == 1) {
                                        val pan = event.calculatePan()
                                        if (!decided && pan.getDistance() > 2f) {
                                            decided = true
                                            val p = event.changes.first().position
                                            val nx = p.x / size.width.coerceAtLeast(1)
                                            val ny = p.y / size.height.coerceAtLeast(1)
                                            val f = liveFrame
                                            val isFullFree = (f.right - f.left) >= 0.98f && (f.bottom - f.top) >= 0.98f
                                            movingFrame = !isFullFree && nx in f.left..f.right && ny in f.top..f.bottom
                                        }
                                        if (decided && pan != Offset.Zero) {
                                            val dxN = pan.x / size.width.coerceAtLeast(1)
                                            val dyN = pan.y / size.height.coerceAtLeast(1)
                                            if (movingFrame) {
                                                val f = liveFrame
                                                val fw = f.right - f.left
                                                val fh = f.bottom - f.top
                                                val nl = (f.left + dxN).coerceIn(0f, (1f - fw).coerceAtLeast(0f))
                                                val nt = (f.top + dyN).coerceIn(0f, (1f - fh).coerceAtLeast(0f))
                                                frame = ImageCropRect(nl, nt, nl + fw, nt + fh)
                                            } else if (imgScale > 1.01f) {
                                                val maxPan = ((imgScale - 1f) * 0.5f).coerceAtLeast(0f)
                                                panXNorm = (panXNorm + dxN).coerceIn(-maxPan, maxPan)
                                                panYNorm = (panYNorm + dyN).coerceIn(-maxPan, maxPan)
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = pendingPhoto.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = imgScale
                                scaleY = imgScale
                                translationX = panXNorm * boxW
                                translationY = panYNorm * boxH
                            }
                    )

                    // Crop Dim Mask (Circle oval or Rectangle) + Grid
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    ) {
                        val l = liveFrame.left * size.width
                        val t = liveFrame.top * size.height
                        val r = liveFrame.right * size.width
                        val b = liveFrame.bottom * size.height
                        val cw = (r - l).coerceAtLeast(1f)
                        val ch = (b - t).coerceAtLeast(1f)

                        drawRect(Color.Black.copy(alpha = 0.62f))
                        if (liveMode == ImportCropMode.CIRCLE) {
                            drawOval(
                                color = Color.Transparent,
                                topLeft = Offset(l, t),
                                size = Size(cw, ch),
                                blendMode = BlendMode.Clear
                            )
                            drawOval(
                                color = Color.White,
                                topLeft = Offset(l, t),
                                size = Size(cw, ch),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        } else {
                            drawRect(
                                color = Color.Transparent,
                                topLeft = Offset(l, t),
                                size = Size(cw, ch),
                                blendMode = BlendMode.Clear
                            )
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(l, t),
                                size = Size(cw, ch),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            val gridColor = Color.White.copy(alpha = 0.4f)
                            for (s in 1..2) {
                                val gx = l + cw * (s / 3f)
                                drawLine(gridColor, Offset(gx, t), Offset(gx, b), strokeWidth = 1.dp.toPx())
                                val gy = t + ch * (s / 3f)
                                drawLine(gridColor, Offset(l, gy), Offset(r, gy), strokeWidth = 1.dp.toPx())
                            }
                        }
                    }

                    // Handles
                    val density = LocalDensity.current
                    val lDp = with(density) { (frame.left * boxW).toDp() }
                    val tDp = with(density) { (frame.top * boxH).toDp() }
                    val rDp = with(density) { (frame.right * boxW).toDp() }
                    val bDp = with(density) { (frame.bottom * boxH).toDp() }
                    val cxDp = with(density) { (((frame.left + frame.right) * 0.5f) * boxW).toDp() }
                    val cyDp = with(density) { (((frame.top + frame.bottom) * 0.5f) * boxH).toDp() }

                    fun updateFrameCorner(cornerXSign: Float, cornerYSign: Float, dxPx: Float, dyPx: Float) {
                        val cur = liveFrame
                        val dx = dxPx / boxW
                        val dy = dyPx / boxH
                        if (liveMode == ImportCropMode.RATIO_9_16) {
                            // Preserve visual 9:16 aspect ratio
                            val targetRatio = 9f / 16f
                            val curWpx = (cur.right - cur.left) * boxW
                            val curHpx = (cur.bottom - cur.top) * boxH
                            val deltaWpx = dxPx * cornerXSign
                            val deltaHpx = dyPx * cornerYSign
                            val useDeltaW = if (abs(deltaWpx) >= abs(deltaHpx * targetRatio)) deltaWpx else deltaHpx * targetRatio
                            var newWpx = (curWpx + useDeltaW).coerceAtLeast(boxW * 0.18f)
                            var newHpx = newWpx / targetRatio
                            if (newWpx > boxW) {
                                newWpx = boxW
                                newHpx = newWpx / targetRatio
                            }
                            if (newHpx > boxH) {
                                newHpx = boxH
                                newWpx = newHpx * targetRatio
                            }
                            val nw = newWpx / boxW
                            val nh = newHpx / boxH
                            val newL = if (cornerXSign < 0f) (cur.right - nw).coerceIn(0f, 1f - nw) else cur.left.coerceIn(0f, 1f - nw)
                            val newT = if (cornerYSign < 0f) (cur.bottom - nh).coerceIn(0f, 1f - nh) else cur.top.coerceIn(0f, 1f - nh)
                            frame = ImageCropRect(newL, newT, newL + nw, newT + nh)
                        } else {
                            val nl = if (cornerXSign < 0f) (cur.left + dx).coerceIn(0f, cur.right - 0.14f) else cur.left
                            val nr = if (cornerXSign > 0f) (cur.right + dx).coerceIn(cur.left + 0.14f, 1f) else cur.right
                            val nt = if (cornerYSign < 0f) (cur.top + dy).coerceIn(0f, cur.bottom - 0.14f) else cur.top
                            val nb = if (cornerYSign > 0f) (cur.bottom + dy).coerceIn(cur.top + 0.14f, 1f) else cur.bottom
                            frame = ImageCropRect(nl, nt, nr, nb)
                        }
                    }

                    // For Free & Circle modes, edge midpoints let users make oval tall/flat or crop any side freely!
                    if (cropMode != ImportCropMode.RATIO_9_16) {
                        ImportCropEdgeHandle(cxDp, tDp, isHorizontal = true) { _, dy ->
                            val cur = liveFrame
                            frame = cur.copy(top = (cur.top + dy / boxH).coerceIn(0f, cur.bottom - 0.14f))
                        }
                        ImportCropEdgeHandle(cxDp, bDp, isHorizontal = true) { _, dy ->
                            val cur = liveFrame
                            frame = cur.copy(bottom = (cur.bottom + dy / boxH).coerceIn(cur.top + 0.14f, 1f))
                        }
                        ImportCropEdgeHandle(lDp, cyDp, isHorizontal = false) { dx, _ ->
                            val cur = liveFrame
                            frame = cur.copy(left = (cur.left + dx / boxW).coerceIn(0f, cur.right - 0.14f))
                        }
                        ImportCropEdgeHandle(rDp, cyDp, isHorizontal = false) { dx, _ ->
                            val cur = liveFrame
                            frame = cur.copy(right = (cur.right + dx / boxW).coerceIn(cur.left + 0.14f, 1f))
                        }
                    }

                    // 4 Corner Handles
                    ImportCropCornerHandle(lDp, tDp) { dx, dy -> updateFrameCorner(-1f, -1f, dx, dy) }
                    ImportCropCornerHandle(rDp, tDp) { dx, dy -> updateFrameCorner(1f, -1f, dx, dy) }
                    ImportCropCornerHandle(lDp, bDp) { dx, dy -> updateFrameCorner(-1f, 1f, dx, dy) }
                    ImportCropCornerHandle(rDp, bDp) { dx, dy -> updateFrameCorner(1f, 1f, dx, dy) }
                }
            }

            // Bottom 3 Options: Free (Default) | Circle | 9:16
            Surface(
                color = Color(0xFF171B24),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImportCropModeButton(
                        icon = Icons.Rounded.Crop,
                        label = ImportCropMode.FREE.label,
                        selected = cropMode == ImportCropMode.FREE,
                        tag = "import_crop_mode_free",
                        onClick = { applyModePreset(ImportCropMode.FREE) }
                    )
                    ImportCropModeButton(
                        icon = Icons.Rounded.RadioButtonUnchecked,
                        label = ImportCropMode.CIRCLE.label,
                        selected = cropMode == ImportCropMode.CIRCLE,
                        tag = "import_crop_mode_circle",
                        onClick = { applyModePreset(ImportCropMode.CIRCLE) }
                    )
                    ImportCropModeButton(
                        icon = Icons.Rounded.CropPortrait,
                        label = ImportCropMode.RATIO_9_16.label,
                        selected = cropMode == ImportCropMode.RATIO_9_16,
                        tag = "import_crop_mode_9_16",
                        onClick = { applyModePreset(ImportCropMode.RATIO_9_16) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportCropModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val accent = if (selected) StudioEmerald else Color.White.copy(alpha = 0.78f)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) StudioEmerald.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) StudioEmerald else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = accent
        )
    }
}

@Composable
private fun ImportCropEdgeHandle(
    xDp: androidx.compose.ui.unit.Dp,
    yDp: androidx.compose.ui.unit.Dp,
    isHorizontal: Boolean,
    onDrag: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .offset(x = xDp - 18.dp, y = yDp - 18.dp)
            .size(36.dp)
            .pointerInput(isHorizontal) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = if (isHorizontal) 24.dp else 6.dp, height = if (isHorizontal) 6.dp else 24.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(StudioEmerald)
                .border(1.5.dp, Color.White, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun ImportCropCornerHandle(
    xDp: androidx.compose.ui.unit.Dp,
    yDp: androidx.compose.ui.unit.Dp,
    onDrag: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .offset(x = xDp - 16.dp, y = yDp - 16.dp)
            .size(32.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
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
