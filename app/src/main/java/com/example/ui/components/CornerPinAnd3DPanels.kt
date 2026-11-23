package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorLayer
import com.example.model.ImageLayer
import com.example.model.Layer3DTransform
import com.example.ui.theme.StudioEmerald
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

private val AmPanelBg = Color(0xFF1F222E)
private val AmPanelDivider = Color(0xFF12141C)
private val AmPillBg = Color(0xFF303549)
private val AmLinkButtonBg = Color(0xFF5A6073)
private val AmGold = Color(0xFFF0B44A)
private val AmActiveModeBg = Color(0xFF0D0F16)
private val AmMutedText = Color(0xFF8B91A6)

@Composable
fun CornerPinToolPanel(
    canUndo: Boolean,
    isAddPointMode: Boolean = false,
    puppetPinCount: Int = 0,
    onToggleAddPointMode: () -> Unit = {},
    onClearPuppetPins: () -> Unit = {},
    onUndo: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Corner Pin",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isAddPointMode) {
                    "Tap layer to pin ($puppetPinCount)"
                } else {
                    "Drag corners or Add Point"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isAddPointMode,
                onClick = onToggleAddPointMode,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AddLocationAlt,
                        contentDescription = "Add Point",
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("Add Point", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("corner_pin_add_point_toggle")
            )

            if (puppetPinCount > 0) {
                OutlinedButton(
                    onClick = onClearPuppetPins,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("corner_pin_clear_pins_button")
                ) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Clear Pins", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear ($puppetPinCount)")
                }
            }

            OutlinedButton(
                onClick = onUndo,
                enabled = canUndo,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("corner_pin_undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                    contentDescription = "Undo Corner Pin",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Undo", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioEmerald,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("corner_pin_confirm_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Confirm Corner Pin",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        }
    }
}

enum class ThreeDSubMode {
    MOVE,
    ROTATE,
    SCALE,
    SKEW
}

