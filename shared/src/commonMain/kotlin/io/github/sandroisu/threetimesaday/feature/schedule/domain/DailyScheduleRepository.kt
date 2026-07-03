package io.github.sandroisu.threetimesaday.feature.schedule.domain

interface DailyScheduleRepository {

    suspend fun getDailySchedule(): DailySchedule

    suspend fun saveDailySchedule(dailySchedule: DailySchedule)
}
