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
