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
import android.util.Log
import io.github.sandroisu.threetimesaday.feature.today.domain.MEDICATION_POSTPONE_MINUTES
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationReminderAction
import io.github.sandroisu.threetimesaday.shared.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
        val notificationRequestCode = intent.getIntExtra(EXTRA_NOTIFICATION_REQUEST_CODE, 0)
        val alarmDurationMinutes = intent.getIntExtra(EXTRA_ALARM_DURATION_MINUTES, 0)
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!hasPostNotificationsPermission(context)) {
            rescheduleFollowingReminders(context, notificationId)
            return
        }
        if (alarmDurationMinutes > 0) {
            ReminderAlarmService.start(
                context = context,
                title = title,
                message = message,
                notificationId = notificationId,
                maxDurationMinutes = alarmDurationMinutes,
            )
        } else {
            ensureChannel(context, notificationManager)
            val notification = buildNotification(
                context = context,
                title = title,
                message = message,
                notificationId = notificationId,
                notificationRequestCode = notificationRequestCode
            )
            notificationManager.notify(notificationRequestCode, notification)
        }
        rescheduleFollowingReminders(context, notificationId)
    }

    private fun hasPostNotificationsPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun ensureChannel(context: Context, notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                )
            )
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
            .setCategory(Notification.CATEGORY_REMINDER)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentIntent(buildContentIntent(context, notificationId, notificationRequestCode))
            .apply {
                notificationActions(notificationId).forEach { action ->
                    addAction(buildAction(context, action, notificationId, notificationRequestCode))
                }
            }
            .build()
    }

    private fun buildAction(
        context: Context,
        action: MedicationReminderAction,
        notificationId: String,
        notificationRequestCode: Int,
    ): Notification.Action {
        val title = when (action) {
            MedicationReminderAction.Taken -> context.getString(R.string.medication_action_taken)
            MedicationReminderAction.Postpone ->
                context.getString(R.string.medication_action_postpone, MEDICATION_POSTPONE_MINUTES)
        }
        // The intent action tells the two buttons apart: PendingIntent ignores extras when it matches intents.
        val actionIntent = Intent(context, MedicationReminderActionReceiver::class.java).apply {
            this.action = medicationReminderActionId(action)
            putExtra(MedicationReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationRequestCode,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(null, title, pendingIntent).build()
    }

    private fun buildContentIntent(
        context: Context,
        notificationId: String,
        notificationRequestCode: Int
    ): PendingIntent? {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?: return null
        launchIntent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (notificationId.startsWith(MEDICATION_REMINDER_ID_PREFIX)) {
                putExtra(LAUNCH_EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(LAUNCH_EXTRA_EVENT_ID, notificationId.removePrefix(MEDICATION_REMINDER_ID_PREFIX))
            }
        }
        return PendingIntent.getActivity(
            context,
            notificationRequestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun rescheduleFollowingReminders(context: Context, notificationId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when {
                    notificationId.startsWith(MEDICATION_REMINDER_ID_PREFIX) ->
                        AndroidMedicationReminderRescheduler.reschedule(
                            context = context.applicationContext,
                            deliveredNotificationId = notificationId,
                        )

                    notificationId.startsWith(REMINDER_NOTIFICATION_ID_PREFIX) ->
                        AndroidReminderRescheduler.reschedule(
                            context = context.applicationContext,
                            deliveredNotificationId = notificationId,
                        )
                }
            } catch (rescheduleFailure: Exception) {
                Log.e(LOG_TAG, "Could not schedule following reminder", rescheduleFailure)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_NOTIFICATION_REQUEST_CODE = "extra_notification_request_code"
        const val EXTRA_ALARM_DURATION_MINUTES = "extra_alarm_duration_minutes"
        const val LAUNCH_EXTRA_NOTIFICATION_ID = "io.github.sandroisu.threetimesaday.LAUNCH_NOTIFICATION_ID"
        const val LAUNCH_EXTRA_EVENT_ID = "io.github.sandroisu.threetimesaday.LAUNCH_EVENT_ID"
        const val LOG_TAG = "MedicationReminder"
    }
}
