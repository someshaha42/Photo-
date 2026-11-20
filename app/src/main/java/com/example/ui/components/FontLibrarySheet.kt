package com.example.ui.components

import android.util.TypedValue
import android.widget.TextView
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.fonts.FontClassification
import com.example.fonts.FontDiagnosticsSnapshot
import com.example.fonts.FontImportProgress
import com.example.fonts.FontMetadata
import com.example.fonts.FontRenderEngine
import com.example.fonts.FontSource
import com.example.fonts.ImportHistoryEntry
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAzure
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioTeal

private enum class FontFilterCategory(val label: String) {
    ALL("All"),
    STANDARD("Standard"),
    COLOR_EMOJI("Color Emoji"),
    SYMBOLS("Symbols"),
    IMPORTED("Imported")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontLibrarySheet(
    fonts: List<FontMetadata>,
    selectedFontId: String,
    renderEngine: FontRenderEngine,
    importProgress: FontImportProgress,
    importHistory: List<ImportHistoryEntry>,
    diagnostics: FontDiagnosticsSnapshot,
    onSelectFont: (String) -> Unit,
    onImportFontsClick: () -> Unit,
    onImportArchiveClick: () -> Unit,
    onDeleteFont: (String) -> Unit,
    onClearImportStatus: () -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(FontFilterCategory.ALL) }
    var inspectedFont by remember { mutableStateOf<FontMetadata?>(null) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    val filteredFonts = remember(fonts, searchQuery, activeFilter) {
        fonts.filter { font ->
            val matchesQuery = searchQuery.isBlank() ||
                font.displayName.contains(searchQuery, ignoreCase = true) ||
                font.fileName.contains(searchQuery, ignoreCase = true)
            val matchesCat = when (activeFilter) {
                FontFilterCategory.ALL -> true
                FontFilterCategory.STANDARD -> font.classification == FontClassification.STANDARD_TEXT
                FontFilterCategory.COLOR_EMOJI ->
                    font.classification == FontClassification.COLOR_EMOJI ||
                        font.classification == FontClassification.MIXED_TEXT_COLOR_EMOJI
                FontFilterCategory.SYMBOLS -> font.classification == FontClassification.SYMBOL_UNICODE
                FontFilterCategory.IMPORTED -> font.source == FontSource.IMPORTED
            }
            matchesQuery && matchesCat
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close")
                }
                Text(
                    text = "Fonts (${filteredFonts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onImportFontsClick,
                    modifier = Modifier.testTag("font_library_top_import_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Import Fonts")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("font_search_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontFilterCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = activeFilter == cat,
                        onClick = { activeFilter = cat },
                        label = { Text(cat.label) }
                    )
                }
            }

            if (importProgress.isImporting || importProgress.summaryMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (importProgress.isImporting) {
                            Text(
                                text = "${importProgress.currentFile} (${importProgress.processed}/${importProgress.total})",
                                style = MaterialTheme.typography.labelLarge
                            )
                            LinearProgressIndicator(
                                progress = {
                                    if (importProgress.total > 0)
                                        importProgress.processed.toFloat() / importProgress.total
                                    else 0f
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = importProgress.summaryMessage.orEmpty(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StudioTeal,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onClearImportStatus) {
                                    Text("OK")
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(filteredFonts, key = { it.id }) { font ->
                    val isSelected = font.id == selectedFontId
                    FontPreviewCard(
                        font = font,
                        isSelected = isSelected,
                        renderEngine = renderEngine,
                        onSelect = {
                            onSelectFont(font.id)
                            onDismiss()
                        },
                        onInspect = { inspectedFont = font },
                        onDelete = if (font.source == FontSource.IMPORTED && !font.isDefaultMandatory) {
                            { onDeleteFont(font.id) }
                        } else null
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onImportFontsClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_fonts_button"),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioAzure,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import Font", fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onImportArchiveClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_font_archive_button"),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ZIP / RAR / 7Z", maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = {
                        onRefreshDiagnostics()
                        showDiagnosticsDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("font_diagnostics_button"),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Diagnostics")
                }
            }
        }
    }

    inspectedFont?.let { font ->
        FontDetailZoomDialog(
            font = font,
            renderEngine = renderEngine,
            onSelectAndClose = {
                onSelectFont(font.id)
                inspectedFont = null
                onDismiss()
            },
            onDismiss = { inspectedFont = null }
        )
    }

    if (showDiagnosticsDialog) {
        FontDiagnosticsDialog(
            diagnostics = diagnostics,
            importHistory = importHistory,
            onDismiss = { showDiagnosticsDialog = false }
        )
    }
}

@Composable
private fun FontPreviewCard(
    font: FontMetadata,
    isSelected: Boolean,
    renderEngine: FontRenderEngine,
    onSelect: () -> Unit,
    onInspect: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val badgeColor = when {
        font.source == FontSource.IMPORTED -> StudioPurple
        font.classification == FontClassification.COLOR_EMOJI -> StudioEmerald
        font.classification == FontClassification.SYMBOL_UNICODE -> StudioAmber
        else -> StudioAzure
    }
    val badgeLabel = when {
        font.source == FontSource.IMPORTED -> "Imported"
        else -> font.classification.badgeName
    }
    val previewSample = remember(font.id) {
        when (font.classification) {
            FontClassification.COLOR_EMOJI -> "😀 🥰 🚀 🌈 🎨 ✨"
            FontClassification.MIXED_TEXT_COLOR_EMOJI -> "Aa Bb 123 😀 🚀 🎨"
            FontClassification.SYMBOL_UNICODE -> "★ ✦ ◆ ♛ ♫ ☀ Aa 123"
            else -> "Aa Bb Cc 123"
        }
    }
    val textColorInt = MaterialTheme.colorScheme.onSurface.toArgb()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
            .testTag("font_card_${font.fileName}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Selected",
                            tint = StudioAzure,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = font.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = badgeColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    IconButton(onClick = onInspect, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.Info, contentDescription = "Inspect", tint = StudioAzure, modifier = Modifier.size(18.dp))
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                        setTextColor(textColorInt)
                        includeFontPadding = true
                        setPadding(0, 8, 0, 8)
                    }
                },
                update = { tv ->
                    tv.typeface = renderEngine.getTypeface(font.id)
                    tv.text = previewSample
                    tv.setTextColor(textColorInt)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FontDetailZoomDialog(
    font: FontMetadata,
    renderEngine: FontRenderEngine,
    onSelectAndClose: () -> Unit,
    onDismiss: () -> Unit
) {
    var zoomPercent by remember { mutableIntStateOf(200) }
    val sampleText = if (font.classification == FontClassification.COLOR_EMOJI || font.hasEmojiCoverage) {
        "😀 🚀 🌈 🎨"
    } else {
        "Aa Bb 123"
    }
    val textColorLong = MaterialTheme.colorScheme.onSurface.toArgb().toLong() and 0xFFFFFFFFL
    val targetPx = (36 * (zoomPercent / 100f)).toInt()
    val renderedBitmap = remember(font.id, zoomPercent, sampleText) {
        renderEngine.getOrRenderGlyphBitmap(
            fontId = font.id,
            text = sampleText,
            targetFontPx = targetPx,
            colorArgb = textColorLong
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(font.displayName, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${font.format.label} • ${font.colorTableType.label}")
                Text("Glyphs: ${font.glyphCount} • Unicode: ${font.unicodeCoverageCount}")
                if (font.bitmapStrikePpem.isNotEmpty()) {
                    Text("Strikes: ${font.bitmapStrikePpem.joinToString(", ")} px")
                }
                if (font.compatibilityWarnings.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = StudioAmber.copy(alpha = 0.14f))) {
                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = StudioAmber)
                            Text(font.compatibilityWarnings.joinToString("\n"), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(100, 200, 400, 800).forEach { pct ->
                        FilterChip(
                            selected = zoomPercent == pct,
                            onClick = { zoomPercent = pct },
                            label = { Text("${pct}%") }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = renderedBitmap.asImageBitmap(),
                        contentDescription = "Glyph Zoom Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSelectAndClose) { Text("Use Font") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun FontDiagnosticsDialog(
    diagnostics: FontDiagnosticsSnapshot,
    importHistory: List<ImportHistoryEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Font Diagnostics", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Fonts: ${diagnostics.totalFonts} (Bundled: ${diagnostics.bundledCount}, Imported: ${diagnostics.importedCount})")
                Text("LRU Cache: ${diagnostics.cacheEntryCount} entries • ${diagnostics.cacheMemoryBytes / 1024} KB / ${diagnostics.cacheMaxBytes / (1024 * 1024)} MB")
                Text("Hit/Miss: ${diagnostics.cacheHitCount} / ${diagnostics.cacheMissCount}")
                Text("Last Raster: ${"%.2f".format(diagnostics.lastRasterizationMs)} ms")
                HorizontalDivider()
                diagnostics.activeFontDiagnostics.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyMedium)
                }
                if (importHistory.isNotEmpty()) {
                    HorizontalDivider()
                    importHistory.take(5).forEach { entry ->
                        Text("• ${entry.sourceLabel}: +${entry.importedCount}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
