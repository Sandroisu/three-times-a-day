package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionAction
import io.github.sandroisu.threetimesaday.core.notification.notificationPermissionPrompt
import io.github.sandroisu.threetimesaday.core.ui.AppIcons
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.LoadingIndicator
import io.github.sandroisu.threetimesaday.core.ui.Notice
import io.github.sandroisu.threetimesaday.core.ui.PrimaryActionButton
import io.github.sandroisu.threetimesaday.core.ui.ScreenHeader
import io.github.sandroisu.threetimesaday.core.ui.SectionHeader
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ReminderListScreen(
    onAddReminderClick: () -> Unit,
    onReminderClick: (String) -> Unit,
    reminderListViewModel: ReminderListViewModel = koinViewModel(),
) {
    val uiState by reminderListViewModel.uiState.collectAsStateWithLifecycle()
    val permissionPrompt = notificationPermissionPrompt(uiState.notificationPermissionStatus)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { reminderListViewModel.loadReminders() }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(AppSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            item {
                ScreenHeader(ReminderLabels.reminders.asString(), subtitle = ReminderLabels.listSubtitle.asString())
            }
            if (permissionPrompt != null) {
                item(key = "notification-permission") {
                    Notice(permissionPrompt.message.asString()) {
                        TextButton(
                            onClick = when (permissionPrompt.action) {
                                NotificationPermissionAction.Request -> reminderListViewModel::requestNotificationPermission
                                NotificationPermissionAction.OpenSettings -> reminderListViewModel::openNotificationSettings
                            }
                        ) {
                            Text(permissionPrompt.actionLabel.asString())
                        }
                    }
                }
            }
            if (!uiState.exactRemindersAllowed) {
                item(key = "exact-reminders") {
                    Notice(ReminderLabels.exactReminders.asString(), isError = true) {
                        TextButton(onClick = reminderListViewModel::openExactReminderSettings) {
                            Text(ReminderLabels.exactRemindersAction.asString())
                        }
                    }
                }
            }
            uiState.notificationErrorMessage?.let { notificationError ->
                item(key = "notification-error") {
                    Notice(notificationError.asString(), isError = true)
                }
            }
            when {
                uiState.isLoading -> item { LoadingIndicator() }
                uiState.errorMessage != null -> item { Notice(uiState.errorMessage?.asString().orEmpty(), isError = true) }
                uiState.reminders.isEmpty() -> item {
                    Column(Modifier.padding(vertical = AppSpacing.large), verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                        Icon(AppIcons.Clock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        SectionHeader(ReminderLabels.emptyTitle.asString())
                        Text(ReminderLabels.emptyMessage.asString(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> items(uiState.reminders, key = { reminder -> reminder.id }) { reminder ->
                    ReminderListItemCard(reminder) { onReminderClick(reminder.id) }
                }
            }
        }
        Column(Modifier.padding(AppSpacing.standard)) {
            PrimaryActionButton(ReminderLabels.add.asString(), onAddReminderClick)
        }
    }
}

@Composable
private fun ReminderListItemCard(reminder: ReminderListItemUiModel, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(AppSpacing.standard),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                Text(reminder.recurrenceText.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                reminder.nextOccurrenceText?.let { nextOccurrenceText ->
                    Text(nextOccurrenceText.asString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(AppIcons.Chevron, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
