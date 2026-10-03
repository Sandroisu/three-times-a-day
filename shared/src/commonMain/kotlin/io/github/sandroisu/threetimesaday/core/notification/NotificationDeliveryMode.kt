package io.github.sandroisu.threetimesaday.core.notification

sealed interface NotificationDeliveryMode {

    data object Standard : NotificationDeliveryMode

    data class Alarm(
        val maxDurationMinutes: Int,
    ) : NotificationDeliveryMode
}
