package io.github.sandroisu.threetimesaday.feature.today.presentation

import io.github.sandroisu.threetimesaday.core.notification.NotificationPermissionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun promptsProvideNonEmptyMessageAndLabel() {
        listOf(
            NotificationPermissionStatus.NotDetermined,
            NotificationPermissionStatus.Denied
        ).forEach { status ->
            val prompt = notificationPermissionPrompt(status)
            assertEquals(true, prompt?.message?.isNotBlank())
            assertEquals(true, prompt?.actionLabel?.isNotBlank())
        }
    }
}
