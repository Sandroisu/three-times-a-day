package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.core.notification.MEDICATION_REMINDER_ID_PREFIX
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.storageJson
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.data.PersistentMedicationRepository
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.schedule.data.PersistentDailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.today.data.PersistentMedicationIntakeRecordRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HandleMedicationReminderActionUseCaseTest {

    private val storage = InMemoryKeyValueStorage()
    private val medicationRepository = PersistentMedicationRepository(storage, storageJson)
    private val recordRepository = PersistentMedicationIntakeRecordRepository(storage, storageJson)
    private val scheduler = RecordingScheduler()
    private val timeProvider = MutableTimeProvider(LocalDateTime(TODAY, LocalTime(9, 2)))
    private val handleAction = createUseCase()

    @Test
    fun takenActionMarksIntakeTakenAndTakesDose() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))

        val result = handleAction(TODAY_EVENT_ID, MedicationReminderAction.Taken)

        assertEquals(MedicationReminderActionResult.Applied, result)
        val record = recordRepository.getRecord(TODAY_EVENT_ID)
        assertEquals(MedicationIntakeStatus.Taken, record?.status)
        assertEquals(MEDICATION_ID, record?.medicationId)
        assertEquals(LocalDateTime(TODAY, LocalTime(9, 2)), record?.updatedDateTime)
        assertEquals(9, dosesLeft())
    }

    @Test
    fun takenActionRemovesPendingReminderForThisIntake() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = null))

        handleAction(TODAY_EVENT_ID, MedicationReminderAction.Taken)

        assertEquals(listOf(MEDICATION_REMINDER_ID_PREFIX), scheduler.cancelledPrefixes)
        assertTrue(scheduler.scheduledNotifications.none { notification ->
            notification.notificationId == MEDICATION_REMINDER_ID_PREFIX + TODAY_EVENT_ID
        })
    }

    @Test
    fun postponeActionSavesPostponedTimeAndSchedulesReminderThere() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))

        val result = handleAction(TODAY_EVENT_ID, MedicationReminderAction.Postpone)

        assertEquals(MedicationReminderActionResult.Applied, result)
        val record = recordRepository.getRecord(TODAY_EVENT_ID)
        assertEquals(MedicationIntakeStatus.Postponed, record?.status)
        assertEquals(LocalDateTime(TODAY, LocalTime(9, 12)), record?.postponedDateTime)
        val postponedNotification = scheduler.scheduledNotifications.first()
        assertEquals(MEDICATION_REMINDER_ID_PREFIX + TODAY_EVENT_ID, postponedNotification.notificationId)
        assertEquals(LocalDateTime(TODAY, LocalTime(9, 12)), postponedNotification.scheduledDateTime)
        assertEquals(10, dosesLeft())
    }

    @Test
    fun postponingAgainCountsFromPostponedTime() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = null))
        handleAction(TODAY_EVENT_ID, MedicationReminderAction.Postpone)
        timeProvider.dateTime = LocalDateTime(TODAY, LocalTime(9, 12))

        handleAction(TODAY_EVENT_ID, MedicationReminderAction.Postpone)

        assertEquals(LocalDateTime(TODAY, LocalTime(9, 22)), recordRepository.getRecord(TODAY_EVENT_ID)?.postponedDateTime)
    }

    @Test
    fun takenActionAfterPostponeTakesDose() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))
        handleAction(TODAY_EVENT_ID, MedicationReminderAction.Postpone)

        val result = handleAction(TODAY_EVENT_ID, MedicationReminderAction.Taken)

        assertEquals(MedicationReminderActionResult.Applied, result)
        assertEquals(MedicationIntakeStatus.Taken, recordRepository.getRecord(TODAY_EVENT_ID)?.status)
        assertEquals(9, dosesLeft())
    }

    @Test
    fun actionOnYesterdayIntakeAfterMidnightIsApplied() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))
        timeProvider.dateTime = LocalDateTime(TODAY, LocalTime(0, 20))

        val result = handleAction(YESTERDAY_EVENT_ID, MedicationReminderAction.Taken)

        assertEquals(MedicationReminderActionResult.Applied, result)
        assertEquals(MedicationIntakeStatus.Taken, recordRepository.getRecord(YESTERDAY_EVENT_ID)?.status)
        assertEquals(9, dosesLeft())
    }

    @Test
    fun actionOnIntakeOlderThanYesterdayIsNotFound() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))

        val result = handleAction("$MEDICATION_ID|2026-07-01|09:00|ExactTime", MedicationReminderAction.Taken)

        assertEquals(MedicationReminderActionResult.EventNotFound, result)
        assertEquals(10, dosesLeft())
        assertTrue(scheduler.cancelledPrefixes.isEmpty())
    }

    @Test
    fun actionForDeletedMedicationIsNotFound() = runTest {
        val result = handleAction(TODAY_EVENT_ID, MedicationReminderAction.Taken)

        assertEquals(MedicationReminderActionResult.EventNotFound, result)
        assertNull(recordRepository.getRecord(TODAY_EVENT_ID))
    }

    @Test
    fun actionForUnknownEventIdIsNotFound() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))

        val result = handleAction("not-an-event", MedicationReminderAction.Postpone)

        assertEquals(MedicationReminderActionResult.EventNotFound, result)
        assertNull(recordRepository.getRecord("not-an-event"))
    }

    @Test
    fun takenActionOnTakenIntakeChangesNothing() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))
        handleAction(TODAY_EVENT_ID, MedicationReminderAction.Taken)
        scheduler.cancelledPrefixes.clear()

        val result = handleAction(TODAY_EVENT_ID, MedicationReminderAction.Taken)

        assertEquals(MedicationReminderActionResult.AlreadyResolved, result)
        assertEquals(9, dosesLeft())
        assertTrue(scheduler.cancelledPrefixes.isEmpty())
    }

    @Test
    fun postponeActionOnSkippedIntakeKeepsItSkipped() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = 10))
        recordRepository.saveRecord(
            MedicationIntakeRecord(
                eventId = TODAY_EVENT_ID,
                medicationId = MEDICATION_ID,
                scheduledDateTime = LocalDateTime(TODAY, LocalTime(9, 0)),
                status = MedicationIntakeStatus.Skipped,
                updatedDateTime = LocalDateTime(TODAY, LocalTime(9, 1)),
            )
        )

        val result = handleAction(TODAY_EVENT_ID, MedicationReminderAction.Postpone)

        assertEquals(MedicationReminderActionResult.AlreadyResolved, result)
        assertEquals(MedicationIntakeStatus.Skipped, recordRepository.getRecord(TODAY_EVENT_ID)?.status)
    }

    @Test
    fun reschedulingFailureReachesCallerAfterStatusIsSaved() = runTest {
        medicationRepository.saveMedication(medication(dosesLeft = null))
        scheduler.scheduleError = IllegalStateException("Scheduler is not available")

        assertFailsWith<IllegalStateException> {
            handleAction(TODAY_EVENT_ID, MedicationReminderAction.Postpone)
        }
        assertEquals(MedicationIntakeStatus.Postponed, recordRepository.getRecord(TODAY_EVENT_ID)?.status)
    }

    private fun createUseCase(): HandleMedicationReminderActionUseCase {
        val scheduleRepository = PersistentDailyScheduleRepository(storage, storageJson)
        val generateEvents = GenerateMedicationIntakeEventsForDateUseCase()
        val applyRecords = ApplyMedicationIntakeRecordsUseCase()
        return HandleMedicationReminderActionUseCase(
            timeProvider = timeProvider,
            dailyScheduleRepository = scheduleRepository,
            medicationRepository = medicationRepository,
            medicationIntakeRecordRepository = recordRepository,
            generateMedicationIntakeEventsForDate = generateEvents,
            applyMedicationIntakeRecords = applyRecords,
            buildMedicationIntakeRecord = BuildMedicationIntakeRecordUseCase(timeProvider),
            changeMedicationIntakeStatus = ChangeMedicationIntakeStatusUseCase(medicationRepository, recordRepository),
            rescheduleMedicationReminders = RescheduleMedicationRemindersUseCase(
                dailyScheduleRepository = scheduleRepository,
                medicationRepository = medicationRepository,
                medicationIntakeRecordRepository = recordRepository,
                generateMedicationIntakeEventsForDate = generateEvents,
                applyMedicationIntakeRecords = applyRecords,
                medicationReminderScheduler = scheduler,
                timeProvider = timeProvider,
                buildReminderMessage = { event -> event.dosageText },
            ),
        )
    }

    private suspend fun dosesLeft(): Int? = medicationRepository.getMedications().single().dosesLeft

    private fun medication(dosesLeft: Int?): Medication = Medication(
        id = MEDICATION_ID,
        name = "Aspirin",
        dosageText = "1 tablet",
        intakeRule = MedicationIntakeRule.AtExactTime(LocalTime(9, 0)),
        courseStartDate = LocalDate(2026, 6, 1),
        courseEndDate = null,
        dosesLeft = dosesLeft,
    )

    private class MutableTimeProvider(var dateTime: LocalDateTime) : TimeProvider {
        override fun currentDate(): LocalDate = dateTime.date
        override fun currentDateTime(): LocalDateTime = dateTime
    }

    private class RecordingScheduler : MedicationReminderScheduler {
        val scheduledNotifications: MutableList<MedicationReminderNotification> = mutableListOf()
        val cancelledPrefixes: MutableList<String> = mutableListOf()
        var scheduleError: Throwable? = null

        override suspend fun getPermissionStatus(): NotificationPermissionStatus = NotificationPermissionStatus.Granted

        override suspend fun requestPermission(): NotificationPermissionStatus = NotificationPermissionStatus.Granted

        override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
            scheduleError?.let { error -> throw error }
            scheduledNotifications.add(notification)
        }

        override suspend fun cancelReminder(notificationId: String) = Unit

        override suspend fun cancelAllReminders() = Unit

        override suspend fun cancelRemindersWithPrefix(notificationIdPrefix: String) {
            cancelledPrefixes.add(notificationIdPrefix)
            scheduledNotifications.removeAll { notification -> notification.notificationId.startsWith(notificationIdPrefix) }
        }
    }

    private companion object {
        val TODAY = LocalDate(2026, 7, 4)
        const val MEDICATION_ID = "aspirin"
        const val TODAY_EVENT_ID = "aspirin|2026-07-04|09:00|ExactTime"
        const val YESTERDAY_EVENT_ID = "aspirin|2026-07-03|09:00|ExactTime"
    }
}
