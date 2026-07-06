package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistentMedicationReminderRegistryTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val remindersKey = "medication_reminder_ids"

    @Test
    fun addStoresReminderIdWithoutDuplicates() = runTest {
        val storage = InMemoryKeyValueStorage()
        val registry = PersistentMedicationReminderRegistry(storage, json)

        registry.addReminderId("wake-event")
        registry.addReminderId("wake-event")

        assertEquals(listOf("wake-event"), registry.getReminderIds())
    }

    @Test
    fun addKeepsMultipleDistinctReminderIds() = runTest {
        val storage = InMemoryKeyValueStorage()
        val registry = PersistentMedicationReminderRegistry(storage, json)

        registry.addReminderId("first-event")
        registry.addReminderId("second-event")

        assertEquals(listOf("first-event", "second-event"), registry.getReminderIds())
    }

    @Test
    fun removeDeletesReminderId() = runTest {
        val storage = InMemoryKeyValueStorage()
        val registry = PersistentMedicationReminderRegistry(storage, json)
        registry.addReminderId("first-event")
        registry.addReminderId("second-event")

        registry.removeReminderId("first-event")

        assertEquals(listOf("second-event"), registry.getReminderIds())
    }

    @Test
    fun clearRemovesAllReminderIds() = runTest {
        val storage = InMemoryKeyValueStorage()
        val registry = PersistentMedicationReminderRegistry(storage, json)
        registry.addReminderId("first-event")
        registry.addReminderId("second-event")

        registry.clear()

        assertTrue(registry.getReminderIds().isEmpty())
    }

    @Test
    fun reminderIdsSurviveRegistryRecreation() = runTest {
        val storage = InMemoryKeyValueStorage()
        PersistentMedicationReminderRegistry(storage, json).addReminderId("wake-event")

        val restoredIds = PersistentMedicationReminderRegistry(storage, json).getReminderIds()

        assertEquals(listOf("wake-event"), restoredIds)
    }

    @Test
    fun corruptedStoredValueReturnsEmptyListWithoutThrowing() = runTest {
        val storage = InMemoryKeyValueStorage()
        storage.putString(remindersKey, "not a json array")
        val registry = PersistentMedicationReminderRegistry(storage, json)

        assertTrue(registry.getReminderIds().isEmpty())
    }
}
