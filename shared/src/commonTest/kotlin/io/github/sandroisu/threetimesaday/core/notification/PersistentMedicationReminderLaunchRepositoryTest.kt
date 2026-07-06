package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersistentMedicationReminderLaunchRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val launchDataKey = "medication_reminder_launch_data"
    private val launchData = MedicationReminderLaunchData(
        notificationId = "medication-reminder|wake-event",
        eventId = "wake-event"
    )

    @Test
    fun saveLaunchDataStoresLaunchData() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentMedicationReminderLaunchRepository(storage, json)

        repository.saveLaunchData(launchData)

        assertEquals(launchData, repository.consumeLaunchData())
    }

    @Test
    fun consumeLaunchDataReturnsSavedLaunchData() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentMedicationReminderLaunchRepository(storage, json)
        repository.saveLaunchData(launchData)

        val consumed = repository.consumeLaunchData()

        assertEquals(launchData, consumed)
    }

    @Test
    fun consumeLaunchDataRemovesLaunchDataAfterReading() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentMedicationReminderLaunchRepository(storage, json)
        repository.saveLaunchData(launchData)

        repository.consumeLaunchData()

        assertNull(repository.consumeLaunchData())
    }

    @Test
    fun corruptedStoredValueReturnsNullAndClearsKey() = runTest {
        val storage = InMemoryKeyValueStorage()
        storage.putString(launchDataKey, "not a json object")
        val repository = PersistentMedicationReminderLaunchRepository(storage, json)

        assertNull(repository.consumeLaunchData())
        assertNull(storage.getString(launchDataKey))
    }

    @Test
    fun launchDataSurvivesRepositoryRecreation() = runTest {
        val storage = InMemoryKeyValueStorage()
        PersistentMedicationReminderLaunchRepository(storage, json).saveLaunchData(launchData)

        val consumed = PersistentMedicationReminderLaunchRepository(storage, json).consumeLaunchData()

        assertEquals(launchData, consumed)
    }
}
