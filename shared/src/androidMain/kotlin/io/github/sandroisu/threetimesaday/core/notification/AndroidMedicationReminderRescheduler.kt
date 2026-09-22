package io.github.sandroisu.threetimesaday.core.notification

import android.content.Context
import io.github.sandroisu.threetimesaday.core.storage.AndroidKeyValueStorage
import io.github.sandroisu.threetimesaday.core.time.SystemTimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.data.PersistentMedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.data.PersistentDailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.today.data.PersistentMedicationIntakeRecordRepository
import io.github.sandroisu.threetimesaday.feature.today.domain.ApplyMedicationIntakeRecordsUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.GenerateMedicationIntakeEventsForDateUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.RescheduleMedicationRemindersUseCase
import kotlinx.serialization.json.Json

internal object AndroidMedicationReminderRescheduler {

    suspend fun reschedule(
        context: Context,
        deliveredNotificationId: String? = null,
    ) {
        val applicationContext = context.applicationContext
        val storage = AndroidKeyValueStorage(applicationContext)
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val reminderRegistry = PersistentMedicationReminderRegistry(storage, json)
        if (deliveredNotificationId != null) {
            reminderRegistry.removeReminderId(deliveredNotificationId)
        }
        val reminderScheduler = AndroidMedicationReminderScheduler(
            context = applicationContext,
            permissionController = AndroidNotificationPermissionController(),
            reminderRegistry = reminderRegistry,
        )
        RescheduleMedicationRemindersUseCase(
            dailyScheduleRepository = PersistentDailyScheduleRepository(storage, json),
            medicationRepository = PersistentMedicationRepository(storage, json),
            medicationIntakeRecordRepository = PersistentMedicationIntakeRecordRepository(storage, json),
            generateMedicationIntakeEventsForDate = GenerateMedicationIntakeEventsForDateUseCase(),
            applyMedicationIntakeRecords = ApplyMedicationIntakeRecordsUseCase(),
            medicationReminderScheduler = reminderScheduler,
            timeProvider = SystemTimeProvider(),
            buildReminderMessage = { event -> event.dosageText },
        ).invoke(replaceExistingReminders = deliveredNotificationId == null)
    }
}
