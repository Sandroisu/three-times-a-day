package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import threetimesaday.shared.generated.resources.Res
import threetimesaday.shared.generated.resources.notification_denied_message
import threetimesaday.shared.generated.resources.notification_request_action
import threetimesaday.shared.generated.resources.notification_request_message
import threetimesaday.shared.generated.resources.notification_settings_action

class NotificationPermissionPromptTest {

    @Test
    fun notDeterminedRequestsPermission() {
        val prompt = notificationPermissionPrompt(NotificationPermissionStatus.NotDetermined)

        assertEquals(NotificationPermissionAction.Request, prompt?.action)
    }

    @Test
    fun deniedOpensSettings() {
        val prompt = notificationPermissionPrompt(NotificationPermissionStatus.Denied)

        assertEquals(NotificationPermissionAction.OpenSettings, prompt?.action)
    }

    @Test
    fun grantedShowsNoPrompt() {
        assertNull(notificationPermissionPrompt(NotificationPermissionStatus.Granted))
    }

    @Test
    fun notSupportedShowsNoPrompt() {
        assertNull(notificationPermissionPrompt(NotificationPermissionStatus.NotSupported))
    }

    @Test
    fun promptsProvideLocalizedMessageAndLabel() {
        listOf(
            NotificationPermissionStatus.NotDetermined,
            NotificationPermissionStatus.Denied
        ).forEach { status ->
            val prompt = notificationPermissionPrompt(status)
            assertEquals(
                if (status == NotificationPermissionStatus.NotDetermined) Res.string.notification_request_message else Res.string.notification_denied_message,
                prompt?.message?.resource,
            )
            assertEquals(
                if (status == NotificationPermissionStatus.NotDetermined) Res.string.notification_request_action else Res.string.notification_settings_action,
                prompt?.actionLabel?.resource,
            )
        }
    }
}
