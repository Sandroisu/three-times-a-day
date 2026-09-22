package io.github.sandroisu.threetimesaday.feature.medication.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.AppIcons
import io.github.sandroisu.threetimesaday.core.ui.DateInputField
import io.github.sandroisu.threetimesaday.core.ui.FormField
import io.github.sandroisu.threetimesaday.core.ui.LoadingIndicator
import io.github.sandroisu.threetimesaday.core.ui.Notice
import io.github.sandroisu.threetimesaday.core.ui.PrimaryActionButton
import io.github.sandroisu.threetimesaday.core.ui.ScreenHeader
import io.github.sandroisu.threetimesaday.core.ui.SectionHeader
import io.github.sandroisu.threetimesaday.core.ui.TimeInputField
import io.github.sandroisu.threetimesaday.core.ui.UiLabels
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun MedicationEditorScreen(
    medicationId: String?,
    onBackClick: () -> Unit,
    onMedicationSaved: () -> Unit,
    onMedicationDeleted: () -> Unit,
    reminderContent: @Composable (String) -> Unit,
    medicationEditorViewModel: MedicationEditorViewModel = koinViewModel(),
) {
    val uiState by medicationEditorViewModel.uiState.collectAsStateWithLifecycle()
    val contentScrollState = rememberScrollState()
    LaunchedEffect(medicationId, uiState.isEditing) { contentScrollState.scrollTo(0) }
    LaunchedEffect(medicationId) { medicationEditorViewModel.start(medicationId) }
    LaunchedEffect(medicationEditorViewModel) {
        medicationEditorViewModel.medicationSavedEvents.collect {
            medicationEditorViewModel.finishSession()
            onMedicationSaved()
        }
    }
    LaunchedEffect(medicationEditorViewModel) {
        medicationEditorViewModel.medicationDeletedEvents.collect {
            medicationEditorViewModel.finishSession()
            onMedicationDeleted()
        }
    }
    val onBack = {
        if (uiState.isEditing && medicationId != null) {
            medicationEditorViewModel.showDetails()
        } else {
            medicationEditorViewModel.finishSession()
            onBackClick()
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(contentScrollState).padding(AppSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.section),
        ) {
            ScreenHeader(
                title = when {
                    medicationId == null -> MedicationLabels.newMedication.asString()
                    uiState.isEditing -> MedicationLabels.editMedication.asString()
                    else -> MedicationLabels.details.asString()
                },
                onBackClick = onBack,
            )
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.nameText.isEmpty() && !uiState.isEditing && uiState.generalErrorMessage != null ->
                    Notice(uiState.generalErrorMessage?.asString().orEmpty(), isError = true)
                !uiState.isEditing -> {
                    uiState.generalErrorMessage?.let { Notice(it.asString(), isError = true) }
                    MedicationDetailsContent(
                        uiState = uiState,
                        onEditClick = medicationEditorViewModel::editMedication,
                        onDeleteClick = medicationEditorViewModel::requestDelete,
                        reminderContent = reminderContent,
                    )
                }
                else -> MedicationForm(
                    uiState = uiState,
                    onNameChange = medicationEditorViewModel::onNameChanged,
                    onDosageChange = medicationEditorViewModel::onDosageChanged,
                    onCourseStartDateChange = medicationEditorViewModel::onCourseStartDateChanged,
                    onCourseEndDateChange = medicationEditorViewModel::onCourseEndDateChanged,
                    onDailyRecurrenceSelected = medicationEditorViewModel::onDailyRecurrenceSelected,
                    onMonthlyRecurrenceSelected = medicationEditorViewModel::onMonthlyRecurrenceSelected,
                    onMonthlyIntervalChange = medicationEditorViewModel::onMonthlyIntervalChanged,
                    onMonthlyDayOfMonthChange = medicationEditorViewModel::onMonthlyDayOfMonthChanged,
                    onMomentSelected = medicationEditorViewModel::onIntakeMomentSelected,
                    onExactTimeSelected = medicationEditorViewModel::onExactTimeRuleSelected,
                    onTimeChange = medicationEditorViewModel::onExactTimeChanged,
                )
            }
        }
        if (uiState.isEditing && !uiState.isLoading) {
            Column(Modifier.padding(AppSpacing.standard)) {
                PrimaryActionButton(UiLabels.save.asString(), medicationEditorViewModel::save, enabled = !uiState.isSaving)
            }
        }
    }
    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = medicationEditorViewModel::dismissDeleteConfirmation,
            title = { Text(MedicationLabels.deleteTitle.asString()) },
            text = { Text(MedicationLabels.deleteMessage.asString()) },
            confirmButton = {
                TextButton(onClick = medicationEditorViewModel::confirmDelete) {
                    Text(MedicationLabels.delete.asString(), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = medicationEditorViewModel::dismissDeleteConfirmation) { Text(UiLabels.cancel.asString()) }
            },
        )
    }
}

