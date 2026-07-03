package io.github.sandroisu.threetimesaday.feature.medication.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Medication(
    val id: String,
    val name: String,
    val dosageText: String,
    val intakeRule: MedicationIntakeRule,
    val courseStartDate: LocalDate,
    val courseEndDate: LocalDate?
)
