package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import kotlinx.datetime.LocalDateTime

internal fun isMedicationIntakeEventExpired(
    event: MedicationIntakeEvent,
    currentDateTime: LocalDateTime
): Boolean {
    val isActionable = event.status == MedicationIntakeStatus.Scheduled ||
        event.status == MedicationIntakeStatus.Postponed
    return isActionable && event.scheduledDateTime < currentDateTime
}
