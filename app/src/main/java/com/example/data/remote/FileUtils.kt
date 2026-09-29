package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.model.AttachmentInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileUtils {

    suspend fun processPickedUri(context: Context, uri: Uri): AttachmentInfo? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromUri(uri)

            var displayName = "file_${System.currentTimeMillis()}"
            var sizeBytes = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }

            val isImage = mimeType.startsWith("image/") ||
                    displayName.endsWith(".jpg", true) ||
                    displayName.endsWith(".jpeg", true) ||
                    displayName.endsWith(".png", true) ||
                    displayName.endsWith(".webp", true)

            // Save to local cache directory so the app retains access across sessions
            val cacheFolder = File(context.filesDir, "chat_attachments").apply { mkdirs() }
            val extension = displayName.substringAfterLast('.', "")
            val localFileName = "${UUID.randomUUID()}${if (extension.isNotEmpty()) ".$extension" else ""}"
            val targetFile = File(cacheFolder, localFileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (sizeBytes <= 0) {
                sizeBytes = targetFile.length()
            }

            // Generate preview snippet for text or code files
            var snippet: String? = null
            if (!isImage) {
                snippet = readTextSnippet(targetFile, maxChars = 200)
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

    private fun getMimeTypeFromUri(uri: Uri): String {
        val path = uri.path ?: return "application/octet-stream"
        return when {
            path.endsWith(".jpg", true) || path.endsWith(".jpeg", true) -> "image/jpeg"
            path.endsWith(".png", true) -> "image/png"
            path.endsWith(".webp", true) -> "image/webp"
            path.endsWith(".pdf", true) -> "application/pdf"
            path.endsWith(".txt", true) -> "text/plain"
            path.endsWith(".md", true) -> "text/markdown"
            path.endsWith(".json", true) -> "application/json"
            path.endsWith(".csv", true) -> "text/csv"
            path.endsWith(".kt", true) || path.endsWith(".java", true) -> "text/x-code"
            path.endsWith(".py", true) -> "text/x-python"
            path.endsWith(".js", true) || path.endsWith(".ts", true) -> "text/javascript"
            path.endsWith(".html", true) || path.endsWith(".xml", true) -> "text/html"
            else -> "application/octet-stream"
        }
    }

    private fun readTextSnippet(file: File, maxChars: Int): String? {
        return try {
            val text = file.readText(Charsets.UTF_8).trim()
            if (text.length > maxChars) {
                text.take(maxChars) + "..."
            } else {
                text
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun readFullTextContent(file: File, maxChars: Int = 120_000): String = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext ""
            val fullText = file.readText(Charsets.UTF_8)
            if (fullText.length > maxChars) {
                fullText.take(maxChars) + "\n\n... [Content truncated due to length]"
            } else {
                fullText
            }
        } catch (e: Exception) {
            "Unable to read file text: ${e.localizedMessage}"
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

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.1f MB", mb)
    }
}
