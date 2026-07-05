package io.github.sandroisu.threetimesaday.core.notification

import kotlinx.datetime.LocalDateTime

const val MEDICATION_REMINDER_ID_PREFIX = "medication-reminder|"

data class MedicationReminderNotification(
    val notificationId: String,
    val title: String,
    val message: String,
    val scheduledDateTime: LocalDateTime
)
