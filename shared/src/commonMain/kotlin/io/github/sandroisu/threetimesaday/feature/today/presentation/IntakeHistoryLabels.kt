package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import org.jetbrains.compose.resources.StringResource
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeAdherence
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeHistoryDay
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeHistoryPeriod
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.intake_history_adherence
import threetimesaday.shared.generated.resources.intake_history_by_day
import threetimesaday.shared.generated.resources.intake_history_by_medication
import threetimesaday.shared.generated.resources.intake_history_day_empty
import threetimesaday.shared.generated.resources.intake_history_day_missed
import threetimesaday.shared.generated.resources.intake_history_day_pending
import threetimesaday.shared.generated.resources.intake_history_day_skipped
import threetimesaday.shared.generated.resources.intake_history_day_taken
import threetimesaday.shared.generated.resources.intake_history_empty_message
import threetimesaday.shared.generated.resources.intake_history_empty_title
import threetimesaday.shared.generated.resources.intake_history_load_error
import threetimesaday.shared.generated.resources.intake_history_no_data
import threetimesaday.shared.generated.resources.intake_history_no_data_hint
import threetimesaday.shared.generated.resources.intake_history_open
import threetimesaday.shared.generated.resources.intake_history_percent
import threetimesaday.shared.generated.resources.intake_history_period_30
import threetimesaday.shared.generated.resources.intake_history_period_7
import threetimesaday.shared.generated.resources.intake_history_title

internal object IntakeHistoryLabels {
    val title = UiText(Res.string.intake_history_title)
    val open = UiText(Res.string.intake_history_open)
    val adherence = UiText(Res.string.intake_history_adherence)
    val byMedication = UiText(Res.string.intake_history_by_medication)
    val byDay = UiText(Res.string.intake_history_by_day)
    val emptyTitle = UiText(Res.string.intake_history_empty_title)
    val emptyMessage = UiText(Res.string.intake_history_empty_message)
    val loadError = UiText(Res.string.intake_history_load_error)
}

internal fun intakeHistoryPeriodLabel(period: IntakeHistoryPeriod): UiText = when (period) {
    IntakeHistoryPeriod.LastSevenDays -> UiText(Res.string.intake_history_period_7)
    IntakeHistoryPeriod.LastThirtyDays -> UiText(Res.string.intake_history_period_30)
}

internal fun adherencePercentLabel(adherence: IntakeAdherence): UiText =
    adherence.takenPercent?.let { percent -> UiText(Res.string.intake_history_percent, listOf(percent)) }
        ?: UiText(Res.string.intake_history_no_data)

internal fun adherenceDetailLabel(adherence: IntakeAdherence): UiText =
    if (adherence.dueCount == 0) {
        UiText(Res.string.intake_history_no_data_hint)
    } else {
        intakeProgressLabel(adherence.takenCount, adherence.dueCount)
    }

internal fun intakeHistoryDaySummary(day: IntakeHistoryDay): List<UiText> {
    val parts = listOfNotNull(
        countPart(Res.string.intake_history_day_taken, day.adherence.takenCount),
        countPart(Res.string.intake_history_day_skipped, day.adherence.skippedCount),
        countPart(Res.string.intake_history_day_missed, day.adherence.missedCount),
        countPart(Res.string.intake_history_day_pending, day.pendingCount),
    )
    return parts.ifEmpty { listOf(UiText(Res.string.intake_history_day_empty)) }
}

private fun countPart(
    resource: StringResource,
    count: Int,
): UiText? = if (count > 0) UiText(resource, listOf(count)) else null
