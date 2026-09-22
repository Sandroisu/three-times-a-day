package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.core.time.formatScreenDate
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.medication.domain.Medication
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRecurrence
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.course_bounded_schedule
import threetimesaday.shared.generated.resources.course_open_schedule
import threetimesaday.shared.generated.resources.medication_about
import threetimesaday.shared.generated.resources.medication_add
import threetimesaday.shared.generated.resources.medication_by_schedule
import threetimesaday.shared.generated.resources.medication_daily
import threetimesaday.shared.generated.resources.medication_day_of_month
import threetimesaday.shared.generated.resources.medication_day_of_month_error
import threetimesaday.shared.generated.resources.medication_days
import threetimesaday.shared.generated.resources.medication_delete
import threetimesaday.shared.generated.resources.medication_delete_error
import threetimesaday.shared.generated.resources.medication_delete_hint
import threetimesaday.shared.generated.resources.medication_delete_message
import threetimesaday.shared.generated.resources.medication_delete_title
import threetimesaday.shared.generated.resources.medication_details
import threetimesaday.shared.generated.resources.medication_distributed_hint
import threetimesaday.shared.generated.resources.medication_dosage
import threetimesaday.shared.generated.resources.medication_dosage_error
import threetimesaday.shared.generated.resources.medication_dosage_hint
import threetimesaday.shared.generated.resources.medication_edit
import threetimesaday.shared.generated.resources.medication_edit_medication
import threetimesaday.shared.generated.resources.medication_empty_message
import threetimesaday.shared.generated.resources.medication_empty_title
import threetimesaday.shared.generated.resources.medication_exact_time
import threetimesaday.shared.generated.resources.medication_end_date
import threetimesaday.shared.generated.resources.medication_end_date_error
import threetimesaday.shared.generated.resources.medication_list_error
import threetimesaday.shared.generated.resources.medication_list_subtitle
import threetimesaday.shared.generated.resources.medication_load_error
import threetimesaday.shared.generated.resources.medication_medications
import threetimesaday.shared.generated.resources.medication_name
import threetimesaday.shared.generated.resources.medication_name_error
import threetimesaday.shared.generated.resources.medication_new_course
import threetimesaday.shared.generated.resources.medication_new_medication
import threetimesaday.shared.generated.resources.medication_not_found
import threetimesaday.shared.generated.resources.medication_reminder
import threetimesaday.shared.generated.resources.medication_reminder_hint
import threetimesaday.shared.generated.resources.medication_monthly
import threetimesaday.shared.generated.resources.medication_recurrence_months
import threetimesaday.shared.generated.resources.medication_repeat_every_months
import threetimesaday.shared.generated.resources.medication_repeat_months_error
import threetimesaday.shared.generated.resources.medication_save_error
import threetimesaday.shared.generated.resources.medication_select_moment
import threetimesaday.shared.generated.resources.medication_short_month_hint
import threetimesaday.shared.generated.resources.medication_start_date
import threetimesaday.shared.generated.resources.moment_after_breakfast
import threetimesaday.shared.generated.resources.moment_after_dinner
import threetimesaday.shared.generated.resources.moment_after_lunch
import threetimesaday.shared.generated.resources.moment_after_wake_up
import threetimesaday.shared.generated.resources.moment_before_breakfast
import threetimesaday.shared.generated.resources.moment_before_dinner
import threetimesaday.shared.generated.resources.moment_before_lunch
import threetimesaday.shared.generated.resources.moment_before_sleep
import threetimesaday.shared.generated.resources.rule_distributed
import threetimesaday.shared.generated.resources.rule_exact_time

internal fun medicationIntakeMomentLabel(intakeMoment: MedicationIntakeMoment): UiText = when (intakeMoment) {
    MedicationIntakeMoment.AfterWakeUp -> UiText(Res.string.moment_after_wake_up)
    MedicationIntakeMoment.BeforeSleep -> UiText(Res.string.moment_before_sleep)
    MedicationIntakeMoment.BeforeBreakfast -> UiText(Res.string.moment_before_breakfast)
    MedicationIntakeMoment.AfterBreakfast -> UiText(Res.string.moment_after_breakfast)
    MedicationIntakeMoment.BeforeLunch -> UiText(Res.string.moment_before_lunch)
    MedicationIntakeMoment.AfterLunch -> UiText(Res.string.moment_after_lunch)
    MedicationIntakeMoment.BeforeDinner -> UiText(Res.string.moment_before_dinner)
    MedicationIntakeMoment.AfterDinner -> UiText(Res.string.moment_after_dinner)
}

