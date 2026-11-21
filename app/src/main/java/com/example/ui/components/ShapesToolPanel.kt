package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.VectorShapeEngine
import com.example.model.LayerShapeType
import com.example.model.ShapeLayer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioEmerald
import kotlin.math.roundToInt

private val ALIGHT_PICKER_SHAPES = listOf(
    LayerShapeType.CIRCLE,
    LayerShapeType.ROUNDED_RECT,
    LayerShapeType.RECTANGLE,
    LayerShapeType.TRIANGLE,
    LayerShapeType.STAR,
    LayerShapeType.HEXAGON,
    LayerShapeType.PIE,
    LayerShapeType.ARROW,
    LayerShapeType.QUAD
)

@Composable
fun ShapesToolPanel(
    selectedShapeLayer: ShapeLayer?,
    isEditPointsMode: Boolean = false,
    onToggleEditPointsMode: () -> Unit = {},
    onAddShape: (LayerShapeType) -> Unit,
    onUpdateShape: ((ShapeLayer) -> ShapeLayer) -> Unit,
    onUpdateShapeTransient: ((ShapeLayer) -> ShapeLayer) -> Unit = onUpdateShape,
    onEndTransaction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: Alight Motion style Shape bar with Vector Points vs Transform Bounds toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Category,
                    contentDescription = "Shapes",
                    tint = StudioEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (selectedShapeLayer != null) "Edit Shape" else "Add Shape",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (selectedShapeLayer != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isEditPointsMode,
                        onClick = onToggleEditPointsMode,
                        label = { Text(if (isEditPointsMode) "Edit Points" else "Bounds") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isEditPointsMode) Icons.Rounded.AutoFixHigh else Icons.Rounded.CropFree,
                                contentDescription = "Toggle Shape Point Handles",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("shape_edit_points_toggle")
                    )
                    OutlinedButton(
                        onClick = {
                            onUpdateShape {
                                it.copy(vectorParams = VectorShapeEngine.defaultParamsFor(it.shapeType))
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("shape_reset_points_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset Shape Geometry",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Alight-Motion-Style White Silhouette Shape Grid (2 compact rows / horizontal scroll strip)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("shape_picker_grid")
        ) {
            val rows = remember { ALIGHT_PICKER_SHAPES.chunked(5) }
            rows.forEach { rowShapes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowShapes.forEach { shapeType ->
                        val isCurrent = selectedShapeLayer?.shapeType == shapeType
                        ShapeSilhouetteTile(
                            shapeType = shapeType,
                            isSelected = isCurrent,
                            onClick = { onAddShape(shapeType) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill trailing grid slots cleanly for visual alignment
                    repeat(5 - rowShapes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (selectedShapeLayer != null) {
            // Quick White-First Fill Swatches
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shapeColors = remember {
                    listOf(0xFFFFFFFFL) + LAYER_COLORS.filterNot { it == 0xFFFFFFFFL }
                }
                shapeColors.forEach { argb ->
                    val selected = selectedShapeLayer.fillColorArgb == argb
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(argb.toInt()))
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) StudioAzure else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onUpdateShape { it.copy(fillColorArgb = argb) } }
                            .testTag("shape_color_${argb.toString(16)}")
                    )
                }
            }

            // Per-shape parametric vector geometry controls
            ShapeParametricSliders(
                layer = selectedShapeLayer,
                onUpdateShapeTransient = onUpdateShapeTransient,
                onEndTransaction = onEndTransaction
            )

            // Shape Border Width
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Border", style = MaterialTheme.typography.labelLarge)
                Text("${selectedShapeLayer.strokeWidth.roundToInt()}dp", style = MaterialTheme.typography.labelLarge)
            }
            Slider(
                value = selectedShapeLayer.strokeWidth.coerceIn(0f, 16f),
                onValueChange = { w -> onUpdateShapeTransient { it.copy(strokeWidth = w) } },
                onValueChangeFinished = onEndTransaction,
                valueRange = 0f..16f,
                modifier = Modifier.testTag("shape_stroke_width_slider")
            )
        }
    }
}

@Composable
private fun ShapeSilhouetteTile(
    shapeType: LayerShapeType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) StudioAzure.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        modifier = modifier
            .height(62.dp)
            .minimumInteractiveComponentSize()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("shape_pick_${shapeType.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Canvas(
                modifier = Modifier
                    .size(30.dp)
            ) {
                val params = VectorShapeEngine.defaultParamsFor(shapeType)
                val base = size.minDimension * 0.86f
                val path = VectorShapeEngine.buildShapePath(
                    shapeType = shapeType,
                    params = params,
                    baseSizePx = base,
                    cx = size.width * 0.5f,
                    cy = size.height * 0.5f
                ).asComposePath()
                drawPath(path = path, color = Color.White, style = Fill)

                // Match Alight Motion white vertex accent dots on editable point shapes
                if (shapeType == LayerShapeType.TRIANGLE || shapeType == LayerShapeType.ARROW || shapeType == LayerShapeType.QUAD) {
                    val handles = VectorShapeEngine.computeControlHandles(
                        layer = ShapeLayer(shapeType = shapeType, vectorParams = params),
                        baseSizePx = base
                    )
                    val offsetX = (size.width - base) * 0.5f
                    val offsetY = (size.height - base) * 0.5f
                    handles.filter { it.kind != com.example.data.ShapeHandleKind.RADIUS_PILL }.forEach { h ->
                        drawCircle(
                            color = StudioAmber,
                            radius = 2.6.dp.toPx(),
                            center = Offset(offsetX + h.xPx, offsetY + h.yPx)
                        )
                    }
                }
            }
            Text(
                text = shapeType.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun ShapeParametricSliders(
    layer: ShapeLayer,
    onUpdateShapeTransient: ((ShapeLayer) -> ShapeLayer) -> Unit,
    onEndTransaction: () -> Unit
) {
    val p = layer.vectorParams
    when (layer.shapeType) {
        LayerShapeType.ROUNDED_RECT, LayerShapeType.RECTANGLE -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Corner Roundness", style = MaterialTheme.typography.labelMedium)
                Text("${(p.cornerRadiusFrac * 200f).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = p.cornerRadiusFrac.coerceIn(0f, 0.5f),
                onValueChange = { r -> onUpdateShapeTransient { it.copy(vectorParams = it.vectorParams.copy(cornerRadiusFrac = r)) } },
                onValueChangeFinished = onEndTransaction,
                valueRange = 0f..0.5f,
                modifier = Modifier.testTag("shape_corner_radius_slider")
            )
        }
        LayerShapeType.STAR, LayerShapeType.BADGE -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Points", style = MaterialTheme.typography.labelMedium)
                Text("${p.starPoints}", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = p.starPoints.toFloat().coerceIn(3f, 12f),
                onValueChange = { pts -> onUpdateShapeTransient { it.copy(vectorParams = it.vectorParams.copy(starPoints = pts.roundToInt())) } },
                onValueChangeFinished = onEndTransaction,
                valueRange = 3f..12f,
                steps = 8,
                modifier = Modifier.testTag("shape_star_points_slider")
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Inner Radius", style = MaterialTheme.typography.labelMedium)
                Text("${(p.starInnerRatio * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = p.starInnerRatio.coerceIn(0.15f, 0.88f),
                onValueChange = { r -> onUpdateShapeTransient { it.copy(vectorParams = it.vectorParams.copy(starInnerRatio = r)) } },
                onValueChangeFinished = onEndTransaction,
                valueRange = 0.15f..0.88f,
                modifier = Modifier.testTag("shape_star_inner_slider")
            )
        }
        LayerShapeType.HEXAGON -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sides", style = MaterialTheme.typography.labelMedium)
                Text("${p.polygonSides}", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = p.polygonSides.toFloat().coerceIn(3f, 12f),
                onValueChange = { s -> onUpdateShapeTransient { it.copy(vectorParams = it.vectorParams.copy(polygonSides = s.roundToInt())) } },
                onValueChangeFinished = onEndTransaction,
                valueRange = 3f..12f,
                steps = 8,
                modifier = Modifier.testTag("shape_polygon_sides_slider")
            )
        }
        LayerShapeType.PIE -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pie Sweep", style = MaterialTheme.typography.labelMedium)
                Text("${p.pieSweepDeg.roundToInt()}°", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = p.pieSweepDeg.coerceIn(10f, 355f),
                onValueChange = { sw -> onUpdateShapeTransient { it.copy(vectorParams = it.vectorParams.copy(pieSweepDeg = sw)) } },
                onValueChangeFinished = onEndTransaction,
                valueRange = 10f..355f,
                modifier = Modifier.testTag("shape_pie_sweep_slider")
            )
        }
        LayerShapeType.ARROW -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Shaft Width", style = MaterialTheme.typography.labelMedium)
                Text("${(p.arrowShaftWidthFrac * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = p.arrowShaftWidthFrac.coerceIn(0.06f, 0.42f),
                onValueChange = { w -> onUpdateShapeTransient { it.copy(vectorParams = it.vectorParams.copy(arrowShaftWidthFrac = w)) } },
                onValueChangeFinished = onEndTransaction,
                valueRange = 0.06f..0.42f,
                modifier = Modifier.testTag("shape_arrow_width_slider")
            )
        }
        else -> Unit
    }
}
