package io.github.sandroisu.threetimesaday.feature.medication.domain

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
sealed interface MedicationIntakeRule {

    @Serializable
    data class AtMoment(
        val moment: MedicationIntakeMoment
    ) : MedicationIntakeRule

    @Serializable
    data class AtExactTime(
        val time: LocalTime
    ) : MedicationIntakeRule

    @Serializable
    data class SeveralTimesPerDay(
        val moments: List<MedicationIntakeMoment>
    ) : MedicationIntakeRule
}
