package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class MedicationIntakeEvent(
    val medicationId: String,
    val medicationName: String,
    val dosageText: String,
    val scheduledDateTime: LocalDateTime,
    val status: MedicationIntakeStatus,
    val intakeMoment: MedicationIntakeMoment? = null
)
