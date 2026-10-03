package io.github.sandroisu.threetimesaday.feature.reminder.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderWeekday(val isoDayNumber: Int) {
    Monday(1),
    Tuesday(2),
    Wednesday(3),
    Thursday(4),
    Friday(5),
    Saturday(6),
    Sunday(7),
}
