package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

class GenerateMedicationIntakeEventsForDateUseCase {

    operator fun invoke(
        date: LocalDate,
        dailySchedule: DailySchedule,
        medications: List<Medication>
    ): List<MedicationIntakeEvent> {
        val generatedEvents = medications
            .filter { medication -> isActiveOnDate(medication, date) }
            .flatMap { medication -> eventsForMedication(date, dailySchedule, medication) }
        return generatedEvents.sortedBy { event -> event.scheduledDateTime }
    }

    private fun isActiveOnDate(medication: Medication, date: LocalDate): Boolean {
        val startedInTime = date >= medication.courseStartDate
        val endDate = medication.courseEndDate
        val notFinished = endDate == null || date <= endDate
        return startedInTime && notFinished
    }

    private fun eventsForMedication(
        date: LocalDate,
        dailySchedule: DailySchedule,
        medication: Medication
    ): List<MedicationIntakeEvent> = when (val intakeRule = medication.intakeRule) {
        is MedicationIntakeRule.AtMoment -> listOf(
            buildEvent(
                date = date,
                time = timeForMoment(intakeRule.moment, dailySchedule),
                medication = medication,
                intakeMoment = intakeRule.moment
            )
        )

        is MedicationIntakeRule.AtExactTime -> listOf(
            buildEvent(
                date = date,
                time = intakeRule.time,
                medication = medication,
                intakeMoment = null
            )
        )

        is MedicationIntakeRule.SeveralTimesPerDay -> distributeEvenly(
            occurrenceCount = intakeRule.moments.size,
            wakeUpTime = dailySchedule.wakeUpTime,
            sleepTime = dailySchedule.sleepTime
        ).map { time ->
            buildEvent(
                date = date,
                time = time,
                medication = medication,
                intakeMoment = null
            )
        }
    }

    private fun timeForMoment(moment: MedicationIntakeMoment, dailySchedule: DailySchedule): LocalTime =
        when (moment) {
            MedicationIntakeMoment.AfterWakeUp -> dailySchedule.wakeUpTime
            MedicationIntakeMoment.BeforeBreakfast -> shiftByMinutes(dailySchedule.breakfastTime, -MEAL_OFFSET_MINUTES)
            MedicationIntakeMoment.AfterBreakfast -> shiftByMinutes(dailySchedule.breakfastTime, MEAL_OFFSET_MINUTES)
            MedicationIntakeMoment.BeforeLunch -> shiftByMinutes(dailySchedule.lunchTime, -MEAL_OFFSET_MINUTES)
            MedicationIntakeMoment.AfterLunch -> shiftByMinutes(dailySchedule.lunchTime, MEAL_OFFSET_MINUTES)
            MedicationIntakeMoment.BeforeDinner -> shiftByMinutes(dailySchedule.dinnerTime, -MEAL_OFFSET_MINUTES)
            MedicationIntakeMoment.AfterDinner -> shiftByMinutes(dailySchedule.dinnerTime, MEAL_OFFSET_MINUTES)
            MedicationIntakeMoment.BeforeSleep -> shiftByMinutes(dailySchedule.sleepTime, -BEFORE_SLEEP_OFFSET_MINUTES)
        }

    private fun distributeEvenly(
        occurrenceCount: Int,
        wakeUpTime: LocalTime,
        sleepTime: LocalTime
    ): List<LocalTime> {
        if (occurrenceCount <= 0) {
            return emptyList()
        }
        val wakeUpSecond = wakeUpTime.toSecondOfDay()
        if (occurrenceCount == 1) {
            return listOf(LocalTime.fromSecondOfDay(wakeUpSecond))
        }
        val spanSeconds = sleepTime.toSecondOfDay() - wakeUpSecond
        return (0 until occurrenceCount).map { occurrenceIndex ->
            val secondOfDay = wakeUpSecond + spanSeconds * occurrenceIndex / (occurrenceCount - 1)
            LocalTime.fromSecondOfDay(secondOfDay)
        }
    }

    private fun buildEvent(
        date: LocalDate,
        time: LocalTime,
        medication: Medication,
        intakeMoment: MedicationIntakeMoment?
    ): MedicationIntakeEvent = MedicationIntakeEvent(
        medicationId = medication.id,
        medicationName = medication.name,
        dosageText = medication.dosageText,
        scheduledDateTime = LocalDateTime(date, time),
        status = MedicationIntakeStatus.Scheduled,
        intakeMoment = intakeMoment
    )

    private fun shiftByMinutes(time: LocalTime, deltaMinutes: Int): LocalTime {
        val shiftedSecond = (time.toSecondOfDay() + deltaMinutes * SECONDS_IN_MINUTE)
            .coerceIn(0, SECONDS_IN_DAY - 1)
        return LocalTime.fromSecondOfDay(shiftedSecond)
    }

    private companion object {
        const val MEAL_OFFSET_MINUTES = 15
        const val BEFORE_SLEEP_OFFSET_MINUTES = 15
        const val SECONDS_IN_MINUTE = 60
        const val SECONDS_IN_DAY = 24 * 60 * 60
    }
}
