package io.github.sandroisu.threetimesaday.feature.schedule.data

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json

class PersistentDailyScheduleRepository(
    private val keyValueStorage: KeyValueStorage,
    private val json: Json
) : DailyScheduleRepository {

    private val mutex = Mutex()

    override suspend fun getDailySchedule(): DailySchedule = mutex.withLock {
        readSchedule()
    }

    override suspend fun saveDailySchedule(dailySchedule: DailySchedule) {
        mutex.withLock {
            keyValueStorage.putString(SCHEDULE_KEY, json.encodeToString(dailySchedule))
        }
    }

    private fun readSchedule(): DailySchedule {
        val storedSchedule = keyValueStorage.getString(SCHEDULE_KEY) ?: return createInitialSchedule()
        return runCatching { json.decodeFromString<DailySchedule>(storedSchedule) }
            .getOrElse { createInitialSchedule() }
    }

    private fun createInitialSchedule(): DailySchedule = DailySchedule(
        wakeUpTime = LocalTime(8, 0),
        breakfastTime = LocalTime(8, 30),
        lunchTime = LocalTime(13, 30),
        dinnerTime = LocalTime(19, 0),
        sleepTime = LocalTime(23, 30)
    )

    private companion object {
        const val SCHEDULE_KEY = "daily_schedule"
    }
}
