package com.example.fonts

import kotlinx.serialization.Serializable

@Serializable
enum class FontSource {
    BUNDLED,
    IMPORTED
}

@Serializable
enum class FontFormat(val extension: String, val label: String) {
    TTF("ttf", "TTF"),
    OTF("otf", "OTF"),
    TTC("ttc", "TTC"),
    OTC("otc", "OTC"),
    WOFF("woff", "WOFF"),
    WOFF2("woff2", "WOFF2"),
    UNKNOWN("bin", "Unknown")
}

@Serializable
enum class ColorTableType(val label: String) {
    NONE("Monochrome / Vector"),
    COLR_CPAL("COLR / CPAL Vector Color"),
    CBDT_CBLC("CBDT / CBLC Color Bitmap"),
    SBIX("sbix Apple Color Bitmap"),
    SVG_IN_OT("SVG-in-OpenType Color")
}

@Serializable
enum class FontClassification(val label: String, val badgeName: String) {
    STANDARD_TEXT("Standard Text Font", "Standard"),
    COLOR_EMOJI("Color Emoji Font", "Color Emoji"),
    SYMBOL_UNICODE("Symbol / Unicode Font", "Symbols"),
    MIXED_TEXT_COLOR_EMOJI("Mixed Text & Color Emoji Font", "Mixed"),
    UNSUPPORTED("Unsupported / Invalid Font", "Unsupported")
}

@Serializable
data class FontMetadata(
    val id: String,
    val displayName: String,
    val fileName: String,
    val source: FontSource,
    val format: FontFormat,
    val classification: FontClassification,
    val colorTableType: ColorTableType,
    val tableTags: List<String> = emptyList(),
    val glyphCount: Int = 0,
    val unicodeCoverageCount: Int = 0,
    val hasEmojiCoverage: Boolean = false,
    val bitmapStrikePpem: List<Int> = emptyList(),
    val sha256Hash: String,
    val fileSizeBytes: Long,
    val storagePath: String,
    val renderablePath: String? = null,
    val isDefaultMandatory: Boolean = false,
    val compatibilityWarnings: List<String> = emptyList(),
    val isValid: Boolean = true,
    val statusSummary: String = "",
    val importedAtMillis: Long = System.currentTimeMillis()
)

enum class FontImportStatus {
    IMPORTED,
    DUPLICATE,
    INVALID,
    UNSUPPORTED
}

data class FontImportItemResult(
    val fileName: String,
    val status: FontImportStatus,
    val message: String,
    val fontMetadata: FontMetadata? = null
)

@Serializable
data class ImportHistoryEntry(
    val id: String,
    val sourceLabel: String,
    val timestampMillis: Long,
    val importedCount: Int,
    val duplicateCount: Int,
    val failedCount: Int,
    val details: List<String>
)

data class FontImportProgress(
    val isImporting: Boolean = false,
    val currentFile: String = "",
    val processed: Int = 0,
    val total: Int = 0,
    val results: List<FontImportItemResult> = emptyList(),
    val summaryMessage: String? = null
)

data class FontDiagnosticsSnapshot(
    val totalFonts: Int = 0,
    val bundledCount: Int = 0,
    val importedCount: Int = 0,
    val colorEmojiCount: Int = 0,
    val cacheHitCount: Long = 0L,
    val cacheMissCount: Long = 0L,
    val cacheMemoryBytes: Int = 0,
    val cacheMaxBytes: Int = 0,
    val cacheEntryCount: Int = 0,
    val lastRasterizationMs: Double = 0.0,
    val avgRasterizationMs: Double = 0.0,
    val lastRequestedPx: Int = 0,
    val lastRenderedPx: Int = 0,
    val activeFontDiagnostics: List<String> = emptyList()
)
