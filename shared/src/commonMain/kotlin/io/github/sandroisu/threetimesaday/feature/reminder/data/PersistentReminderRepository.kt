package io.github.sandroisu.threetimesaday.feature.reminder.data

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.feature.reminder.domain.Reminder
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class PersistentReminderRepository(
    private val keyValueStorage: KeyValueStorage,
    private val json: Json,
) : ReminderRepository {

    private val mutex = Mutex()

    override suspend fun getReminders(): List<Reminder> = mutex.withLock { readReminders() }

    override suspend fun saveReminder(reminder: Reminder) {
        mutex.withLock {
            val reminders = readReminders().toMutableList()
            reminders.add(reminder)
            writeReminders(reminders)
        }
    }

    override suspend fun updateReminder(reminder: Reminder) {
        mutex.withLock {
            val reminders = readReminders().toMutableList()
            val existingIndex = reminders.indexOfFirst { storedReminder -> storedReminder.id == reminder.id }
            if (existingIndex >= 0) {
                reminders[existingIndex] = reminder
            } else {
                reminders.add(reminder)
            }
            writeReminders(reminders)
        }
    }

    override suspend fun deleteReminder(reminderId: String) {
        mutex.withLock {
            val reminders = readReminders().toMutableList()
            reminders.removeAll { reminder -> reminder.id == reminderId }
            writeReminders(reminders)
        }
    }

    private fun readReminders(): List<Reminder> {
        val storedReminders = keyValueStorage.getString(REMINDERS_KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Reminder>>(storedReminders) }
            .getOrElse { emptyList() }
    }

    private fun writeReminders(reminders: List<Reminder>) {
        keyValueStorage.putString(REMINDERS_KEY, json.encodeToString(reminders))
    }

    private companion object {
        const val REMINDERS_KEY = "reminders"
    }
}
