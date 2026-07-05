package io.github.sandroisu.threetimesaday.core.notification

class NoOpMedicationReminderScheduler : MedicationReminderScheduler {

    override suspend fun getPermissionStatus(): NotificationPermissionStatus =
        NotificationPermissionStatus.NotDetermined

    override suspend fun requestPermission(): NotificationPermissionStatus =
        NotificationPermissionStatus.NotDetermined

    override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
    }

    override suspend fun cancelReminder(notificationId: String) {
    }

    override suspend fun cancelAllReminders() {
    }
}
