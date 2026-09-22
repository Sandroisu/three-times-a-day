package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiLabels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.core.time.formatDateInput
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.core.time.parseDateInput
import io.github.sandroisu.threetimesaday.core.time.parseTimeOfDay
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIdGenerator
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRecurrence
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

internal class MedicationEditorViewModel(
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
    private var isSessionStarted = false

    fun start(medicationId: String?) {
        if (isSessionStarted && mutableUiState.value.medicationId == medicationId) return
        isSessionStarted = true
        if (medicationId == null) {
            startCreation()
        } else {
            startEditing(medicationId)
        }
    }

    fun finishSession() {
        isSessionStarted = false
    }

    fun editMedication() {
        mutableUiState.update { it.copy(isEditing = true) }
    }

    fun showDetails() {
        val medication = loadedMedication ?: return
        mutableUiState.update { validate(stateForMedication(medication)) }
    }

    fun onNameChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(nameText = text)) }
    }

    fun onDosageChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(dosageText = text)) }
    }

    fun onCourseStartDateChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(courseStartDateText = text)) }
    }

    fun onCourseEndDateChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(courseEndDateText = text)) }
    }

    fun onDailyRecurrenceSelected() {
        mutableUiState.update { currentState -> validate(currentState.copy(isMonthlyRecurrence = false)) }
    }

    fun onMonthlyRecurrenceSelected() {
        mutableUiState.update { currentState ->
            validate(
                currentState.copy(
                    isMonthlyRecurrence = true,
                    monthlyDayOfMonthText = currentState.monthlyDayOfMonthText.ifBlank {
                        timeProvider.currentDate().day.toString()
                    },
                )
            )
        }
    }

    fun onMonthlyIntervalChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(monthlyIntervalText = text)) }
    }

    fun onMonthlyDayOfMonthChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(monthlyDayOfMonthText = text)) }
    }

    fun onIntakeMomentSelected(intakeMoment: MedicationIntakeMoment) {
        mutableUiState.update { currentState ->
            validate(
                currentState.copy(
                    selectedIntakeMoment = intakeMoment,
                    isExactTimeVisible = false,
                    isDistributedRule = false,
                )
            )
        }
    }

    fun onExactTimeRuleSelected() {
        mutableUiState.update { currentState ->
            validate(
                currentState.copy(
                    selectedIntakeMoment = null,
                    isExactTimeVisible = true,
                    isDistributedRule = false,
                )
            )
        }
    }

    fun onExactTimeChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(exactTimeText = text)) }
    }

    fun save() {
        if (mutableUiState.value.isSaving) return
        val validatedState = validate(mutableUiState.value, forceErrors = true)
        mutableUiState.update { validatedState }
        if (!validatedState.isSaveEnabled) {
            return
        }
        val intakeRule = buildIntakeRule(validatedState) ?: return
        val courseStartDate = parseDateInput(validatedState.courseStartDateText) ?: return
        val courseEndDate = validatedState.courseEndDateText.trim()
            .takeIf { text -> text.isNotEmpty() }
            ?.let(::parseDateInput)
        val recurrence = buildRecurrence(validatedState) ?: return
        mutableUiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                persistMedication(
                    state = validatedState,
                    intakeRule = intakeRule,
                    courseStartDate = courseStartDate,
                    courseEndDate = courseEndDate,
                    recurrence = recurrence,
                )
                medicationSavedEventsChannel.tryEmit(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (saveFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        generalErrorMessage = MedicationLabels.saveError
                    )
                }
            } finally {
                mutableUiState.update { it.copy(isSaving = false) }
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
                        generalErrorMessage = MedicationLabels.deleteError
                    )
                }
            }
        }
    }

    private fun startCreation() {
        loadedMedication = null
        mutableUiState.update {
            validate(
                MedicationEditorUiState(
                    isLoading = false,
                    medicationId = null,
                    selectedIntakeMoment = MedicationIntakeMoment.AfterWakeUp,
                    isExactTimeVisible = false,
                    isDeleteVisible = false,
                    courseStartDateText = formatDateInput(timeProvider.currentDate()),
                )
            )
        }
    }

    private fun startEditing(medicationId: String) {
        loadedMedication = null
        mutableUiState.update {
            MedicationEditorUiState(
                isLoading = true,
                medicationId = medicationId,
                isDeleteVisible = true,
                isEditing = false,
            )
        }
        viewModelScope.launch {
            try {
                val medication = medicationRepository.getMedications()
                    .firstOrNull { it.id == medicationId }
                if (medication == null) {
                    mutableUiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            generalErrorMessage = MedicationLabels.notFound
                        )
                    }
                    return@launch
                }
                loadedMedication = medication
                mutableUiState.update { validate(stateForMedication(medication)) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (loadFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        generalErrorMessage = MedicationLabels.loadError
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
            courseStartDateText = formatDateInput(medication.courseStartDate),
            courseEndDateText = medication.courseEndDate?.let(::formatDateInput).orEmpty(),
            isDeleteVisible = true,
            isEditing = false,
            intakeRuleLabel = medicationIntakeRuleText(medication.intakeRule),
            recurrenceLabel = medicationRecurrenceLabel(medication.recurrence),
            courseScheduleLabel = medicationCourseScheduleLabel(medication),
            courseLabel = medicationCourseLabel(
                courseStartDate = medication.courseStartDate,
                courseEndDate = medication.courseEndDate,
                recurrence = medication.recurrence,
                today = timeProvider.currentDate(),
            ),
        )
        val recurrenceState = when (val recurrence = medication.recurrence) {
            MedicationRecurrence.Daily -> baseState
            is MedicationRecurrence.EveryMonthsOnDay -> baseState.copy(
                isMonthlyRecurrence = true,
                monthlyIntervalText = recurrence.intervalMonths.toString(),
                monthlyDayOfMonthText = recurrence.dayOfMonth.toString(),
            )
        }
        return when (val intakeRule = medication.intakeRule) {
            is MedicationIntakeRule.AtMoment -> recurrenceState.copy(
                selectedIntakeMoment = intakeRule.moment,
                isExactTimeVisible = false
            )

            is MedicationIntakeRule.AtExactTime -> recurrenceState.copy(
                selectedIntakeMoment = null,
                isExactTimeVisible = true,
                exactTimeText = formatTimeOfDay(intakeRule.time)
            )

            is MedicationIntakeRule.SeveralTimesPerDay -> {
                recurrenceState.copy(
                    isDistributedRule = true,
                )
            }
        }
    }

    private suspend fun persistMedication(
        state: MedicationEditorUiState,
        intakeRule: MedicationIntakeRule,
        courseStartDate: kotlinx.datetime.LocalDate,
        courseEndDate: kotlinx.datetime.LocalDate?,
        recurrence: MedicationRecurrence,
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
                courseStartDate = courseStartDate,
                courseEndDate = courseEndDate,
                recurrence = recurrence,
            )
            medicationRepository.saveMedication(newMedication)
        } else {
            val existingMedication = loadedMedication
            val updatedMedication = existingMedication?.copy(
                name = trimmedName,
                dosageText = trimmedDosage,
                intakeRule = intakeRule,
                courseStartDate = courseStartDate,
                courseEndDate = courseEndDate,
                recurrence = recurrence,
            ) ?: Medication(
                id = editedMedicationId,
                name = trimmedName,
                dosageText = trimmedDosage,
                intakeRule = intakeRule,
                courseStartDate = courseStartDate,
                courseEndDate = courseEndDate,
                recurrence = recurrence,
            )
            medicationRepository.updateMedication(updatedMedication)
        }
    }

    private fun buildIntakeRule(state: MedicationEditorUiState): MedicationIntakeRule? {
        if (state.isDistributedRule) return loadedMedication?.intakeRule
        if (state.isExactTimeVisible) {
            val exactTime = parseTimeOfDay(state.exactTimeText) ?: return null
            return MedicationIntakeRule.AtExactTime(exactTime)
        }
        val selectedMoment = state.selectedIntakeMoment ?: return null
        return MedicationIntakeRule.AtMoment(selectedMoment)
    }

    private fun buildRecurrence(state: MedicationEditorUiState): MedicationRecurrence? {
        if (!state.isMonthlyRecurrence) {
            return MedicationRecurrence.Daily
        }
        val intervalMonths = state.monthlyIntervalText.toIntOrNull() ?: return null
        val dayOfMonth = state.monthlyDayOfMonthText.toIntOrNull() ?: return null
        if (intervalMonths !in MIN_MONTHLY_INTERVAL..MAX_MONTHLY_INTERVAL || dayOfMonth !in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH) {
            return null
        }
        return MedicationRecurrence.EveryMonthsOnDay(
            intervalMonths = intervalMonths,
            dayOfMonth = dayOfMonth,
        )
    }

    private fun validate(state: MedicationEditorUiState, forceErrors: Boolean = false): MedicationEditorUiState {
        val nameValid = state.nameText.trim().isNotEmpty()
        val dosageValid = state.dosageText.trim().isNotEmpty()
        val exactTimeParsed = parseTimeOfDay(state.exactTimeText)
        val exactTimeValid = !state.isExactTimeVisible || exactTimeParsed != null
        val courseStartDate = parseDateInput(state.courseStartDateText)
        val courseEndDateText = state.courseEndDateText.trim()
        val courseEndDate = courseEndDateText.takeIf { text -> text.isNotEmpty() }?.let(::parseDateInput)
        val courseStartDateValid = courseStartDate != null
        val courseEndDateFormatValid = courseEndDateText.isEmpty() || courseEndDate != null
        val courseDateRangeValid = courseStartDate != null && (courseEndDate == null || courseEndDate >= courseStartDate)
        val monthlyInterval = state.monthlyIntervalText.toIntOrNull()
        val monthlyDayOfMonth = state.monthlyDayOfMonthText.toIntOrNull()
        val monthlyIntervalValid = !state.isMonthlyRecurrence || monthlyInterval in MIN_MONTHLY_INTERVAL..MAX_MONTHLY_INTERVAL
        val monthlyDayOfMonthValid = !state.isMonthlyRecurrence || monthlyDayOfMonth in MIN_DAY_OF_MONTH..MAX_DAY_OF_MONTH
        val showNameError = !nameValid && (forceErrors || state.nameText.isNotEmpty())
        val showDosageError = !dosageValid && (forceErrors || state.dosageText.isNotEmpty())
        val showExactTimeError = state.isExactTimeVisible && exactTimeParsed == null &&
            (forceErrors || state.exactTimeText.isNotEmpty())
        val showCourseStartDateError = !courseStartDateValid && (forceErrors || state.courseStartDateText.isNotEmpty())
        val showCourseEndDateError = (!courseEndDateFormatValid || !courseDateRangeValid) &&
            (forceErrors || courseEndDateText.isNotEmpty())
        val showMonthlyIntervalError = !monthlyIntervalValid &&
            (forceErrors || state.monthlyIntervalText.isNotEmpty())
        val showMonthlyDayOfMonthError = !monthlyDayOfMonthValid &&
            (forceErrors || state.monthlyDayOfMonthText.isNotEmpty())
        return state.copy(
            nameError = if (showNameError) MedicationLabels.nameError else null,
            dosageError = if (showDosageError) MedicationLabels.dosageError else null,
            exactTimeError = if (showExactTimeError) UiLabels.timeError else null,
            courseStartDateError = if (showCourseStartDateError) UiLabels.dateError else null,
            courseEndDateError = when {
                !showCourseEndDateError -> null
                !courseEndDateFormatValid -> UiLabels.dateError
                else -> MedicationLabels.endDateError
            },
            monthlyIntervalError = if (showMonthlyIntervalError) MedicationLabels.repeatMonthsError else null,
            monthlyDayOfMonthError = if (showMonthlyDayOfMonthError) MedicationLabels.dayOfMonthError else null,
            isSaveEnabled = nameValid && dosageValid && exactTimeValid && courseStartDateValid && courseEndDateFormatValid &&
                courseDateRangeValid && monthlyIntervalValid && monthlyDayOfMonthValid && !state.isLoading,
        )
    }

    private companion object {
        const val MIN_MONTHLY_INTERVAL = 1
        const val MAX_MONTHLY_INTERVAL = 24
        const val MIN_DAY_OF_MONTH = 1
        const val MAX_DAY_OF_MONTH = 31
    }

}
