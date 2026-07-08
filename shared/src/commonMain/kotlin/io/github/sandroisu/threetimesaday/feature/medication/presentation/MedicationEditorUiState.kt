package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment

data class MedicationEditorUiState(
    val isLoading: Boolean = false,
    val medicationId: String? = null,
    val nameText: String = "",
    val dosageText: String = "",
    val selectedIntakeMoment: MedicationIntakeMoment? = MedicationIntakeMoment.AfterWakeUp,
    val exactTimeText: String = "",
    val nameError: String? = null,
    val dosageError: String? = null,
    val exactTimeError: String? = null,
    val generalErrorMessage: String? = null,
    val isExactTimeVisible: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val isDeleteVisible: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false
)
