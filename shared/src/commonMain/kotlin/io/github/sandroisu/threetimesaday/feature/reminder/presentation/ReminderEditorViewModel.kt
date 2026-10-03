package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.core.time.formatDateInput
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.core.time.parseDateInput
import io.github.sandroisu.threetimesaday.core.time.parseTimeOfDay
import io.github.sandroisu.threetimesaday.core.ui.UiLabels
import io.github.sandroisu.threetimesaday.feature.reminder.domain.Reminder
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderAlertMode
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderIdGenerator
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRecurrence
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRepository
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderWeekday
import io.github.sandroisu.threetimesaday.feature.reminder.domain.RescheduleRemindersUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ReminderEditorViewModel(
    private val reminderRepository: ReminderRepository,
    private val reminderIdGenerator: ReminderIdGenerator,
    private val rescheduleReminders: RescheduleRemindersUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(ReminderEditorUiState())
    val uiState: StateFlow<ReminderEditorUiState> = mutableUiState.asStateFlow()

    private val reminderSavedEventsChannel = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reminderSavedEvents: SharedFlow<Unit> = reminderSavedEventsChannel.asSharedFlow()

    private val reminderDeletedEventsChannel = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reminderDeletedEvents: SharedFlow<Unit> = reminderDeletedEventsChannel.asSharedFlow()

    private var loadedReminder: Reminder? = null
    private var isSessionStarted = false

    fun start(reminderId: String?) {
        if (isSessionStarted && mutableUiState.value.reminderId == reminderId) return
        isSessionStarted = true
        if (reminderId == null) {
            startCreation()
        } else {
            startEditing(reminderId)
        }
    }

    fun finishSession() {
        isSessionStarted = false
    }

    fun onTitleChanged(text: String) {
        mutableUiState.update { state -> validate(state.copy(titleText = text)) }
    }

    fun onDateChanged(text: String) {
        mutableUiState.update { state -> validate(state.copy(dateText = text)) }
    }

    fun onTimeChanged(text: String) {
        mutableUiState.update { state -> validate(state.copy(timeText = text)) }
    }

    fun onRecurrenceTypeSelected(recurrenceType: ReminderRecurrenceType) {
        mutableUiState.update { state ->
            validate(
                state.copy(
                    recurrenceType = recurrenceType,
                    monthlyDayOfMonthText = state.monthlyDayOfMonthText.ifBlank {
                        timeProvider.currentDate().day.toString()
                    },
                )
            )
        }
    }

    fun onWeekdayToggled(weekday: ReminderWeekday) {
        mutableUiState.update { state ->
            val selectedWeekdays = if (weekday in state.selectedWeekdays) {
                state.selectedWeekdays - weekday
            } else {
                state.selectedWeekdays + weekday
            }
            validate(state.copy(selectedWeekdays = selectedWeekdays))
        }
    }

    fun onDayIntervalChanged(text: String) {
        mutableUiState.update { state -> validate(state.copy(dayIntervalText = text)) }
    }

    fun onMonthlyIntervalChanged(text: String) {
        mutableUiState.update { state -> validate(state.copy(monthlyIntervalText = text)) }
    }

    fun onMonthlyDayOfMonthChanged(text: String) {
        mutableUiState.update { state -> validate(state.copy(monthlyDayOfMonthText = text)) }
    }

    fun onCyclicIntervalsChanged(text: String) {
        mutableUiState.update { state -> validate(state.copy(cyclicIntervalsText = text)) }
    }

    fun onAlertModeSelected(alertMode: ReminderAlertMode) {
        mutableUiState.update { state -> validate(state.copy(alertMode = alertMode)) }
    }

    fun save() {
        if (mutableUiState.value.isSaving) return
        val validatedState = validate(mutableUiState.value, forceErrors = true)
        mutableUiState.update { validatedState }
        if (!validatedState.isSaveEnabled) return
        val date = parseDateInput(validatedState.dateText) ?: return
        val time = parseTimeOfDay(validatedState.timeText) ?: return
        val recurrence = buildRecurrence(validatedState) ?: return
        mutableUiState.update { state -> state.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                persistReminder(validatedState, date, time, recurrence)
                rescheduleReminders()
                reminderSavedEventsChannel.tryEmit(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (saveFailure: Exception) {
                mutableUiState.update { state -> state.copy(generalErrorMessage = ReminderLabels.saveError) }
            } finally {
                mutableUiState.update { state -> state.copy(isSaving = false) }
            }
        }
    }

    fun requestDelete() {
        if (mutableUiState.value.reminderId != null) {
            mutableUiState.update { state -> state.copy(isDeleteConfirmationVisible = true) }
        }
    }

    fun dismissDeleteConfirmation() {
        mutableUiState.update { state -> state.copy(isDeleteConfirmationVisible = false) }
    }

    fun confirmDelete() {
        val reminderId = mutableUiState.value.reminderId ?: return
        mutableUiState.update { state -> state.copy(isDeleteConfirmationVisible = false) }
        viewModelScope.launch {
            try {
                reminderRepository.deleteReminder(reminderId)
                rescheduleReminders()
                reminderDeletedEventsChannel.tryEmit(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (deleteFailure: Exception) {
                mutableUiState.update { state -> state.copy(generalErrorMessage = ReminderLabels.deleteError) }
            }
        }
    }

    private fun startCreation() {
        loadedReminder = null
        mutableUiState.update {
            validate(
                ReminderEditorUiState(
                    dateText = formatDateInput(timeProvider.currentDate()),
                    monthlyDayOfMonthText = timeProvider.currentDate().day.toString(),
                )
            )
        }
    }

    private fun startEditing(reminderId: String) {
        loadedReminder = null
        mutableUiState.update { ReminderEditorUiState(isLoading = true, reminderId = reminderId) }
        viewModelScope.launch {
            try {
                val reminder = reminderRepository.getReminders().firstOrNull { storedReminder -> storedReminder.id == reminderId }
                if (reminder == null) {
                    mutableUiState.update { state ->
                        state.copy(isLoading = false, generalErrorMessage = ReminderLabels.notFound)
                    }
                    return@launch
                }
                loadedReminder = reminder
                mutableUiState.update { validate(stateForReminder(reminder)) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (loadFailure: Exception) {
                mutableUiState.update { state ->
                    state.copy(isLoading = false, generalErrorMessage = ReminderLabels.loadError)
                }
            }
        }
    }

    private fun stateForReminder(reminder: Reminder): ReminderEditorUiState {
        val baseState = ReminderEditorUiState(
            reminderId = reminder.id,
            titleText = reminder.title,
            dateText = formatDateInput(reminder.date),
            timeText = formatTimeOfDay(reminder.time),
            alertMode = reminder.alertMode,
        )
        return when (val recurrence = reminder.recurrence) {
            ReminderRecurrence.Once -> baseState
            ReminderRecurrence.Daily -> baseState.copy(recurrenceType = ReminderRecurrenceType.Daily)
            is ReminderRecurrence.OnWeekdays -> baseState.copy(
                recurrenceType = ReminderRecurrenceType.Weekdays,
                selectedWeekdays = recurrence.weekdays.toSet(),
            )

            is ReminderRecurrence.EveryDays -> baseState.copy(
                recurrenceType = ReminderRecurrenceType.EveryDays,
                dayIntervalText = recurrence.intervalDays.toString(),
            )

            is ReminderRecurrence.EveryMonthsOnDay -> baseState.copy(
                recurrenceType = ReminderRecurrenceType.EveryMonths,
                monthlyIntervalText = recurrence.intervalMonths.toString(),
                monthlyDayOfMonthText = recurrence.dayOfMonth.toString(),
            )

            is ReminderRecurrence.CyclicDayIntervals -> baseState.copy(
                recurrenceType = ReminderRecurrenceType.CyclicIntervals,
                cyclicIntervalsText = recurrence.intervals.joinToString(", "),
            )
        }
    }

    private suspend fun persistReminder(
        state: ReminderEditorUiState,
        date: kotlinx.datetime.LocalDate,
        time: kotlinx.datetime.LocalTime,
        recurrence: ReminderRecurrence,
    ) {
        val existingReminderId = state.reminderId
        val reminder = if (existingReminderId == null) {
            Reminder(
                id = reminderIdGenerator.nextId(),
                title = state.titleText.trim(),
                date = date,
                time = time,
                recurrence = recurrence,
                alertMode = state.alertMode,
            )
        } else {
            loadedReminder?.copy(
                title = state.titleText.trim(),
                date = date,
                time = time,
                recurrence = recurrence,
                alertMode = state.alertMode,
            ) ?: Reminder(
                id = existingReminderId,
                title = state.titleText.trim(),
                date = date,
                time = time,
                recurrence = recurrence,
                alertMode = state.alertMode,
            )
        }
        if (existingReminderId == null) {
            reminderRepository.saveReminder(reminder)
        } else {
            reminderRepository.updateReminder(reminder)
        }
    }

    private fun buildRecurrence(state: ReminderEditorUiState): ReminderRecurrence? {
        return when (state.recurrenceType) {
            ReminderRecurrenceType.Once -> ReminderRecurrence.Once
            ReminderRecurrenceType.Daily -> ReminderRecurrence.Daily
            ReminderRecurrenceType.Weekdays -> state.selectedWeekdays
                .takeIf { weekdays -> weekdays.isNotEmpty() }
                ?.sortedBy { weekday -> weekday.isoDayNumber }
                ?.let(ReminderRecurrence::OnWeekdays)

            ReminderRecurrenceType.EveryDays -> state.dayIntervalText.toIntOrNull()
                ?.takeIf { intervalDays -> intervalDays in MIN_DAY_INTERVAL..MAX_DAY_INTERVAL }
                ?.let(ReminderRecurrence::EveryDays)

            ReminderRecurrenceType.EveryMonths -> {
                val intervalMonths = state.monthlyIntervalText.toIntOrNull() ?: return null
                val dayOfMonth = state.monthlyDayOfMonthText.toIntOrNull() ?: return null
                if (intervalMonths !in MIN_MONTHLY_INTERVAL..MAX_MONTHLY_INTERVAL ||
                    dayOfMonth !in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH
                ) {
                    return null
                }
                ReminderRecurrence.EveryMonthsOnDay(intervalMonths, dayOfMonth)
            }

            ReminderRecurrenceType.CyclicIntervals -> parseCyclicIntervals(state.cyclicIntervalsText)
                ?.let(ReminderRecurrence::CyclicDayIntervals)
        }
    }

    private fun validate(state: ReminderEditorUiState, forceErrors: Boolean = false): ReminderEditorUiState {
        val titleValid = state.titleText.trim().isNotEmpty()
        val dateValid = parseDateInput(state.dateText) != null
        val timeValid = parseTimeOfDay(state.timeText) != null
        val weekdaysValid = state.recurrenceType != ReminderRecurrenceType.Weekdays || state.selectedWeekdays.isNotEmpty()
        val dayInterval = state.dayIntervalText.toIntOrNull()
        val dayIntervalValid = state.recurrenceType != ReminderRecurrenceType.EveryDays ||
            dayInterval in MIN_DAY_INTERVAL..MAX_DAY_INTERVAL
        val monthlyInterval = state.monthlyIntervalText.toIntOrNull()
        val monthlyDayOfMonth = state.monthlyDayOfMonthText.toIntOrNull()
        val monthlyIntervalValid = state.recurrenceType != ReminderRecurrenceType.EveryMonths ||
            monthlyInterval in MIN_MONTHLY_INTERVAL..MAX_MONTHLY_INTERVAL
        val monthlyDayOfMonthValid = state.recurrenceType != ReminderRecurrenceType.EveryMonths ||
            monthlyDayOfMonth in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH
        val cyclicIntervals = parseCyclicIntervals(state.cyclicIntervalsText)
        val cyclicIntervalsValid = state.recurrenceType != ReminderRecurrenceType.CyclicIntervals || cyclicIntervals != null
        return state.copy(
            titleError = if (!titleValid && (forceErrors || state.titleText.isNotEmpty())) ReminderLabels.titleError else null,
            dateError = if (!dateValid && (forceErrors || state.dateText.isNotEmpty())) UiLabels.dateError else null,
            timeError = if (!timeValid && (forceErrors || state.timeText.isNotEmpty())) UiLabels.timeError else null,
            weekdaysError = if (!weekdaysValid && forceErrors) ReminderLabels.weekdaysError else null,
            dayIntervalError = if (!dayIntervalValid && (forceErrors || state.dayIntervalText.isNotEmpty())) {
                ReminderLabels.dayIntervalError
            } else {
                null
            },
            monthlyIntervalError = if (!monthlyIntervalValid && (forceErrors || state.monthlyIntervalText.isNotEmpty())) {
                ReminderLabels.repeatMonthsError
            } else {
                null
            },
            monthlyDayOfMonthError = if (!monthlyDayOfMonthValid && (forceErrors || state.monthlyDayOfMonthText.isNotEmpty())) {
                ReminderLabels.dayOfMonthError
            } else {
                null
            },
            cyclicIntervalsError = if (!cyclicIntervalsValid && (forceErrors || state.cyclicIntervalsText.isNotEmpty())) {
                ReminderLabels.cyclicIntervalsError
            } else {
                null
            },
            isSaveEnabled = titleValid && dateValid && timeValid && weekdaysValid && dayIntervalValid &&
                monthlyIntervalValid && monthlyDayOfMonthValid && cyclicIntervalsValid && !state.isLoading,
        )
    }

    private fun parseCyclicIntervals(text: String): List<Int>? {
        val parts = text.trim().split(CYCLIC_INTERVAL_SEPARATOR).filter { part -> part.isNotBlank() }
        if (parts.isEmpty() || parts.size > MAX_CYCLIC_INTERVAL_COUNT) {
            return null
        }
        val intervals = parts.map { part -> part.toIntOrNull() ?: return null }
        return intervals.takeIf { values -> values.all { interval -> interval in MIN_DAY_INTERVAL..MAX_DAY_INTERVAL } }
    }

    private companion object {
        val CYCLIC_INTERVAL_SEPARATOR = Regex("[,;\\s]+")
        const val MIN_DAY_INTERVAL = 1
        const val MAX_DAY_INTERVAL = 3_650
        const val MAX_CYCLIC_INTERVAL_COUNT = 32
        const val MIN_MONTHLY_INTERVAL = 1
        const val MAX_MONTHLY_INTERVAL = 120
        const val MIN_DAY_OF_MONTH = 1
        const val MAX_DAY_OF_MONTH = 31
    }
}
