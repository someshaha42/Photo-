package com.example.fonts

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.zip.Inflater

data class ParsedFontInspection(
    val isValid: Boolean,
    val displayName: String,
    val format: FontFormat,
    val tableTags: List<String>,
    val glyphCount: Int,
    val unicodeCoverageCount: Int,
    val hasEmojiCoverage: Boolean,
    val colorTableType: ColorTableType,
    val bitmapStrikePpem: List<Int>,
    val classification: FontClassification,
    val warnings: List<String>,
    val sha256: String,
    val unwrappedSfntBytes: ByteArray? = null,
    val errorReason: String? = null
)

object TrueTypeTableParser {

    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val out = CharArray(digest.size * 2)
        for (i in digest.indices) {
            val v = digest[i].toInt() and 0xFF
            out[i * 2] = HEX_CHARS[v ushr 4]
            out[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        return String(out)
    }

    fun inspectBytes(bytes: ByteArray, fallbackName: String): ParsedFontInspection {
        val sha256 = sha256Hex(bytes)
        if (bytes.size < 16) {
            return invalid(fallbackName, sha256, "File too small to be a valid font (${bytes.size} bytes).")
        }

        val magic = readTag(bytes, 0)
        val extFormat = guessFormatByMagicAndExt(magic, fallbackName)
        var warnings = mutableListOf<String>()
        var sfntBytes: ByteArray = bytes

        when (magic) {
            "wOFF" -> {
                val unwrapped = runCatching { unwrapWoff1(bytes) }.getOrNull()
                if (unwrapped == null) {
                    return invalid(fallbackName, sha256, "Corrupted or unsupported WOFF1 font container.")
                }
                sfntBytes = unwrapped
            }
            "wOF2" -> {
                return ParsedFontInspection(
                    isValid = false,
                    displayName = cleanFallbackName(fallbackName),
                    format = FontFormat.WOFF2,
                    tableTags = emptyList(),
                    glyphCount = 0,
                    unicodeCoverageCount = 0,
                    hasEmojiCoverage = false,
                    colorTableType = ColorTableType.NONE,
                    bitmapStrikePpem = emptyList(),
                    classification = FontClassification.UNSUPPORTED,
                    warnings = listOf("WOFF2 uses transformed Brotli sfnt streams; please convert to TTF/OTF/WOFF1 for native Skia rendering."),
                    sha256 = sha256,
                    errorReason = "WOFF2 table reconstruction is not supported natively on Android."
                )
            }
            "ttcf" -> {
                val offset = parseTtcFirstFontOffset(bytes)
                if (offset < 0 || offset + 12 >= bytes.size) {
                    return invalid(fallbackName, sha256, "Invalid TrueType Collection (TTC/OTC) header.")
                }
                return inspectSfntAtOffset(bytes, offset, extFormat, fallbackName, sha256, warnings, null)
            }
        }

        val sfntTag = readUInt32(sfntBytes, 0)
        val isSfnt = sfntTag == 0x00010000L || magic == "OTTO" || magic == "true" || magic == "typ1" || extFormat == FontFormat.WOFF
        if (!isSfnt) {
            return invalid(fallbackName, sha256, "Unrecognized font header signature ($magic).")
        }

        return inspectSfntAtOffset(
            sfntBytes,
            0,
            extFormat,
            fallbackName,
            sha256,
            warnings,
            if (magic == "wOFF") sfntBytes else null
        )
    }

    private fun inspectSfntAtOffset(
        bytes: ByteArray,
        sfntOffset: Int,
        format: FontFormat,
        fallbackName: String,
        sha256: String,
        warnings: MutableList<String>,
        unwrappedSfntBytes: ByteArray?
    ): ParsedFontInspection {
        if (sfntOffset + 12 > bytes.size) {
            return invalid(fallbackName, sha256, "Truncated SFNT header.")
        }
        val numTables = readUInt16(bytes, sfntOffset + 4)
        if (numTables <= 0 || numTables > 200 || sfntOffset + 12 + numTables * 16 > bytes.size) {
            return invalid(fallbackName, sha256, "Invalid SFNT table directory ($numTables tables).")
        }

        val tables = LinkedHashMap<String, Pair<Int, Int>>()
        for (i in 0 until numTables) {
            val recOffset = sfntOffset + 12 + i * 16
            val tag = readTag(bytes, recOffset)
            val offset = readUInt32(bytes, recOffset + 8).toInt()
            val length = readUInt32(bytes, recOffset + 12).toInt()
            if (offset >= 0 && length >= 0 && offset.toLong() + length.toLong() <= bytes.size.toLong()) {
                tables[tag] = offset to length
            }
        }

        val tableTags = tables.keys.toList()
        if (!tables.containsKey("head") || !tables.containsKey("maxp")) {
            warnings.add("Missing standard head/maxp tables; font compatibility may be limited.")
        }

        val glyphCount = tables["maxp"]?.let { (off, len) ->
            if (len >= 6) readUInt16(bytes, off + 4) else 0
        } ?: 0

        val familyName = tables["name"]?.let { (off, len) ->
            parseNameTableFamily(bytes, off, len)
        }?.takeIf { it.isNotBlank() } ?: cleanFallbackName(fallbackName)

        val (unicodeCount, hasEmojiRange) = tables["cmap"]?.let { (off, len) ->
            parseCmapCoverage(bytes, off, len)
        } ?: (0 to false)

        val hasColr = tables.containsKey("COLR") && tables.containsKey("CPAL")
        val hasCbdt = tables.containsKey("CBDT") && tables.containsKey("CBLC")
        val hasSbix = tables.containsKey("sbix")
        val hasSvg = tables.containsKey("SVG ")

        val colorTableType = when {
            hasColr -> ColorTableType.COLR_CPAL
            hasCbdt -> ColorTableType.CBDT_CBLC
            hasSbix -> ColorTableType.SBIX
            hasSvg -> ColorTableType.SVG_IN_OT
            else -> ColorTableType.NONE
        }

        val strikes = mutableListOf<Int>()
        if (hasCbdt) {
            tables["CBLC"]?.let { (off, len) ->
                strikes.addAll(parseCblcStrikes(bytes, off, len))
            }
        }
        if (hasSbix) {
            tables["sbix"]?.let { (off, len) ->
                strikes.addAll(parseSbixStrikes(bytes, off, len))
            }
            warnings.add("Apple sbix bitmap emoji table detected; rendering uses highest available bitmap strike on Android.")
        }
        if (hasSvg) {
            warnings.add("SVG-in-OpenType table detected; Android Skia renders fallback glyphs if SVG table isn't rasterized natively.")
        }
        if (bytes.size > 25 * 1024 * 1024) {
            warnings.add("Large font file (${bytes.size / (1024 * 1024)} MB); cached glyph rendering keeps memory bounded.")
        }

        val lowerName = familyName.lowercase()
        val isEmojiNamed = lowerName.contains("emoji") || lowerName.contains("apple color")
        val classification = when {
            colorTableType != ColorTableType.NONE && unicodeCount > 1200 && !isEmojiNamed ->
                FontClassification.MIXED_TEXT_COLOR_EMOJI
            colorTableType != ColorTableType.NONE || isEmojiNamed ->
                FontClassification.COLOR_EMOJI
            hasEmojiRange && unicodeCount < 600 ->
                FontClassification.SYMBOL_UNICODE
            lowerName.contains("symbol") || lowerName.contains("icon") || lowerName.contains("dingbat") ->
                FontClassification.SYMBOL_UNICODE
            else -> FontClassification.STANDARD_TEXT
        }

        val resolvedFormat = when {
            format != FontFormat.UNKNOWN -> format
            tables.containsKey("CFF ") || tables.containsKey("CFF2") -> FontFormat.OTF
            else -> FontFormat.TTF
        }

        return ParsedFontInspection(
            isValid = true,
            displayName = familyName,
            format = resolvedFormat,
            tableTags = tableTags,
            glyphCount = glyphCount,
            unicodeCoverageCount = unicodeCount,
            hasEmojiCoverage = hasEmojiRange || colorTableType != ColorTableType.NONE,
            colorTableType = colorTableType,
            bitmapStrikePpem = strikes.distinct().sorted(),
            classification = classification,
            warnings = warnings,
            sha256 = sha256,
            unwrappedSfntBytes = unwrappedSfntBytes
        )
    }

    private fun parseTtcFirstFontOffset(bytes: ByteArray): Int {
        if (bytes.size < 16) return -1
        val numFonts = readUInt32(bytes, 8).toInt()
        if (numFonts <= 0) return -1
        return readUInt32(bytes, 12).toInt()
    }

    /**
     * Unwraps WOFF1 (RFC 1952 zlib compressed tables) into a clean TTF/OTF SFNT byte array
     * so Android's Typeface / Skia / HarfBuzz / FreeType pipeline renders it at full vector resolution!
     */
    private fun unwrapWoff1(woff: ByteArray): ByteArray? {
        if (woff.size < 44) return null
        val flavor = readUInt32(woff, 4)
        val numTables = readUInt16(woff, 12)
        if (numTables <= 0 || numTables > 200 || 44 + numTables * 20 > woff.size) return null

        data class WoffRec(
            val tag: ByteArray,
            val offset: Int,
            val compLen: Int,
            val origLen: Int,
            val checksum: Int
        )

        val records = ArrayList<WoffRec>(numTables)
        for (i in 0 until numTables) {
            val base = 44 + i * 20
            val tag = woff.copyOfRange(base, base + 4)
            val offset = readUInt32(woff, base + 4).toInt()
            val compLen = readUInt32(woff, base + 8).toInt()
            val origLen = readUInt32(woff, base + 12).toInt()
            val checksum = readUInt32(woff, base + 16).toInt()
            if (offset < 0 || compLen < 0 || origLen < 0 || origLen > 64 * 1024 * 1024) return null
            if (offset.toLong() + compLen.toLong() > woff.size.toLong()) return null
            records.add(WoffRec(tag, offset, compLen, origLen, checksum))
        }

        val out = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN)
        header.putInt(flavor.toInt())
        header.putShort(numTables.toShort())
        val maxPow2 = Integer.highestOneBit(numTables)
        val searchRange = maxPow2 * 16
        val entrySelector = Integer.numberOfTrailingZeros(maxPow2)
        val rangeShift = numTables * 16 - searchRange
        header.putShort(searchRange.toShort())
        header.putShort(entrySelector.toShort())
        header.putShort(rangeShift.toShort())
        out.write(header.array())

        val tableDataList = ArrayList<ByteArray>(numTables)
        for (rec in records) {
            val data = if (rec.compLen == rec.origLen) {
                woff.copyOfRange(rec.offset, rec.offset + rec.compLen)
            } else {
                val inflater = Inflater()
                try {
                    inflater.setInput(woff, rec.offset, rec.compLen)
                    val decompressed = ByteArray(rec.origLen)
                    var count = 0
                    while (!inflater.finished() && count < rec.origLen) {
                        val n = inflater.inflate(decompressed, count, rec.origLen - count)
                        if (n == 0) break
                        count += n
                    }
                    if (count != rec.origLen) return null
                    decompressed
                } finally {
                    inflater.end()
                }
            }
            tableDataList.add(data)
        }

        var currentOffset = 12 + numTables * 16
        for (i in 0 until numTables) {
            val rec = records[i]
            val dir = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
            dir.put(rec.tag)
            dir.putInt(rec.checksum)
            dir.putInt(currentOffset)
            dir.putInt(rec.origLen)
            out.write(dir.array())
            val paddedLen = (rec.origLen + 3) and -4
            currentOffset += paddedLen
        }

        for (i in 0 until numTables) {
            val data = tableDataList[i]
            out.write(data)
            val pad = (4 - (data.size % 4)) % 4
            repeat(pad) { out.write(0) }
        }

        return out.toByteArray()
    }

