package com.example.ui.components

import android.graphics.Bitmap
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fonts.FontClassification
import com.example.fonts.FontMetadata
import com.example.fonts.FontRenderEngine
import com.example.model.EditorLayer
import com.example.model.EmojiSourceMode
import com.example.model.ImageAdjustments
import com.example.model.ImageLayer
import com.example.model.LayerShapeType
import com.example.model.ShapeLayer
import com.example.model.TextAlignOption
import com.example.model.TextLayer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioEmerald
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val LAYER_COLORS = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFF4D4F, 0xFFFFB020,
    0xFF22C55E, 0xFF2D8CFF, 0xFF00D4B4, 0xFF9333EA
)

@Composable
fun HorizontalLayerStrip(
    layers: List<EditorLayer>,
    selectedLayerId: String?,
    imageBitmaps: Map<String, Bitmap>,
    onSelectLayer: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("horizontal_layer_strip")
    ) {
        if (layers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Layers",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(layers, key = { it.id }) { layer ->
                    val isSelected = layer.id == selectedLayerId
                    Box(
                        modifier = Modifier
                            .size(width = 62.dp, height = 62.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectLayer(layer.id) }
                            .testTag("layer_thumb_${layer.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        when (layer) {
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
                                    when (layer.shapeType) {
                                        LayerShapeType.RECTANGLE -> drawRect(fill)
                                        LayerShapeType.ROUNDED_RECT -> drawRoundRect(fill, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
                                        LayerShapeType.CIRCLE -> drawCircle(fill)
                                        LayerShapeType.STAR, LayerShapeType.BADGE -> {
                                            val pts = if (layer.shapeType == LayerShapeType.STAR) 5 else 8
                                            val inner = if (layer.shapeType == LayerShapeType.STAR) 0.45f else 0.78f
                                            val path = Path()
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            val outerR = size.minDimension / 2f
                                            val total = pts * 2
                                            for (i in 0 until total) {
                                                val rad = if (i % 2 == 0) outerR else outerR * inner
                                                val ang = Math.toRadians((i * 360.0 / total) - 90.0)
                                                val px = cx + (rad * cos(ang)).toFloat()
                                                val py = cy + (rad * sin(ang)).toFloat()
                                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                            }
                                            path.close()
                                            drawPath(path, color = fill)
                                        }
                                    }
                                }
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
            }
        }
    }
}

