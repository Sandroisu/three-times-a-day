package io.github.sandroisu.threetimesaday.feature.medication.domain

import kotlinx.serialization.Serializable

@Serializable
enum class MedicationIntakeStatus {
    Scheduled,
    Taken,
    Skipped,
    Postponed
}
