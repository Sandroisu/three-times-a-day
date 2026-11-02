package io.github.sandroisu.threetimesaday.feature.settings.presentation

import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreen
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsStoredStartScreen() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(FakeStartScreenRepository(StartScreen.Medications))

        advanceUntilIdle()

        assertEquals(StartScreen.Medications, viewModel.uiState.value.startScreen)
    }

    @Test
    fun selectedStartScreenIsSaved() = runTest(testDispatcher) {
        val repository = FakeStartScreenRepository(StartScreen.Reminders)
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onStartScreenSelected(StartScreen.Medications)
        advanceUntilIdle()

        assertEquals(StartScreen.Medications, repository.storedStartScreen)
        assertEquals(StartScreen.Medications, viewModel.uiState.value.startScreen)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun failedSaveRestoresPreviousChoiceAndShowsError() = runTest(testDispatcher) {
        val repository = FakeStartScreenRepository(StartScreen.Reminders, failOnSave = true)
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onStartScreenSelected(StartScreen.Medications)
        advanceUntilIdle()

        assertEquals(StartScreen.Reminders, viewModel.uiState.value.startScreen)
        assertEquals(SettingsLabels.saveError, viewModel.uiState.value.errorMessage)
    }

    private class FakeStartScreenRepository(
        var storedStartScreen: StartScreen,
        private val failOnSave: Boolean = false,
    ) : StartScreenRepository {

        override suspend fun getStartScreen(): StartScreen = storedStartScreen

        override suspend fun saveStartScreen(startScreen: StartScreen) {
            if (failOnSave) throw IllegalStateException("Storage is not available")
            storedStartScreen = startScreen
        }
    }
}
