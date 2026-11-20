package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.BackgroundConfig
import com.example.model.BackgroundTab
import com.example.model.EditorBlendMode
import com.example.ui.theme.CheckerDark1
import com.example.ui.theme.CheckerDark2
import com.example.ui.theme.StudioAzure
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private val PRESET_SWATCHES = listOf(
    0xFF2D7FF9,
    0xFFFF4D4F,
    0xFFFFB020,
    0xFF22C55E,
    0xFF9333EA,
    0xFFFFFFFF,
    0xFF000000
)

@Composable
fun BackgroundSettingsPanel(
    background: BackgroundConfig,
    blurredPreview: Bitmap?,
    selectedBgImageBitmap: Bitmap?,
    isEyedropperActive: Boolean,
    onUpdateBackground: ((BackgroundConfig) -> BackgroundConfig) -> Unit,
    onPickBackgroundImage: () -> Unit,
    onToggleEyedropper: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Background",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = "Background",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BackgroundTab.entries.forEach { tab ->
                    val selected = background.activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) StudioAzure else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onUpdateBackground { it.copy(activeTab = tab) }
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("bg_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            when (background.activeTab) {
                BackgroundTab.TRANSPARENT -> {
                    TransparentBackgroundTab(
                        checkerboardOpacity = background.checkerboardOpacity,
                        onOpacityChange = { op ->
                            onUpdateBackground { it.copy(checkerboardOpacity = op) }
                        }
                    )
                }
                BackgroundTab.BLUR -> {
                    BlurBackgroundTab(
                        blurStrength = background.blurStrength,
                        backgroundOpacity = background.blurBackgroundOpacity,
                        blurredPreview = blurredPreview,
                        onBlurStrengthChange = { b ->
                            onUpdateBackground { it.copy(blurStrength = b) }
                        },
                        onBgOpacityChange = { op ->
                            onUpdateBackground { it.copy(blurBackgroundOpacity = op) }
                        }
                    )
                }
                BackgroundTab.COLORS -> {
                    ColorsBackgroundTab(
                        colorArgb = background.colorArgb,
                        colorOpacity = background.colorOpacity,
                        isEyedropperActive = isEyedropperActive,
                        onColorChange = { argb ->
                            onUpdateBackground { it.copy(colorArgb = argb) }
                        },
                        onOpacityChange = { op ->
                            onUpdateBackground { it.copy(colorOpacity = op) }
                        },
                        onToggleEyedropper = onToggleEyedropper
                    )
                }
                BackgroundTab.IMAGE -> {
                    ImageBackgroundTab(
                        bgImageBitmap = selectedBgImageBitmap,
                        backgroundOpacity = background.backgroundImageOpacity,
                        onPickImage = onPickBackgroundImage,
                        onOpacityChange = { op ->
                            onUpdateBackground { it.copy(backgroundImageOpacity = op) }
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Text(
                text = "Blend Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val rows = EditorBlendMode.entries.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowModes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowModes.forEach { mode ->
                            val selected = background.blendMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.surfaceContainerHigh
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (selected) 1.5.dp else 0.dp,
                                        color = if (selected) StudioAzure else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onUpdateBackground { it.copy(blendMode = mode) }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                                    .testTag("blend_mode_${mode.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransparentBackgroundTab(
    checkerboardOpacity: Float,
    onOpacityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = 12.dp.toPx()
                val cols = (size.width / cell).toInt() + 1
                val rows = (size.height / cell).toInt() + 1
                for (r in 0..rows) {
                    for (c in 0..cols) {
                        val base = if ((r + c) % 2 == 0) CheckerDark1 else CheckerDark2
                        drawRect(
                            color = base.copy(alpha = checkerboardOpacity),
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell, cell)
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Grid Opacity", style = MaterialTheme.typography.bodyMedium)
            Text("${(checkerboardOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = checkerboardOpacity, onValueChange = onOpacityChange, valueRange = 0.1f..1f)
    }
}

@Composable
private fun BlurBackgroundTab(
    blurStrength: Float,
    backgroundOpacity: Float,
    blurredPreview: Bitmap?,
    onBlurStrengthChange: (Float) -> Unit,
    onBgOpacityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF3E4552)),
            contentAlignment = Alignment.Center
        ) {
            if (blurredPreview != null) {
                Image(
                    bitmap = blurredPreview.asImageBitmap(),
                    contentDescription = "Blurred background",
                    contentScale = ContentScale.Crop,
                    alpha = backgroundOpacity,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Blur", style = MaterialTheme.typography.bodyMedium)
            Text("${(blurStrength * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = blurStrength, onValueChange = onBlurStrengthChange)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Opacity", style = MaterialTheme.typography.bodyMedium)
            Text("${(backgroundOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = backgroundOpacity, onValueChange = onBgOpacityChange)
    }
}

@Composable
private fun ColorsBackgroundTab(
    colorArgb: Long,
    colorOpacity: Float,
    isEyedropperActive: Boolean,
    onColorChange: (Long) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onToggleEyedropper: () -> Unit
) {
    val activeColor = Color(colorArgb.toInt()).copy(alpha = colorOpacity)
    val hsv = remember(colorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(colorArgb.toInt(), it) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(activeColor)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        )

        var wheelSize by remember { mutableStateOf(IntSize.Zero) }
        fun updateFromOffset(offset: Offset) {
            if (wheelSize.width == 0 || wheelSize.height == 0) return
            val cx = wheelSize.width / 2f
            val cy = wheelSize.height / 2f
            val maxR = minOf(cx, cy).coerceAtLeast(1f)
            val dx = offset.x - cx
            val dy = offset.y - cy
            val angleDeg = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
            val sat = (hypot(dx.toDouble(), dy.toDouble()).toFloat() / maxR).coerceIn(0f, 1f)
            val newColorInt = AndroidColor.HSVToColor(floatArrayOf(angleDeg, sat, 1f))
            onColorChange(newColorInt.toLong() and 0xFFFFFFFFL)
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(152.dp)
                    .onSizeChanged { wheelSize = it }
                    .pointerInput(Unit) { detectTapGestures { updateFromOffset(it) } }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            updateFromOffset(change.position)
                        }
                    }
            ) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                        center = center
                    ),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
                val rad = Math.toRadians(hsv[0].toDouble())
                val dist = hsv[1].coerceIn(0f, 1f) * (radius - 10.dp.toPx())
                val hx = center.x + (dist * cos(rad)).toFloat()
                val hy = center.y + (dist * sin(rad)).toFloat()
                drawCircle(color = Color(0xFF1A1F2A), radius = 14.dp.toPx(), center = Offset(hx, hy))
                drawCircle(color = Color.White, radius = 14.dp.toPx(), center = Offset(hx, hy), style = Stroke(width = 2.dp.toPx()))
                drawCircle(color = Color(colorArgb.toInt()), radius = 6.dp.toPx(), center = Offset(hx, hy))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PRESET_SWATCHES.forEach { swatch ->
                val isSelected = (swatch and 0xFFFFFF) == (colorArgb and 0xFFFFFF)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(swatch.toInt()))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(swatch) }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Opacity", style = MaterialTheme.typography.bodyMedium)
            Text("${(colorOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = colorOpacity, onValueChange = onOpacityChange)

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isEyedropperActive) StudioAzure.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onToggleEyedropper)
                .testTag("eyedropper_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Colorize, contentDescription = "Eyedropper", tint = StudioAzure)
                Text(
                    text = if (isEyedropperActive) "Eyedropper On" else "Eyedropper",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isEyedropperActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ImageBackgroundTab(
    bgImageBitmap: Bitmap?,
    backgroundOpacity: Float,
    onPickImage: () -> Unit,
    onOpacityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2B313D)),
            contentAlignment = Alignment.Center
        ) {
            if (bgImageBitmap != null) {
                Image(
                    bitmap = bgImageBitmap.asImageBitmap(),
                    contentDescription = "Background Image",
                    contentScale = ContentScale.Crop,
                    alpha = backgroundOpacity,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Button(
            onClick = onPickImage,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bg_pick_image_button"),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF12151C)
            )
        ) {
            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pick Photo", fontWeight = FontWeight.Bold)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Opacity", style = MaterialTheme.typography.bodyMedium)
            Text("${(backgroundOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = backgroundOpacity, onValueChange = onOpacityChange)
    }
}
