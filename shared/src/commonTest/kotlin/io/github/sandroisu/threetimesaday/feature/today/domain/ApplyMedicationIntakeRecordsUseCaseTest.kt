package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplyMedicationIntakeRecordsUseCaseTest {

    private val applyMedicationIntakeRecords = ApplyMedicationIntakeRecordsUseCase()
    private val testDate = LocalDate(2026, 7, 4)

    @Test
    fun eventWithoutRecordStaysScheduled() {
        val event = createEvent("wake", LocalTime(8, 0))

        val mergedEvents = applyMedicationIntakeRecords(listOf(event), emptyList())

        assertEquals(MedicationIntakeStatus.Scheduled, mergedEvents.single().status)
    }

    @Test
    fun takenRecordChangesStatusToTaken() {
        val event = createEvent("wake", LocalTime(8, 0))
        val record = createRecord(event, MedicationIntakeStatus.Taken)

        val mergedEvents = applyMedicationIntakeRecords(listOf(event), listOf(record))

        assertEquals(MedicationIntakeStatus.Taken, mergedEvents.single().status)
        assertEquals(LocalTime(8, 0), mergedEvents.single().scheduledDateTime.time)
    }

    @Test
    fun skippedRecordChangesStatusToSkipped() {
        val event = createEvent("wake", LocalTime(8, 0))
        val record = createRecord(event, MedicationIntakeStatus.Skipped)

        val mergedEvents = applyMedicationIntakeRecords(listOf(event), listOf(record))

        assertEquals(MedicationIntakeStatus.Skipped, mergedEvents.single().status)
    }

    @Test
    fun postponedRecordChangesStatusAndMovesTime() {
        val event = createEvent("wake", LocalTime(8, 0))
        val record = createRecord(
            event = event,
            status = MedicationIntakeStatus.Postponed,
            postponedDateTime = LocalDateTime(testDate, LocalTime(8, 10))
        )

        val mergedEvents = applyMedicationIntakeRecords(listOf(event), listOf(record))

        assertEquals(MedicationIntakeStatus.Postponed, mergedEvents.single().status)
        assertEquals(LocalTime(8, 10), mergedEvents.single().scheduledDateTime.time)
    }

    @Test
    fun postponedEventIsReorderedByNewTime() {
        val morningEvent = createEvent("morning", LocalTime(8, 0))
        val noonEvent = createEvent("noon", LocalTime(12, 0))
        val postponedRecord = createRecord(
            event = morningEvent,
            status = MedicationIntakeStatus.Postponed,
            postponedDateTime = LocalDateTime(testDate, LocalTime(13, 0))
        )

        val mergedEvents = applyMedicationIntakeRecords(
            generatedEvents = listOf(morningEvent, noonEvent),
            records = listOf(postponedRecord)
        )

        assertEquals(listOf("noon", "morning"), mergedEvents.map { event -> event.medicationId })
    }

    @Test
    fun recordForUnknownEventIdIsIgnored() {
        val event = createEvent("wake", LocalTime(8, 0))
        val unknownRecord = MedicationIntakeRecord(
            eventId = "unknown-event",
            medicationId = "unknown",
            scheduledDateTime = LocalDateTime(testDate, LocalTime(9, 0)),
            status = MedicationIntakeStatus.Taken,
            updatedDateTime = LocalDateTime(testDate, LocalTime(9, 0)),
            postponedDateTime = null
        )

        val mergedEvents = applyMedicationIntakeRecords(listOf(event), listOf(unknownRecord))

        assertEquals(1, mergedEvents.size)
        assertEquals(MedicationIntakeStatus.Scheduled, mergedEvents.single().status)
    }

    private fun createEvent(
        medicationId: String,
        time: LocalTime
    ): MedicationIntakeEvent = MedicationIntakeEvent(
        eventId = "$medicationId|$testDate|${time.hour}:${time.minute}|rule",
        medicationId = medicationId,
        medicationName = "Препарат $medicationId",
        dosageText = "1 таблетка",
        scheduledDateTime = LocalDateTime(testDate, time),
        status = MedicationIntakeStatus.Scheduled,
        intakeMoment = null
    )

    private fun createRecord(
        event: MedicationIntakeEvent,
        status: MedicationIntakeStatus,
        postponedDateTime: LocalDateTime? = null
    ): MedicationIntakeRecord = MedicationIntakeRecord(
        eventId = event.eventId,
        medicationId = event.medicationId,
        scheduledDateTime = event.scheduledDateTime,
        status = status,
        updatedDateTime = LocalDateTime(testDate, LocalTime(10, 0)),
        postponedDateTime = postponedDateTime
    )
}
