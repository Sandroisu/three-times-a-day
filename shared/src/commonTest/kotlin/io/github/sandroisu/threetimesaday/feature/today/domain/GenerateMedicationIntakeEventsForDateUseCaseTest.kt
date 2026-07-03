package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateMedicationIntakeEventsForDateUseCaseTest {

    private val generateMedicationIntakeEventsForDate = GenerateMedicationIntakeEventsForDateUseCase()
    private val testDate = LocalDate(2026, 7, 3)

    @Test
    fun afterWakeUpProducesEventAtWakeUpTime() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp)
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        assertEquals(1, events.size)
        assertEquals(LocalTime(8, 0), events.single().scheduledDateTime.time)
        assertEquals(testDate, events.single().scheduledDateTime.date)
    }

    @Test
    fun beforeSleepProducesEventFifteenMinutesBeforeSleep() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep)
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        assertEquals(LocalTime(23, 15), events.single().scheduledDateTime.time)
    }

    @Test
    fun afterBreakfastProducesEventFifteenMinutesAfterBreakfast() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterBreakfast)
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        assertEquals(LocalTime(8, 45), events.single().scheduledDateTime.time)
    }

    @Test
    fun beforeLunchProducesEventFifteenMinutesBeforeLunch() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeLunch)
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        assertEquals(LocalTime(13, 15), events.single().scheduledDateTime.time)
    }

    @Test
    fun afterDinnerProducesEventFifteenMinutesAfterDinner() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterDinner)
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        assertEquals(LocalTime(19, 15), events.single().scheduledDateTime.time)
    }

    @Test
    fun exactTimeProducesEventAtGivenTime() {
        val exactTime = LocalTime(9, 20)
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtExactTime(exactTime)
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        assertEquals(exactTime, events.single().scheduledDateTime.time)
    }

    @Test
    fun eventsAreSortedByTime() {
        val morningMedication = createMedication(
            medicationId = "morning",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp)
        )
        val nightMedication = createMedication(
            medicationId = "night",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep)
        )
        val noonMedication = createMedication(
            medicationId = "noon",
            intakeRule = MedicationIntakeRule.AtExactTime(LocalTime(12, 0))
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(nightMedication, noonMedication, morningMedication)
        )

        val actualTimes = events.map { event -> event.scheduledDateTime.time }
        assertEquals(listOf(LocalTime(8, 0), LocalTime(12, 0), LocalTime(23, 15)), actualTimes)
    }

    @Test
    fun medicationBeforeCourseStartIsExcluded() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp),
            courseStartDate = LocalDate(2026, 7, 10),
            courseEndDate = null
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        assertTrue(events.isEmpty())
    }

    @Test
    fun medicationAfterCourseEndIsExcluded() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp),
            courseStartDate = LocalDate(2026, 1, 1),
            courseEndDate = LocalDate(2026, 7, 1)
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        assertTrue(events.isEmpty())
    }

    @Test
    fun medicationWithoutCourseEndIsActiveAfterCourseStart() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp),
            courseStartDate = LocalDate(2026, 1, 1),
            courseEndDate = null
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        assertEquals(1, events.size)
    }

    @Test
    fun severalTimesPerDayWithSingleOccurrenceUsesWakeUpTime() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.SeveralTimesPerDay(
                listOf(MedicationIntakeMoment.AfterWakeUp)
            )
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        assertEquals(1, events.size)
        assertEquals(LocalTime(8, 0), events.single().scheduledDateTime.time)
    }

    @Test
    fun severalTimesPerDayWithThreeOccurrencesDistributesEvenly() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.SeveralTimesPerDay(
                listOf(
                    MedicationIntakeMoment.AfterWakeUp,
                    MedicationIntakeMoment.AfterLunch,
                    MedicationIntakeMoment.BeforeSleep
                )
            )
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(),
            medications = listOf(medication)
        )

        val actualTimes = events.map { event -> event.scheduledDateTime.time }
        assertEquals(listOf(LocalTime(8, 0), LocalTime(15, 45), LocalTime(23, 30)), actualTimes)
    }

    @Test
    fun beforeBreakfastDoesNotUnderflowWhenBreakfastIsTooEarly() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeBreakfast)
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(breakfastTime = LocalTime(0, 5)),
            medications = listOf(medication)
        )

        assertEquals(LocalTime(0, 0), events.single().scheduledDateTime.time)
    }

    @Test
    fun afterDinnerDoesNotOverflowWhenDinnerIsTooLate() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterDinner)
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(dinnerTime = LocalTime(23, 59)),
            medications = listOf(medication)
        )

        assertEquals(LocalTime(23, 59, 59), events.single().scheduledDateTime.time)
    }

    @Test
    fun beforeSleepDoesNotUnderflowWhenSleepIsTooEarly() {
        val medication = createMedication(
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep)
        )

        val events = generateMedicationIntakeEventsForDate(
            date = testDate,
            dailySchedule = createDailySchedule(sleepTime = LocalTime(0, 10)),
            medications = listOf(medication)
        )

        assertEquals(LocalTime(0, 0), events.single().scheduledDateTime.time)
    }

    private fun createDailySchedule(
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
        medicationId: String = "medication",
        medicationName: String = "Тестовый препарат",
        dosageText: String = "1 таблетка",
        intakeRule: MedicationIntakeRule,
        courseStartDate: LocalDate = LocalDate(2020, 1, 1),
        courseEndDate: LocalDate? = null
    ): Medication = Medication(
        id = medicationId,
        name = medicationName,
        dosageText = dosageText,
        intakeRule = intakeRule,
        courseStartDate = courseStartDate,
        courseEndDate = courseEndDate
    )
}
