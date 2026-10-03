package io.github.sandroisu.threetimesaday.feature.medication.data

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRecurrence
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
    fun returnsEmptyListWhenNothingStored() = runTest {
        val repository = PersistentMedicationRepository(InMemoryKeyValueStorage(), json)

        val medications = repository.getMedications()

        assertTrue(medications.isEmpty())
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
    fun storedMedicationWithoutRecurrenceUsesDailyRecurrenceForBackwardCompatibility() = runTest {
        val storage = InMemoryKeyValueStorage()
        storage.putString(
            "medications",
            """[{"id":"aspirin","name":"Аспирин","dosageText":"1 таблетка","intakeRule":{"type":"io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule.AtExactTime","time":"09:30:00"},"courseStartDate":"2026-07-04","courseEndDate":null}]""",
        )
        val repository = PersistentMedicationRepository(storage, json)

        val medication = repository.getMedications().single()

        assertEquals(MedicationRecurrence.Daily, medication.recurrence)
    }

    @Test
    fun deletingAllMedicationsPersistsEmptyList() = runTest {
        val storage = InMemoryKeyValueStorage()
        val repository = PersistentMedicationRepository(storage, json)
        repository.saveMedication(
            Medication(
                id = "aspirin",
                name = "Аспирин",
                dosageText = "1 таблетка",
                intakeRule = MedicationIntakeRule.AtExactTime(LocalTime(9, 30)),
                courseStartDate = LocalDate(2026, 7, 4),
                courseEndDate = null,
            )
        )
        repository.deleteMedication("aspirin")

        val restoredMedications = PersistentMedicationRepository(storage, json).getMedications()

        assertTrue(restoredMedications.isEmpty())
    }

    @Test
    fun removesUnchangedLegacyDemoMedicationsButPreservesEditedOnes() = runTest {
        val storage = InMemoryKeyValueStorage()
        val legacyStartDate = LocalDate(2025, 1, 1)
        val legacyDemo = Medication(
            id = "entecavir",
            name = "Энтекавир",
            dosageText = "1 таблетка",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp),
            courseStartDate = legacyStartDate,
            courseEndDate = null,
        )
        val editedDemo = legacyDemo.copy(id = "magnesium", name = "Мой магний")
        storage.putString("medications", json.encodeToString(listOf(legacyDemo, editedDemo)))

        val medications = PersistentMedicationRepository(storage, json).getMedications()

        assertEquals(listOf(editedDemo), medications)
        assertEquals(listOf(editedDemo), json.decodeFromString<List<Medication>>(storage.getString("medications").orEmpty()))
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
