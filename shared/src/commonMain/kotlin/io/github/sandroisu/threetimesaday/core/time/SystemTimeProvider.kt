package io.github.sandroisu.threetimesaday.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SystemTimeProvider : TimeProvider {

    override fun currentDate(): LocalDate = currentDateTime().date

    override fun currentDateTime(): LocalDateTime {
        val currentTimeZone = TimeZone.currentSystemDefault()
        return Clock.System.now().toLocalDateTime(currentTimeZone)
    }
}
