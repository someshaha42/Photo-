package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
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
    onApplyCustom: (ratioInput: String, pixelInput: String, preferPixels: Boolean) -> Boolean
) {
    var showCustomInputs by remember(canvas.isCustom) { mutableStateOf(canvas.isCustom) }
    var ratioText by remember(canvas) { mutableStateOf(canvas.label) }
    var pixelText by remember(canvas) { mutableStateOf(canvas.pixelLabel) }
    var lastEditedPixels by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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

            // ONLY 2 Canvas Options: 1:1 (Default) and Custom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val squareSelected = !showCustomInputs && !canvas.isCustom
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (squareSelected) StudioAzure.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = if (squareSelected) 2.dp else 1.dp,
                            color = if (squareSelected) StudioAzure else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            showCustomInputs = false
                            errorMsg = null
                            onSelectSquare()
                        }
                        .padding(vertical = 14.dp)
                        .testTag("canvas_preset_1_1"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CropSquare,
                        contentDescription = "1:1",
                        tint = if (squareSelected) StudioAzure else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "1:1",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (squareSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Selected",
                            tint = StudioAzure,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                val customSelected = showCustomInputs || canvas.isCustom
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (customSelected) StudioAzure.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = if (customSelected) 2.dp else 1.dp,
                            color = if (customSelected) StudioAzure else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            showCustomInputs = true
                            errorMsg = null
                        }
                        .padding(vertical = 14.dp)
                        .testTag("canvas_preset_custom"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Custom",
                        tint = if (customSelected) StudioAzure else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (customSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Selected",
                            tint = StudioAzure,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
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
