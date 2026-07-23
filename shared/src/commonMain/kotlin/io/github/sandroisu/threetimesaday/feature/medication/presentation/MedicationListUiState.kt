package io.github.sandroisu.threetimesaday.feature.medication.presentation

data class MedicationListItemUiModel(
    val id: String,
    val name: String,
    val dosageText: String,
    val intakeRuleText: String,
    val courseLabel: String?
)

data class MedicationListUiState(
    val isLoading: Boolean = false,
    val medications: List<MedicationListItemUiModel> = emptyList(),
    val errorMessage: String? = null
)
