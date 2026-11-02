package io.github.sandroisu.threetimesaday.feature.settings.data

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreen
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistentStartScreenRepositoryTest {

    @Test
    fun opensRemindersWhenNothingStored() = runTest {
        val repository = PersistentStartScreenRepository(InMemoryKeyValueStorage())

        assertEquals(StartScreen.Reminders, repository.getStartScreen())
    }

    @Test
    fun savedStartScreenSurvivesRepositoryRecreation() = runTest {
        val storage = InMemoryKeyValueStorage()
        PersistentStartScreenRepository(storage).saveStartScreen(StartScreen.Medications)

        val restoredStartScreen = PersistentStartScreenRepository(storage).getStartScreen()

        assertEquals(StartScreen.Medications, restoredStartScreen)
    }

    @Test
    fun unknownStoredValueFallsBackToReminders() = runTest {
        val storage = InMemoryKeyValueStorage()
        storage.putString("start_screen", "Calendar")

        assertEquals(StartScreen.Reminders, PersistentStartScreenRepository(storage).getStartScreen())
    }
}
