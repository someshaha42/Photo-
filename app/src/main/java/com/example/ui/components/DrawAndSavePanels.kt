package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.MediaStorageManager
import com.example.model.CanvasDimensions
import com.example.model.ExportImageFormat
import com.example.model.ExportResolutionPreset
import com.example.model.VectorStrokeStyle
import com.example.ui.ExportFeedbackState
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioTeal
import kotlin.math.abs
import kotlin.math.roundToInt

private val DRAW_PALETTE_COLORS = listOf(
    0xFFFF4D4FL,
    0xFFFFFFFFL,
    0xFF000000L,
    0xFFFFB020L,
    0xFF22C55EL,
    0xFF2D8CFFL,
    0xFF00D4B4L,
    0xFF9333EAL
)

private val DRAW_QUICK_SIZES = listOf(0.036f, 0.026f, 0.018f, 0.014f, 0.010f, 0.006f, 0.004f)

@Composable
fun DrawToolPanel(
    brushColorArgb: Long,
    brushSizeNorm: Float,
    strokeStyle: VectorStrokeStyle = VectorStrokeStyle(),
    showStrokeControls: Boolean = false,
    selectedDrawLayerCount: Int = 0,
    canUndoStroke: Boolean,
    onSelectColor: (Long) -> Unit,
    onSetBrushSize: (Float) -> Unit,
    onToggleStrokeControls: () -> Unit = {},
    onStartNewStrokeSession: () -> Unit = {},
    onUpdateStrokeStyleTransient: ((VectorStrokeStyle) -> VectorStrokeStyle) -> Unit = {},
    onUpdateStrokeStyleCommit: ((VectorStrokeStyle) -> VectorStrokeStyle) -> Unit = {},
    onEndTransaction: () -> Unit = {},
    onUndoStroke: () -> Unit,
    onClearDraw: () -> Unit,
    onDone: () -> Unit
) {
    if (showStrokeControls) {
        StrokeControlsPanel(
            style = strokeStyle,
            selectedDrawLayerCount = selectedDrawLayerCount,
            onStartNewStrokeSession = onStartNewStrokeSession,
            onUpdateStyleTransient = onUpdateStrokeStyleTransient,
            onUpdateStyleCommit = onUpdateStrokeStyleCommit,
            onEndTransaction = onEndTransaction,
            onClosePanel = onToggleStrokeControls
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.Brush, contentDescription = null, tint = StudioCoral, modifier = Modifier.size(18.dp))
                Text(
                    text = "Draw",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onToggleStrokeControls,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("draw_stroke_controls_button")
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Stroke Controls", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stroke")
                }
                OutlinedButton(
                    onClick = onUndoStroke,
                    enabled = canUndoStroke,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("draw_undo_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo Stroke", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(
                    onClick = onClearDraw,
                    enabled = canUndoStroke,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("draw_clear_button")
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear Draw", tint = StudioCoral, modifier = Modifier.size(16.dp))
                }
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("draw_done_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Done Drawing", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Clean Brush Color Swatches (with Red selected indicator box matching screenshot)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DRAW_PALETTE_COLORS.forEach { argb ->
                val isSelected = brushColorArgb == argb
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(argb.toInt()))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onSelectColor(argb) }
                        .testTag("draw_color_${argb.toString(16)}")
                )
            }
        }

        // Clean Size Dots Strip (Screenshots 3 & 4 style: large-to-small white dots + selected circle)
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Size", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "${(brushSizeNorm * 1000).roundToInt()}pt · ${strokeStyle.pattern.label}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DRAW_QUICK_SIZES.forEachIndexed { idx, sizeVal ->
                    val isSelected = abs(brushSizeNorm - sizeVal) <= 0.002f
                    val dotDp = (6f + (DRAW_QUICK_SIZES.size - idx) * 2.4f).dp
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(if (isSelected) StudioCoral.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { onSetBrushSize(sizeVal) }
                            .testTag("draw_size_dot_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(dotDp)
                                .clip(CircleShape)
                                .background(if (isSelected) StudioCoral else MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaveExportDialog(
    canvas: CanvasDimensions,
    initialFormat: ExportImageFormat,
    initialPreset: ExportResolutionPreset,
    initialCustomWidth: Int,
    initialCustomHeight: Int,
    exportFeedback: ExportFeedbackState,
    onExport: (format: ExportImageFormat, preset: ExportResolutionPreset, customW: Int, customH: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember(initialFormat) { mutableStateOf(initialFormat) }
    var selectedPreset by remember(initialPreset) { mutableStateOf(initialPreset) }
    var customWText by remember(initialCustomWidth) { mutableStateOf(initialCustomWidth.toString()) }
    var customHText by remember(initialCustomHeight) { mutableStateOf(initialCustomHeight.toString()) }

    val parsedCustomW = (customWText.toIntOrNull() ?: initialCustomWidth).coerceIn(64, 8192)
    val parsedCustomH = (customHText.toIntOrNull() ?: initialCustomHeight).coerceIn(64, 8192)
    val (targetW, targetH) = MediaStorageManager.computeTargetResolution(
        canvas = canvas,
        preset = selectedPreset,
        customWidth = parsedCustomW,
        customHeight = parsedCustomH
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, tint = StudioAzure)
                Text("Save Image", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Format Selection: PNG (Default) | JPG
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Format",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportImageFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = {
                                    Text(
                                        text = if (fmt == ExportImageFormat.PNG) "PNG (Default)" else "JPG",
                                        fontWeight = if (selectedFormat == fmt) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.testTag("export_format_${fmt.name.lowercase()}")
                            )
                        }
                    }
                }

                // Resolution Presets: 4K | 2K | Custom (persistent!)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Resolution",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportResolutionPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = selectedPreset == preset,
                                onClick = { selectedPreset = preset },
                                label = { Text(preset.label) },
                                modifier = Modifier.testTag("export_res_${preset.name.lowercase()}")
                            )
                        }
                    }
                }

                if (selectedPreset == ExportResolutionPreset.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customWText,
                            onValueChange = { s -> customWText = s.filter { it.isDigit() }.take(4) },
                            label = { Text("Width px") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_custom_width_input")
                        )
                        OutlinedTextField(
                            value = customHText,
                            onValueChange = { s -> customHText = s.filter { it.isDigit() }.take(4) },
                            label = { Text("Height px") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_custom_height_input")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Output: ${targetW} × ${targetH} px (${canvas.label}) • ${selectedFormat.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                when (exportFeedback) {
                    ExportFeedbackState.Idle -> Unit
                    ExportFeedbackState.Exporting -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("Saving ${selectedFormat.label}...")
                        }
                    }
                    is ExportFeedbackState.Success -> {
                        Text(exportFeedback.message, color = StudioTeal, fontWeight = FontWeight.SemiBold)
                    }
                    is ExportFeedbackState.Error -> {
                        Text(exportFeedback.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (exportFeedback is ExportFeedbackState.Success) "Close" else "Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(selectedFormat, selectedPreset, parsedCustomW, parsedCustomH) },
                enabled = exportFeedback !is ExportFeedbackState.Exporting,
                colors = ButtonDefaults.buttonColors(containerColor = StudioAzure),
                modifier = Modifier.testTag("export_confirm_save_button")
            ) {
                Text("Save ${selectedFormat.label}", fontWeight = FontWeight.Bold)
            }
        }
    )
}
