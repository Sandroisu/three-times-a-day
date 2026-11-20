package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.feature.today.domain.MEDICATION_POSTPONE_MINUTES
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationReminderAction
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.intake_postpone
import threetimesaday.shared.generated.resources.intake_taken
import kotlin.test.Test
import kotlin.test.assertEquals

class MedicationReminderActionLabelTest {

    @Test
    fun takenActionUsesTakenStatusText() {
        val label = medicationReminderActionLabel(MedicationReminderAction.Taken)

        assertEquals(Res.string.intake_taken, label.resource)
        assertEquals(emptyList(), label.arguments)
    }

    @Test
    fun postponeActionShowsPostponeInterval() {
        val label = medicationReminderActionLabel(MedicationReminderAction.Postpone)

        assertEquals(Res.string.intake_postpone, label.resource)
        assertEquals(listOf<Any>(MEDICATION_POSTPONE_MINUTES), label.arguments)
    }
}
