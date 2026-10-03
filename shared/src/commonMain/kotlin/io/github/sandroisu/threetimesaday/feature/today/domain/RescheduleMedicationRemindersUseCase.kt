package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.core.notification.MEDICATION_REMINDER_ID_PREFIX
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailySchedule
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

class RescheduleMedicationRemindersUseCase(
    private val dailyScheduleRepository: DailyScheduleRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationIntakeRecordRepository: MedicationIntakeRecordRepository,
    private val generateMedicationIntakeEventsForDate: GenerateMedicationIntakeEventsForDateUseCase,
    private val applyMedicationIntakeRecords: ApplyMedicationIntakeRecordsUseCase,
    private val medicationReminderScheduler: MedicationReminderScheduler,
    private val timeProvider: TimeProvider,
    private val buildReminderMessage: suspend (MedicationIntakeEvent) -> String,
) {

    suspend operator fun invoke(replaceExistingReminders: Boolean = true) {
        val currentDateTime = timeProvider.currentDateTime()
        val dailySchedule = dailyScheduleRepository.getDailySchedule()
        val medications = medicationRepository.getMedications()
        if (replaceExistingReminders) {
            medicationReminderScheduler.cancelRemindersWithPrefix(MEDICATION_REMINDER_ID_PREFIX)
        }
        if (medications.isEmpty()) {
            return
        }
        findUpcomingEvents(
            currentDateTime = currentDateTime,
            dailySchedule = dailySchedule,
            medications = medications,
        ).forEach { event -> medicationReminderScheduler.scheduleReminder(buildNotification(event)) }
    }

    private suspend fun findUpcomingEvents(
        currentDateTime: LocalDateTime,
        dailySchedule: DailySchedule,
        medications: List<Medication>,
    ): List<MedicationIntakeEvent> {
        val upcomingEvents = mutableListOf<MedicationIntakeEvent>()
        for (date in datesToSearch(currentDateTime.date)) {
            val generatedEvents = generateMedicationIntakeEventsForDate(date, dailySchedule, medications)
            if (generatedEvents.isEmpty()) {
                continue
            }
            val records = medicationIntakeRecordRepository.getRecordsForDate(date)
            val scheduledEvents = applyMedicationIntakeRecords(generatedEvents, records)
                .filter { event ->
                    event.status == MedicationIntakeStatus.Scheduled ||
                        event.status == MedicationIntakeStatus.Postponed
                }
                .filter { event -> event.scheduledDateTime > currentDateTime }
            upcomingEvents.addAll(scheduledEvents)
            if (upcomingEvents.size >= MAX_SCHEDULED_OCCURRENCES) {
                return upcomingEvents.sortedBy { event -> event.scheduledDateTime }
                    .take(MAX_SCHEDULED_OCCURRENCES)
            }
        }
        return upcomingEvents.sortedBy { event -> event.scheduledDateTime }
    }

    private fun datesToSearch(startDate: LocalDate): List<LocalDate> =
        (0..SCHEDULING_LOOKAHEAD_DAYS).map { dayOffset -> startDate.plus(dayOffset, DateTimeUnit.DAY) }

    private suspend fun buildNotification(event: MedicationIntakeEvent): MedicationReminderNotification =
        MedicationReminderNotification(
            notificationId = MEDICATION_REMINDER_ID_PREFIX + event.eventId,
            title = event.medicationName,
            message = buildReminderMessage(event),
            scheduledDateTime = event.scheduledDateTime
        )

    private companion object {
        const val SCHEDULING_LOOKAHEAD_DAYS = 25 * 31
        // Medication and personal reminders split iOS's 64 pending-notification slots.
        const val MAX_SCHEDULED_OCCURRENCES = 32
    }
}
