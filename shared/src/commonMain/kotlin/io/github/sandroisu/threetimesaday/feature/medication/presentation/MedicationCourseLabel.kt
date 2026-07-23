package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.core.time.formatDayMonth
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

fun medicationCourseLabel(
    courseStartDate: LocalDate,
    courseEndDate: LocalDate?,
    today: LocalDate
): String? {
    if (today < courseStartDate) {
        return "Курс с ${formatDayMonth(courseStartDate)}"
    }
    val dayNumber = courseStartDate.daysUntil(today) + 1
    if (courseEndDate == null) {
        return "День $dayNumber"
    }
    if (courseEndDate < courseStartDate) {
        return null
    }
    if (today > courseEndDate) {
        return "Курс завершён"
    }
    val totalDays = courseStartDate.daysUntil(courseEndDate) + 1
    return "День $dayNumber из $totalDays"
}
