package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fonts.FontMetadata
import com.example.model.EditorLayer
import com.example.model.LayerShapeType
import com.example.model.TextAlignOption
import com.example.model.TextLayer
import com.example.model.VideoLayer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioTeal
import com.example.video.VideoExportState
import kotlin.math.roundToInt

private val TEXT_COLORS = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFF4D4F, 0xFFFFB020,
    0xFF22C55E, 0xFF2D8CFF, 0xFF00D4B4, 0xFF9333EA
)

private val QUICK_QUOTES = listOf(
    "Create Without Limits.",
    "Stay Hungry. Stay Bold.",
    "Design Is Intelligence Made Visible.",
    "😀 🚀 🌈 🎨 ✨"
)

@Composable
fun TextEditorPanel(
    selectedTextLayer: TextLayer?,
    activeFont: FontMetadata?,
    onAddTextLayer: (String) -> Unit,
    onOpenFontLibrary: () -> Unit,
    onUpdateTextLayer: ((TextLayer) -> TextLayer) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAddTextLayer("New Text") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_text_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Text")
                }
                FilledTonalButton(
                    onClick = onOpenFontLibrary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_panel_font_button")
                ) {
                    Icon(Icons.Rounded.FontDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(activeFont?.displayName?.take(12) ?: "Font")
                }
            }

            if (selectedTextLayer == null) {
                Text(
                    text = "Quick Quotes & Emoji Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QUICK_QUOTES.forEach { quote ->
                        FilledTonalButton(onClick = { onAddTextLayer(quote) }) {
                            Icon(Icons.Rounded.FormatQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(quote, maxLines = 1)
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = selectedTextLayer.text,
                    onValueChange = { txt -> onUpdateTextLayer { it.copy(text = txt) } },
                    label = { Text("Edit Text / Emoji") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_layer_editor_input")
                )

                // Font Size + Opacity
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Size: ${selectedTextLayer.fontSizeSp.roundToInt()}sp", style = MaterialTheme.typography.labelLarge)
                    Text("Opacity: ${(selectedTextLayer.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                }
                Slider(
                    value = selectedTextLayer.fontSizeSp,
                    onValueChange = { sz -> onUpdateTextLayer { it.copy(fontSizeSp = sz) } },
                    valueRange = 14f..140f
                )

                // Style toggles + Align
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedTextLayer.isBold,
                        onClick = { onUpdateTextLayer { it.copy(isBold = !it.isBold) } },
                        label = { Icon(Icons.Rounded.FormatBold, contentDescription = "Bold") }
                    )
                    FilterChip(
                        selected = selectedTextLayer.isItalic,
                        onClick = { onUpdateTextLayer { it.copy(isItalic = !it.isItalic) } },
                        label = { Icon(Icons.Rounded.FormatItalic, contentDescription = "Italic") }
                    )
                    FilterChip(
                        selected = selectedTextLayer.shadowEnabled,
                        onClick = { onUpdateTextLayer { it.copy(shadowEnabled = !it.shadowEnabled) } },
                        label = { Text("Shadow") }
                    )
                    FilterChip(
                        selected = selectedTextLayer.strokeWidth > 0f,
                        onClick = {
                            onUpdateTextLayer {
                                it.copy(strokeWidth = if (it.strokeWidth > 0f) 0f else 4f)
                            }
                        },
                        label = { Text("Stroke") }
                    )
                    FilterChip(
                        selected = selectedTextLayer.bgPillEnabled,
                        onClick = { onUpdateTextLayer { it.copy(bgPillEnabled = !it.bgPillEnabled) } },
                        label = { Text("Pill BG") }
                    )
                    TextAlignOption.entries.forEach { alignOpt ->
                        FilterChip(
                            selected = selectedTextLayer.align == alignOpt,
                            onClick = { onUpdateTextLayer { it.copy(align = alignOpt) } },
                            label = { Text(alignOpt.label) }
                        )
                    }
                }

                // Color Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TEXT_COLORS.forEach { argb ->
                        val selected = selectedTextLayer.colorArgb == argb
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

@Composable
fun MediaAndVideoPanel(
    selectedVideoLayer: VideoLayer?,
    isVideoPlaying: Boolean,
    videoExportState: VideoExportState,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onToggleVideoPlay: () -> Unit,
    onAddShape: (LayerShapeType) -> Unit,
    onUpdateVideoLayer: ((VideoLayer) -> VideoLayer) -> Unit,
    onExportVideo: () -> Unit,
    onCancelVideoExport: () -> Unit,
    onOpenAiImage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPickImage,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("media_pick_image_button")
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pick Image")
                }
                Button(
                    onClick = onPickVideo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("media_pick_video_button")
                ) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pick Video")
                }
            }

            if (selectedVideoLayer != null) {
                val dur = selectedVideoLayer.durationMs.coerceAtLeast(500L).toFloat()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Trim: ${selectedVideoLayer.trimStartMs / 1000f}s – ${selectedVideoLayer.trimEndMs / 1000f}s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
                        IconButton(onClick = onToggleVideoPlay) {
                            Icon(
                                if (isVideoPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play or Pause Video"
                            )
                        }
                        IconButton(onClick = { onUpdateVideoLayer { it.copy(isMuted = !it.isMuted) } }) {
                            Icon(
                                if (selectedVideoLayer.isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                                contentDescription = "Mute Video"
                            )
                        }
                        IconButton(onClick = { onUpdateVideoLayer { it.copy(rotationDeg = (it.rotationDeg + 90f) % 360f) } }) {
                            Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate 90°")
                        }
                    }
                }

                RangeSlider(
                    value = selectedVideoLayer.trimStartMs.toFloat()..selectedVideoLayer.trimEndMs.toFloat(),
                    onValueChange = { range ->
                        val s = range.start.toLong().coerceAtLeast(0L)
                        val e = range.endInclusive.toLong().coerceAtLeast(s + 300L)
                        onUpdateVideoLayer { it.copy(trimStartMs = s, trimEndMs = e) }
                    },
                    valueRange = 0f..dur
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Speed:", style = MaterialTheme.typography.labelLarge)
                    listOf(0.5f, 1f, 1.5f, 2f).forEach { spd ->
                        FilterChip(
                            selected = kotlin.math.abs(selectedVideoLayer.speed - spd) < 0.05f,
                            onClick = { onUpdateVideoLayer { it.copy(speed = spd) } },
                            label = { Text("${spd}x") }
                        )
                    }
                }

                when (videoExportState) {
                    is VideoExportState.Exporting -> {
                        Text("Exporting MP4 (${videoExportState.progressPercent}%)...", style = MaterialTheme.typography.labelLarge)
                        LinearProgressIndicator(
                            progress = { videoExportState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(onClick = onCancelVideoExport, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancel Video Export")
                        }
                    }
                    else -> {
                        FilledTonalButton(
                            onClick = onExportVideo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_mp4_button")
                        ) {
                            Icon(Icons.Rounded.Movie, contentDescription = null, tint = StudioTeal)
                            Spacer(Modifier.width(8.dp))
                            Text("Export Edited Video (MP4)")
                        }
                    }
                }
            } else {
                Text("Shapes & AI Assets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = onOpenAiImage) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = StudioPurple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Image (1K–4K)")
                    }
                    LayerShapeType.entries.forEach { shape ->
                        OutlinedButton(onClick = { onAddShape(shape) }) {
                            Text(shape.label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LayersAndAiPanel(
    layers: List<EditorLayer>,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onMoveLayer: (String, Int) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenAiImage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Layers (${layers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = onOpenAiImage) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudioPurple)
                        Spacer(Modifier.width(4.dp))
                        Text("AI Art")
                    }
                    FilledTonalButton(onClick = onOpenAiChat) {
                        Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudioAzure)
                        Spacer(Modifier.width(4.dp))
                        Text("AI Chat")
                    }
                }
            }

            if (layers.isEmpty()) {
                Text(
                    text = "No layers yet. Use Image, Video, Text, or Shapes to add layers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                layers.reversed().forEach { layer ->
                    val isSelected = layer.id == selectedLayerId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) StudioAzure.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onSelectLayer(layer.id) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = layer.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(onClick = { onMoveLayer(layer.id, 1) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Bring Forward", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onMoveLayer(layer.id, -1) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Send Backward", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDuplicateLayer(layer.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onToggleVisibility(layer.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    if (layer.visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { onToggleLock(layer.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    if (layer.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                    contentDescription = "Toggle Lock",
                                    tint = if (layer.locked) StudioAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteLayer(layer.id) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Layer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
