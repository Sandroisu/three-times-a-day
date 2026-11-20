package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.settings.AppSettingsOpener
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.reminder.domain.FindNextReminderDateTimeUseCase
import io.github.sandroisu.threetimesaday.feature.reminder.domain.Reminder
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRepository
import io.github.sandroisu.threetimesaday.feature.reminder.domain.RescheduleRemindersUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

internal class ReminderListViewModel(
    private val reminderRepository: ReminderRepository,
    private val findNextReminderDateTime: FindNextReminderDateTimeUseCase,
    private val timeProvider: TimeProvider,
    private val medicationReminderScheduler: MedicationReminderScheduler,
    private val rescheduleReminders: RescheduleRemindersUseCase,
    private val appSettingsOpener: AppSettingsOpener,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(ReminderListUiState())
    val uiState: StateFlow<ReminderListUiState> = mutableUiState.asStateFlow()

    fun loadReminders() {
        viewModelScope.launch {
            mutableUiState.update { currentState -> currentState.copy(isLoading = true, errorMessage = null) }
            try {
                val reminders = readListItems()
                mutableUiState.update { currentState ->
                    currentState.copy(isLoading = false, reminders = reminders)
                }
                refreshNotificationState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (loadFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(isLoading = false, errorMessage = ReminderLabels.listError)
                }
            }
        }
    }

    fun onReminderEnabledChanged(reminderId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            mutableUiState.update { state -> state.copy(toggleErrorMessage = null) }
            try {
                val storedReminder = reminderRepository.getReminders()
                    .firstOrNull { reminder -> reminder.id == reminderId }
                // The reminder may be deleted in the editor while the list still shows it.
                // Updating it would add it back, so only refresh the list then.
                if (storedReminder != null) {
                    reminderRepository.updateReminder(storedReminder.copy(isEnabled = isEnabled))
                }
                val reminders = readListItems()
                mutableUiState.update { state -> state.copy(reminders = reminders) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (toggleFailure: Exception) {
                mutableUiState.update { state -> state.copy(toggleErrorMessage = ReminderLabels.toggleError) }
                return@launch
            }
            refreshNotificationState()
        }
    }

    fun requestNotificationPermission() {
        viewModelScope.launch {
            try {
                val permissionStatus = medicationReminderScheduler.requestPermission()
                mutableUiState.update { state -> state.copy(notificationPermissionStatus = permissionStatus) }
                rescheduleReminders()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (notificationFailure: Exception) {
                mutableUiState.update { state ->
                    state.copy(notificationErrorMessage = ReminderLabels.notificationError)
                }
            }
        }
    }

    fun openNotificationSettings() {
        appSettingsOpener.openAppSettings()
    }

    fun openExactReminderSettings() {
        appSettingsOpener.openExactReminderSettings()
    }

    private suspend fun refreshNotificationState() {
        try {
            val permissionStatus = medicationReminderScheduler.getPermissionStatus()
            val exactRemindersAllowed = medicationReminderScheduler.areExactRemindersAllowed()
            rescheduleReminders()
            mutableUiState.update { state ->
                state.copy(
                    notificationPermissionStatus = permissionStatus,
                    exactRemindersAllowed = exactRemindersAllowed,
                    notificationErrorMessage = null,
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (notificationFailure: Exception) {
            mutableUiState.update { state ->
                state.copy(notificationErrorMessage = ReminderLabels.notificationError)
            }
        }
    }

    private suspend fun readListItems(): List<ReminderListItemUiModel> {
        val currentDateTime = timeProvider.currentDateTime()
        return reminderRepository.getReminders()
            .map { reminder -> toListItem(reminder, currentDateTime) }
            .sortedWith(
                compareByDescending<ReminderListItemUiModel> { reminder -> reminder.isEnabled }
                    .thenBy(nullsLast()) { reminder -> reminder.nextOccurrence },
            )
    }

    private fun toListItem(reminder: Reminder, currentDateTime: LocalDateTime): ReminderListItemUiModel {
        val nextOccurrence = if (reminder.isEnabled) findNextReminderDateTime(reminder, currentDateTime) else null
        return ReminderListItemUiModel(
            id = reminder.id,
            title = reminder.title,
            recurrenceText = reminderRecurrenceLabel(reminder.recurrence),
            nextOccurrence = nextOccurrence,
            nextOccurrenceText = if (reminder.isEnabled) nextOccurrence?.let(::reminderNextLabel) else ReminderLabels.paused,
            isEnabled = reminder.isEnabled,
        )
    }
}
