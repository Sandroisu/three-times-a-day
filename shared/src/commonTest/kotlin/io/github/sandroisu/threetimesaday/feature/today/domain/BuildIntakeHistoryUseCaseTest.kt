package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.storageJson
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.data.PersistentMedicationRepository
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRecurrence
import io.github.sandroisu.threetimesaday.feature.schedule.data.PersistentDailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.today.data.PersistentMedicationIntakeRecordRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BuildIntakeHistoryUseCaseTest {

    private val storage = InMemoryKeyValueStorage()
    private val scheduleRepository = PersistentDailyScheduleRepository(storage, storageJson)
    private val medicationRepository = PersistentMedicationRepository(storage, storageJson)
    private val recordRepository = PersistentMedicationIntakeRecordRepository(storage, storageJson)
    private val generateEvents = GenerateMedicationIntakeEventsForDateUseCase()
    private val buildIntakeHistory = BuildIntakeHistoryUseCase(
        timeProvider = FixedTimeProvider(NOW),
        dailyScheduleRepository = scheduleRepository,
        medicationRepository = medicationRepository,
        medicationIntakeRecordRepository = recordRepository,
        generateMedicationIntakeEventsForDate = generateEvents,
        applyMedicationIntakeRecords = ApplyMedicationIntakeRecordsUseCase(),
    )

    @Test
    fun noMedicationsGivesNoMedicationsResult() = runTest {
        assertEquals(IntakeHistoryResult.NoMedications, buildIntakeHistory(IntakeHistoryPeriod.LastSevenDays))
    }

    @Test
    fun sevenDayPeriodEndsTodayAndStartsSixDaysAgo() = runTest {
        medicationRepository.saveMedication(medication("vitamin", LocalTime(9, 0)))

        val days = loaded(IntakeHistoryPeriod.LastSevenDays).days

        assertEquals(7, days.size)
        assertEquals(LocalDate(2026, 7, 4), days.first().date)
        assertEquals(TODAY, days.last().date)
    }

    @Test
    fun thirtyDayPeriodEndsTodayAndStartsTwentyNineDaysAgo() = runTest {
        medicationRepository.saveMedication(medication("vitamin", LocalTime(9, 0)))

        val days = loaded(IntakeHistoryPeriod.LastThirtyDays).days

        assertEquals(30, days.size)
        assertEquals(LocalDate(2026, 6, 11), days.first().date)
        assertEquals(TODAY, days.last().date)
    }

    @Test
    fun pastIntakesAreTakenSkippedOrMissedBySavedRecord() = runTest {
        val vitamin = medication("vitamin", LocalTime(9, 0), courseStartDate = LocalDate(2026, 7, 7))
        medicationRepository.saveMedication(vitamin)
        saveRecord(vitamin, LocalDate(2026, 7, 9), MedicationIntakeStatus.Taken)
        saveRecord(vitamin, LocalDate(2026, 7, 8), MedicationIntakeStatus.Skipped)

        val days = loaded(IntakeHistoryPeriod.LastSevenDays).days.associateBy { day -> day.date }

        assertEquals(IntakeAdherence(takenCount = 0, skippedCount = 0, missedCount = 1), days.getValue(LocalDate(2026, 7, 7)).adherence)
        assertEquals(IntakeAdherence(takenCount = 0, skippedCount = 1, missedCount = 0), days.getValue(LocalDate(2026, 7, 8)).adherence)
        assertEquals(IntakeAdherence(takenCount = 1, skippedCount = 0, missedCount = 0), days.getValue(LocalDate(2026, 7, 9)).adherence)
    }

    @Test
    fun intakePostponedAndThenTakenCountsAsTaken() = runTest {
        val vitamin = medication("vitamin", LocalTime(9, 0), courseStartDate = YESTERDAY)
        medicationRepository.saveMedication(vitamin)
        saveRecord(
            vitamin,
            YESTERDAY,
            MedicationIntakeStatus.Taken,
            postponedDateTime = LocalDateTime(YESTERDAY, LocalTime(9, 10)),
        )

        val yesterday = loaded(IntakeHistoryPeriod.LastSevenDays).days.single { day -> day.date == YESTERDAY }

        assertEquals(IntakeAdherence(takenCount = 1, skippedCount = 0, missedCount = 0), yesterday.adherence)
    }

    @Test
    fun intakePostponedAndNeverTakenCountsAsMissed() = runTest {
        val vitamin = medication("vitamin", LocalTime(9, 0), courseStartDate = YESTERDAY)
        medicationRepository.saveMedication(vitamin)
        saveRecord(
            vitamin,
            YESTERDAY,
            MedicationIntakeStatus.Postponed,
            postponedDateTime = LocalDateTime(YESTERDAY, LocalTime(9, 10)),
        )

        val yesterday = loaded(IntakeHistoryPeriod.LastSevenDays).days.single { day -> day.date == YESTERDAY }

        assertEquals(IntakeAdherence(takenCount = 0, skippedCount = 0, missedCount = 1), yesterday.adherence)
    }

    @Test
    fun todayIntakeThatIsNotDueYetIsPendingAndNotMissed() = runTest {
        medicationRepository.saveMedication(medication("morning", LocalTime(9, 0), courseStartDate = TODAY))
        medicationRepository.saveMedication(medication("evening", LocalTime(18, 0), courseStartDate = TODAY))

        val today = loaded(IntakeHistoryPeriod.LastSevenDays).days.last()

        assertEquals(IntakeAdherence(takenCount = 0, skippedCount = 0, missedCount = 1), today.adherence)
        assertEquals(1, today.pendingCount)
    }

    @Test
    fun todayIntakePostponedToLaterIsPending() = runTest {
        val vitamin = medication("vitamin", LocalTime(9, 0), courseStartDate = TODAY)
        medicationRepository.saveMedication(vitamin)
        saveRecord(
            vitamin,
            TODAY,
            MedicationIntakeStatus.Postponed,
            postponedDateTime = LocalDateTime(TODAY, LocalTime(12, 30)),
        )

        val today = loaded(IntakeHistoryPeriod.LastSevenDays).days.last()

        assertEquals(0, today.adherence.dueCount)
        assertEquals(1, today.pendingCount)
    }

    @Test
    fun daysOutsideTheCourseHaveNoIntakes() = runTest {
        medicationRepository.saveMedication(
            medication(
                "course",
                LocalTime(9, 0),
                courseStartDate = LocalDate(2026, 7, 6),
                courseEndDate = LocalDate(2026, 7, 7),
            )
        )

        val dueByDate = loaded(IntakeHistoryPeriod.LastSevenDays).days.associate { day -> day.date to day.adherence.dueCount }

        assertEquals(
            mapOf(
                LocalDate(2026, 7, 4) to 0,
                LocalDate(2026, 7, 5) to 0,
                LocalDate(2026, 7, 6) to 1,
                LocalDate(2026, 7, 7) to 1,
                LocalDate(2026, 7, 8) to 0,
                LocalDate(2026, 7, 9) to 0,
                TODAY to 0,
            ),
            dueByDate,
        )
    }

    @Test
    fun monthlyRecurrenceGivesOnlyItsDayInTheRange() = runTest {
        medicationRepository.saveMedication(
            medication(
                "monthly",
                LocalTime(9, 0),
                courseStartDate = LocalDate(2026, 1, 5),
                recurrence = MedicationRecurrence.EveryMonthsOnDay(intervalMonths = 1, dayOfMonth = 5),
            )
        )

        val daysWithIntakes = loaded(IntakeHistoryPeriod.LastThirtyDays).days
            .filter { day -> day.adherence.dueCount > 0 }
            .map { day -> day.date }

        assertEquals(listOf(LocalDate(2026, 7, 5)), daysWithIntakes)
    }

    @Test
    fun adherenceIsUnknownWhenNothingWasDue() = runTest {
        medicationRepository.saveMedication(medication("later", LocalTime(9, 0), courseStartDate = LocalDate(2026, 7, 11)))

        val history = loaded(IntakeHistoryPeriod.LastSevenDays)

        assertEquals(0, history.overall.dueCount)
        assertNull(history.overall.takenPercent)
        assertNull(history.medications.single().adherence.takenPercent)
    }

    @Test
    fun adherenceIsCountedOverallAndPerMedication() = runTest {
        val vitamin = medication("vitamin", LocalTime(9, 0), courseStartDate = YESTERDAY)
        val iron = medication("iron", LocalTime(10, 0), courseStartDate = YESTERDAY)
        medicationRepository.saveMedication(vitamin)
        medicationRepository.saveMedication(iron)
        saveRecord(vitamin, YESTERDAY, MedicationIntakeStatus.Taken)
        saveRecord(vitamin, TODAY, MedicationIntakeStatus.Taken)
        saveRecord(iron, YESTERDAY, MedicationIntakeStatus.Skipped)

        val history = loaded(IntakeHistoryPeriod.LastSevenDays)

        assertEquals(IntakeAdherence(takenCount = 2, skippedCount = 1, missedCount = 1), history.overall)
        assertEquals(50, history.overall.takenPercent)
        assertEquals(
            listOf(
                MedicationAdherence("vitamin", "Name vitamin", IntakeAdherence(2, 0, 0)),
                MedicationAdherence("iron", "Name iron", IntakeAdherence(0, 1, 1)),
            ),
            history.medications,
        )
        assertEquals(100, history.medications[0].adherence.takenPercent)
        assertEquals(0, history.medications[1].adherence.takenPercent)
    }

    @Test
    fun takenPercentIsRoundedDown() {
        assertEquals(66, IntakeAdherence(takenCount = 2, skippedCount = 0, missedCount = 1).takenPercent)
        assertEquals(99, IntakeAdherence(takenCount = 199, skippedCount = 1, missedCount = 0).takenPercent)
    }

    @Test
    fun recordsOutsideThePeriodAreIgnored() = runTest {
        val vitamin = medication("vitamin", LocalTime(9, 0), courseStartDate = LocalDate(2026, 7, 1))
        medicationRepository.saveMedication(vitamin)
        saveRecord(vitamin, LocalDate(2026, 7, 3), MedicationIntakeStatus.Taken)

        val history = loaded(IntakeHistoryPeriod.LastSevenDays)

        assertEquals(IntakeAdherence(takenCount = 0, skippedCount = 0, missedCount = 7), history.overall)
    }

    private suspend fun loaded(period: IntakeHistoryPeriod): IntakeHistoryResult.Loaded =
        assertIs<IntakeHistoryResult.Loaded>(buildIntakeHistory(period))

    private suspend fun saveRecord(
        medication: Medication,
        date: LocalDate,
        status: MedicationIntakeStatus,
        postponedDateTime: LocalDateTime? = null,
    ) {
        val event = generateEvents(date, scheduleRepository.getDailySchedule(), listOf(medication)).single()
        recordRepository.saveRecord(
            MedicationIntakeRecord(
                eventId = event.eventId,
                medicationId = medication.id,
                scheduledDateTime = event.scheduledDateTime,
                status = status,
                updatedDateTime = NOW,
                postponedDateTime = postponedDateTime,
            )
        )
    }

    private fun medication(
        id: String,
        time: LocalTime,
        courseStartDate: LocalDate = LocalDate(2026, 6, 1),
        courseEndDate: LocalDate? = null,
        recurrence: MedicationRecurrence = MedicationRecurrence.Daily,
    ): Medication = Medication(
        id = id,
        name = "Name $id",
        dosageText = "1 tablet",
        intakeRule = MedicationIntakeRule.AtExactTime(time),
        courseStartDate = courseStartDate,
        courseEndDate = courseEndDate,
        recurrence = recurrence,
    )

    private class FixedTimeProvider(private val dateTime: LocalDateTime) : TimeProvider {
        override fun currentDate(): LocalDate = dateTime.date
        override fun currentDateTime(): LocalDateTime = dateTime
    }

    private companion object {
        val TODAY = LocalDate(2026, 7, 10)
        val YESTERDAY = LocalDate(2026, 7, 9)
        val NOW = LocalDateTime(TODAY, LocalTime(12, 0))
    }
}