internal fun medicationIntakeRuleText(intakeRule: MedicationIntakeRule): UiText = when (intakeRule) {
    is MedicationIntakeRule.AtMoment -> medicationIntakeMomentLabel(intakeRule.moment)
    is MedicationIntakeRule.AtExactTime -> UiText(Res.string.rule_exact_time, listOf(formatTimeOfDay(intakeRule.time)))
    is MedicationIntakeRule.SeveralTimesPerDay -> UiText(Res.string.rule_distributed, listOf(intakeRule.moments.size))
}

internal object MedicationLabels {
    val medications = UiText(Res.string.medication_medications)
    val listSubtitle = UiText(Res.string.medication_list_subtitle)
    val add = UiText(Res.string.medication_add)
    val emptyTitle = UiText(Res.string.medication_empty_title)
    val emptyMessage = UiText(Res.string.medication_empty_message)
    val newMedication = UiText(Res.string.medication_new_medication)
    val editMedication = UiText(Res.string.medication_edit_medication)
    val details = UiText(Res.string.medication_details)
    val name = UiText(Res.string.medication_name)
    val dosage = UiText(Res.string.medication_dosage)
    val dosageHint = UiText(Res.string.medication_dosage_hint)
    val about = UiText(Res.string.medication_about)
    val reminder = UiText(Res.string.medication_reminder)
    val reminderHint = UiText(Res.string.medication_reminder_hint)
    val bySchedule = UiText(Res.string.medication_by_schedule)
    val exactTime = UiText(Res.string.medication_exact_time)
    val selectMoment = UiText(Res.string.medication_select_moment)
    val days = UiText(Res.string.medication_days)
    val daily = UiText(Res.string.medication_daily)
    val monthly = UiText(Res.string.medication_monthly)
    val repeatEveryMonths = UiText(Res.string.medication_repeat_every_months)
    val repeatMonthsError = UiText(Res.string.medication_repeat_months_error)
    val dayOfMonth = UiText(Res.string.medication_day_of_month)
    val dayOfMonthError = UiText(Res.string.medication_day_of_month_error)
    val shortMonthHint = UiText(Res.string.medication_short_month_hint)
    val startDate = UiText(Res.string.medication_start_date)
    val endDate = UiText(Res.string.medication_end_date)
    val endDateError = UiText(Res.string.medication_end_date_error)
    val newCourse = UiText(Res.string.medication_new_course)
    val edit = UiText(Res.string.medication_edit)
    val delete = UiText(Res.string.medication_delete)
    val deleteTitle = UiText(Res.string.medication_delete_title)
    val deleteMessage = UiText(Res.string.medication_delete_message)
    val deleteHint = UiText(Res.string.medication_delete_hint)
    val distributedHint = UiText(Res.string.medication_distributed_hint)
    val loadError = UiText(Res.string.medication_load_error)
    val saveError = UiText(Res.string.medication_save_error)
    val deleteError = UiText(Res.string.medication_delete_error)
    val notFound = UiText(Res.string.medication_not_found)
    val nameError = UiText(Res.string.medication_name_error)
    val dosageError = UiText(Res.string.medication_dosage_error)
    val listError = UiText(Res.string.medication_list_error)
}

internal fun medicationCourseScheduleLabel(medication: Medication): UiText {
    val start = formatScreenDate(medication.courseStartDate)
    val end = medication.courseEndDate?.let { formatScreenDate(it) }
    return if (end == null) {
        UiText(Res.string.course_open_schedule, listOf(start))
    } else {
        UiText(Res.string.course_bounded_schedule, listOf(start, end))
    }
}

internal fun medicationRecurrenceLabel(recurrence: MedicationRecurrence): UiText = when (recurrence) {
    MedicationRecurrence.Daily -> MedicationLabels.daily
    is MedicationRecurrence.EveryMonthsOnDay -> UiText(
        Res.string.medication_recurrence_months,
        listOf(recurrence.intervalMonths, recurrence.dayOfMonth),
    )
}
