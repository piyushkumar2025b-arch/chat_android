package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object AppNotificationManager {

    const val CHANNEL_REMINDERS = "omnichat_reminders"
    const val CHANNEL_FITNESS = "omnichat_fitness"
    const val CHANNEL_AI_TIPS = "omnichat_ai_tips"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "OmniChat Reminders & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important task reminders and alert notifications from OmniChat"
                enableVibration(true)
            }

            val fitnessChannel = NotificationChannel(
                CHANNEL_FITNESS,
                "Fitness & Step Counter",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily step milestones, distance achievements, and activity goals"
            }

            val aiTipsChannel = NotificationChannel(
                CHANNEL_AI_TIPS,
                "AI Daily Insights & Tips",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily AI insights, summaries, and smart knowledge highlights"
            }

            notificationManager.createNotificationChannels(listOf(reminderChannel, fitnessChannel, aiTipsChannel))
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = CHANNEL_REMINDERS,
        notificationId: Int = System.currentTimeMillis().toInt()
    ): Boolean {
        initChannels(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val smallIcon = android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    fun sendStepGoalAchieved(context: Context, steps: Int, goal: Int): Boolean {
        return sendNotification(
            context = context,
            title = "🎉 Daily Step Goal Achieved!",
            body = "Incredible job! You reached $steps steps (Goal: $goal). Keep up the fantastic momentum!",
            channelId = CHANNEL_FITNESS,
            notificationId = 1001
        )
    }

    fun sendAiTip(context: Context, topic: String, tip: String): Boolean {
        return sendNotification(
            context = context,
            title = "💡 AI Insight: $topic",
            body = tip,
            channelId = CHANNEL_AI_TIPS,
            notificationId = 1002
        )
    }
}
