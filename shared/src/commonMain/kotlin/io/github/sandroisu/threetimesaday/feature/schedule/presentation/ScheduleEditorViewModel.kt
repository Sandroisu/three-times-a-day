package io.github.sandroisu.threetimesaday.feature.schedule.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.core.time.parseTimeOfDay
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import kotlinx.datetime.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScheduleEditorViewModel(
    private val dailyScheduleRepository: DailyScheduleRepository
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(ScheduleEditorUiState(isLoading = true))
    val uiState: StateFlow<ScheduleEditorUiState> = mutableUiState.asStateFlow()

    private val scheduleSavedEventsChannel = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scheduleSavedEvents: SharedFlow<Unit> = scheduleSavedEventsChannel.asSharedFlow()

    init {
        loadSchedule()
    }

    fun onWakeUpTimeChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(wakeUpTimeText = text)) }
    }

    fun onBreakfastTimeChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(breakfastTimeText = text)) }
    }

    fun onLunchTimeChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(lunchTimeText = text)) }
    }

    fun onDinnerTimeChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(dinnerTimeText = text)) }
    }

    fun onSleepTimeChanged(text: String) {
        mutableUiState.update { currentState -> validate(currentState.copy(sleepTimeText = text)) }
    }

    fun save() {
        val currentState = validate(mutableUiState.value)
        mutableUiState.value = currentState
        val validatedSchedule = buildScheduleOrNull(currentState) ?: return
        viewModelScope.launch {
            try {
                dailyScheduleRepository.saveDailySchedule(validatedSchedule)
                scheduleSavedEventsChannel.tryEmit(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (saveFailure: Exception) {
                mutableUiState.update { stateBeforeFailure ->
                    stateBeforeFailure.copy(
                        generalErrorMessage = saveFailure.message ?: "Не удалось сохранить режим дня"
                    )
                }
            }
        }
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            mutableUiState.update { currentState ->
                currentState.copy(isLoading = true, generalErrorMessage = null)
            }
            try {
                val dailySchedule = dailyScheduleRepository.getDailySchedule()
                mutableUiState.update { currentState ->
                    validate(
                        currentState.copy(
                            isLoading = false,
                            wakeUpTimeText = formatTimeOfDay(dailySchedule.wakeUpTime),
                            breakfastTimeText = formatTimeOfDay(dailySchedule.breakfastTime),
                            lunchTimeText = formatTimeOfDay(dailySchedule.lunchTime),
                            dinnerTimeText = formatTimeOfDay(dailySchedule.dinnerTime),
                            sleepTimeText = formatTimeOfDay(dailySchedule.sleepTime),
                            generalErrorMessage = null
                        )
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (loadFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        generalErrorMessage = loadFailure.message ?: "Не удалось загрузить режим дня"
                    )
                }
            }
        }
    }

    private fun validate(state: ScheduleEditorUiState): ScheduleEditorUiState {
        val wakeUpTime = parseTimeOfDay(state.wakeUpTimeText)
        val breakfastTime = parseTimeOfDay(state.breakfastTimeText)
        val lunchTime = parseTimeOfDay(state.lunchTimeText)
        val dinnerTime = parseTimeOfDay(state.dinnerTimeText)
        val sleepTime = parseTimeOfDay(state.sleepTimeText)
        val allValid = wakeUpTime != null &&
            breakfastTime != null &&
            lunchTime != null &&
            dinnerTime != null &&
            sleepTime != null
        return state.copy(
            wakeUpTimeError = errorFor(wakeUpTime),
            breakfastTimeError = errorFor(breakfastTime),
            lunchTimeError = errorFor(lunchTime),
            dinnerTimeError = errorFor(dinnerTime),
            sleepTimeError = errorFor(sleepTime),
            isSaveEnabled = allValid && !state.isLoading
        )
    }

    private fun buildScheduleOrNull(state: ScheduleEditorUiState): DailySchedule? {
        val wakeUpTime = parseTimeOfDay(state.wakeUpTimeText)
        val breakfastTime = parseTimeOfDay(state.breakfastTimeText)
        val lunchTime = parseTimeOfDay(state.lunchTimeText)
        val dinnerTime = parseTimeOfDay(state.dinnerTimeText)
        val sleepTime = parseTimeOfDay(state.sleepTimeText)
        if (wakeUpTime == null ||
            breakfastTime == null ||
            lunchTime == null ||
            dinnerTime == null ||
            sleepTime == null
        ) {
            return null
        }
        return DailySchedule(
            wakeUpTime = wakeUpTime,
            breakfastTime = breakfastTime,
            lunchTime = lunchTime,
            dinnerTime = dinnerTime,
            sleepTime = sleepTime
        )
    }

    private fun errorFor(parsedTime: LocalTime?): String? =
        if (parsedTime == null) TIME_FORMAT_ERROR_MESSAGE else null

    private companion object {
        const val TIME_FORMAT_ERROR_MESSAGE = "Введите время в формате HH:mm"
    }
}
