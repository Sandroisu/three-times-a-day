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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class AndroidMedicationReminderScheduler(
    private val context: Context,
    private val permissionController: AndroidNotificationPermissionController
) : MedicationReminderScheduler {

    private val mutex = Mutex()
    private val scheduledNotificationIds = mutableSetOf<String>()

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

    override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
        mutex.withLock { scheduledNotificationIds.add(notification.notificationId) }
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = notification.scheduledDateTime
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        val pendingIntent = buildPendingIntent(notification)
        val useExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (useExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (exactAlarmDenied: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    override suspend fun cancelReminder(notificationId: String) {
        mutex.withLock { scheduledNotificationIds.remove(notificationId) }
        val requestCode = requestCodeFor(notificationId)
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

    override suspend fun cancelAllReminders() {
        val idsToCancel = mutex.withLock { scheduledNotificationIds.toList() }
        idsToCancel.forEach { notificationId -> cancelReminder(notificationId) }
    }

    private fun buildPendingIntent(notification: MedicationReminderNotification): PendingIntent {
        val requestCode = requestCodeFor(notification.notificationId)
        val intent = reminderIntent().apply {
            putExtra(MedicationReminderReceiver.EXTRA_TITLE, notification.title)
            putExtra(MedicationReminderReceiver.EXTRA_MESSAGE, notification.message)
            putExtra(MedicationReminderReceiver.EXTRA_NOTIFICATION_REQUEST_CODE, requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reminderIntent(): Intent = Intent(context, MedicationReminderReceiver::class.java).apply {
        action = ACTION_MEDICATION_REMINDER
    }

    private fun requestCodeFor(notificationId: String): Int = notificationId.hashCode()

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
