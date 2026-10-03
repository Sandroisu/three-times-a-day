package io.github.sandroisu.threetimesaday.feature.reminder.domain

import io.github.sandroisu.threetimesaday.core.notification.REMINDER_NOTIFICATION_ID_PREFIX
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.notification.NotificationDeliveryMode
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import kotlinx.datetime.LocalDateTime

class RescheduleRemindersUseCase(
    private val reminderRepository: ReminderRepository,
    private val findNextReminderDateTime: FindNextReminderDateTimeUseCase,
    private val medicationReminderScheduler: MedicationReminderScheduler,
    private val timeProvider: TimeProvider,
    private val buildReminderMessage: suspend (Reminder) -> String,
) {

    suspend operator fun invoke(replaceExistingReminders: Boolean = true) {
        if (replaceExistingReminders) {
            medicationReminderScheduler.cancelRemindersWithPrefix(REMINDER_NOTIFICATION_ID_PREFIX)
        }
        val currentDateTime = timeProvider.currentDateTime()
        val occurrences = reminderRepository.getReminders()
            .flatMap { reminder -> upcomingOccurrences(reminder, currentDateTime) }
            .sortedBy { occurrence -> occurrence.scheduledDateTime }
            .take(MAX_SCHEDULED_OCCURRENCES)
        occurrences.forEach { occurrence ->
            val reminder = occurrence.reminder
            medicationReminderScheduler.scheduleReminder(
                MedicationReminderNotification(
                    notificationId = buildNotificationId(reminder.id, occurrence.scheduledDateTime),
                    title = reminder.title,
                    message = buildReminderMessage(reminder),
                    scheduledDateTime = occurrence.scheduledDateTime,
                    deliveryMode = when (reminder.alertMode) {
                        ReminderAlertMode.Notification -> NotificationDeliveryMode.Standard
                        ReminderAlertMode.Alarm -> NotificationDeliveryMode.Alarm(ALARM_MAX_DURATION_MINUTES)
                    },
                )
            )
        }
    }

    private fun upcomingOccurrences(
        reminder: Reminder,
        currentDateTime: LocalDateTime,
    ): List<ReminderOccurrence> {
        val occurrences = mutableListOf<ReminderOccurrence>()
        var after = currentDateTime
        repeat(MAX_SCHEDULED_OCCURRENCES) {
            val scheduledDateTime = findNextReminderDateTime(reminder, after) ?: return occurrences
            occurrences.add(ReminderOccurrence(reminder, scheduledDateTime))
            after = scheduledDateTime
        }
        return occurrences
    }

    private fun buildNotificationId(reminderId: String, scheduledDateTime: LocalDateTime): String =
        "$REMINDER_NOTIFICATION_ID_PREFIX$reminderId|$scheduledDateTime"

    private data class ReminderOccurrence(
        val reminder: Reminder,
        val scheduledDateTime: LocalDateTime,
    )

    private companion object {
        // Personal and medication reminders split iOS's 64 pending-notification slots.
        const val MAX_SCHEDULED_OCCURRENCES = 32
        const val ALARM_MAX_DURATION_MINUTES = 10
    }
}
