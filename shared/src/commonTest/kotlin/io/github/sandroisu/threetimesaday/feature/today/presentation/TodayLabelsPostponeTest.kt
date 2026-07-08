package io.github.sandroisu.threetimesaday.feature.today.presentation

import kotlin.test.Test
import kotlin.test.assertTrue

class TodayLabelsPostponeTest {

    @Test
    fun postponeActionLabelShowsConfiguredInterval() {
        assertTrue(postponeActionLabel().contains(MEDICATION_POSTPONE_MINUTES.toString()))
    }

    @Test
    fun postponeActionLabelMentionsPostponing() {
        assertTrue(postponeActionLabel().startsWith("Отложить"))
    }
}
