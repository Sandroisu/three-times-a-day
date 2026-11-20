package io.github.sandroisu.threetimesaday.feature.today.domain

internal enum class MedicationReminderAction {
    Taken,
    Postpone,
}

internal sealed interface MedicationReminderActionResult {

    data object Applied : MedicationReminderActionResult

    // The medication was deleted, its schedule changed, or the intake is older than yesterday.
    data object EventNotFound : MedicationReminderActionResult

    data object AlreadyResolved : MedicationReminderActionResult
}
