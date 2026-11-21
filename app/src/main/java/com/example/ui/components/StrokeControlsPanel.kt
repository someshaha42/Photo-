package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.VectorStrokeEngine
import com.example.model.VectorLineCap
import com.example.model.VectorLineJoin
import com.example.model.VectorStrokeAlignment
import com.example.model.VectorStrokePattern
import com.example.model.VectorStrokeStyle
import com.example.model.WidthPointTransition
import com.example.model.WidthProfilePoint
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import kotlin.math.abs
import kotlin.math.roundToInt

enum class StrokeControlSection(val title: String) {
    THICKNESS("Thickness"),
    TAPER("Taper"),
    CAP_AND_JOIN("Cap & Join"),
    STROKE_STYLE("Stroke Style"),
    OPACITY("Opacity"),
    SMOOTHING("Smoothing"),
    WIDTH_PROFILE("Width Profile"),
    PRESSURE("Pressure"),
    ADVANCED("Advanced Controls")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StrokeControlsPanel(
    style: VectorStrokeStyle,
    selectedDrawLayerCount: Int,
    onStartNewStrokeSession: () -> Unit,
    onUpdateStyleTransient: ((VectorStrokeStyle) -> VectorStrokeStyle) -> Unit,
    onUpdateStyleCommit: ((VectorStrokeStyle) -> VectorStrokeStyle) -> Unit,
    onEndTransaction: () -> Unit,
    onClosePanel: () -> Unit
) {
    var expandedSection by remember { mutableStateOf<StrokeControlSection?>(StrokeControlSection.THICKNESS) }
    var selectedWidthPointId by remember { mutableStateOf<String?>(null) }

    fun toggleSection(section: StrokeControlSection) {
        expandedSection = if (expandedSection == section) null else section
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("stroke_controls_panel"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Context-Awareness Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (selectedDrawLayerCount > 0) StudioAzure.copy(alpha = 0.16f) else StudioEmerald.copy(alpha = 0.16f),
                modifier = Modifier.testTag("stroke_context_badge")
            ) {
                Text(
                    text = when {
                        selectedDrawLayerCount > 1 -> "Batch Editing $selectedDrawLayerCount Layers"
                        selectedDrawLayerCount == 1 -> "Editing Selected Layer"
                        else -> "New Stroke Defaults"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedDrawLayerCount > 0) StudioAzure else StudioEmerald,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (selectedDrawLayerCount > 0) {
                    TextButton(
                        onClick = onStartNewStrokeSession,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("stroke_new_defaults_button")
                    ) {
                        Text("New Stroke", style = MaterialTheme.typography.labelMedium)
                    }
                }
                IconButton(
                    onClick = onClosePanel,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("stroke_controls_close_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Stroke Controls", modifier = Modifier.size(18.dp))
                }
            }
        }

        // 1. Thickness
        CollapsibleStrokeSection(
            section = StrokeControlSection.THICKNESS,
            expanded = StrokeControlSection.THICKNESS == expandedSection,
            summary = "${(style.overallWidthNorm * 1000).roundToInt()}pt",
            onToggle = { toggleSection(StrokeControlSection.THICKNESS) }
        ) {
            LabeledStrokeSlider(
                label = "Overall Width",
                valueText = "${(style.overallWidthNorm * 1000).roundToInt()}pt",
                value = style.overallWidthNorm.coerceIn(0.003f, 0.08f),
                range = 0.003f..0.08f,
                tag = "stroke_overall_width_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(overallWidthNorm = v) } },
                onFinished = onEndTransaction
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Start / End Link", style = MaterialTheme.typography.labelMedium)
                FilterChip(
                    selected = style.widthsLinked,
                    onClick = {
                        onUpdateStyleCommit {
                            val nextLinked = !it.widthsLinked
                            if (nextLinked) it.copy(widthsLinked = true, endWidthFactor = it.startWidthFactor)
                            else it.copy(widthsLinked = false)
                        }
                    },
                    label = { Text(if (style.widthsLinked) "Linked" else "Unlinked") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (style.widthsLinked) Icons.Rounded.Link else Icons.Rounded.LinkOff,
                            contentDescription = "Toggle Start End Link",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("stroke_link_widths_toggle")
                )
            }

            LabeledStrokeSlider(
                label = "Start Width",
                valueText = "${(style.startWidthFactor * 100).roundToInt()}%",
                value = style.startWidthFactor.coerceIn(0.08f, 2.5f),
                range = 0.08f..2.5f,
                tag = "stroke_start_width_slider",
                onValueChange = { v ->
                    onUpdateStyleTransient {
                        if (it.widthsLinked) it.copy(startWidthFactor = v, endWidthFactor = v)
                        else it.copy(startWidthFactor = v)
                    }
                },
                onFinished = onEndTransaction
            )

            LabeledStrokeSlider(
                label = "End Width",
                valueText = "${((if (style.widthsLinked) style.startWidthFactor else style.endWidthFactor) * 100).roundToInt()}%",
                value = (if (style.widthsLinked) style.startWidthFactor else style.endWidthFactor).coerceIn(0.08f, 2.5f),
                range = 0.08f..2.5f,
                tag = "stroke_end_width_slider",
                onValueChange = { v ->
                    onUpdateStyleTransient {
                        if (it.widthsLinked) it.copy(startWidthFactor = v, endWidthFactor = v)
                        else it.copy(endWidthFactor = v)
                    }
                },
                onFinished = onEndTransaction
            )
        }

        // 2. Taper
        CollapsibleStrokeSection(
            section = StrokeControlSection.TAPER,
            expanded = StrokeControlSection.TAPER == expandedSection,
            summary = if (style.startTaperLength > 0.01f || style.endTaperLength > 0.01f) "Active" else "None",
            onToggle = { toggleSection(StrokeControlSection.TAPER) }
        ) {
            LabeledStrokeSlider(
                label = "Start Taper Length",
                valueText = "${(style.startTaperLength * 200).roundToInt()}%",
                value = style.startTaperLength.coerceIn(0f, 0.5f),
                range = 0f..0.5f,
                tag = "stroke_start_taper_length_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(startTaperLength = v) } },
                onFinished = onEndTransaction
            )
            LabeledStrokeSlider(
                label = "Start Taper Intensity",
                valueText = "${(style.startTaperIntensity * 100).roundToInt()}%",
                value = style.startTaperIntensity.coerceIn(0f, 1f),
                range = 0f..1f,
                tag = "stroke_start_taper_intensity_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(startTaperIntensity = v) } },
                onFinished = onEndTransaction
            )
            LabeledStrokeSlider(
                label = "End Taper Length",
                valueText = "${(style.endTaperLength * 200).roundToInt()}%",
                value = style.endTaperLength.coerceIn(0f, 0.5f),
                range = 0f..0.5f,
                tag = "stroke_end_taper_length_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(endTaperLength = v) } },
                onFinished = onEndTransaction
            )
            LabeledStrokeSlider(
                label = "End Taper Intensity",
                valueText = "${(style.endTaperIntensity * 100).roundToInt()}%",
                value = style.endTaperIntensity.coerceIn(0f, 1f),
                range = 0f..1f,
                tag = "stroke_end_taper_intensity_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(endTaperIntensity = v) } },
                onFinished = onEndTransaction
            )
            LabeledStrokeSlider(
                label = "Start Roundness",
                valueText = "${(style.startRoundness * 100).roundToInt()}%",
                value = style.startRoundness.coerceIn(0f, 1f),
                range = 0f..1f,
                tag = "stroke_start_roundness_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(startRoundness = v) } },
                onFinished = onEndTransaction
            )
            LabeledStrokeSlider(
                label = "End Roundness",
                valueText = "${(style.endRoundness * 100).roundToInt()}%",
                value = style.endRoundness.coerceIn(0f, 1f),
                range = 0f..1f,
                tag = "stroke_end_roundness_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(endRoundness = v) } },
                onFinished = onEndTransaction
            )
        }

        // 3. Cap & Join
        CollapsibleStrokeSection(
            section = StrokeControlSection.CAP_AND_JOIN,
            expanded = StrokeControlSection.CAP_AND_JOIN == expandedSection,
            summary = "${style.lineCap.label} · ${style.lineJoin.label}",
            onToggle = { toggleSection(StrokeControlSection.CAP_AND_JOIN) }
        ) {
            Text("Line Cap", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VectorLineCap.entries.forEach { cap ->
                    FilterChip(
                        selected = style.lineCap == cap,
                        onClick = { onUpdateStyleCommit { it.copy(lineCap = cap) } },
                        label = { Text(cap.label) },
                        modifier = Modifier.testTag("stroke_cap_${cap.name.lowercase()}")
                    )
                }
            }
            Text("Line Join", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VectorLineJoin.entries.forEach { join ->
                    FilterChip(
                        selected = style.lineJoin == join,
                        onClick = { onUpdateStyleCommit { it.copy(lineJoin = join) } },
                        label = { Text(join.label) },
                        modifier = Modifier.testTag("stroke_join_${join.name.lowercase()}")
                    )
                }
            }
        }

        // 4. Stroke Style
        CollapsibleStrokeSection(
            section = StrokeControlSection.STROKE_STYLE,
            expanded = StrokeControlSection.STROKE_STYLE == expandedSection,
            summary = style.pattern.label,
            onToggle = { toggleSection(StrokeControlSection.STROKE_STYLE) }
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VectorStrokePattern.entries.forEach { pat ->
                    FilterChip(
                        selected = style.pattern == pat,
                        onClick = { onUpdateStyleCommit { it.copy(pattern = pat) } },
                        label = { Text(pat.label) },
                        modifier = Modifier.testTag("stroke_pattern_${pat.name.lowercase()}")
                    )
                }
            }
            if (style.pattern == VectorStrokePattern.DASHED || style.pattern == VectorStrokePattern.DASH_DOT) {
                LabeledStrokeSlider(
                    label = "Dash Length",
                    valueText = "${(style.dashLengthNorm * 1000).roundToInt()}",
                    value = style.dashLengthNorm.coerceIn(0.005f, 0.14f),
                    range = 0.005f..0.14f,
                    tag = "stroke_dash_length_slider",
                    onValueChange = { v -> onUpdateStyleTransient { it.copy(dashLengthNorm = v) } },
                    onFinished = onEndTransaction
                )
                LabeledStrokeSlider(
                    label = "Dash Spacing",
                    valueText = "${(style.dashSpacingNorm * 1000).roundToInt()}",
                    value = style.dashSpacingNorm.coerceIn(0.004f, 0.14f),
                    range = 0.004f..0.14f,
                    tag = "stroke_dash_spacing_slider",
                    onValueChange = { v -> onUpdateStyleTransient { it.copy(dashSpacingNorm = v) } },
                    onFinished = onEndTransaction
                )
            }
            if (style.pattern == VectorStrokePattern.DOTTED || style.pattern == VectorStrokePattern.DASH_DOT) {
                LabeledStrokeSlider(
                    label = "Dot Spacing",
                    valueText = "${(style.dotSpacingNorm * 1000).roundToInt()}",
                    value = style.dotSpacingNorm.coerceIn(0.004f, 0.12f),
                    range = 0.004f..0.12f,
                    tag = "stroke_dot_spacing_slider",
                    onValueChange = { v -> onUpdateStyleTransient { it.copy(dotSpacingNorm = v) } },
                    onFinished = onEndTransaction
                )
            }
            LabeledStrokeSlider(
                label = "Stroke Spacing",
                valueText = "${(style.strokeSpacingNorm * 1000).roundToInt()}",
                value = style.strokeSpacingNorm.coerceIn(0f, 0.06f),
                range = 0f..0.06f,
                tag = "stroke_extra_spacing_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(strokeSpacingNorm = v) } },
                onFinished = onEndTransaction
            )
        }

        // 5. Opacity (Independent from Layer Opacity)
        CollapsibleStrokeSection(
            section = StrokeControlSection.OPACITY,
            expanded = StrokeControlSection.OPACITY == expandedSection,
            summary = "${(style.strokeOpacity * 100).roundToInt()}%",
            onToggle = { toggleSection(StrokeControlSection.OPACITY) }
        ) {
            LabeledStrokeSlider(
                label = "Stroke Opacity",
                valueText = "${(style.strokeOpacity * 100).roundToInt()}%",
                value = style.strokeOpacity.coerceIn(0f, 1f),
                range = 0f..1f,
                tag = "stroke_opacity_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(strokeOpacity = v) } },
                onFinished = onEndTransaction
            )
        }

        // 6. Smoothing
        CollapsibleStrokeSection(
            section = StrokeControlSection.SMOOTHING,
            expanded = StrokeControlSection.SMOOTHING == expandedSection,
            summary = "${(style.smoothing * 100).roundToInt()}%",
            onToggle = { toggleSection(StrokeControlSection.SMOOTHING) }
        ) {
            LabeledStrokeSlider(
                label = "Smoothing Intensity",
                valueText = "${(style.smoothing * 100).roundToInt()}%",
                value = style.smoothing.coerceIn(0f, 1f),
                range = 0f..1f,
                tag = "stroke_smoothing_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(smoothing = v) } },
                onFinished = onEndTransaction
            )
        }

        // 7. Width Profile & Individual Width Points
        CollapsibleStrokeSection(
            section = StrokeControlSection.WIDTH_PROFILE,
            expanded = StrokeControlSection.WIDTH_PROFILE == expandedSection,
            summary = if (style.variableWidthEnabled || style.widthPoints.isNotEmpty()) "Variable" else "Uniform",
            onToggle = { toggleSection(StrokeControlSection.WIDTH_PROFILE) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Variable Width", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = style.variableWidthEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateStyleCommit { it.copy(variableWidthEnabled = enabled) }
                    },
                    modifier = Modifier.testTag("stroke_variable_width_switch")
                )
            }

            InteractiveWidthProfileGraph(
                profile = style.widthProfile.ifEmpty { VectorStrokeStyle.defaultWidthProfile() },
                onDragPoint = { idx, pos, mult ->
                    onUpdateStyleTransient { cur ->
                        val base = cur.widthProfile.ifEmpty { VectorStrokeStyle.defaultWidthProfile() }.toMutableList()
                        if (idx in base.indices) {
                            val clampedPos = when (idx) {
                                0 -> 0f
                                base.lastIndex -> 1f
                                else -> pos.coerceIn(0.05f, 0.95f)
                            }
                            base[idx] = base[idx].copy(
                                positionNorm = clampedPos,
                                widthMultiplier = mult.coerceIn(0.1f, 2.5f)
                            )
                        }
                        cur.copy(variableWidthEnabled = true, widthProfile = base.sortedBy { it.positionNorm })
                    }
                },
                onDragFinished = onEndTransaction
            )

            // Individual Width Points along path
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Path Width Points (${style.widthPoints.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = {
                        onUpdateStyleCommit { cur ->
                            val existing = cur.widthPoints
                            val nextPos = when (existing.size) {
                                0 -> 0.5f
                                1 -> 0.25f
                                2 -> 0.75f
                                else -> ((existing.size + 1) * 0.19f) % 0.85f + 0.08f
                            }
                            val created = WidthProfilePoint(
                                positionNorm = nextPos,
                                widthMultiplier = 1.6f,
                                transition = WidthPointTransition.SMOOTH
                            )
                            selectedWidthPointId = created.id
                            cur.copy(widthPoints = existing + created)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("add_width_point_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Width Point", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Point")
                }
            }

            style.widthPoints.forEachIndexed { idx, wp ->
                val isSelected = selectedWidthPointId == wp.id || (selectedWidthPointId == null && idx == 0)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) StudioAzure.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedWidthPointId = wp.id }
                        .padding(vertical = 2.dp)
                        .testTag("width_point_item_$idx")
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Point ${idx + 1}: ${(wp.positionNorm * 100).roundToInt()}% · ${(wp.widthMultiplier * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = wp.transition == WidthPointTransition.SMOOTH,
                                    onClick = {
                                        val nextTrans = if (wp.transition == WidthPointTransition.SMOOTH) {
                                            WidthPointTransition.SHARP
                                        } else {
                                            WidthPointTransition.SMOOTH
                                        }
                                        onUpdateStyleCommit { cur ->
                                            cur.copy(widthPoints = cur.widthPoints.map { if (it.id == wp.id) it.copy(transition = nextTrans) else it })
                                        }
                                    },
                                    label = { Text(wp.transition.label) }
                                )
                                IconButton(
                                    onClick = {
                                        onUpdateStyleCommit { cur ->
                                            cur.copy(widthPoints = cur.widthPoints.filterNot { it.id == wp.id })
                                        }
                                    },
                                    modifier = Modifier.testTag("remove_width_point_$idx")
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove Width Point", tint = StudioCoral, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (isSelected) {
                            LabeledStrokeSlider(
                                label = "Position",
                                valueText = "${(wp.positionNorm * 100).roundToInt()}%",
                                value = wp.positionNorm.coerceIn(0f, 1f),
                                range = 0f..1f,
                                tag = "width_point_pos_slider_$idx",
                                onValueChange = { p ->
                                    onUpdateStyleTransient { cur ->
                                        cur.copy(widthPoints = cur.widthPoints.map { if (it.id == wp.id) it.copy(positionNorm = p) else it })
                                    }
                                },
                                onFinished = onEndTransaction
                            )
                            LabeledStrokeSlider(
                                label = "Width Multiplier",
                                valueText = "${(wp.widthMultiplier * 100).roundToInt()}%",
                                value = wp.widthMultiplier.coerceIn(0.1f, 2.5f),
                                range = 0.1f..2.5f,
                                tag = "width_point_mult_slider_$idx",
                                onValueChange = { m ->
                                    onUpdateStyleTransient { cur ->
                                        cur.copy(widthPoints = cur.widthPoints.map { if (it.id == wp.id) it.copy(widthMultiplier = m) else it })
                                    }
                                },
                                onFinished = onEndTransaction
                            )
                        }
                    }
                }
            }
        }

