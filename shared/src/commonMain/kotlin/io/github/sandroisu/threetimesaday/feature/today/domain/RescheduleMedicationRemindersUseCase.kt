package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.core.notification.MEDICATION_REMINDER_ID_PREFIX
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderNotification
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderScheduler
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.core.time.plusMinutes
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class RescheduleMedicationRemindersUseCase(
    private val dailyScheduleRepository: DailyScheduleRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationIntakeRecordRepository: MedicationIntakeRecordRepository,
    private val generateMedicationIntakeEventsForDate: GenerateMedicationIntakeEventsForDateUseCase,
    private val applyMedicationIntakeRecords: ApplyMedicationIntakeRecordsUseCase,
    private val medicationReminderScheduler: MedicationReminderScheduler,
    private val timeProvider: TimeProvider,
    private val buildReminderMessage: (MedicationIntakeEvent) -> String
) {

    suspend operator fun invoke() {
        val currentDateTime = timeProvider.currentDateTime()
        val horizonEnd = plusMinutes(currentDateTime, HORIZON_MINUTES)
        val dailySchedule = dailyScheduleRepository.getDailySchedule()
        val medications = medicationRepository.getMedications()
        medicationReminderScheduler.cancelAllReminders()
        horizonDates(currentDateTime.date).forEach { date ->
            val generatedEvents = generateMedicationIntakeEventsForDate(date, dailySchedule, medications)
            val records = medicationIntakeRecordRepository.getRecordsForDate(date)
            val intakeEvents = applyMedicationIntakeRecords(generatedEvents, records)
            intakeEvents
                .filter { event ->
                    event.status == MedicationIntakeStatus.Scheduled ||
                        event.status == MedicationIntakeStatus.Postponed
                }
                .filter { event ->
                    event.scheduledDateTime > currentDateTime && event.scheduledDateTime <= horizonEnd
                }
                .forEach { event -> medicationReminderScheduler.scheduleReminder(buildNotification(event)) }
        }
    }

    private fun horizonDates(startDate: LocalDate): List<LocalDate> =
        (0 until HORIZON_DAY_SPAN).map { dayOffset -> startDate.plus(dayOffset, DateTimeUnit.DAY) }

    private fun buildNotification(event: MedicationIntakeEvent): MedicationReminderNotification =
        MedicationReminderNotification(
            notificationId = MEDICATION_REMINDER_ID_PREFIX + event.eventId,
            title = event.medicationName,
            message = buildReminderMessage(event),
            scheduledDateTime = event.scheduledDateTime
        )

    private companion object {
        const val HORIZON_MINUTES = 48 * 60
        const val HORIZON_DAY_SPAN = 3
    }
}
