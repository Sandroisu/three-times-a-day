package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.reminder.domain.FindNextReminderDateTimeUseCase
import io.github.sandroisu.threetimesaday.feature.reminder.domain.Reminder
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderIdGenerator
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRecurrence
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRepository
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderWeekday
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

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val currentDateTime = LocalDateTime(LocalDate(2026, 11, 2), LocalTime(8, 0))
    private val workdays = listOf(
        ReminderWeekday.Monday,
        ReminderWeekday.Tuesday,
        ReminderWeekday.Wednesday,
        ReminderWeekday.Thursday,
        ReminderWeekday.Friday,
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
    fun workdaysPresetSavesMondayToFriday() = runTest(testDispatcher) {
        val repository = FakeReminderRepository()
        val viewModel = createViewModel(repository)
        viewModel.start(reminderId = null)
        viewModel.onTitleChanged("Stand-up")
        viewModel.onRecurrenceTypeSelected(ReminderRecurrenceType.Workdays)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(ReminderRecurrence.OnWeekdays(workdays), repository.reminders.single().recurrence)
    }

    @Test
    fun reminderOnMondayToFridayOpensAsWorkdays() = runTest(testDispatcher) {
        val repository = FakeReminderRepository(reminder(ReminderRecurrence.OnWeekdays(workdays)))
        val viewModel = createViewModel(repository)

        viewModel.start(reminderId = REMINDER_ID)
        advanceUntilIdle()

        assertEquals(ReminderRecurrenceType.Workdays, viewModel.uiState.value.recurrenceType)
    }

    @Test
    fun reminderOnOtherWeekdaysOpensAsWeekdays() = runTest(testDispatcher) {
        val weekdays = listOf(ReminderWeekday.Monday, ReminderWeekday.Wednesday)
        val repository = FakeReminderRepository(reminder(ReminderRecurrence.OnWeekdays(weekdays)))
        val viewModel = createViewModel(repository)

        viewModel.start(reminderId = REMINDER_ID)
        advanceUntilIdle()

        assertEquals(ReminderRecurrenceType.Weekdays, viewModel.uiState.value.recurrenceType)
        assertEquals(weekdays.toSet(), viewModel.uiState.value.selectedWeekdays)
    }

    @Test
    fun mondayToFridayIsLabeledAsWorkdaysInList() {
        assertEquals(
            ReminderLabels.workdaysRecurrence,
            reminderRecurrenceLabel(ReminderRecurrence.OnWeekdays(workdays)),
        )
    }

    private fun createViewModel(repository: FakeReminderRepository): ReminderEditorViewModel {
        val timeProvider = FakeTimeProvider(currentDateTime)
        return ReminderEditorViewModel(
            reminderRepository = repository,
            reminderIdGenerator = FakeReminderIdGenerator(),
            rescheduleReminders = RescheduleRemindersUseCase(
                reminderRepository = repository,
                findNextReminderDateTime = FindNextReminderDateTimeUseCase(),
                medicationReminderScheduler = FakeScheduler(),
                timeProvider = timeProvider,
                buildReminderMessage = { "Reminder" },
            ),
            timeProvider = timeProvider,
        )
    }

    private fun reminder(recurrence: ReminderRecurrence): Reminder = Reminder(
        id = REMINDER_ID,
        title = "Stand-up",
        date = currentDateTime.date,
        time = LocalTime(10, 0),
        recurrence = recurrence,
    )

    private class FakeReminderRepository(vararg initialReminders: Reminder) : ReminderRepository {
        val reminders: MutableList<Reminder> = initialReminders.toMutableList()

        override suspend fun getReminders(): List<Reminder> = reminders.toList()

        override suspend fun saveReminder(reminder: Reminder) {
            reminders.add(reminder)
        }

        override suspend fun updateReminder(reminder: Reminder) {
            val index = reminders.indexOfFirst { storedReminder -> storedReminder.id == reminder.id }
            if (index >= 0) reminders[index] = reminder
        }

        override suspend fun deleteReminder(reminderId: String) {
            reminders.removeAll { storedReminder -> storedReminder.id == reminderId }
        }
    }

    private class FakeReminderIdGenerator : ReminderIdGenerator {
        override suspend fun nextId(): String = REMINDER_ID
    }

    private class FakeScheduler : MedicationReminderScheduler {
        override suspend fun getPermissionStatus(): NotificationPermissionStatus = NotificationPermissionStatus.Granted
        override suspend fun requestPermission(): NotificationPermissionStatus = NotificationPermissionStatus.Granted
        override suspend fun scheduleReminder(notification: MedicationReminderNotification) = Unit
        override suspend fun cancelReminder(notificationId: String) = Unit
        override suspend fun cancelAllReminders() = Unit
    }

    private class FakeTimeProvider(
        private val dateTime: LocalDateTime,
    ) : TimeProvider {
        override fun currentDate(): LocalDate = dateTime.date
        override fun currentDateTime(): LocalDateTime = dateTime
    }

    private companion object {
        const val REMINDER_ID = "reminder-1"
    }
}
