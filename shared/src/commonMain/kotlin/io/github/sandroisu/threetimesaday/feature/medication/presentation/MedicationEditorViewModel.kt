package io.github.sandroisu.threetimesaday.feature.medication.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.core.time.parseTimeOfDay
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIdGenerator
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MedicationEditorViewModel(
    private val medicationRepository: MedicationRepository,
    private val timeProvider: TimeProvider,
    private val medicationIdGenerator: MedicationIdGenerator
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(MedicationEditorUiState())
    val uiState: StateFlow<MedicationEditorUiState> = mutableUiState.asStateFlow()

    private val medicationSavedEventsChannel = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val medicationSavedEvents: SharedFlow<Unit> = medicationSavedEventsChannel.asSharedFlow()

    private val medicationDeletedEventsChannel = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val medicationDeletedEvents: SharedFlow<Unit> = medicationDeletedEventsChannel.asSharedFlow()

    private var loadedMedication: Medication? = null
    private var editingUnsupportedRule = false

    fun start(medicationId: String?) {
        if (medicationId == null) {
            startCreation()
        } else {
            startEditing(medicationId)
        }
    }

    fun onNameChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(nameText = text)) }
    }

    fun onDosageChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(dosageText = text)) }
    }

    fun onIntakeMomentSelected(intakeMoment: MedicationIntakeMoment) {
        mutableUiState.update { currentState ->
            validate(
                currentState.copy(
                    selectedIntakeMoment = intakeMoment,
                    isExactTimeVisible = false
                )
            )
        }
    }

    fun onExactTimeRuleSelected() {
        mutableUiState.update { currentState ->
            validate(
                currentState.copy(
                    selectedIntakeMoment = null,
                    isExactTimeVisible = true
                )
            )
        }
    }

    fun onExactTimeChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(exactTimeText = text)) }
    }

    fun save() {
        val validatedState = validate(mutableUiState.value)
        mutableUiState.value = validatedState
        if (!validatedState.isSaveEnabled) {
            return
        }
        val intakeRule = buildIntakeRule(validatedState) ?: return
        viewModelScope.launch {
            try {
                persistMedication(validatedState, intakeRule)
                medicationSavedEventsChannel.tryEmit(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (saveFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        generalErrorMessage = saveFailure.message ?: "Не удалось сохранить препарат"
                    )
                }
            }
        }
    }

    fun requestDelete() {
        if (mutableUiState.value.medicationId == null) {
            return
        }
        mutableUiState.update { currentState ->
            currentState.copy(isDeleteConfirmationVisible = true)
        }
    }

    fun dismissDeleteConfirmation() {
        mutableUiState.update { currentState ->
            currentState.copy(isDeleteConfirmationVisible = false)
        }
    }

    fun confirmDelete() {
        val medicationId = mutableUiState.value.medicationId ?: return
        mutableUiState.update { currentState ->
            currentState.copy(isDeleteConfirmationVisible = false)
        }
        viewModelScope.launch {
            try {
                medicationRepository.deleteMedication(medicationId)
                medicationDeletedEventsChannel.tryEmit(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (deleteFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        generalErrorMessage = deleteFailure.message ?: "Не удалось удалить препарат"
                    )
                }
            }
        }
    }

    private fun startCreation() {
        loadedMedication = null
        editingUnsupportedRule = false
        mutableUiState.value = validate(
            MedicationEditorUiState(
                isLoading = false,
                medicationId = null,
                selectedIntakeMoment = MedicationIntakeMoment.AfterWakeUp,
                isExactTimeVisible = false,
                isDeleteVisible = false
            )
        )
    }

    private fun startEditing(medicationId: String) {
        loadedMedication = null
        editingUnsupportedRule = false
        mutableUiState.value = MedicationEditorUiState(
            isLoading = true,
            medicationId = medicationId,
            isDeleteVisible = true
        )
        viewModelScope.launch {
            try {
                val medication = medicationRepository.getMedications()
                    .firstOrNull { it.id == medicationId }
                if (medication == null) {
                    mutableUiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            generalErrorMessage = "Препарат не найден"
                        )
                    }
                    return@launch
                }
                loadedMedication = medication
                mutableUiState.value = validate(stateForMedication(medication))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (loadFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        generalErrorMessage = loadFailure.message ?: "Не удалось загрузить препарат"
                    )
                }
            }
        }
    }

    private fun stateForMedication(medication: Medication): MedicationEditorUiState {
        val baseState = MedicationEditorUiState(
            isLoading = false,
            medicationId = medication.id,
            nameText = medication.name,
            dosageText = medication.dosageText,
            isDeleteVisible = true
        )
        return when (val intakeRule = medication.intakeRule) {
            is MedicationIntakeRule.AtMoment -> baseState.copy(
                selectedIntakeMoment = intakeRule.moment,
                isExactTimeVisible = false
            )

            is MedicationIntakeRule.AtExactTime -> baseState.copy(
                selectedIntakeMoment = null,
                isExactTimeVisible = true,
                exactTimeText = formatTimeOfDay(intakeRule.time)
            )

            is MedicationIntakeRule.SeveralTimesPerDay -> {
                editingUnsupportedRule = true
                baseState.copy(
                    generalErrorMessage = "Этот тип приёма пока нельзя редактировать"
                )
            }
        }
    }

    private suspend fun persistMedication(
        state: MedicationEditorUiState,
        intakeRule: MedicationIntakeRule
    ) {
        val trimmedName = state.nameText.trim()
        val trimmedDosage = state.dosageText.trim()
        val editedMedicationId = state.medicationId
        if (editedMedicationId == null) {
            val newMedication = Medication(
                id = medicationIdGenerator.nextId(),
                name = trimmedName,
                dosageText = trimmedDosage,
                intakeRule = intakeRule,
                courseStartDate = timeProvider.currentDate(),
                courseEndDate = null
            )
            medicationRepository.saveMedication(newMedication)
        } else {
            val existingMedication = loadedMedication
            val updatedMedication = existingMedication?.copy(
                name = trimmedName,
                dosageText = trimmedDosage,
                intakeRule = intakeRule
            ) ?: Medication(
                id = editedMedicationId,
                name = trimmedName,
                dosageText = trimmedDosage,
                intakeRule = intakeRule,
                courseStartDate = timeProvider.currentDate(),
                courseEndDate = null
            )
            medicationRepository.updateMedication(updatedMedication)
        }
    }

    private fun buildIntakeRule(state: MedicationEditorUiState): MedicationIntakeRule? {
        if (state.isExactTimeVisible) {
            val exactTime = parseTimeOfDay(state.exactTimeText) ?: return null
            return MedicationIntakeRule.AtExactTime(exactTime)
        }
        val selectedMoment = state.selectedIntakeMoment ?: return null
        return MedicationIntakeRule.AtMoment(selectedMoment)
    }

    private fun validate(state: MedicationEditorUiState): MedicationEditorUiState {
        val nameValid = state.nameText.trim().isNotEmpty()
        val dosageValid = state.dosageText.trim().isNotEmpty()
        val exactTimeParsed = parseTimeOfDay(state.exactTimeText)
        val exactTimeValid = !state.isExactTimeVisible || exactTimeParsed != null
        return state.copy(
            nameError = if (!nameValid && state.nameText.isNotEmpty()) NAME_ERROR_MESSAGE else null,
            dosageError = if (!dosageValid && state.dosageText.isNotEmpty()) DOSAGE_ERROR_MESSAGE else null,
            exactTimeError = if (state.isExactTimeVisible && state.exactTimeText.isNotEmpty() && exactTimeParsed == null) {
                TIME_FORMAT_ERROR_MESSAGE
            } else {
                null
            },
            isSaveEnabled = nameValid && dosageValid && exactTimeValid && !editingUnsupportedRule && !state.isLoading
        )
    }

    private companion object {
        const val NAME_ERROR_MESSAGE = "Введите название"
        const val DOSAGE_ERROR_MESSAGE = "Введите дозировку"
        const val TIME_FORMAT_ERROR_MESSAGE = "Введите время в формате HH:mm"
    }
}
