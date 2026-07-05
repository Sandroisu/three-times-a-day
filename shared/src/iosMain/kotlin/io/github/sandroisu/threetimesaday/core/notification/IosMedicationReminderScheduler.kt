package io.github.sandroisu.threetimesaday.core.notification

import kotlinx.coroutines.suspendCancellableCoroutine
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
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

class IosMedicationReminderScheduler : MedicationReminderScheduler {

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
            notificationCenter.requestAuthorizationWithOptions(options) { isGranted, _ ->
                val status = if (isGranted) {
                    NotificationPermissionStatus.Granted
                } else {
                    NotificationPermissionStatus.Denied
                }
                continuation.resume(status)
            }
        }

    override suspend fun scheduleReminder(notification: MedicationReminderNotification) {
        val content = UNMutableNotificationContent()
        content.setTitle(notification.title)
        content.setBody(notification.message)
        val dateComponents = NSDateComponents().apply {
            year = notification.scheduledDateTime.year.toLong()
            month = notification.scheduledDateTime.monthNumber.toLong()
            day = notification.scheduledDateTime.dayOfMonth.toLong()
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
            notificationCenter.addNotificationRequest(request) { _ ->
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun cancelReminder(notificationId: String) {
        val identifiers = listOf(notificationId)
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(identifiers)
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(identifiers)
    }

    override suspend fun cancelAllReminders() {
        suspendCancellableCoroutine { continuation ->
            notificationCenter.getPendingNotificationRequestsWithCompletionHandler { requests ->
                val identifiers = requests.orEmpty()
                    .mapNotNull { pendingRequest -> (pendingRequest as? UNNotificationRequest)?.identifier }
                    .filter { identifier -> identifier.startsWith(MEDICATION_REMINDER_ID_PREFIX) }
                if (identifiers.isNotEmpty()) {
                    notificationCenter.removePendingNotificationRequestsWithIdentifiers(identifiers)
                    notificationCenter.removeDeliveredNotificationsWithIdentifiers(identifiers)
                }
                continuation.resume(Unit)
            }
        }
    }
}
