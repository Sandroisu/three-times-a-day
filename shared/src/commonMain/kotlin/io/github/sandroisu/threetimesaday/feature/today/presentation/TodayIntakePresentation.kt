package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecord
import kotlinx.datetime.LocalDateTime

internal enum class IntakeDisplayStatus {
    Taken,
    Skipped,
    Upcoming,
    Due,
    Overdue,
}

internal data class MedicationIntakeUiModel(
    val eventId: String,
    val medicationName: String,
    val dosageText: String,
    val scheduledDateTime: LocalDateTime,
    val contextLabel: UiText,
    val status: IntakeDisplayStatus,
    val statusLabel: UiText,
    val isActionable: Boolean,
)

internal data class IntakeTimeGroup(
    val timeLabel: UiText,
    val intakes: List<MedicationIntakeUiModel>,
)

internal fun todayIntakeGroups(
    events: List<MedicationIntakeEvent>,
    records: List<MedicationIntakeRecord>,
    currentDateTime: LocalDateTime?,
): List<IntakeTimeGroup> {
    val recordsById = records.associateBy { it.eventId }
    return events.sortedBy { it.scheduledDateTime }
        .map { event ->
            val status = intakeDisplayStatus(event, currentDateTime)
            val takenAt = recordsById[event.eventId]?.takeIf { it.status == MedicationIntakeStatus.Taken }?.updatedDateTime
            MedicationIntakeUiModel(
                eventId = event.eventId,
                medicationName = event.medicationName,
                dosageText = event.dosageText,
                scheduledDateTime = event.scheduledDateTime,
                contextLabel = if (event.status == MedicationIntakeStatus.Postponed) {
                    intakeStatusLabel(event.status)
                } else {
                    intakeMomentLabel(event.intakeMoment)
                },
                status = status,
                statusLabel = intakeDisplayStatusLabel(status, takenAt),
                isActionable = status == IntakeDisplayStatus.Due || status == IntakeDisplayStatus.Overdue,
            )
        }
        .groupBy { it.scheduledDateTime }
        .map { (dateTime, intakes) -> IntakeTimeGroup(intakeGroupTimeLabel(dateTime, currentDateTime), intakes) }
}

private fun intakeDisplayStatus(
    event: MedicationIntakeEvent,
    currentDateTime: LocalDateTime?,
): IntakeDisplayStatus =
    when (event.status) {
        MedicationIntakeStatus.Taken -> IntakeDisplayStatus.Taken
        MedicationIntakeStatus.Skipped -> IntakeDisplayStatus.Skipped
        MedicationIntakeStatus.Scheduled, MedicationIntakeStatus.Postponed -> when {
            currentDateTime == null || event.scheduledDateTime > currentDateTime -> IntakeDisplayStatus.Upcoming
            isMedicationIntakeEventExpired(event, currentDateTime) -> IntakeDisplayStatus.Overdue
            else -> IntakeDisplayStatus.Due
        }
    }

internal fun intakeCompletionFraction(events: List<MedicationIntakeEvent>): Float {
    if (events.isEmpty()) return 0f
    return events.count { it.status == MedicationIntakeStatus.Taken }.toFloat() / events.size
}
