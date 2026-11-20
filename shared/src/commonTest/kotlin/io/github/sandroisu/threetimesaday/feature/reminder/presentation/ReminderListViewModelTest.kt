package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import io.github.sandroisu.threetimesaday.core.settings.AppSettingsOpener
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.reminder.domain.FindNextReminderDateTimeUseCase
import io.github.sandroisu.threetimesaday.feature.reminder.domain.Reminder
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRecurrence
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRepository
import io.github.sandroisu.threetimesaday.feature.reminder.domain.RescheduleRemindersUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val currentDateTime = LocalDateTime(LocalDate(2026, 11, 2), LocalTime(8, 0))
    private val earlyPausedReminder = Reminder(
        id = "gym",
        title = "Go to the gym",
        date = currentDateTime.date,
        time = LocalTime(9, 0),
        recurrence = ReminderRecurrence.Daily,
        isEnabled = false,
    )
    private val lateActiveReminder = Reminder(
        id = "water",
        title = "Water plants",
        date = currentDateTime.date,
        time = LocalTime(18, 30),
        recurrence = ReminderRecurrence.Daily,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun pausedReminderIsListedAfterActiveOnesWithoutNextOccurrence() = runTest(testDispatcher) {
        val viewModel = createViewModel(FakeReminderRepository(earlyPausedReminder, lateActiveReminder))

        viewModel.loadReminders()
        advanceUntilIdle()

        val reminders = viewModel.uiState.value.reminders
        assertEquals(listOf("water", "gym"), reminders.map { reminder -> reminder.id })
        val pausedItem = reminders.last()
        assertFalse(pausedItem.isEnabled)
        assertNull(pausedItem.nextOccurrence)
        assertEquals(ReminderLabels.paused, pausedItem.nextOccurrenceText)
    }

    private fun createViewModel(
        repository: FakeReminderRepository,
        scheduler: FakeScheduler = FakeScheduler(),
    ): ReminderListViewModel {
        val timeProvider = FakeTimeProvider(currentDateTime)
        return ReminderListViewModel(
            reminderRepository = repository,
            findNextReminderDateTime = FindNextReminderDateTimeUseCase(),
            timeProvider = timeProvider,
            medicationReminderScheduler = scheduler,
            rescheduleReminders = RescheduleRemindersUseCase(
                reminderRepository = repository,
                findNextReminderDateTime = FindNextReminderDateTimeUseCase(),
                medicationReminderScheduler = scheduler,
                timeProvider = timeProvider,
                buildReminderMessage = { "Reminder" },
            ),
            appSettingsOpener = FakeAppSettingsOpener(),
        )
    }

    private class FakeReminderRepository(vararg initialReminders: Reminder) : ReminderRepository {
        val reminders: MutableList<Reminder> = initialReminders.toMutableList()
        var failOnUpdate: Boolean = false

        override suspend fun getReminders(): List<Reminder> = reminders.toList()

        override suspend fun saveReminder(reminder: Reminder) {
            reminders.add(reminder)
        }

        override suspend fun updateReminder(reminder: Reminder) {
            if (failOnUpdate) throw IllegalStateException("Storage is full")
            val index = reminders.indexOfFirst { storedReminder -> storedReminder.id == reminder.id }
            if (index >= 0) reminders[index] = reminder
        }

        override suspend fun deleteReminder(reminderId: String) {
            reminders.removeAll { storedReminder -> storedReminder.id == reminderId }
        }
    }

    private class FakeScheduler : MedicationReminderScheduler {
        val scheduledNotificationIds: MutableList<String> = mutableListOf()

        override suspend fun getPermissionStatus(): NotificationPermissionStatus = NotificationPermissionStatus.Granted
        override suspend fun requestPermission(): NotificationPermissionStatus = NotificationPermissionStatus.Granted
        override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
            scheduledNotificationIds.add(notification.notificationId)
        }

        override suspend fun cancelReminder(notificationId: String) = Unit
        override suspend fun cancelAllReminders() = Unit
        override suspend fun cancelRemindersWithPrefix(notificationIdPrefix: String) {
            scheduledNotificationIds.removeAll { notificationId -> notificationId.startsWith(notificationIdPrefix) }
        }
    }

    private class FakeAppSettingsOpener : AppSettingsOpener {
        override fun openAppSettings() = Unit
        override fun openExactReminderSettings() = Unit
    }

    private class FakeTimeProvider(
        private val dateTime: LocalDateTime,
    ) : TimeProvider {
        override fun currentDate(): LocalDate = dateTime.date
        override fun currentDateTime(): LocalDateTime = dateTime
    }
}
