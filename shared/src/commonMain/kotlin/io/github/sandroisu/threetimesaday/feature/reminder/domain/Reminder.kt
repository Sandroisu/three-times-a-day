package io.github.sandroisu.threetimesaday.feature.reminder.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val id: String,
    val title: String,
    val date: LocalDate,
    val time: LocalTime,
    val recurrence: ReminderRecurrence,
    val alertMode: ReminderAlertMode = ReminderAlertMode.Notification,
    // Reminders saved before pause support have no such field and must stay on.
    val isEnabled: Boolean = true,
)