    private fun parseNameTableFamily(bytes: ByteArray, offset: Int, length: Int): String? {
        if (length < 6) return null
        val count = readUInt16(bytes, offset + 2)
        val stringOffset = readUInt16(bytes, offset + 4)
        val storageStart = offset + stringOffset
        var fallbackMac: String? = null
        for (i in 0 until count) {
            val rec = offset + 6 + i * 12
            if (rec + 12 > offset + length) break
            val platformId = readUInt16(bytes, rec)
            val nameId = readUInt16(bytes, rec + 6)
            val strLen = readUInt16(bytes, rec + 8)
            val strOff = readUInt16(bytes, rec + 10)
            if (nameId != 1 && nameId != 4 && nameId != 16) continue
            val start = storageStart + strOff
            if (start < 0 || start + strLen > bytes.size) continue
            if (platformId == 3 || platformId == 0) {
                val decoded = String(bytes, start, strLen, StandardCharsets.UTF_16BE).trim('\u0000', ' ')
                if (decoded.isNotBlank() && nameId == 1) return decoded
                if (decoded.isNotBlank() && fallbackMac == null) fallbackMac = decoded
            } else if (platformId == 1 && fallbackMac == null) {
                val decoded = String(bytes, start, strLen, StandardCharsets.ISO_8859_1).trim('\u0000', ' ')
                if (decoded.isNotBlank()) fallbackMac = decoded
            }
        }
        return fallbackMac
    }

