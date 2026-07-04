package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.presentation.medicationIntakeMomentLabel

fun intakeMomentLabel(intakeMoment: MedicationIntakeMoment?): String =
    if (intakeMoment == null) "По расписанию" else medicationIntakeMomentLabel(intakeMoment)

fun intakeStatusLabel(status: MedicationIntakeStatus): String = when (status) {
    MedicationIntakeStatus.Scheduled -> "Запланировано"
    MedicationIntakeStatus.Taken -> "Принято"
    MedicationIntakeStatus.Skipped -> "Пропущено"
    MedicationIntakeStatus.Postponed -> "Отложено"
}
