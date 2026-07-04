package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus

class ApplyMedicationIntakeRecordsUseCase {

    operator fun invoke(
        generatedEvents: List<MedicationIntakeEvent>,
        records: List<MedicationIntakeRecord>
    ): List<MedicationIntakeEvent> {
        val recordsByEventId = records.associateBy { record -> record.eventId }
        val mergedEvents = generatedEvents.map { event ->
            val record = recordsByEventId[event.eventId] ?: return@map event
            val scheduledDateTime =
                if (record.status == MedicationIntakeStatus.Postponed && record.postponedDateTime != null) {
                    record.postponedDateTime
                } else {
                    event.scheduledDateTime
                }
            event.copy(status = record.status, scheduledDateTime = scheduledDateTime)
        }
        return mergedEvents.sortedBy { event -> event.scheduledDateTime }
    }
}
