package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import io.github.sandroisu.threetimesaday.core.ui.UiText
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.notification_denied_message
import threetimesaday.shared.generated.resources.notification_request_action
import threetimesaday.shared.generated.resources.notification_request_message
import threetimesaday.shared.generated.resources.notification_settings_action

enum class NotificationPermissionAction {
    Request,
    OpenSettings
}

internal data class NotificationPermissionPrompt(
    val message: UiText,
    val actionLabel: UiText,
    val action: NotificationPermissionAction
)

internal fun notificationPermissionPrompt(status: NotificationPermissionStatus): NotificationPermissionPrompt? =
    when (status) {
        NotificationPermissionStatus.NotDetermined -> NotificationPermissionPrompt(
            message = UiText(Res.string.notification_request_message),
            actionLabel = UiText(Res.string.notification_request_action),
            action = NotificationPermissionAction.Request
        )

        NotificationPermissionStatus.Denied -> NotificationPermissionPrompt(
            message = UiText(Res.string.notification_denied_message),
            actionLabel = UiText(Res.string.notification_settings_action),
            action = NotificationPermissionAction.OpenSettings
        )

        NotificationPermissionStatus.Granted,
        NotificationPermissionStatus.NotSupported -> null
    }
