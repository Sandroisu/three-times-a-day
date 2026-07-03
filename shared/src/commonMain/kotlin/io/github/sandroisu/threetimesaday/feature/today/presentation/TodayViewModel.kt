package io.github.sandroisu.threetimesaday.feature.today.presentation

import androidx.lifecycle.ViewModel
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TodayViewModel(
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = mutableUiState.asStateFlow()

    fun currentDateLabel(): String = timeProvider.currentDate().toString()
}
