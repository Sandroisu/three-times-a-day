package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MedicationIntakeEventExpiryTest {

    private val currentDateTime = LocalDateTime(LocalDate(2026, 7, 6), LocalTime(17, 20))
    private val pastDateTime = LocalDateTime(LocalDate(2026, 7, 6), LocalTime(5, 0))
    private val futureDateTime = LocalDateTime(LocalDate(2026, 7, 6), LocalTime(23, 15))

    @Test
    fun scheduledEventInPastIsExpired() {
        assertTrue(isMedicationIntakeEventExpired(createEvent(MedicationIntakeStatus.Scheduled, pastDateTime), currentDateTime))
    }

    @Test
    fun scheduledEventInFutureIsNotExpired() {
        assertFalse(isMedicationIntakeEventExpired(createEvent(MedicationIntakeStatus.Scheduled, futureDateTime), currentDateTime))
    }

    @Test
    fun takenEventInPastIsNotExpired() {
        assertFalse(isMedicationIntakeEventExpired(createEvent(MedicationIntakeStatus.Taken, pastDateTime), currentDateTime))
    }

    @Test
    fun skippedEventInPastIsNotExpired() {
        assertFalse(isMedicationIntakeEventExpired(createEvent(MedicationIntakeStatus.Skipped, pastDateTime), currentDateTime))
    }

    @Test
    fun postponedEventInPastIsExpired() {
        assertTrue(isMedicationIntakeEventExpired(createEvent(MedicationIntakeStatus.Postponed, pastDateTime), currentDateTime))
    }

    @Test
    fun postponedEventInFutureIsNotExpired() {
        assertFalse(isMedicationIntakeEventExpired(createEvent(MedicationIntakeStatus.Postponed, futureDateTime), currentDateTime))
    }

    private fun createEvent(
        status: MedicationIntakeStatus,
        scheduledDateTime: LocalDateTime
    ): MedicationIntakeEvent = MedicationIntakeEvent(
        eventId = "event",
        medicationId = "medication",
        medicationName = "Препарат",
        dosageText = "1 таблетка",
        scheduledDateTime = scheduledDateTime,
        status = status,
        intakeMoment = null
    )
}
