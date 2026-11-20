package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.logging.IosErrorLogger
import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.NsUserDefaultsKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.storageJson
import io.github.sandroisu.threetimesaday.core.time.SystemTimeProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import platform.UserNotifications.UNUserNotificationCenter

fun parseMedicationReminderLaunchData(userInfo: Map<Any?, *>?): MedicationReminderLaunchData? {
    if (userInfo == null) return null
    val notificationId = userInfo[NOTIFICATION_USER_INFO_ID_KEY] as? String ?: return null
    val eventId = userInfo[NOTIFICATION_USER_INFO_EVENT_ID_KEY] as? String ?: return null
    return MedicationReminderLaunchData(notificationId = notificationId, eventId = eventId)
}

internal fun writeMedicationReminderLaunchData(
    storage: KeyValueStorage,
    json: Json,
    launchData: MedicationReminderLaunchData
) {
    storage.putString(MEDICATION_REMINDER_LAUNCH_DATA_KEY, json.encodeToString(launchData))
}

object MedicationReminderTapHandler {

    // Swift must call completionHandler only after onComplete: iOS may suspend the app right after it.
    fun handleNotificationResponse(
        actionIdentifier: String,
        userInfo: Map<Any?, *>?,
        onComplete: () -> Unit,
    ) {
        val action = medicationReminderActionFromId(actionIdentifier)
        val notificationId = userInfo?.get(NOTIFICATION_USER_INFO_ID_KEY) as? String
        if (action == null || notificationId == null) {
            openTappedReminder(userInfo)
            onComplete()
            return
        }
        val errorLogger = IosErrorLogger()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                createMedicationReminderActionHandler(
                    keyValueStorage = NsUserDefaultsKeyValueStorage(),
                    medicationReminderScheduler = IosMedicationReminderScheduler(),
                    errorLogger = errorLogger,
                    timeProvider = SystemTimeProvider(),
                    buildReminderMessage = { event -> event.dosageText },
                ).handle(notificationId, action)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (actionFailure: Exception) {
                errorLogger.logError(LOG_TAG, "Could not handle reminder action", actionFailure)
            } finally {
                UNUserNotificationCenter.currentNotificationCenter()
                    .removeDeliveredNotificationsWithIdentifiers(listOf(notificationId))
                onComplete()
            }
        }
    }

    private fun openTappedReminder(userInfo: Map<Any?, *>?) {
        val launchData = parseMedicationReminderLaunchData(userInfo) ?: return
        val storage: KeyValueStorage = NsUserDefaultsKeyValueStorage()
        writeMedicationReminderLaunchData(storage, storageJson, launchData)
    }

    private const val LOG_TAG = "MedicationReminderAction"
}
