package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import io.github.sandroisu.threetimesaday.core.time.formatScreenDate
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRecurrence
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderWeekday
import kotlinx.datetime.LocalDateTime
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.date_time
import threetimesaday.shared.generated.resources.reminder_add
import threetimesaday.shared.generated.resources.reminder_alert_alarm
import threetimesaday.shared.generated.resources.reminder_alert_alarm_hint
import threetimesaday.shared.generated.resources.reminder_alert_normal
import threetimesaday.shared.generated.resources.reminder_alert_normal_hint
import threetimesaday.shared.generated.resources.reminder_alert_type
import threetimesaday.shared.generated.resources.reminder_cyclic
import threetimesaday.shared.generated.resources.reminder_cyclic_intervals
import threetimesaday.shared.generated.resources.reminder_cyclic_intervals_error
import threetimesaday.shared.generated.resources.reminder_cyclic_intervals_hint
import threetimesaday.shared.generated.resources.reminder_daily
import threetimesaday.shared.generated.resources.reminder_day_interval_error
import threetimesaday.shared.generated.resources.reminder_day_of_month
import threetimesaday.shared.generated.resources.reminder_day_of_month_error
import threetimesaday.shared.generated.resources.reminder_date
import threetimesaday.shared.generated.resources.reminder_delete
import threetimesaday.shared.generated.resources.reminder_delete_error
import threetimesaday.shared.generated.resources.reminder_delete_message
import threetimesaday.shared.generated.resources.reminder_delete_title
import threetimesaday.shared.generated.resources.reminder_edit
import threetimesaday.shared.generated.resources.reminder_every_days
import threetimesaday.shared.generated.resources.reminder_exact_reminders
import threetimesaday.shared.generated.resources.reminder_exact_reminders_action
import threetimesaday.shared.generated.resources.reminder_empty_message
import threetimesaday.shared.generated.resources.reminder_empty_title
import threetimesaday.shared.generated.resources.reminder_list_error
import threetimesaday.shared.generated.resources.reminder_list_subtitle
import threetimesaday.shared.generated.resources.reminder_load_error
import threetimesaday.shared.generated.resources.reminder_monthly
import threetimesaday.shared.generated.resources.reminder_new
import threetimesaday.shared.generated.resources.reminder_next
import threetimesaday.shared.generated.resources.reminder_not_found
import threetimesaday.shared.generated.resources.reminder_notification_message
import threetimesaday.shared.generated.resources.reminder_notification_error
import threetimesaday.shared.generated.resources.reminder_once
import threetimesaday.shared.generated.resources.reminder_recurrence_months
import threetimesaday.shared.generated.resources.reminder_recurrence_cyclic
import threetimesaday.shared.generated.resources.reminder_recurrence_daily
import threetimesaday.shared.generated.resources.reminder_recurrence_days
import threetimesaday.shared.generated.resources.reminder_recurrence_once
import threetimesaday.shared.generated.resources.reminder_recurrence_weekdays
import threetimesaday.shared.generated.resources.reminder_reminders
import threetimesaday.shared.generated.resources.reminder_repeat
import threetimesaday.shared.generated.resources.reminder_repeat_every_days
import threetimesaday.shared.generated.resources.reminder_repeat_every_months
import threetimesaday.shared.generated.resources.reminder_repeat_months_error
import threetimesaday.shared.generated.resources.reminder_save_error
import threetimesaday.shared.generated.resources.reminder_short_month_hint
import threetimesaday.shared.generated.resources.reminder_time
import threetimesaday.shared.generated.resources.reminder_title
import threetimesaday.shared.generated.resources.reminder_title_error
import threetimesaday.shared.generated.resources.reminder_weekday_fri
import threetimesaday.shared.generated.resources.reminder_weekday_mon
import threetimesaday.shared.generated.resources.reminder_weekday_sat
import threetimesaday.shared.generated.resources.reminder_weekday_sun
import threetimesaday.shared.generated.resources.reminder_weekday_thu
import threetimesaday.shared.generated.resources.reminder_weekday_tue
import threetimesaday.shared.generated.resources.reminder_weekday_wed
import threetimesaday.shared.generated.resources.reminder_weekdays
import threetimesaday.shared.generated.resources.reminder_weekdays_error

