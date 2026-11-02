package io.github.sandroisu.threetimesaday.feature.settings.domain

internal interface StartScreenRepository {

    suspend fun getStartScreen(): StartScreen

    suspend fun saveStartScreen(startScreen: StartScreen)
}
