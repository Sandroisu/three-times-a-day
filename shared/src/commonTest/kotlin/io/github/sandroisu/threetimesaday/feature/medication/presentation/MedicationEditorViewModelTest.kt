package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIdGenerator
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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

class MedicationEditorViewModelTest {

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
    fun newMedicationStartsEmptyWithSaveDisabled() = runTest(testDispatcher) {
        val viewModel = createViewModel(FakeMedicationRepository(emptyList()))

        viewModel.start(null)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNull(uiState.medicationId)
        assertEquals("", uiState.nameText)
        assertEquals("", uiState.dosageText)
        assertFalse(uiState.isSaveEnabled)
        assertFalse(uiState.isDeleteVisible)
    }

    @Test
    fun validNameAndDosageEnableSaveForMomentRule() = runTest(testDispatcher) {
        val viewModel = createViewModel(FakeMedicationRepository(emptyList()))
        viewModel.start(null)
        advanceUntilIdle()

        viewModel.onNameChanged("Аспирин")
        viewModel.onDosageChanged("1 таблетка")

        assertTrue(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun exactTimeRuleRequiresValidTime() = runTest(testDispatcher) {
        val viewModel = createViewModel(FakeMedicationRepository(emptyList()))
        viewModel.start(null)
        advanceUntilIdle()
        viewModel.onNameChanged("Аспирин")
        viewModel.onDosageChanged("1 таблетка")

        viewModel.onExactTimeRuleSelected()
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        viewModel.onExactTimeChanged("09:30")
        assertTrue(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun invalidExactTimeDisablesSave() = runTest(testDispatcher) {
        val viewModel = createViewModel(FakeMedicationRepository(emptyList()))
        viewModel.start(null)
        advanceUntilIdle()
        viewModel.onNameChanged("Аспирин")
        viewModel.onDosageChanged("1 таблетка")
        viewModel.onExactTimeRuleSelected()

        viewModel.onExactTimeChanged("24:00")

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.exactTimeError)
        assertFalse(uiState.isSaveEnabled)
    }

    @Test
    fun savingEmptyFormSurfacesFieldErrorsAndDoesNotPersist() = runTest(testDispatcher) {
        val medicationRepository = FakeMedicationRepository(emptyList())
        val viewModel = createViewModel(medicationRepository)
        viewModel.start(null)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.nameError)
        assertNotNull(uiState.dosageError)
        assertFalse(uiState.isSaveEnabled)
        assertTrue(medicationRepository.currentMedications().isEmpty())
    }

    @Test
    fun savingBlankNameSurfacesNameErrorAndDoesNotPersist() = runTest(testDispatcher) {
        val medicationRepository = FakeMedicationRepository(emptyList())
        val viewModel = createViewModel(medicationRepository)
        viewModel.start(null)
        advanceUntilIdle()
        viewModel.onNameChanged("   ")
        viewModel.onDosageChanged("1 таблетка")

        viewModel.save()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.nameError)
        assertFalse(uiState.isSaveEnabled)
        assertTrue(medicationRepository.currentMedications().isEmpty())
    }

    @Test
    fun savingWithExactTimeRuleAndNoTimeSurfacesErrorOnSave() = runTest(testDispatcher) {
        val medicationRepository = FakeMedicationRepository(emptyList())
        val viewModel = createViewModel(medicationRepository)
        viewModel.start(null)
        advanceUntilIdle()
        viewModel.onNameChanged("Аспирин")
        viewModel.onDosageChanged("1 таблетка")
        viewModel.onExactTimeRuleSelected()

        viewModel.save()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.exactTimeError)
        assertFalse(uiState.isSaveEnabled)
        assertTrue(medicationRepository.currentMedications().isEmpty())
    }

    @Test
    fun successfulCreateWritesMedicationToRepository() = runTest(testDispatcher) {
        val medicationRepository = FakeMedicationRepository(emptyList())
        val viewModel = createViewModel(
            medicationRepository = medicationRepository,
            idGenerator = FakeMedicationIdGenerator("generated-id")
        )
        viewModel.start(null)
        advanceUntilIdle()
        viewModel.onNameChanged("Аспирин")
        viewModel.onDosageChanged("2 таблетки")

        viewModel.save()
        advanceUntilIdle()

        val savedMedication = medicationRepository.currentMedications().single()
        assertEquals("generated-id", savedMedication.id)
        assertEquals("Аспирин", savedMedication.name)
        assertEquals("2 таблетки", savedMedication.dosageText)
        assertEquals(MedicationIntakeRule.AtMoment(MedicationIntakeMoment.AfterWakeUp), savedMedication.intakeRule)
        assertEquals(testDate, savedMedication.courseStartDate)
        assertNull(savedMedication.courseEndDate)
    }

