package io.github.sandroisu.threetimesaday.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateTimeArithmeticTest {

    @Test
    fun crossesMidnightWithinSameMonth() {
        assertEquals(
            LocalDateTime(LocalDate(2026, 7, 7), LocalTime(0, 10)),
            plusMinutes(LocalDateTime(LocalDate(2026, 7, 6), LocalTime(23, 50)), 20)
        )
    }

    @Test
    fun crossesMonthBoundary() {
        assertEquals(
            LocalDateTime(LocalDate(2026, 2, 1), LocalTime(0, 30)),
            plusMinutes(LocalDateTime(LocalDate(2026, 1, 31), LocalTime(23, 30)), 60)
        )
    }

    @Test
    fun crossesYearBoundary() {
        assertEquals(
            LocalDateTime(LocalDate(2027, 1, 1), LocalTime(0, 30)),
            plusMinutes(LocalDateTime(LocalDate(2026, 12, 31), LocalTime(23, 30)), 60)
        )
    }

    @Test
    fun crossesIntoLeapDay() {
        assertEquals(
            LocalDateTime(LocalDate(2028, 2, 29), LocalTime(23, 30)),
            plusMinutes(LocalDateTime(LocalDate(2028, 2, 28), LocalTime(23, 30)), 1440)
        )
    }

    @Test
    fun skipsNonExistentLeapDayInCommonYear() {
        assertEquals(
            LocalDateTime(LocalDate(2027, 3, 1), LocalTime(23, 30)),
            plusMinutes(LocalDateTime(LocalDate(2027, 2, 28), LocalTime(23, 30)), 1440)
        )
    }
}
