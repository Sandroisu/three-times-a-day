package io.github.sandroisu.threetimesaday.core.notification

interface MedicationReminderLaunchRepository {

    suspend fun saveLaunchData(launchData: MedicationReminderLaunchData)

    suspend fun consumeLaunchData(): MedicationReminderLaunchData?
}
