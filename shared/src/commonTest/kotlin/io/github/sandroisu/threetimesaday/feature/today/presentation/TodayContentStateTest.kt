package io.github.sandroisu.threetimesaday.feature.today.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class TodayContentStateTest {

    @Test
    fun loadingTakesPrecedenceOverEverything() {
        assertEquals(
            TodayContentState.Loading,
            todayContentState(isLoading = true, hasErrorMessage = true, hasEvents = false)
        )
    }

    @Test
    fun errorShownWhenNotLoading() {
        assertEquals(
            TodayContentState.Error,
            todayContentState(isLoading = false, hasErrorMessage = true, hasEvents = true)
        )
    }

    @Test
    fun emptyShownWhenNoEventsAndNoError() {
        assertEquals(
            TodayContentState.Empty,
            todayContentState(isLoading = false, hasErrorMessage = false, hasEvents = false)
        )
    }

    @Test
    fun eventsShownWhenPresent() {
        assertEquals(
            TodayContentState.Events,
            todayContentState(isLoading = false, hasErrorMessage = false, hasEvents = true)
        )
    }

    @Test
    fun loadingNeverResolvesToEmpty() {
        assertEquals(
            TodayContentState.Loading,
            todayContentState(isLoading = true, hasErrorMessage = false, hasEvents = false)
        )
    }
}
