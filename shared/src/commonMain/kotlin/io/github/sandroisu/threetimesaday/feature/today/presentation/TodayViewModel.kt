package io.github.sandroisu.threetimesaday.feature.today.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sandroisu.threetimesaday.core.notification.MEDICATION_REMINDER_ID_PREFIX
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.core.time.formatScreenDate
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.today.domain.ApplyMedicationIntakeRecordsUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.GenerateMedicationIntakeEventsForDateUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecord
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecordRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

class TodayViewModel(
    private val timeProvider: TimeProvider,
    private val dailyScheduleRepository: DailyScheduleRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationIntakeRecordRepository: MedicationIntakeRecordRepository,
    private val generateMedicationIntakeEventsForDate: GenerateMedicationIntakeEventsForDateUseCase,
    private val applyMedicationIntakeRecords: ApplyMedicationIntakeRecordsUseCase,
    private val medicationReminderScheduler: MedicationReminderScheduler
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = mutableUiState.asStateFlow()

    init {
        loadToday()
    }

    fun loadToday() {
        viewModelScope.launch {
            mutableUiState.update { currentState ->
                currentState.copy(isLoading = true, errorMessage = null)
            }
            try {
                val currentDate = timeProvider.currentDate()
                val dailySchedule = dailyScheduleRepository.getDailySchedule()
                val medications = medicationRepository.getMedications()
                val generatedEvents = generateMedicationIntakeEventsForDate(
                    date = currentDate,
                    dailySchedule = dailySchedule,
                    medications = medications
                )
                val records = medicationIntakeRecordRepository.getRecordsForDate(currentDate)
                val intakeEvents = applyMedicationIntakeRecords(generatedEvents, records)
                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        dateTitle = formatScreenDate(currentDate),
                        intakeEvents = intakeEvents,
                        errorMessage = null
                    )
                }
                refreshPermissionStatus()
                rescheduleReminders(intakeEvents)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (loadFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        errorMessage = loadFailure.message ?: "Не удалось загрузить приёмы"
                    )
                }
            }
        }
    }

    fun highlightEvent(eventId: String) {
        mutableUiState.update { currentState ->
            currentState.copy(highlightedEventId = eventId)
        }
    }

    fun clearHighlightedEvent() {
        mutableUiState.update { currentState ->
            currentState.copy(highlightedEventId = null)
        }
    }

    fun requestNotificationPermission() {
        viewModelScope.launch {
            try {
                val permissionStatus = medicationReminderScheduler.requestPermission()
                mutableUiState.update { currentState ->
                    currentState.copy(notificationPermissionStatus = permissionStatus)
                }
                loadToday()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (permissionFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        notificationErrorMessage = permissionFailure.message
                            ?: "Не удалось запросить разрешение на уведомления"
                    )
                }
            }
        }
    }

    fun markIntakeTaken(eventId: String) {
        applyStatus(eventId, MedicationIntakeStatus.Taken)
    }

    fun markIntakeSkipped(eventId: String) {
        applyStatus(eventId, MedicationIntakeStatus.Skipped)
    }

    fun postponeIntake(eventId: String) {
        applyStatus(eventId, MedicationIntakeStatus.Postponed)
    }

    private fun applyStatus(eventId: String, status: MedicationIntakeStatus) {
        val targetEvent = mutableUiState.value.intakeEvents.firstOrNull { event -> event.eventId == eventId }
            ?: return
        viewModelScope.launch {
            try {
                medicationIntakeRecordRepository.saveRecord(buildRecord(targetEvent, status))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (saveFailure: Exception) {
                mutableUiState.update { currentState ->
                    currentState.copy(
                        errorMessage = saveFailure.message ?: "Не удалось сохранить статус приёма"
                    )
                }
                return@launch
            }
            runReminderUpdate {
                medicationReminderScheduler.cancelReminder(reminderIdFor(eventId))
            }
            loadToday()
        }
    }

    private fun buildRecord(event: MedicationIntakeEvent, status: MedicationIntakeStatus): MedicationIntakeRecord {
        val updatedDateTime = timeProvider.currentDateTime()
        val postponedDateTime = if (status == MedicationIntakeStatus.Postponed) {
            plusMinutes(event.scheduledDateTime, POSTPONE_MINUTES)
        } else {
            null
        }
        return MedicationIntakeRecord(
            eventId = event.eventId,
            medicationId = event.medicationId,
            scheduledDateTime = event.scheduledDateTime,
            status = status,
            updatedDateTime = updatedDateTime,
            postponedDateTime = postponedDateTime
        )
    }

    private suspend fun refreshPermissionStatus() {
        runReminderUpdate {
            val permissionStatus = medicationReminderScheduler.getPermissionStatus()
            mutableUiState.update { currentState ->
                currentState.copy(notificationPermissionStatus = permissionStatus)
            }
        }
    }

    private suspend fun rescheduleReminders(intakeEvents: List<MedicationIntakeEvent>) {
        runReminderUpdate {
            medicationReminderScheduler.cancelAllReminders()
            val currentDateTime = timeProvider.currentDateTime()
            intakeEvents
                .filter { event -> event.status == MedicationIntakeStatus.Scheduled || event.status == MedicationIntakeStatus.Postponed }
                .filter { event -> event.scheduledDateTime > currentDateTime }
                .forEach { event -> medicationReminderScheduler.scheduleReminder(buildNotification(event)) }
        }
    }

    private fun buildNotification(event: MedicationIntakeEvent): MedicationReminderNotification =
        MedicationReminderNotification(
            notificationId = reminderIdFor(event.eventId),
            title = event.medicationName,
            message = "${event.dosageText} · ${intakeMomentLabel(event.intakeMoment)}",
            scheduledDateTime = event.scheduledDateTime
        )

    private suspend fun runReminderUpdate(action: suspend () -> Unit) {
        try {
            action()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (notificationFailure: Exception) {
            mutableUiState.update { currentState ->
                currentState.copy(
                    notificationErrorMessage = notificationFailure.message
                        ?: "Не удалось обновить уведомления"
                )
            }
        }
    }

    private fun reminderIdFor(eventId: String): String = MEDICATION_REMINDER_ID_PREFIX + eventId

    private fun plusMinutes(dateTime: LocalDateTime, minutes: Int): LocalDateTime {
        val totalSeconds = dateTime.time.toSecondOfDay() + minutes * SECONDS_IN_MINUTE
        val dayOffset = totalSeconds.floorDiv(SECONDS_IN_DAY)
        val secondOfDay = totalSeconds.mod(SECONDS_IN_DAY)
        val shiftedDate = dateTime.date.plus(dayOffset, DateTimeUnit.DAY)
        return LocalDateTime(shiftedDate, LocalTime.fromSecondOfDay(secondOfDay))
    }

    private companion object {
        const val POSTPONE_MINUTES = 10
        const val SECONDS_IN_MINUTE = 60
        const val SECONDS_IN_DAY = 24 * 60 * 60
    }
}
