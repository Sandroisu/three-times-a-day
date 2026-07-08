package io.github.sandroisu.threetimesaday.core.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class AndroidMedicationReminderScheduler(
    private val context: Context,
    private val permissionController: AndroidNotificationPermissionController,
    private val reminderRegistry: MedicationReminderRegistry
) : MedicationReminderScheduler {

    init {
        ensureChannel()
    }

    override suspend fun getPermissionStatus(): NotificationPermissionStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            return if (isGranted) NotificationPermissionStatus.Granted else NotificationPermissionStatus.NotDetermined
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
            ?: return NotificationPermissionStatus.NotSupported
        return if (notificationManager.areNotificationsEnabled()) {
            NotificationPermissionStatus.Granted
        } else {
            NotificationPermissionStatus.Denied
        }
    }

    override suspend fun requestPermission(): NotificationPermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return getPermissionStatus()
        }
        if (getPermissionStatus() == NotificationPermissionStatus.Granted) {
            return NotificationPermissionStatus.Granted
        }
        val isGranted = permissionController.requestPermission()
        return if (isGranted) NotificationPermissionStatus.Granted else NotificationPermissionStatus.Denied
    }

    override suspend fun areExactRemindersAllowed(): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return true
        return canScheduleExact(alarmManager)
    }

    override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = notification.scheduledDateTime
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        val pendingIntent = buildPendingIntent(notification)
        val useExact = canScheduleExact(alarmManager)
        val scheduled = try {
            if (useExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            true
        } catch (exactAlarmDenied: SecurityException) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }.isSuccess
        }
        if (scheduled) {
            reminderRegistry.addReminderId(notification.notificationId)
        }
    }

    override suspend fun cancelReminder(notificationId: String) {
        cancelSystemReminder(notificationId)
        reminderRegistry.removeReminderId(notificationId)
    }

    override suspend fun cancelAllReminders() {
        val idsToCancel = reminderRegistry.getReminderIds()
        idsToCancel.forEach { notificationId ->
            runCatching { cancelSystemReminder(notificationId) }
        }
        reminderRegistry.clear()
    }

    private fun cancelSystemReminder(notificationId: String) {
        val requestCode = reminderRequestCode(notificationId)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val existingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            reminderIntent(),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existingIntent != null) {
            alarmManager?.cancel(existingIntent)
            existingIntent.cancel()
        }
        context.getSystemService(NotificationManager::class.java)?.cancel(requestCode)
    }

    private fun buildPendingIntent(notification: MedicationReminderNotification): PendingIntent {
        val requestCode = reminderRequestCode(notification.notificationId)
        val intent = reminderIntent().apply {
            putExtra(MedicationReminderReceiver.EXTRA_TITLE, notification.title)
            putExtra(MedicationReminderReceiver.EXTRA_MESSAGE, notification.message)
            putExtra(MedicationReminderReceiver.EXTRA_NOTIFICATION_ID, notification.notificationId)
            putExtra(MedicationReminderReceiver.EXTRA_NOTIFICATION_REQUEST_CODE, requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun reminderIntent(): Intent = Intent(context, MedicationReminderReceiver::class.java).apply {
        action = ACTION_MEDICATION_REMINDER
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
            if (notificationManager.getNotificationChannel(MedicationReminderReceiver.CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        MedicationReminderReceiver.CHANNEL_ID,
                        MedicationReminderReceiver.CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
            }
        }
    }

    private companion object {
        const val ACTION_MEDICATION_REMINDER = "io.github.sandroisu.threetimesaday.MEDICATION_REMINDER"
    }
}
