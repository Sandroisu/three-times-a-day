package io.github.sandroisu.threetimesaday.feature.today.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sandroisu.threetimesaday.core.time.formatDayMonth
import io.github.sandroisu.threetimesaday.core.ui.AppIcons
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.LoadingIndicator
import io.github.sandroisu.threetimesaday.core.ui.Notice
import io.github.sandroisu.threetimesaday.core.ui.ScreenHeader
import io.github.sandroisu.threetimesaday.core.ui.SectionHeader
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeAdherence
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeHistoryDay
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeHistoryPeriod
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationAdherence
import org.koin.compose.viewmodel.koinViewModel

private const val FULL_PERCENT = 100f
private const val DAY_SUMMARY_SEPARATOR = " · "

@Composable
internal fun IntakeHistoryScreen(
    onBackClick: () -> Unit,
    intakeHistoryViewModel: IntakeHistoryViewModel = koinViewModel(),
) {
    val uiState by intakeHistoryViewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = AppSpacing.standard, vertical = AppSpacing.section),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        item(key = "header") {
            ScreenHeader(IntakeHistoryLabels.title.asString(), onBackClick = onBackClick)
        }
        item(key = "periods") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                IntakeHistoryPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = uiState.selectedPeriod == period,
                        onClick = { intakeHistoryViewModel.selectPeriod(period) },
                        label = { Text(intakeHistoryPeriodLabel(period).asString()) },
                    )
                }
            }
        }
        val errorMessage = uiState.errorMessage
        val overallAdherence = uiState.overallAdherence
        when {
            uiState.isLoading -> item(key = "loading") { LoadingIndicator() }
            errorMessage != null -> item(key = "error") {
                Notice(errorMessage.asString(), isError = true) {
                    TextButton(onClick = intakeHistoryViewModel::loadHistory) { Text(TodayLabels.retry.asString()) }
                }
            }
            !uiState.hasMedications -> item(key = "empty") { NoMedications() }
            overallAdherence != null -> {
                item(key = "overall") { OverallAdherence(overallAdherence) }
                item(key = "medications-title") {
                    Column(Modifier.padding(top = AppSpacing.medium)) { SectionHeader(IntakeHistoryLabels.byMedication.asString()) }
                }
                items(uiState.medicationAdherence, key = { medication -> "medication-${medication.medicationId}" }) { medication ->
                    MedicationAdherenceRow(medication)
                }
                item(key = "days-title") {
                    Column(Modifier.padding(top = AppSpacing.medium)) { SectionHeader(IntakeHistoryLabels.byDay.asString()) }
                }
                items(uiState.days, key = { day -> "day-${day.date}" }) { day -> IntakeHistoryDayRow(day) }
            }
        }
    }
}

@Composable
private fun NoMedications() {
    Column(
        Modifier.padding(vertical = AppSpacing.large),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.standard),
    ) {
        Icon(AppIcons.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        SectionHeader(IntakeHistoryLabels.emptyTitle.asString())
        Text(IntakeHistoryLabels.emptyMessage.asString(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OverallAdherence(adherence: IntakeAdherence) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.fillMaxWidth().padding(AppSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            Text(IntakeHistoryLabels.adherence.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(adherencePercentLabel(adherence).asString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            adherence.takenPercent?.let { percent ->
                LinearProgressIndicator(
                    progress = { percent / FULL_PERCENT },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                    drawStopIndicator = {},
                )
            }
            Text(adherenceDetailLabel(adherence).asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun MedicationAdherenceRow(medication: MedicationAdherence) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier.fillMaxWidth().padding(AppSpacing.standard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                Text(medication.medicationName, style = MaterialTheme.typography.titleSmall)
                Text(
                    adherenceDetailLabel(medication.adherence).asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(adherencePercentLabel(medication.adherence).asString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun IntakeHistoryDayRow(day: IntakeHistoryDay) {
    val missedAny = day.adherence.missedCount > 0
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.fillMaxWidth().padding(AppSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
        ) {
            Text(formatDayMonth(day.date).asString(), style = MaterialTheme.typography.titleSmall)
            Text(
                intakeHistoryDaySummary(day).map { part -> part.asString() }.joinToString(DAY_SUMMARY_SEPARATOR),
                style = MaterialTheme.typography.bodyMedium,
                color = if (missedAny) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
