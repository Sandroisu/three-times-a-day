package io.github.sandroisu.threetimesaday.feature.reminder.data

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.feature.reminder.domain.Reminder
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRecurrence
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistentReminderRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun savedReminderSurvivesRepositoryRecreation() = runTest {
        val storage = InMemoryKeyValueStorage()
        val reminder = Reminder(
            id = "taxes",
            title = "Pay taxes",
            date = LocalDate(2026, 1, 14),
            time = LocalTime(9, 30),
            recurrence = ReminderRecurrence.EveryMonthsOnDay(3, 14),
        )
        PersistentReminderRepository(storage, json).saveReminder(reminder)

        val restoredReminders = PersistentReminderRepository(storage, json).getReminders()

        assertEquals(listOf(reminder), restoredReminders)
    }

    @Test
    fun updateAndDeletePersistChanges() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentReminderRepository(storage, json)
        val reminder = Reminder(
            id = "passport",
            title = "Renew passport",
            date = LocalDate(2026, 4, 14),
            time = LocalTime(10, 0),
            recurrence = ReminderRecurrence.Once,
        )
        repository.saveReminder(reminder)
        repository.updateReminder(reminder.copy(title = "Renew passport documents"))
        repository.deleteReminder(reminder.id)

        assertTrue(PersistentReminderRepository(storage, json).getReminders().isEmpty())
    }

    @Test
    fun generatedIdsStayUniqueAfterGeneratorRecreation() = runTest {
        val storage = InMemoryKeyValueStorage()

        val firstId = PersistentReminderIdGenerator(storage).nextId()
        val secondId = PersistentReminderIdGenerator(storage).nextId()

        assertEquals("reminder-1", firstId)
        assertEquals("reminder-2", secondId)
    }
}
