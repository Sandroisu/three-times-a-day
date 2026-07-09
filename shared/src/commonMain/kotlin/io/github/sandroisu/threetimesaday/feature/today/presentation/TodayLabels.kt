package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus
import io.github.sandroisu.threetimesaday.feature.medication.presentation.medicationIntakeMomentLabel

const val MEDICATION_POSTPONE_MINUTES = 10

fun postponeActionLabel(): String = "Отложить на $MEDICATION_POSTPONE_MINUTES мин"

fun overdueLabel(): String = "Просрочено"

fun intakeMomentLabel(intakeMoment: MedicationIntakeMoment?): String =
    if (intakeMoment == null) "По расписанию" else medicationIntakeMomentLabel(intakeMoment)

fun intakeStatusLabel(status: MedicationIntakeStatus): String = when (status) {
    MedicationIntakeStatus.Scheduled -> "Запланировано"
    MedicationIntakeStatus.Taken -> "Принято"
    MedicationIntakeStatus.Skipped -> "Пропущено"
    MedicationIntakeStatus.Postponed -> "Отложено"
}
