package io.github.sandroisu.threetimesaday.core.notification

internal fun reminderRequestCode(notificationId: String): Int =
    normalizeReminderRequestCode(notificationId.hashCode())

internal fun normalizeReminderRequestCode(rawHashCode: Int): Int = when {
    rawHashCode == Int.MIN_VALUE -> 0
    rawHashCode < 0 -> -rawHashCode
    else -> rawHashCode
}
