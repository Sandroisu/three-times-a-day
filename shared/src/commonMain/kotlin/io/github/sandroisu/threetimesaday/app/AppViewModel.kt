package io.github.sandroisu.threetimesaday.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sandroisu.threetimesaday.core.logging.ErrorLogger
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreen
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreenRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AppViewModel(
    private val startScreenRepository: StartScreenRepository,
    private val errorLogger: ErrorLogger,
) : ViewModel() {

    private val mutableStartScreen = MutableStateFlow<StartScreen?>(null)

    // Null until the stored value is read: the nav graph cannot change its start later.
    val startScreen: StateFlow<StartScreen?> = mutableStartScreen.asStateFlow()

    init {
        viewModelScope.launch {
            val startScreen = try {
                startScreenRepository.getStartScreen()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (loadFailure: Exception) {
                errorLogger.logError(LOG_TAG, "Could not read start screen, opening reminders", loadFailure)
                StartScreen.Reminders
            }
            mutableStartScreen.update { startScreen }
        }
    }

    private companion object {
        const val LOG_TAG = "AppViewModel"
    }
}
