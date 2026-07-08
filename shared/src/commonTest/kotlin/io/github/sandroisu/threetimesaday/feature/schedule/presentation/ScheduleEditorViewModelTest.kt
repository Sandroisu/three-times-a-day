package io.github.sandroisu.threetimesaday.feature.schedule.presentation

import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScheduleEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val timeFormatErrorMessage = "Введите время в формате HH:mm"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsScheduleAndFormatsFieldsOnStart() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createInitialSchedule())
        val viewModel = ScheduleEditorViewModel(scheduleRepository)

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("08:00", uiState.wakeUpTimeText)
        assertEquals("08:30", uiState.breakfastTimeText)
        assertEquals("13:30", uiState.lunchTimeText)
        assertEquals("19:00", uiState.dinnerTimeText)
        assertEquals("23:30", uiState.sleepTimeText)
        assertTrue(uiState.isSaveEnabled)
    }

    @Test
    fun missingLeadingZeroMarksErrorAndDisablesSave() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createInitialSchedule())
        val viewModel = ScheduleEditorViewModel(scheduleRepository)
        advanceUntilIdle()

        viewModel.onWakeUpTimeChanged("8:00")

        val uiState = viewModel.uiState.value
        assertEquals(timeFormatErrorMessage, uiState.wakeUpTimeError)
        assertFalse(uiState.isSaveEnabled)
    }

    @Test
    fun hourAboveRangeMarksErrorAndDisablesSave() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createInitialSchedule())
        val viewModel = ScheduleEditorViewModel(scheduleRepository)
        advanceUntilIdle()

        viewModel.onBreakfastTimeChanged("24:00")

        val uiState = viewModel.uiState.value
        assertEquals(timeFormatErrorMessage, uiState.breakfastTimeError)
        assertFalse(uiState.isSaveEnabled)
    }

    @Test
    fun fixingInvalidFieldReenablesSave() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createInitialSchedule())
        val viewModel = ScheduleEditorViewModel(scheduleRepository)
        advanceUntilIdle()

        viewModel.onWakeUpTimeChanged("8:00")
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onWakeUpTimeChanged("07:00")

        val uiState = viewModel.uiState.value
        assertNull(uiState.wakeUpTimeError)
        assertTrue(uiState.isSaveEnabled)
    }

    @Test
    fun duplicateTimesShowGeneralErrorAndDisableSave() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createInitialSchedule())
        val viewModel = ScheduleEditorViewModel(scheduleRepository)
        advanceUntilIdle()

        viewModel.onBreakfastTimeChanged("08:00")

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.generalErrorMessage)
        assertFalse(uiState.isSaveEnabled)
    }

    @Test
    fun duplicateTimesPreventSave() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createInitialSchedule())
        val viewModel = ScheduleEditorViewModel(scheduleRepository)
        advanceUntilIdle()
        viewModel.onBreakfastTimeChanged("08:00")

        val receivedSavedEvents = mutableListOf<Unit>()
        val collectJob = launch {
            viewModel.scheduleSavedEvents.collect { receivedSavedEvents.add(it) }
        }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertNull(scheduleRepository.savedSchedule)
        assertTrue(receivedSavedEvents.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun resolvingDuplicateReenablesSaveAndClearsGeneralError() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createInitialSchedule())
        val viewModel = ScheduleEditorViewModel(scheduleRepository)
        advanceUntilIdle()
        viewModel.onBreakfastTimeChanged("08:00")
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onBreakfastTimeChanged("09:00")

        val uiState = viewModel.uiState.value
        assertNull(uiState.generalErrorMessage)
        assertTrue(uiState.isSaveEnabled)
    }

    @Test
    fun successfulSaveWritesNewScheduleToRepository() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createInitialSchedule())
        val viewModel = ScheduleEditorViewModel(scheduleRepository)
        advanceUntilIdle()

        viewModel.onWakeUpTimeChanged("07:15")
        viewModel.save()
        advanceUntilIdle()

        val expectedSchedule = createInitialSchedule().copy(wakeUpTime = LocalTime(7, 15))
        assertEquals(expectedSchedule, scheduleRepository.savedSchedule)
    }

    @Test
    fun successfulSaveEmitsSavedEvent() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createInitialSchedule())
        val viewModel = ScheduleEditorViewModel(scheduleRepository)
        advanceUntilIdle()

        val receivedSavedEvents = mutableListOf<Unit>()
        val collectJob = launch {
            viewModel.scheduleSavedEvents.collect { receivedSavedEvents.add(it) }
        }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, receivedSavedEvents.size)
        collectJob.cancel()
    }

    @Test
    fun failingSaveSetsGeneralErrorAndDoesNotEmitSavedEvent() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createInitialSchedule())
        scheduleRepository.saveError = IllegalStateException("Хранилище недоступно")
        val viewModel = ScheduleEditorViewModel(scheduleRepository)
        advanceUntilIdle()

        val receivedSavedEvents = mutableListOf<Unit>()
        val collectJob = launch {
            viewModel.scheduleSavedEvents.collect { receivedSavedEvents.add(it) }
        }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.generalErrorMessage)
        assertTrue(receivedSavedEvents.isEmpty())
        collectJob.cancel()
    }

    private fun createInitialSchedule(): DailySchedule = DailySchedule(
        wakeUpTime = LocalTime(8, 0),
        breakfastTime = LocalTime(8, 30),
        lunchTime = LocalTime(13, 30),
        dinnerTime = LocalTime(19, 0),
        sleepTime = LocalTime(23, 30)
    )

    private class FakeDailyScheduleRepository(
        initialSchedule: DailySchedule
    ) : DailyScheduleRepository {

        private var storedSchedule: DailySchedule = initialSchedule
        var savedSchedule: DailySchedule? = null
        var saveError: Throwable? = null

        override suspend fun getDailySchedule(): DailySchedule = storedSchedule

        override suspend fun saveDailySchedule(dailySchedule: DailySchedule) {
            saveError?.let { throw it }
            storedSchedule = dailySchedule
            savedSchedule = dailySchedule
        }
    }
}
