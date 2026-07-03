package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent

data class TodayUiState(
    val screenTitle: String = "Три раза в день",
    val intakeEvents: List<MedicationIntakeEvent> = emptyList()
)