    private fun parseCmapCoverage(bytes: ByteArray, offset: Int, length: Int): Pair<Int, Boolean> {
        if (length < 4) return 0 to false
        val numSubtables = readUInt16(bytes, offset + 2)
        var bestCount = 0
        var hasEmoji = false
        for (i in 0 until numSubtables) {
            val rec = offset + 4 + i * 8
            if (rec + 8 > offset + length) break
            val subOff = offset + readUInt32(bytes, rec + 4).toInt()
            if (subOff + 4 > bytes.size || subOff < offset) continue
            val format = readUInt16(bytes, subOff)
            if (format == 4 && subOff + 14 <= bytes.size) {
                val segCount = readUInt16(bytes, subOff + 6) / 2
                val endCodesOff = subOff + 14
                val startCodesOff = endCodesOff + segCount * 2 + 2
                if (startCodesOff + segCount * 2 <= bytes.size) {
                    var total = 0
                    for (s in 0 until segCount) {
                        val startCode = readUInt16(bytes, startCodesOff + s * 2)
                        val endCode = readUInt16(bytes, endCodesOff + s * 2)
                        if (endCode >= startCode && endCode != 0xFFFF) {
                            total += (endCode - startCode + 1)
                            if (startCode in 0x2600..0x27BF || endCode in 0x2600..0x27BF) hasEmoji = true
                        }
                    }
                    if (total > bestCount) bestCount = total
                }
            } else if (format == 12 && subOff + 16 <= bytes.size) {
                val numGroups = readUInt32(bytes, subOff + 12).toInt().coerceAtMost(20000)
                var total = 0
                for (g in 0 until numGroups) {
                    val gOff = subOff + 16 + g * 12
                    if (gOff + 12 > bytes.size) break
                    val startChar = readUInt32(bytes, gOff)
                    val endChar = readUInt32(bytes, gOff + 4)
                    if (endChar >= startChar) {
                        total += (endChar - startChar + 1).toInt().coerceAtMost(100000)
                        if (endChar >= 0x1F300L && startChar <= 0x1FAFFL) {
                            hasEmoji = true
                        }
                    }
                }
                if (total > bestCount) bestCount = total
            }
        }
        return bestCount to hasEmoji
    }

