package com.example.util

import android.content.Context
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