        // 8. Pressure Sensitivity
        CollapsibleStrokeSection(
            section = StrokeControlSection.PRESSURE,
            expanded = StrokeControlSection.PRESSURE == expandedSection,
            summary = if (style.pressureEnabled) "${(style.pressureSensitivity * 100).roundToInt()}%" else "Off",
            onToggle = { toggleSection(StrokeControlSection.PRESSURE) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pressure Sensitivity", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = style.pressureEnabled,
                    onCheckedChange = { on -> onUpdateStyleCommit { it.copy(pressureEnabled = on) } },
                    modifier = Modifier.testTag("stroke_pressure_switch")
                )
            }
            if (style.pressureEnabled) {
                LabeledStrokeSlider(
                    label = "Sensitivity",
                    valueText = "${(style.pressureSensitivity * 100).roundToInt()}%",
                    value = style.pressureSensitivity.coerceIn(0f, 1f),
                    range = 0f..1f,
                    tag = "stroke_pressure_slider",
                    onValueChange = { v -> onUpdateStyleTransient { it.copy(pressureSensitivity = v) } },
                    onFinished = onEndTransaction
                )
            }
        }

        // 9. Advanced Controls
        CollapsibleStrokeSection(
            section = StrokeControlSection.ADVANCED,
            expanded = StrokeControlSection.ADVANCED == expandedSection,
            summary = style.strokeAlignment.label,
            onToggle = { toggleSection(StrokeControlSection.ADVANCED) }
        ) {
            LabeledStrokeSlider(
                label = "Miter Limit",
                valueText = String.format("%.1f", style.miterLimit),
                value = style.miterLimit.coerceIn(1f, 20f),
                range = 1f..20f,
                tag = "stroke_miter_limit_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(miterLimit = v) } },
                onFinished = onEndTransaction
            )
            LabeledStrokeSlider(
                label = "Stroke Offset",
                valueText = "${(style.strokeOffsetNorm * 1000).roundToInt()}",
                value = style.strokeOffsetNorm.coerceIn(-0.05f, 0.05f),
                range = -0.05f..0.05f,
                tag = "stroke_offset_slider",
                onValueChange = { v -> onUpdateStyleTransient { it.copy(strokeOffsetNorm = v) } },
                onFinished = onEndTransaction
            )
            Text("Stroke Alignment", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VectorStrokeAlignment.entries.forEach { align ->
                    FilterChip(
                        selected = style.strokeAlignment == align,
                        onClick = { onUpdateStyleCommit { it.copy(strokeAlignment = align) } },
                        label = { Text(align.label) },
                        modifier = Modifier.testTag("stroke_alignment_${align.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleStrokeSection(
    section: StrokeControlSection,
    expanded: Boolean,
    summary: String,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("stroke_section_${section.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioAzure
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse ${section.title}" else "Expand ${section.title}",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    content()
                }
            }
        }
    }
}

@Composable
private fun LabeledStrokeSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(valueText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            onValueChangeFinished = onFinished,
            valueRange = range,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun InteractiveWidthProfileGraph(
    profile: List<WidthProfilePoint>,
    onDragPoint: (index: Int, posNorm: Float, widthMult: Float) -> Unit,
    onDragFinished: () -> Unit
) {
    val liveProfile by rememberUpdatedState(profile)
    var activeIdx by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF151923))
            .border(1.dp, StudioAzure.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val pts = liveProfile
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val pad = 16f
                    val tapT = ((pos.x - pad) / (w - 2f * pad)).coerceIn(0f, 1f)
                    val hitIdx = pts.indexOfFirst { pt ->
                        val px = pad + pt.positionNorm * (w - 2f * pad)
                        abs(pos.x - px) < 28f
                    }
                    if (hitIdx >= 0) {
                        val mult = (1f - (pos.y / h)) * 2.5f
                        onDragPoint(hitIdx, tapT, mult)
                        onDragFinished()
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startPos ->
                        val pts = liveProfile
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val pad = 16f
                        activeIdx = pts.indices.minByOrNull { idx ->
                            val px = pad + pts[idx].positionNorm * (w - 2f * pad)
                            abs(startPos.x - px)
                        }
                    },
                    onDragEnd = {
                        activeIdx = null
                        onDragFinished()
                    },
                    onDragCancel = {
                        activeIdx = null
                        onDragFinished()
                    }
                ) { change, _ ->
                    change.consume()
                    val idx = activeIdx ?: return@detectDragGestures
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val pad = 16f
                    val nextT = ((change.position.x - pad) / (w - 2f * pad)).coerceIn(0f, 1f)
                    val nextMult = ((1f - (change.position.y / h)) * 2.5f).coerceIn(0.1f, 2.5f)
                    onDragPoint(idx, nextT, nextMult)
                }
            }
            .testTag("width_profile_graph_editor")
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(92.dp)) {
            val pad = 16f
            val innerW = (size.width - 2f * pad).coerceAtLeast(1f)
            val cy = size.height * 0.5f
            val maxHalfH = size.height * 0.42f

            // Center reference line
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(pad, cy),
                end = Offset(size.width - pad, cy),
                strokeWidth = 1.dp.toPx()
            )

            // Envelope fill preview
            val envelopePath = Path()
            val samples = 40
            for (i in 0..samples) {
                val t = i.toFloat() / samples
                val mult = VectorStrokeEngine.evaluateWidthCurve(t, liveProfile)
                val half = (mult / 2.5f) * maxHalfH
                val px = pad + t * innerW
                if (i == 0) envelopePath.moveTo(px, cy - half) else envelopePath.lineTo(px, cy - half)
            }
            for (i in samples downTo 0) {
                val t = i.toFloat() / samples
                val mult = VectorStrokeEngine.evaluateWidthCurve(t, liveProfile)
                val half = (mult / 2.5f) * maxHalfH
                val px = pad + t * innerW
                envelopePath.lineTo(px, cy + half)
            }
            envelopePath.close()
            drawPath(envelopePath, color = StudioAzure.copy(alpha = 0.30f))
            drawPath(envelopePath, color = StudioAzure, style = Stroke(width = 1.5.dp.toPx()))

            // Draggable profile control nodes
            liveProfile.forEachIndexed { idx, pt ->
                val px = pad + pt.positionNorm * innerW
                val py = size.height * (1f - (pt.widthMultiplier / 2.5f).coerceIn(0.05f, 0.95f))
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(px, py))
                drawCircle(color = if (activeIdx == idx) StudioAmber else StudioAzure, radius = 4.dp.toPx(), center = Offset(px, py))
            }
        }
    }
}
