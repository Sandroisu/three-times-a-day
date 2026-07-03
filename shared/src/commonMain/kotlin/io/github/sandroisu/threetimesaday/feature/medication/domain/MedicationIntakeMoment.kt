package io.github.sandroisu.threetimesaday.feature.medication.domain

import kotlinx.serialization.Serializable

@Serializable
enum class MedicationIntakeMoment {
    AfterWakeUp,
    BeforeBreakfast,
    AfterBreakfast,
    BeforeLunch,
    AfterLunch,
    BeforeDinner,
    AfterDinner,
    BeforeSleep
}
