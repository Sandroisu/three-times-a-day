package io.github.sandroisu.threetimesaday.feature.today.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sandroisu.threetimesaday.core.ui.AppIcons
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.IntakeColors
import io.github.sandroisu.threetimesaday.core.ui.PrimaryActionButton

@Composable
internal fun MedicationIntakeCard(
    intake: MedicationIntakeUiModel,
    isHighlighted: Boolean,
    onMarkTaken: () -> Unit,
    onMarkSkipped: () -> Unit,
    onPostpone: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (intake.status == IntakeDisplayStatus.Taken) IntakeColors.successContainer else MaterialTheme.colorScheme.surface,
        border = if (isHighlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(AppSpacing.standard), verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
            Text(intake.medicationName, style = MaterialTheme.typography.titleMedium)
            Text(
                intakeDescriptionLabel(intake.dosageText, intake.contextLabel).asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MedicationStatusIndicator(intake.status, intake.statusLabel.asString())
            if (intake.isActionable) {
                Column(Modifier.padding(top = AppSpacing.compact)) {
                    PrimaryActionButton(TodayLabels.markTaken.asString(), onMarkTaken)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                        TextButton(onClick = onPostpone) { Text(postponeActionLabel().asString()) }
                        TextButton(onClick = onMarkSkipped) { Text(TodayLabels.skip.asString()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationStatusIndicator(
    status: IntakeDisplayStatus,
    label: String,
) {
    val foreground = when (status) {
        IntakeDisplayStatus.Taken -> IntakeColors.success
        IntakeDisplayStatus.Overdue -> MaterialTheme.colorScheme.error
        IntakeDisplayStatus.Due -> MaterialTheme.colorScheme.primary
        IntakeDisplayStatus.Upcoming, IntakeDisplayStatus.Skipped -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (status) {
        IntakeDisplayStatus.Taken -> AppIcons.Check
        IntakeDisplayStatus.Overdue -> AppIcons.Warning
        IntakeDisplayStatus.Upcoming, IntakeDisplayStatus.Due, IntakeDisplayStatus.Skipped -> AppIcons.Clock
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = foreground)
    }
}
