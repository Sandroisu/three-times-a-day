package io.github.sandroisu.threetimesaday.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

interface TimeProvider {

    fun currentDate(): LocalDate

    fun currentDateTime(): LocalDateTime
}
