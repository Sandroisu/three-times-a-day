package io.github.sandroisu.threetimesaday.feature.medication.data

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIdGenerator

class IncrementingMedicationIdGenerator : MedicationIdGenerator {

    private var lastAssignedNumber = 0L

    override fun nextId(): String {
        lastAssignedNumber += 1
        return "medication-$lastAssignedNumber"
    }
}
