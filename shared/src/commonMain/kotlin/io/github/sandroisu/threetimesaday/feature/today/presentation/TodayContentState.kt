package io.github.sandroisu.threetimesaday.feature.today.presentation

enum class TodayContentState {
    Loading,
    Error,
    Empty,
    Events
}

fun todayContentState(
    isLoading: Boolean,
    hasErrorMessage: Boolean,
    hasEvents: Boolean
): TodayContentState = when {
    isLoading -> TodayContentState.Loading
    hasErrorMessage -> TodayContentState.Error
    !hasEvents -> TodayContentState.Empty
    else -> TodayContentState.Events
}
