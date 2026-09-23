package io.github.sandroisu.threetimesaday.feature.reminder.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface ReminderRecurrence {

    @Serializable
    data object Once : ReminderRecurrence

    @Serializable
    data class EveryMonthsOnDay(
        val intervalMonths: Int,
        val dayOfMonth: Int,
    ) : ReminderRecurrence
}
