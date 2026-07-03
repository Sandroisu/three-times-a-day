package io.github.sandroisu.threetimesaday.feature.schedule.domain

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class DailySchedule(
    val wakeUpTime: LocalTime,
    val breakfastTime: LocalTime,
    val lunchTime: LocalTime,
    val dinnerTime: LocalTime,
    val sleepTime: LocalTime
)
