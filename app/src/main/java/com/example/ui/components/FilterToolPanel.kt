package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.EditorLayer
import com.example.model.LayerBlendMode
import com.example.model.LayerEffectStyle
import com.example.model.TextLayer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioEmerald
import kotlin.math.roundToInt

private enum class FilterSubTab(val label: String) {
    BLEND("Blend"),
    SHADOW("Shadow"),
    FILL("Fill")
}

@Composable
fun FilterToolPanel(
    selectedLayer: EditorLayer?,
    isEditingFillMedia: Boolean,
    onSetBlendMode: (LayerBlendMode) -> Unit,
    onUpdateEffectStyleTransient: ((LayerEffectStyle) -> LayerEffectStyle) -> Unit,
    onSetOpacityTransient: (Float) -> Unit,
    onSetFillColor: (Long?) -> Unit,
    onPickFillMedia: () -> Unit,
    onSetEditingFillMedia: (Boolean) -> Unit,
    onClearFillMedia: () -> Unit,
    onEndTransaction: () -> Unit
) {
    var activeTab by remember { mutableStateOf(FilterSubTab.BLEND) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedLayer == null) {
            Text(
                text = "Select any Photo, Text, or Group layer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            return
        }

        // Sub-tabs: Blend | Shadow | Fill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterSubTab.entries.forEach { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = {
                        activeTab = tab
                        if (tab != FilterSubTab.FILL && isEditingFillMedia) {
                            onSetEditingFillMedia(false)
                        }
                    },
                    label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("filter_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeTab) {
            FilterSubTab.BLEND -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LayerBlendMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedLayer.blendMode == mode,
                            onClick = { onSetBlendMode(mode) },
                            label = {
                                Text(
                                    text = if (mode == LayerBlendMode.NORMAL) "Normal (Default)" else mode.label,
                                    fontWeight = if (selectedLayer.blendMode == mode) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("blend_mode_${mode.name.lowercase()}")
                        )
                    }
                }
            }

            FilterSubTab.SHADOW -> {
                val eff = selectedLayer.effectStyle
                val textLayer = selectedLayer as? TextLayer
                val strokeOn = if (textLayer != null) textLayer.strokeWidth > 0.01f else eff.strokeWidth > 0.01f
                val strokeW = if (textLayer != null) textLayer.strokeWidth else eff.strokeWidth
                val strokeCol = if (textLayer != null) textLayer.strokeColorArgb else eff.strokeColorArgb
                val shadowOn = if (textLayer != null) textLayer.shadowEnabled else eff.shadowEnabled
                val shadowR = if (textLayer != null) textLayer.shadowRadius else eff.shadowRadius
                val shadowCol = if (textLayer != null) textLayer.shadowColorArgb else eff.shadowColorArgb

                // 1. Stroke
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (strokeOn) "Stroke (${strokeW.roundToInt()}px)" else "Stroke",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = strokeOn,
                        onCheckedChange = { checked ->
                            onUpdateEffectStyleTransient { prev ->
                                prev.copy(
                                    strokeWidth = if (checked) (if (prev.strokeWidth <= 0.5f) 4f else prev.strokeWidth) else 0f
                                )
                            }
                            onEndTransaction()
                        },
                        modifier = Modifier.testTag("filter_stroke_toggle")
                    )
                }
                if (strokeOn) {
                    Slider(
                        value = strokeW.coerceIn(1f, 24f),
                        onValueChange = { w ->
                            onUpdateEffectStyleTransient { it.copy(strokeWidth = w) }
                        },
                        onValueChangeFinished = onEndTransaction,
                        valueRange = 1f..24f,
                        modifier = Modifier.testTag("filter_stroke_width_slider")
                    )
                    FilterColorSwatchesRow(
                        selectedArgb = strokeCol,
                        tagPrefix = "filter_stroke_color",
                        onSelectColor = { c ->
                            onUpdateEffectStyleTransient { it.copy(strokeColorArgb = c) }
                            onEndTransaction()
                        }
                    )
                }

                // 2. Shadow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (shadowOn) "Shadow (${shadowR.roundToInt()}px)" else "Shadow",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = shadowOn,
                        onCheckedChange = { checked ->
                            onUpdateEffectStyleTransient { prev ->
                                prev.copy(
                                    shadowEnabled = checked,
                                    shadowRadius = if (checked && prev.shadowRadius <= 0.5f) 10f else prev.shadowRadius
                                )
                            }
                            onEndTransaction()
                        },
                        modifier = Modifier.testTag("filter_shadow_toggle")
                    )
                }
                if (shadowOn) {
                    Slider(
                        value = shadowR.coerceIn(1f, 36f),
                        onValueChange = { r ->
                            onUpdateEffectStyleTransient { it.copy(shadowEnabled = true, shadowRadius = r) }
                        },
                        onValueChangeFinished = onEndTransaction,
                        valueRange = 1f..36f,
                        modifier = Modifier.testTag("filter_shadow_radius_slider")
                    )
                    FilterColorSwatchesRow(
                        selectedArgb = shadowCol,
                        tagPrefix = "filter_shadow_color",
                        onSelectColor = { c ->
                            onUpdateEffectStyleTransient { it.copy(shadowEnabled = true, shadowColorArgb = c) }
                            onEndTransaction()
                        }
                    )
                }

                // 3. Opacity (moved cleanly from Adjust & Rotate into Filter -> Shadow)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Opacity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text("${(selectedLayer.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                }
                Slider(
                    value = selectedLayer.opacity.coerceIn(0f, 1f),
                    onValueChange = onSetOpacityTransient,
                    onValueChangeFinished = onEndTransaction,
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("filter_opacity_slider")
                )
            }

            FilterSubTab.FILL -> {
                val fill = selectedLayer.fillStyle
                Text(
                    text = "Fill Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onSetFillColor(null) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("filter_fill_color_clear")
                    ) {
                        Text("None")
                    }
                    LAYER_COLORS.forEachIndexed { idx, argb ->
                        val selected = fill.fillColorArgb == argb
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
                                .clickable { onSetFillColor(argb) }
                                .testTag("filter_fill_color_$idx")
                        )
                    }
                }

                Text(
                    text = "Fill Media (Clips Inside Layer)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPickFillMedia,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAzure),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("filter_fill_pick_media")
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (fill.fillMediaUri != null) "Replace Media" else "Pick Media")
                    }

                    if (fill.fillMediaUri != null) {
                        FilterChip(
                            selected = isEditingFillMedia,
                            onClick = { onSetEditingFillMedia(!isEditingFillMedia) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    text = if (isEditingFillMedia) "Done Media Edit" else "Adjust Inside",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            modifier = Modifier.testTag("filter_fill_edit_media_toggle")
                        )

                        OutlinedButton(
                            onClick = onClearFillMedia,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_fill_media_clear")
                        ) {
                            Icon(Icons.Rounded.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                }

                if (fill.fillMediaUri != null) {
                    Text(
                        text = if (isEditingFillMedia) {
                            "Drag, pinch-zoom, or rotate inside canvas to move only the fill photo."
                        } else {
                            "Tap \"Adjust Inside\" to reposition/zoom the fill photo without moving the layer."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEditingFillMedia) StudioAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterColorSwatchesRow(
    selectedArgb: Long,
    tagPrefix: String,
    onSelectColor: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LAYER_COLORS.forEachIndexed { idx, argb ->
            val selected = selectedArgb == argb
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(argb.toInt()))
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) StudioEmerald else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(argb) }
                    .testTag("${tagPrefix}_$idx")
            )
        }
    }
}
