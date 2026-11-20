package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.storageJson
import io.github.sandroisu.threetimesaday.feature.medication.data.PersistentMedicationRepository
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.today.data.PersistentMedicationIntakeRecordRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChangeMedicationIntakeStatusUseCaseTest {

    private val storage = InMemoryKeyValueStorage()
    private val medicationRepository = PersistentMedicationRepository(storage, storageJson)
    private val recordRepository = PersistentMedicationIntakeRecordRepository(storage, storageJson)
    private val changeStatus = ChangeMedicationIntakeStatusUseCase(medicationRepository, recordRepository)

    @Test
    fun takenIntakeRemovesOneDose() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))

        changeStatus(record(MedicationIntakeStatus.Taken))

        assertEquals(9, dosesLeft())
        assertEquals(MedicationIntakeStatus.Taken, recordRepository.getRecord(EVENT_ID)?.status)
    }

    @Test
    fun takingSameIntakeTwiceRemovesOnlyOneDose() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))

        changeStatus(record(MedicationIntakeStatus.Taken))
        changeStatus(record(MedicationIntakeStatus.Taken))

        assertEquals(9, dosesLeft())
    }

    @Test
    fun skippingTakenIntakeGivesDoseBack() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))
        changeStatus(record(MedicationIntakeStatus.Taken))

        changeStatus(record(MedicationIntakeStatus.Skipped))

        assertEquals(10, dosesLeft())
        assertEquals(MedicationIntakeStatus.Skipped, recordRepository.getRecord(EVENT_ID)?.status)
    }

    @Test
    fun movingTakenIntakeBackToScheduledGivesDoseBackOnce() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))
        changeStatus(record(MedicationIntakeStatus.Taken))

        changeStatus(record(MedicationIntakeStatus.Scheduled))
        changeStatus(record(MedicationIntakeStatus.Postponed))

        assertEquals(10, dosesLeft())
    }

    @Test
    fun skippingIntakeDoesNotChangeStock() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))

        changeStatus(record(MedicationIntakeStatus.Skipped))

        assertEquals(10, dosesLeft())
    }

    @Test
    fun undoingIntakeTakenFromEmptyStockDoesNotAddDose() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 0))
        changeStatus(record(MedicationIntakeStatus.Taken))

        changeStatus(record(MedicationIntakeStatus.Skipped))

        assertEquals(0, dosesLeft())
    }

    @Test
    fun undoingIntakeTakenBeforeStockWasTrackedDoesNotAddDose() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = null))
        changeStatus(record(MedicationIntakeStatus.Taken))
        medicationRepository.updateMedication(medication(dosesLeft = 5))

        changeStatus(record(MedicationIntakeStatus.Skipped))

        assertEquals(5, dosesLeft())
    }

    @Test
    fun takingIntakeOfUntrackedMedicationKeepsStockUntracked() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = null))

        changeStatus(record(MedicationIntakeStatus.Taken))

        assertNull(dosesLeft())
        assertEquals(MedicationIntakeStatus.Taken, recordRepository.getRecord(EVENT_ID)?.status)
    }

    private suspend fun dosesLeft(): Int? = medicationRepository.getMedications().single().dosesLeft

    private fun medication(dosesLeft: Int?): Medication = Medication(
        id = MEDICATION_ID,
        name = "Aspirin",
        dosageText = "1 tablet",
        intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterBreakfast),
        courseStartDate = LocalDate(2026, 7, 1),
        courseEndDate = null,
        dosesLeft = dosesLeft,
    )

    private fun record(status: MedicationIntakeStatus): MedicationIntakeRecord = MedicationIntakeRecord(
        eventId = EVENT_ID,
        medicationId = MEDICATION_ID,
        scheduledDateTime = LocalDateTime(2026, 7, 4, 9, 30),
        status = status,
        updatedDateTime = LocalDateTime(2026, 7, 4, 9, 41),
    )

    private companion object {
        const val MEDICATION_ID = "aspirin"
        const val EVENT_ID = "aspirin|2026-07-04|09:30|AfterBreakfast"
    }
}
