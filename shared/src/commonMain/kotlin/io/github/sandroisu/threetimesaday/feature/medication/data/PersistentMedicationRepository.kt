package io.github.sandroisu.threetimesaday.feature.medication.data

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
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
        val storedMedications = keyValueStorage.getString(MEDICATIONS_KEY) ?: return createInitialMedications()
        return runCatching { json.decodeFromString<List<Medication>>(storedMedications) }
            .getOrElse { createInitialMedications() }
    }

    private fun writeMedications(medications: List<Medication>) {
        keyValueStorage.putString(MEDICATIONS_KEY, json.encodeToString(medications))
    }

    private fun createInitialMedications(): List<Medication> {
        val courseStartDate = LocalDate(2025, 1, 1)
        return listOf(
            Medication(
                id = "entecavir",
                name = "Энтекавир",
                dosageText = "1 таблетка",
                intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp),
                courseStartDate = courseStartDate,
                courseEndDate = null
            ),
            Medication(
                id = "magnesium",
                name = "Магний",
                dosageText = "1 таблетка",
                intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep),
                courseStartDate = courseStartDate,
                courseEndDate = null
            ),
            Medication(
                id = "vitamin-d",
                name = "Витамин D",
                dosageText = "1 капсула",
                intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterBreakfast),
                courseStartDate = courseStartDate,
                courseEndDate = null
            )
        )
    }

    private companion object {
        const val MEDICATIONS_KEY = "medications"
    }
}
