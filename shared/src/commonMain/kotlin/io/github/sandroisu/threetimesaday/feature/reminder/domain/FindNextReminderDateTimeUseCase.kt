package io.github.sandroisu.threetimesaday.feature.reminder.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

class FindNextReminderDateTimeUseCase {

    operator fun invoke(reminder: Reminder, after: LocalDateTime): LocalDateTime? =
        when (val recurrence = reminder.recurrence) {
            ReminderRecurrence.Once -> LocalDateTime(reminder.date, reminder.time)
                .takeIf { scheduledDateTime -> scheduledDateTime > after }

            ReminderRecurrence.Daily -> nextEveryDaysDateTime(
                reminder = reminder,
                intervalDays = 1,
                after = after,
            )

            is ReminderRecurrence.OnWeekdays -> nextWeekdayDateTime(
                reminder = reminder,
                recurrence = recurrence,
                after = after,
            )

            is ReminderRecurrence.EveryDays -> nextEveryDaysDateTime(
                reminder = reminder,
                intervalDays = recurrence.intervalDays,
                after = after,
            )

            is ReminderRecurrence.EveryMonthsOnDay -> nextMonthlyDateTime(
                reminder = reminder,
                recurrence = recurrence,
                after = after,
            )

            is ReminderRecurrence.CyclicDayIntervals -> nextCyclicDateTime(
                reminder = reminder,
                recurrence = recurrence,
                after = after,
            )
        }

    private fun nextEveryDaysDateTime(
        reminder: Reminder,
        intervalDays: Int,
        after: LocalDateTime,
    ): LocalDateTime? {
        if (intervalDays !in MIN_INTERVAL_DAYS..MAX_INTERVAL_DAYS) {
            return null
        }
        val firstDateTime = LocalDateTime(reminder.date, reminder.time)
        if (firstDateTime > after) {
            return firstDateTime
        }
        val daysSinceStart = reminder.date.daysUntil(after.date).coerceAtLeast(0)
        var occurrenceIndex = daysSinceStart / intervalDays
        while (true) {
            val scheduledDate = reminder.date.plus(occurrenceIndex * intervalDays, DateTimeUnit.DAY)
            val scheduledDateTime = LocalDateTime(scheduledDate, reminder.time)
            if (scheduledDateTime > after) {
                return scheduledDateTime
            }
            occurrenceIndex += 1
        }
    }

    private fun nextWeekdayDateTime(
        reminder: Reminder,
        recurrence: ReminderRecurrence.OnWeekdays,
        after: LocalDateTime,
    ): LocalDateTime? {
        val selectedDayNumbers = recurrence.weekdays.map { weekday -> weekday.isoDayNumber }.toSet()
        if (selectedDayNumbers.isEmpty()) {
            return null
        }
        val firstCandidateDate = if (after.date < reminder.date) reminder.date else after.date
        for (dayOffset in 0 until DAYS_IN_TWO_WEEKS) {
            val candidateDate = firstCandidateDate.plus(dayOffset, DateTimeUnit.DAY)
            val candidateDateTime = LocalDateTime(candidateDate, reminder.time)
            if (candidateDate.dayOfWeek.ordinal + 1 in selectedDayNumbers &&
                candidateDateTime >= LocalDateTime(reminder.date, reminder.time) &&
                candidateDateTime > after
            ) {
                return candidateDateTime
            }
        }
        return null
    }

    private fun nextCyclicDateTime(
        reminder: Reminder,
        recurrence: ReminderRecurrence.CyclicDayIntervals,
        after: LocalDateTime,
    ): LocalDateTime? {
        val intervals = recurrence.intervals
        if (intervals.isEmpty() || intervals.size > MAX_CYCLIC_INTERVAL_COUNT ||
            intervals.any { interval -> interval !in MIN_INTERVAL_DAYS..MAX_INTERVAL_DAYS }
        ) {
            return null
        }
        val firstDateTime = LocalDateTime(reminder.date, reminder.time)
        if (firstDateTime > after) {
            return firstDateTime
        }
        val cycleDayCount = intervals.sum()
        val daysSinceStart = reminder.date.daysUntil(after.date).coerceAtLeast(0)
        var cycleStartDay = daysSinceStart / cycleDayCount * cycleDayCount
        repeat(2) {
            var dayOffsetInCycle = 0
            intervals.forEach { intervalDays ->
                val scheduledDate = reminder.date.plus(cycleStartDay + dayOffsetInCycle, DateTimeUnit.DAY)
                val scheduledDateTime = LocalDateTime(scheduledDate, reminder.time)
                if (scheduledDateTime > after) {
                    return scheduledDateTime
                }
                dayOffsetInCycle += intervalDays
            }
            cycleStartDay += cycleDayCount
        }
        return null
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
        const val DAYS_IN_TWO_WEEKS = 14
        const val MIN_INTERVAL_DAYS = 1
        const val MAX_INTERVAL_DAYS = 3_650
        const val MAX_CYCLIC_INTERVAL_COUNT = 32
        const val MIN_INTERVAL_MONTHS = 1
        const val MAX_INTERVAL_MONTHS = 120
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
