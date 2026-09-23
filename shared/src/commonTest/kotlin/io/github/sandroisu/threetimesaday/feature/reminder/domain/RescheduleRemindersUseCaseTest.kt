package io.github.sandroisu.threetimesaday.feature.reminder.domain

import io.github.sandroisu.threetimesaday.core.notification.REMINDER_NOTIFICATION_ID_PREFIX
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RescheduleRemindersUseCaseTest {

    @Test
    fun schedulesNextQuarterlyReminderWithoutCancellingMedicationReminders() = runTest {
        val scheduler = FakeScheduler()
        val reminder = Reminder(
            id = "taxes",
            title = "Pay taxes",
            date = LocalDate(2026, 1, 14),
            time = LocalTime(9, 0),
            recurrence = ReminderRecurrence.EveryMonthsOnDay(3, 14),
        )
        val useCase = RescheduleRemindersUseCase(
            reminderRepository = FakeReminderRepository(listOf(reminder)),
            findNextReminderDateTime = FindNextReminderDateTimeUseCase(),
            medicationReminderScheduler = scheduler,
            timeProvider = FakeTimeProvider(LocalDateTime(LocalDate(2026, 1, 15), LocalTime(12, 0))),
            buildReminderMessage = { "Reminder" },
        )

        useCase()

        assertEquals(listOf(REMINDER_NOTIFICATION_ID_PREFIX), scheduler.cancelledPrefixes)
        assertEquals(
            MedicationReminderNotification(
                notificationId = REMINDER_NOTIFICATION_ID_PREFIX + "taxes",
                title = "Pay taxes",
                message = "Reminder",
                scheduledDateTime = LocalDateTime(LocalDate(2026, 4, 14), LocalTime(9, 0)),
            ),
            scheduler.scheduledNotifications.single(),
        )
    }

    @Test
    fun doesNotSchedulePastOneTimeReminder() = runTest {
        val scheduler = FakeScheduler()
        val useCase = RescheduleRemindersUseCase(
            reminderRepository = FakeReminderRepository(
                listOf(
                    Reminder(
                        id = "passport",
                        title = "Renew passport",
                        date = LocalDate(2026, 1, 14),
                        time = LocalTime(9, 0),
                        recurrence = ReminderRecurrence.Once,
                    ),
                ),
            ),
            findNextReminderDateTime = FindNextReminderDateTimeUseCase(),
            medicationReminderScheduler = scheduler,
            timeProvider = FakeTimeProvider(LocalDateTime(LocalDate(2026, 1, 15), LocalTime(12, 0))),
            buildReminderMessage = { "Reminder" },
        )

        useCase()

        assertTrue(scheduler.scheduledNotifications.isEmpty())
    }

    private class FakeReminderRepository(
        private val reminders: List<Reminder>,
    ) : ReminderRepository {
        override suspend fun getReminders(): List<Reminder> = reminders
        override suspend fun saveReminder(reminder: Reminder) = Unit
        override suspend fun updateReminder(reminder: Reminder) = Unit
        override suspend fun deleteReminder(reminderId: String) = Unit
    }

    private class FakeScheduler : MedicationReminderScheduler {
        val cancelledPrefixes: MutableList<String> = mutableListOf()
        val scheduledNotifications: MutableList<MedicationReminderNotification> = mutableListOf()

        override suspend fun getPermissionStatus(): NotificationPermissionStatus = NotificationPermissionStatus.Granted
        override suspend fun requestPermission(): NotificationPermissionStatus = NotificationPermissionStatus.Granted
        override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
            scheduledNotifications.add(notification)
        }

        override suspend fun cancelReminder(notificationId: String) = Unit
        override suspend fun cancelAllReminders() = Unit
        override suspend fun cancelRemindersWithPrefix(notificationIdPrefix: String) {
            cancelledPrefixes.add(notificationIdPrefix)
        }
    }

    private class FakeTimeProvider(
        private val dateTime: LocalDateTime,
    ) : TimeProvider {
        override fun currentDate(): LocalDate = dateTime.date
        override fun currentDateTime(): LocalDateTime = dateTime
    }
}
