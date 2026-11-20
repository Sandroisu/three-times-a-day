package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.storageJson
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.data.PersistentDailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.today.data.PersistentMedicationIntakeRecordRepository
import io.github.sandroisu.threetimesaday.feature.today.domain.ApplyMedicationIntakeRecordsUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.BuildIntakeHistoryUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.GenerateMedicationIntakeEventsForDateUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeAdherence
import io.github.sandroisu.threetimesaday.feature.today.domain.IntakeHistoryPeriod
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationAdherence
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecord
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IntakeHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val storage = InMemoryKeyValueStorage()
    private val scheduleRepository = PersistentDailyScheduleRepository(storage, storageJson)
    private val recordRepository = PersistentMedicationIntakeRecordRepository(storage, storageJson)
    private val medicationRepository = FakeMedicationRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsLastSevenDaysNewestFirst() = runTest(testDispatcher) {
        medicationRepository.medications = listOf(vitamin)
        saveRecord(YESTERDAY, MedicationIntakeStatus.Taken)
        val loadGate = CompletableDeferred<Unit>()
        medicationRepository.nextLoadGate = loadGate

        val viewModel = createViewModel()
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)
        loadGate.complete(Unit)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(IntakeHistoryPeriod.LastSevenDays, uiState.selectedPeriod)
        assertEquals(7, uiState.days.size)
        assertEquals(TODAY, uiState.days.first().date)
        assertEquals(LocalDate(2026, 7, 4), uiState.days.last().date)
        assertEquals(IntakeAdherence(takenCount = 1, skippedCount = 0, missedCount = 1), uiState.overallAdherence)
        assertEquals(
            listOf(MedicationAdherence("vitamin", "Vitamin D", IntakeAdherence(1, 0, 1))),
            uiState.medicationAdherence,
        )
        assertNull(uiState.errorMessage)
    }

    @Test
    fun selectingThirtyDaysReloadsTheLongerPeriod() = runTest(testDispatcher) {
        medicationRepository.medications = listOf(vitamin)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.selectPeriod(IntakeHistoryPeriod.LastThirtyDays)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(IntakeHistoryPeriod.LastThirtyDays, uiState.selectedPeriod)
        assertEquals(30, uiState.days.size)
        assertEquals(LocalDate(2026, 6, 11), uiState.days.last().date)
    }

    @Test
    fun olderSlowLoadDoesNotReplaceTheSelectedPeriod() = runTest(testDispatcher) {
        medicationRepository.medications = listOf(vitamin)
        val firstLoadGate = CompletableDeferred<Unit>()
        medicationRepository.nextLoadGate = firstLoadGate
        val viewModel = createViewModel()
        runCurrent()

        viewModel.selectPeriod(IntakeHistoryPeriod.LastThirtyDays)
        advanceUntilIdle()
        firstLoadGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(IntakeHistoryPeriod.LastThirtyDays, viewModel.uiState.value.selectedPeriod)
        assertEquals(30, viewModel.uiState.value.days.size)
    }

    @Test
    fun noMedicationsShowsEmptyState() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertFalse(uiState.hasMedications)
        assertTrue(uiState.days.isEmpty())
        assertNull(uiState.overallAdherence)
    }

    @Test
    fun loadFailureShowsErrorAndRetryRecovers() = runTest(testDispatcher) {
        medicationRepository.medications = listOf(vitamin)
        medicationRepository.failure = IllegalStateException("storage is broken")
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(IntakeHistoryLabels.loadError, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.days.isEmpty())

        medicationRepository.failure = null
        viewModel.loadHistory()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(7, viewModel.uiState.value.days.size)
    }

    private fun createViewModel(): IntakeHistoryViewModel = IntakeHistoryViewModel(
        BuildIntakeHistoryUseCase(
            timeProvider = FixedTimeProvider(NOW),
            dailyScheduleRepository = scheduleRepository,
            medicationRepository = medicationRepository,
            medicationIntakeRecordRepository = recordRepository,
            generateMedicationIntakeEventsForDate = GenerateMedicationIntakeEventsForDateUseCase(),
            applyMedicationIntakeRecords = ApplyMedicationIntakeRecordsUseCase(),
        )
    )

    private suspend fun saveRecord(date: LocalDate, status: MedicationIntakeStatus) {
        val event = GenerateMedicationIntakeEventsForDateUseCase()(date, scheduleRepository.getDailySchedule(), listOf(vitamin))
            .single()
        recordRepository.saveRecord(
            MedicationIntakeRecord(
                eventId = event.eventId,
                medicationId = vitamin.id,
                scheduledDateTime = event.scheduledDateTime,
                status = status,
                updatedDateTime = NOW,
            )
        )
    }

    private class FakeMedicationRepository : MedicationRepository {
        var medications: List<Medication> = emptyList()
        var failure: Exception? = null
        var nextLoadGate: CompletableDeferred<Unit>? = null

        override suspend fun getMedications(): List<Medication> {
            val gate = nextLoadGate
            nextLoadGate = null
            gate?.await()
            failure?.let { loadFailure -> throw loadFailure }
            return medications
        }

        override suspend fun saveMedication(medication: Medication) = Unit
        override suspend fun updateMedication(medication: Medication) = Unit
        override suspend fun deleteMedication(medicationId: String) = Unit
        override suspend fun takeDose(medicationId: String): Boolean = false
        override suspend fun returnDose(medicationId: String) = Unit
    }

    private class FixedTimeProvider(private val dateTime: LocalDateTime) : TimeProvider {
        override fun currentDate(): LocalDate = dateTime.date
        override fun currentDateTime(): LocalDateTime = dateTime
    }

    private companion object {
        val TODAY = LocalDate(2026, 7, 10)
        val YESTERDAY = LocalDate(2026, 7, 9)
        val NOW = LocalDateTime(TODAY, LocalTime(12, 0))
        val vitamin = Medication(
            id = "vitamin",
            name = "Vitamin D",
            dosageText = "1 tablet",
            intakeRule = MedicationIntakeRule.AtExactTime(LocalTime(9, 0)),
            courseStartDate = YESTERDAY,
            courseEndDate = null,
        )
    }
}
