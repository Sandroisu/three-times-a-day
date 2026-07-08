package io.github.sandroisu.threetimesaday.feature.today.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent
import kotlinx.datetime.LocalDateTime
import org.koin.compose.viewmodel.koinViewModel

private const val HIGHLIGHT_VISIBLE_MILLIS = 2500L

@Composable
fun TodayScreen(
    onEditScheduleClick: () -> Unit,
    onEditMedicationsClick: () -> Unit,
    todayViewModel: TodayViewModel = koinViewModel()
) {
    val uiState by todayViewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = uiState.screenTitle,
            style = MaterialTheme.typography.headlineMedium
        )
        if (uiState.dateTitle.isNotEmpty()) {
            Text(
                text = uiState.dateTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onEditScheduleClick) {
                Text(text = "Режим дня")
            }
            Button(onClick = onEditMedicationsClick) {
                Text(text = "Препараты")
            }
        }
        NotificationStatusBlock(
            permissionStatus = uiState.notificationPermissionStatus,
            onEnableNotificationsClick = todayViewModel::requestNotificationPermission,
            onOpenSettingsClick = todayViewModel::openNotificationSettings
        )
        if (!uiState.exactRemindersAllowed) {
            Text(
                text = "Точные напоминания отключены в системе — уведомления могут приходить с задержкой. " +
                    "Включите точные будильники в настройках, чтобы напоминания приходили вовремя.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (uiState.notificationErrorMessage != null) {
            Text(
                text = uiState.notificationErrorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        when {
            uiState.isLoading -> LoadingState()
            uiState.errorMessage != null -> Text(
                text = uiState.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )

            uiState.intakeEvents.isEmpty() -> Text(
                text = "На сегодня приёмов пока нет",
                style = MaterialTheme.typography.bodyMedium
            )

            else -> IntakeEventList(
                intakeEvents = uiState.intakeEvents,
                highlightedEventId = uiState.highlightedEventId,
                currentDateTime = uiState.currentDateTime,
                onMarkTaken = todayViewModel::markIntakeTaken,
                onMarkSkipped = todayViewModel::markIntakeSkipped,
                onPostpone = todayViewModel::postponeIntake,
                onHighlightShown = todayViewModel::clearHighlightedEvent
            )
        }
    }
}

@Composable
private fun NotificationStatusBlock(
    permissionStatus: NotificationPermissionStatus,
    onEnableNotificationsClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    val prompt = notificationPermissionPrompt(permissionStatus) ?: return
    val onActionClick = when (prompt.action) {
        NotificationPermissionAction.Request -> onEnableNotificationsClick
        NotificationPermissionAction.OpenSettings -> onOpenSettingsClick
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = prompt.message,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onActionClick) {
                Text(text = prompt.actionLabel)
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun IntakeEventList(
    intakeEvents: List<MedicationIntakeEvent>,
    highlightedEventId: String?,
    currentDateTime: LocalDateTime?,
    onMarkTaken: (String) -> Unit,
    onMarkSkipped: (String) -> Unit,
    onPostpone: (String) -> Unit,
    onHighlightShown: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(highlightedEventId, intakeEvents) {
        if (highlightedEventId == null) {
            return@LaunchedEffect
        }
        val highlightedIndex = intakeEvents.indexOfFirst { event -> event.eventId == highlightedEventId }
        if (highlightedIndex < 0) {
            return@LaunchedEffect
        }
        listState.animateScrollToItem(highlightedIndex)
        delay(HIGHLIGHT_VISIBLE_MILLIS)
        onHighlightShown()
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(intakeEvents) { intakeEvent ->
            IntakeEventCard(
                intakeEvent = intakeEvent,
                isHighlighted = intakeEvent.eventId == highlightedEventId,
                isExpired = currentDateTime != null &&
                    isMedicationIntakeEventExpired(intakeEvent, currentDateTime),
                onMarkTaken = onMarkTaken,
                onMarkSkipped = onMarkSkipped,
                onPostpone = onPostpone
            )
        }
    }
}

@Composable
private fun IntakeEventCard(
    intakeEvent: MedicationIntakeEvent,
    isHighlighted: Boolean,
    isExpired: Boolean,
    onMarkTaken: (String) -> Unit,
    onMarkSkipped: (String) -> Unit,
    onPostpone: (String) -> Unit
) {
    val cardColors = if (isHighlighted) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    val cardModifier = if (isHighlighted) {
        Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
    } else {
        Modifier.fillMaxWidth()
    }
    Card(modifier = cardModifier, colors = cardColors) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${formatTimeOfDay(intakeEvent.scheduledDateTime.time)} · ${intakeEvent.medicationName}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = intakeEvent.dosageText,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = intakeMomentLabel(intakeEvent.intakeMoment),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = intakeStatusLabel(intakeEvent.status),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (isExpired) {
                Text(
                    text = "Сегодня уже прошло",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                IntakeEventActions(
                    intakeEvent = intakeEvent,
                    onMarkTaken = onMarkTaken,
                    onMarkSkipped = onMarkSkipped,
                    onPostpone = onPostpone
                )
            }
        }
    }
}

@Composable
private fun IntakeEventActions(
    intakeEvent: MedicationIntakeEvent,
    onMarkTaken: (String) -> Unit,
    onMarkSkipped: (String) -> Unit,
    onPostpone: (String) -> Unit
) {
    val actionsVisible = intakeEvent.status == MedicationIntakeStatus.Scheduled ||
        intakeEvent.status == MedicationIntakeStatus.Postponed
    if (actionsVisible) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { onMarkTaken(intakeEvent.eventId) }) {
                Text(text = "Принял")
            }
            OutlinedButton(onClick = { onMarkSkipped(intakeEvent.eventId) }) {
                Text(text = "Пропустить")
            }
            TextButton(onClick = { onPostpone(intakeEvent.eventId) }) {
                Text(text = "Отложить")
            }
        }
    }
}
