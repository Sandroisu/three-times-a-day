package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRecurrence
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
        return startedInTime && notFinished && matchesRecurrence(medication, date)
    }

    private fun matchesRecurrence(medication: Medication, date: LocalDate): Boolean = when (val recurrence = medication.recurrence) {
        MedicationRecurrence.Daily -> true
        is MedicationRecurrence.EveryMonthsOnDay -> matchesMonthlyRecurrence(
            courseStartDate = medication.courseStartDate,
            date = date,
            intervalMonths = recurrence.intervalMonths,
            dayOfMonth = recurrence.dayOfMonth,
        )
    }

    private fun matchesMonthlyRecurrence(
        courseStartDate: LocalDate,
        date: LocalDate,
        intervalMonths: Int,
        dayOfMonth: Int,
    ): Boolean {
        if (intervalMonths <= 0 || dayOfMonth !in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH) {
            return false
        }
        val monthsFromStart = (date.year - courseStartDate.year) * MONTHS_IN_YEAR +
            date.month.ordinal - courseStartDate.month.ordinal
        if (monthsFromStart < 0 || monthsFromStart % intervalMonths != 0) {
            return false
        }
        return date.day == dayOfMonth.coerceAtMost(daysInMonth(date.year, date.month.ordinal + 1))
    }

    private fun daysInMonth(year: Int, monthNumber: Int): Int = when (monthNumber) {
        2 -> if (isLeapYear(year)) LEAP_FEBRUARY_DAYS else FEBRUARY_DAYS
        4, 6, 9, 11 -> THIRTY_DAY_MONTH_DAYS
        else -> THIRTY_ONE_DAY_MONTH_DAYS
    }

    private fun isLeapYear(year: Int): Boolean =
        year % LEAP_YEAR_DIVISOR == 0 && (year % CENTURY_DIVISOR != 0 || year % QUADRICENTENNIAL_DIVISOR == 0)

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
                intakeMoment = intakeRule.moment,
                ruleKey = intakeRule.moment.name
            )
        )

        is MedicationIntakeRule.AtExactTime -> listOf(
            buildEvent(
                date = date,
                time = intakeRule.time,
                medication = medication,
                intakeMoment = null,
                ruleKey = EXACT_TIME_RULE_KEY
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
                intakeMoment = null,
                ruleKey = SEVERAL_TIMES_RULE_KEY
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
        intakeMoment: MedicationIntakeMoment?,
        ruleKey: String
    ): MedicationIntakeEvent = MedicationIntakeEvent(
        eventId = buildEventId(medication.id, date, time, ruleKey),
        medicationId = medication.id,
        medicationName = medication.name,
        dosageText = medication.dosageText,
        scheduledDateTime = LocalDateTime(date, time),
        status = MedicationIntakeStatus.Scheduled,
        intakeMoment = intakeMoment
    )

    private fun buildEventId(
        medicationId: String,
        date: LocalDate,
        time: LocalTime,
        ruleKey: String
    ): String {
        val hour = time.hour.toString().padStart(2, '0')
        val minute = time.minute.toString().padStart(2, '0')
        return "$medicationId|$date|$hour:$minute|$ruleKey"
    }

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
        const val MONTHS_IN_YEAR = 12
        const val MIN_DAY_OF_MONTH = 1
        const val MAX_DAY_OF_MONTH = 31
        const val FEBRUARY_DAYS = 28
        const val LEAP_FEBRUARY_DAYS = 29
        const val THIRTY_DAY_MONTH_DAYS = 30
        const val THIRTY_ONE_DAY_MONTH_DAYS = 31
        const val LEAP_YEAR_DIVISOR = 4
        const val CENTURY_DIVISOR = 100
        const val QUADRICENTENNIAL_DIVISOR = 400
        const val EXACT_TIME_RULE_KEY = "ExactTime"
        const val SEVERAL_TIMES_RULE_KEY = "SeveralTimesPerDay"
    }
}
