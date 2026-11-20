package io.github.sandroisu.threetimesaday.feature.medication.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MedicationStockTest {

    @Test
    fun untrackedStockIsNeverLow() {
        assertFalse(isMedicationStockLow(onceDaily(dosesLeft = null)))
    }

    @Test
    fun onceDailyStockForExactlyThreeDaysIsNotLow() {
        assertFalse(isMedicationStockLow(onceDaily(dosesLeft = 3)))
    }

    @Test
    fun onceDailyStockForTwoDaysIsLow() {
        assertTrue(isMedicationStockLow(onceDaily(dosesLeft = 2)))
    }

    @Test
    fun emptyStockIsLow() {
        assertTrue(isMedicationStockLow(onceDaily(dosesLeft = 0)))
    }

    @Test
    fun exactTimeRuleUsesOneDosePerDay() {
        val medication = onceDaily(dosesLeft = 3).copy(intakeRule = MedicationIntakeRule.AtExactTime(LocalTime(9, 0)))

        assertFalse(isMedicationStockLow(medication))
        assertTrue(isMedicationStockLow(medication.copy(dosesLeft = 2)))
    }

    @Test
    fun severalTimesPerDayNeedsDosesForEveryIntakeOfThreeDays() {
        val threeTimesDaily = onceDaily(dosesLeft = 9).copy(
            intakeRule = MedicationIntakeRule.SeveralTimesPerDay(
                listOf(
                    MedicationIntakeMoment.AfterBreakfast,
                    MedicationIntakeMoment.AfterLunch,
                    MedicationIntakeMoment.AfterDinner,
                ),
            ),
        )

        assertFalse(isMedicationStockLow(threeTimesDaily))
        assertTrue(isMedicationStockLow(threeTimesDaily.copy(dosesLeft = 8)))
    }

    @Test
    fun monthlyStockIsLowOnlyWhenNextIntakeIsNotCovered() {
        val monthly = onceDaily(dosesLeft = 1).copy(
            recurrence = MedicationRecurrence.EveryMonthsOnDay(intervalMonths = 3, dayOfMonth = 10),
        )

        assertFalse(isMedicationStockLow(monthly))
        assertTrue(isMedicationStockLow(monthly.copy(dosesLeft = 0)))
    }

    private fun onceDaily(dosesLeft: Int?): Medication = Medication(
        id = "aspirin",
        name = "Aspirin",
        dosageText = "1 tablet",
        intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp),
        courseStartDate = LocalDate(2026, 7, 4),
        courseEndDate = null,
        dosesLeft = dosesLeft,
    )
}
