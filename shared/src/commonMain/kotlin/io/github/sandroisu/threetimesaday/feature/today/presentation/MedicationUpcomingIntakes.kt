package io.github.sandroisu.threetimesaday.feature.today.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sandroisu.threetimesaday.core.ui.AppIcons
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.IconLabel
import io.github.sandroisu.threetimesaday.core.ui.LoadingIndicator
import io.github.sandroisu.threetimesaday.core.ui.Notice
import io.github.sandroisu.threetimesaday.core.ui.SectionHeader

@Composable
internal fun MedicationUpcomingIntakes(
    medicationId: String,
    todayViewModel: TodayViewModel,
) {
    val uiState by todayViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(medicationId) { todayViewModel.refreshDisplayedTime() }
    RefreshIntakeTime(todayViewModel)
    val upcoming = uiState.upcomingIntakes.filter { it.medicationId == medicationId }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        SectionHeader(TodayLabels.nextIntake.asString())
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.upcomingIntakesError != null || uiState.errorMessage != null -> Notice(TodayLabels.upcomingError.asString(), isError = true) {
                TextButton(onClick = todayViewModel::loadToday) { Text(TodayLabels.retry.asString()) }
            }
            upcoming.isEmpty() -> Text(TodayLabels.noUpcoming.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(AppSpacing.standard), verticalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                        Text(upcomingIntakeDateLabel(upcoming.first().scheduledDateTime).asString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(intakeMomentLabel(upcoming.first().intakeMoment).asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                if (upcoming.size > 1) {
                    Text(TodayLabels.upcoming.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    upcoming.drop(1).forEach { event ->
                        IconLabel(AppIcons.Clock, upcomingIntakeDateLabel(event.scheduledDateTime).asString())
                    }
                }
            }
        }
    }
}
