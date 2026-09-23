package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.DateInputField
import io.github.sandroisu.threetimesaday.core.ui.FormField
import io.github.sandroisu.threetimesaday.core.ui.LoadingIndicator
import io.github.sandroisu.threetimesaday.core.ui.Notice
import io.github.sandroisu.threetimesaday.core.ui.PrimaryActionButton
import io.github.sandroisu.threetimesaday.core.ui.ScreenHeader
import io.github.sandroisu.threetimesaday.core.ui.SectionHeader
import io.github.sandroisu.threetimesaday.core.ui.TimeInputField
import io.github.sandroisu.threetimesaday.core.ui.UiLabels
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ReminderEditorScreen(
    reminderId: String?,
    onBackClick: () -> Unit,
    onReminderSaved: () -> Unit,
    onReminderDeleted: () -> Unit,
    reminderEditorViewModel: ReminderEditorViewModel = koinViewModel(),
) {
    val uiState by reminderEditorViewModel.uiState.collectAsStateWithLifecycle()
    val contentScrollState = rememberScrollState()
    LaunchedEffect(reminderId) {
        contentScrollState.scrollTo(0)
        reminderEditorViewModel.start(reminderId)
    }
    LaunchedEffect(reminderEditorViewModel) {
        reminderEditorViewModel.reminderSavedEvents.collect {
            reminderEditorViewModel.finishSession()
            onReminderSaved()
        }
    }
    LaunchedEffect(reminderEditorViewModel) {
        reminderEditorViewModel.reminderDeletedEvents.collect {
            reminderEditorViewModel.finishSession()
            onReminderDeleted()
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(contentScrollState).padding(AppSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.section),
        ) {
            ScreenHeader(
                title = if (reminderId == null) ReminderLabels.newReminder.asString() else ReminderLabels.editReminder.asString(),
                onBackClick = {
                    reminderEditorViewModel.finishSession()
                    onBackClick()
                },
            )
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.generalErrorMessage != null && uiState.titleText.isEmpty() ->
                    Notice(uiState.generalErrorMessage?.asString().orEmpty(), isError = true)
                else -> ReminderForm(
                    uiState = uiState,
                    onTitleChanged = reminderEditorViewModel::onTitleChanged,
                    onDateChanged = reminderEditorViewModel::onDateChanged,
                    onTimeChanged = reminderEditorViewModel::onTimeChanged,
                    onOnceSelected = reminderEditorViewModel::onOnceSelected,
                    onMonthlyRecurrenceSelected = reminderEditorViewModel::onMonthlyRecurrenceSelected,
                    onMonthlyIntervalChanged = reminderEditorViewModel::onMonthlyIntervalChanged,
                    onMonthlyDayOfMonthChanged = reminderEditorViewModel::onMonthlyDayOfMonthChanged,
                )
            }
        }
        if (!uiState.isLoading && (uiState.generalErrorMessage == null || uiState.titleText.isNotEmpty())) {
            Column(Modifier.padding(AppSpacing.standard), verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                PrimaryActionButton(UiLabels.save.asString(), reminderEditorViewModel::save, enabled = !uiState.isSaving)
                if (reminderId != null) {
                    TextButton(onClick = reminderEditorViewModel::requestDelete, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)) {
                        Text(ReminderLabels.delete.asString(), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = reminderEditorViewModel::dismissDeleteConfirmation,
            title = { Text(ReminderLabels.deleteTitle.asString()) },
            text = { Text(ReminderLabels.deleteMessage.asString()) },
            confirmButton = {
                TextButton(onClick = reminderEditorViewModel::confirmDelete) {
                    Text(ReminderLabels.delete.asString(), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = reminderEditorViewModel::dismissDeleteConfirmation) {
                    Text(UiLabels.cancel.asString())
                }
            },
        )
    }
}

@Composable
private fun ReminderForm(
    uiState: ReminderEditorUiState,
    onTitleChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onTimeChanged: (String) -> Unit,
    onOnceSelected: () -> Unit,
    onMonthlyRecurrenceSelected: () -> Unit,
    onMonthlyIntervalChanged: (String) -> Unit,
    onMonthlyDayOfMonthChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.section)) {
        FormField(
            label = ReminderLabels.title.asString(),
            value = uiState.titleText,
            onValueChange = onTitleChanged,
            error = uiState.titleError?.asString(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.standard)) {
            SectionHeader(ReminderLabels.repeat.asString())
            DateInputField(
                label = ReminderLabels.date.asString(),
                value = uiState.dateText,
                error = uiState.dateError?.asString(),
                onValueChange = onDateChanged,
            )
            TimeInputField(
                label = ReminderLabels.time.asString(),
                value = uiState.timeText,
                error = uiState.timeError?.asString(),
                onValueChange = onTimeChanged,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                FilterChip(
                    selected = !uiState.isMonthlyRecurrence,
                    onClick = onOnceSelected,
                    label = { Text(ReminderLabels.once.asString()) },
                )
                FilterChip(
                    selected = uiState.isMonthlyRecurrence,
                    onClick = onMonthlyRecurrenceSelected,
                    label = { Text(ReminderLabels.monthly.asString()) },
                )
            }
            if (uiState.isMonthlyRecurrence) {
                FormField(
                    label = ReminderLabels.repeatEveryMonths.asString(),
                    value = uiState.monthlyIntervalText,
                    onValueChange = onMonthlyIntervalChanged,
                    error = uiState.monthlyIntervalError?.asString(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
                FormField(
                    label = ReminderLabels.dayOfMonth.asString(),
                    value = uiState.monthlyDayOfMonthText,
                    onValueChange = onMonthlyDayOfMonthChanged,
                    error = uiState.monthlyDayOfMonthError?.asString(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                )
                Text(
                    ReminderLabels.shortMonthHint.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        uiState.generalErrorMessage?.let { errorMessage -> Notice(errorMessage.asString(), isError = true) }
    }
}
