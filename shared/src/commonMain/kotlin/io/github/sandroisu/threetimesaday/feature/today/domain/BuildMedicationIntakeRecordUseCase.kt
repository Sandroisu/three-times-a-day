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
        val currentDateTime = timeProvider.currentDateTime()
        // An overdue intake postponed from its old time would land in the past and never remind again.
        val postponedDateTime = if (status == MedicationIntakeStatus.Postponed) {
            plusMinutes(maxOf(event.scheduledDateTime, currentDateTime), MEDICATION_POSTPONE_MINUTES)
        } else {
            null
        }
        return MedicationIntakeRecord(
            eventId = event.eventId,
            medicationId = event.medicationId,
            scheduledDateTime = event.scheduledDateTime,
            status = status,
            updatedDateTime = currentDateTime,
            postponedDateTime = postponedDateTime,
        )
    }
}
