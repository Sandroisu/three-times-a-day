package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent

interface MedicationReminderScheduler {

    suspend fun scheduleReminder(intakeEvent: MedicationIntakeEvent)

    suspend fun cancelReminder(medicationId: String)

    suspend fun cancelAllReminders()
}
