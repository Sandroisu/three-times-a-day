package io.github.sandroisu.threetimesaday.feature.schedule.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.routine_breakfast
import threetimesaday.shared.generated.resources.routine_dinner
import threetimesaday.shared.generated.resources.routine_duplicate_error
import threetimesaday.shared.generated.resources.routine_load_error
import threetimesaday.shared.generated.resources.routine_lunch
import threetimesaday.shared.generated.resources.routine_save_error
import threetimesaday.shared.generated.resources.routine_sleep
import threetimesaday.shared.generated.resources.routine_subtitle
import threetimesaday.shared.generated.resources.routine_title
import threetimesaday.shared.generated.resources.routine_wake_up

internal object ScheduleLabels {
    val title = UiText(Res.string.routine_title)
    val subtitle = UiText(Res.string.routine_subtitle)
    val wakeUp = UiText(Res.string.routine_wake_up)
    val breakfast = UiText(Res.string.routine_breakfast)
    val lunch = UiText(Res.string.routine_lunch)
    val dinner = UiText(Res.string.routine_dinner)
    val sleep = UiText(Res.string.routine_sleep)
    val saveError = UiText(Res.string.routine_save_error)
    val loadError = UiText(Res.string.routine_load_error)
    val duplicateError = UiText(Res.string.routine_duplicate_error)
}
