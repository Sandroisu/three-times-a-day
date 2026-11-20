package io.github.sandroisu.threetimesaday.feature.medication.domain

private const val LOW_STOCK_DAYS = 3

internal fun isMedicationStockLow(medication: Medication): Boolean {
    val dosesLeft = medication.dosesLeft ?: return false
    if (dosesLeft <= 0) return true
    val intakesPerDay = intakesPerDay(medication.intakeRule)
    // A monthly intake comes at most once in the warning window,
    // so the stock is low only when the next intake is not covered.
    val dosesNeeded = when (medication.recurrence) {
        MedicationRecurrence.Daily -> intakesPerDay * LOW_STOCK_DAYS
        is MedicationRecurrence.EveryMonthsOnDay -> intakesPerDay
    }
    return dosesLeft < dosesNeeded
}

private fun intakesPerDay(intakeRule: MedicationIntakeRule): Int = when (intakeRule) {
    is MedicationIntakeRule.AtMoment -> 1
    is MedicationIntakeRule.AtExactTime -> 1
    is MedicationIntakeRule.SeveralTimesPerDay -> intakeRule.moments.size
}
