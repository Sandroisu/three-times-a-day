package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.time.formatDayMonth
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.presentation.medicationIntakeMomentLabel
import kotlinx.datetime.LocalDateTime
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.date_time
import threetimesaday.shared.generated.resources.intake_by_schedule
import threetimesaday.shared.generated.resources.intake_description
import threetimesaday.shared.generated.resources.intake_due
import threetimesaday.shared.generated.resources.intake_overdue
import threetimesaday.shared.generated.resources.intake_postpone
import threetimesaday.shared.generated.resources.intake_postponed
import threetimesaday.shared.generated.resources.intake_progress
import threetimesaday.shared.generated.resources.intake_scheduled
import threetimesaday.shared.generated.resources.intake_skipped
import threetimesaday.shared.generated.resources.intake_taken
import threetimesaday.shared.generated.resources.intake_taken_at
import threetimesaday.shared.generated.resources.intake_upcoming
import threetimesaday.shared.generated.resources.time_value
import threetimesaday.shared.generated.resources.today_add_medication
import threetimesaday.shared.generated.resources.today_complete
import threetimesaday.shared.generated.resources.today_daily_progress
import threetimesaday.shared.generated.resources.today_empty_message
import threetimesaday.shared.generated.resources.today_empty_title
import threetimesaday.shared.generated.resources.today_exact_reminders
import threetimesaday.shared.generated.resources.today_exact_reminders_action
import threetimesaday.shared.generated.resources.today_load_error
import threetimesaday.shared.generated.resources.today_mark_taken
import threetimesaday.shared.generated.resources.today_medications
import threetimesaday.shared.generated.resources.today_next_intake
import threetimesaday.shared.generated.resources.today_no_upcoming
import threetimesaday.shared.generated.resources.today_notification_error
import threetimesaday.shared.generated.resources.today_permission_error
import threetimesaday.shared.generated.resources.today_plan
import threetimesaday.shared.generated.resources.today_retry
import threetimesaday.shared.generated.resources.today_save_error
import threetimesaday.shared.generated.resources.today_schedule
import threetimesaday.shared.generated.resources.today_skip
import threetimesaday.shared.generated.resources.today_title
import threetimesaday.shared.generated.resources.today_upcoming
import threetimesaday.shared.generated.resources.today_upcoming_error

const val MEDICATION_POSTPONE_MINUTES = 10

internal fun postponeActionLabel(): UiText = UiText(Res.string.intake_postpone, listOf(MEDICATION_POSTPONE_MINUTES))

internal fun overdueLabel(): UiText = UiText(Res.string.intake_overdue)

internal fun intakeMomentLabel(intakeMoment: MedicationIntakeMoment?): UiText =
    if (intakeMoment == null) UiText(Res.string.intake_by_schedule) else medicationIntakeMomentLabel(intakeMoment)

internal fun intakeStatusLabel(status: MedicationIntakeStatus): UiText = when (status) {
    MedicationIntakeStatus.Scheduled -> UiText(Res.string.intake_scheduled)
    MedicationIntakeStatus.Taken -> UiText(Res.string.intake_taken)
    MedicationIntakeStatus.Skipped -> UiText(Res.string.intake_skipped)
    MedicationIntakeStatus.Postponed -> UiText(Res.string.intake_postponed)
}

internal object TodayLabels {
    val title = UiText(Res.string.today_title)
    val schedule = UiText(Res.string.today_schedule)
    val medications = UiText(Res.string.today_medications)
    val dailyProgress = UiText(Res.string.today_daily_progress)
    val plan = UiText(Res.string.today_plan)
    val markTaken = UiText(Res.string.today_mark_taken)
    val skip = UiText(Res.string.today_skip)
    val emptyTitle = UiText(Res.string.today_empty_title)
    val emptyMessage = UiText(Res.string.today_empty_message)
    val addMedication = UiText(Res.string.today_add_medication)
    val exactReminders = UiText(Res.string.today_exact_reminders)
    val exactRemindersAction = UiText(Res.string.today_exact_reminders_action)
    val nextIntake = UiText(Res.string.today_next_intake)
    val upcoming = UiText(Res.string.today_upcoming)
    val noUpcoming = UiText(Res.string.today_no_upcoming)
    val upcomingError = UiText(Res.string.today_upcoming_error)
    val retry = UiText(Res.string.today_retry)
    val complete = UiText(Res.string.today_complete)
    val loadError = UiText(Res.string.today_load_error)
    val permissionError = UiText(Res.string.today_permission_error)
    val saveError = UiText(Res.string.today_save_error)
    val notificationError = UiText(Res.string.today_notification_error)
}

internal fun intakeProgressLabel(takenCount: Int, totalCount: Int): UiText = UiText(Res.string.intake_progress, listOf(takenCount, totalCount))

internal fun intakeDescriptionLabel(dosage: String, context: UiText): UiText =
    if (dosage.isBlank()) context else UiText(Res.string.intake_description, listOf(dosage, context))

internal fun intakeDisplayStatusLabel(status: IntakeDisplayStatus, takenAt: LocalDateTime?): UiText = when (status) {
    IntakeDisplayStatus.Taken -> takenAt?.let { UiText(Res.string.intake_taken_at, listOf(formatTimeOfDay(it.time))) } ?: UiText(Res.string.intake_taken)
    IntakeDisplayStatus.Skipped -> UiText(Res.string.intake_skipped)
    IntakeDisplayStatus.Upcoming -> UiText(Res.string.intake_upcoming)
    IntakeDisplayStatus.Due -> UiText(Res.string.intake_due)
    IntakeDisplayStatus.Overdue -> overdueLabel()
}

internal fun upcomingIntakeDateLabel(dateTime: LocalDateTime): UiText =
    UiText(Res.string.date_time, listOf(formatDayMonth(dateTime.date), formatTimeOfDay(dateTime.time)))

internal fun intakeGroupTimeLabel(dateTime: LocalDateTime, currentDateTime: LocalDateTime?): UiText =
    if (currentDateTime != null && dateTime.date != currentDateTime.date) {
        upcomingIntakeDateLabel(dateTime)
    } else {
        UiText(Res.string.time_value, listOf(formatTimeOfDay(dateTime.time)))
    }
