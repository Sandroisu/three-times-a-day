package io.github.sandroisu.threetimesaday.feature.settings.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreen
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.settings_about
import threetimesaday.shared.generated.resources.settings_load_error
import threetimesaday.shared.generated.resources.settings_save_error
import threetimesaday.shared.generated.resources.settings_start_screen
import threetimesaday.shared.generated.resources.settings_start_screen_hint
import threetimesaday.shared.generated.resources.settings_start_screen_medications
import threetimesaday.shared.generated.resources.settings_start_screen_reminders
import threetimesaday.shared.generated.resources.settings_title

internal object SettingsLabels {
    val title = UiText(Res.string.settings_title)
    val about = UiText(Res.string.settings_about)
    val startScreen = UiText(Res.string.settings_start_screen)
    val startScreenHint = UiText(Res.string.settings_start_screen_hint)
    val loadError = UiText(Res.string.settings_load_error)
    val saveError = UiText(Res.string.settings_save_error)
}

internal fun startScreenLabel(startScreen: StartScreen): UiText = when (startScreen) {
    StartScreen.Reminders -> UiText(Res.string.settings_start_screen_reminders)
    StartScreen.Medications -> UiText(Res.string.settings_start_screen_medications)
}
