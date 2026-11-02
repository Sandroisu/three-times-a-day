package io.github.sandroisu.threetimesaday.feature.settings.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreen

internal data class SettingsUiState(
    val startScreen: StartScreen? = null,
    val errorMessage: UiText? = null,
)
