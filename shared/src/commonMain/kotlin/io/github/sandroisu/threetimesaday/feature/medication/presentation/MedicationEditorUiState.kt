package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment

internal data class MedicationEditorUiState(
    val isLoading: Boolean = false,
    val medicationId: String? = null,
    val nameText: String = "",
    val dosageText: String = "",
    val courseStartDateText: String = "",
    val courseEndDateText: String = "",
    val monthlyIntervalText: String = "3",
    val monthlyDayOfMonthText: String = "",
    val selectedIntakeMoment: MedicationIntakeMoment? = MedicationIntakeMoment.AfterWakeUp,
    val exactTimeText: String = "",
    val nameError: UiText? = null,
    val dosageError: UiText? = null,
    val exactTimeError: UiText? = null,
    val courseStartDateError: UiText? = null,
    val courseEndDateError: UiText? = null,
    val monthlyIntervalError: UiText? = null,
    val monthlyDayOfMonthError: UiText? = null,
    val generalErrorMessage: UiText? = null,
    val isExactTimeVisible: Boolean = false,
    val isMonthlyRecurrence: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val isDeleteVisible: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
    val isEditing: Boolean = true,
    val isSaving: Boolean = false,
    val intakeRuleLabel: UiText? = null,
    val recurrenceLabel: UiText = MedicationLabels.daily,
    val courseScheduleLabel: UiText = MedicationLabels.newCourse,
    val courseLabel: UiText? = null,
    val isDistributedRule: Boolean = false,
)