    @Test
    fun successfulCreateEmitsSavedEvent() = runTest(testDispatcher) {
        val viewModel = createViewModel(FakeMedicationRepository(emptyList()))
        viewModel.start(null)
        advanceUntilIdle()
        viewModel.onNameChanged("Аспирин")
        viewModel.onDosageChanged("1 таблетка")

        val receivedSavedEvents = mutableListOf<Unit>()
        val collectJob = launch {
            viewModel.medicationSavedEvents.collect { receivedSavedEvents.add(it) }
        }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, receivedSavedEvents.size)
        collectJob.cancel()
    }

    @Test
    fun existingMedicationLoadsFieldsForEditing() = runTest(testDispatcher) {
        val existingMedication = createMedication(
            medicationId = "existing",
            name = "Магний",
            dosageText = "1 таблетка",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep)
        )
        val viewModel = createViewModel(FakeMedicationRepository(listOf(existingMedication)))

        viewModel.start("existing")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals("existing", uiState.medicationId)
        assertEquals("Магний", uiState.nameText)
        assertEquals("1 таблетка", uiState.dosageText)
        assertEquals(MedicationIntakeMoment.BeforeSleep, uiState.selectedIntakeMoment)
        assertFalse(uiState.isExactTimeVisible)
        assertTrue(uiState.isDeleteVisible)
    }

    @Test
    fun successfulEditUpdatesMedicationInRepository() = runTest(testDispatcher) {
        val existingMedication = createMedication(
            medicationId = "existing",
            name = "Магний",
            dosageText = "1 таблетка",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep),
            courseStartDate = LocalDate(2024, 5, 1)
        )
        val medicationRepository = FakeMedicationRepository(listOf(existingMedication))
        val viewModel = createViewModel(medicationRepository)
        viewModel.start("existing")
        advanceUntilIdle()

        viewModel.onNameChanged("Магний B6")
        viewModel.save()
        advanceUntilIdle()

        val updatedMedication = medicationRepository.currentMedications().single { it.id == "existing" }
        assertEquals("Магний B6", updatedMedication.name)
        assertEquals(LocalDate(2024, 5, 1), updatedMedication.courseStartDate)
    }

    @Test
    fun successfulDeleteRemovesMedicationFromRepository() = runTest(testDispatcher) {
        val existingMedication = createMedication(
            medicationId = "existing",
            name = "Магний",
            dosageText = "1 таблетка",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep)
        )
        val medicationRepository = FakeMedicationRepository(listOf(existingMedication))
        val viewModel = createViewModel(medicationRepository)
        viewModel.start("existing")
        advanceUntilIdle()

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(medicationRepository.currentMedications().none { it.id == "existing" })
    }

    @Test
    fun requestDeleteShowsConfirmationWithoutRemovingMedication() = runTest(testDispatcher) {
        val existingMedication = createMedication(
            medicationId = "existing",
            name = "Магний",
            dosageText = "1 таблетка",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep)
        )
        val medicationRepository = FakeMedicationRepository(listOf(existingMedication))
        val viewModel = createViewModel(medicationRepository)
        viewModel.start("existing")
        advanceUntilIdle()

        viewModel.requestDelete()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)
        assertTrue(medicationRepository.currentMedications().any { it.id == "existing" })
    }

    @Test
    fun dismissDeleteConfirmationHidesDialogAndKeepsMedication() = runTest(testDispatcher) {
        val existingMedication = createMedication(
            medicationId = "existing",
            name = "Магний",
            dosageText = "1 таблетка",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep)
        )
        val medicationRepository = FakeMedicationRepository(listOf(existingMedication))
        val viewModel = createViewModel(medicationRepository)
        viewModel.start("existing")
        advanceUntilIdle()
        viewModel.requestDelete()

        viewModel.dismissDeleteConfirmation()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
        assertTrue(medicationRepository.currentMedications().any { it.id == "existing" })
    }

    @Test
    fun confirmDeleteHidesConfirmationAndRemovesMedication() = runTest(testDispatcher) {
        val existingMedication = createMedication(
            medicationId = "existing",
            name = "Магний",
            dosageText = "1 таблетка",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep)
        )
        val medicationRepository = FakeMedicationRepository(listOf(existingMedication))
        val viewModel = createViewModel(medicationRepository)
        viewModel.start("existing")
        advanceUntilIdle()
        viewModel.requestDelete()

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
        assertTrue(medicationRepository.currentMedications().none { it.id == "existing" })
    }

    @Test
    fun failingSaveSetsGeneralErrorAndDoesNotEmitSavedEvent() = runTest(testDispatcher) {
        val medicationRepository = FakeMedicationRepository(emptyList())
        medicationRepository.saveError = IllegalStateException("Хранилище недоступно")
        val viewModel = createViewModel(medicationRepository)
        viewModel.start(null)
        advanceUntilIdle()
        viewModel.onNameChanged("Аспирин")
        viewModel.onDosageChanged("1 таблетка")

        val receivedSavedEvents = mutableListOf<Unit>()
        val collectJob = launch {
            viewModel.medicationSavedEvents.collect { receivedSavedEvents.add(it) }
        }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.generalErrorMessage)
        assertTrue(receivedSavedEvents.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun failingDeleteSetsGeneralErrorAndDoesNotEmitDeletedEvent() = runTest(testDispatcher) {
        val existingMedication = createMedication(
            medicationId = "existing",
            name = "Магний",
            dosageText = "1 таблетка",
            intakeRule = MedicationIntakeRule.AtMoment(MedicationIntakeMoment.BeforeSleep)
        )
        val medicationRepository = FakeMedicationRepository(listOf(existingMedication))
        medicationRepository.deleteError = IllegalStateException("Хранилище недоступно")
        val viewModel = createViewModel(medicationRepository)
        viewModel.start("existing")
        advanceUntilIdle()

        val receivedDeletedEvents = mutableListOf<Unit>()
        val collectJob = launch {
            viewModel.medicationDeletedEvents.collect { receivedDeletedEvents.add(it) }
        }
        advanceUntilIdle()

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.generalErrorMessage)
        assertTrue(receivedDeletedEvents.isEmpty())
        collectJob.cancel()
    }

    private fun createViewModel(
        medicationRepository: MedicationRepository,
        idGenerator: MedicationIdGenerator = FakeMedicationIdGenerator("generated-id")
    ): MedicationEditorViewModel = MedicationEditorViewModel(
        medicationRepository = medicationRepository,
        timeProvider = FakeTimeProvider(testDate),
        medicationIdGenerator = idGenerator
    )

    private fun createMedication(
        medicationId: String,
        name: String,
        dosageText: String,
        intakeRule: MedicationIntakeRule,
        courseStartDate: LocalDate = LocalDate(2020, 1, 1)
    ): Medication = Medication(
        id = medicationId,
        name = name,
        dosageText = dosageText,
        intakeRule = intakeRule,
        courseStartDate = courseStartDate,
        courseEndDate = null
    )

    private class FakeTimeProvider(
        private val fixedDate: LocalDate
    ) : TimeProvider {

        override fun currentDate(): LocalDate = fixedDate

        override fun currentDateTime(): LocalDateTime = LocalDateTime(fixedDate, LocalTime(0, 0))
    }

    private class FakeMedicationIdGenerator(
        private val idToReturn: String
    ) : MedicationIdGenerator {

        override fun nextId(): String = idToReturn
    }

    private class FakeMedicationRepository(
        initialMedications: List<Medication>
    ) : MedicationRepository {

        private val storedMedications: MutableList<Medication> = initialMedications.toMutableList()
        var loadError: Throwable? = null
        var saveError: Throwable? = null
        var updateError: Throwable? = null
        var deleteError: Throwable? = null

        fun currentMedications(): List<Medication> = storedMedications.toList()

        override suspend fun getMedications(): List<Medication> {
            loadError?.let { throw it }
            return storedMedications.toList()
        }

        override suspend fun saveMedication(medication: Medication) {
            saveError?.let { throw it }
            storedMedications.add(medication)
        }

        override suspend fun updateMedication(medication: Medication) {
            updateError?.let { throw it }
            val existingIndex = storedMedications.indexOfFirst { it.id == medication.id }
            if (existingIndex >= 0) {
                storedMedications[existingIndex] = medication
            } else {
                storedMedications.add(medication)
            }
        }

        override suspend fun deleteMedication(medicationId: String) {
            deleteError?.let { throw it }
            storedMedications.removeAll { it.id == medicationId }
        }
    }
}
