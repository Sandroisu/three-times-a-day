package io.github.sandroisu.threetimesaday.core.notification

import kotlinx.serialization.Serializable

@Serializable
data class MedicationReminderLaunchData(
    val notificationId: String,
    val eventId: String
)
