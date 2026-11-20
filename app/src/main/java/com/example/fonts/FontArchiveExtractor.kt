package com.example.fonts

import android.content.Context
import com.github.junrar.Archive
import com.github.junrar.exception.RarException
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipInputStream

data class ExtractedFontCandidate(
    val relativePath: String,
    val file: File
)

data class ArchiveExtractionOutcome(
    val candidates: List<ExtractedFontCandidate>,
    val skippedNonFonts: Int,
    val errorMessage: String? = null
)

/**
 * Security-hardened archive extractor supporting ZIP, RAR, 7Z, TAR, TAR.GZ/TGZ, GZIP, BZIP2, and XZ.
 * Protects against ZIP Slip, path traversal, and archive bombs, and always cleans up temp files.
 */
class FontArchiveExtractor(private val context: Context) {

    companion object {
        private const val MAX_ENTRIES = 500
        private const val MAX_SINGLE_FILE_BYTES = 50L * 1024L * 1024L // 50 MB
        private const val MAX_TOTAL_BYTES = 150L * 1024L * 1024L // 150 MB
        private val SUPPORTED_FONT_EXTS = setOf("ttf", "otf", "ttc", "otc", "woff", "woff2")
    }

    suspend fun extractAndProcess(
        archiveFile: File,
        archiveName: String,
        onExtracted: suspend (ArchiveExtractionOutcome) -> Unit
    ) {
        val tempRoot = File(context.cacheDir, "font_archive_tmp_${UUID.randomUUID()}")
        tempRoot.mkdirs()
        try {
            val outcome = extractToTempDir(archiveFile, archiveName, tempRoot)
            onExtracted(outcome)
        } finally {
            runCatching { tempRoot.deleteRecursively() }
        }
    }

    private fun extractToTempDir(
        archiveFile: File,
        archiveName: String,
        tempRoot: File
    ): ArchiveExtractionOutcome {
        val lower = archiveName.lowercase()
        val canonicalRoot = tempRoot.canonicalPath
        var entryCount = 0
        var totalExtractedBytes = 0L
        var skippedNonFonts = 0

        fun safeCreateFile(entryName: String): File {
            val sanitized = entryName.replace('\\', '/').trimStart('/')
            val outFile = File(tempRoot, sanitized)
            val canonical = outFile.canonicalPath
            if (!canonical.startsWith(canonicalRoot + File.separator)) {
                throw SecurityException("Blocked ZIP Slip / path traversal entry: $entryName")
            }
            return outFile
        }

        fun copyBounded(input: InputStream, outFile: File): Boolean {
            outFile.parentFile?.mkdirs()
            val buffer = ByteArray(16 * 1024)
            var fileBytes = 0L
            FileOutputStream(outFile).use { fos ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    fileBytes += read
                    totalExtractedBytes += read
                    if (fileBytes > MAX_SINGLE_FILE_BYTES) {
                        throw IllegalStateException("Entry exceeds 50 MB single-file security limit.")
                    }
                    if (totalExtractedBytes > MAX_TOTAL_BYTES) {
                        throw IllegalStateException("Archive exceeds 150 MB decompression limit (possible archive bomb).")
                    }
                    fos.write(buffer, 0, read)
                }
            }
            return true
        }

