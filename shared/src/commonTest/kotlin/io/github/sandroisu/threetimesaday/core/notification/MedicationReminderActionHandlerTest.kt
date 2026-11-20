package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.logging.ErrorLogger
import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.storageJson
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.data.PersistentMedicationRepository
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.today.data.PersistentMedicationIntakeRecordRepository
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationReminderAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MedicationReminderActionHandlerTest {

    private val storage = InMemoryKeyValueStorage()
    private val medicationRepository = PersistentMedicationRepository(storage, storageJson)
    private val recordRepository = PersistentMedicationIntakeRecordRepository(storage, storageJson)
    private val scheduler = ScriptedScheduler()
    private val errorLogger = RecordingErrorLogger()
    private val handler = createMedicationReminderActionHandler(
        keyValueStorage = storage,
        medicationReminderScheduler = scheduler,
        errorLogger = errorLogger,
        timeProvider = FixedTimeProvider(LocalDateTime(2026, 7, 4, 9, 3)),
        buildReminderMessage = { event -> event.dosageText },
    )

    @Test
    fun everyActionIsFoundByItsId() {
        MedicationReminderAction.entries.forEach { action ->
            assertEquals(action, medicationReminderActionFromId(medicationReminderActionId(action)))
        }
    }

    @Test
    fun actionIdsDifferFromEachOther() {
        val actionIds = MedicationReminderAction.entries.map(::medicationReminderActionId)

        assertEquals(actionIds.size, actionIds.toSet().size)
        assertTrue(actionIds.none { actionId -> actionId.isBlank() })
    }

    @Test
    fun plainNotificationTapIsNotAnAction() {
        assertNull(medicationReminderActionFromId("com.apple.UNNotificationDefaultActionIdentifier"))
    }

    @Test
    fun medicationReminderOffersTakenAndPostpone() {
        assertEquals(
            listOf(MedicationReminderAction.Taken, MedicationReminderAction.Postpone),
            notificationActions(NOTIFICATION_ID),
        )
    }

    @Test
    fun personalReminderOffersNoActions() {
        assertTrue(notificationActions("${REMINDER_NOTIFICATION_ID_PREFIX}7|2026-07-04T09:00").isEmpty())
    }

    @Test
    fun takenActionFromNotificationMarksIntakeTaken() = runTest {
        medicationRepository.saveMedication(medication())

        handler.handle(NOTIFICATION_ID, MedicationReminderAction.Taken)

        assertEquals(MedicationIntakeStatus.Taken, recordRepository.getRecord(EVENT_ID)?.status)
        assertEquals(4, medicationRepository.getMedications().single().dosesLeft)
        assertTrue(errorLogger.messages.isEmpty())
    }

    @Test
    fun postponeActionFromNotificationSchedulesReminderWithDosageText() = runTest {
        medicationRepository.saveMedication(medication())

        handler.handle(NOTIFICATION_ID, MedicationReminderAction.Postpone)

        val postponedNotification = scheduler.scheduledNotifications.first()
        assertEquals(NOTIFICATION_ID, postponedNotification.notificationId)
        assertEquals(LocalDateTime(2026, 7, 4, 9, 13), postponedNotification.scheduledDateTime)
        assertEquals("2 tablets", postponedNotification.message)
    }

    @Test
    fun actionOnPersonalReminderIsIgnoredAndLogged() = runTest {
        medicationRepository.saveMedication(medication())

        handler.handle("${REMINDER_NOTIFICATION_ID_PREFIX}7|2026-07-04T09:00", MedicationReminderAction.Taken)

        assertNull(recordRepository.getRecord(EVENT_ID))
        assertEquals(1, errorLogger.messages.size)
    }

    @Test
    fun actionOnMissingIntakeIsLoggedWithEventId() = runTest {
        handler.handle(NOTIFICATION_ID, MedicationReminderAction.Taken)

        assertNull(recordRepository.getRecord(EVENT_ID))
        assertTrue(errorLogger.messages.single().contains(EVENT_ID))
    }

    @Test
    fun actionOnTakenIntakeIsNotLogged() = runTest {
        medicationRepository.saveMedication(medication())
        handler.handle(NOTIFICATION_ID, MedicationReminderAction.Taken)

        handler.handle(NOTIFICATION_ID, MedicationReminderAction.Taken)

        assertTrue(errorLogger.messages.isEmpty())
        assertEquals(4, medicationRepository.getMedications().single().dosesLeft)
    }

    @Test
    fun schedulerFailureIsLoggedAndDoesNotCrash() = runTest {
        medicationRepository.saveMedication(medication())
        scheduler.scheduleError = IllegalStateException("Scheduler is not available")

        handler.handle(NOTIFICATION_ID, MedicationReminderAction.Postpone)

        assertEquals(1, errorLogger.messages.size)
        assertEquals(MedicationIntakeStatus.Postponed, recordRepository.getRecord(EVENT_ID)?.status)
    }

    @Test
    fun cancellationIsNotSwallowed() = runTest {
        medicationRepository.saveMedication(medication())
        scheduler.scheduleError = CancellationException("Receiver time is over")

        assertFailsWith<CancellationException> {
            handler.handle(NOTIFICATION_ID, MedicationReminderAction.Postpone)
        }
        assertTrue(errorLogger.messages.isEmpty())
    }

    private fun medication(): Medication = Medication(
        id = MEDICATION_ID,
        name = "Ibuprofen",
        dosageText = "2 tablets",
        intakeRule = MedicationIntakeRule.AtExactTime(LocalTime(9, 0)),
        courseStartDate = LocalDate(2026, 6, 1),
        courseEndDate = null,
        dosesLeft = 5,
    )

    private class FixedTimeProvider(private val dateTime: LocalDateTime) : TimeProvider {
        override fun currentDate(): LocalDate = dateTime.date
        override fun currentDateTime(): LocalDateTime = dateTime
    }

    private class RecordingErrorLogger : ErrorLogger {
        val messages: MutableList<String> = mutableListOf()

        override fun logError(tag: String, message: String, error: Throwable) {
            messages.add(message)
        }
    }

    private class ScriptedScheduler : MedicationReminderScheduler {
        val scheduledNotifications: MutableList<MedicationReminderNotification> = mutableListOf()
        var scheduleError: Throwable? = null

        override suspend fun getPermissionStatus(): NotificationPermissionStatus = NotificationPermissionStatus.Granted

        override suspend fun requestPermission(): NotificationPermissionStatus = NotificationPermissionStatus.Granted

        override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
            scheduleError?.let { error -> throw error }
            scheduledNotifications.add(notification)
        }

        override suspend fun cancelReminder(notificationId: String) = Unit

        override suspend fun cancelAllReminders() = Unit

        override suspend fun cancelRemindersWithPrefix(notificationIdPrefix: String) = Unit
    }

    private companion object {
        const val MEDICATION_ID = "ibuprofen"
        const val EVENT_ID = "ibuprofen|2026-07-04|09:00|ExactTime"
        const val NOTIFICATION_ID = MEDICATION_REMINDER_ID_PREFIX + EVENT_ID
    }
}
