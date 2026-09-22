package io.github.sandroisu.threetimesaday.feature.medication.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sandroisu.threetimesaday.core.ui.AppIcons
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.IconLabel
import io.github.sandroisu.threetimesaday.core.ui.PrimaryActionButton
import io.github.sandroisu.threetimesaday.core.ui.SectionHeader

@Composable
internal fun MedicationDetailsContent(
    uiState: MedicationEditorUiState,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    reminderContent: @Composable (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.section)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                Icon(
                    AppIcons.Medication,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(AppSpacing.medium).size(28.dp),
                )
            }
            Text(uiState.nameText, style = MaterialTheme.typography.headlineLarge)
            Text(uiState.dosageText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            uiState.courseLabel?.let { label ->
                Text(label.asString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
            SectionHeader(MedicationLabels.reminder.asString())
            IconLabel(AppIcons.Clock, uiState.intakeRuleLabel?.asString().orEmpty())
            if (uiState.isDistributedRule) {
                Text(MedicationLabels.distributedHint.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
            SectionHeader(MedicationLabels.days.asString())
            Text(uiState.recurrenceLabel.asString(), style = MaterialTheme.typography.bodyLarge)
            Text(uiState.courseScheduleLabel.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        uiState.medicationId?.let { reminderContent(it) }
        PrimaryActionButton(MedicationLabels.edit.asString(), onEditClick)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            TextButton(onClick = onDeleteClick, modifier = Modifier.fillMaxWidth()) {
                Text(MedicationLabels.delete.asString(), color = MaterialTheme.colorScheme.error)
            }
            Text(MedicationLabels.deleteHint.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
