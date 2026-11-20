package io.github.sandroisu.threetimesaday.core.storage

import io.github.sandroisu.threetimesaday.core.di.commonAppModule
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderLaunchData
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRecurrence
import io.github.sandroisu.threetimesaday.feature.reminder.domain.Reminder
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderAlertMode
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRecurrence
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderWeekday
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecord
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class StorageJsonTest {

    private val json: Json = storageJson

    private val medication = Medication(
        id = "aspirin",
        name = "Aspirin",
        dosageText = "1 tablet",
        intakeRule = MedicationIntakeRule.SeveralTimesPerDay(
            listOf(MedicationIntakeMoment.AfterBreakfast, MedicationIntakeMoment.BeforeSleep),
        ),
        courseStartDate = LocalDate(2026, 7, 4),
        courseEndDate = null,
    )
    private val storedMedication =
        """{"id":"aspirin","name":"Aspirin","dosageText":"1 tablet",""" +
            """"intakeRule":{"type":"io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule.SeveralTimesPerDay","moments":["AfterBreakfast","BeforeSleep"]},""" +
            """"courseStartDate":"2026-07-04","courseEndDate":null,""" +
            """"recurrence":{"type":"io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRecurrence.Daily"},"dosesLeft":null}"""

    private val reminder = Reminder(
        id = "gym",
        title = "Gym",
        date = LocalDate(2026, 3, 2),
        time = LocalTime(18, 45),
        recurrence = ReminderRecurrence.OnWeekdays(listOf(ReminderWeekday.Monday, ReminderWeekday.Thursday)),
        isEnabled = false,
    )
    private val storedReminder =
        """{"id":"gym","title":"Gym","date":"2026-03-02","time":"18:45",""" +
            """"recurrence":{"type":"io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRecurrence.OnWeekdays","weekdays":["Monday","Thursday"]},""" +
            """"alertMode":{"type":"io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderAlertMode.Notification"},"isEnabled":false}"""

    private val intakeRecord = MedicationIntakeRecord(
        eventId = "aspirin|2026-07-04|09:30|AfterBreakfast",
        medicationId = "aspirin",
        scheduledDateTime = LocalDateTime(2026, 7, 4, 9, 30),
        status = MedicationIntakeStatus.Taken,
        updatedDateTime = LocalDateTime(2026, 7, 4, 9, 41),
    )
    private val storedIntakeRecord =
        """{"eventId":"aspirin|2026-07-04|09:30|AfterBreakfast","medicationId":"aspirin",""" +
            """"scheduledDateTime":"2026-07-04T09:30","status":"Taken",""" +
            """"updatedDateTime":"2026-07-04T09:41","postponedDateTime":null,"stockDoseTaken":false}"""

    private val dailySchedule = DailySchedule(
        wakeUpTime = LocalTime(7, 0),
        breakfastTime = LocalTime(7, 30),
        lunchTime = LocalTime(13, 0),
        dinnerTime = LocalTime(19, 15),
        sleepTime = LocalTime(23, 0),
    )
    private val storedDailySchedule =
        """{"wakeUpTime":"07:00","breakfastTime":"07:30","lunchTime":"13:00",""" +
            """"dinnerTime":"19:15","sleepTime":"23:00"}"""

    private val launchData = MedicationReminderLaunchData(
        notificationId = "medication-reminder|event-123",
        eventId = "event-123",
    )
    private val storedLaunchData = """{"notificationId":"medication-reminder|event-123","eventId":"event-123"}"""

    @Test
    fun dependencyInjectionProvidesStorageJson() {
        val injectedJson: Json = koinApplication { modules(commonAppModule) }.koin.get()

        assertSame(storageJson, injectedJson)
    }

    @Test
    fun encodesMedicationInStoredFormat() {
        assertEquals(storedMedication, json.encodeToString(medication))
    }

    @Test
    fun decodesStoredMedication() {
        assertEquals(medication, json.decodeFromString<Medication>(storedMedication))
    }

    @Test
    fun decodesStoredMedicationWithUnknownKeyAndMissingDefault() {
        val stored = """{"id":"aspirin","name":"Aspirin","dosageText":"1 tablet",""" +
            """"intakeRule":{"type":"io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule.AtExactTime","time":"09:30:00"},""" +
            """"courseStartDate":"2026-07-04","courseEndDate":"2026-08-01","removedField":true}"""

        val decoded = json.decodeFromString<Medication>(stored)

        assertEquals(MedicationIntakeRule.AtExactTime(LocalTime(9, 30)), decoded.intakeRule)
        assertEquals(LocalDate(2026, 8, 1), decoded.courseEndDate)
        assertEquals(MedicationRecurrence.Daily, decoded.recurrence)
        assertNull(decoded.dosesLeft)
    }

    @Test
    fun encodesReminderInStoredFormat() {
        assertEquals(storedReminder, json.encodeToString(reminder))
    }

    @Test
    fun decodesStoredReminder() {
        assertEquals(reminder, json.decodeFromString<Reminder>(storedReminder))
    }

    @Test
    fun encodesIntakeRecordInStoredFormat() {
        assertEquals(storedIntakeRecord, json.encodeToString(intakeRecord))
    }

    @Test
    fun decodesStoredIntakeRecord() {
        assertEquals(intakeRecord, json.decodeFromString<MedicationIntakeRecord>(storedIntakeRecord))
    }

    @Test
    fun encodesDailyScheduleInStoredFormat() {
        assertEquals(storedDailySchedule, json.encodeToString(dailySchedule))
    }

    @Test
    fun decodesStoredDailySchedule() {
        assertEquals(dailySchedule, json.decodeFromString<DailySchedule>(storedDailySchedule))
    }

    @Test
    fun encodesLaunchDataInStoredFormat() {
        assertEquals(storedLaunchData, json.encodeToString(launchData))
    }

    @Test
    fun decodesStoredLaunchData() {
        assertEquals(launchData, json.decodeFromString<MedicationReminderLaunchData>(storedLaunchData))
    }

    @Test
    fun readsLaunchDataWrittenWithFormerLaunchWriterConfig() {
        val formerLaunchWriterJson = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val writtenBefore = formerLaunchWriterJson.encodeToString(launchData)

        assertEquals(writtenBefore, json.encodeToString(launchData))
        assertEquals(launchData, json.decodeFromString<MedicationReminderLaunchData>(writtenBefore))
    }

    @Test
    fun keepsReminderRegistryFormat() {
        val ids = listOf("medication-reminder|event-1", "reminder|gym")
        val storedIds = """["medication-reminder|event-1","reminder|gym"]"""

        assertEquals(storedIds, json.encodeToString(ids))
        assertEquals(ids, json.decodeFromString<List<String>>(storedIds))
    }
}
