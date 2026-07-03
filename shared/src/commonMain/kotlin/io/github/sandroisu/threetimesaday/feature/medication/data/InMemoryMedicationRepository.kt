package io.github.sandroisu.threetimesaday.feature.medication.data

import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

class InMemoryMedicationRepository : MedicationRepository {

    private val mutex = Mutex()
    private val storedMedications: MutableList<Medication> = createInitialMedications().toMutableList()

    override suspend fun getMedications(): List<Medication> = mutex.withLock { storedMedications.toList() }

    override suspend fun saveMedication(medication: Medication) {
        mutex.withLock { storedMedications.add(medication) }
    }

    override suspend fun updateMedication(medication: Medication) {
        mutex.withLock {
            val existingIndex = storedMedications.indexOfFirst { it.id == medication.id }
            if (existingIndex >= 0) {
                storedMedications[existingIndex] = medication
            } else {
                storedMedications.add(medication)
            }
        }
    }

    override suspend fun deleteMedication(medicationId: String) {
        mutex.withLock { storedMedications.removeAll { it.id == medicationId } }
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
}
