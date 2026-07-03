package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.today.domain.GenerateMedicationIntakeEventsForDateUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDate = LocalDate(2026, 7, 3)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadTodayTogglesLoadingAndDeliversEvents() = runTest(testDispatcher) {
        val loadGate = CompletableDeferred<Unit>()
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        scheduleRepository.loadGate = loadGate
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("wake", MedicationIntakeMoment.AfterWakeUp))
        )
        val viewModel = createViewModel(scheduleRepository, medicationRepository)

        runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)

        loadGate.complete(Unit)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.intakeEvents.isNotEmpty())
        assertNull(uiState.errorMessage)
    }

    @Test
    fun successfulLoadProducesEventsInTimeOrder() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(
            listOf(
                createMedication("sleep", MedicationIntakeMoment.BeforeSleep),
                createMedication("wake", MedicationIntakeMoment.AfterWakeUp),
                createMedication("breakfast", MedicationIntakeMoment.AfterBreakfast)
            )
        )
        val viewModel = createViewModel(scheduleRepository, medicationRepository)

        advanceUntilIdle()

        val eventTimes = viewModel.uiState.value.intakeEvents.map { event -> event.scheduledDateTime.time }
        assertEquals(listOf(LocalTime(8, 0), LocalTime(8, 45), LocalTime(23, 15)), eventTimes)
    }

    @Test
    fun changingScheduleRecomputesEventTimesOnReload() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(
            createSchedule(breakfastTime = LocalTime(8, 30))
        )
        val medicationRepository = FakeMedicationRepository(
            listOf(createMedication("breakfast", MedicationIntakeMoment.AfterBreakfast))
        )
        val viewModel = createViewModel(scheduleRepository, medicationRepository)
        advanceUntilIdle()
        assertEquals(
            LocalTime(8, 45),
            viewModel.uiState.value.intakeEvents.single().scheduledDateTime.time
        )

        scheduleRepository.scheduleToReturn = createSchedule(breakfastTime = LocalTime(9, 0))
        viewModel.loadToday()
        advanceUntilIdle()

        assertEquals(
            LocalTime(9, 15),
            viewModel.uiState.value.intakeEvents.single().scheduledDateTime.time
        )
    }

    @Test
    fun scheduleRepositoryFailureSetsErrorMessage() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        scheduleRepository.loadError = IllegalStateException("Режим дня недоступен")
        val medicationRepository = FakeMedicationRepository(emptyList())
        val viewModel = createViewModel(scheduleRepository, medicationRepository)

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun medicationRepositoryFailureSetsErrorMessage() = runTest(testDispatcher) {
        val scheduleRepository = FakeDailyScheduleRepository(createSchedule())
        val medicationRepository = FakeMedicationRepository(emptyList())
        medicationRepository.loadError = IllegalStateException("Препараты недоступны")
        val viewModel = createViewModel(scheduleRepository, medicationRepository)

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }

    private fun createViewModel(
        scheduleRepository: DailyScheduleRepository,
        medicationRepository: MedicationRepository
    ): TodayViewModel = TodayViewModel(
        timeProvider = FakeTimeProvider(testDate),
        dailyScheduleRepository = scheduleRepository,
        medicationRepository = medicationRepository,
        generateMedicationIntakeEventsForDate = GenerateMedicationIntakeEventsForDateUseCase()
    )

    private fun createSchedule(
        wakeUpTime: LocalTime = LocalTime(8, 0),
        breakfastTime: LocalTime = LocalTime(8, 30),
        lunchTime: LocalTime = LocalTime(13, 30),
        dinnerTime: LocalTime = LocalTime(19, 0),
        sleepTime: LocalTime = LocalTime(23, 30)
    ): DailySchedule = DailySchedule(
        wakeUpTime = wakeUpTime,
        breakfastTime = breakfastTime,
        lunchTime = lunchTime,
        dinnerTime = dinnerTime,
        sleepTime = sleepTime
    )

    private fun createMedication(
        medicationId: String,
        intakeMoment: MedicationIntakeMoment
    ): Medication = Medication(
        id = medicationId,
        name = "Препарат $medicationId",
        dosageText = "1 таблетка",
        intakeRule = MedicationIntakeRule.AtMoment(intakeMoment),
        courseStartDate = LocalDate(2020, 1, 1),
        courseEndDate = null
    )

    private class FakeTimeProvider(
        private val fixedDate: LocalDate
    ) : TimeProvider {

        override fun currentDate(): LocalDate = fixedDate

        override fun currentDateTime(): LocalDateTime = LocalDateTime(fixedDate, LocalTime(0, 0))
    }

    private class FakeDailyScheduleRepository(
        var scheduleToReturn: DailySchedule
    ) : DailyScheduleRepository {

        var loadError: Throwable? = null
        var loadGate: CompletableDeferred<Unit>? = null

        override suspend fun getDailySchedule(): DailySchedule {
            loadError?.let { throw it }
            loadGate?.await()
            return scheduleToReturn
        }

        override suspend fun saveDailySchedule(dailySchedule: DailySchedule) {
            scheduleToReturn = dailySchedule
        }
    }

    private class FakeMedicationRepository(
        initialMedications: List<Medication>
    ) : MedicationRepository {

        private val storedMedications: MutableList<Medication> = initialMedications.toMutableList()
        var loadError: Throwable? = null

        override suspend fun getMedications(): List<Medication> {
            loadError?.let { throw it }
            return storedMedications.toList()
        }

        override suspend fun saveMedication(medication: Medication) {
            storedMedications.add(medication)
        }

        override suspend fun updateMedication(medication: Medication) {
            val existingIndex = storedMedications.indexOfFirst { it.id == medication.id }
            if (existingIndex >= 0) {
                storedMedications[existingIndex] = medication
            }
        }

        override suspend fun deleteMedication(medicationId: String) {
            storedMedications.removeAll { it.id == medicationId }
        }
    }
}
