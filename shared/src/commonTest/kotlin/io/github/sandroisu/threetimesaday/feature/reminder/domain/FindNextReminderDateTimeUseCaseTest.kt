package io.github.sandroisu.threetimesaday.feature.reminder.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindNextReminderDateTimeUseCaseTest {

    private val useCase = FindNextReminderDateTimeUseCase()

    @Test
    fun findsNextQuarterlyOccurrenceOnSelectedDayOfMonth() {
        val reminder = monthlyReminder(date = LocalDate(2026, 1, 14), intervalMonths = 3, dayOfMonth = 14)

        val nextDateTime = useCase(reminder, LocalDateTime(LocalDate(2026, 1, 15), LocalTime(12, 0)))

        assertEquals(LocalDateTime(LocalDate(2026, 4, 14), LocalTime(9, 0)), nextDateTime)
    }

    @Test
    fun waitsForFirstSelectedDayAfterStartDate() {
        val reminder = monthlyReminder(date = LocalDate(2026, 1, 20), intervalMonths = 3, dayOfMonth = 14)

        val nextDateTime = useCase(reminder, LocalDateTime(LocalDate(2026, 1, 20), LocalTime(8, 0)))

        assertEquals(LocalDateTime(LocalDate(2026, 4, 14), LocalTime(9, 0)), nextDateTime)
    }

    @Test
    fun usesLastDayForMissingDayInFebruary() {
        val reminder = monthlyReminder(date = LocalDate(2026, 1, 31), intervalMonths = 1, dayOfMonth = 31)

        val nextDateTime = useCase(reminder, LocalDateTime(LocalDate(2026, 2, 1), LocalTime(0, 0)))

        assertEquals(LocalDateTime(LocalDate(2026, 2, 28), LocalTime(9, 0)), nextDateTime)
    }

    @Test
    fun doesNotReturnPastOneTimeReminder() {
        val reminder = Reminder(
            id = "dentist",
            title = "Dentist",
            date = LocalDate(2026, 1, 14),
            time = LocalTime(9, 0),
            recurrence = ReminderRecurrence.Once,
        )

        val nextDateTime = useCase(reminder, LocalDateTime(LocalDate(2026, 1, 14), LocalTime(9, 0)))

        assertNull(nextDateTime)
    }

    @Test
    fun findsNextDailyOccurrence() {
        val reminder = reminder(ReminderRecurrence.Daily)

        val nextDateTime = useCase(reminder, LocalDateTime(LocalDate(2026, 1, 2), LocalTime(9, 0)))

        assertEquals(LocalDateTime(LocalDate(2026, 1, 3), LocalTime(9, 0)), nextDateTime)
    }

    @Test
    fun findsNextSelectedWeekday() {
        val reminder = reminder(ReminderRecurrence.OnWeekdays(listOf(ReminderWeekday.Thursday)))

        val nextDateTime = useCase(reminder, LocalDateTime(LocalDate(2026, 1, 1), LocalTime(9, 0)))

        assertEquals(LocalDateTime(LocalDate(2026, 1, 8), LocalTime(9, 0)), nextDateTime)
    }

    @Test
    fun findsNextOccurrenceEveryTwoDays() {
        val reminder = reminder(ReminderRecurrence.EveryDays(intervalDays = 2))

        val nextDateTime = useCase(reminder, LocalDateTime(LocalDate(2026, 1, 2), LocalTime(12, 0)))

        assertEquals(LocalDateTime(LocalDate(2026, 1, 3), LocalTime(9, 0)), nextDateTime)
    }

    @Test
    fun repeatsCustomDayIntervalCycle() {
        val reminder = reminder(ReminderRecurrence.CyclicDayIntervals(listOf(2, 4, 8, 2, 6)))

        val nextDateTime = useCase(reminder, LocalDateTime(LocalDate(2026, 1, 7), LocalTime(9, 0)))

        assertEquals(LocalDateTime(LocalDate(2026, 1, 15), LocalTime(9, 0)), nextDateTime)
    }

    private fun monthlyReminder(date: LocalDate, intervalMonths: Int, dayOfMonth: Int): Reminder = Reminder(
        id = "quarterly",
        title = "Quarterly task",
        date = date,
        time = LocalTime(9, 0),
        recurrence = ReminderRecurrence.EveryMonthsOnDay(intervalMonths, dayOfMonth),
    )

    private fun reminder(recurrence: ReminderRecurrence): Reminder = Reminder(
        id = "custom",
        title = "Custom reminder",
        date = LocalDate(2026, 1, 1),
        time = LocalTime(9, 0),
        recurrence = recurrence,
    )
}
