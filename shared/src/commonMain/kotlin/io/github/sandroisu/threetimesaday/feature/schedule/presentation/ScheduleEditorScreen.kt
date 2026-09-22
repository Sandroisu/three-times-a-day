package io.github.sandroisu.threetimesaday.feature.schedule.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.LoadingIndicator
import io.github.sandroisu.threetimesaday.core.ui.Notice
import io.github.sandroisu.threetimesaday.core.ui.PrimaryActionButton
import io.github.sandroisu.threetimesaday.core.ui.ScreenHeader
import io.github.sandroisu.threetimesaday.core.ui.TimeInputField
import io.github.sandroisu.threetimesaday.core.ui.UiLabels
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ScheduleEditorScreen(
    onBackClick: () -> Unit,
    onScheduleSaved: () -> Unit,
    scheduleEditorViewModel: ScheduleEditorViewModel = koinViewModel(),
) {
    val uiState by scheduleEditorViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(scheduleEditorViewModel) {
        scheduleEditorViewModel.scheduleSavedEvents.collect { onScheduleSaved() }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(AppSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.standard),
        ) {
            ScreenHeader(ScheduleLabels.title.asString(), onBackClick, ScheduleLabels.subtitle.asString())
            if (uiState.isLoading) {
                LoadingIndicator()
            } else {
                TimeInputField(ScheduleLabels.wakeUp.asString(), uiState.wakeUpTimeText, uiState.wakeUpTimeError?.asString(), scheduleEditorViewModel::onWakeUpTimeChanged)
                TimeInputField(ScheduleLabels.breakfast.asString(), uiState.breakfastTimeText, uiState.breakfastTimeError?.asString(), scheduleEditorViewModel::onBreakfastTimeChanged)
                TimeInputField(ScheduleLabels.lunch.asString(), uiState.lunchTimeText, uiState.lunchTimeError?.asString(), scheduleEditorViewModel::onLunchTimeChanged)
                TimeInputField(ScheduleLabels.dinner.asString(), uiState.dinnerTimeText, uiState.dinnerTimeError?.asString(), scheduleEditorViewModel::onDinnerTimeChanged)
                TimeInputField(ScheduleLabels.sleep.asString(), uiState.sleepTimeText, uiState.sleepTimeError?.asString(), scheduleEditorViewModel::onSleepTimeChanged)
                uiState.generalErrorMessage?.let { Notice(it.asString(), isError = true) }
            }
        }
        if (!uiState.isLoading) {
            Column(Modifier.padding(AppSpacing.standard)) {
                PrimaryActionButton(UiLabels.save.asString(), scheduleEditorViewModel::save, enabled = uiState.isSaveEnabled)
            }
        }
    }
}
