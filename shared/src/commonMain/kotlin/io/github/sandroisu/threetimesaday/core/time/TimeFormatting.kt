package io.github.sandroisu.threetimesaday.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private val monthNamesInGenitive = listOf(
    "января",
    "февраля",
    "марта",
    "апреля",
    "мая",
    "июня",
    "июля",
    "августа",
    "сентября",
    "октября",
    "ноября",
    "декабря"
)

fun formatTimeOfDay(time: LocalTime): String {
    val hours = time.hour.toString().padStart(2, '0')
    val minutes = time.minute.toString().padStart(2, '0')
    return "$hours:$minutes"
}

fun formatScreenDate(date: LocalDate): String {
    val monthName = monthNamesInGenitive[date.month.ordinal]
    return "${date.day} $monthName ${date.year}"
}

fun formatDayMonth(date: LocalDate): String {
    val monthName = monthNamesInGenitive[date.month.ordinal]
    return "${date.day} $monthName"
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
