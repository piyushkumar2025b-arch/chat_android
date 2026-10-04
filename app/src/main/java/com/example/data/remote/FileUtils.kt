package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.model.AttachmentInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.Inflater
import java.util.zip.ZipInputStream

data class DeviceStorageStats(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedPercent: Int,
    val totalFormatted: String,
    val availableFormatted: String,
    val cachedAttachmentsCount: Int,
    val cachedAttachmentsBytes: Long,
    val cachedAttachmentsFormatted: String
)

object FileUtils {

    suspend fun processPickedUri(context: Context, uri: Uri): AttachmentInfo? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            var displayName = "file_${System.currentTimeMillis()}"
            var sizeBytes = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        val retrieved = cursor.getString(nameIndex)
                        if (!retrieved.isNullOrBlank()) {
                            displayName = retrieved
                        }
                    }
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }

            if (displayName.startsWith("file_") && !uri.lastPathSegment.isNullOrBlank()) {
                val segment = uri.lastPathSegment!!.substringAfterLast('/')
                if (segment.isNotBlank()) displayName = segment
            }

            // Save to local cache directory so the app retains access across sessions
            val cacheFolder = File(context.filesDir, "chat_attachments").apply { mkdirs() }
            val originalExt = displayName.substringAfterLast('.', "").lowercase()

            val tempFile = File(cacheFolder, "${UUID.randomUUID()}_temp")
            val maxBytes = 50 * 1024 * 1024L // 50MB safe limit
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            var totalBytes = 0L
            val buffer = ByteArray(8192)

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        totalBytes += read
                        if (totalBytes > maxBytes) {
                            try { tempFile.delete() } catch (_: Exception) {}
                            return@withContext null
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }

            if (!tempFile.exists() || tempFile.length() <= 0L) {
                try { tempFile.delete() } catch (_: Exception) {}
                return@withContext null
            }

            val rawMime = contentResolver.getType(uri)
            val detectedType = detectFileType(tempFile, displayName, rawMime)
            val finalExt = (if (originalExt.isNotEmpty()) originalExt else detectedType.extension).filter { it.isLetterOrDigit() }
            val localFileName = "${UUID.randomUUID()}${if (finalExt.isNotEmpty()) ".$finalExt" else ""}"
            val targetFile = File(cacheFolder, localFileName)
            
            val moved = tempFile.renameTo(targetFile)
            if (!moved) {
                tempFile.copyTo(targetFile, overwrite = true)
                try { tempFile.delete() } catch (_: Exception) {}
            }

            if (sizeBytes <= 0) {
                sizeBytes = targetFile.length()
            }

            if (!displayName.contains('.') && finalExt.isNotEmpty()) {
                displayName = "$displayName.$finalExt"
            }

            val mimeType = if (detectedType.mimeType.isNotBlank() && detectedType.mimeType != "application/octet-stream") {
                detectedType.mimeType
            } else {
                resolveMimeType(displayName, rawMime, uri)
            }

            val isImage = mimeType.startsWith("image/") || isImageExtension(displayName)

            // Generate preview snippet for documents, data, code or text files
            var snippet: String? = null
            if (!isImage) {
                snippet = readTextSnippet(targetFile, maxChars = 300)
            }

            AttachmentInfo(
                id = UUID.randomUUID().toString(),
                name = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                localUri = targetFile.absolutePath,
                isImage = isImage,
                previewSnippet = snippet
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class DetectedTypeInfo(
        val extension: String,
        val mimeType: String,
        val description: String
    )

    fun detectFileType(file: File, fileName: String, rawMime: String?): DetectedTypeInfo {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isNotEmpty()) {
            val resolvedMime = resolveMimeType(fileName, rawMime, null)
            return DetectedTypeInfo(ext, resolvedMime, ext.uppercase())
        }

        // Magic bytes detection
        return try {
            val header = ByteArray(32)
            file.inputStream().use { it.read(header) }

            if (header.size >= 4 && header[0] == 0x25.toByte() && header[1] == 0x50.toByte() && header[2] == 0x44.toByte() && header[3] == 0x46.toByte()) {
                DetectedTypeInfo("pdf", "application/pdf", "PDF Document")
            } else if (header.size >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) {
                // ZIP container: Check if docx, xlsx, pptx or regular zip
                inspectZipType(file)
            } else if (header.size >= 8 && header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()) {
                DetectedTypeInfo("png", "image/png", "PNG Image")
            } else if (header.size >= 3 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
                DetectedTypeInfo("jpg", "image/jpeg", "JPEG Image")
            } else if (header.size >= 4 && header[0] == 0x47.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x38.toByte()) {
                DetectedTypeInfo("gif", "image/gif", "GIF Image")
            } else if (header.size >= 12 && String(header, 0, 4, Charsets.US_ASCII) == "RIFF" && String(header, 8, 4, Charsets.US_ASCII) == "WEBP") {
                DetectedTypeInfo("webp", "image/webp", "WEBP Image")
            } else if (header.size >= 16 && String(header, 0, 16, Charsets.US_ASCII).startsWith("SQLite format 3")) {
                DetectedTypeInfo("db", "application/x-sqlite3", "SQLite Database")
            } else {
                val fallbackExt = if (!rawMime.isNullOrBlank()) {
                    when {
                        rawMime.contains("pdf") -> "pdf"
                        rawMime.contains("csv") -> "csv"
                        rawMime.contains("json") -> "json"
                        rawMime.contains("word") -> "docx"
                        rawMime.contains("sheet") -> "xlsx"
                        rawMime.contains("presentation") -> "pptx"
                        rawMime.startsWith("image/") -> rawMime.substringAfter("image/")
                        rawMime.startsWith("text/") -> "txt"
                        else -> "bin"
                    }
                } else "txt"
                DetectedTypeInfo(fallbackExt, rawMime ?: "text/plain", fallbackExt.uppercase())
            }
        } catch (_: Exception) {
            DetectedTypeInfo("txt", "text/plain", "Text")
        }
    }

    private fun inspectZipType(file: File): DetectedTypeInfo {
        return try {
            val zip = ZipInputStream(file.inputStream())
            var entry = zip.nextEntry
            var isDocx = false
            var isXlsx = false
            var isPptx = false
            var count = 0

            while (entry != null && count < 20) {
                val name = entry.name
                if (name.startsWith("word/")) {
                    isDocx = true; break
                } else if (name.startsWith("xl/")) {
                    isXlsx = true; break
                } else if (name.startsWith("ppt/")) {
                    isPptx = true; break
                }
                entry = zip.nextEntry
                count++
            }
            zip.close()

            when {
                isDocx -> DetectedTypeInfo("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word DOCX")
                isXlsx -> DetectedTypeInfo("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel XLSX")
                isPptx -> DetectedTypeInfo("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "PowerPoint PPTX")
                else -> DetectedTypeInfo("zip", "application/zip", "ZIP Archive")
            }
        } catch (_: Exception) {
            DetectedTypeInfo("zip", "application/zip", "ZIP Archive")
        }
    }

    fun isImageExtension(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg")
    }

    fun isMediaExtension(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "mp4", "mkv", "mov", "webm", "3gp")
    }

    fun resolveMimeType(fileName: String, contentResolverMime: String?, uri: Uri?): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc" -> "application/msword"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> "application/vnd.ms-powerpoint"
            "odt" -> "application/vnd.oasis.opendocument.text"
            "ods" -> "application/vnd.oasis.opendocument.spreadsheet"
            "odp" -> "application/vnd.oasis.opendocument.presentation"
            "rtf" -> "application/rtf"
            "txt" -> "text/plain"
            "md", "markdown" -> "text/markdown"
            "csv" -> "text/csv"
            "tsv" -> "text/tab-separated-values"
            "json", "jsonl", "geojson" -> "application/json"
            "xml" -> "text/xml"
            "html", "htm" -> "text/html"
            "yaml", "yml" -> "text/yaml"
            "toml" -> "text/x-toml"
            "py" -> "text/x-python"
            "kt", "kts" -> "text/x-kotlin"
            "java" -> "text/x-java"
            "js", "jsx", "mjs" -> "text/javascript"
            "ts", "tsx" -> "text/typescript"
            "c", "cpp", "h", "hpp", "cc", "cxx" -> "text/x-c"
            "cs" -> "text/x-csharp"
            "go" -> "text/x-go"
            "rs" -> "text/x-rust"
            "rb" -> "text/x-ruby"
            "php" -> "text/x-php"
            "swift" -> "text/x-swift"
            "sql" -> "text/x-sql"
            "sh", "bash", "zsh" -> "text/x-shellscript"
            "properties", "env", "ini", "conf", "gradle", "log" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "svg" -> "image/svg+xml"
            "heic", "heif" -> "image/heic"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "mp4" -> "video/mp4"
            "zip" -> "application/zip"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"
            else -> {
                if (!contentResolverMime.isNullOrBlank() && contentResolverMime != "application/octet-stream") {
                    contentResolverMime
                } else {
                    "application/octet-stream"
                }
            }
        }
    }

    private suspend fun readTextSnippet(file: File, maxChars: Int): String? = withContext(Dispatchers.IO) {
        try {
            val content = readFullTextContent(file, maxChars = maxChars + 100)
            if (content.isBlank()) return@withContext null
            if (content.length > maxChars) {
                content.take(maxChars).trim() + "..."
            } else {
                content.trim()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Comprehensive multi-format text & data extractor.
     * Supports: PDF, DOCX, DOC, XLSX, XLS, PPTX, PPT, CSV, TSV, JSON, XML, HTML, RTF, Code, TXT, MD, ZIP, Audio/Video.
     */
    suspend fun readFullTextContent(file: File, maxChars: Int = 120_000): String = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext "Error: File does not exist on disk"
            val extension = file.name.substringAfterLast('.', "").lowercase()

            val extracted = when (extension) {
                "pdf" -> extractTextFromPdf(file)
                "docx" -> extractTextFromDocx(file)
                "doc" -> extractTextFromLegacyDoc(file)
                "xlsx" -> extractTextFromXlsx(file)
                "xls" -> extractTextFromLegacyXls(file)
                "pptx" -> extractTextFromPptx(file)
                "ppt" -> extractTextFromLegacyDoc(file)
                "odt" -> extractTextFromDocx(file) // ODT uses content.xml which is handled similarly
                "ods" -> extractTextFromXlsx(file)
                "odp" -> extractTextFromPptx(file)
                "rtf" -> extractTextFromRtf(file)
                "csv" -> formatCsvContent(file)
                "tsv" -> formatTsvContent(file)
                "json", "jsonl", "geojson" -> formatJsonContent(file)
                "xml", "html", "htm", "svg" -> extractTextFromXmlOrHtml(file)
                "zip", "jar" -> inspectZipArchive(file)
                "mp3", "wav", "m4a", "aac", "ogg", "mp4", "mkv", "mov" -> extractMediaMetadata(file)
                else -> {
                    // Check magic bytes if extension is unknown or bin
                    val detected = detectFileType(file, file.name, null)
                    when (detected.extension) {
                        "pdf" -> extractTextFromPdf(file)
                        "docx" -> extractTextFromDocx(file)
                        "xlsx" -> extractTextFromXlsx(file)
                        "pptx" -> extractTextFromPptx(file)
                        "zip" -> inspectZipArchive(file)
                        else -> readTextWithCharsetFallback(file)
                    }
                }
            }

            if (extracted.length > maxChars) {
                extracted.take(maxChars) + "\n\n... [Content truncated due to length limits: ${formatFileSize(file.length())}]"
            } else {
                extracted
            }
        } catch (e: Exception) {
            "Unable to parse file data (${file.name}): ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    /**
     * Extracts text from PDF files using FlateDecode decompression, standard text operators, and PdfRenderer.
     */
    fun extractTextFromPdf(file: File): String {
        val sb = StringBuilder()
        var pageCount = 0

        // Determine page count via native Android PdfRenderer
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            pageCount = renderer.pageCount
            renderer.close()
            pfd.close()
        } catch (_: Exception) {}

        try {
            val bytes = file.readBytes()

            // 1. First, search for and decompress FlateDecode streams
            val decompressedStreams = mutableListOf<String>()
            val streamMarker = "stream".toByteArray(Charsets.US_ASCII)
            val endStreamMarker = "endstream".toByteArray(Charsets.US_ASCII)

            var searchIdx = 0
            while (searchIdx < bytes.size - 10) {
                val streamStart = indexOfByteArray(bytes, streamMarker, searchIdx)
                if (streamStart == -1) break

                val contentStart = skipStreamNewline(bytes, streamStart + streamMarker.size)
                val streamEnd = indexOfByteArray(bytes, endStreamMarker, contentStart)
                if (streamEnd == -1) break

                // Check preceding dictionary for /FlateDecode
                val dictLookbackStart = (streamStart - 300).coerceAtLeast(0)
                val dictString = String(bytes, dictLookbackStart, streamStart - dictLookbackStart, Charsets.ISO_8859_1)

                val streamBytes = bytes.copyOfRange(contentStart, streamEnd)

                if (dictString.contains("/FlateDecode") || dictString.contains("/Fl")) {
                    try {
                        val inflater = Inflater(false)
                        inflater.setInput(streamBytes)
                        val outBuf = ByteArray(8192)
                        val outStream = ByteArrayOutputStream()
                        while (!inflater.finished() && !inflater.needsInput()) {
                            val count = inflater.inflate(outBuf)
                            if (count > 0) outStream.write(outBuf, 0, count)
                            else break
                        }
                        inflater.end()
                        val decompressed = String(outStream.toByteArray(), Charsets.ISO_8859_1)
                        if (decompressed.isNotBlank()) {
                            decompressedStreams.add(decompressed)
                        }
                    } catch (_: Exception) {}
                } else {
                    // Uncompressed stream
                    val uncompressed = String(streamBytes, Charsets.ISO_8859_1)
                    if (uncompressed.contains("BT") && uncompressed.contains("ET")) {
                        decompressedStreams.add(uncompressed)
                    }
                }

                searchIdx = streamEnd + endStreamMarker.size
            }

            // Also include raw text for any uncompressed objects
            val raw = String(bytes, Charsets.ISO_8859_1)
            decompressedStreams.add(raw)

            val textPieces = mutableListOf<String>()

            for (textBlock in decompressedStreams) {
                val btMatches = Regex("BT[\\s\\S]*?ET").findAll(textBlock)
                for (bt in btMatches) {
                    val block = bt.value

                    // Match strings like (Hello World) Tj or (Text) ' or (Text) "
                    val tjMatches = Regex("\\((.*?)\\)\\s*(?:Tj|'|\")").findAll(block)
                    for (m in tjMatches) {
                        val decoded = decodePdfString(m.groupValues[1])
                        if (decoded.isNotBlank()) textPieces.add(decoded)
                    }

                    // Match array forms [(Part1) 10 (Part2)] TJ
                    val tjArrayMatches = Regex("\\[(.*?)\\]\\s*TJ").findAll(block)
                    for (m in tjArrayMatches) {
                        val inner = m.groupValues[1]
                        val parts = Regex("\\((.*?)\\)").findAll(inner).map { decodePdfString(it.groupValues[1]) }.joinToString("")
                        if (parts.isNotBlank()) textPieces.add(parts)
                    }

                    // Match hex encoded strings: <48656C6C6F> Tj
                    val hexMatches = Regex("<([0-9a-fA-F]+)>\\s*(?:Tj|'|\")").findAll(block)
                    for (m in hexMatches) {
                        val decoded = decodePdfHexString(m.groupValues[1])
                        if (decoded.isNotBlank()) textPieces.add(decoded)
                    }
                }
            }

            if (textPieces.isNotEmpty()) {
                sb.append("[PDF Document: ${file.name} • Total Pages: ${if (pageCount > 0) pageCount else "1+"}]\n\n")
                // Clean up spacing and join text
                val joined = textPieces.joinToString(" ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                sb.append(joined)
            }
        } catch (_: Exception) {}

        if (sb.length > 60) {
            return sb.toString().trim()
        }

        // Fallback for visual/scanned PDF
        val pagesInfo = if (pageCount > 0) "$pageCount page(s)" else "available pages"
        return "[PDF Document: ${file.name} • Size: ${formatFileSize(file.length())} • $pagesInfo]\n" +
                "Scanned or visual PDF layout detected. Page imagery is pre-rendered for multimodal vision analysis."
    }

    private fun indexOfByteArray(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || fromIndex >= source.size) return -1
        outer@ for (i in fromIndex..(source.size - target.size)) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun skipStreamNewline(bytes: ByteArray, offset: Int): Int {
        var idx = offset
        while (idx < bytes.size && (bytes[idx] == 0x0D.toByte() || bytes[idx] == 0x0A.toByte() || bytes[idx] == 0x20.toByte())) {
            idx++
        }
        return idx
    }

    private fun decodePdfString(raw: String): String {
        return raw.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\b", "\b")
            .replace("\\f", "")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
    }

    private fun decodePdfHexString(hex: String): String {
        val clean = if (hex.length % 2 != 0) hex + "0" else hex
        val sb = StringBuilder()
        var i = 0
        while (i < clean.length) {
            val b = clean.substring(i, i + 2).toIntOrNull(16) ?: break
            if (b in 32..126 || b == 10 || b == 13 || b == 9) {
                sb.append(b.toChar())
            }
            i += 2
        }
        return sb.toString()
    }

    /**
     * Renders a PDF page to a Bitmap using Android's native PdfRenderer,
     * compressing it to Base64 JPEG for multimodal LLM vision input.
     */
    fun convertPdfPageToBase64(file: File, pageIndex: Int = 0): String? {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex < renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                val width = (page.width * 1.5f).toInt().coerceAtMost(1600)
                val height = (page.height * 1.5f).toInt().coerceAtMost(2200)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val bytes = outputStream.toByteArray()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts text from Microsoft Word documents (.docx) including body, tables, and notes.
     */
    fun extractTextFromDocx(file: File): String {
        return try {
            val zip = ZipInputStream(file.inputStream())
            var entry = zip.nextEntry
            val sb = StringBuilder()
            var found = false

            while (entry != null) {
                val name = entry.name
                if (name == "word/document.xml" || name == "content.xml" || name.startsWith("word/header") || name.startsWith("word/footer")) {
                    found = true
                    val xml = zip.bufferedReader(Charsets.UTF_8).readText()

                    // Parse paragraphs and tables
                    val paragraphs = Regex("<w:p[ >]([\\s\\S]*?)</w:p>").findAll(xml)
                    for (p in paragraphs) {
                        val pXml = p.value
                        val tMatches = Regex("<w:t[^>]*>([\\s\\S]*?)</w:t>").findAll(pXml)
                        val pText = tMatches.map { unescapeXml(it.groupValues[1]) }.joinToString("")
                        if (pText.isNotBlank()) {
                            sb.append(pText).append("\n\n")
                        }
                    }

                    // Fallback for ODT content.xml (<text:p> and <text:h>)
                    if (name == "content.xml") {
                        val odtMatches = Regex("<text:[ph][^>]*>([\\s\\S]*?)</text:[ph]>").findAll(xml)
                        for (m in odtMatches) {
                            val plain = stripXmlTags(m.groupValues[1])
                            if (plain.isNotBlank()) sb.append(plain).append("\n\n")
                        }
                    }
                }
                entry = zip.nextEntry
            }
            zip.close()

            if (found && sb.isNotBlank()) {
                "[Word Document: ${file.name}]\n\n${sb.toString().trim()}"
            } else {
                readTextWithCharsetFallback(file)
            }
        } catch (e: Exception) {
            "Unable to parse DOCX: ${e.localizedMessage}"
        }
    }

    /**
     * Extracts text from Microsoft Excel spreadsheets (.xlsx) parsing sharedStrings AND sheet cells!
     */
    fun extractTextFromXlsx(file: File): String {
        return try {
            val zip = ZipInputStream(file.inputStream())
            var entry = zip.nextEntry
            val sharedStrings = mutableListOf<String>()
            val sheetDataMap = mutableMapOf<String, String>()

            while (entry != null) {
                val name = entry.name
                if (name == "xl/sharedStrings.xml") {
                    val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                    val tMatches = Regex("<t[^>]*>([\\s\\S]*?)</t>").findAll(xml)
                    for (m in tMatches) {
                        sharedStrings.add(unescapeXml(m.groupValues[1]))
                    }
                } else if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                    val sheetName = name.substringAfterLast('/').substringBefore('.')
                    val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                    sheetDataMap[sheetName] = xml
                }
                entry = zip.nextEntry
            }
            zip.close()

            val sb = StringBuilder()
            sb.append("[Excel Spreadsheet: ${file.name}]\n\n")

            if (sheetDataMap.isNotEmpty()) {
                for ((sheetName, sheetXml) in sheetDataMap) {
                    sb.append("--- Sheet: $sheetName ---\n")
                    val rows = Regex("<row[^>]*>([\\s\\S]*?)</row>").findAll(sheetXml)
                    var rowCount = 0
                    for (row in rows) {
                        if (rowCount >= 150) {
                            sb.append("... [Sheet truncated after 150 rows]\n")
                            break
                        }
                        val cells = Regex("<c r=\"([A-Z0-9]+)\"([^>]*)>([\\s\\S]*?)</c>").findAll(row.value)
                        val rowCells = mutableListOf<String>()
                        for (c in cells) {
                            val coord = c.groupValues[1]
                            val attrs = c.groupValues[2]
                            val body = c.groupValues[3]
                            val isStringRef = attrs.contains("t=\"s\"")

                            val valueMatch = Regex("<v>([\\s\\S]*?)</v>").find(body)
                            val inlineMatch = Regex("<is><t[^>]*>([\\s\\S]*?)</t></is>").find(body)

                            val cellText = when {
                                inlineMatch != null -> unescapeXml(inlineMatch.groupValues[1])
                                isStringRef && valueMatch != null -> {
                                    val idx = valueMatch.groupValues[1].toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else valueMatch.groupValues[1]
                                }
                                valueMatch != null -> valueMatch.groupValues[1]
                                else -> ""
                            }
                            if (cellText.isNotBlank()) {
                                rowCells.add("$coord: $cellText")
                            }
                        }
                        if (rowCells.isNotEmpty()) {
                            sb.append(rowCells.joinToString(" | ")).append("\n")
                            rowCount++
                        }
                    }
                    sb.append("\n")
                }
                sb.toString().trim()
            } else if (sharedStrings.isNotEmpty()) {
                sb.append("Shared Strings (${sharedStrings.size} entries):\n")
                sharedStrings.chunked(4).forEach { row ->
                    sb.append(row.joinToString(" | ")).append("\n")
                }
                sb.toString().trim()
            } else {
                readTextWithCharsetFallback(file)
            }
        } catch (e: Exception) {
            "Unable to parse XLSX: ${e.localizedMessage}"
        }
    }

    /**
     * Extracts text from Microsoft PowerPoint presentations (.pptx).
     */
    fun extractTextFromPptx(file: File): String {
        return try {
            val zip = ZipInputStream(file.inputStream())
            var entry = zip.nextEntry
            val slides = mutableMapOf<Int, String>()

            while (entry != null) {
                val name = entry.name
                if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                    val slideNum = Regex("slide(\\d+)\\.xml").find(name)?.groupValues?.get(1)?.toIntOrNull() ?: 999
                    val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                    val tMatches = Regex("<a:t[^>]*>([\\s\\S]*?)</a:t>").findAll(xml)
                    val slideText = tMatches.map { unescapeXml(it.groupValues[1]) }.joinToString(" ")
                    if (slideText.isNotBlank()) {
                        slides[slideNum] = slideText
                    }
                }
                entry = zip.nextEntry
            }
            zip.close()

            if (slides.isNotEmpty()) {
                val sb = StringBuilder()
                sb.append("[PowerPoint Presentation: ${file.name} • ${slides.size} slides]\n\n")
                slides.keys.sorted().forEach { num ->
                    sb.append("--- Slide $num ---\n${slides[num]}\n\n")
                }
                sb.toString().trim()
            } else {
                readTextWithCharsetFallback(file)
            }
        } catch (e: Exception) {
            "Unable to parse PPTX: ${e.localizedMessage}"
        }
    }

    /**
     * Extracts readable text strings from legacy binary formats (.doc, .xls, .ppt).
     */
    fun extractTextLegacyBinary(file: File): String {
        return try {
            val bytes = file.readBytes()
            val sb = StringBuilder()
            var currentRun = StringBuilder()

            for (b in bytes) {
                val ch = b.toInt().toChar()
                if (ch in ' '..'~' || ch == '\n' || ch == '\t') {
                    currentRun.append(ch)
                } else {
                    if (currentRun.length >= 4) {
                        sb.append(currentRun.toString().trim()).append(" ")
                    }
                    currentRun = StringBuilder()
                }
            }
            if (currentRun.length >= 4) {
                sb.append(currentRun.toString().trim())
            }

            val text = sb.toString().replace(Regex("\\s+"), " ").trim()
            if (text.length > 50) {
                "[Legacy Document: ${file.name}]\n\n$text"
            } else {
                readTextWithCharsetFallback(file)
            }
        } catch (e: Exception) {
            readTextWithCharsetFallback(file)
        }
    }

    fun extractTextFromLegacyDoc(file: File): String = extractTextLegacyBinary(file)
    fun extractTextFromLegacyXls(file: File): String = extractTextLegacyBinary(file)

    /**
     * Parses Rich Text Format (.rtf) files by stripping RTF control sequences.
     */
    fun extractTextFromRtf(file: File): String {
        return try {
            val raw = readTextWithCharsetFallback(file)
            val withoutHex = Regex("\\\\'([0-9a-fA-F]{2})").replace(raw) { m ->
                val code = m.groupValues[1].toIntOrNull(16) ?: 32
                code.toChar().toString()
            }
            val withoutControls = Regex("\\\\[a-zA-Z]+-?\\d* ?").replace(withoutHex, " ")
            val clean = withoutControls
                .replace("{", "")
                .replace("}", "")
                .replace(Regex("\\s+"), " ")
                .trim()
            "[RTF Document: ${file.name}]\n\n$clean"
        } catch (e: Exception) {
            readTextWithCharsetFallback(file)
        }
    }

    /**
     * Parses CSV files into structured table format for LLM reasoning.
     */
    fun formatCsvContent(file: File): String {
        val raw = readTextWithCharsetFallback(file)
        val lines = raw.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return "[Empty CSV File: ${file.name}]"

        val sb = StringBuilder()
        sb.append("[CSV Spreadsheet: ${file.name} • ${lines.size} rows]\n\n")
        lines.take(200).forEachIndexed { index, line ->
            if (index == 0) {
                sb.append("HEADER: $line\n")
                sb.append("-".repeat(line.length.coerceAtMost(60))).append("\n")
            } else {
                sb.append("Row $index: $line\n")
            }
        }
        if (lines.size > 200) {
            sb.append("\n... [Showing first 200 of ${lines.size} total rows]")
        }
        return sb.toString()
    }

    /**
     * Parses TSV files into structured table format.
     */
    fun formatTsvContent(file: File): String {
        val raw = readTextWithCharsetFallback(file)
        val lines = raw.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return "[Empty TSV File: ${file.name}]"

        val sb = StringBuilder()
        sb.append("[TSV Data: ${file.name} • ${lines.size} records]\n\n")
        lines.take(200).forEachIndexed { index, line ->
            val cells = line.split("\t")
            sb.append(if (index == 0) "HEADER: " else "Record $index: ")
            sb.append(cells.joinToString(" | ")).append("\n")
        }
        return sb.toString()
    }

    /**
     * Pretty-prints JSON data with error tolerance.
     */
    fun formatJsonContent(file: File): String {
        val raw = readTextWithCharsetFallback(file).trim()
        return try {
            if (raw.startsWith("{")) {
                val obj = JSONObject(raw)
                "[JSON Object: ${file.name}]\n" + obj.toString(2)
            } else if (raw.startsWith("[")) {
                val array = JSONArray(raw)
                "[JSON Array: ${file.name} • ${array.length()} items]\n" + array.toString(2)
            } else {
                raw
            }
        } catch (_: Exception) {
            raw
        }
    }

    /**
     * Extracts text from XML, HTML, or SVG files.
     */
    fun extractTextFromXmlOrHtml(file: File): String {
        val raw = readTextWithCharsetFallback(file)
        val clean = stripXmlTags(raw)
        return "[Markup Document: ${file.name}]\n\n$clean"
    }

    fun stripXmlTags(xml: String): String {
        return xml.replace(Regex("<[^>]*>"), " ")
            .let { unescapeXml(it) }
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun unescapeXml(text: String): String {
        return text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#160;", " ")
    }

    /**
     * Inspects zip archive file listings and extracts text from readable files inside.
     */
    fun inspectZipArchive(file: File): String {
        return try {
            val zip = ZipInputStream(file.inputStream())
            var entry = zip.nextEntry
            val entries = mutableListOf<String>()
            val samples = mutableListOf<String>()

            while (entry != null && entries.size < 100) {
                val name = entry.name
                val size = entry.size
                entries.add("$name (${formatFileSize(size.coerceAtLeast(0))})")

                val ext = name.substringAfterLast('.', "").lowercase()
                if (samples.size < 3 && ext in listOf("txt", "md", "json", "py", "kt", "csv", "xml", "js")) {
                    val sampleText = zip.bufferedReader(Charsets.UTF_8).readText().take(400)
                    samples.add("--- File: $name ---\n$sampleText\n")
                }
                entry = zip.nextEntry
            }
            zip.close()

            val sb = StringBuilder()
            sb.append("[ZIP Archive: ${file.name} • ${entries.size} files]\n\nManifest:\n")
            entries.take(40).forEach { sb.append("• $it\n") }
            if (entries.size > 40) sb.append("... and ${entries.size - 40} more files\n")
            if (samples.isNotEmpty()) {
                sb.append("\nSample File Previews:\n").append(samples.joinToString("\n"))
            }
            sb.toString()
        } catch (e: Exception) {
            "ZIP Archive (${file.name}): ${e.localizedMessage}"
        }
    }

    /**
     * Extracts media metadata (audio/video duration, bitrate, resolution) via MediaMetadataRetriever.
     */
    fun extractMediaMetadata(file: File): String {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            retriever.release()

            val durationSec = durationMs / 1000
            val min = durationSec / 60
            val sec = durationSec % 60
            val timeStr = String.format(java.util.Locale.US, "%d:%02d", min, sec)

            val sb = StringBuilder()
            sb.append("[Media File: ${file.name} • Format: ${mime ?: "Audio/Video"}]\n")
            sb.append("• Duration: $timeStr ($durationMs ms)\n")
            sb.append("• File Size: ${formatFileSize(file.length())}\n")
            if (!title.isNullOrBlank()) sb.append("• Title: $title\n")
            if (!artist.isNullOrBlank()) sb.append("• Artist: $artist\n")
            if (bitrate > 0) sb.append("• Bitrate: ${bitrate / 1000} kbps\n")
            if (!width.isNullOrBlank() && !height.isNullOrBlank()) sb.append("• Video Resolution: ${width}x${height}\n")
            sb.toString()
        } catch (e: Exception) {
            "[Media File: ${file.name} • Size: ${formatFileSize(file.length())}]"
        }
    }

    /**
     * Universal charset reader fallback with bounded streaming and binary data detection.
     */
    fun readTextWithCharsetFallback(file: File, maxBytesToRead: Int = 262_144): String {
        return try {
            val stream = file.inputStream()
            val buffer = ByteArray(maxBytesToRead)
            val readCount = stream.use { it.read(buffer) }
            if (readCount <= 0) return ""
            val bytes = if (readCount < maxBytesToRead) buffer.copyOf(readCount) else buffer

            // Binary check: count non-printable control characters
            var nonPrintable = 0
            for (b in bytes) {
                val unsigned = b.toInt() and 0xFF
                if (unsigned == 0 || (unsigned < 9 && unsigned != 0) || (unsigned in 14..31)) {
                    nonPrintable++
                }
            }
            if (nonPrintable > bytes.size * 0.15) {
                return "Binary file format detected (${file.name}, ${formatFileSize(file.length())}). Raw binary contents are omitted to preserve prompt context."
            }

            val charsets = listOf(
                Charsets.UTF_8,
                Charsets.ISO_8859_1,
                Charset.forName("Windows-1252"),
                Charsets.UTF_16
            )

            for (cs in charsets) {
                try {
                    val text = String(bytes, cs)
                    if (text.isNotBlank() && !text.contains('\uFFFD')) {
                        return text
                    }
                } catch (_: Exception) {}
            }

            String(bytes, Charsets.UTF_8).replace("\u0000", "")
        } catch (e: Exception) {
            "Unable to read file text: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    suspend fun convertImageToBase64(file: File, maxDimension: Int = 1280): String? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return@withContext null

            // EXIF orientation detection
            val exif = try {
                ExifInterface(file.absolutePath)
            } catch (_: Throwable) { null }

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }

            val outputStream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format(java.util.Locale.US, "%.2f GB", gb)
    }

    /**
     * Reads phone storage statistics (total device storage, available free storage, cached attachment stats).
     */
    fun getDeviceStorageStats(context: Context): DeviceStorageStats {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize
            val realTotalBytes = if (totalBytes > 0) totalBytes else 64_000_000_000L
            val realAvailableBytes = if (availableBytes > 0) availableBytes else 32_000_000_000L
            val usedBytes = (realTotalBytes - realAvailableBytes).coerceAtLeast(0)
            val usedPercent = ((usedBytes * 100) / realTotalBytes).toInt()

            val cacheFolder = File(context.filesDir, "chat_attachments")
            val files = cacheFolder.listFiles().orEmpty()
            val cacheBytes = files.sumOf { it.length() }

            DeviceStorageStats(
                totalBytes = realTotalBytes,
                availableBytes = realAvailableBytes,
                usedPercent = usedPercent,
                totalFormatted = formatFileSize(realTotalBytes),
                availableFormatted = formatFileSize(realAvailableBytes),
                cachedAttachmentsCount = files.size,
                cachedAttachmentsBytes = cacheBytes,
                cachedAttachmentsFormatted = formatFileSize(cacheBytes)
            )
        } catch (_: Exception) {
            DeviceStorageStats(
                totalBytes = 64_000_000_000L,
                availableBytes = 32_000_000_000L,
                usedPercent = 50,
                totalFormatted = "64.0 GB",
                availableFormatted = "32.0 GB",
                cachedAttachmentsCount = 0,
                cachedAttachmentsBytes = 0L,
                cachedAttachmentsFormatted = "0 B"
            )
        }
    }

    /**
     * Retrieves all cached attachments stored on internal device storage for quick access.
     */
    fun getCachedAttachments(context: Context): List<File> {
        val cacheFolder = File(context.filesDir, "chat_attachments")
        if (!cacheFolder.exists()) return emptyList()
        return cacheFolder.listFiles()?.filter { it.isFile && it.length() > 0 }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Prepares built-in sample files in app storage so the user can immediately test
     * full data retrieval without needing to transfer external files first.
     */
    suspend fun createSampleAttachment(context: Context, sampleType: String): AttachmentInfo = withContext(Dispatchers.IO) {
        val cacheFolder = File(context.filesDir, "chat_attachments").apply { mkdirs() }

        val (fileName, mime, content) = when (sampleType) {
            "csv" -> Triple(
                "Financial_Q3_Report.csv",
                "text/csv",
                """Region,Product,Quarter,Units Sold,Revenue USD,Growth Rate
North America,AI Cloud Studio,Q3,14200,710000,18.4%
Europe,Developer Suite Pro,Q3,9800,490000,12.1%
Asia Pacific,Enterprise Analytics,Q3,18500,925000,24.8%
Latin America,Voice API Connect,Q3,4300,215000,9.7%
Middle East,Security Shield,Q3,6100,305000,15.2%""".trimIndent()
            )
            "python" -> Triple(
                "Data_Analysis_Script.py",
                "text/x-python",
                """# Data Analysis and Token Usage Estimator
import math
from typing import List, Dict

def compute_statistics(values: List[float]) -> Dict[str, float]:
    n = len(values)
    if n == 0:
        return {"count": 0, "mean": 0.0, "std_dev": 0.0}
    mean = sum(values) / n
    variance = sum((x - mean) ** 2 for x in values) / (n - 1 if n > 1 else 1)
    return {
        "count": n,
        "mean": round(mean, 2),
        "std_dev": round(math.sqrt(variance), 2),
        "min": min(values),
        "max": max(values)
    }

# Sample telemetry data
telemetry = [42.5, 51.2, 38.9, 64.1, 55.0, 49.8, 71.3]
print("Results:", compute_statistics(telemetry))""".trimIndent()
            )
            "json" -> Triple(
                "System_Metrics.json",
                "application/json",
                """{
  "system": "OmniChat Cloud Node",
  "version": "3.5.0",
  "status": "operational",
  "uptime_hours": 742.5,
  "endpoints": [
    {"name": "Gemini 3.8 Flash", "latency_ms": 142, "health": "healthy"},
    {"name": "Groq LLaMA 3.3", "latency_ms": 78, "health": "healthy"},
    {"name": "Cerebras Fast", "latency_ms": 45, "health": "healthy"}
  ],
  "storage_connected": true,
  "supported_formats": ["PDF", "DOCX", "XLSX", "PPTX", "CSV", "JSON", "Python", "Kotlin", "Images", "RTF", "ZIP"]
}""".trimIndent()
            )
            "excel" -> Triple(
                "Sales_Forecast.csv",
                "text/csv",
                """Department,Manager,Target Q4,Actual Sales,Variance %,Status
Enterprise,Sarah Connor,1200000,1450000,+20.8%,Exceeded
SMB Accounts,John Smith,650000,610000,-6.1%,Under Review
Govt & Edu,Dr. Aris,850000,920000,+8.2%,Met
Global Partners,Elena Rostova,2100000,2400000,+14.3%,Exceeded""".trimIndent()
            )
            else -> Triple(
                "Project_Architecture_Notes.txt",
                "text/plain",
                """# Project OmniChat Architecture Overview
Date: October 2026

Core Architecture:
1. Multi-Provider AI Routing: Dynamically bridges Gemini, Groq, OpenRouter, Cerebras, and Pollinations.
2. Local Storage & Offline Cache: Room database stores full chat sessions, artifacts, and token counters.
3. Multi-Format File Extractor: Direct parsing of PDF, Word (.docx), Excel (.xlsx), PowerPoint (.pptx), CSV, and Code.
4. Voice & Speech Synthesis: On-device Android TTS with adjustable pitch, speed, and real-time audio playback.
5. Responsive Edge-to-Edge Material 3 UI with dynamic custom theming.""".trimIndent()
            )
        }

        val file = File(cacheFolder, "${UUID.randomUUID()}_$fileName")
        file.writeText(content, Charsets.UTF_8)

        AttachmentInfo(
            id = UUID.randomUUID().toString(),
            name = fileName,
            mimeType = mime,
            sizeBytes = file.length(),
            localUri = file.absolutePath,
            isImage = false,
            previewSnippet = content.take(200) + "..."
        )
    }

    suspend fun extractFileDetails(file: File, mimeType: String): FileDetailsInfo = withContext(Dispatchers.IO) {
        val isImg = mimeType.startsWith("image/") || isImageExtension(file.name)
        val extracted = if (isImg) {
            val base64 = convertImageToBase64(file)
            if (base64 != null) "[Image file: ${file.name} (${formatFileSize(file.length())}) • Encoded for multimodal vision analysis]"
            else "[Image file: ${file.name}]"
        } else {
            readFullTextContent(file, maxChars = 200_000)
        }

        val lines = if (isImg) 1 else extracted.lines().size
        val words = if (isImg) 0 else extracted.split(Regex("\\s+")).count { it.isNotBlank() }
        val chars = extracted.length
        val summary = if (extracted.length > 320) extracted.take(320).trim() + "..." else extracted.trim()

        FileDetailsInfo(
            fileName = file.name,
            mimeType = mimeType,
            sizeBytes = file.length(),
            sizeFormatted = formatFileSize(file.length()),
            lineCount = lines,
            wordCount = words,
            characterCount = chars,
            extractedText = extracted,
            summarySnippet = summary,
            isImage = isImg
        )
    }
}

data class FileDetailsInfo(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val lineCount: Int,
    val wordCount: Int,
    val characterCount: Int,
    val extractedText: String,
    val summarySnippet: String,
    val isImage: Boolean
)
