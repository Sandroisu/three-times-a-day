package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class PersistentMedicationReminderLaunchRepository(
    private val keyValueStorage: KeyValueStorage,
    private val json: Json
) : MedicationReminderLaunchRepository {

    private val mutex = Mutex()

    override suspend fun saveLaunchData(launchData: MedicationReminderLaunchData) {
        mutex.withLock {
            keyValueStorage.putString(LAUNCH_DATA_KEY, json.encodeToString(launchData))
        }
    }

    override suspend fun consumeLaunchData(): MedicationReminderLaunchData? = mutex.withLock {
        val storedLaunchData = keyValueStorage.getString(LAUNCH_DATA_KEY) ?: return@withLock null
        keyValueStorage.remove(LAUNCH_DATA_KEY)
        runCatching { json.decodeFromString<MedicationReminderLaunchData>(storedLaunchData) }
            .getOrNull()
    }

    private companion object {
        const val LAUNCH_DATA_KEY = "medication_reminder_launch_data"
    }
}
