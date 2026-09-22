package io.github.sandroisu.threetimesaday.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeParsingTest {

    @Test
    fun parsesStartOfDay() {
        assertEquals(LocalTime(8, 0), parseTimeOfDay("08:00"))
    }

    @Test
    fun parsesEndOfDay() {
        assertEquals(LocalTime(23, 59), parseTimeOfDay("23:59"))
    }

    @Test
    fun parsesMidnight() {
        assertEquals(LocalTime(0, 0), parseTimeOfDay("00:00"))
    }

    @Test
    fun rejectsHourAboveRange() {
        assertNull(parseTimeOfDay("24:00"))
    }

    @Test
    fun rejectsMinuteAboveRange() {
        assertNull(parseTimeOfDay("12:60"))
    }

    @Test
    fun rejectsMissingLeadingZeroInHours() {
        assertNull(parseTimeOfDay("8:00"))
    }

    @Test
    fun rejectsWrongSeparator() {
        assertNull(parseTimeOfDay("08-00"))
    }

    @Test
    fun rejectsEmptyString() {
        assertNull(parseTimeOfDay(""))
    }

    @Test
    fun parsesDateInput() {
        assertEquals(LocalDate(2026, 7, 14), parseDateInput("14.07.2026"))
    }

    @Test
    fun rejectsImpossibleDateInput() {
        assertNull(parseDateInput("31.02.2026"))
    }

    @Test
    fun formatsDateInput() {
        assertEquals("03.07.2026", formatDateInput(LocalDate(2026, 7, 3)))
    }
}
