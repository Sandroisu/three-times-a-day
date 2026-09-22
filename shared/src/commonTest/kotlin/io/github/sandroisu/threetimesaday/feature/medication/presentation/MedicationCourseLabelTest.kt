package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRecurrence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.course_completed
import threetimesaday.shared.generated.resources.course_active
import threetimesaday.shared.generated.resources.course_day
import threetimesaday.shared.generated.resources.course_day_of_total
import threetimesaday.shared.generated.resources.course_starts
import threetimesaday.shared.generated.resources.date_day_month
import threetimesaday.shared.generated.resources.month_7

class MedicationCourseLabelTest {

    @Test
    fun activeBoundedCourseShowsDayOfTotal() {
        assertEquals(
            UiText(Res.string.course_day_of_total, listOf(3, 7)),
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
            UiText(Res.string.course_day_of_total, listOf(1, 7)),
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
            UiText(Res.string.course_day, listOf(3)),
            medicationCourseLabel(
                courseStartDate = LocalDate(2026, 7, 8),
                courseEndDate = null,
                today = LocalDate(2026, 7, 10)
            )
        )
    }

    @Test
    fun openEndedMonthlyCourseShowsActiveInsteadOfDailyCounter() {
        assertEquals(
            UiText(Res.string.course_active),
            medicationCourseLabel(
                courseStartDate = LocalDate(2026, 7, 8),
                courseEndDate = null,
                recurrence = MedicationRecurrence.EveryMonthsOnDay(3, 14),
                today = LocalDate(2026, 7, 10),
            ),
        )
    }

    @Test
    fun futureCourseShowsStartDate() {
        assertEquals(
            UiText(Res.string.course_starts, listOf(UiText(Res.string.date_day_month, listOf(14, UiText(Res.string.month_7))))),
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
            UiText(Res.string.course_completed),
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
