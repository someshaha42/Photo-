package com.example.ui.components

import android.graphics.Color as AndroidColor
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fonts.FontClassification
import com.example.fonts.FontMetadata
import com.example.fonts.FontRenderEngine
import com.example.model.EditorLayer
import com.example.model.ImageLayer
import com.example.model.ShapeLayer
import com.example.model.TextLayer
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPurple
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private val EXTENDED_PALETTE_SWATCHES = listOf(
    0xFFFFFFFFL,
    0xFF000000L,
    0xFF64748BL,
    0xFFEF4444L,
    0xFFFF4D4FL,
    0xFFF97316L,
    0xFFFFB020L,
    0xFFFACC15L,
    0xFF84CC16L,
    0xFF22C55EL,
    0xFF10B981L,
    0xFF00D4B4L,
    0xFF06B6D4L,
    0xFF2D8CFFL,
    0xFF3B82F6L,
    0xFF6366F1L,
    0xFF8B5CF6L,
    0xFF9333EAL,
    0xFFD946EFL,
    0xFFEC4899L,
    0xFFF43F5EL,
    0xFFA16207L,
    0xFF78350FL
)

@Composable
fun PhotoImportToolPanel(
    selectedImageLayer: ImageLayer?,
    totalPhotoCount: Int,
    onImportFromGallery: () -> Unit,
    onOpenCrop: () -> Unit,
    onOpenAdjust: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (totalPhotoCount == 0) "Tap Import to pick from gallery" else "$totalPhotoCount photo(s) on canvas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onImportFromGallery,
                colors = ButtonDefaults.buttonColors(containerColor = StudioAzure),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                modifier = Modifier.testTag("photo_panel_import_button")
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import", fontWeight = FontWeight.Bold)
            }
        }

        if (selectedImageLayer != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenCrop,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Crop")
                }
                OutlinedButton(
                    onClick = onOpenAdjust,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Adjust")
                }
            }
        }
    }
}

