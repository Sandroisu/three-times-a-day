package io.github.sandroisu.threetimesaday.feature.medication.presentation

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MedicationCourseLabelTest {

    @Test
    fun activeBoundedCourseShowsDayOfTotal() {
        assertEquals(
            "День 3 из 7",
            medicationCourseLabel(
                courseStartDate = LocalDate(2026, 7, 8),
                courseEndDate = LocalDate(2026, 7, 14),
                today = LocalDate(2026, 7, 10)
            )
        )
    }

    @Test
    fun firstDayOfCourseIsDayOne() {
        assertEquals(
            "День 1 из 7",
            medicationCourseLabel(
                courseStartDate = LocalDate(2026, 7, 8),
                courseEndDate = LocalDate(2026, 7, 14),
                today = LocalDate(2026, 7, 8)
            )
        )
    }

    @Test
    fun openEndedCourseShowsDayWithoutTotal() {
        assertEquals(
            "День 3",
            medicationCourseLabel(
                courseStartDate = LocalDate(2026, 7, 8),
                courseEndDate = null,
                today = LocalDate(2026, 7, 10)
            )
        )
    }

    @Test
    fun futureCourseShowsStartDate() {
        assertEquals(
            "Курс с 14 июля",
            medicationCourseLabel(
                courseStartDate = LocalDate(2026, 7, 14),
                courseEndDate = LocalDate(2026, 7, 20),
                today = LocalDate(2026, 7, 10)
            )
        )
    }

    @Test
    fun finishedCourseShowsCompleted() {
        assertEquals(
            "Курс завершён",
            medicationCourseLabel(
                courseStartDate = LocalDate(2026, 7, 1),
                courseEndDate = LocalDate(2026, 7, 7),
                today = LocalDate(2026, 7, 10)
            )
        )
    }

    @Test
    fun endBeforeStartHasNoLabel() {
        assertNull(
            medicationCourseLabel(
                courseStartDate = LocalDate(2026, 7, 10),
                courseEndDate = LocalDate(2026, 7, 1),
                today = LocalDate(2026, 7, 10)
            )
        )
    }
}
