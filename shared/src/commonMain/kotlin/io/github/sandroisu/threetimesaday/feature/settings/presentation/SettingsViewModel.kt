package io.github.sandroisu.threetimesaday.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreen
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreenRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val startScreenRepository: StartScreenRepository,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        loadSettings()
    }

    fun onStartScreenSelected(startScreen: StartScreen) {
        val previousStartScreen = mutableUiState.value.startScreen
        mutableUiState.update { state -> state.copy(startScreen = startScreen, errorMessage = null) }
        viewModelScope.launch {
            try {
                startScreenRepository.saveStartScreen(startScreen)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (saveFailure: Exception) {
                mutableUiState.update { state ->
                    state.copy(startScreen = previousStartScreen, errorMessage = SettingsLabels.saveError)
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val startScreen = startScreenRepository.getStartScreen()
                mutableUiState.update { state -> state.copy(startScreen = startScreen) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (loadFailure: Exception) {
                mutableUiState.update { state -> state.copy(errorMessage = SettingsLabels.loadError) }
            }
        }
    }
}
