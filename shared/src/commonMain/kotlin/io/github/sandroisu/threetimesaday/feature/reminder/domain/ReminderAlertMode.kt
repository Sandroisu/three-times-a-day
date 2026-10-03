package io.github.sandroisu.threetimesaday.feature.reminder.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface ReminderAlertMode {

    @Serializable
    data object Notification : ReminderAlertMode

    @Serializable
    data object Alarm : ReminderAlertMode
}
