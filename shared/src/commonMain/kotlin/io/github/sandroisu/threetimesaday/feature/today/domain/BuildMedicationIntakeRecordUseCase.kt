package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.core.time.plusMinutes
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus

internal const val MEDICATION_POSTPONE_MINUTES = 10

internal class BuildMedicationIntakeRecordUseCase(
    private val timeProvider: TimeProvider,
) {

    operator fun invoke(
        event: MedicationIntakeEvent,
        status: MedicationIntakeStatus,
    ): MedicationIntakeRecord {
        val postponedDateTime = if (status == MedicationIntakeStatus.Postponed) {
            plusMinutes(event.scheduledDateTime, MEDICATION_POSTPONE_MINUTES)
        } else {
            null
        }
        return MedicationIntakeRecord(
            eventId = event.eventId,
            medicationId = event.medicationId,
            scheduledDateTime = event.scheduledDateTime,
            status = status,
            updatedDateTime = timeProvider.currentDateTime(),
            postponedDateTime = postponedDateTime,
        )
    }
}
