package io.github.sandroisu.threetimesaday.feature.schedule.presentation

data class ScheduleEditorUiState(
    val isLoading: Boolean = false,
    val wakeUpTimeText: String = "",
    val breakfastTimeText: String = "",
    val lunchTimeText: String = "",
    val dinnerTimeText: String = "",
    val sleepTimeText: String = "",
    val wakeUpTimeError: String? = null,
    val breakfastTimeError: String? = null,
    val lunchTimeError: String? = null,
    val dinnerTimeError: String? = null,
    val sleepTimeError: String? = null,
    val generalErrorMessage: String? = null,
    val isSaveEnabled: Boolean = false
)
