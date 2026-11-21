package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Crop169
import androidx.compose.material.icons.rounded.CropPortrait
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CanvasDimensions
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioEmerald

@Composable
fun CanvasSettingsPanel(
    canvas: CanvasDimensions,
    onSelectSquare: () -> Unit,
    onSelectPreset: (CanvasDimensions) -> Unit = { preset ->
        if (preset == CanvasDimensions.SQUARE_1_1) onSelectSquare()
    },
    onApplyCustom: (ratioInput: String, pixelInput: String, preferPixels: Boolean) -> Boolean
) {
    var showCustomInputs by remember(canvas.isCustom) { mutableStateOf(canvas.isCustom) }
    var ratioText by remember(canvas) { mutableStateOf(canvas.label) }
    var pixelText by remember(canvas) { mutableStateOf(canvas.pixelLabel) }
    var lastEditedPixels by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val squareSelected = !showCustomInputs && !canvas.isCustom &&
        canvas.ratioWidth == 1 && canvas.ratioHeight == 1
    val wideSelected = !showCustomInputs && !canvas.isCustom &&
        canvas.ratioWidth == 16 && canvas.ratioHeight == 9
    val tallSelected = !showCustomInputs && !canvas.isCustom &&
        canvas.ratioWidth == 9 && canvas.ratioHeight == 16
    val customSelected = showCustomInputs || canvas.isCustom

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Canvas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioEmerald.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "${canvas.label} • ${canvas.pixelLabel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = StudioEmerald,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Presets: 1:1 | 16:9 | 9:16 | Custom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CanvasPresetCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.CropSquare,
                    label = "1:1",
                    selected = squareSelected,
                    tag = "canvas_preset_1_1",
                    onClick = {
                        showCustomInputs = false
                        errorMsg = null
                        onSelectPreset(CanvasDimensions.SQUARE_1_1)
                    }
                )
                CanvasPresetCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Crop169,
                    label = "16:9",
                    selected = wideSelected,
                    tag = "canvas_preset_16_9",
                    onClick = {
                        showCustomInputs = false
                        errorMsg = null
                        onSelectPreset(CanvasDimensions.LANDSCAPE_16_9)
                    }
                )
                CanvasPresetCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.CropPortrait,
                    label = "9:16",
                    selected = tallSelected,
                    tag = "canvas_preset_9_16",
                    onClick = {
                        showCustomInputs = false
                        errorMsg = null
                        onSelectPreset(CanvasDimensions.PORTRAIT_9_16)
                    }
                )
                CanvasPresetCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Tune,
                    label = "Custom",
                    selected = customSelected,
                    tag = "canvas_preset_custom",
                    onClick = {
                        showCustomInputs = true
                        errorMsg = null
                    }
                )
            }

            if (showCustomInputs) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ratioText,
                        onValueChange = { input ->
                            ratioText = input
                            lastEditedPixels = false
                            errorMsg = null
                            CanvasDimensions.fromRatioInput(input)?.let { computed ->
                                pixelText = computed.pixelLabel
                            }
                        },
                        label = { Text("Ratio") },
                        placeholder = { Text("9:16") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_ratio_input")
                    )

                    OutlinedTextField(
                        value = pixelText,
                        onValueChange = { input ->
                            pixelText = input
                            lastEditedPixels = true
                            errorMsg = null
                            CanvasDimensions.fromPixelInput(input)?.let { computed ->
                                ratioText = computed.label
                            }
                        },
                        label = { Text("Pixels") },
                        placeholder = { Text("1080x1920") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_pixel_input")
                    )
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        val ok = onApplyCustom(ratioText, pixelText, lastEditedPixels)
                        if (!ok) {
                            errorMsg = "Use e.g. 9:16 or 1080x1920"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAzure),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_custom_canvas_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Apply")
                    Spacer(Modifier.width(6.dp))
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CanvasPresetCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) StudioAzure.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) StudioAzure else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) StudioAzure else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Selected",
                tint = StudioAzure,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

