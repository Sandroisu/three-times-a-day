package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuildMedicationIntakeRecordUseCaseTest {

    private val eventDateTime = LocalDateTime(2026, 7, 3, 8, 45)
    private val currentDateTime = LocalDateTime(2026, 7, 3, 8, 41)
    private val buildRecord = BuildMedicationIntakeRecordUseCase(FixedTimeProvider(currentDateTime))

    @Test
    fun takenRecordKeepsEventDataAndCurrentTime() {
        val record = buildRecord(event(eventDateTime), MedicationIntakeStatus.Taken)

        assertEquals(EVENT_ID, record.eventId)
        assertEquals(MEDICATION_ID, record.medicationId)
        assertEquals(eventDateTime, record.scheduledDateTime)
        assertEquals(MedicationIntakeStatus.Taken, record.status)
        assertEquals(currentDateTime, record.updatedDateTime)
        assertNull(record.postponedDateTime)
    }

    @Test
    fun postponedRecordMovesTimeByPostponeInterval() {
        val record = buildRecord(event(eventDateTime), MedicationIntakeStatus.Postponed)

        assertEquals(MedicationIntakeStatus.Postponed, record.status)
        assertEquals(LocalDateTime(2026, 7, 3, 8, 55), record.postponedDateTime)
    }

    @Test
    fun postponingOverdueIntakeCountsFromCurrentTime() {
        val buildLateRecord = BuildMedicationIntakeRecordUseCase(FixedTimeProvider(LocalDateTime(2026, 7, 3, 9, 30)))

        val record = buildLateRecord(event(eventDateTime), MedicationIntakeStatus.Postponed)

        assertEquals(LocalDateTime(2026, 7, 3, 9, 40), record.postponedDateTime)
    }

    @Test
    fun postponeNearMidnightMovesToNextDay() {
        val lateEvent = event(LocalDateTime(2026, 7, 3, 23, 55))
        val buildLateRecord = BuildMedicationIntakeRecordUseCase(FixedTimeProvider(LocalDateTime(2026, 7, 3, 23, 50)))

        val record = buildLateRecord(lateEvent, MedicationIntakeStatus.Postponed)

        assertEquals(LocalDateTime(LocalDate(2026, 7, 4), LocalTime(0, 5)), record.postponedDateTime)
    }

    private fun event(scheduledDateTime: LocalDateTime): MedicationIntakeEvent = MedicationIntakeEvent(
        eventId = EVENT_ID,
        medicationId = MEDICATION_ID,
        medicationName = "Vitamin D",
        dosageText = "1 tablet",
        scheduledDateTime = scheduledDateTime,
        status = MedicationIntakeStatus.Scheduled,
    )

    private class FixedTimeProvider(private val dateTime: LocalDateTime) : TimeProvider {
        override fun currentDate(): LocalDate = dateTime.date
        override fun currentDateTime(): LocalDateTime = dateTime
    }

    private companion object {
        const val EVENT_ID = "vitamin|2026-07-03|08:45|ExactTime"
        const val MEDICATION_ID = "vitamin"
    }
}
