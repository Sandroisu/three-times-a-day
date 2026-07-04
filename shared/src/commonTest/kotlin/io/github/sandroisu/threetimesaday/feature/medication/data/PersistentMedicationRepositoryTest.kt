package io.github.sandroisu.threetimesaday.feature.medication.data

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistentMedicationRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun returnsSeedMedicationsWhenNothingStored() = runTest {
        val repository = PersistentMedicationRepository(InMemoryKeyValueStorage(), json)

        val medications = repository.getMedications()

        assertEquals(3, medications.size)
        assertTrue(medications.any { it.id == "entecavir" })
    }

    @Test
    fun savedMedicationSurvivesRepositoryRecreation() = runTest {
        val storage = InMemoryKeyValueStorage()
        val addedMedication = Medication(
            id = "aspirin",
            name = "Аспирин",
            dosageText = "1 таблетка",
            intakeRule = MedicationIntakeRule.AtExactTime(LocalTime(9, 30)),
            courseStartDate = LocalDate(2026, 7, 4),
            courseEndDate = null
        )
        PersistentMedicationRepository(storage, json).saveMedication(addedMedication)

        val restoredMedications = PersistentMedicationRepository(storage, json).getMedications()

        assertTrue(restoredMedications.any { it.id == "aspirin" })
        assertEquals(addedMedication, restoredMedications.single { it.id == "aspirin" })
    }

    @Test
    fun deletingAllMedicationsPersistsEmptyListWithoutReseeding() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentMedicationRepository(storage, json)
        val seedMedications = repository.getMedications()
        seedMedications.forEach { repository.deleteMedication(it.id) }

        val restoredMedications = PersistentMedicationRepository(storage, json).getMedications()

        assertTrue(restoredMedications.isEmpty())
    }

    @Test
    fun updateReplacesExistingMedicationAndPersists() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentMedicationRepository(storage, json)
        val updatedMedication = Medication(
            id = "entecavir",
            name = "Энтекавир Форте",
            dosageText = "2 таблетки",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp),
            courseStartDate = LocalDate(2025, 1, 1),
            courseEndDate = null
        )

        repository.updateMedication(updatedMedication)
        val restoredMedication = PersistentMedicationRepository(storage, json)
            .getMedications()
            .single { it.id == "entecavir" }

        assertEquals("Энтекавир Форте", restoredMedication.name)
        assertEquals("2 таблетки", restoredMedication.dosageText)
    }
}
