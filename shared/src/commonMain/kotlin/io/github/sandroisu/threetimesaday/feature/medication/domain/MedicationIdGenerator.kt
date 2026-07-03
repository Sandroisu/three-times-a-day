package io.github.sandroisu.threetimesaday.feature.medication.domain

interface MedicationIdGenerator {

    fun nextId(): String
}
