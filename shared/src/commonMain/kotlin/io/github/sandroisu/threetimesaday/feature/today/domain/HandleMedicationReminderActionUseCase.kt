package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

internal class HandleMedicationReminderActionUseCase(
    private val timeProvider: TimeProvider,
    private val dailyScheduleRepository: DailyScheduleRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationIntakeRecordRepository: MedicationIntakeRecordRepository,
    private val generateMedicationIntakeEventsForDate: GenerateMedicationIntakeEventsForDateUseCase,
    private val applyMedicationIntakeRecords: ApplyMedicationIntakeRecordsUseCase,
    private val buildMedicationIntakeRecord: BuildMedicationIntakeRecordUseCase,
    private val changeMedicationIntakeStatus: ChangeMedicationIntakeStatusUseCase,
    private val rescheduleMedicationReminders: RescheduleMedicationRemindersUseCase,
) {

    suspend operator fun invoke(
        eventId: String,
        action: MedicationReminderAction,
    ): MedicationReminderActionResult {
        val event = findEvent(eventId) ?: return MedicationReminderActionResult.EventNotFound
        if (event.status != MedicationIntakeStatus.Scheduled && event.status != MedicationIntakeStatus.Postponed) {
            return MedicationReminderActionResult.AlreadyResolved
        }
        val status = when (action) {
            MedicationReminderAction.Taken -> MedicationIntakeStatus.Taken
            MedicationReminderAction.Postpone -> MedicationIntakeStatus.Postponed
        }
        changeMedicationIntakeStatus(buildMedicationIntakeRecord(event, status))
        rescheduleMedicationReminders()
        return MedicationReminderActionResult.Applied
    }

    // A late dose or a postpone can move past midnight, so yesterday's intakes still count.
    private suspend fun findEvent(eventId: String): MedicationIntakeEvent? {
        val today = timeProvider.currentDate()
        val dailySchedule = dailyScheduleRepository.getDailySchedule()
        val medications = medicationRepository.getMedications()
        val event = listOf(today, today.minus(1, DateTimeUnit.DAY))
            .flatMap { date -> generateMedicationIntakeEventsForDate(date, dailySchedule, medications) }
            .firstOrNull { generatedEvent -> generatedEvent.eventId == eventId }
            ?: return null
        val record = medicationIntakeRecordRepository.getRecord(eventId)
        return applyMedicationIntakeRecords(listOf(event), listOfNotNull(record)).single()
    }
}
