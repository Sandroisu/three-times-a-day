package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent

data class TodayUiState(
    val screenTitle: String = "Три раза в день",
    val dateTitle: String = "",
    val isLoading: Boolean = false,
    val intakeEvents: List<MedicationIntakeEvent> = emptyList(),
    val errorMessage: String? = null
)
