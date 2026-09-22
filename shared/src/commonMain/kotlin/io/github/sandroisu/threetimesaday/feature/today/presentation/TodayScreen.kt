package io.github.sandroisu.threetimesaday.feature.today.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.sandroisu.threetimesaday.core.ui.AppIcons
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.IconLabel
import io.github.sandroisu.threetimesaday.core.ui.LoadingIndicator
import io.github.sandroisu.threetimesaday.core.ui.Notice
import io.github.sandroisu.threetimesaday.core.ui.PrimaryActionButton
import io.github.sandroisu.threetimesaday.core.ui.PrivacyPolicyDialog
import io.github.sandroisu.threetimesaday.core.ui.ScreenHeader
import io.github.sandroisu.threetimesaday.core.ui.SectionHeader
import io.github.sandroisu.threetimesaday.core.ui.UiLabels
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private const val HIGHLIGHT_VISIBLE_MILLIS = 2500L
private const val DISPLAY_CLOCK_REFRESH_MILLIS = 1000L
private const val INTAKE_LIST_HEADER_COUNT = 3

@Composable
internal fun TodayScreen(
    onEditScheduleClick: () -> Unit,
    onEditMedicationsClick: () -> Unit,
    todayViewModel: TodayViewModel = koinViewModel(),
) {
    val uiState by todayViewModel.uiState.collectAsStateWithLifecycle()
    var isPrivacyPolicyVisible by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { todayViewModel.loadToday() }
    RefreshIntakeTime(todayViewModel)
    TodayContent(
        uiState = uiState,
        onEditScheduleClick = onEditScheduleClick,
        onEditMedicationsClick = onEditMedicationsClick,
        onMarkTaken = todayViewModel::markIntakeTaken,
        onMarkSkipped = todayViewModel::markIntakeSkipped,
        onPostpone = todayViewModel::postponeIntake,
        onHighlightShown = todayViewModel::clearHighlightedEvent,
        onEnableNotificationsClick = todayViewModel::requestNotificationPermission,
        onOpenSettingsClick = todayViewModel::openNotificationSettings,
        onOpenExactReminderSettingsClick = todayViewModel::openExactReminderSettings,
        onPrivacyPolicyClick = { isPrivacyPolicyVisible = true },
        onRetry = todayViewModel::loadToday,
    )
    if (isPrivacyPolicyVisible) {
        PrivacyPolicyDialog(onDismissRequest = { isPrivacyPolicyVisible = false })
    }
}

@Composable
internal fun RefreshIntakeTime(todayViewModel: TodayViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(todayViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(DISPLAY_CLOCK_REFRESH_MILLIS)
                todayViewModel.refreshDisplayedTime()
            }
        }
    }
}

