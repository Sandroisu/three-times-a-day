package io.github.sandroisu.threetimesaday.core.notification

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val NOTIFICATION_USER_INFO_ID_KEY = "notificationId"
const val NOTIFICATION_USER_INFO_EVENT_ID_KEY = "eventId"

class IosMedicationReminderScheduler(
    private val alarmBridge: IosAlarmBridge? = null,
) : MedicationReminderScheduler {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun getPermissionStatus(): NotificationPermissionStatus =
        suspendCancellableCoroutine { continuation ->
            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                val status = when (settings?.authorizationStatus) {
                    UNAuthorizationStatusAuthorized,
                    UNAuthorizationStatusProvisional,
                    UNAuthorizationStatusEphemeral -> NotificationPermissionStatus.Granted

                    UNAuthorizationStatusDenied -> NotificationPermissionStatus.Denied
                    UNAuthorizationStatusNotDetermined -> NotificationPermissionStatus.NotDetermined
                    else -> NotificationPermissionStatus.NotDetermined
                }
                continuation.resume(status)
            }
        }

    override suspend fun requestPermission(): NotificationPermissionStatus =
        suspendCancellableCoroutine { continuation ->
            val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            notificationCenter.requestAuthorizationWithOptions(options) { isGranted, error ->
                if (error != null) {
                    continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                    return@requestAuthorizationWithOptions
                }
                val status = if (isGranted) {
                    NotificationPermissionStatus.Granted
                } else {
                    NotificationPermissionStatus.Denied
                }
                continuation.resume(status)
            }
        }

    override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
        if (notification.deliveryMode is NotificationDeliveryMode.Alarm && alarmBridge != null) {
            alarmBridge.scheduleAlarm(
                notificationId = notification.notificationId,
                title = notification.title,
                message = notification.message,
                triggerAtEpochMilliseconds = notification.scheduledDateTime
                    .toInstant(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds(),
            )
            return
        }
        val content = UNMutableNotificationContent()
        content.setTitle(notification.title)
        content.setBody(notification.message)
        content.setSound(UNNotificationSound.defaultSound)
        val userInfo = mutableMapOf<Any?, Any?>(NOTIFICATION_USER_INFO_ID_KEY to notification.notificationId)
        if (notification.notificationId.startsWith(MEDICATION_REMINDER_ID_PREFIX)) {
            userInfo[NOTIFICATION_USER_INFO_EVENT_ID_KEY] = notification.notificationId.removePrefix(MEDICATION_REMINDER_ID_PREFIX)
        }
        content.setUserInfo(userInfo)
        val dateComponents = NSDateComponents().apply {
            year = notification.scheduledDateTime.year.toLong()
            month = (notification.scheduledDateTime.month.ordinal + 1).toLong()
            day = notification.scheduledDateTime.day.toLong()
            hour = notification.scheduledDateTime.hour.toLong()
            minute = notification.scheduledDateTime.minute.toLong()
            second = notification.scheduledDateTime.second.toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(dateComponents, false)
        val request = UNNotificationRequest.requestWithIdentifier(
            notification.notificationId,
            content,
            trigger
        )
        suspendCancellableCoroutine { continuation ->
            notificationCenter.addNotificationRequest(request) { error ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                }
            }
        }
    }

    override suspend fun cancelReminder(notificationId: String) {
        alarmBridge?.cancelAlarm(notificationId)
        val identifiers = listOf(notificationId)
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(identifiers)
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(identifiers)
    }

    override suspend fun cancelAllReminders() {
        cancelRemindersWithPrefix(MEDICATION_REMINDER_ID_PREFIX)
        cancelRemindersWithPrefix(REMINDER_NOTIFICATION_ID_PREFIX)
    }

    override suspend fun cancelRemindersWithPrefix(notificationIdPrefix: String) {
        alarmBridge?.cancelAlarmsWithPrefix(notificationIdPrefix)
        suspendCancellableCoroutine { continuation ->
            notificationCenter.getPendingNotificationRequestsWithCompletionHandler { requests ->
                val identifiers = requests.orEmpty()
                    .mapNotNull { pendingRequest -> (pendingRequest as? UNNotificationRequest)?.identifier }
                    .filter { identifier -> identifier.startsWith(notificationIdPrefix) }
                if (identifiers.isNotEmpty()) {
                    notificationCenter.removePendingNotificationRequestsWithIdentifiers(identifiers)
                    notificationCenter.removeDeliveredNotificationsWithIdentifiers(identifiers)
                }
                continuation.resume(Unit)
            }
        }
    }
}
