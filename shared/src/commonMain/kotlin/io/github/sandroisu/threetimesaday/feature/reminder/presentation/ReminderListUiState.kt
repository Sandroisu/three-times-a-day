package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import kotlinx.datetime.LocalDateTime

internal data class ReminderListItemUiModel(
    val id: String,
    val title: String,
    val recurrenceText: UiText,
    val nextOccurrence: LocalDateTime?,
    val nextOccurrenceText: UiText?,
)

internal data class ReminderListUiState(
    val isLoading: Boolean = false,
    val reminders: List<ReminderListItemUiModel> = emptyList(),
    val errorMessage: UiText? = null,
    val notificationPermissionStatus: NotificationPermissionStatus = NotificationPermissionStatus.NotDetermined,
    val exactRemindersAllowed: Boolean = true,
    val notificationErrorMessage: UiText? = null,
)
