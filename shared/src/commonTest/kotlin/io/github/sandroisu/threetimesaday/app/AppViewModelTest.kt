package io.github.sandroisu.threetimesaday.app

import io.github.sandroisu.threetimesaday.core.logging.ErrorLogger
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

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
    fun startScreenIsUnknownUntilStoredValueIsRead() = runTest(testDispatcher) {
        val viewModel = AppViewModel(FakeStartScreenRepository(StartScreen.Medications), RecordingErrorLogger())

        assertNull(viewModel.startScreen.value)
        advanceUntilIdle()

        assertEquals(StartScreen.Medications, viewModel.startScreen.value)
    }

    @Test
    fun failedReadOpensRemindersAndLogsError() = runTest(testDispatcher) {
        val errorLogger = RecordingErrorLogger()
        val viewModel = AppViewModel(FakeStartScreenRepository(failure = IllegalStateException("broken")), errorLogger)

        advanceUntilIdle()

        assertEquals(StartScreen.Reminders, viewModel.startScreen.value)
        assertTrue(errorLogger.loggedErrors.single() is IllegalStateException)
    }

    private class FakeStartScreenRepository(
        private val storedStartScreen: StartScreen = StartScreen.Reminders,
        private val failure: Exception? = null,
    ) : StartScreenRepository {

        override suspend fun getStartScreen(): StartScreen {
            if (failure != null) throw failure
            return storedStartScreen
        }

        override suspend fun saveStartScreen(startScreen: StartScreen) = Unit
    }

    private class RecordingErrorLogger : ErrorLogger {
        val loggedErrors: MutableList<Throwable> = mutableListOf()

        override fun logError(tag: String, message: String, error: Throwable) {
            loggedErrors.add(error)
        }
    }
}
