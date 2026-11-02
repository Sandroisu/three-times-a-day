package io.github.sandroisu.threetimesaday.feature.settings.data

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreen
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreenRepository

internal class PersistentStartScreenRepository(
    private val keyValueStorage: KeyValueStorage,
) : StartScreenRepository {

    override suspend fun getStartScreen(): StartScreen {
        val storedName = keyValueStorage.getString(START_SCREEN_KEY) ?: return DEFAULT_START_SCREEN
        return StartScreen.entries.firstOrNull { startScreen -> startScreen.name == storedName }
            ?: DEFAULT_START_SCREEN
    }

    override suspend fun saveStartScreen(startScreen: StartScreen) {
        keyValueStorage.putString(START_SCREEN_KEY, startScreen.name)
    }

    private companion object {
        const val START_SCREEN_KEY = "start_screen"
        val DEFAULT_START_SCREEN = StartScreen.Reminders
    }
}
