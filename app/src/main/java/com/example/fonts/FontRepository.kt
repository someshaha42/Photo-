package com.example.fonts

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FontRepository(
    private val context: Context,
    val renderEngine: FontRenderEngine
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val importedDir = File(context.filesDir, "imported_fonts")
    private val unwrappedDir = File(context.filesDir, "imported_fonts/unwrapped")
    private val registryFile = File(importedDir, "registry.json")
    private val historyFile = File(importedDir, "import_history.json")
    private val archiveExtractor = FontArchiveExtractor(context)

    private val _fonts = MutableStateFlow<List<FontMetadata>>(emptyList())
    val fonts: StateFlow<List<FontMetadata>> = _fonts.asStateFlow()

    private val _selectedFontId = MutableStateFlow("bundled:Roboto.ttf")
    val selectedFontId: StateFlow<String> = _selectedFontId.asStateFlow()

    private val _importProgress = MutableStateFlow(FontImportProgress())
    val importProgress: StateFlow<FontImportProgress> = _importProgress.asStateFlow()

    private val _importHistory = MutableStateFlow<List<ImportHistoryEntry>>(emptyList())
    val importHistory: StateFlow<List<ImportHistoryEntry>> = _importHistory.asStateFlow()

    private var initialized = false

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (initialized) return@withContext
        importedDir.mkdirs()
        unwrappedDir.mkdirs()

        val bundled = scanBundledFontsDirectory()
        val imported = loadImportedRegistry().filter { meta ->
            File(meta.storagePath).exists()
        }
        val combined = bundled + imported
        renderEngine.registerFonts(combined)
        _fonts.value = combined

        val defaultId = bundled.firstOrNull { it.isDefaultMandatory }?.id
            ?: bundled.firstOrNull()?.id
            ?: "bundled:Roboto.ttf"
        _selectedFontId.value = defaultId

        _importHistory.value = loadHistory()
        initialized = true
    }

    fun selectFont(fontId: String) {
        if (_fonts.value.any { it.id == fontId }) {
            _selectedFontId.value = fontId
        }
    }

    /**
     * Dynamically scans the dedicated project Fonts folder (`assets/Fonts/`) on every initialization.
     * Any newly added font file dropped into `app/src/main/assets/Fonts/` is discovered automatically
     * on subsequent builds without hardcoding filenames.
     */
    private fun scanBundledFontsDirectory(): List<FontMetadata> {
        val assetFiles = context.assets.list("Fonts")?.sorted() ?: emptyList()
        val supportedExts = setOf("ttf", "otf", "ttc", "otc", "woff", "woff2")
        val results = mutableListOf<FontMetadata>()

        for (fileName in assetFiles) {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            if (ext !in supportedExts) continue

            val assetPath = "Fonts/$fileName"
            val bytes = runCatching {
                context.assets.open(assetPath).use { it.readBytes() }
            }.getOrNull() ?: continue

            val inspection = TrueTypeTableParser.inspectBytes(bytes, fileName)
            val isDefault = fileName.equals("Roboto.ttf", ignoreCase = true) ||
                inspection.displayName.equals("Roboto", ignoreCase = true)

            val summary = if (isDefault) {
                "Built-in default font • Works offline • ${inspection.glyphCount} glyphs"
            } else {
                "Bundled in Fonts/ • ${inspection.format.label} • ${inspection.glyphCount} glyphs"
            }

            results.add(
                FontMetadata(
                    id = "bundled:$fileName",
                    displayName = inspection.displayName,
                    fileName = fileName,
                    source = FontSource.BUNDLED,
                    format = inspection.format,
                    classification = inspection.classification,
                    colorTableType = inspection.colorTableType,
                    tableTags = inspection.tableTags,
                    glyphCount = inspection.glyphCount,
                    unicodeCoverageCount = inspection.unicodeCoverageCount,
                    hasEmojiCoverage = inspection.hasEmojiCoverage,
                    bitmapStrikePpem = inspection.bitmapStrikePpem,
                    sha256Hash = inspection.sha256,
                    fileSizeBytes = bytes.size.toLong(),
                    storagePath = assetPath,
                    isDefaultMandatory = isDefault,
                    compatibilityWarnings = inspection.warnings,
                    isValid = inspection.isValid,
                    statusSummary = summary
                )
            )
        }

        // Ensure mandatory default Roboto appears first
        return results.sortedWith(
            compareByDescending<FontMetadata> { it.isDefaultMandatory }
                .thenBy { it.displayName.lowercase() }
        )
    }

    suspend fun importFontUris(uris: List<Uri>) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext
        _importProgress.value = FontImportProgress(
            isImporting = true,
            currentFile = "Preparing...",
            processed = 0,
            total = uris.size
        )

        val results = mutableListOf<FontImportItemResult>()
        val existingHashes = _fonts.value.map { it.sha256Hash }.toMutableSet()

        uris.forEachIndexed { idx, uri ->
            val name = queryDisplayName(uri) ?: "font_${idx + 1}.ttf"
            _importProgress.value = _importProgress.value.copy(
                currentFile = name,
                processed = idx
            )
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()

            if (bytes == null) {
                results.add(FontImportItemResult(name, FontImportStatus.INVALID, "Could not read selected file."))
            } else {
                val res = validateAndStoreFont(bytes, name, existingHashes)
                if (res.fontMetadata != null) existingHashes.add(res.fontMetadata.sha256Hash)
                results.add(res)
            }
        }

        finalizeImportBatch("Direct File Picker (${uris.size} selected)", results)
    }

    suspend fun importArchiveUri(uri: Uri) = withContext(Dispatchers.IO) {
        val archiveName = queryDisplayName(uri) ?: "fonts_archive.zip"
        _importProgress.value = FontImportProgress(
            isImporting = true,
            currentFile = "Inspecting $archiveName...",
            processed = 0,
            total = 1
        )

        val tempArchive = File(context.cacheDir, "import_src_${UUID.randomUUID()}_$archiveName")
        val results = mutableListOf<FontImportItemResult>()
        val existingHashes = _fonts.value.map { it.sha256Hash }.toMutableSet()

        try {
            context.contentResolver.openInputStream(uri)?.use { inp ->
                FileOutputStream(tempArchive).use { out ->
                    inp.copyTo(out, 64 * 1024)
                }
            } ?: run {
                _importProgress.value = FontImportProgress(
                    isImporting = false,
                    summaryMessage = "Could not open archive $archiveName."
                )
                return@withContext
            }

            archiveExtractor.extractAndProcess(tempArchive, archiveName) { outcome ->
                if (outcome.errorMessage != null && outcome.candidates.isEmpty()) {
                    results.add(
                        FontImportItemResult(
                            archiveName,
                            FontImportStatus.INVALID,
                            outcome.errorMessage
                        )
                    )
                } else {
                    val total = outcome.candidates.size
                    outcome.candidates.forEachIndexed { idx, candidate ->
                        _importProgress.value = _importProgress.value.copy(
                            currentFile = candidate.relativePath,
                            processed = idx,
                            total = total.coerceAtLeast(1)
                        )
                        val bytes = runCatching { candidate.file.readBytes() }.getOrNull()
                        if (bytes == null) {
                            results.add(
                                FontImportItemResult(
                                    candidate.relativePath,
                                    FontImportStatus.INVALID,
                                    "Unreadable extracted file."
                                )
                            )
                        } else {
                            val res = validateAndStoreFont(bytes, candidate.file.name, existingHashes)
                            if (res.fontMetadata != null) existingHashes.add(res.fontMetadata.sha256Hash)
                            results.add(res)
                        }
                    }
                    if (outcome.skippedNonFonts > 0) {
                        results.add(
                            FontImportItemResult(
                                "${outcome.skippedNonFonts} unrelated file(s)",
                                FontImportStatus.UNSUPPORTED,
                                "Ignored non-font files inside archive."
                            )
                        )
                    }
                }
            }
        } finally {
            runCatching { tempArchive.delete() }
        }

        finalizeImportBatch("Archive: $archiveName", results)
    }

    private fun validateAndStoreFont(
        bytes: ByteArray,
        originalName: String,
        existingHashes: Set<String>
    ): FontImportItemResult {
        val inspection = TrueTypeTableParser.inspectBytes(bytes, originalName)
        if (!inspection.isValid) {
            return FontImportItemResult(
                fileName = originalName,
                status = if (inspection.format == FontFormat.WOFF2) FontImportStatus.UNSUPPORTED else FontImportStatus.INVALID,
                message = inspection.errorReason ?: "Invalid or unsupported font."
            )
        }
        if (inspection.sha256 in existingHashes) {
            return FontImportItemResult(
                fileName = originalName,
                status = FontImportStatus.DUPLICATE,
                message = "Already in Font Library (${inspection.displayName})."
            )
        }

        val id = "imported:${inspection.sha256.take(12)}_${UUID.randomUUID().toString().take(6)}"
        val safeName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val storedFile = File(importedDir, "${inspection.sha256.take(12)}_$safeName")
        storedFile.writeBytes(bytes)

        // If WOFF1 was unwrapped into SFNT bytes, store clean unwrapped SFNT for Android native Typeface rendering
        var renderablePath: String? = storedFile.absolutePath
        if (inspection.unwrappedSfntBytes != null) {
            val unwrappedFile = File(unwrappedDir, "${inspection.sha256.take(12)}.ttf")
            unwrappedFile.writeBytes(inspection.unwrappedSfntBytes)
            renderablePath = unwrappedFile.absolutePath
        }

        val summary = buildString {
            append("${inspection.classification.badgeName} • ${inspection.format.label} • ${inspection.glyphCount} glyphs")
            if (inspection.bitmapStrikePpem.isNotEmpty()) {
                append(" • ${inspection.bitmapStrikePpem.max()}px strike")
            }
        }

        val meta = FontMetadata(
            id = id,
            displayName = inspection.displayName,
            fileName = originalName,
            source = FontSource.IMPORTED,
            format = inspection.format,
            classification = inspection.classification,
            colorTableType = inspection.colorTableType,
            tableTags = inspection.tableTags,
            glyphCount = inspection.glyphCount,
            unicodeCoverageCount = inspection.unicodeCoverageCount,
            hasEmojiCoverage = inspection.hasEmojiCoverage,
            bitmapStrikePpem = inspection.bitmapStrikePpem,
            sha256Hash = inspection.sha256,
            fileSizeBytes = bytes.size.toLong(),
            storagePath = storedFile.absolutePath,
            renderablePath = renderablePath,
            isDefaultMandatory = false,
            compatibilityWarnings = inspection.warnings,
            isValid = true,
            statusSummary = summary
        )
        return FontImportItemResult(
            fileName = originalName,
            status = FontImportStatus.IMPORTED,
            message = "Imported ${meta.displayName} (${meta.classification.badgeName})",
            fontMetadata = meta
        )
    }

    private fun finalizeImportBatch(sourceLabel: String, results: List<FontImportItemResult>) {
        val newlyImported = results.mapNotNull { it.fontMetadata }
        if (newlyImported.isNotEmpty()) {
            val current = _fonts.value.toMutableList()
            current.addAll(newlyImported)
            renderEngine.registerFonts(newlyImported)
            _fonts.value = current
            saveImportedRegistry(current.filter { it.source == FontSource.IMPORTED })
            _selectedFontId.value = newlyImported.first().id
        }

        val importedCount = newlyImported.size
        val dupCount = results.count { it.status == FontImportStatus.DUPLICATE }
        val failCount = results.count { it.status == FontImportStatus.INVALID || it.status == FontImportStatus.UNSUPPORTED }

        val entry = ImportHistoryEntry(
            id = UUID.randomUUID().toString(),
            sourceLabel = sourceLabel,
            timestampMillis = System.currentTimeMillis(),
            importedCount = importedCount,
            duplicateCount = dupCount,
            failedCount = failCount,
            details = results.take(12).map { "${it.fileName}: ${it.message}" }
        )
        val updatedHistory = (listOf(entry) + _importHistory.value).take(25)
        _importHistory.value = updatedHistory
        saveHistory(updatedHistory)

        val summary = "Imported: $importedCount • Duplicates skipped: $dupCount • Failed/Ignored: $failCount"
        _importProgress.value = FontImportProgress(
            isImporting = false,
            processed = results.size,
            total = results.size,
            results = results,
            summaryMessage = summary
        )
    }

    fun clearImportProgressMessage() {
        _importProgress.value = FontImportProgress()
    }

    suspend fun deleteImportedFont(fontId: String): Boolean = withContext(Dispatchers.IO) {
        val target = _fonts.value.firstOrNull { it.id == fontId } ?: return@withContext false
        if (target.isDefaultMandatory || target.source == FontSource.BUNDLED) return@withContext false

        runCatching { File(target.storagePath).delete() }
        target.renderablePath?.let { path ->
            if (path != target.storagePath) runCatching { File(path).delete() }
        }
        renderEngine.evictFont(fontId)

        val remaining = _fonts.value.filterNot { it.id == fontId }
        _fonts.value = remaining
        saveImportedRegistry(remaining.filter { it.source == FontSource.IMPORTED })
        if (_selectedFontId.value == fontId) {
            _selectedFontId.value = remaining.firstOrNull { it.isDefaultMandatory }?.id
                ?: remaining.firstOrNull()?.id
                ?: "bundled:Roboto.ttf"
        }
        true
    }

    private fun loadImportedRegistry(): List<FontMetadata> {
        if (!registryFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<FontMetadata>>(registryFile.readText())
        }.getOrElse { emptyList() }
    }

    private fun saveImportedRegistry(imported: List<FontMetadata>) {
        runCatching {
            registryFile.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(FontMetadata.serializer()), imported))
        }
    }

    private fun loadHistory(): List<ImportHistoryEntry> {
        if (!historyFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<ImportHistoryEntry>>(historyFile.readText())
        }.getOrElse { emptyList() }
    }

    private fun saveHistory(entries: List<ImportHistoryEntry>) {
        runCatching {
            historyFile.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ImportHistoryEntry.serializer()), entries))
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
    }
}
