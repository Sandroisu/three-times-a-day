package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeAdherence
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeHistoryDay
import kotlinx.datetime.LocalDate
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.intake_history_day_empty
import threetimesaday.shared.generated.resources.intake_history_day_missed
import threetimesaday.shared.generated.resources.intake_history_day_pending
import threetimesaday.shared.generated.resources.intake_history_day_taken
import threetimesaday.shared.generated.resources.intake_history_no_data
import threetimesaday.shared.generated.resources.intake_history_no_data_hint
import threetimesaday.shared.generated.resources.intake_history_percent
import threetimesaday.shared.generated.resources.intake_progress
import kotlin.test.Test
import kotlin.test.assertEquals

class IntakeHistoryLabelsTest {

    @Test
    fun percentLabelSaysNoDataWhenNothingWasDue() {
        assertEquals(UiText(Res.string.intake_history_no_data), adherencePercentLabel(IntakeAdherence(0, 0, 0)))
    }

    @Test
    fun percentLabelShowsTakenPercent() {
        assertEquals(UiText(Res.string.intake_history_percent, listOf(75)), adherencePercentLabel(IntakeAdherence(3, 0, 1)))
    }

    @Test
    fun detailLabelShowsTakenOfDue() {
        assertEquals(UiText(Res.string.intake_progress, listOf(3, 5)), adherenceDetailLabel(IntakeAdherence(3, 1, 1)))
    }

    @Test
    fun detailLabelExplainsMissingData() {
        assertEquals(UiText(Res.string.intake_history_no_data_hint), adherenceDetailLabel(IntakeAdherence(0, 0, 0)))
    }

    @Test
    fun daySummaryListsOnlyNonZeroCounts() {
        val day = IntakeHistoryDay(LocalDate(2026, 7, 10), IntakeAdherence(2, 0, 1), pendingCount = 3)

        assertEquals(
            listOf(
                UiText(Res.string.intake_history_day_taken, listOf(2)),
                UiText(Res.string.intake_history_day_missed, listOf(1)),
                UiText(Res.string.intake_history_day_pending, listOf(3)),
            ),
            intakeHistoryDaySummary(day),
        )
    }

    @Test
    fun daySummarySaysNoIntakesForEmptyDay() {
        val day = IntakeHistoryDay(LocalDate(2026, 7, 10), IntakeAdherence(0, 0, 0), pendingCount = 0)

        assertEquals(listOf(UiText(Res.string.intake_history_day_empty)), intakeHistoryDaySummary(day))
    }
}