        try {
            when {
                lower.endsWith(".zip") -> {
                    ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zis ->
                        while (true) {
                            val entry = zis.nextEntry ?: break
                            if (entry.isDirectory) continue
                            entryCount++
                            if (entryCount > MAX_ENTRIES) throw IllegalStateException("Archive exceeds $MAX_ENTRIES entries limit.")
                            val ext = entry.name.substringAfterLast('.', "").lowercase()
                            if (ext !in SUPPORTED_FONT_EXTS) {
                                skippedNonFonts++
                                continue
                            }
                            val target = safeCreateFile(entry.name)
                            copyBounded(zis, target)
                        }
                    }
                }
                lower.endsWith(".7z") -> {
                    @Suppress("DEPRECATION")
                    SevenZFile(archiveFile).use { sevenZ ->
                        while (true) {
                            val entry = sevenZ.nextEntry ?: break
                            if (entry.isDirectory) continue
                            entryCount++
                            if (entryCount > MAX_ENTRIES) throw IllegalStateException("Archive exceeds $MAX_ENTRIES entries limit.")
                            val ext = entry.name.substringAfterLast('.', "").lowercase()
                            if (ext !in SUPPORTED_FONT_EXTS) {
                                skippedNonFonts++
                                continue
                            }
                            val target = safeCreateFile(entry.name)
                            sevenZ.getInputStream(entry).use { input ->
                                copyBounded(input, target)
                            }
                        }
                    }
                }
                lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> {
                    GzipCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { gz ->
                        extractTarStream(gz, { safeCreateFile(it) }, { inp, out -> copyBounded(inp, out) }) { isFont ->
                            entryCount++
                            if (entryCount > MAX_ENTRIES) throw IllegalStateException("Archive exceeds $MAX_ENTRIES entries limit.")
                            if (!isFont) skippedNonFonts++
                        }
                    }
                }
                lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") -> {
                    BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { bz ->
                        extractTarStream(bz, { safeCreateFile(it) }, { inp, out -> copyBounded(inp, out) }) { isFont ->
                            entryCount++
                            if (entryCount > MAX_ENTRIES) throw IllegalStateException("Archive exceeds $MAX_ENTRIES entries limit.")
                            if (!isFont) skippedNonFonts++
                        }
                    }
                }
                lower.endsWith(".tar.xz") || lower.endsWith(".txz") -> {
                    XZCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { xz ->
                        extractTarStream(xz, { safeCreateFile(it) }, { inp, out -> copyBounded(inp, out) }) { isFont ->
                            entryCount++
                            if (entryCount > MAX_ENTRIES) throw IllegalStateException("Archive exceeds $MAX_ENTRIES entries limit.")
                            if (!isFont) skippedNonFonts++
                        }
                    }
                }
                lower.endsWith(".tar") -> {
                    BufferedInputStream(FileInputStream(archiveFile)).use { fis ->
                        extractTarStream(fis, { safeCreateFile(it) }, { inp, out -> copyBounded(inp, out) }) { isFont ->
                            entryCount++
                            if (entryCount > MAX_ENTRIES) throw IllegalStateException("Archive exceeds $MAX_ENTRIES entries limit.")
                            if (!isFont) skippedNonFonts++
                        }
                    }
                }
                lower.endsWith(".gz") || lower.endsWith(".gzip") -> {
                    val innerName = archiveName.substringBeforeLast('.')
                    GzipCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { gz ->
                        val ext = innerName.substringAfterLast('.', "").lowercase()
                        if (ext in SUPPORTED_FONT_EXTS) {
                            copyBounded(gz, safeCreateFile(innerName))
                        } else {
                            // Try reading as TAR inside GZ
                            runCatching {
                                extractTarStream(gz, { safeCreateFile(it) }, { inp, out -> copyBounded(inp, out) }) { isFont ->
                                    entryCount++
                                    if (!isFont) skippedNonFonts++
                                }
                            }.onFailure { skippedNonFonts++ }
                        }
                    }
                }
                lower.endsWith(".bz2") || lower.endsWith(".bzip2") -> {
                    val innerName = archiveName.substringBeforeLast('.')
                    BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { bz ->
                        val ext = innerName.substringAfterLast('.', "").lowercase()
                        if (ext in SUPPORTED_FONT_EXTS) {
                            copyBounded(bz, safeCreateFile(innerName))
                        } else {
                            skippedNonFonts++
                        }
                    }
                }
                lower.endsWith(".xz") -> {
                    val innerName = archiveName.substringBeforeLast('.')
                    XZCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { xz ->
                        val ext = innerName.substringAfterLast('.', "").lowercase()
                        if (ext in SUPPORTED_FONT_EXTS) {
                            copyBounded(xz, safeCreateFile(innerName))
                        } else {
                            skippedNonFonts++
                        }
                    }
                }
                lower.endsWith(".rar") -> {
                    Archive(archiveFile).use { rar ->
                        if (rar.isEncrypted) {
                            return ArchiveExtractionOutcome(
                                emptyList(),
                                0,
                                "Password-protected / encrypted RAR archives are not supported."
                            )
                        }
                        while (true) {
                            val header = rar.nextFileHeader() ?: break
                            if (header.isDirectory) continue
                            if (header.isEncrypted) {
                                return ArchiveExtractionOutcome(
                                    emptyList(),
                                    0,
                                    "Encrypted RAR entries cannot be extracted safely."
                                )
                            }
                            entryCount++
                            if (entryCount > MAX_ENTRIES) throw IllegalStateException("Archive exceeds $MAX_ENTRIES entries limit.")
                            val name = header.fileName.replace('\\', '/')
                            val ext = name.substringAfterLast('.', "").lowercase()
                            if (ext !in SUPPORTED_FONT_EXTS) {
                                skippedNonFonts++
                                continue
                            }
                            val target = safeCreateFile(name)
                            rar.getInputStream(header).use { inp ->
                                copyBounded(inp, target)
                            }
                        }
                    }
                }
                else -> {
                    return ArchiveExtractionOutcome(
                        emptyList(),
                        0,
                        "Unsupported archive format: $archiveName"
                    )
                }
            }
        } catch (e: RarException) {
            return ArchiveExtractionOutcome(
                emptyList(),
                skippedNonFonts,
                "Unable to extract RAR archive (may be RAR5, corrupted, or password-protected): ${e.localizedMessage ?: "error"}"
            )
        } catch (e: SecurityException) {
            return ArchiveExtractionOutcome(emptyList(), skippedNonFonts, e.message ?: "Blocked unsafe archive path.")
        } catch (e: Exception) {
            return ArchiveExtractionOutcome(
                emptyList(),
                skippedNonFonts,
                "Archive extraction error: ${e.localizedMessage ?: e::class.java.simpleName}"
            )
        }

        // Recursively collect all extracted font files across nested folders
        val found = tempRoot.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in SUPPORTED_FONT_EXTS }
            .map { file ->
                val rel = file.relativeTo(tempRoot).path
                ExtractedFontCandidate(rel, file)
            }
            .toList()

        return ArchiveExtractionOutcome(
            candidates = found,
            skippedNonFonts = skippedNonFonts,
            errorMessage = if (found.isEmpty()) "No supported font files found inside $archiveName." else null
        )
    }

    private fun extractTarStream(
        input: InputStream,
        safeCreateFile: (String) -> File,
        copyBounded: (InputStream, File) -> Boolean,
        onEntrySeen: (Boolean) -> Unit
    ) {
        TarArchiveInputStream(input).use { tis ->
            while (true) {
                @Suppress("DEPRECATION")
                val entry = tis.nextTarEntry ?: break
                if (entry.isDirectory) continue
                val ext = entry.name.substringAfterLast('.', "").lowercase()
                val isFont = ext in SUPPORTED_FONT_EXTS
                onEntrySeen(isFont)
                if (!isFont) continue
                val target = safeCreateFile(entry.name)
                copyBounded(tis, target)
            }
        }
    }
}
