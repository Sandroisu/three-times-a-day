package io.github.sandroisu.threetimesaday.feature.medication.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface MedicationRecurrence {

    @Serializable
    data object Daily : MedicationRecurrence

    @Serializable
    data class EveryMonthsOnDay(
        val intervalMonths: Int,
        val dayOfMonth: Int,
    ) : MedicationRecurrence
}
