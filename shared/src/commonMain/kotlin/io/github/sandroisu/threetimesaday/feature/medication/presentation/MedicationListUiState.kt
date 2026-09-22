package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
internal data class MedicationListItemUiModel(
    val id: String,
    val name: String,
    val dosageText: String,
    val intakeRuleText: UiText,
    val recurrenceText: UiText,
    val courseLabel: UiText?
)

internal data class MedicationListUiState(
    val isLoading: Boolean = false,
    val medications: List<MedicationListItemUiModel> = emptyList(),
    val errorMessage: UiText? = null
)
