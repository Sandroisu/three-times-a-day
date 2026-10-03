package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderAlertMode
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderWeekday

internal enum class ReminderRecurrenceType {
    Once,
    Daily,
    Weekdays,
    EveryDays,
    EveryMonths,
    CyclicIntervals,
}

internal data class ReminderEditorUiState(
    val isLoading: Boolean = false,
    val reminderId: String? = null,
    val titleText: String = "",
    val dateText: String = "",
    val timeText: String = "09:00",
    val recurrenceType: ReminderRecurrenceType = ReminderRecurrenceType.Once,
    val selectedWeekdays: Set<ReminderWeekday> = emptySet(),
    val dayIntervalText: String = "2",
    val monthlyIntervalText: String = "3",
    val monthlyDayOfMonthText: String = "",
    val cyclicIntervalsText: String = "2, 4, 8, 2, 6",
    val alertMode: ReminderAlertMode = ReminderAlertMode.Notification,
    val titleError: UiText? = null,
    val dateError: UiText? = null,
    val timeError: UiText? = null,
    val weekdaysError: UiText? = null,
    val dayIntervalError: UiText? = null,
    val monthlyIntervalError: UiText? = null,
    val monthlyDayOfMonthError: UiText? = null,
    val cyclicIntervalsError: UiText? = null,
    val generalErrorMessage: UiText? = null,
    val isSaveEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
)
