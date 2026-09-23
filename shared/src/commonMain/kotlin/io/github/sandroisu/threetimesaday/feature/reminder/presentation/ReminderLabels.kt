package io.github.sandroisu.threetimesaday.feature.reminder.presentation

import io.github.sandroisu.threetimesaday.core.time.formatScreenDate
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.core.ui.UiText
import io.github.sandroisu.threetimesaday.feature.reminder.domain.ReminderRecurrence
import kotlinx.datetime.LocalDateTime
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.date_time
import threetimesaday.shared.generated.resources.reminder_add
import threetimesaday.shared.generated.resources.reminder_day_of_month
import threetimesaday.shared.generated.resources.reminder_day_of_month_error
import threetimesaday.shared.generated.resources.reminder_date
import threetimesaday.shared.generated.resources.reminder_delete
import threetimesaday.shared.generated.resources.reminder_delete_error
import threetimesaday.shared.generated.resources.reminder_delete_message
import threetimesaday.shared.generated.resources.reminder_delete_title
import threetimesaday.shared.generated.resources.reminder_edit
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
import threetimesaday.shared.generated.resources.reminder_once
import threetimesaday.shared.generated.resources.reminder_recurrence_months
import threetimesaday.shared.generated.resources.reminder_recurrence_once
import threetimesaday.shared.generated.resources.reminder_reminders
import threetimesaday.shared.generated.resources.reminder_repeat
import threetimesaday.shared.generated.resources.reminder_repeat_every_months
import threetimesaday.shared.generated.resources.reminder_repeat_months_error
import threetimesaday.shared.generated.resources.reminder_save_error
import threetimesaday.shared.generated.resources.reminder_short_month_hint
import threetimesaday.shared.generated.resources.reminder_time
import threetimesaday.shared.generated.resources.reminder_title
import threetimesaday.shared.generated.resources.reminder_title_error

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
    val monthly = UiText(Res.string.reminder_monthly)
    val repeatEveryMonths = UiText(Res.string.reminder_repeat_every_months)
    val repeatMonthsError = UiText(Res.string.reminder_repeat_months_error)
    val dayOfMonth = UiText(Res.string.reminder_day_of_month)
    val dayOfMonthError = UiText(Res.string.reminder_day_of_month_error)
    val shortMonthHint = UiText(Res.string.reminder_short_month_hint)
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
}

internal fun reminderRecurrenceLabel(recurrence: ReminderRecurrence): UiText = when (recurrence) {
    ReminderRecurrence.Once -> UiText(Res.string.reminder_recurrence_once)
    is ReminderRecurrence.EveryMonthsOnDay -> UiText(
        Res.string.reminder_recurrence_months,
        listOf(recurrence.intervalMonths, recurrence.dayOfMonth),
    )
}

internal fun reminderNextLabel(dateTime: LocalDateTime): UiText = UiText(
    Res.string.reminder_next,
    listOf(UiText(Res.string.date_time, listOf(formatScreenDate(dateTime.date), formatTimeOfDay(dateTime.time)))),
)