internal object ReminderLabels {
    val reminders = UiText(Res.string.reminder_reminders)
    val listSubtitle = UiText(Res.string.reminder_list_subtitle)
    val add = UiText(Res.string.reminder_add)
    val emptyTitle = UiText(Res.string.reminder_empty_title)
    val emptyMessage = UiText(Res.string.reminder_empty_message)
    val newReminder = UiText(Res.string.reminder_new)
    val editReminder = UiText(Res.string.reminder_edit)
    val title = UiText(Res.string.reminder_title)
    val date = UiText(Res.string.reminder_date)
    val time = UiText(Res.string.reminder_time)
    val repeat = UiText(Res.string.reminder_repeat)
    val once = UiText(Res.string.reminder_once)
    val daily = UiText(Res.string.reminder_daily)
    val weekdays = UiText(Res.string.reminder_weekdays)
    val everyDays = UiText(Res.string.reminder_every_days)
    val monthly = UiText(Res.string.reminder_monthly)
    val cyclic = UiText(Res.string.reminder_cyclic)
    val repeatEveryDays = UiText(Res.string.reminder_repeat_every_days)
    val dayIntervalError = UiText(Res.string.reminder_day_interval_error)
    val weekdaysError = UiText(Res.string.reminder_weekdays_error)
    val repeatEveryMonths = UiText(Res.string.reminder_repeat_every_months)
    val repeatMonthsError = UiText(Res.string.reminder_repeat_months_error)
    val dayOfMonth = UiText(Res.string.reminder_day_of_month)
    val dayOfMonthError = UiText(Res.string.reminder_day_of_month_error)
    val shortMonthHint = UiText(Res.string.reminder_short_month_hint)
    val cyclicIntervals = UiText(Res.string.reminder_cyclic_intervals)
    val cyclicIntervalsHint = UiText(Res.string.reminder_cyclic_intervals_hint)
    val cyclicIntervalsError = UiText(Res.string.reminder_cyclic_intervals_error)
    val alertType = UiText(Res.string.reminder_alert_type)
    val normalAlert = UiText(Res.string.reminder_alert_normal)
    val normalAlertHint = UiText(Res.string.reminder_alert_normal_hint)
    val alarmAlert = UiText(Res.string.reminder_alert_alarm)
    val alarmAlertHint = UiText(Res.string.reminder_alert_alarm_hint)
    val delete = UiText(Res.string.reminder_delete)
    val deleteTitle = UiText(Res.string.reminder_delete_title)
    val deleteMessage = UiText(Res.string.reminder_delete_message)
    val titleError = UiText(Res.string.reminder_title_error)
    val listError = UiText(Res.string.reminder_list_error)
    val loadError = UiText(Res.string.reminder_load_error)
    val saveError = UiText(Res.string.reminder_save_error)
    val deleteError = UiText(Res.string.reminder_delete_error)
    val notFound = UiText(Res.string.reminder_not_found)
    val notificationMessage = UiText(Res.string.reminder_notification_message)
    val notificationError = UiText(Res.string.reminder_notification_error)
    val exactReminders = UiText(Res.string.reminder_exact_reminders)
    val exactRemindersAction = UiText(Res.string.reminder_exact_reminders_action)
}

internal fun reminderRecurrenceLabel(recurrence: ReminderRecurrence): UiText = when (recurrence) {
    ReminderRecurrence.Once -> UiText(Res.string.reminder_recurrence_once)
    ReminderRecurrence.Daily -> UiText(Res.string.reminder_recurrence_daily)
    is ReminderRecurrence.OnWeekdays -> UiText(Res.string.reminder_recurrence_weekdays)
    is ReminderRecurrence.EveryDays -> UiText(Res.string.reminder_recurrence_days, listOf(recurrence.intervalDays))
    is ReminderRecurrence.EveryMonthsOnDay -> UiText(
        Res.string.reminder_recurrence_months,
        listOf(recurrence.intervalMonths, recurrence.dayOfMonth),
    )

    is ReminderRecurrence.CyclicDayIntervals -> UiText(
        Res.string.reminder_recurrence_cyclic,
        listOf(recurrence.intervals.joinToString(", ")),
    )
}

internal fun reminderRecurrenceTypeLabel(recurrenceType: ReminderRecurrenceType): UiText = when (recurrenceType) {
    ReminderRecurrenceType.Once -> ReminderLabels.once
    ReminderRecurrenceType.Daily -> ReminderLabels.daily
    ReminderRecurrenceType.Weekdays -> ReminderLabels.weekdays
    ReminderRecurrenceType.EveryDays -> ReminderLabels.everyDays
    ReminderRecurrenceType.EveryMonths -> ReminderLabels.monthly
    ReminderRecurrenceType.CyclicIntervals -> ReminderLabels.cyclic
}

internal fun reminderWeekdayShortLabel(weekday: ReminderWeekday): UiText = when (weekday) {
    ReminderWeekday.Monday -> UiText(Res.string.reminder_weekday_mon)
    ReminderWeekday.Tuesday -> UiText(Res.string.reminder_weekday_tue)
    ReminderWeekday.Wednesday -> UiText(Res.string.reminder_weekday_wed)
    ReminderWeekday.Thursday -> UiText(Res.string.reminder_weekday_thu)
    ReminderWeekday.Friday -> UiText(Res.string.reminder_weekday_fri)
    ReminderWeekday.Saturday -> UiText(Res.string.reminder_weekday_sat)
    ReminderWeekday.Sunday -> UiText(Res.string.reminder_weekday_sun)
}

internal fun reminderNextLabel(dateTime: LocalDateTime): UiText = UiText(
    Res.string.reminder_next,
    listOf(UiText(Res.string.date_time, listOf(formatScreenDate(dateTime.date), formatTimeOfDay(dateTime.time)))),
)
