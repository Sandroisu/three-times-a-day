package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.date_day_month
import threetimesaday.shared.generated.resources.date_time
import threetimesaday.shared.generated.resources.intake_postponed
import threetimesaday.shared.generated.resources.intake_taken
import threetimesaday.shared.generated.resources.intake_taken_at
import threetimesaday.shared.generated.resources.month_9
import threetimesaday.shared.generated.resources.time_value

class TodayIntakePresentationTest {
    private val now = LocalDateTime(2026, 9, 20, 12, 0)

    @Test
    fun postponedAcrossMidnightShowsTheDateWithoutChangingItsTime() {
        val postponed = event("tomorrow", 8, MedicationIntakeStatus.Postponed).copy(
            scheduledDateTime = LocalDateTime(2026, 9, 21, 0, 5),
        )
        val group = todayIntakeGroups(listOf(postponed), emptyList(), now).single()
        assertEquals(UiText(Res.string.date_time, listOf(UiText(Res.string.date_day_month, listOf(21, UiText(Res.string.month_9))), "00:05")), group.timeLabel)
        assertEquals(postponed.scheduledDateTime, group.intakes.single().scheduledDateTime)
    }

    @Test
    fun groupsByScheduledTimeInChronologicalOrder() {
        val groups = todayIntakeGroups(
            listOf(event("late", 18), event("early", 8), event("same-time", 8)),
            emptyList(),
            now,
        )
        assertEquals(listOf("08:00", "18:00").map { UiText(Res.string.time_value, listOf(it)) }, groups.map { it.timeLabel })
        assertEquals(listOf("early", "same-time"), groups.first().intakes.map { it.eventId })
    }

    @Test
    fun actionableStatesRespectExistingStrictOverdueBoundary() {
        val groups = todayIntakeGroups(listOf(event("past", 8), event("due", 12), event("future", 18)), emptyList(), now)
        val intakes = groups.flatMap { it.intakes }
        assertEquals(listOf(IntakeDisplayStatus.Overdue, IntakeDisplayStatus.Due, IntakeDisplayStatus.Upcoming), intakes.map { it.status })
        assertTrue(intakes[0].isActionable)
        assertTrue(intakes[1].isActionable)
        assertFalse(intakes[2].isActionable)
    }

    @Test
    fun completionCountsOnlyTakenAndUsesRecordedTime() {
        val taken = event("taken", 8, MedicationIntakeStatus.Taken)
        val skipped = event("skipped", 9, MedicationIntakeStatus.Skipped)
        val upcoming = event("upcoming", 18)
        val record = MedicationIntakeRecord(
            eventId = taken.eventId,
            medicationId = taken.medicationId,
            scheduledDateTime = taken.scheduledDateTime,
            status = MedicationIntakeStatus.Taken,
            updatedDateTime = LocalDateTime(2026, 9, 20, 8, 12),
            postponedDateTime = null,
        )
        val events = listOf(taken, skipped, upcoming)
        val groups = todayIntakeGroups(events, listOf(record), now)
        assertEquals(UiText(Res.string.intake_taken_at, listOf("08:12")), groups.first().intakes.single().statusLabel)
        assertFalse(groups.first().intakes.single().isActionable)
        assertEquals(IntakeDisplayStatus.Skipped, groups[1].intakes.single().status)
        assertEquals(1f / 3f, intakeCompletionFraction(events))
        assertEquals(0f, intakeCompletionFraction(emptyList()))
    }

    @Test
    fun postponedIntakeUsesEffectiveTimeAndRetainsPostponedContext() {
        val group = todayIntakeGroups(listOf(event("postponed", 18, MedicationIntakeStatus.Postponed)), emptyList(), now).single()
        assertEquals(UiText(Res.string.time_value, listOf("18:00")), group.timeLabel)
        assertEquals(IntakeDisplayStatus.Upcoming, group.intakes.single().status)
        assertEquals(UiText(Res.string.intake_postponed), group.intakes.single().contextLabel)
        assertFalse(group.intakes.single().isActionable)
    }

    @Test
    fun missingClockDoesNotOfferEarlyActionsAndMissingRecordDoesNotInventTakenTime() {
        val upcoming = todayIntakeGroups(listOf(event("unknown", 8)), emptyList(), null).single().intakes.single()
        assertFalse(upcoming.isActionable)
        val taken = todayIntakeGroups(listOf(event("taken", 8, MedicationIntakeStatus.Taken)), emptyList(), now).single().intakes.single()
        assertEquals(UiText(Res.string.intake_taken), taken.statusLabel)
    }

    private fun event(id: String, hour: Int, status: MedicationIntakeStatus = MedicationIntakeStatus.Scheduled): MedicationIntakeEvent =
        MedicationIntakeEvent(
            eventId = id,
            medicationId = "medication-$id",
            medicationName = "Препарат",
            dosageText = "1 таблетка",
            scheduledDateTime = LocalDateTime(2026, 9, 20, hour, 0),
            status = status,
        )
}
