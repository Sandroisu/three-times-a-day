package io.github.sandroisu.threetimesaday.feature.schedule.presentation

import io.github.sandroisu.threetimesaday.core.ui.UiText
internal data class ScheduleEditorUiState(
    val isLoading: Boolean = false,
    val wakeUpTimeText: String = "",
    val breakfastTimeText: String = "",
    val lunchTimeText: String = "",
    val dinnerTimeText: String = "",
    val sleepTimeText: String = "",
    val wakeUpTimeError: UiText? = null,
    val breakfastTimeError: UiText? = null,
    val lunchTimeError: UiText? = null,
    val dinnerTimeError: UiText? = null,
    val sleepTimeError: UiText? = null,
    val generalErrorMessage: UiText? = null,
    val isSaveEnabled: Boolean = false
)