@Composable
private fun MedicationForm(
    uiState: MedicationEditorUiState,
    onNameChange: (String) -> Unit,
    onDosageChange: (String) -> Unit,
    onCourseStartDateChange: (String) -> Unit,
    onCourseEndDateChange: (String) -> Unit,
    onDailyRecurrenceSelected: () -> Unit,
    onMonthlyRecurrenceSelected: () -> Unit,
    onMonthlyIntervalChange: (String) -> Unit,
    onMonthlyDayOfMonthChange: (String) -> Unit,
    onMomentSelected: (MedicationIntakeMoment) -> Unit,
    onExactTimeSelected: () -> Unit,
    onTimeChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.section)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.standard)) {
            SectionHeader(MedicationLabels.about.asString())
            FormField(
                label = MedicationLabels.name.asString(),
                value = uiState.nameText,
                onValueChange = onNameChange,
                error = uiState.nameError?.asString(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            )
            FormField(
                label = MedicationLabels.dosage.asString(),
                value = uiState.dosageText,
                onValueChange = onDosageChange,
                error = uiState.dosageError?.asString(),
                hint = MedicationLabels.dosageHint.asString(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
            SectionHeader(MedicationLabels.reminder.asString())
            if (uiState.isDistributedRule) {
                Text(uiState.intakeRuleLabel?.asString().orEmpty(), style = MaterialTheme.typography.bodyLarge)
                Text(MedicationLabels.distributedHint.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(MedicationLabels.reminderHint.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                    FilterChip(
                        selected = !uiState.isExactTimeVisible,
                        onClick = { onMomentSelected(uiState.selectedIntakeMoment ?: MedicationIntakeMoment.AfterWakeUp) },
                        label = { Text(MedicationLabels.bySchedule.asString()) },
                    )
                    FilterChip(selected = uiState.isExactTimeVisible, onClick = onExactTimeSelected, label = { Text(MedicationLabels.exactTime.asString()) })
                }
                if (uiState.isExactTimeVisible) {
                    TimeInputField(MedicationLabels.reminder.asString(), uiState.exactTimeText, uiState.exactTimeError?.asString(), onTimeChange)
                } else {
                    MomentSelector(uiState.selectedIntakeMoment, onMomentSelected)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
            SectionHeader(MedicationLabels.days.asString())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                FilterChip(
                    selected = !uiState.isMonthlyRecurrence,
                    onClick = onDailyRecurrenceSelected,
                    label = { Text(MedicationLabels.daily.asString()) },
                )
                FilterChip(
                    selected = uiState.isMonthlyRecurrence,
                    onClick = onMonthlyRecurrenceSelected,
                    label = { Text(MedicationLabels.monthly.asString()) },
                )
            }
            if (uiState.isMonthlyRecurrence) {
                FormField(
                    label = MedicationLabels.repeatEveryMonths.asString(),
                    value = uiState.monthlyIntervalText,
                    onValueChange = onMonthlyIntervalChange,
                    error = uiState.monthlyIntervalError?.asString(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
                FormField(
                    label = MedicationLabels.dayOfMonth.asString(),
                    value = uiState.monthlyDayOfMonthText,
                    onValueChange = onMonthlyDayOfMonthChange,
                    error = uiState.monthlyDayOfMonthError?.asString(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
                Text(MedicationLabels.shortMonthHint.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DateInputField(
                label = MedicationLabels.startDate.asString(),
                value = uiState.courseStartDateText,
                error = uiState.courseStartDateError?.asString(),
                onValueChange = onCourseStartDateChange,
            )
            DateInputField(
                label = MedicationLabels.endDate.asString(),
                value = uiState.courseEndDateText,
                error = uiState.courseEndDateError?.asString(),
                onValueChange = onCourseEndDateChange,
            )
        }
        uiState.generalErrorMessage?.let { Notice(it.asString(), isError = true) }
    }
}

@Composable
private fun MomentSelector(selectedMoment: MedicationIntakeMoment?, onMomentSelected: (MedicationIntakeMoment) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        Text(MedicationLabels.selectMoment.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(selectedMoment?.let(::medicationIntakeMomentLabel)?.asString().orEmpty(), modifier = Modifier.weight(1f))
                Icon(AppIcons.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                MedicationIntakeMoment.entries.forEach { moment ->
                    DropdownMenuItem(
                        text = { Text(medicationIntakeMomentLabel(moment).asString()) },
                        onClick = {
                            onMomentSelected(moment)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