@Composable
fun TextToolPanel(
    selectedTextLayer: TextLayer?,
    activeFont: FontMetadata?,
    onTextChangeLive: (String) -> Unit,
    onConfirmText: () -> Unit,
    onDeleteOrCancelText: () -> Unit,
    onAddTextLayer: () -> Unit,
    onOpenFontLibrary: () -> Unit,
    onUpdateTextLayer: ((TextLayer) -> TextLayer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedTextLayer == null) {
            FilledTonalButton(
                onClick = onAddTextLayer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_text_button")
            ) {
                Text("Add Text", fontWeight = FontWeight.Bold)
            }
        } else {
            // Live Text Input + Confirm (✓) + Delete/Clear (✕)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = selectedTextLayer.text,
                    onValueChange = onTextChangeLive,
                    placeholder = { Text("Text") },
                    maxLines = 2,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_layer_editor_input")
                )
                FilledIconButton(
                    onClick = onConfirmText,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = StudioEmerald),
                    modifier = Modifier.testTag("text_confirm_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Confirm", tint = Color.White)
                }
                FilledIconButton(
                    onClick = onDeleteOrCancelText,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = StudioCoral),
                    modifier = Modifier.testTag("text_cancel_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Delete", tint = Color.White)
                }
            }

            // Font button inside Text controls + Style chips
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
                    Text(activeFont?.displayName?.take(12) ?: "Font", maxLines = 1)
                }
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
                    label = { Text("Pill") }
                )
                TextAlignOption.entries.forEach { alignOpt ->
                    FilterChip(
                        selected = selectedTextLayer.align == alignOpt,
                        onClick = { onUpdateTextLayer { it.copy(align = alignOpt) } },
                        label = { Text(alignOpt.label) }
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Size", style = MaterialTheme.typography.labelLarge)
                Text("${selectedTextLayer.fontSizeSp.roundToInt()}sp", style = MaterialTheme.typography.labelLarge)
            }
            Slider(
                value = selectedTextLayer.fontSizeSp,
                onValueChange = { sz -> onUpdateTextLayer { it.copy(fontSizeSp = sz) } },
                valueRange = 14f..140f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LAYER_COLORS.forEach { argb ->
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

private enum class AdjustParam(val label: String, val min: Float, val max: Float) {
    EXPOSURE("Exposure", -1f, 1f),
    CONTRAST("Contrast", -1f, 1f),
    SATURATION("Saturation", -1f, 1f),
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
    onResetAdjustments: () -> Unit
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
                val formatted = if (activeParam == AdjustParam.HUE) {
                    "${currentVal.roundToInt()}°"
                } else {
                    "${(currentVal * 100).roundToInt()}"
                }
                Text(
                    text = "${activeParam.label}: $formatted",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onResetAdjustments) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            Slider(
                value = currentVal,
                onValueChange = { v ->
                    onUpdateAdjustments { prev ->
                        when (activeParam) {
                            AdjustParam.EXPOSURE -> prev.copy(exposure = v)
                            AdjustParam.CONTRAST -> prev.copy(contrast = v)
                            AdjustParam.SATURATION -> prev.copy(saturation = v)
                            AdjustParam.HUE -> prev.copy(hue = v)
                            AdjustParam.VIBRANCE -> prev.copy(vibrance = v)
                            AdjustParam.SHARPNESS -> prev.copy(sharpness = v)
                            AdjustParam.VIGNETTE -> prev.copy(vignette = v)
                            AdjustParam.BRIGHTNESS -> prev.copy(brightness = v)
                            AdjustParam.WARMTH -> prev.copy(warmth = v)
                        }
                    }
                },
                valueRange = activeParam.min..activeParam.max,
                modifier = Modifier.testTag("adjust_slider")
            )
        }
    }
}

@Composable
fun ShapesToolPanel(
    selectedShapeLayer: ShapeLayer?,
    onAddShape: (LayerShapeType) -> Unit,
    onUpdateShape: ((ShapeLayer) -> ShapeLayer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LayerShapeType.entries.forEach { shape ->
                OutlinedButton(onClick = { onAddShape(shape) }) {
                    Text(shape.label)
                }
            }
        }

        if (selectedShapeLayer != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LAYER_COLORS.forEach { argb ->
                    val selected = selectedShapeLayer.fillColorArgb == argb
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
                            .clickable { onUpdateShape { it.copy(fillColorArgb = argb) } }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Stroke", style = MaterialTheme.typography.labelLarge)
                Text("${selectedShapeLayer.strokeWidth.roundToInt()}", style = MaterialTheme.typography.labelLarge)
            }
            Slider(
                value = selectedShapeLayer.strokeWidth,
                onValueChange = { w -> onUpdateShape { it.copy(strokeWidth = w) } },
                valueRange = 0f..16f
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
    onDuplicate: () -> Unit
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
                FilledTonalButton(onClick = { onSetRotation(0f) }) {
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
                valueRange = -180f..180f
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Size", style = MaterialTheme.typography.labelLarge)
                Text("${(selectedLayer.scale * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            Slider(
                value = selectedLayer.scale.coerceIn(0.2f, 4f),
                onValueChange = onSetScale,
                valueRange = 0.2f..4f
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Opacity", style = MaterialTheme.typography.labelLarge)
                Text("${(selectedLayer.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            Slider(
                value = selectedLayer.opacity.coerceIn(0f, 1f),
                onValueChange = onSetOpacity,
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
    val textColorInt = MaterialTheme.colorScheme.onSurface.toArgb()

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
                    items(fontsRecentToOld, key = { it.id }) { font ->
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
                                AndroidView(
                                    factory = { ctx ->
                                        TextView(ctx).apply {
                                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                                            setTextColor(textColorInt)
                                        }
                                    },
                                    update = { tv ->
                                        tv.typeface = renderEngine.getTypeface(font.id)
                                        tv.text = sample
                                        tv.setTextColor(textColorInt)
                                    }
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