    private fun parseCblcStrikes(bytes: ByteArray, offset: Int, length: Int): List<Int> {
        if (length < 8) return emptyList()
        val numSizes = readUInt32(bytes, offset + 4).toInt().coerceIn(0, 32)
        val ppems = mutableListOf<Int>()
        for (i in 0 until numSizes) {
            val sizeRec = offset + 8 + i * 48
            if (sizeRec + 48 > offset + length) break
            val ppemX = bytes[sizeRec + 44].toInt() and 0xFF
            if (ppemX > 0) ppems.add(ppemX)
        }
        return ppems
    }

    private fun parseSbixStrikes(bytes: ByteArray, offset: Int, length: Int): List<Int> {
        if (length < 8) return emptyList()
        val numStrikes = readUInt32(bytes, offset + 4).toInt().coerceIn(0, 32)
        val ppems = mutableListOf<Int>()
        for (i in 0 until numStrikes) {
            val ptrOff = offset + 8 + i * 4
            if (ptrOff + 4 > offset + length) break
            val strikeOff = offset + readUInt32(bytes, ptrOff).toInt()
            if (strikeOff + 4 <= offset + length) {
                val ppem = readUInt16(bytes, strikeOff)
                if (ppem > 0) ppems.add(ppem)
            }
        }
        return ppems
    }

    private fun guessFormatByMagicAndExt(magic: String, name: String): FontFormat {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when {
            magic == "wOFF" || ext == "woff" -> FontFormat.WOFF
            magic == "wOF2" || ext == "woff2" -> FontFormat.WOFF2
            magic == "ttcf" && ext == "otc" -> FontFormat.OTC
            magic == "ttcf" -> FontFormat.TTC
            magic == "OTTO" || ext == "otf" -> FontFormat.OTF
            ext == "ttf" -> FontFormat.TTF
            else -> FontFormat.UNKNOWN
        }
    }

    private fun cleanFallbackName(name: String): String {
        return name.substringAfterLast('/').substringBeforeLast('.')
            .replace('_', ' ').replace('-', ' ').trim().ifBlank { "Custom Font" }
    }

    private fun invalid(name: String, sha256: String, reason: String) = ParsedFontInspection(
        isValid = false,
        displayName = cleanFallbackName(name),
        format = FontFormat.UNKNOWN,
        tableTags = emptyList(),
        glyphCount = 0,
        unicodeCoverageCount = 0,
        hasEmojiCoverage = false,
        colorTableType = ColorTableType.NONE,
        bitmapStrikePpem = emptyList(),
        classification = FontClassification.UNSUPPORTED,
        warnings = listOf(reason),
        sha256 = sha256,
        errorReason = reason
    )

    private fun readTag(bytes: ByteArray, off: Int): String {
        if (off + 4 > bytes.size) return "????"
        return String(bytes, off, 4, StandardCharsets.US_ASCII)
    }

    private fun readUInt16(bytes: ByteArray, off: Int): Int {
        return ((bytes[off].toInt() and 0xFF) shl 8) or (bytes[off + 1].toInt() and 0xFF)
    }

    private fun readUInt32(bytes: ByteArray, off: Int): Long {
        return ((bytes[off].toLong() and 0xFF) shl 24) or
            ((bytes[off + 1].toLong() and 0xFF) shl 16) or
            ((bytes[off + 2].toLong() and 0xFF) shl 8) or
            (bytes[off + 3].toLong() and 0xFF)
    }
}
