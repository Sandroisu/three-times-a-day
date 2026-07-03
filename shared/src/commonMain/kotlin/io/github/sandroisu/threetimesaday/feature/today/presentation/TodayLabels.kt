package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeStatus

fun intakeMomentLabel(intakeMoment: MedicationIntakeMoment?): String = when (intakeMoment) {
    MedicationIntakeMoment.AfterWakeUp -> "После пробуждения"
    MedicationIntakeMoment.BeforeBreakfast -> "До завтрака"
    MedicationIntakeMoment.AfterBreakfast -> "После завтрака"
    MedicationIntakeMoment.BeforeLunch -> "До обеда"
    MedicationIntakeMoment.AfterLunch -> "После обеда"
    MedicationIntakeMoment.BeforeDinner -> "До ужина"
    MedicationIntakeMoment.AfterDinner -> "После ужина"
    MedicationIntakeMoment.BeforeSleep -> "Перед сном"
    null -> "По расписанию"
}

fun intakeStatusLabel(status: MedicationIntakeStatus): String = when (status) {
    MedicationIntakeStatus.Scheduled -> "Запланировано"
    MedicationIntakeStatus.Taken -> "Принято"
    MedicationIntakeStatus.Postponed -> "Отложено"
    MedicationIntakeStatus.Missed -> "Пропущено"
}
