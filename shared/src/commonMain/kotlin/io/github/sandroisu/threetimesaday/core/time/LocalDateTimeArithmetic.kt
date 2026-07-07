package io.github.sandroisu.threetimesaday.core.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

fun plusMinutes(dateTime: LocalDateTime, minutes: Int): LocalDateTime {
    val totalSeconds = dateTime.time.toSecondOfDay() + minutes * SECONDS_IN_MINUTE
    val dayOffset = totalSeconds.floorDiv(SECONDS_IN_DAY)
    val secondOfDay = totalSeconds.mod(SECONDS_IN_DAY)
    val shiftedDate = dateTime.date.plus(dayOffset, DateTimeUnit.DAY)
    return LocalDateTime(shiftedDate, LocalTime.fromSecondOfDay(secondOfDay))
}

private const val SECONDS_IN_MINUTE = 60
private const val SECONDS_IN_DAY = 24 * 60 * 60
