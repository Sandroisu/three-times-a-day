package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent
import kotlinx.datetime.LocalDateTime

fun isMedicationIntakeEventExpired(
    event: MedicationIntakeEvent,
    currentDateTime: LocalDateTime
): Boolean {
    val isActionable = event.status == MedicationIntakeStatus.Scheduled ||
        event.status == MedicationIntakeStatus.Postponed
    return isActionable && event.scheduledDateTime < currentDateTime
}
