package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.core.time.formatDayMonth
import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRecurrence
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.course_completed
import threetimesaday.shared.generated.resources.course_active
import threetimesaday.shared.generated.resources.course_day
import threetimesaday.shared.generated.resources.course_day_of_total
import threetimesaday.shared.generated.resources.course_starts

internal fun medicationCourseLabel(
    courseStartDate: LocalDate,
    courseEndDate: LocalDate?,
    recurrence: MedicationRecurrence = MedicationRecurrence.Daily,
    today: LocalDate,
): UiText? {
    if (today < courseStartDate) {
        return UiText(Res.string.course_starts, listOf(formatDayMonth(courseStartDate)))
    }
    val dayNumber = courseStartDate.daysUntil(today) + 1
    if (courseEndDate == null) {
        if (recurrence is MedicationRecurrence.EveryMonthsOnDay) {
            return UiText(Res.string.course_active)
        }
        return UiText(Res.string.course_day, listOf(dayNumber))
    }
    if (courseEndDate < courseStartDate) {
        return null
    }
    if (today > courseEndDate) {
        return UiText(Res.string.course_completed)
    }
    if (recurrence is MedicationRecurrence.EveryMonthsOnDay) {
        return UiText(Res.string.course_active)
    }
    val totalDays = courseStartDate.daysUntil(courseEndDate) + 1
    return UiText(Res.string.course_day_of_total, listOf(dayNumber, totalDays))
}
