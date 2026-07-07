package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.core.notification.MEDICATION_REMINDER_ID_PREFIX
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RescheduleMedicationRemindersUseCaseTest {

    private val testDate = LocalDate(2026, 7, 6)
    private val nextDate = LocalDate(2026, 7, 7)
    private val afterNextDate = LocalDate(2026, 7, 8)
    private val currentDateTime = LocalDateTime(testDate, LocalTime(12, 0))

    @Test
    fun schedulesTodayEventInFuture() = runTest {
        val scheduler = FakeScheduler()
        val useCase = createUseCase(
            medications = listOf(exactTimeMedication("vitamin", LocalTime(18, 0), courseEndDate = testDate)),
            scheduler = scheduler
        )

        useCase()

        val notification = scheduler.scheduledNotifications.single()
        assertEquals(LocalDateTime(testDate, LocalTime(18, 0)), notification.scheduledDateTime)
    }

    @Test
    fun doesNotScheduleTodayEventInPast() = runTest {
        val scheduler = FakeScheduler()
        val useCase = createUseCase(
            medications = listOf(exactTimeMedication("vitamin", LocalTime(8, 0), courseEndDate = testDate)),
            scheduler = scheduler
        )

        useCase()

        assertTrue(scheduler.scheduledNotifications.isEmpty())
    }

    @Test
    fun schedulesTomorrowEventWithinHorizon() = runTest {
        val scheduler = FakeScheduler()
        val useCase = createUseCase(
            medications = listOf(exactTimeMedication("vitamin", LocalTime(9, 0), courseEndDate = nextDate)),
            scheduler = scheduler
        )

        useCase()

        val notification = scheduler.scheduledNotifications.single()
        assertEquals(LocalDateTime(nextDate, LocalTime(9, 0)), notification.scheduledDateTime)
    }

    @Test
    fun doesNotScheduleEventBeyondHorizon() = runTest {
        val scheduler = FakeScheduler()
        val useCase = createUseCase(
            medications = listOf(exactTimeMedication("vitamin", LocalTime(20, 0), courseEndDate = afterNextDate)),
            scheduler = scheduler
        )

        useCase()

        assertFalse(
            scheduler.scheduledNotifications.any { notification ->
                notification.scheduledDateTime.date == afterNextDate
            }
        )
    }

    @Test
    fun doesNotScheduleTakenOrSkippedEvents() = runTest {
        val scheduler = FakeScheduler()
        val recordRepository = FakeRecordRepository()
        val medications = listOf(
            exactTimeMedication("taken", LocalTime(18, 0), courseEndDate = testDate),
            exactTimeMedication("skipped", LocalTime(19, 0), courseEndDate = testDate)
        )
        val takenEvent = generatedEvent(medications, "taken", testDate)
        val skippedEvent = generatedEvent(medications, "skipped", testDate)
        recordRepository.saveRecord(recordFor(takenEvent, MedicationIntakeStatus.Taken))
        recordRepository.saveRecord(recordFor(skippedEvent, MedicationIntakeStatus.Skipped))
        val useCase = createUseCase(medications = medications, recordRepository = recordRepository, scheduler = scheduler)

        useCase()

        assertTrue(scheduler.scheduledNotifications.isEmpty())
    }

    @Test
    fun schedulesPostponedEventByPostponedDateTime() = runTest {
        val scheduler = FakeScheduler()
        val recordRepository = FakeRecordRepository()
        val medications = listOf(exactTimeMedication("vitamin", LocalTime(8, 0), courseEndDate = testDate))
        val pastEvent = generatedEvent(medications, "vitamin", testDate)
        val postponedDateTime = LocalDateTime(testDate, LocalTime(18, 0))
        recordRepository.saveRecord(
            recordFor(pastEvent, MedicationIntakeStatus.Postponed, postponedDateTime = postponedDateTime)
        )
        val useCase = createUseCase(medications = medications, recordRepository = recordRepository, scheduler = scheduler)

        useCase()

        val notification = scheduler.scheduledNotifications.single()
        assertEquals(postponedDateTime, notification.scheduledDateTime)
    }

    @Test
    fun cancelsAllRemindersBeforeScheduling() = runTest {
        val scheduler = FakeScheduler()
        val useCase = createUseCase(
            medications = listOf(exactTimeMedication("vitamin", LocalTime(18, 0), courseEndDate = testDate)),
            scheduler = scheduler
        )

        useCase()

        assertEquals(1, scheduler.cancelAllCount)
        assertEquals(CANCEL_ALL_OPERATION, scheduler.operations.first())
        assertTrue(scheduler.operations.count { operation -> operation == SCHEDULE_OPERATION } >= 1)
    }

    @Test
    fun schedulerFailurePropagates() = runTest {
        val scheduler = FakeScheduler()
        scheduler.scheduleError = IllegalStateException("Планировщик недоступен")
        val useCase = createUseCase(
            medications = listOf(exactTimeMedication("vitamin", LocalTime(18, 0), courseEndDate = testDate)),
            scheduler = scheduler
        )

        assertFailsWith<IllegalStateException> { useCase() }
    }

    private fun createUseCase(
        medications: List<Medication>,
        recordRepository: FakeRecordRepository = FakeRecordRepository(),
        scheduler: FakeScheduler = FakeScheduler()
    ): RescheduleMedicationRemindersUseCase = RescheduleMedicationRemindersUseCase(
        dailyScheduleRepository = FakeScheduleRepository(),
        medicationRepository = FakeMedicationRepository(medications),
        medicationIntakeRecordRepository = recordRepository,
        generateMedicationIntakeEventsForDate = GenerateMedicationIntakeEventsForDateUseCase(),
        applyMedicationIntakeRecords = ApplyMedicationIntakeRecordsUseCase(),
        medicationReminderScheduler = scheduler,
        timeProvider = FakeTimeProvider(currentDateTime),
        buildReminderMessage = { event -> event.medicationName }
    )

    private fun generatedEvent(
        medications: List<Medication>,
        medicationId: String,
        date: LocalDate
    ): MedicationIntakeEvent = GenerateMedicationIntakeEventsForDateUseCase()
        .invoke(date, defaultSchedule, medications)
        .first { event -> event.medicationId == medicationId }

    private fun recordFor(
        event: MedicationIntakeEvent,
        status: MedicationIntakeStatus,
        postponedDateTime: LocalDateTime? = null
    ): MedicationIntakeRecord = MedicationIntakeRecord(
        eventId = event.eventId,
        medicationId = event.medicationId,
        scheduledDateTime = event.scheduledDateTime,
        status = status,
        updatedDateTime = currentDateTime,
        postponedDateTime = postponedDateTime
    )

    private fun exactTimeMedication(
        medicationId: String,
        time: LocalTime,
        courseEndDate: LocalDate?
    ): Medication = Medication(
        id = medicationId,
        name = "Препарат $medicationId",
        dosageText = "1 таблетка",
        intakeRule = MedicationIntakeRule.AtExactTime(time),
        courseStartDate = LocalDate(2020, 1, 1),
        courseEndDate = courseEndDate
    )

    private val defaultSchedule = DailySchedule(
        wakeUpTime = LocalTime(8, 0),
        breakfastTime = LocalTime(8, 30),
        lunchTime = LocalTime(13, 30),
        dinnerTime = LocalTime(19, 0),
        sleepTime = LocalTime(23, 30)
    )

    private inner class FakeScheduleRepository : DailyScheduleRepository {
        override suspend fun getDailySchedule(): DailySchedule = defaultSchedule
        override suspend fun saveDailySchedule(dailySchedule: DailySchedule) = Unit
    }

    private class FakeMedicationRepository(
        private val medications: List<Medication>
    ) : MedicationRepository {
        override suspend fun getMedications(): List<Medication> = medications
        override suspend fun saveMedication(medication: Medication) = Unit
        override suspend fun updateMedication(medication: Medication) = Unit
        override suspend fun deleteMedication(medicationId: String) = Unit
    }

    private class FakeRecordRepository : MedicationIntakeRecordRepository {
        private val storedRecords: MutableList<MedicationIntakeRecord> = mutableListOf()

        override suspend fun getRecordsForDate(date: LocalDate): List<MedicationIntakeRecord> =
            storedRecords.filter { record -> record.scheduledDateTime.date == date }

        override suspend fun saveRecord(record: MedicationIntakeRecord) {
            storedRecords.removeAll { stored -> stored.eventId == record.eventId }
            storedRecords.add(record)
        }

        override suspend fun deleteRecord(eventId: String) {
            storedRecords.removeAll { stored -> stored.eventId == eventId }
        }
    }

    private class FakeTimeProvider(
        private val fixedDateTime: LocalDateTime
    ) : TimeProvider {
        override fun currentDate(): LocalDate = fixedDateTime.date
        override fun currentDateTime(): LocalDateTime = fixedDateTime
    }

    private class FakeScheduler : MedicationReminderScheduler {
        val scheduledNotifications: MutableList<MedicationReminderNotification> = mutableListOf()
        val operations: MutableList<String> = mutableListOf()
        var cancelAllCount = 0
        var scheduleError: Throwable? = null

        override suspend fun getPermissionStatus(): NotificationPermissionStatus =
            NotificationPermissionStatus.Granted

        override suspend fun requestPermission(): NotificationPermissionStatus =
            NotificationPermissionStatus.Granted

        override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
            operations.add(SCHEDULE_OPERATION)
            scheduleError?.let { throw it }
            scheduledNotifications.add(notification)
        }

        override suspend fun cancelReminder(notificationId: String) = Unit

        override suspend fun cancelAllReminders() {
            operations.add(CANCEL_ALL_OPERATION)
            cancelAllCount++
        }
    }

    private companion object {
        const val CANCEL_ALL_OPERATION = "cancelAll"
        const val SCHEDULE_OPERATION = "schedule"
    }
}
