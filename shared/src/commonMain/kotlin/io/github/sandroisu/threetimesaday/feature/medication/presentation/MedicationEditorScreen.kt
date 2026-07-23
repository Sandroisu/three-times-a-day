import io.github.sandroisu.threetimesaday.feature.medication.presentation.MedicationEditorViewModel
import io.github.sandroisu.threetimesaday.feature.medication.presentation.medicationIntakeMomentLabel

M`` §]\]=['-pfgrte4dwsqa    Z~Ты исполнитель, не ревьюер.

Задача: изучи текущую модель medication/course в three-times-a-day и добавь минимальный course-based контекст в UI, если данные уже доступны без изменения архитектуры.

Цель:
Пользователь должен лучше понимать, где он находится внутри курса лекарства. Например:

“День 3 из 7”
или “До 14 июля”
или другой уже поддерживаемый текущей моделью короткий course label.

Сначала проверь существующие поля модели и use case’ы. Не добавляй БД, сервер, auth, push, navigation library, новые Gradle-модули. Не меняй applicationId/namespace/root package. Не ломай notification tap flow и reminder lifecycle. Без TODO/Temp/Foo/Bar/Sample. Без комментариев в коде. Не пушить.

Если модель уже содержит даты/длительность курса:

добавь небольшой label в Medication list card и/или Today event card;
добавь/обнови pure tests для formatter/label logic, если есть практичное место.

Если данных для course label в модели нет:

не придумывай новую большую модель;
отчитайся, каких именно полей не хватает и предложи минимальный следующий implementation step.

Запусти:
./gradlew :shared
./gradlew :shared
./gradlew :androidApp
./gradlew :shared
./gradlew :shared

Отчёт коротко:

Что реализовано или почему не реализовано.
Изменённые файлы.
Результаты test/build команд.
Ручные проверки.
Блокеры/риски./**/package io.github.sandroisu.threetimesaday.feature.medication
    .presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MedicationEditorScreen(
    medicationId: String?,
    onBackClick: () -> Unit,
    onMedicationSaved: () -> Unit,
    onMedicationDeleted: () -> Unit,
    medicationEditorViewModel: MedicationEditorViewModel = koinViewModel()
) {
    val uiState by medicationEditorViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(medicationId) {
        medicationEditorViewModel.start(medicationId)
    }
    LaunchedEffect(medicationEditorViewModel) {
        medicationEditorViewModel.medicationSavedEvents.collect { onMedicationSaved() }
    }
    LaunchedEffect(medicationEditorViewModel) {
        medicationEditorViewModel.medicationDeletedEvents.collect { onMedicationDeleted() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBackClick) {
            Text(text = "Назад")
        }
        Text(
            text = if (medicationId == null) "Новый препарат" else "Препарат",
            style = MaterialTheme.typography.headlineMedium
        )
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            OutlinedTextField(
                value = uiState.nameText,
                onValueChange = medicationEditorViewModel::onNameChanged,
                label = { Text(text = "Название") },
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = { uiState.nameError?.let { message -> Text(text = message) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.dosageText,
                onValueChange = medicationEditorViewModel::onDosageChanged,
                label = { Text(text = "Дозировка") },
                singleLine = true,
                isError = uiState.dosageError != null,
                supportingText = { uiState.dosageError?.let { message -> Text(text = message) } },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Когда принимать",
                style = MaterialTheme.typography.titleMedium
            )
            MedicationIntakeMoment.entries.forEach { intakeMoment ->
                IntakeOptionRow(
                    label = medicationIntakeMomentLabel(intakeMoment),
                    selected = !uiState.isExactTimeVisible && uiState.selectedIntakeMoment == intakeMoment,
                    onClick = { medicationEditorViewModel.onIntakeMomentSelected(intakeMoment) }
                )
            }
            IntakeOptionRow(
                label = "В точное время",
                selected = uiState.isExactTimeVisible,
                onClick = medicationEditorViewModel::onExactTimeRuleSelected
            )
            if (uiState.isExactTimeVisible) {
                OutlinedTextField(
                    value = uiState.exactTimeText,
                    onValueChange = medicationEditorViewModel::onExactTimeChanged,
                    label = { Text(text = "Время (HH:mm)") },
                    singleLine = true,
                    isError = uiState.exactTimeError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { uiState.exactTimeError?.let { message -> Text(text = message) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (uiState.generalErrorMessage != null) {
                Text(
                    text = uiState.generalErrorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = medicationEditorViewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Сохранить")
            }
            if (uiState.isDeleteVisible) {
                OutlinedButton(
                    onClick = medicationEditorViewModel::requestDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Удалить")
                }
            }
        }
    }

    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = medicationEditorViewModel::dismissDeleteConfirmation,
            title = { Text(text = "Удалить препарат?") },
            text = {
                Text(text = "Препарат и его напоминания будут удалены. Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(onClick = medicationEditorViewModel::confirmDelete) {
                    Text(text = "Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = medicationEditorViewModel::dismissDeleteConfirmation) {
                    Text(text = "Отмена")
                }
            }
        )
    }
}

@Composable
private fun IntakeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label)
    }
}
