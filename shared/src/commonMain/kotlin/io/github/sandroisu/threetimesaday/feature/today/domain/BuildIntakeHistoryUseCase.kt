package io.github.sandroisu.threetimesaday.feature.today.domain

import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

internal class BuildIntakeHistoryUseCase(
    private val timeProvider: TimeProvider,
    private val dailyScheduleRepository: DailyScheduleRepository,
    private val medicationRepository: MedicationRepository,
    private val medicationIntakeRecordRepository: MedicationIntakeRecordRepository,
    private val generateMedicationIntakeEventsForDate: GenerateMedicationIntakeEventsForDateUseCase,
    private val applyMedicationIntakeRecords: ApplyMedicationIntakeRecordsUseCase,
) {

    suspend operator fun invoke(period: IntakeHistoryPeriod): IntakeHistoryResult {
        val medications = medicationRepository.getMedications()
        if (medications.isEmpty()) {
            return IntakeHistoryResult.NoMedications
        }
        val currentDateTime = timeProvider.currentDateTime()
        val dailySchedule = dailyScheduleRepository.getDailySchedule()
        val outcomesByDay = (period.dayCount - 1 downTo 0).map { daysAgo ->
            val date = currentDateTime.date.minus(daysAgo, DateTimeUnit.DAY)
            val generatedEvents = generateMedicationIntakeEventsForDate(date, dailySchedule, medications)
            val events = if (generatedEvents.isEmpty()) {
                emptyList()
            } else {
                applyMedicationIntakeRecords(generatedEvents, medicationIntakeRecordRepository.getRecordsForDate(date))
            }
            date to events.map { event -> IntakeOutcome(event.medicationId, classify(event, currentDateTime)) }
        }
        val allOutcomes = outcomesByDay.flatMap { (_, outcomes) -> outcomes }
        return IntakeHistoryResult.Loaded(
            days = outcomesByDay.map { (date, outcomes) ->
                IntakeHistoryDay(
                    date = date,
                    adherence = adherenceOf(outcomes),
                    pendingCount = outcomes.count { outcome -> outcome.kind == IntakeOutcomeKind.Pending },
                )
            },
            overall = adherenceOf(allOutcomes),
            medications = medications.map { medication ->
                MedicationAdherence(
                    medicationId = medication.id,
                    medicationName = medication.name,
                    adherence = adherenceOf(allOutcomes.filter { outcome -> outcome.medicationId == medication.id }),
                )
            },
        )
    }

    private fun classify(event: MedicationIntakeEvent, currentDateTime: LocalDateTime): IntakeOutcomeKind =
        when (event.status) {
            MedicationIntakeStatus.Taken -> IntakeOutcomeKind.Taken
            MedicationIntakeStatus.Skipped -> IntakeOutcomeKind.Skipped
            MedicationIntakeStatus.Scheduled,
            MedicationIntakeStatus.Postponed ->
                if (isMedicationIntakeEventExpired(event, currentDateTime)) {
                    IntakeOutcomeKind.Missed
                } else {
                    IntakeOutcomeKind.Pending
                }
        }

    private fun adherenceOf(outcomes: List<IntakeOutcome>): IntakeAdherence = IntakeAdherence(
        takenCount = outcomes.count { outcome -> outcome.kind == IntakeOutcomeKind.Taken },
        skippedCount = outcomes.count { outcome -> outcome.kind == IntakeOutcomeKind.Skipped },
        missedCount = outcomes.count { outcome -> outcome.kind == IntakeOutcomeKind.Missed },
    )

    private data class IntakeOutcome(
        val medicationId: String,
        val kind: IntakeOutcomeKind,
    )

    private enum class IntakeOutcomeKind {
        Taken,
        Skipped,
        Missed,
        Pending,
    }
}