@Composable
fun FloatingThreeDControllerPanel(
    selectedLayer: EditorLayer?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onMoveXY: (dxNorm: Float, dyNorm: Float) -> Unit,
    onUpdate3D: ((Layer3DTransform) -> Layer3DTransform) -> Unit,
    onSetRotationZ: (Float) -> Unit,
    onScale3D: (scaleFactor: Float?, stretchXFactor: Float?, stretchYFactor: Float?) -> Unit,
    onResetMode: (Int) -> Unit,
    onCenterLayer: () -> Unit,
    onResetAll: () -> Unit,
    onEndTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedLayer == null) return

    var activeMode by remember { mutableStateOf(ThreeDSubMode.MOVE) }
    var moveIsZAxis by remember { mutableStateOf(false) }
    var rotateAxis by remember { mutableIntStateOf(1) } // 0=X, 1=Y, 2=Z
    var isScaleLinked by remember { mutableStateOf(true) }
    var scaleActiveWidth by remember { mutableStateOf(true) }
    var skewActiveX by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val liveLayer by rememberUpdatedState(selectedLayer)

    FloatingDraggableResizablePanel(
        panelTag = "three_d_floating_panel",
        dragHandleTag = "three_d_drag_handle",
        resizeTagPrefix = "three_d_resize",
        defaultWidthFraction = 0.96f,
        defaultHeightDp = 268.dp,
        minWidthDp = 210.dp,
        minHeightDp = 165.dp,
        defaultAlignment = Alignment.BottomCenter,
        bottomClearanceDp = 4.dp,
        surfaceColor = AmPanelBg,
        borderColor = Color.White.copy(alpha = 0.08f),
        cornerAccentColor = AmGold,
        dragStripColor = Color.White.copy(alpha = 0.28f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 3-Column Body (NO Keyframe diamonds, NO Easing graphs)
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // LEFT VERTICAL COLUMN: Top = Undo / Redo, Bottom = 3-dot Menu
                    Column(
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight()
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onUndo,
                                enabled = canUndo,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("three_d_undo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                                    contentDescription = "Undo",
                                    tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.28f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = onRedo,
                                enabled = canRedo,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("three_d_redo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Redo,
                                    contentDescription = "Redo",
                                    tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.28f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("three_d_more_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreHoriz,
                                    contentDescription = "3D Options",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Reset Current Mode") },
                                    onClick = {
                                        showMoreMenu = false
                                        onResetMode(activeMode.ordinal)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Center Layer") },
                                    onClick = {
                                        showMoreMenu = false
                                        onCenterLayer()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset All 3D") },
                                    onClick = {
                                        showMoreMenu = false
                                        onResetAll()
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(AmPanelDivider))

                    // CENTER GESTURE CONTROLLER PANEL
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        when (activeMode) {
                            ThreeDSubMode.MOVE -> {
                                ThreeDMoveCenterPanel(
                                    moveIsZAxis = moveIsZAxis,
                                    onSelectXY = { moveIsZAxis = false },
                                    onSelectZ = { moveIsZAxis = true },
                                    onMoveXY = onMoveXY,
                                    onAdjustZ = { dz ->
                                        onUpdate3D { t ->
                                            t.copy(positionZ = (t.positionZ + dz).coerceIn(-600f, 600f))
                                        }
                                    },
                                    onEndTransaction = onEndTransaction
                                )
                            }
                            ThreeDSubMode.ROTATE -> {
                                val curDeg = when (rotateAxis) {
                                    0 -> liveLayer.transform3D.rotationXDeg
                                    1 -> liveLayer.transform3D.rotationYDeg
                                    else -> liveLayer.rotationDeg
                                }
                                ThreeDRotateCenterPanel(
                                    selectedAxis = rotateAxis,
                                    currentAngleDeg = curDeg,
                                    onSelectAxis = { rotateAxis = it },
                                    onDeltaAngle = { deltaDeg ->
                                        when (rotateAxis) {
                                            0 -> onUpdate3D { t -> t.copy(rotationXDeg = wrapAngleDeg(t.rotationXDeg + deltaDeg)) }
                                            1 -> onUpdate3D { t -> t.copy(rotationYDeg = wrapAngleDeg(t.rotationYDeg + deltaDeg)) }
                                            else -> onSetRotationZ(wrapAngleDeg(liveLayer.rotationDeg + deltaDeg))
                                        }
                                    },
                                    onEndTransaction = onEndTransaction
                                )
                            }
                            ThreeDSubMode.SCALE -> {
                                val layerForScale = liveLayer
                                val baseRefW = if (layerForScale is ImageLayer) layerForScale.originalWidth.toFloat().coerceIn(240f, 1600f) else 600f
                                val baseRefH = if (layerForScale is ImageLayer) layerForScale.originalHeight.toFloat().coerceIn(240f, 1600f) else 400f
                                val dispW = baseRefW * layerForScale.scale * layerForScale.stretchX
                                val dispH = baseRefH * layerForScale.scale * layerForScale.stretchY
                                ThreeDScaleCenterPanel(
                                    displayWidth = dispW,
                                    displayHeight = dispH,
                                    isLinked = isScaleLinked,
                                    activeWidth = scaleActiveWidth,
                                    onToggleLink = { isScaleLinked = !isScaleLinked },
                                    onSelectWidth = { scaleActiveWidth = true },
                                    onSelectHeight = { scaleActiveWidth = false },
                                    onScrubUniform = { dxPx ->
                                        val factor = exp(dxPx / 260f).coerceIn(0.85f, 1.18f)
                                        onScale3D(factor, null, null)
                                    },
                                    onScrubWidth = { dxPx ->
                                        scaleActiveWidth = true
                                        val factor = exp(dxPx / 260f).coerceIn(0.85f, 1.18f)
                                        onScale3D(null, factor, null)
                                    },
                                    onScrubHeight = { dxPx ->
                                        scaleActiveWidth = false
                                        val factor = exp(dxPx / 260f).coerceIn(0.85f, 1.18f)
                                        onScale3D(null, null, factor)
                                    },
                                    onEndTransaction = onEndTransaction
                                )
                            }
                            ThreeDSubMode.SKEW -> {
                                ThreeDSkewCenterPanel(
                                    skewXDeg = liveLayer.transform3D.skewXDeg,
                                    skewYDeg = liveLayer.transform3D.skewYDeg,
                                    activeX = skewActiveX,
                                    onSelectX = { skewActiveX = true },
                                    onSelectY = { skewActiveX = false },
                                    onScrubX = { dxPx ->
                                        skewActiveX = true
                                        onUpdate3D { t ->
                                            t.copy(skewXDeg = (t.skewXDeg + dxPx * 0.25f).coerceIn(-75f, 75f))
                                        }
                                    },
                                    onScrubY = { dxPx ->
                                        skewActiveX = false
                                        onUpdate3D { t ->
                                            t.copy(skewYDeg = (t.skewYDeg + dxPx * 0.25f).coerceIn(-75f, 75f))
                                        }
                                    },
                                    onEndTransaction = onEndTransaction
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(AmPanelDivider))

                    // RIGHT VERTICAL COLUMN: 4 Transform Mode Buttons (Move | Rotate | Scale | Skew)
                    Column(
                        modifier = Modifier
                            .width(56.dp)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val railBtnModifier = Modifier
                            .weight(1f, fill = false)
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp, maxWidth = 46.dp, maxHeight = 46.dp)
                        RightRailModeButton(
                            selected = activeMode == ThreeDSubMode.MOVE,
                            tag = "three_d_mode_move",
                            onClick = { activeMode = ThreeDSubMode.MOVE },
                            modifier = railBtnModifier
                        ) { tint ->
                            Icon(
                                imageVector = Icons.Rounded.OpenWith,
                                contentDescription = "Move Mode",
                                tint = tint,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        RightRailModeButton(
                            selected = activeMode == ThreeDSubMode.ROTATE,
                            tag = "three_d_mode_rotate",
                            onClick = { activeMode = ThreeDSubMode.ROTATE },
                            modifier = railBtnModifier
                        ) { tint ->
                            RotateBoxIcon(tint = tint, modifier = Modifier.size(22.dp))
                        }

                        RightRailModeButton(
                            selected = activeMode == ThreeDSubMode.SCALE,
                            tag = "three_d_mode_scale",
                            onClick = { activeMode = ThreeDSubMode.SCALE },
                            modifier = railBtnModifier
                        ) { tint ->
                            ScaleBoxIcon(tint = tint, modifier = Modifier.size(21.dp))
                        }

                        RightRailModeButton(
                            selected = activeMode == ThreeDSubMode.SKEW,
                            tag = "three_d_mode_skew",
                            onClick = { activeMode = ThreeDSubMode.SKEW },
                            modifier = railBtnModifier
                        ) { tint ->
                            SkewBoxIcon(tint = tint, modifier = Modifier.size(21.dp))
                        }
                }
            }
        }
    }
}

@Composable
private fun ThreeDMoveCenterPanel(
    moveIsZAxis: Boolean,
    onSelectXY: () -> Unit,
    onSelectZ: () -> Unit,
    onMoveXY: (dxNorm: Float, dyNorm: Float) -> Unit,
    onAdjustZ: (deltaZ: Float) -> Unit,
    onEndTransaction: () -> Unit
) {
    val liveIsZ by rememberUpdatedState(moveIsZAxis)

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle 4 corner L-brackets framing the swipe pad (like Alight Motion screenshots)
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val len = 12.dp.toPx()
            val stroke = 2.dp.toPx()
            val col = AmMutedText.copy(alpha = 0.7f)
            // TL
            drawLine(col, Offset(0f, len), Offset(0f, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(col, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            // TR
            drawLine(col, Offset(size.width - len, 0f), Offset(size.width, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(col, Offset(size.width, 0f), Offset(size.width, len), strokeWidth = stroke, cap = StrokeCap.Round)
            // BL
            drawLine(col, Offset(0f, size.height - len), Offset(0f, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(col, Offset(0f, size.height), Offset(len, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
            // BR
            drawLine(col, Offset(size.width - len, size.height), Offset(size.width, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(col, Offset(size.width, size.height), Offset(size.width, size.height - len), strokeWidth = stroke, cap = StrokeCap.Round)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Pills: [ X   Y ] and [ Z ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.7f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSelectXY)
                        .testTag("three_d_move_axis_xy"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmPillBg),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val xyColor = if (!moveIsZAxis) AmGold else Color.White
                        Text("X", color = xyColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Y", color = xyColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("X", color = AmMutedText, fontSize = 10.sp)
                        Text("Y", color = AmMutedText, fontSize = 10.sp)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSelectZ)
                        .testTag("three_d_move_axis_z"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmPillBg),
                        contentAlignment = Alignment.Center
                    ) {
                        val zColor = if (moveIsZAxis) AmGold else Color.White
                        Text("Z", color = zColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    Text("Z", color = AmMutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }

            // Interactive Swipe Pad
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { onEndTransaction() },
                            onDragCancel = { onEndTransaction() }
                        ) { change, dragAmount ->
                            change.consume()
                            if (liveIsZ) {
                                // Dragging UP brings closer (+Z), DOWN pushes deeper (-Z)
                                onAdjustZ(-dragAmount.y * 1.15f)
                            } else {
                                onMoveXY(
                                    dragAmount.x / size.width.coerceAtLeast(180).toFloat() * 0.55f,
                                    dragAmount.y / size.height.coerceAtLeast(120).toFloat() * 0.45f
                                )
                            }
                        }
                    }
                    .testTag("three_d_move_pad"),
                contentAlignment = Alignment.Center
            ) {
                if (moveIsZAxis) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(width = 22.dp, height = 10.dp)) {
                            val p = Path().apply {
                                moveTo(size.width / 2f, 0f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(p, color = AmMutedText.copy(alpha = 0.45f))
                        }
                        Text(
                            text = "Swipe here to adjust Z position",
                            color = AmMutedText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Canvas(modifier = Modifier.size(width = 36.dp, height = 16.dp)) {
                            val p = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                lineTo(size.width / 2f, size.height)
                                close()
                            }
                            drawPath(p, color = AmMutedText.copy(alpha = 0.55f))
                        }
                    }
                } else {
                    Text(
                        text = "Swipe here to move layer",
                        color = AmMutedText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreeDRotateCenterPanel(
    selectedAxis: Int,
    currentAngleDeg: Float,
    onSelectAxis: (Int) -> Unit,
    onDeltaAngle: (Float) -> Unit,
    onEndTransaction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Pills: X | Y | Z
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("X" to "three_d_rotate_axis_x", "Y" to "three_d_rotate_axis_y", "Z" to "three_d_rotate_axis_z")
                .forEachIndexed { idx, (label, tag) ->
                    val isSel = selectedAxis == idx
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectAxis(idx) }
                            .testTag(tag),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmPillBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) AmGold else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                        Text(label, color = AmMutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
                    }
                }
        }

        // Circular Orbital Rotation Dial
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(selectedAxis) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        var prevAngleRad = atan2(down.position.y - cy, down.position.x - cx)
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (change == null || !change.pressed) break
                                change.consume()
                                val curRad = atan2(change.position.y - cy, change.position.x - cx)
                                var dRad = curRad - prevAngleRad
                                while (dRad > PI) dRad -= (2.0 * PI).toFloat()
                                while (dRad < -PI) dRad += (2.0 * PI).toFloat()
                                prevAngleRad = curRad
                                val dDeg = -Math.toDegrees(dRad.toDouble()).toFloat()
                                if (dDeg != 0f) {
                                    onDeltaAngle(dDeg)
                                }
                            }
                        } finally {
                            onEndTransaction()
                        }
                    }
                }
                .testTag("three_d_rotate_dial"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = (minOf(size.width, size.height) * 0.42f).coerceAtLeast(24f)
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(
                    color = AmPanelDivider,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw arc from 3 o'clock (0°) by -currentAngleDeg so positive angle sweeps upward right like screenshot
                drawArc(
                    color = AmGold,
                    startAngle = 0f,
                    sweepAngle = -currentAngleDeg.coerceIn(-360f, 360f),
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )

                val rad = Math.toRadians(-currentAngleDeg.toDouble())
                val knobCenter = Offset(
                    x = center.x + (radius * cos(rad)).toFloat(),
                    y = center.y + (radius * sin(rad)).toFloat()
                )
                drawCircle(
                    color = Color.White,
                    radius = 15.dp.toPx(),
                    center = knobCenter
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmPillBg)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${currentAngleDeg.roundToInt()}°",
                    color = AmGold,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ThreeDScaleCenterPanel(
    displayWidth: Float,
    displayHeight: Float,
    isLinked: Boolean,
    activeWidth: Boolean,
    onToggleLink: () -> Unit,
    onSelectWidth: () -> Unit,
    onSelectHeight: () -> Unit,
    onScrubUniform: (dxPx: Float) -> Unit,
    onScrubWidth: (dxPx: Float) -> Unit,
    onScrubHeight: (dxPx: Float) -> Unit,
    onEndTransaction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Width pill | Link toggle | Height pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelectWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmPillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", displayWidth),
                        color = if (isLinked || activeWidth) AmGold else Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
                Text("Width", color = AmMutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmLinkButtonBg)
                    .clickable(onClick = onToggleLink)
                    .testTag("three_d_scale_link_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLinked) Icons.Rounded.Link else Icons.Rounded.LinkOff,
                    contentDescription = if (isLinked) "Linked Scale" else "Unlinked Scale",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelectHeight),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmPillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", displayHeight),
                        color = if (isLinked || !activeWidth) AmGold else Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
                Text("Height", color = AmMutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }

        if (isLinked) {
            TickScrubberRuler(
                valueTracker = displayWidth,
                isActive = true,
                onScrub = onScrubUniform,
                onEndTransaction = onEndTransaction,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("three_d_scale_ruler_linked")
            )
        } else {
            TickScrubberRuler(
                valueTracker = displayWidth,
                isActive = activeWidth,
                onScrub = onScrubWidth,
                onEndTransaction = onEndTransaction,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("three_d_scale_ruler_w")
            )
            TickScrubberRuler(
                valueTracker = displayHeight,
                isActive = !activeWidth,
                onScrub = onScrubHeight,
                onEndTransaction = onEndTransaction,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("three_d_scale_ruler_h")
            )
        }
    }
}

@Composable
private fun ThreeDSkewCenterPanel(
    skewXDeg: Float,
    skewYDeg: Float,
    activeX: Boolean,
    onSelectX: () -> Unit,
    onSelectY: () -> Unit,
    onScrubX: (dxPx: Float) -> Unit,
    onScrubY: (dxPx: Float) -> Unit,
    onEndTransaction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelectX),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmPillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f°", skewXDeg),
                        color = if (activeX) AmGold else Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
                Text("X SKEW", color = AmMutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelectY),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmPillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f°", skewYDeg),
                        color = if (!activeX) AmGold else Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
                Text("Y SKEW", color = AmMutedText, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }

        TickScrubberRuler(
            valueTracker = skewXDeg * 4f,
            isActive = activeX,
            onScrub = onScrubX,
            onEndTransaction = onEndTransaction,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("three_d_skew_ruler_x")
        )
        TickScrubberRuler(
            valueTracker = skewYDeg * 4f,
            isActive = !activeX,
            onScrub = onScrubY,
            onEndTransaction = onEndTransaction,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("three_d_skew_ruler_y")
        )
    }
}

@Composable
private fun TickScrubberRuler(
    valueTracker: Float,
    isActive: Boolean,
    onScrub: (dxPx: Float) -> Unit,
    onEndTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scrollPhasePx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { onEndTransaction() },
                    onDragCancel = { onEndTransaction() }
                ) { change, dragAmount ->
                    change.consume()
                    scrollPhasePx += dragAmount
                    onScrub(dragAmount)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 6.dp.toPx()
            val centerX = size.width / 2f
            val effectiveOffset = ((scrollPhasePx + valueTracker) % step)
            val tickTop = size.height * 0.14f
            val tickBottom = size.height * 0.86f
            val countHalf = (size.width / (2f * step)).toInt() + 2

            for (i in -countHalf..countHalf) {
                val x = centerX + i * step + effectiveOffset
                if (x < 0f || x > size.width) continue
                val distFrac = (kotlin.math.abs(x - centerX) / (size.width * 0.5f)).coerceIn(0f, 1f)
                val alpha = (1f - distFrac * 0.85f).coerceIn(0.08f, 0.72f)
                drawLine(
                    color = Color.White.copy(alpha = alpha * 0.55f),
                    start = Offset(x, tickTop),
                    end = Offset(x, tickBottom),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Center glowing vertical marker line
            drawLine(
                color = if (isActive) AmGold else Color.White,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 2.4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun RightRailModeButton(
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (tint: Color) -> Unit
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = 34.dp, minHeight = 34.dp, maxWidth = 46.dp, maxHeight = 46.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AmActiveModeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        content(if (selected) AmGold else Color.White)
    }
}

@Composable
private fun RotateBoxIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.8.dp.toPx()
        val boxS = size.minDimension * 0.34f
        drawRect(
            color = tint,
            topLeft = Offset(center.x - boxS / 2f, center.y - boxS / 2f),
            size = Size(boxS, boxS),
            style = Stroke(width = stroke, join = StrokeJoin.Round)
        )
        val arcR = size.minDimension * 0.44f
        drawArc(
            color = tint,
            startAngle = -140f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(center.x - arcR, center.y - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = 40f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(center.x - arcR, center.y - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ScaleBoxIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.8.dp.toPx()
        val pad = size.minDimension * 0.14f
        val inner = size.minDimension - pad * 2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(pad, pad),
            size = Size(inner, inner),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = stroke)
        )
        val smallS = inner * 0.38f
        drawRect(
            color = tint,
            topLeft = Offset(pad, size.height - pad - smallS),
            size = Size(smallS, smallS),
            style = Stroke(width = stroke)
        )
        val ax0 = pad + smallS * 0.9f
        val ay0 = size.height - pad - smallS * 0.9f
        val ax1 = size.width - pad - inner * 0.22f
        val ay1 = pad + inner * 0.22f
        drawLine(tint, Offset(ax0, ay0), Offset(ax1, ay1), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(ax1, ay1), Offset(ax1 - inner * 0.22f, ay1), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(ax1, ay1), Offset(ax1, ay1 + inner * 0.22f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun SkewBoxIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.7.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))
        val left = size.width * 0.14f
        val right = size.width * 0.68f
        val top = size.height * 0.2f
        val bottom = size.height * 0.82f

        drawRect(
            color = tint.copy(alpha = 0.7f),
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            style = Stroke(width = stroke * 0.85f, pathEffect = dash)
        )

        val p = Path().apply {
            moveTo(size.width * 0.32f, top)
            lineTo(size.width * 0.88f, top)
            lineTo(size.width * 0.66f, bottom)
            lineTo(size.width * 0.12f, bottom)
            close()
        }
        drawPath(p, color = tint, style = Stroke(width = stroke, join = StrokeJoin.Round))
    }
}

private fun wrapAngleDeg(deg: Float): Float {
    var d = deg % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}
