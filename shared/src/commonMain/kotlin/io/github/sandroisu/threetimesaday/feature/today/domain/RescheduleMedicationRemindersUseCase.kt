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
            medicationReminderScheduler.cancelAllReminders()
        }
        val nextScheduledDateTime = findNextScheduledDateTime(
            currentDateTime = currentDateTime,
            dailySchedule = dailySchedule,
            medications = medications,
        ) ?: return
        eventsForScheduledDateTime(
            scheduledDateTime = nextScheduledDateTime,
            currentDateTime = currentDateTime,
            dailySchedule = dailySchedule,
            medications = medications,
        ).forEach { event -> medicationReminderScheduler.scheduleReminder(buildNotification(event)) }
    }

    private suspend fun findNextScheduledDateTime(
        currentDateTime: LocalDateTime,
        dailySchedule: DailySchedule,
        medications: List<Medication>,
    ): LocalDateTime? {
        for (date in datesToSearch(currentDateTime.date)) {
            val generatedEvents = generateMedicationIntakeEventsForDate(date, dailySchedule, medications)
            val records = medicationIntakeRecordRepository.getRecordsForDate(date)
            val intakeEvents = applyMedicationIntakeRecords(generatedEvents, records)
            val nextEvent = intakeEvents
                .filter { event ->
                    event.status == MedicationIntakeStatus.Scheduled ||
                        event.status == MedicationIntakeStatus.Postponed
                }
                .firstOrNull { event -> event.scheduledDateTime > currentDateTime }
            if (nextEvent != null) {
                return nextEvent.scheduledDateTime
            }
        }
        return null
    }

    private suspend fun eventsForScheduledDateTime(
        scheduledDateTime: LocalDateTime,
        currentDateTime: LocalDateTime,
        dailySchedule: DailySchedule,
        medications: List<Medication>,
    ): List<MedicationIntakeEvent> {
        val generatedEvents = generateMedicationIntakeEventsForDate(
            scheduledDateTime.date,
            dailySchedule,
            medications,
        )
        val records = medicationIntakeRecordRepository.getRecordsForDate(scheduledDateTime.date)
        return applyMedicationIntakeRecords(generatedEvents, records)
            .filter { event ->
                (event.status == MedicationIntakeStatus.Scheduled || event.status == MedicationIntakeStatus.Postponed) &&
                    event.scheduledDateTime == scheduledDateTime && event.scheduledDateTime > currentDateTime
            }
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
    }
}
