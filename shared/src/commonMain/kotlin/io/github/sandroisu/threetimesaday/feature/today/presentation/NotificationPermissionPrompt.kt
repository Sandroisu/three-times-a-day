package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus

enum class NotificationPermissionAction {
    Request,
    OpenSettings
}

data class NotificationPermissionPrompt(
    val message: String,
    val actionLabel: String,
    val action: NotificationPermissionAction
)

fun notificationPermissionPrompt(status: NotificationPermissionStatus): NotificationPermissionPrompt? =
    when (status) {
        NotificationPermissionStatus.NotDetermined -> NotificationPermissionPrompt(
            message = "Чтобы приходили напоминания о приёме, разрешите уведомления.",
            actionLabel = "Включить уведомления",
            action = NotificationPermissionAction.Request
        )

        NotificationPermissionStatus.Denied -> NotificationPermissionPrompt(
            message = "Напоминания не будут приходить, пока уведомления запрещены. Разрешите их в настройках.",
            actionLabel = "Открыть настройки",
            action = NotificationPermissionAction.OpenSettings
        )

        NotificationPermissionStatus.Granted,
        NotificationPermissionStatus.NotSupported -> null
    }
