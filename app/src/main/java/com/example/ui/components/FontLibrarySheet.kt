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

    val selectedFont = remember(fonts, selectedFontId) {
        fonts.firstOrNull { it.id == selectedFontId } ?: fonts.firstOrNull()
    }

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
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close Font Library")
                }
                Text(
                    text = "Font Library",
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

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search fonts...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("font_search_input")
            )

            // Filter Pills
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

            // Import Progress / Result Banner
            if (importProgress.isImporting || importProgress.summaryMessage != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (importProgress.isImporting) {
                            Text(
                                text = "Importing ${importProgress.currentFile} (${importProgress.processed}/${importProgress.total})",
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
                                    Text("Dismiss")
                                }
                            }
                            importProgress.results.take(4).forEach { item ->
                                Text(
                                    text = "• ${item.fileName}: ${item.message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Currently Selected Font Card
            if (selectedFont != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CURRENTLY SELECTED FONT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedFont.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedFont.classification.badgeName,
                                style = MaterialTheme.typography.labelMedium,
                                color = StudioAzure
                            )
                        }
                        Text(
                            text = "Selected typeface • Active font",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available Fonts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredFonts.size} fonts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Vertical Notebook-style Font List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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

            // Bottom Action Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onImportFontsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_fonts_button"),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF10131A)
                    )
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import Fonts", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onImportArchiveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_font_archive_button"),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import Font Archive (ZIP, RAR, 7Z, TAR, XZ)")
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
                    Text("Font Information / Diagnostics")
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
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) StudioAzure else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .testTag("font_card_${font.fileName}"),
        shape = RoundedCornerShape(16.dp),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Active Font",
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

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            // Live Typeface Preview using the font's own native Typeface (preserving Color Emoji RGBA colors!)
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                        setTextColor(textColorInt)
                    }
                },
                update = { tv ->
                    tv.typeface = renderEngine.getTypeface(font.id)
                    tv.text = previewSample
                    tv.setTextColor(textColorInt)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = font.statusSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onInspect) {
                        Text("Preview", color = StudioAzure)
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete imported font",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
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
                Text("Classification: ${font.classification.label}")
                Text("Format: ${font.format.label} • Color Table: ${font.colorTableType.label}")
                Text("Glyphs: ${font.glyphCount} • Unicode Codepoints: ${font.unicodeCoverageCount}")
                if (font.bitmapStrikePpem.isNotEmpty()) {
                    Text("Bitmap Strikes: ${font.bitmapStrikePpem.joinToString(", ")} px")
                }
                Text(
                    text = "Tables: ${font.tableTags.take(12).joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (font.compatibilityWarnings.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = StudioAmber.copy(alpha = 0.14f))) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = StudioAmber)
                            Text(font.compatibilityWarnings.joinToString("\n"), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                HorizontalDivider()
                Text("Zoom Quality Test (${zoomPercent}% • ${renderedBitmap.width}×${renderedBitmap.height}px RGBA8888)", fontWeight = FontWeight.SemiBold)
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
                        .height(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = renderedBitmap.asImageBitmap(),
                        contentDescription = "High resolution glyph zoom preview",
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
        title = { Text("Font Engine Diagnostics", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Installed Fonts: ${diagnostics.totalFonts} (Bundled: ${diagnostics.bundledCount}, Imported: ${diagnostics.importedCount}, Color Emoji: ${diagnostics.colorEmojiCount})")
                val totalLookups = diagnostics.cacheHitCount + diagnostics.cacheMissCount
                val hitRate = if (totalLookups > 0) (diagnostics.cacheHitCount * 100 / totalLookups) else 0
                Text("Glyph LRU Cache: ${diagnostics.cacheEntryCount} entries • ${diagnostics.cacheMemoryBytes / 1024} KB / ${diagnostics.cacheMaxBytes / (1024 * 1024)} MB")
                Text("Cache Hit/Miss: ${diagnostics.cacheHitCount} / ${diagnostics.cacheMissCount} ($hitRate% hit rate)")
                Text("Last Rasterization: ${"%.2f".format(diagnostics.lastRasterizationMs)} ms (Avg: ${"%.2f".format(diagnostics.avgRasterizationMs)} ms)")
                Text("Last Resolution: requested=${diagnostics.lastRequestedPx}px, rendered=${diagnostics.lastRenderedPx}px")

                HorizontalDivider()
                Text("Registered Fonts & Tables", fontWeight = FontWeight.Bold)
                diagnostics.activeFontDiagnostics.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodyMedium)
                }

                if (importHistory.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Import History", fontWeight = FontWeight.Bold)
                    importHistory.take(8).forEach { entry ->
                        Text(
                            text = "• ${entry.sourceLabel}: +${entry.importedCount} imported, ${entry.duplicateCount} dup, ${entry.failedCount} failed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