@Composable
internal fun TodayContent(
    uiState: TodayUiState,
    onEditScheduleClick: () -> Unit,
    onEditMedicationsClick: () -> Unit,
    onMarkTaken: (String) -> Unit,
    onMarkSkipped: (String) -> Unit,
    onPostpone: (String) -> Unit,
    onHighlightShown: () -> Unit,
    onEnableNotificationsClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onOpenExactReminderSettingsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onRetry: () -> Unit,
) {
    val listState = rememberLazyListState()
    val permissionPrompt = notificationPermissionPrompt(uiState.notificationPermissionStatus)
    val contentState = todayContentState(uiState.isLoading, uiState.errorMessage != null, uiState.intakeEvents.isNotEmpty())
    val eventIds = uiState.intakeGroups.flatMap { group -> group.intakes.map { it.eventId } }
    val headerItemCount = INTAKE_LIST_HEADER_COUNT + listOf(
        permissionPrompt != null,
        !uiState.exactRemindersAllowed,
        uiState.notificationErrorMessage != null,
    ).count { it }
    LaunchedEffect(uiState.highlightedEventId, eventIds, contentState, headerItemCount) {
        val highlightedId = uiState.highlightedEventId ?: return@LaunchedEffect
        if (contentState != TodayContentState.Events) return@LaunchedEffect
        var itemIndex = headerItemCount
        for (group in uiState.intakeGroups) {
            val indexInGroup = group.intakes.indexOfFirst { it.eventId == highlightedId }
            if (indexInGroup >= 0) {
                listState.animateScrollToItem(itemIndex + 1 + indexInGroup)
                delay(HIGHLIGHT_VISIBLE_MILLIS)
                onHighlightShown()
                break
            }
            itemIndex += group.intakes.size + 1
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = AppSpacing.standard, vertical = AppSpacing.section),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.standard)) {
                ScreenHeader(uiState.screenTitle.asString(), subtitle = uiState.dateTitle?.asString().orEmpty().takeIf { it.isNotEmpty() })
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                    OutlinedButton(onClick = onEditMedicationsClick, modifier = Modifier.weight(1f)) {
                        Text(TodayLabels.medications.asString())
                    }
                    OutlinedButton(onClick = onEditScheduleClick, modifier = Modifier.weight(1f)) {
                        Text(TodayLabels.schedule.asString())
                    }
                }
            }
        }
        if (permissionPrompt != null) {
            item(key = "permission") {
                Notice(permissionPrompt.message.asString()) {
                    TextButton(onClick = when (permissionPrompt.action) {
                        NotificationPermissionAction.Request -> onEnableNotificationsClick
                        NotificationPermissionAction.OpenSettings -> onOpenSettingsClick
                    }) { Text(permissionPrompt.actionLabel.asString()) }
                }
            }
        }
        if (!uiState.exactRemindersAllowed) {
            item(key = "exact") {
                Notice(TodayLabels.exactReminders.asString(), isError = true) {
                    TextButton(onClick = onOpenExactReminderSettingsClick) {
                        Text(TodayLabels.exactRemindersAction.asString())
                    }
                }
            }
        }
        item(key = "privacy-policy") {
            TextButton(onClick = onPrivacyPolicyClick) { Text(UiLabels.privacyPolicy.asString()) }
        }
        uiState.notificationErrorMessage?.let { message ->
            item(key = "notification-error") { Notice(message.asString(), isError = true) }
        }
        when (contentState) {
            TodayContentState.Loading -> item { LoadingIndicator() }
            TodayContentState.Error -> item {
                Notice(uiState.errorMessage?.asString().orEmpty(), isError = true) {
                    TextButton(onClick = onRetry) { Text(TodayLabels.retry.asString()) }
                }
            }
            TodayContentState.Empty -> item {
                Column(
                    Modifier.padding(vertical = AppSpacing.large),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.standard),
                ) {
                    Icon(AppIcons.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    SectionHeader(TodayLabels.emptyTitle.asString())
                    Text(TodayLabels.emptyMessage.asString(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryActionButton(TodayLabels.addMedication.asString(), onEditMedicationsClick)
                }
            }
            TodayContentState.Events -> {
                item(key = "progress") {
                    IntakeProgress(uiState.takenCount, uiState.intakeEvents.size, uiState.completionFraction)
                }
                item(key = "schedule-title") {
                    Column(Modifier.padding(top = AppSpacing.medium)) { SectionHeader(TodayLabels.plan.asString()) }
                }
                uiState.intakeGroups.forEach { group ->
                    item(key = "time-${group.intakes.first().scheduledDateTime}") {
                        IconLabel(
                            AppIcons.Clock,
                            group.timeLabel.asString(),
                            modifier = Modifier.padding(top = AppSpacing.compact),
                        )
                    }
                    items(group.intakes, key = { it.eventId }) { intake ->
                        MedicationIntakeCard(
                            intake = intake,
                            isHighlighted = intake.eventId == uiState.highlightedEventId,
                            onMarkTaken = { onMarkTaken(intake.eventId) },
                            onMarkSkipped = { onMarkSkipped(intake.eventId) },
                            onPostpone = { onPostpone(intake.eventId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntakeProgress(takenCount: Int, totalCount: Int, completionFraction: Float) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.fillMaxWidth().padding(AppSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            Text(TodayLabels.dailyProgress.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(intakeProgressLabel(takenCount, totalCount).asString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            LinearProgressIndicator(
                progress = { completionFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                drawStopIndicator = {},
            )
            if (takenCount == totalCount) {
                Text(TodayLabels.complete.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
