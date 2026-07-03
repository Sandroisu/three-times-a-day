package io.github.sandroisu.threetimesaday.feature.medication.domain

interface MedicationRepository {

    suspend fun getMedications(): List<Medication>

    suspend fun saveMedication(medication: Medication)

    suspend fun updateMedication(medication: Medication)

    suspend fun deleteMedication(medicationId: String)
}
