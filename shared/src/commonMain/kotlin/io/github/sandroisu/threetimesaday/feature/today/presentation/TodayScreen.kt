package io.github.sandroisu.threetimesaday.feature.today.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent
import org.koin.compose.viewmodel.koinViewModel

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

            else -> IntakeEventList(intakeEvents = uiState.intakeEvents)
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
private fun IntakeEventList(intakeEvents: List<MedicationIntakeEvent>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(intakeEvents) { intakeEvent ->
            IntakeEventCard(intakeEvent = intakeEvent)
        }
    }
}

@Composable
private fun IntakeEventCard(intakeEvent: MedicationIntakeEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
        }
    }
}
