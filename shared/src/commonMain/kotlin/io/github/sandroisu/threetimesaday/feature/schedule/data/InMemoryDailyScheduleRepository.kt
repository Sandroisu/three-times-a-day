package io.github.sandroisu.threetimesaday.feature.schedule.data

import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalTime

class InMemoryDailyScheduleRepository : DailyScheduleRepository {

    private val mutex = Mutex()
    private var storedSchedule: DailySchedule = createInitialSchedule()

    override suspend fun getDailySchedule(): DailySchedule = mutex.withLock { storedSchedule }

    override suspend fun saveDailySchedule(dailySchedule: DailySchedule) {
        mutex.withLock { storedSchedule = dailySchedule }
    }

    private fun createInitialSchedule(): DailySchedule = DailySchedule(
        wakeUpTime = LocalTime(8, 0),
        breakfastTime = LocalTime(8, 30),
        lunchTime = LocalTime(13, 30),
        dinnerTime = LocalTime(19, 0),
        sleepTime = LocalTime(23, 30)
    )
}
