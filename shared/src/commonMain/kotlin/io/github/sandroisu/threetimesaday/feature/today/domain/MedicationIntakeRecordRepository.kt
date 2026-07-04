package io.github.sandroisu.threetimesaday.feature.today.domain

import kotlinx.datetime.LocalDate

interface MedicationIntakeRecordRepository {

    suspend fun getRecordsForDate(date: LocalDate): List<MedicationIntakeRecord>

    suspend fun saveRecord(record: MedicationIntakeRecord)

    suspend fun deleteRecord(eventId: String)
}
