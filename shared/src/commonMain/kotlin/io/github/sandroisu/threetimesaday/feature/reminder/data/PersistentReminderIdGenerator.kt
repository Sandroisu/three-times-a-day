package io.github.sandroisu.threetimesaday.feature.reminder.data

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderIdGenerator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PersistentReminderIdGenerator(
    private val keyValueStorage: KeyValueStorage,
) : ReminderIdGenerator {

    private val mutex = Mutex()

    override suspend fun nextId(): String = mutex.withLock {
        val nextNumber = (keyValueStorage.getString(LAST_ASSIGNED_NUMBER_KEY)?.toLongOrNull() ?: 0L) + 1L
        keyValueStorage.putString(LAST_ASSIGNED_NUMBER_KEY, nextNumber.toString())
        "reminder-$nextNumber"
    }

    private companion object {
        const val LAST_ASSIGNED_NUMBER_KEY = "last_reminder_id_number"
    }
}
