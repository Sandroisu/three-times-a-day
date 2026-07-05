package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.notification.MEDICATION_REMINDER_ID_PREFIX
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.today.domain.ApplyMedicationIntakeRecordsUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.GenerateMedicationIntakeEventsForDateUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecord
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecordRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDate = LocalDate(2026, 7, 3)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadTodayTogglesLoadingAndDeliversEvents() = runTest(testDispatcher) {
        val loadGate = CompletableDeferred<Unit>()
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        scheduleRepository.loadGate = loadGate
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("wake", MedicationIntakeMoment.AfterWakeUp))
        )
        val viewModel = createViewModel(scheduleRepository, medicationRepository)

        runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)

        loadGate.complete(Unit)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.intakeEvents.isNotEmpty())
        assertNull(uiState.errorMessage)
    }

    @Test
    fun successfulLoadProducesEventsInTimeOrder() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(
            listOf(
                createMedication("sleep", MedicationIntakeMoment.BeforeSleep),
                createMedication("wake", MedicationIntakeMoment.AfterWakeUp),
                createMedication("breakfast", MedicationIntakeMoment.AfterBreakfast)
            )
        )
        val viewModel = createViewModel(scheduleRepository, medicationRepository)

        advanceUntilIdle()

        val eventTimes = viewModel.uiState.value.intakeEvents.map { event -> event.scheduledDateTime.time }
        assertEquals(listOf(LocalTime(8, 0), LocalTime(8, 45), LocalTime(23, 15)), eventTimes)
    }

    @Test
    fun changingScheduleRecomputesEventTimesOnReload() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(
            createSchedule(breakfastTime = LocalTime(8, 30))
        )
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("breakfast", MedicationIntakeMoment.AfterBreakfast))
        )
        val viewModel = createViewModel(scheduleRepository, medicationRepository)
        advanceUntilIdle()
        assertEquals(
            LocalTime(8, 45),
            viewModel.uiState.value.intakeEvents.single().scheduledDateTime.time
        )

        scheduleRepository.scheduleToReturn = createSchedule(breakfastTime = LocalTime(9, 0))
        viewModel.loadToday()
        advanceUntilIdle()

        assertEquals(
            LocalTime(9, 15),
            viewModel.uiState.value.intakeEvents.single().scheduledDateTime.time
        )
    }

    @Test
    fun scheduleRepositoryFailureSetsErrorMessage() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        scheduleRepository.loadError = IllegalStateException("Режим дня недоступен")
        val medicationRepository = FakeMedicationRepository(emptyList())
        val viewModel = createViewModel(scheduleRepository, medicationRepository)

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun medicationRepositoryFailureSetsErrorMessage() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(emptyList())
        medicationRepository.loadError = IllegalStateException("Препараты недоступны")
        val viewModel = createViewModel(scheduleRepository, medicationRepository)

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun loadTodayAppliesStoredTakenRecord() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("wake", MedicationIntakeMoment.AfterWakeUp))
        )
        val recordRepository = FakeMedicationIntakeRecordRepository()
        val viewModel = createViewModel(scheduleRepository, medicationRepository, recordRepository)
        advanceUntilIdle()
        val generatedEvent = viewModel.uiState.value.intakeEvents.single()
        recordRepository.saveRecord(
            MedicationIntakeRecord(
                eventId = generatedEvent.eventId,
                medicationId = generatedEvent.medicationId,
                scheduledDateTime = generatedEvent.scheduledDateTime,
                status = MedicationIntakeStatus.Taken,
                updatedDateTime = LocalDateTime(testDate, LocalTime(8, 5)),
                postponedDateTime = null
            )
        )

        viewModel.loadToday()
        advanceUntilIdle()

        assertEquals(MedicationIntakeStatus.Taken, viewModel.uiState.value.intakeEvents.single().status)
    }

    @Test
    fun markIntakeTakenSavesRecordAndUpdatesUi() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("wake", MedicationIntakeMoment.AfterWakeUp))
        )
        val recordRepository = FakeMedicationIntakeRecordRepository()
        val viewModel = createViewModel(scheduleRepository, medicationRepository, recordRepository)
        advanceUntilIdle()
        val eventId = viewModel.uiState.value.intakeEvents.single().eventId

        viewModel.markIntakeTaken(eventId)
        advanceUntilIdle()

        assertEquals(MedicationIntakeStatus.Taken, viewModel.uiState.value.intakeEvents.single().status)
        val savedRecord = recordRepository.currentRecords().single()
        assertEquals(eventId, savedRecord.eventId)
        assertEquals(MedicationIntakeStatus.Taken, savedRecord.status)
    }

    @Test
    fun markIntakeSkippedSavesRecordAndUpdatesUi() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("wake", MedicationIntakeMoment.AfterWakeUp))
        )
        val recordRepository = FakeMedicationIntakeRecordRepository()
        val viewModel = createViewModel(scheduleRepository, medicationRepository, recordRepository)
        advanceUntilIdle()
        val eventId = viewModel.uiState.value.intakeEvents.single().eventId

        viewModel.markIntakeSkipped(eventId)
        advanceUntilIdle()

        assertEquals(MedicationIntakeStatus.Skipped, viewModel.uiState.value.intakeEvents.single().status)
        assertEquals(MedicationIntakeStatus.Skipped, recordRepository.currentRecords().single().status)
    }

    @Test
    fun postponeIntakeMovesEventByTenMinutesAndUpdatesUi() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(
            createSchedule(breakfastTime = LocalTime(8, 30))
        )
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("breakfast", MedicationIntakeMoment.AfterBreakfast))
        )
        val recordRepository = FakeMedicationIntakeRecordRepository()
        val viewModel = createViewModel(scheduleRepository, medicationRepository, recordRepository)
        advanceUntilIdle()
        val eventBeforePostpone = viewModel.uiState.value.intakeEvents.single()
        assertEquals(LocalTime(8, 45), eventBeforePostpone.scheduledDateTime.time)

        viewModel.postponeIntake(eventBeforePostpone.eventId)
        advanceUntilIdle()

        val eventAfterPostpone = viewModel.uiState.value.intakeEvents.single()
        assertEquals(MedicationIntakeStatus.Postponed, eventAfterPostpone.status)
        assertEquals(LocalTime(8, 55), eventAfterPostpone.scheduledDateTime.time)
        val savedRecord = recordRepository.currentRecords().single()
        assertEquals(LocalDateTime(testDate, LocalTime(8, 55)), savedRecord.postponedDateTime)
    }

    @Test
    fun loadTodaySchedulesRemindersForScheduledEvents() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("wake", MedicationIntakeMoment.AfterWakeUp))
        )
        val reminderScheduler = FakeMedicationReminderScheduler()
        val viewModel = createViewModel(
            scheduleRepository = scheduleRepository,
            medicationRepository = medicationRepository,
            reminderScheduler = reminderScheduler
        )

        advanceUntilIdle()

        val scheduledNotification = reminderScheduler.scheduledNotifications.single()
        val eventId = viewModel.uiState.value.intakeEvents.single().eventId
        assertEquals(MEDICATION_REMINDER_ID_PREFIX + eventId, scheduledNotification.notificationId)
        assertTrue(reminderScheduler.cancelAllCount >= 1)
    }

    @Test
    fun loadTodayDoesNotScheduleTakenOrSkippedEvents() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(
            listOf(
                createMedication("wake", MedicationIntakeMoment.AfterWakeUp),
                createMedication("breakfast", MedicationIntakeMoment.AfterBreakfast)
            )
        )
        val recordRepository = FakeMedicationIntakeRecordRepository()
        val reminderScheduler = FakeMedicationReminderScheduler()
        val viewModel = createViewModel(
            scheduleRepository = scheduleRepository,
            medicationRepository = medicationRepository,
            recordRepository = recordRepository,
            reminderScheduler = reminderScheduler
        )
        advanceUntilIdle()
        val events = viewModel.uiState.value.intakeEvents
        recordRepository.saveRecord(recordFor(events[0], MedicationIntakeStatus.Taken))
        recordRepository.saveRecord(recordFor(events[1], MedicationIntakeStatus.Skipped))

        viewModel.loadToday()
        advanceUntilIdle()

        assertTrue(reminderScheduler.scheduledNotifications.isEmpty())
    }

    @Test
    fun markIntakeTakenCancelsReminder() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("wake", MedicationIntakeMoment.AfterWakeUp))
        )
        val reminderScheduler = FakeMedicationReminderScheduler()
        val viewModel = createViewModel(
            scheduleRepository = scheduleRepository,
            medicationRepository = medicationRepository,
            reminderScheduler = reminderScheduler
        )
        advanceUntilIdle()
        val eventId = viewModel.uiState.value.intakeEvents.single().eventId

        viewModel.markIntakeTaken(eventId)
        advanceUntilIdle()

        assertTrue(reminderScheduler.canceledNotificationIds.contains(MEDICATION_REMINDER_ID_PREFIX + eventId))
    }

    @Test
    fun markIntakeSkippedCancelsReminder() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("wake", MedicationIntakeMoment.AfterWakeUp))
        )
        val reminderScheduler = FakeMedicationReminderScheduler()
        val viewModel = createViewModel(
            scheduleRepository = scheduleRepository,
            medicationRepository = medicationRepository,
            reminderScheduler = reminderScheduler
        )
        advanceUntilIdle()
        val eventId = viewModel.uiState.value.intakeEvents.single().eventId

        viewModel.markIntakeSkipped(eventId)
        advanceUntilIdle()

        assertTrue(reminderScheduler.canceledNotificationIds.contains(MEDICATION_REMINDER_ID_PREFIX + eventId))
    }

    @Test
    fun postponeIntakeCancelsOldReminderAndSchedulesPostponedReminder() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(
            createSchedule(breakfastTime = LocalTime(8, 30))
        )
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("breakfast", MedicationIntakeMoment.AfterBreakfast))
        )
        val reminderScheduler = FakeMedicationReminderScheduler()
        val viewModel = createViewModel(
            scheduleRepository = scheduleRepository,
            medicationRepository = medicationRepository,
            reminderScheduler = reminderScheduler
        )
        advanceUntilIdle()
        val eventId = viewModel.uiState.value.intakeEvents.single().eventId

        viewModel.postponeIntake(eventId)
        advanceUntilIdle()

        assertTrue(reminderScheduler.canceledNotificationIds.contains(MEDICATION_REMINDER_ID_PREFIX + eventId))
        val postponedNotification = reminderScheduler.scheduledNotifications.single()
        assertEquals(MEDICATION_REMINDER_ID_PREFIX + eventId, postponedNotification.notificationId)
        assertEquals(LocalTime(8, 55), postponedNotification.scheduledDateTime.time)
    }

    @Test
    fun schedulerFailureDoesNotPreventStatusSave() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("wake", MedicationIntakeMoment.AfterWakeUp))
        )
        val recordRepository = FakeMedicationIntakeRecordRepository()
        val reminderScheduler = FakeMedicationReminderScheduler()
        val viewModel = createViewModel(
            scheduleRepository = scheduleRepository,
            medicationRepository = medicationRepository,
            recordRepository = recordRepository,
            reminderScheduler = reminderScheduler
        )
        advanceUntilIdle()
        val eventId = viewModel.uiState.value.intakeEvents.single().eventId
        reminderScheduler.cancelError = IllegalStateException("Уведомления недоступны")

        viewModel.markIntakeTaken(eventId)
        advanceUntilIdle()

        assertEquals(MedicationIntakeStatus.Taken, recordRepository.currentRecords().single().status)
        assertNotNull(viewModel.uiState.value.notificationErrorMessage)
    }

    private fun recordFor(
        event: MedicationIntakeEvent,
        status: MedicationIntakeStatus
    ): MedicationIntakeRecord = MedicationIntakeRecord(
        eventId = event.eventId,
        medicationId = event.medicationId,
        scheduledDateTime = event.scheduledDateTime,
        status = status,
        updatedDateTime = LocalDateTime(testDate, LocalTime(8, 5)),
        postponedDateTime = null
    )

    private fun createViewModel(
        scheduleRepository: DailyScheduleRepository,
        medicationRepository: MedicationRepository,
        recordRepository: MedicationIntakeRecordRepository = FakeMedicationIntakeRecordRepository(),
        reminderScheduler: MedicationReminderScheduler = FakeMedicationReminderScheduler()
    ): TodayViewModel = TodayViewModel(
        timeProvider = FakeTimeProvider(testDate),
        dailyScheduleRepository = scheduleRepository,
        medicationRepository = medicationRepository,
        medicationIntakeRecordRepository = recordRepository,
        generateMedicationIntakeEventsForDate = GenerateMedicationIntakeEventsForDateUseCase(),
        applyMedicationIntakeRecords = ApplyMedicationIntakeRecordsUseCase(),
        medicationReminderScheduler = reminderScheduler
    )

    private fun createSchedule(
        wakeUpTime: LocalTime = LocalTime(8, 0),
        breakfastTime: LocalTime = LocalTime(8, 30),
        lunchTime: LocalTime = LocalTime(13, 30),
        dinnerTime: LocalTime = LocalTime(19, 0),
        sleepTime: LocalTime = LocalTime(23, 30)
    ): DailySchedule = DailySchedule(
        wakeUpTime = wakeUpTime,
        breakfastTime = breakfastTime,
        lunchTime = lunchTime,
        dinnerTime = dinnerTime,
        sleepTime = sleepTime
    )

    private fun createMedication(
        medicationId: String,
        intakeMoment: MedicationIntakeMoment
    ): Medication = Medication(
        id = medicationId,
        name = "Препарат $medicationId",
        dosageText = "1 таблетка",
        intakeRule = MedicationIntakeRule.AtMoment(intakeMoment),
        courseStartDate = LocalDate(2020, 1, 1),
        courseEndDate = null
    )

    private class FakeTimeProvider(
        private val fixedDate: LocalDate
    ) : TimeProvider {

        override fun currentDate(): LocalDate = fixedDate

        override fun currentDateTime(): LocalDateTime = LocalDateTime(fixedDate, LocalTime(0, 0))
    }

    private class FakeDailyScheduleRepository(
        var scheduleToReturn: DailySchedule
    ) : DailyScheduleRepository {

        var loadError: Throwable? = null
        var loadGate: CompletableDeferred<Unit>? = null

        override suspend fun getDailySchedule(): DailySchedule {
            loadError?.let { throw it }
            loadGate?.await()
            return scheduleToReturn
        }

        override suspend fun saveDailySchedule(dailySchedule: DailySchedule) {
            scheduleToReturn = dailySchedule
        }
    }

    private class FakeMedicationRepository(
        initialMedications: List<Medication>
    ) : MedicationRepository {

        private val storedMedications: MutableList<Medication> = initialMedications.toMutableList()
        var loadError: Throwable? = null

        override suspend fun getMedications(): List<Medication> {
            loadError?.let { throw it }
            return storedMedications.toList()
        }

        override suspend fun saveMedication(medication: Medication) {
            storedMedications.add(medication)
        }

        override suspend fun updateMedication(medication: Medication) {
            val existingIndex = storedMedications.indexOfFirst { it.id == medication.id }
            if (existingIndex >= 0) {
                storedMedications[existingIndex] = medication
            }
        }

        override suspend fun deleteMedication(medicationId: String) {
            storedMedications.removeAll { it.id == medicationId }
        }
    }

    private class FakeMedicationIntakeRecordRepository : MedicationIntakeRecordRepository {

        private val storedRecords: MutableList<MedicationIntakeRecord> = mutableListOf()
        var saveError: Throwable? = null

        fun currentRecords(): List<MedicationIntakeRecord> = storedRecords.toList()

        override suspend fun getRecordsForDate(date: LocalDate): List<MedicationIntakeRecord> =
            storedRecords.filter { record -> record.scheduledDateTime.date == date }

        override suspend fun saveRecord(record: MedicationIntakeRecord) {
            saveError?.let { throw it }
            val existingIndex = storedRecords.indexOfFirst { it.eventId == record.eventId }
            if (existingIndex >= 0) {
                storedRecords[existingIndex] = record
            } else {
                storedRecords.add(record)
            }
        }

        override suspend fun deleteRecord(eventId: String) {
            storedRecords.removeAll { it.eventId == eventId }
        }
    }

    private class FakeMedicationReminderScheduler : MedicationReminderScheduler {

        val scheduledNotifications: MutableList<MedicationReminderNotification> = mutableListOf()
        val canceledNotificationIds: MutableList<String> = mutableListOf()
        var cancelAllCount = 0
        var permissionStatus = NotificationPermissionStatus.Granted
        var requestPermissionResult = NotificationPermissionStatus.Granted
        var scheduleError: Throwable? = null
        var cancelError: Throwable? = null

        override suspend fun getPermissionStatus(): NotificationPermissionStatus = permissionStatus

        override suspend fun requestPermission(): NotificationPermissionStatus {
            permissionStatus = requestPermissionResult
            return requestPermissionResult
        }

        override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
            scheduleError?.let { throw it }
            scheduledNotifications.add(notification)
        }

        override suspend fun cancelReminder(notificationId: String) {
            cancelError?.let { throw it }
            canceledNotificationIds.add(notificationId)
        }

        override suspend fun cancelAllReminders() {
            cancelError?.let { throw it }
            cancelAllCount++
            scheduledNotifications.clear()
        }
    }
}
