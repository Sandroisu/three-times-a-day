package io.github.sandroisu.threetimesaday.feature.medication.domain

interface MedicationRepository {

    suspend fun getMedications(): List<Medication>

    suspend fun saveMedication(medication: Medication)

    suspend fun updateMedication(medication: Medication)

    suspend fun deleteMedication(medicationId: String)

    /** Returns true only when a dose was really removed: the stock is tracked and not empty. */
    suspend fun takeDose(medicationId: String): Boolean

    suspend fun returnDose(medicationId: String)
}
