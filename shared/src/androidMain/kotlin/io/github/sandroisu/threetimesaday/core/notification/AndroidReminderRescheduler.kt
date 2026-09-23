package io.github.sandroisu.threetimesaday.core.notification

import android.content.Context
import io.github.sandroisu.threetimesaday.core.storage.AndroidKeyValueStorage
import io.github.sandroisu.threetimesaday.core.time.SystemTimeProvider
import io.github.sandroisu.threetimesaday.feature.reminder.data.PersistentReminderRepository
import io.github.sandroisu.threetimesaday.feature.reminder.domain.FindNextReminderDateTimeUseCase
import io.github.sandroisu.threetimesaday.feature.reminder.domain.RescheduleRemindersUseCase
import kotlinx.serialization.json.Json

internal object AndroidReminderRescheduler {

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
        RescheduleRemindersUseCase(
            reminderRepository = PersistentReminderRepository(storage, json),
            findNextReminderDateTime = FindNextReminderDateTimeUseCase(),
            medicationReminderScheduler = reminderScheduler,
            timeProvider = SystemTimeProvider(),
            buildReminderMessage = { reminder -> reminder.title },
        ).invoke(replaceExistingReminders = deliveredNotificationId == null)
    }
}
