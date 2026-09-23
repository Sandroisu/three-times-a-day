package io.github.sandroisu.threetimesaday.feature.reminder.domain

import io.github.sandroisu.threetimesaday.core.notification.REMINDER_NOTIFICATION_ID_PREFIX
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.time.TimeProvider

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
        reminderRepository.getReminders().forEach { reminder ->
            val scheduledDateTime = findNextReminderDateTime(reminder, currentDateTime) ?: return@forEach
            medicationReminderScheduler.scheduleReminder(
                MedicationReminderNotification(
                    notificationId = REMINDER_NOTIFICATION_ID_PREFIX + reminder.id,
                    title = reminder.title,
                    message = buildReminderMessage(reminder),
                    scheduledDateTime = scheduledDateTime,
                )
            )
        }
    }
}
