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
                val currentDateTime = timeProvider.currentDateTime()
                val reminders = reminderRepository.getReminders()
                    .map { reminder -> toListItem(reminder, currentDateTime) }
                    .sortedWith(compareBy(nullsLast()) { reminder -> reminder.nextOccurrence })
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

    private fun toListItem(reminder: Reminder, currentDateTime: LocalDateTime): ReminderListItemUiModel {
        val nextOccurrence = findNextReminderDateTime(reminder, currentDateTime)
        return ReminderListItemUiModel(
            id = reminder.id,
            title = reminder.title,
            recurrenceText = reminderRecurrenceLabel(reminder.recurrence),
            nextOccurrence = nextOccurrence,
            nextOccurrenceText = nextOccurrence?.let(::reminderNextLabel),
        )
    }
}
