package io.github.sandroisu.threetimesaday.feature.schedule.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScheduleEditorScreen(
    onBackClick: () -> Unit,
    onScheduleSaved: () -> Unit,
    scheduleEditorViewModel: ScheduleEditorViewModel = koinViewModel()
) {
    val uiState by scheduleEditorViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(scheduleEditorViewModel) {
        scheduleEditorViewModel.scheduleSavedEvents.collect {
            onScheduleSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBackClick) {
            Text(text = "Назад")
        }
        Text(
            text = "Режим дня",
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
            TimeInputField(
                label = "Пробуждение",
                value = uiState.wakeUpTimeText,
                errorMessage = uiState.wakeUpTimeError,
                onValueChange = scheduleEditorViewModel::onWakeUpTimeChanged
            )
            TimeInputField(
                label = "Завтрак",
                value = uiState.breakfastTimeText,
                errorMessage = uiState.breakfastTimeError,
                onValueChange = scheduleEditorViewModel::onBreakfastTimeChanged
            )
            TimeInputField(
                label = "Обед",
                value = uiState.lunchTimeText,
                errorMessage = uiState.lunchTimeError,
                onValueChange = scheduleEditorViewModel::onLunchTimeChanged
            )
            TimeInputField(
                label = "Ужин",
                value = uiState.dinnerTimeText,
                errorMessage = uiState.dinnerTimeError,
                onValueChange = scheduleEditorViewModel::onDinnerTimeChanged
            )
            TimeInputField(
                label = "Сон",
                value = uiState.sleepTimeText,
                errorMessage = uiState.sleepTimeError,
                onValueChange = scheduleEditorViewModel::onSleepTimeChanged
            )
            if (uiState.generalErrorMessage != null) {
                Text(
                    text = uiState.generalErrorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = scheduleEditorViewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Сохранить")
            }
        }
    }
}

@Composable
private fun TimeInputField(
    label: String,
    value: String,
    errorMessage: String?,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        isError = errorMessage != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = {
            if (errorMessage != null) {
                Text(text = errorMessage)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
