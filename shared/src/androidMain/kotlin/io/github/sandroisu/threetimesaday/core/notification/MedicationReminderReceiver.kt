package io.github.sandroisu.threetimesaday.core.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
        val notificationRequestCode = intent.getIntExtra(EXTRA_NOTIFICATION_REQUEST_CODE, 0)
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!hasPostNotificationsPermission(context)) {
            return
        }
        ensureChannel(notificationManager)
        val notification = buildNotification(
            context = context,
            title = title,
            message = message,
            notificationId = notificationId,
            notificationRequestCode = notificationRequestCode
        )
        notificationManager.notify(notificationRequestCode, notification)
    }

    private fun hasPostNotificationsPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun ensureChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }

    private fun buildNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: String,
        notificationRequestCode: Int
    ): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        return builder
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setAutoCancel(true)
            .setContentIntent(buildContentIntent(context, notificationId, notificationRequestCode))
            .build()
    }

    private fun buildContentIntent(
        context: Context,
        notificationId: String,
        notificationRequestCode: Int
    ): PendingIntent? {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?: return null
        val eventId = notificationId.removePrefix(MEDICATION_REMINDER_ID_PREFIX)
        launchIntent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LAUNCH_EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(LAUNCH_EXTRA_EVENT_ID, eventId)
        }
        return PendingIntent.getActivity(
            context,
            notificationRequestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val CHANNEL_NAME = "Напоминания о приёме"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_NOTIFICATION_REQUEST_CODE = "extra_notification_request_code"
        const val LAUNCH_EXTRA_NOTIFICATION_ID = "io.github.sandroisu.threetimesaday.LAUNCH_NOTIFICATION_ID"
        const val LAUNCH_EXTRA_EVENT_ID = "io.github.sandroisu.threetimesaday.LAUNCH_EVENT_ID"
    }
}