@Composable
fun FontBrowserToolPanel(
    selectedTextLayer: TextLayer?,
    fonts: List<FontMetadata>,
    fontsRecentToOld: List<FontMetadata>,
    selectedFontId: String,
    renderEngine: FontRenderEngine,
    onSelectFont: (String) -> Unit,
    onImportFontsClick: () -> Unit,
    onImportArchiveClick: () -> Unit,
    onOpenFullFontLibrary: () -> Unit
) {
    val activeFontId = selectedTextLayer?.fontId ?: selectedFontId
    val activeFont = fonts.firstOrNull { it.id == activeFontId }
    var searchQuery by remember { mutableStateOf("") }
    val textColorInt = MaterialTheme.colorScheme.onSurface.toArgb()

    val filteredFonts = remember(fontsRecentToOld, searchQuery) {
        if (searchQuery.isBlank()) fontsRecentToOld
        else fontsRecentToOld.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.fileName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active font summary + Total Fonts + Import actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Selected: ${activeFont?.displayName ?: "Roboto"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = StudioAzure,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Total Fonts: ${fonts.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = onImportFontsClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("editor_font_import_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Import Font", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import", maxLines = 1)
                }
                IconButton(
                    onClick = onImportArchiveClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("editor_font_import_zip_button")
                ) {
                    Icon(Icons.Rounded.Archive, contentDescription = "Import ZIP", tint = StudioPurple, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onOpenFullFontLibrary,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("editor_font_library_expand_button")
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Full Font Library", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Quick Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search ${fonts.size} fonts...") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("editor_font_search_input")
        )

        // Horizontal quick-pick cards showing real font glyphs & display names right above keyboard/bar
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(filteredFonts, key = { it.id }) { font ->
                val isSelected = font.id == activeFontId
                val samplePreview = when {
                    !selectedTextLayer?.text.isNullOrBlank() -> selectedTextLayer!!.text.take(14)
                    font.classification == FontClassification.COLOR_EMOJI -> "😀🚀🌈"
                    else -> "Abc 123"
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) StudioAzure.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .width(126.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectFont(font.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("editor_font_item_${font.fileName}")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = font.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = StudioAzure,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        AndroidView(
                            factory = { ctx ->
                                TextView(ctx).apply {
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                                    setTextColor(textColorInt)
                                    maxLines = 1
                                }
                            },
                            update = { tv ->
                                tv.typeface = renderEngine.getTypeface(font.id)
                                tv.text = samplePreview
                                tv.setTextColor(textColorInt)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorToolPanel(
    selectedLayer: EditorLayer?,
    onSetColorTransient: (Long?) -> Unit,
    onCommitColor: (Long?) -> Unit,
    onLaunchFloatingEyedropper: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedLayer == null) {
            Text(
                text = "Select a Photo, Text, or Shape",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            return
        }

        val currentLayerColor: Long? = when (selectedLayer) {
            is TextLayer -> selectedLayer.colorArgb
            is ImageLayer -> selectedLayer.tintColorArgb
            is ShapeLayer -> selectedLayer.fillColorArgb
            is com.example.model.GroupLayer -> null
        }
        val isDefaultState = when (selectedLayer) {
            is ImageLayer -> selectedLayer.tintColorArgb == null
            is TextLayer -> selectedLayer.colorArgb == 0xFFFFFFFFL
            is ShapeLayer -> selectedLayer.fillColorArgb == 0xFF2D8CFFL
            is com.example.model.GroupLayer -> true
        }

        val effectiveSeedColorInt = (currentLayerColor ?: 0xFFFF4D4FL).toInt()
        var showColorWheel by remember { mutableStateOf(false) }

        val initialHsv = remember { FloatArray(3) }
        AndroidColor.colorToHSV(effectiveSeedColorInt, initialHsv)

        var hue by remember(selectedLayer.id) { mutableFloatStateOf(initialHsv[0]) }
        var sat by remember(selectedLayer.id) { mutableFloatStateOf(initialHsv[1].coerceAtLeast(0.01f)) }
        var valueBrightness by remember(selectedLayer.id) { mutableFloatStateOf(initialHsv[2].coerceAtLeast(0.15f)) }
        var alphaFraction by remember(selectedLayer.id) {
            mutableFloatStateOf(
                if (selectedLayer is ImageLayer && selectedLayer.tintColorArgb == null) 0.65f
                else (AndroidColor.alpha(effectiveSeedColorInt) / 255f).coerceIn(0f, 1f)
            )
        }

        LaunchedEffect(currentLayerColor) {
            if (currentLayerColor != null) {
                val cInt = currentLayerColor.toInt()
                val hsv = FloatArray(3)
                AndroidColor.colorToHSV(cInt, hsv)
                hue = hsv[0]
                sat = hsv[1]
                valueBrightness = hsv[2]
                alphaFraction = (AndroidColor.alpha(cInt) / 255f).coerceIn(0f, 1f)
            }
        }

        fun composeArgb(h: Float, s: Float, v: Float, aFrac: Float): Long {
            val alphaInt = (aFrac.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
            val rgbInt = AndroidColor.HSVToColor(alphaInt, floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))
            return rgbInt.toLong() and 0xFFFFFFFFL
        }

        // Row 1: Default + Color Wheel Icon + Floating Pencil Eyedropper + Curated Color Palette
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Default Reset Swatch (always stays first!)
            item {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isDefaultState) StudioAzure.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .border(
                            width = if (isDefaultState) 2.dp else 1.dp,
                            color = if (isDefaultState) StudioAzure else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onCommitColor(null) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("color_default_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Default Color", modifier = Modifier.size(14.dp))
                        Text("Default", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Color Wheel Palette Button (opens interactive circular color wheel)
            item {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                            )
                        )
                        .border(
                            width = if (showColorWheel) 2.5.dp else 1.5.dp,
                            color = if (showColorWheel) StudioAzure else Color.White,
                            shape = CircleShape
                        )
                        .clickable { showColorWheel = !showColorWheel }
                        .testTag("color_wheel_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = "Color Wheel",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 3. Floating Screen-Wide Pencil Eyedropper Button
            item {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.5.dp, StudioAzure, CircleShape)
                        .clickable(onClick = onLaunchFloatingEyedropper)
                        .testTag("color_floating_eyedropper_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Colorize,
                        contentDescription = "Pick Color from Screen",
                        tint = StudioAzure,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 4. Direct Color Swatches
            items(EXTENDED_PALETTE_SWATCHES) { baseArgb ->
                val baseRgb = baseArgb.toInt()
                val currentAlphaInt = (alphaFraction.coerceAtLeast(if (isDefaultState && selectedLayer is ImageLayer) 0.65f else alphaFraction) * 255f)
                    .roundToInt().coerceIn(0, 255)
                val appliedArgb = ((currentAlphaInt.toLong() and 0xFFL) shl 24) or (baseArgb and 0x00FFFFFFL)
                val isSelected = currentLayerColor != null && ((currentLayerColor and 0x00FFFFFFL) == (baseArgb and 0x00FFFFFFL))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(baseRgb))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable {
                            val hsv = FloatArray(3)
                            AndroidColor.colorToHSV(baseRgb, hsv)
                            hue = hsv[0]
                            sat = hsv[1]
                            valueBrightness = hsv[2]
                            if (selectedLayer is ImageLayer && isDefaultState) {
                                alphaFraction = 0.65f
                            } else if (alphaFraction <= 0.02f) {
                                alphaFraction = 1f
                            }
                            onCommitColor(
                                ((alphaFraction.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255).toLong() shl 24) or
                                    (baseArgb and 0x00FFFFFFL)
                            )
                        }
                        .testTag("color_swatch_${baseArgb.toString(16)}")
                )
            }
        }

        // Expandable Circular Color Wheel + Brightness Slider
        AnimatedVisibility(visible = showColorWheel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HSVColorWheelPicker(
                    hue = hue,
                    saturation = sat,
                    value = valueBrightness,
                    onColorChanged = { h, s ->
                        hue = h
                        sat = s
                        if (alphaFraction <= 0.02f) alphaFraction = 1f
                        onSetColorTransient(composeArgb(h, s, valueBrightness, alphaFraction))
                    },
                    onColorFinished = {
                        onCommitColor(composeArgb(hue, sat, valueBrightness, alphaFraction))
                    },
                    modifier = Modifier
                        .size(126.dp)
                        .testTag("hsv_color_wheel")
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val previewColorInt = AndroidColor.HSVToColor(
                        (alphaFraction * 255f).roundToInt().coerceIn(0, 255),
                        floatArrayOf(hue, sat, valueBrightness)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(previewColorInt))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        Text(
                            text = String.format("#%08X", previewColorInt),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text("Brightness", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = valueBrightness,
                        onValueChange = { v ->
                            valueBrightness = v
                            if (alphaFraction <= 0.02f) alphaFraction = 1f
                            onSetColorTransient(composeArgb(hue, sat, v, alphaFraction))
                        },
                        onValueChangeFinished = {
                            onCommitColor(composeArgb(hue, sat, valueBrightness, alphaFraction))
                        },
                        valueRange = 0f..1f
                    )
                }
            }
        }

        // Transparency Controller right beneath palette/wheel (Solid <-> Transparent)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transparency • Solid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(alphaFraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Transparent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = 1f - alphaFraction,
                onValueChange = { transparencyVal ->
                    val nextAlpha = (1f - transparencyVal).coerceIn(0f, 1f)
                    alphaFraction = nextAlpha
                    onSetColorTransient(composeArgb(hue, sat, valueBrightness, nextAlpha))
                },
                onValueChangeFinished = {
                    onCommitColor(composeArgb(hue, sat, valueBrightness, alphaFraction))
                },
                valueRange = 0f..1f,
                modifier = Modifier.testTag("color_transparency_slider")
            )
        }
    }
}

@Composable
private fun HSVColorWheelPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onColorChanged: (hue: Float, saturation: Float) -> Unit,
    onColorFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hueBrush = remember {
        Brush.sweepGradient(
            listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color.Magenta,
                Color.Red
            )
        )
    }

    fun updateFromOffset(pos: Offset, width: Float, height: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f).coerceAtLeast(1f)
        val dx = pos.x - cx
        val dy = pos.y - cy
        val dist = hypot(dx, dy).coerceAtMost(radius)
        val angleDeg = ((atan2(dy, dx) * 180.0 / PI) + 360.0) % 360.0
        val nextHue = angleDeg.toFloat().coerceIn(0f, 360f)
        val nextSat = (dist / radius).coerceIn(0f, 1f)
        onColorChanged(nextHue, nextSat)
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    updateFromOffset(pos, size.width.toFloat(), size.height.toFloat())
                    onColorFinished()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onColorFinished() },
                    onDragCancel = { onColorFinished() }
                ) { change, _ ->
                    change.consume()
                    updateFromOffset(change.position, size.width.toFloat(), size.height.toFloat())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(brush = hueBrush, radius = radius, center = center)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            if (value < 0.99f) {
                drawCircle(
                    color = Color.Black.copy(alpha = (1f - value).coerceIn(0f, 0.92f)),
                    radius = radius,
                    center = center
                )
            }
            drawCircle(color = Color.White.copy(alpha = 0.6f), radius = radius, center = center, style = Stroke(2.dp.toPx()))

            val radAngle = Math.toRadians(hue.toDouble())
            val thumbDist = saturation * radius
            val thumbCenter = Offset(
                x = center.x + (thumbDist * cos(radAngle)).toFloat(),
                y = center.y + (thumbDist * sin(radAngle)).toFloat()
            )
            drawCircle(color = Color.Black, radius = 8.dp.toPx(), center = thumbCenter, style = Stroke(2.5.dp.toPx()))
            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = thumbCenter, style = Stroke(2.dp.toPx()))
        }
    }
}
