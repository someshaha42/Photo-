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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CanvasAspectRatio
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioEmerald

@Composable
fun CanvasSettingsPanel(
    selectedRatio: CanvasAspectRatio,
    autoImageDetectionEnabled: Boolean,
    manualOverride: Boolean,
    onSelectRatio: (CanvasAspectRatio) -> Unit,
    onToggleAutoDetection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Canvas Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = "Canvas Dimensions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "Aspect Ratio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CanvasAspectRatio.entries.forEach { ratio ->
                    val isSelected = ratio == selectedRatio
                    val (boxW, boxH) = when (ratio) {
                        CanvasAspectRatio.RATIO_1_1 -> 30.dp to 30.dp
                        CanvasAspectRatio.RATIO_16_9 -> 34.dp to 22.dp
                        CanvasAspectRatio.RATIO_9_16 -> 20.dp to 32.dp
                        CanvasAspectRatio.RATIO_4_5 -> 25.dp to 31.dp
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) StudioAzure.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectRatio(ratio) }
                            .padding(vertical = 12.dp, horizontal = 6.dp)
                            .testTag("aspect_ratio_${ratio.label}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.height(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = boxW, height = boxH)
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.onSurfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                        Text(
                            text = ratio.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White.takeIf { false } ?: MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSelected) {
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
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected Ratio",
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioEmerald.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = selectedRatio.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = StudioEmerald,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleAutoDetection)
                    .padding(vertical = 4.dp)
                    .testTag("auto_image_detection_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto Image Detection",
                    style = MaterialTheme.typography.bodyLarge
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (autoImageDetectionEnabled && !manualOverride)
                        StudioEmerald.copy(alpha = 0.16f)
                    else
                        StudioAzure.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = when {
                            !autoImageDetectionEnabled -> "Disabled"
                            manualOverride -> "Manual Override"
                            else -> "Enabled"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (autoImageDetectionEnabled && !manualOverride) StudioEmerald else StudioAzure,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Imported image remains unchanged. Canvas adjusts independently.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
