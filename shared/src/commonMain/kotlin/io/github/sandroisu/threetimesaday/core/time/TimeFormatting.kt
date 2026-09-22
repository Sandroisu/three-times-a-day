package io.github.sandroisu.threetimesaday.core.time

import io.github.sandroisu.threetimesaday.core.ui.UiText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.date_day_month
import threetimesaday.shared.generated.resources.date_screen
import threetimesaday.shared.generated.resources.month_1
import threetimesaday.shared.generated.resources.month_10
import threetimesaday.shared.generated.resources.month_11
import threetimesaday.shared.generated.resources.month_12
import threetimesaday.shared.generated.resources.month_2
import threetimesaday.shared.generated.resources.month_3
import threetimesaday.shared.generated.resources.month_4
import threetimesaday.shared.generated.resources.month_5
import threetimesaday.shared.generated.resources.month_6
import threetimesaday.shared.generated.resources.month_7
import threetimesaday.shared.generated.resources.month_8
import threetimesaday.shared.generated.resources.month_9

private val monthNames = listOf(
    Res.string.month_1,
    Res.string.month_2,
    Res.string.month_3,
    Res.string.month_4,
    Res.string.month_5,
    Res.string.month_6,
    Res.string.month_7,
    Res.string.month_8,
    Res.string.month_9,
    Res.string.month_10,
    Res.string.month_11,
    Res.string.month_12,
)

fun formatTimeOfDay(time: LocalTime): String {
    val hours = time.hour.toString().padStart(2, '0')
    val minutes = time.minute.toString().padStart(2, '0')
    return "$hours:$minutes"
}

fun formatDateInput(date: LocalDate): String {
    val day = date.day.toString().padStart(2, '0')
    val month = (date.month.ordinal + 1).toString().padStart(2, '0')
    return "$day.$month.${date.year}"
}

internal fun formatScreenDate(date: LocalDate): UiText {
    val monthName = UiText(monthNames[date.month.ordinal])
    return UiText(Res.string.date_screen, listOf(date.day, monthName, date.year))
}

internal fun formatDayMonth(date: LocalDate): UiText {
    val monthName = UiText(monthNames[date.month.ordinal])
    return UiText(Res.string.date_day_month, listOf(date.day, monthName))
}

fun parseTimeOfDay(text: String): LocalTime? {
    if (text.length != 5 || text[2] != ':') {
        return null
    }
    val hoursPart = text.substring(0, 2)
    val minutesPart = text.substring(3, 5)
    val allDigits = (hoursPart + minutesPart).all { character -> character in '0'..'9' }
    if (!allDigits) {
        return null
    }
    val hours = hoursPart.toInt()
    val minutes = minutesPart.toInt()
    if (hours !in 0..23 || minutes !in 0..59) {
        return null
    }
    return LocalTime(hours, minutes)
}

fun parseDateInput(text: String): LocalDate? {
    if (text.length != DATE_INPUT_LENGTH || text[DATE_SEPARATOR_INDEX] != '.' || text[SECOND_DATE_SEPARATOR_INDEX] != '.') {
        return null
    }
    val dayPart = text.substring(0, DATE_SEPARATOR_INDEX)
    val monthPart = text.substring(DATE_SEPARATOR_INDEX + 1, SECOND_DATE_SEPARATOR_INDEX)
    val yearPart = text.substring(SECOND_DATE_SEPARATOR_INDEX + 1)
    if (!(dayPart + monthPart + yearPart).all { character -> character in '0'..'9' }) {
        return null
    }
    return try {
        LocalDate(
            year = yearPart.toInt(),
            month = Month(monthPart.toInt()),
            day = dayPart.toInt(),
        )
    } catch (invalidDate: IllegalArgumentException) {
        null
    }
}

private const val DATE_INPUT_LENGTH = 10
private const val DATE_SEPARATOR_INDEX = 2
private const val SECOND_DATE_SEPARATOR_INDEX = 5
