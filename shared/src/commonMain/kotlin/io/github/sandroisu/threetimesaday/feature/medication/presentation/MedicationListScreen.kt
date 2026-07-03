package io.github.sandroisu.threetimesaday.feature.medication.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MedicationListScreen(
    onBackClick: () -> Unit,
    onAddMedicationClick: () -> Unit,
    onMedicationClick: (String) -> Unit,
    medicationListViewModel: MedicationListViewModel = koinViewModel()
) {
    val uiState by medicationListViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(medicationListViewModel) {
        medicationListViewModel.loadMedications()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text(text = "Назад")
            }
            Button(onClick = onAddMedicationClick) {
                Text(text = "Добавить")
            }
        }
        Text(
            text = "Препараты",
            style = MaterialTheme.typography.headlineMedium
        )
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null -> Text(
                text = uiState.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )

            uiState.medications.isEmpty() -> Text(
                text = "Препараты не добавлены",
                style = MaterialTheme.typography.bodyMedium
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.medications) { medicationItem ->
                    MedicationListItemCard(
                        medicationItem = medicationItem,
                        onClick = { onMedicationClick(medicationItem.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicationListItemCard(
    medicationItem: MedicationListItemUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = medicationItem.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = medicationItem.dosageText,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = medicationItem.intakeRuleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
