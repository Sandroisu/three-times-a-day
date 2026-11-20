package io.github.sandroisu.threetimesaday.feature.today.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sandroisu.threetimesaday.feature.today.domain.BuildIntakeHistoryUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeHistoryPeriod
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeHistoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class IntakeHistoryViewModel(
    private val buildIntakeHistory: BuildIntakeHistoryUseCase,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(IntakeHistoryUiState())
    val uiState: StateFlow<IntakeHistoryUiState> = mutableUiState.asStateFlow()

    // A slow load for the old period must not overwrite the period the user picked later.
    private var loadJob: Job? = null

    init {
        loadHistory()
    }

    fun selectPeriod(period: IntakeHistoryPeriod) {
        mutableUiState.update { currentState -> currentState.copy(selectedPeriod = period) }
        loadHistory()
    }

    fun loadHistory() {
        val period = mutableUiState.value.selectedPeriod
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableUiState.update { currentState -> currentState.copy(isLoading = true, errorMessage = null) }
            try {
                val result = buildIntakeHistory(period)
                mutableUiState.update { currentState ->
                    when (result) {
                        IntakeHistoryResult.NoMedications -> currentState.copy(
                            isLoading = false,
                            hasMedications = false,
                            overallAdherence = null,
                            medicationAdherence = emptyList(),
                            days = emptyList(),
                        )
                        is IntakeHistoryResult.Loaded -> currentState.copy(
                            isLoading = false,
                            hasMedications = true,
                            overallAdherence = result.overall,
                            medicationAdherence = result.medications,
                            days = result.days.reversed(),
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (loadFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        overallAdherence = null,
                        medicationAdherence = emptyList(),
                        days = emptyList(),
                        errorMessage = IntakeHistoryLabels.loadError,
                    )
                }
            }
        }
    }
}
