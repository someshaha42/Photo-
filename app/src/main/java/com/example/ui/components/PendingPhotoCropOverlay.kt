package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.rounded.DeleteOutline
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald

@Composable
fun PendingPhotoCropOverlay(
    pendingPhoto: PendingImportPhoto,
    onDelete: () -> Unit,
    onDone: (crop: ImageCropRect, shape: ImageCropShape) -> Unit
) {
    BackHandler(onBack = onDelete)

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

    fun computeFinalCropRect(): ImageCropRect {
        val invScale = 1f / imgScale.coerceAtLeast(1f)
        val origLeft = (0.5f + (frame.left - 0.5f - panXNorm) * invScale).coerceIn(0f, 0.88f)
        val origTop = (0.5f + (frame.top - 0.5f - panYNorm) * invScale).coerceIn(0f, 0.88f)
        val origRight = (0.5f + (frame.right - 0.5f - panXNorm) * invScale)
            .coerceIn((origLeft + 0.12f).coerceAtMost(1f), 1f)
        val origBottom = (0.5f + (frame.bottom - 0.5f - panYNorm) * invScale)
            .coerceIn((origTop + 0.12f).coerceAtMost(1f), 1f)
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Crop Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { onDone(computeFinalCropRect(), ImageCropShape.RECTANGLE) },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("import_crop_done_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Done", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }

            // Interactive Direct-Manipulation Crop Box (4 Corners = Free Crop Resize, 4 Sides = Single-Edge Crop, Center = Move Frame)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
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

                val liveFrame by rememberUpdatedState(frame)

                Box(
                    modifier = containerMod
                        .onSizeChanged { sz ->
                            boxW = sz.width.toFloat().coerceAtLeast(1f)
                            boxH = sz.height.toFloat().coerceAtLeast(1f)
                        }
                ) {
                    // Clipped image + crop frame box (supports dragging crop frame & optional 2-finger pinch/pan)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .clipToBounds()
                            .pointerInput(pendingPhoto.id) {
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
                                                val insideFrame = nx in f.left..f.right && ny in f.top..f.bottom
                                                movingFrame = !isFullFree && insideFrame
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

                        // Crop Dim Mask + Grid
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

                    // 4 Side Edge Handles (strictly single-edge crop) + 4 Corner Dots (free corner crop resize!)
                    val density = LocalDensity.current
                    val lDp = with(density) { (frame.left * boxW).toDp() }
                    val tDp = with(density) { (frame.top * boxH).toDp() }
                    val rDp = with(density) { (frame.right * boxW).toDp() }
                    val bDp = with(density) { (frame.bottom * boxH).toDp() }
                    val cxDp = with(density) { (((frame.left + frame.right) * 0.5f) * boxW).toDp() }
                    val cyDp = with(density) { (((frame.top + frame.bottom) * 0.5f) * boxH).toDp() }

                    // Top edge: crops ONLY top edge
                    ImportCropEdgeHandle(cxDp, tDp, isHorizontal = true, tag = "import_crop_edge_top") { _, dy ->
                        val cur = liveFrame
                        frame = cur.copy(top = (cur.top + dy / boxH).coerceIn(0f, cur.bottom - 0.12f))
                    }
                    // Bottom edge: crops ONLY bottom edge
                    ImportCropEdgeHandle(cxDp, bDp, isHorizontal = true, tag = "import_crop_edge_bottom") { _, dy ->
                        val cur = liveFrame
                        frame = cur.copy(bottom = (cur.bottom + dy / boxH).coerceIn(cur.top + 0.12f, 1f))
                    }
                    // Left edge: crops ONLY left edge
                    ImportCropEdgeHandle(lDp, cyDp, isHorizontal = false, tag = "import_crop_edge_left") { dx, _ ->
                        val cur = liveFrame
                        frame = cur.copy(left = (cur.left + dx / boxW).coerceIn(0f, cur.right - 0.12f))
                    }
                    // Right edge: crops ONLY right edge
                    ImportCropEdgeHandle(rDp, cyDp, isHorizontal = false, tag = "import_crop_edge_right") { dx, _ ->
                        val cur = liveFrame
                        frame = cur.copy(right = (cur.right + dx / boxW).coerceIn(cur.left + 0.12f, 1f))
                    }

                    // 4 Corner Dots: Dragging any corner dot freely resizes that corner of the crop area
                    ImportCropCornerHandle(lDp, tDp, "import_crop_corner_tl") { dx, dy ->
                        val cur = liveFrame
                        frame = cur.copy(
                            left = (cur.left + dx / boxW).coerceIn(0f, cur.right - 0.12f),
                            top = (cur.top + dy / boxH).coerceIn(0f, cur.bottom - 0.12f)
                        )
                    }
                    ImportCropCornerHandle(rDp, tDp, "import_crop_corner_tr") { dx, dy ->
                        val cur = liveFrame
                        frame = cur.copy(
                            right = (cur.right + dx / boxW).coerceIn(cur.left + 0.12f, 1f),
                            top = (cur.top + dy / boxH).coerceIn(0f, cur.bottom - 0.12f)
                        )
                    }
                    ImportCropCornerHandle(lDp, bDp, "import_crop_corner_bl") { dx, dy ->
                        val cur = liveFrame
                        frame = cur.copy(
                            left = (cur.left + dx / boxW).coerceIn(0f, cur.right - 0.12f),
                            bottom = (cur.bottom + dy / boxH).coerceIn(cur.top + 0.12f, 1f)
                        )
                    }
                    ImportCropCornerHandle(rDp, bDp, "import_crop_corner_br") { dx, dy ->
                        val cur = liveFrame
                        frame = cur.copy(
                            right = (cur.right + dx / boxW).coerceIn(cur.left + 0.12f, 1f),
                            bottom = (cur.bottom + dy / boxH).coerceIn(cur.top + 0.12f, 1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportCropEdgeHandle(
    xDp: androidx.compose.ui.unit.Dp,
    yDp: androidx.compose.ui.unit.Dp,
    isHorizontal: Boolean,
    tag: String,
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
            }
            .testTag(tag),
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
    tag: String,
    onDrag: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .offset(x = xDp - 18.dp, y = yDp - 18.dp)
            .size(36.dp)
            .pointerInput(tag) {
                detectDragGestures { change, dragAmount ->
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

