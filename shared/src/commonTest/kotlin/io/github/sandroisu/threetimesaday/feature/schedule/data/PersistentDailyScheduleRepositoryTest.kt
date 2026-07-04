package io.github.sandroisu.threetimesaday.feature.schedule.data

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistentDailyScheduleRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun returnsDefaultScheduleWhenNothingStored() = runTest {
        val repository = PersistentDailyScheduleRepository(InMemoryKeyValueStorage(), json)

        val schedule = repository.getDailySchedule()

        assertEquals(LocalTime(8, 0), schedule.wakeUpTime)
        assertEquals(LocalTime(23, 30), schedule.sleepTime)
    }

    @Test
    fun savedScheduleSurvivesRepositoryRecreation() = runTest {
        val storage = InMemoryKeyValueStorage()
        val savedSchedule = DailySchedule(
            wakeUpTime = LocalTime(6, 45),
            breakfastTime = LocalTime(7, 15),
            lunchTime = LocalTime(12, 0),
            dinnerTime = LocalTime(18, 30),
            sleepTime = LocalTime(22, 0)
        )
        PersistentDailyScheduleRepository(storage, json).saveDailySchedule(savedSchedule)

        val restoredSchedule = PersistentDailyScheduleRepository(storage, json).getDailySchedule()

        assertEquals(savedSchedule, restoredSchedule)
    }
}
