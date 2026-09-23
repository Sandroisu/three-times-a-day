package io.github.sandroisu.threetimesaday.feature.reminder.domain

interface ReminderRepository {

    suspend fun getReminders(): List<Reminder>

    suspend fun saveReminder(reminder: Reminder)

    suspend fun updateReminder(reminder: Reminder)

    suspend fun deleteReminder(reminderId: String)
}
