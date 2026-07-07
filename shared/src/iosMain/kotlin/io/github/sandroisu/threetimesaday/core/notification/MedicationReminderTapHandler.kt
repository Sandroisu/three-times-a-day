package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.NsUserDefaultsKeyValueStorage
import kotlinx.serialization.json.Json

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

    fun handleNotificationResponse(userInfo: Map<Any?, *>?) {
        val launchData = parseMedicationReminderLaunchData(userInfo) ?: return
        val storage: KeyValueStorage = NsUserDefaultsKeyValueStorage()
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        writeMedicationReminderLaunchData(storage, json, launchData)
    }
}
