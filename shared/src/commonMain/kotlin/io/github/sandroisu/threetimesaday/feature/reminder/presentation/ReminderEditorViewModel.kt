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
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderIdGenerator
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRecurrence
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRepository
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

    fun onOnceSelected() {
        mutableUiState.update { state -> validate(state.copy(isMonthlyRecurrence = false)) }
    }

    fun onMonthlyRecurrenceSelected() {
        mutableUiState.update { state ->
            validate(
                state.copy(
                    isMonthlyRecurrence = true,
                    monthlyDayOfMonthText = state.monthlyDayOfMonthText.ifBlank {
                        timeProvider.currentDate().day.toString()
                    },
                )
            )
        }
    }

    fun onMonthlyIntervalChanged(text: String) {
        mutableUiState.update { state -> validate(state.copy(monthlyIntervalText = text)) }
    }

    fun onMonthlyDayOfMonthChanged(text: String) {
        mutableUiState.update { state -> validate(state.copy(monthlyDayOfMonthText = text)) }
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
        )
        return when (val recurrence = reminder.recurrence) {
            ReminderRecurrence.Once -> baseState
            is ReminderRecurrence.EveryMonthsOnDay -> baseState.copy(
                isMonthlyRecurrence = true,
                monthlyIntervalText = recurrence.intervalMonths.toString(),
                monthlyDayOfMonthText = recurrence.dayOfMonth.toString(),
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
            )
        } else {
            loadedReminder?.copy(
                title = state.titleText.trim(),
                date = date,
                time = time,
                recurrence = recurrence,
            ) ?: Reminder(
                id = existingReminderId,
                title = state.titleText.trim(),
                date = date,
                time = time,
                recurrence = recurrence,
            )
        }
        if (existingReminderId == null) {
            reminderRepository.saveReminder(reminder)
        } else {
            reminderRepository.updateReminder(reminder)
        }
    }

    private fun buildRecurrence(state: ReminderEditorUiState): ReminderRecurrence? {
        if (!state.isMonthlyRecurrence) return ReminderRecurrence.Once
        val intervalMonths = state.monthlyIntervalText.toIntOrNull() ?: return null
        val dayOfMonth = state.monthlyDayOfMonthText.toIntOrNull() ?: return null
        if (intervalMonths !in MIN_MONTHLY_INTERVAL..MAX_MONTHLY_INTERVAL || dayOfMonth !in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH) {
            return null
        }
        return ReminderRecurrence.EveryMonthsOnDay(intervalMonths, dayOfMonth)
    }

    private fun validate(state: ReminderEditorUiState, forceErrors: Boolean = false): ReminderEditorUiState {
        val titleValid = state.titleText.trim().isNotEmpty()
        val dateValid = parseDateInput(state.dateText) != null
        val timeValid = parseTimeOfDay(state.timeText) != null
        val monthlyInterval = state.monthlyIntervalText.toIntOrNull()
        val monthlyDayOfMonth = state.monthlyDayOfMonthText.toIntOrNull()
        val monthlyIntervalValid = !state.isMonthlyRecurrence || monthlyInterval in MIN_MONTHLY_INTERVAL..MAX_MONTHLY_INTERVAL
        val monthlyDayOfMonthValid = !state.isMonthlyRecurrence || monthlyDayOfMonth in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH
        return state.copy(
            titleError = if (!titleValid && (forceErrors || state.titleText.isNotEmpty())) ReminderLabels.titleError else null,
            dateError = if (!dateValid && (forceErrors || state.dateText.isNotEmpty())) UiLabels.dateError else null,
            timeError = if (!timeValid && (forceErrors || state.timeText.isNotEmpty())) UiLabels.timeError else null,
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
            isSaveEnabled = titleValid && dateValid && timeValid && monthlyIntervalValid && monthlyDayOfMonthValid && !state.isLoading,
        )
    }

    private companion object {
        const val MIN_MONTHLY_INTERVAL = 1
        const val MAX_MONTHLY_INTERVAL = 24
        const val MIN_DAY_OF_MONTH = 1
        const val MAX_DAY_OF_MONTH = 31
    }
}
