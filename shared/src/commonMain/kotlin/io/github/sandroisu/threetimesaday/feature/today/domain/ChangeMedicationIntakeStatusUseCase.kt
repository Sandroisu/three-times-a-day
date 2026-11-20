package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ChangeMedicationIntakeStatusUseCase(
    private val medicationRepository: MedicationRepository,
    private val medicationIntakeRecordRepository: MedicationIntakeRecordRepository,
) {

    // The previous record and the stock change must be one step.
    // Otherwise a double tap on "Taken" takes two doses.
    private val mutex = Mutex()

    suspend operator fun invoke(record: MedicationIntakeRecord) {
        mutex.withLock {
            val doseAlreadyTaken = medicationIntakeRecordRepository.getRecord(record.eventId)?.stockDoseTaken == true
            val stockDoseTaken = when {
                record.status != MedicationIntakeStatus.Taken -> {
                    if (doseAlreadyTaken) {
                        medicationRepository.returnDose(record.medicationId)
                    }
                    false
                }
                doseAlreadyTaken -> true
                else -> medicationRepository.takeDose(record.medicationId)
            }
            medicationIntakeRecordRepository.saveRecord(record.copy(stockDoseTaken = stockDoseTaken))
        }
    }
}
