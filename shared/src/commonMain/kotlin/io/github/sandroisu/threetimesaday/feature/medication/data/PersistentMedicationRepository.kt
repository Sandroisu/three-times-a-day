package io.github.sandroisu.threetimesaday.feature.medication.data

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRecurrence
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class PersistentMedicationRepository(
    private val keyValueStorage: KeyValueStorage,
    private val json: Json
) : MedicationRepository {

    private val mutex = Mutex()

    override suspend fun getMedications(): List<Medication> = mutex.withLock {
        readMedications()
    }

    override suspend fun saveMedication(medication: Medication) {
        mutex.withLock {
            val medications = readMedications().toMutableList()
            medications.add(medication)
            writeMedications(medications)
        }
    }

    override suspend fun updateMedication(medication: Medication) {
        mutex.withLock {
            val medications = readMedications().toMutableList()
            val existingIndex = medications.indexOfFirst { it.id == medication.id }
            if (existingIndex >= 0) {
                medications[existingIndex] = medication
            } else {
                medications.add(medication)
            }
            writeMedications(medications)
        }
    }

    override suspend fun deleteMedication(medicationId: String) {
        mutex.withLock {
            val medications = readMedications().toMutableList()
            medications.removeAll { it.id == medicationId }
            writeMedications(medications)
        }
    }

    private fun readMedications(): List<Medication> {
        val storedMedications = keyValueStorage.getString(MEDICATIONS_KEY) ?: return emptyList()
        val medications = runCatching { json.decodeFromString<List<Medication>>(storedMedications) }
            .getOrElse { return emptyList() }
        return removeLegacyDemoMedications(medications)
    }

    private fun writeMedications(medications: List<Medication>) {
        keyValueStorage.putString(MEDICATIONS_KEY, json.encodeToString(medications))
    }

    private fun removeLegacyDemoMedications(medications: List<Medication>): List<Medication> {
        if (keyValueStorage.getString(LEGACY_DEMO_MIGRATION_KEY) != null) {
            return medications
        }
        val migratedMedications = medications.filterNot { medication -> medication in LEGACY_DEMO_MEDICATIONS }
        if (migratedMedications != medications) {
            writeMedications(migratedMedications)
        }
        keyValueStorage.putString(LEGACY_DEMO_MIGRATION_KEY, MIGRATION_COMPLETED_VALUE)
        return migratedMedications
    }

    private companion object {
        const val MEDICATIONS_KEY = "medications"
        const val LEGACY_DEMO_MIGRATION_KEY = "legacy_demo_medications_removed"
        const val MIGRATION_COMPLETED_VALUE = "true"

        val LEGACY_DEMO_MEDICATIONS = listOf(
            Medication(
                id = "entecavir",
                name = "Энтекавир",
                dosageText = "1 таблетка",
                intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp),
                courseStartDate = LocalDate(2025, 1, 1),
                courseEndDate = null,
                recurrence = MedicationRecurrence.Daily,
            ),
            Medication(
                id = "magnesium",
                name = "Магний",
                dosageText = "1 таблетка",
                intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep),
                courseStartDate = LocalDate(2025, 1, 1),
                courseEndDate = null,
                recurrence = MedicationRecurrence.Daily,
            ),
            Medication(
                id = "vitamin-d",
                name = "Витамин D",
                dosageText = "1 капсула",
                intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterBreakfast),
                courseStartDate = LocalDate(2025, 1, 1),
                courseEndDate = null,
                recurrence = MedicationRecurrence.Daily,
            ),
        )
    }
}
