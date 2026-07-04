package io.github.sandroisu.threetimesaday.feature.today.data

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecord
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecordRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class PersistentMedicationIntakeRecordRepository(
    private val keyValueStorage: KeyValueStorage,
    private val json: Json
) : MedicationIntakeRecordRepository {

    private val mutex = Mutex()

    override suspend fun getRecordsForDate(date: LocalDate): List<MedicationIntakeRecord> = mutex.withLock {
        readRecords().filter { record -> record.scheduledDateTime.date == date }
    }

    override suspend fun saveRecord(record: MedicationIntakeRecord) {
        mutex.withLock {
            val records = readRecords().toMutableList()
            val existingIndex = records.indexOfFirst { storedRecord -> storedRecord.eventId == record.eventId }
            if (existingIndex >= 0) {
                records[existingIndex] = record
            } else {
                records.add(record)
            }
            writeRecords(records)
        }
    }

    override suspend fun deleteRecord(eventId: String) {
        mutex.withLock {
            val records = readRecords().toMutableList()
            records.removeAll { storedRecord -> storedRecord.eventId == eventId }
            writeRecords(records)
        }
    }

    private fun readRecords(): List<MedicationIntakeRecord> {
        val storedRecords = keyValueStorage.getString(RECORDS_KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<List<MedicationIntakeRecord>>(storedRecords) }
            .getOrElse { emptyList() }
    }

    private fun writeRecords(records: List<MedicationIntakeRecord>) {
        keyValueStorage.putString(RECORDS_KEY, json.encodeToString(records))
    }

    private companion object {
        const val RECORDS_KEY = "medication_intake_records"
    }
}
