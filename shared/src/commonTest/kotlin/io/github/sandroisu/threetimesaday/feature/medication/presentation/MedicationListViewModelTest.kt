package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MedicationListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadMedicationsDeliversMappedList() = runTest(testDispatcher) {
        val medicationRepository = FakeMedicationRepository(
            listOf(
                createMedication("wake", "Энтекавир", "1 таблетка", MedicationIntakeMoment.AfterWakeUp)
            )
        )
        val viewModel = MedicationListViewModel(medicationRepository)

        advanceUntilIdle()

        val medications = viewModel.uiState.value.medications
        assertEquals(1, medications.size)
        val listItem = medications.single()
        assertEquals("wake", listItem.id)
        assertEquals("Энтекавир", listItem.name)
        assertEquals("1 таблетка", listItem.dosageText)
        assertEquals("После пробуждения", listItem.intakeRuleText)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun emptyRepositoryGivesEmptyListWithoutError() = runTest(testDispatcher) {
        val medicationRepository = FakeMedicationRepository(emptyList())
        val viewModel = MedicationListViewModel(medicationRepository)

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState.medications.isEmpty())
        assertFalse(uiState.isLoading)
        assertEquals(null, uiState.errorMessage)
    }

    @Test
    fun repositoryFailureSetsErrorMessageAndStopsLoading() = runTest(testDispatcher) {
        val medicationRepository = FakeMedicationRepository(emptyList())
        medicationRepository.loadError = IllegalStateException("Хранилище недоступно")
        val viewModel = MedicationListViewModel(medicationRepository)

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.errorMessage)
        assertFalse(uiState.isLoading)
    }

    private fun createMedication(
        medicationId: String,
        medicationName: String,
        dosageText: String,
        intakeMoment: MedicationIntakeMoment
    ): Medication = Medication(
        id = medicationId,
        name = medicationName,
        dosageText = dosageText,
        intakeRule = MedicationIntakeRule.AtMoment(intakeMoment),
        courseStartDate = LocalDate(2020, 1, 1),
        courseEndDate = null
    )

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
