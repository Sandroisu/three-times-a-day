package io.github.sandroisu.threetimesaday.feature.reminder.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month

class FindNextReminderDateTimeUseCase {

    operator fun invoke(reminder: Reminder, after: LocalDateTime): LocalDateTime? =
        when (val recurrence = reminder.recurrence) {
            ReminderRecurrence.Once -> LocalDateTime(reminder.date, reminder.time)
                .takeIf { scheduledDateTime -> scheduledDateTime > after }

            is ReminderRecurrence.EveryMonthsOnDay -> nextMonthlyDateTime(
                reminder = reminder,
                recurrence = recurrence,
                after = after,
            )
        }

    private fun nextMonthlyDateTime(
        reminder: Reminder,
        recurrence: ReminderRecurrence.EveryMonthsOnDay,
        after: LocalDateTime,
    ): LocalDateTime? {
        if (recurrence.intervalMonths !in MIN_INTERVAL_MONTHS..MAX_INTERVAL_MONTHS ||
            recurrence.dayOfMonth !in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH
        ) {
            return null
        }
        val anchorMonthIndex = monthIndex(reminder.date)
        val currentMonthIndex = monthIndex(after.date)
        var occurrenceIndex = ((currentMonthIndex - anchorMonthIndex) / recurrence.intervalMonths)
            .coerceAtLeast(0)
        while (true) {
            val scheduledDate = dateForOccurrence(
                monthIndex = anchorMonthIndex + occurrenceIndex * recurrence.intervalMonths,
                dayOfMonth = recurrence.dayOfMonth,
            )
            val scheduledDateTime = LocalDateTime(scheduledDate, reminder.time)
            if (scheduledDateTime >= LocalDateTime(reminder.date, reminder.time) && scheduledDateTime > after) {
                return scheduledDateTime
            }
            occurrenceIndex += 1
        }
    }

    private fun dateForOccurrence(monthIndex: Int, dayOfMonth: Int): LocalDate {
        val year = monthIndex.floorDiv(MONTHS_IN_YEAR)
        val monthNumber = monthIndex.mod(MONTHS_IN_YEAR) + 1
        val day = dayOfMonth.coerceAtMost(daysInMonth(year, monthNumber))
        return LocalDate(year, Month(monthNumber), day)
    }

    private fun monthIndex(date: LocalDate): Int = date.year * MONTHS_IN_YEAR + date.month.ordinal

    private fun daysInMonth(year: Int, monthNumber: Int): Int = when (monthNumber) {
        2 -> if (isLeapYear(year)) LEAP_FEBRUARY_DAYS else FEBRUARY_DAYS
        4, 6, 9, 11 -> THIRTY_DAY_MONTH_DAYS
        else -> THIRTY_ONE_DAY_MONTH_DAYS
    }

    private fun isLeapYear(year: Int): Boolean =
        year % LEAP_YEAR_DIVISOR == 0 && (year % CENTURY_DIVISOR != 0 || year % QUADRICENTENNIAL_DIVISOR == 0)

    private companion object {
        const val MONTHS_IN_YEAR = 12
        const val MIN_INTERVAL_MONTHS = 1
        const val MAX_INTERVAL_MONTHS = 24
        const val MIN_DAY_OF_MONTH = 1
        const val MAX_DAY_OF_MONTH = 31
        const val FEBRUARY_DAYS = 28
        const val LEAP_FEBRUARY_DAYS = 29
        const val THIRTY_DAY_MONTH_DAYS = 30
        const val THIRTY_ONE_DAY_MONTH_DAYS = 31
        const val LEAP_YEAR_DIVISOR = 4
        const val CENTURY_DIVISOR = 100
        const val QUADRICENTENNIAL_DIVISOR = 400
    }
}
