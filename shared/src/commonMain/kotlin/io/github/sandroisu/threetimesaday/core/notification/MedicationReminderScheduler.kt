package io.github.sandroisu.threetimesaday.core.notification

interface MedicationReminderScheduler {

    suspend fun getPermissionStatus(): NotificationPermissionStatus

    suspend fun requestPermission(): NotificationPermissionStatus

    suspend fun scheduleReminder(notification: MedicationReminderNotification)

    suspend fun cancelReminder(notificationId: String)

    suspend fun cancelAllReminders()
}
