package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "ls_job_alerts_channel_v3"
    private const val CHANNEL_NAME = "LS Services Job Alerts"
    private const val CHANNEL_DESC = "Notifications for new job openings and targeted alerts published on LS Services."

    fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            Log.d(TAG, "areNotificationsEnabled: App-level notifications disabled")
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                Log.d(TAG, "areNotificationsEnabled: POST_NOTIFICATIONS permission not granted")
            }
            return hasPermission
        }
        return true
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showJobAlertNotification(
        context: Context,
        jobId: String,
        title: String,
        organization: String,
        location: String,
        isTargetedMatch: Boolean = false,
        matchedReason: String? = null
    ): Boolean {
        createNotificationChannel(context)

        if (!areNotificationsEnabled(context)) {
            Log.w(TAG, "Cannot show notification: Notifications disabled or permission missing.")
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("job_id", jobId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Unique request code so PendingIntent isn't overwritten
        val reqCode = (jobId.hashCode() xor System.currentTimeMillis().toInt()) and 0x7FFFFFFF

        val pendingIntent = PendingIntent.getActivity(
            context,
            reqCode,
            intent,
            pendingIntentFlags
        )

        val contentTitle = if (isTargetedMatch) "🎯 Target Job Match: $title" else "🆕 New Vacancy: $title"
        val contentText = if (isTargetedMatch && !matchedReason.isNullOrBlank()) {
            "$organization • $location ($matchedReason)"
        } else {
            "$organization • $location"
        }

        val bigText = if (isTargetedMatch && !matchedReason.isNullOrBlank()) {
            "⚡ Matching Vacancy! $organization in $location published '$title' matching your $matchedReason preferences. Tap to view and apply."
        } else {
            "$organization in $location published a new job: '$title'. Tap to view details and apply."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = (reqCode and 0x7FFFFFFF)
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Successfully dispatched notification id: $notificationId for job: $title")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch notification: ${e.message}", e)
            false
        }
    }

    fun showOfflineSummaryNotification(
        context: Context,
        newCount: Int
    ): Boolean {
        createNotificationChannel(context)

        if (!areNotificationsEnabled(context)) {
            Log.w(TAG, "Cannot show summary notification: Notifications disabled or permission missing.")
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            999999,
            intent,
            pendingIntentFlags
        )

        val title = "🌐 Back Online: $newCount New Vacancy Uploads"
        val text = "You received $newCount new job notifications published while you were offline. Tap to view all new listings."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(999999, builder.build())
            Log.d(TAG, "Successfully dispatched offline summary notification for $newCount items")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch summary notification: ${e.message}", e)
            false
        }
    }
}
