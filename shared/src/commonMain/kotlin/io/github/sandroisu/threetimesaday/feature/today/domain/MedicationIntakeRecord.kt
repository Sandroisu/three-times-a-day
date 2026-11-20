package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class MedicationIntakeRecord(
    val eventId: String,
    val medicationId: String,
    val scheduledDateTime: LocalDateTime,
    val status: MedicationIntakeStatus,
    val updatedDateTime: LocalDateTime,
    val postponedDateTime: LocalDateTime? = null,
    // True when this intake took a dose from the stock. Only then undo gives the dose back.
    val stockDoseTaken: Boolean = false,
)
