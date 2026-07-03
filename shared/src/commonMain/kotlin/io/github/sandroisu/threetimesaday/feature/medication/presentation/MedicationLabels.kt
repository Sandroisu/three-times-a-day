package io.github.sandroisu.threetimesaday.feature.medication.presentation

import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeMoment
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIntakeRule

fun medicationIntakeMomentLabel(intakeMoment: MedicationIntakeMoment): String = when (intakeMoment) {
    MedicationIntakeMoment.AfterWakeUp -> "После пробуждения"
    MedicationIntakeMoment.BeforeSleep -> "Перед сном"
    MedicationIntakeMoment.BeforeBreakfast -> "Перед завтраком"
    MedicationIntakeMoment.AfterBreakfast -> "После завтрака"
    MedicationIntakeMoment.BeforeLunch -> "Перед обедом"
    MedicationIntakeMoment.AfterLunch -> "После обеда"
    MedicationIntakeMoment.BeforeDinner -> "Перед ужином"
    MedicationIntakeMoment.AfterDinner -> "После ужина"
}

fun medicationIntakeRuleText(intakeRule: MedicationIntakeRule): String = when (intakeRule) {
    is MedicationIntakeRule.AtMoment -> medicationIntakeMomentLabel(intakeRule.moment)
    is MedicationIntakeRule.AtExactTime -> "В точное время ${formatTimeOfDay(intakeRule.time)}"
    is MedicationIntakeRule.SeveralTimesPerDay -> "Несколько раз в день"
}
