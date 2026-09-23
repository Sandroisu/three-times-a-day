package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText

internal data class ReminderEditorUiState(
    val isLoading: Boolean = false,
    val reminderId: String? = null,
    val titleText: String = "",
    val dateText: String = "",
    val timeText: String = "09:00",
    val monthlyIntervalText: String = "3",
    val monthlyDayOfMonthText: String = "",
    val titleError: UiText? = null,
    val dateError: UiText? = null,
    val timeError: UiText? = null,
    val monthlyIntervalError: UiText? = null,
    val monthlyDayOfMonthError: UiText? = null,
    val generalErrorMessage: UiText? = null,
    val isMonthlyRecurrence: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
)
