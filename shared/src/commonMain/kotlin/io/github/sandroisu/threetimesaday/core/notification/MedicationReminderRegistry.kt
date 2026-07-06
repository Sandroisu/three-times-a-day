package io.github.sandroisu.threetimesaday.core.notification

interface MedicationReminderRegistry {

    suspend fun addReminderId(notificationId: String)

    suspend fun removeReminderId(notificationId: String)

    suspend fun getReminderIds(): List<String>

    suspend fun clear()
}
