package io.github.sandroisu.threetimesaday.feature.reminder.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface ReminderRecurrence {

    @Serializable
    data object Once : ReminderRecurrence

    @Serializable
    data object Daily : ReminderRecurrence

    @Serializable
    data class OnWeekdays(
        val weekdays: List<ReminderWeekday>,
    ) : ReminderRecurrence

    @Serializable
    data class EveryDays(
        val intervalDays: Int,
    ) : ReminderRecurrence

    @Serializable
    data class EveryMonthsOnDay(
        val intervalMonths: Int,
        val dayOfMonth: Int,
    ) : ReminderRecurrence

    @Serializable
    data class CyclicDayIntervals(
        val intervals: List<Int>,
    ) : ReminderRecurrence
}
