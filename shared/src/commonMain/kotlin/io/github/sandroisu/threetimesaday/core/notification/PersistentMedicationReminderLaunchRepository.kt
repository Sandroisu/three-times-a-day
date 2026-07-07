package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal const val MEDICATION_REMINDER_LAUNCH_DATA_KEY = "medication_reminder_launch_data"

class PersistentMedicationReminderLaunchRepository(
    private val keyValueStorage: KeyValueStorage,
    private val json: Json
) : MedicationReminderLaunchRepository {

    private val mutex = Mutex()

    override suspend fun saveLaunchData(launchData: MedicationReminderLaunchData) {
        mutex.withLock {
            keyValueStorage.putString(MEDICATION_REMINDER_LAUNCH_DATA_KEY, json.encodeToString(launchData))
        }
    }

    override suspend fun consumeLaunchData(): MedicationReminderLaunchData? = mutex.withLock {
        val storedLaunchData = keyValueStorage.getString(MEDICATION_REMINDER_LAUNCH_DATA_KEY)
            ?: return@withLock null
        keyValueStorage.remove(MEDICATION_REMINDER_LAUNCH_DATA_KEY)
        runCatching { json.decodeFromString<MedicationReminderLaunchData>(storedLaunchData) }
            .getOrNull()
    }
}
