package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Displays a [Toast] safely from any thread or coroutine dispatcher.
 */
fun showMainToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
    val appContext = context.applicationContext ?: context
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Toast.makeText(appContext, message, duration).show()
    } else {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(appContext, message, duration).show()
        }
    }
}

/**
 * Normalizes a web URL and opens it safely in an external browser without crashing if no browser is present.
 */
fun openExternalUrl(
    context: Context,
    rawUrl: String,
    failureMessage: String = "No browser or app available to open this link."
) {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) {
        showMainToast(context, failureMessage)
        return
    }
    val normalized = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        showMainToast(context, failureMessage)
    }
}

/**
 * Launches the Android share sheet safely with chooser fallback protection.
 */
fun shareTextContent(
    context: Context,
    text: String,
    chooserTitle: String = "Share",
    subject: String? = null,
    failureMessage: String = "No app available to share this content."
) {
    runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            if (!subject.isNullOrBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TITLE, subject)
            }
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
    }.onFailure {
        showMainToast(context, failureMessage)
    }
}

/**
 * Decodes a photo [Uri] cleanly without OOMing on high-resolution smartphone camera images (12MP-50MP).
 */
fun decodeSampledBitmapFromUri(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int = 1600
): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width
                val height = info.size.height
                val largest = maxOf(width, height)
                if (largest > maxDimensionPx && largest > 0) {
                    val scale = maxDimensionPx.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            if (largest > maxDimensionPx && maxDimensionPx > 0) {
                while (largest / sampleSize > maxDimensionPx * 2) {
                    sampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@runCatching null

            val w = decoded.width
            val h = decoded.height
            val postLargest = maxOf(w, h)
            if (postLargest > maxDimensionPx && postLargest > 0) {
                val scale = maxDimensionPx.toFloat() / postLargest.toFloat()
                Bitmap.createScaledBitmap(
                    decoded,
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }
        }
    }.getOrNull()
}
