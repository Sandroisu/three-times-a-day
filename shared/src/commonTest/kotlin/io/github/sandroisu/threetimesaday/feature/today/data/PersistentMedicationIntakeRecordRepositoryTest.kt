package io.github.sandroisu.threetimesaday.feature.today.data

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecord
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistentMedicationIntakeRecordRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val firstDate = LocalDate(2026, 7, 4)
    private val secondDate = LocalDate(2026, 7, 5)

    @Test
    fun saveRecordStoresRecord() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentMedicationIntakeRecordRepository(storage, json)
        val record = createRecord("wake-event", firstDate, MedicationIntakeStatus.Taken)

        repository.saveRecord(record)

        assertEquals(record, repository.getRecordsForDate(firstDate).single())
    }

    @Test
    fun saveRecordWithSameEventIdReplacesRecord() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentMedicationIntakeRecordRepository(storage, json)
        repository.saveRecord(createRecord("wake-event", firstDate, MedicationIntakeStatus.Taken))

        repository.saveRecord(createRecord("wake-event", firstDate, MedicationIntakeStatus.Skipped))

        val records = repository.getRecordsForDate(firstDate)
        assertEquals(1, records.size)
        assertEquals(MedicationIntakeStatus.Skipped, records.single().status)
    }

    @Test
    fun getRecordsForDateFiltersByDate() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentMedicationIntakeRecordRepository(storage, json)
        repository.saveRecord(createRecord("first-event", firstDate, MedicationIntakeStatus.Taken))
        repository.saveRecord(createRecord("second-event", secondDate, MedicationIntakeStatus.Skipped))

        val recordsForFirstDate = repository.getRecordsForDate(firstDate)

        assertEquals(1, recordsForFirstDate.size)
        assertEquals("first-event", recordsForFirstDate.single().eventId)
    }

    @Test
    fun recordsSurviveRepositoryRecreation() = runTest {
        val storage = InMemoryKeyValueStorage()
        val savedRecord = createRecord("wake-event", firstDate, MedicationIntakeStatus.Taken)
        PersistentMedicationIntakeRecordRepository(storage, json).saveRecord(savedRecord)

        val restoredRecords = PersistentMedicationIntakeRecordRepository(storage, json)
            .getRecordsForDate(firstDate)

        assertEquals(savedRecord, restoredRecords.single())
    }

    @Test
    fun deleteRecordRemovesRecord() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentMedicationIntakeRecordRepository(storage, json)
        repository.saveRecord(createRecord("wake-event", firstDate, MedicationIntakeStatus.Taken))

        repository.deleteRecord("wake-event")

        assertTrue(repository.getRecordsForDate(firstDate).isEmpty())
    }

    private fun createRecord(
        eventId: String,
        date: LocalDate,
        status: MedicationIntakeStatus
    ): MedicationIntakeRecord = MedicationIntakeRecord(
        eventId = eventId,
        medicationId = "medication",
        scheduledDateTime = LocalDateTime(date, LocalTime(8, 0)),
        status = status,
        updatedDateTime = LocalDateTime(date, LocalTime(9, 0)),
        postponedDateTime = null
    )
}
