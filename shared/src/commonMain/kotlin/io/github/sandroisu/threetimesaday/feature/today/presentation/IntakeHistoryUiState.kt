package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeAdherence
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeHistoryDay
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeHistoryPeriod
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationAdherence

internal data class IntakeHistoryUiState(
    val isLoading: Boolean = false,
    val selectedPeriod: IntakeHistoryPeriod = IntakeHistoryPeriod.LastSevenDays,
    val hasMedications: Boolean = true,
    val overallAdherence: IntakeAdherence? = null,
    val medicationAdherence: List<MedicationAdherence> = emptyList(),
    // Newest day first.
    val days: List<IntakeHistoryDay> = emptyList(),
    val errorMessage: UiText? = null,
)
