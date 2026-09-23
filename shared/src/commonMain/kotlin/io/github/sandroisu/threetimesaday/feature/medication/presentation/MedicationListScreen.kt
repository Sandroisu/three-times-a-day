package io.github.sandroisu.threetimesaday.feature.medication.presentation

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.sandroisu.threetimesaday.core.ui.AppIcons
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.LoadingIndicator
import io.github.sandroisu.threetimesaday.core.ui.Notice
import io.github.sandroisu.threetimesaday.core.ui.PrimaryActionButton
import io.github.sandroisu.threetimesaday.core.ui.ScreenHeader
import io.github.sandroisu.threetimesaday.core.ui.SectionHeader
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun MedicationListScreen(
    onBackClick: () -> Unit,
    onAddMedicationClick: () -> Unit,
    onMedicationClick: (String) -> Unit,
    medicationListViewModel: MedicationListViewModel = koinViewModel(),
) {
    val uiState by medicationListViewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { medicationListViewModel.loadMedications() }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(AppSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            item {
                ScreenHeader(MedicationLabels.medications.asString(), onBackClick, MedicationLabels.listSubtitle.asString())
            }
            when {
                uiState.isLoading -> item { LoadingIndicator() }
                uiState.errorMessage != null -> item { Notice(uiState.errorMessage?.asString().orEmpty(), isError = true) }
                uiState.medications.isEmpty() -> item {
                    Column(Modifier.padding(vertical = AppSpacing.large), verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                        Icon(AppIcons.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        SectionHeader(MedicationLabels.emptyTitle.asString())
                        Text(MedicationLabels.emptyMessage.asString(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> items(uiState.medications, key = { it.id }) { medication ->
                    MedicationListItemCard(medication) { onMedicationClick(medication.id) }
                }
            }
        }
        Column(Modifier.padding(AppSpacing.standard)) {
            PrimaryActionButton(MedicationLabels.add.asString(), onAddMedicationClick)
        }
    }
}

@Composable
private fun MedicationListItemCard(medication: MedicationListItemUiModel, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(AppSpacing.standard),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                Text(medication.name, style = MaterialTheme.typography.titleMedium)
                Text(medication.dosageText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(medication.intakeRuleText.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(medication.recurrenceText.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                medication.courseLabel?.let { label ->
                    Text(label.asString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(AppIcons.Chevron, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
