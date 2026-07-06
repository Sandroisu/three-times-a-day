package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class PersistentMedicationReminderRegistry(
    private val keyValueStorage: KeyValueStorage,
    private val json: Json
) : MedicationReminderRegistry {

    private val mutex = Mutex()

    override suspend fun addReminderId(notificationId: String) {
        mutex.withLock {
            val ids = readIds().toMutableList()
            if (!ids.contains(notificationId)) {
                ids.add(notificationId)
                writeIds(ids)
            }
        }
    }

    override suspend fun removeReminderId(notificationId: String) {
        mutex.withLock {
            val ids = readIds().toMutableList()
            if (ids.remove(notificationId)) {
                writeIds(ids)
            }
        }
    }

    override suspend fun getReminderIds(): List<String> = mutex.withLock {
        readIds()
    }

    override suspend fun clear() {
        mutex.withLock {
            keyValueStorage.remove(REMINDER_IDS_KEY)
        }
    }

    private fun readIds(): List<String> {
        val storedIds = keyValueStorage.getString(REMINDER_IDS_KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(storedIds) }
            .getOrElse { emptyList() }
    }

    private fun writeIds(ids: List<String>) {
        keyValueStorage.putString(REMINDER_IDS_KEY, json.encodeToString(ids))
    }

    private companion object {
        const val REMINDER_IDS_KEY = "medication_reminder_ids"
    }
}
