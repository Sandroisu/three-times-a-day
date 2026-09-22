package io.github.sandroisu.threetimesaday.feature.today.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.intake_postpone

class TodayLabelsPostponeTest {

    @Test
    fun postponeActionLabelShowsConfiguredInterval() {
        assertEquals(listOf(MEDICATION_POSTPONE_MINUTES), postponeActionLabel().arguments)
    }

    @Test
    fun postponeActionLabelUsesLocalizedResource() {
        assertEquals(Res.string.intake_postpone, postponeActionLabel().resource)
    }
}
