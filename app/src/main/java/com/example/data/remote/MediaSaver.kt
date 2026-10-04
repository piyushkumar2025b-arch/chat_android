package com.example.data.remote

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * Universal Media and Document Saver for OmniChat AI.
 * Handles saving photos, videos, audio tracks, and generated files
 * directly to the phone's native storage (Gallery, Movies, Music, Downloads)
 * with Scoped Storage support for Android 10+ (API 29+) and legacy fallback.
 */
object MediaSaver {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Download and save an image from URL directly into Phone's Gallery (Pictures/OmniChat).
     */
    suspend fun saveImageFromUrl(
        context: Context,
        imageUrl: String,
        promptTitle: String = "image"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "OmniChat/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download image (HTTP ${response.code})"))
            }

            val bytes = response.body?.bytes()
                ?: return@withContext Result.failure(Exception("Image data was empty"))

            val cleanName = promptTitle.take(30).replace(Regex("[^a-zA-Z0-9_]"), "_").trim('_').ifEmpty { "OmniArt" }
            val fileName = "Omni_${cleanName}_${System.currentTimeMillis()}.png"
            val uri = saveMediaToStorage(
                context = context,
                bytes = bytes,
                fileName = fileName,
                mimeType = "image/png",
                collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                },
                subDirectory = "${Environment.DIRECTORY_PICTURES}/OmniChat"
            )

            if (uri != null) {
                showToast(context, "✅ Photo saved to Gallery (/Pictures/OmniChat)")
                Result.success(uri)
            } else {
                Result.failure(Exception("Failed to save image to Gallery"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "❌ Error saving photo: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Save a generated Bitmap into Phone's Gallery (Pictures/OmniChat).
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        title: String = "OmniArt"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val cleanName = title.take(30).replace(Regex("[^a-zA-Z0-9_]"), "_").trim('_').ifEmpty { "OmniArt" }
            val fileName = "Omni_${cleanName}_${System.currentTimeMillis()}.png"

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/OmniChat")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Could not create MediaStore entry"))

            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            showToast(context, "✅ Photo saved to Gallery (/Pictures/OmniChat)")
            Result.success(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "❌ Error saving image: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Save generated video / animation to Movies (Movies/OmniChat).
     */
    suspend fun saveVideoFromUrl(
        context: Context,
        videoUrl: String,
        promptTitle: String = "video"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(videoUrl)
                .header("User-Agent", "OmniChat/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download video (HTTP ${response.code})"))
            }

            val bytes = response.body?.bytes()
                ?: return@withContext Result.failure(Exception("Video content was empty"))

            val cleanName = promptTitle.take(30).replace(Regex("[^a-zA-Z0-9_]"), "_").trim('_').ifEmpty { "OmniVideo" }
            val fileName = "Omni_${cleanName}_${System.currentTimeMillis()}.mp4"

            val uri = saveMediaToStorage(
                context = context,
                bytes = bytes,
                fileName = fileName,
                mimeType = "video/mp4",
                collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                },
                subDirectory = "${Environment.DIRECTORY_MOVIES}/OmniChat"
            )

            if (uri != null) {
                showToast(context, "✅ Video saved to Phone (/Movies/OmniChat)")
                Result.success(uri)
            } else {
                Result.failure(Exception("Failed to save video"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "❌ Error saving video: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Save synthesized or recorded Audio track to Phone (Music/OmniChat).
     */
    suspend fun saveAudioBytes(
        context: Context,
        audioBytes: ByteArray,
        trackName: String = "OmniSound",
        extension: String = "wav"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val cleanName = trackName.take(30).replace(Regex("[^a-zA-Z0-9_]"), "_").trim('_').ifEmpty { "OmniTrack" }
            val fileName = "Omni_${cleanName}_${System.currentTimeMillis()}.$extension"
            val mimeType = if (extension == "mp3") "audio/mpeg" else "audio/wav"

            val uri = saveMediaToStorage(
                context = context,
                bytes = audioBytes,
                fileName = fileName,
                mimeType = mimeType,
                collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                },
                subDirectory = "${Environment.DIRECTORY_MUSIC}/OmniChat"
            )

            if (uri != null) {
                showToast(context, "✅ Audio saved to Music (/Music/OmniChat)")
                Result.success(uri)
            } else {
                Result.failure(Exception("Failed to save audio file"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "❌ Error saving audio: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Save text file, source code, document, or HTML/SVG artifact to Phone (Download/OmniChat).
     */
    suspend fun saveDocumentToDownloads(
        context: Context,
        fileName: String,
        content: String,
        mimeType: String = "text/plain"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val bytes = content.toByteArray(Charsets.UTF_8)
            val cleanName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetName = if (cleanName.contains(".")) cleanName else "$cleanName.txt"

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, targetName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OmniChat")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (downloadUri != null) {
                    resolver.openOutputStream(downloadUri)?.use { it.write(bytes) }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(downloadUri, contentValues, null, null)
                }
                downloadUri
            } else {
                val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "OmniChat")
                if (!downloadDir.exists()) downloadDir.mkdirs()
                val outFile = File(downloadDir, targetName)
                FileOutputStream(outFile).use { it.write(bytes) }
                Uri.fromFile(outFile)
            }

            if (uri != null) {
                showToast(context, "✅ Saved $targetName to Downloads (/OmniChat)")
                Result.success(uri)
            } else {
                Result.failure(Exception("Failed to save document to Downloads"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "❌ Error saving file: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    private fun saveMediaToStorage(
        context: Context,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        collection: Uri,
        subDirectory: String
    ): Uri? {
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, subDirectory)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(uri)?.use { stream: OutputStream ->
                stream.write(bytes)
                stream.flush()
            }

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } else {
            val root = Environment.getExternalStorageDirectory()
            val dir = File(root, subDirectory)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            }
            return resolver.insert(collection, values) ?: Uri.fromFile(file)
        }
    }

    /**
     * View or open saved file in external viewer app (Photos, Video Player, Files).
     */
    fun openFile(context: Context, uri: Uri, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast(context, "No app available to open this file")
        }
    }
}
