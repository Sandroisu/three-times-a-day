package io.github.sandroisu.threetimesaday.feature.today.domain

import kotlinx.datetime.LocalDate

internal enum class IntakeHistoryPeriod(val dayCount: Int) {
    LastSevenDays(7),
    LastThirtyDays(30),
}

internal data class IntakeAdherence(
    val takenCount: Int,
    val skippedCount: Int,
    val missedCount: Int,
) {
    val dueCount: Int get() = takenCount + skippedCount + missedCount

    // Null when nothing was due. "No data" must not look like 0%.
    // Rounded down, so a single missed dose never shows as 100%.
    val takenPercent: Int? get() = if (dueCount == 0) null else takenCount * FULL_PERCENT / dueCount

    private companion object {
        const val FULL_PERCENT = 100
    }
}

internal data class IntakeHistoryDay(
    val date: LocalDate,
    val adherence: IntakeAdherence,
    // Intakes of today that are not due yet. They do not count in adherence.
    val pendingCount: Int,
)

internal data class MedicationAdherence(
    val medicationId: String,
    val medicationName: String,
    val adherence: IntakeAdherence,
)

internal sealed interface IntakeHistoryResult {

    data object NoMedications : IntakeHistoryResult

    data class Loaded(
        val days: List<IntakeHistoryDay>,
        val overall: IntakeAdherence,
        val medications: List<MedicationAdherence>,
    ) : IntakeHistoryResult
}
