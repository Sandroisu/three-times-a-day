package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent
import kotlinx.datetime.LocalDateTime

internal data class TodayUiState(
    val screenTitle: UiText = TodayLabels.title,
    val dateTitle: UiText? = null,
    val isLoading: Boolean = false,
    val intakeEvents: List<MedicationIntakeEvent> = emptyList(),
    val currentDateTime: LocalDateTime? = null,
    val errorMessage: UiText? = null,
    val notificationPermissionStatus: NotificationPermissionStatus = NotificationPermissionStatus.NotDetermined,
    val notificationErrorMessage: UiText? = null,
    val exactRemindersAllowed: Boolean = true,
    val highlightedEventId: String? = null,
    internal val intakeGroups: List<IntakeTimeGroup> = emptyList(),
    val completionFraction: Float = 0f,
    val takenCount: Int = 0,
    val upcomingIntakes: List<MedicationIntakeEvent> = emptyList(),
    val upcomingIntakesError: UiText? = null,
)
