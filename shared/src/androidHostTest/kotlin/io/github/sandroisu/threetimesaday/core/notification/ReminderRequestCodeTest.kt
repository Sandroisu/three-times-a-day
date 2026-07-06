package io.github.sandroisu.threetimesaday.core.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReminderRequestCodeTest {

    @Test
    fun reminderRequestCodeIsDeterministicForSameNotificationId() {
        assertEquals(
            reminderRequestCode("wake-event"),
            reminderRequestCode("wake-event")
        )
    }

    @Test
    fun differentNotificationIdsProduceDifferentRequestCodes() {
        assertNotEquals(
            reminderRequestCode("wake-event"),
            reminderRequestCode("sleep-event")
        )
    }

    @Test
    fun normalizeHandlesIntMinValueWithoutOverflow() {
        assertEquals(0, normalizeReminderRequestCode(Int.MIN_VALUE))
    }

    @Test
    fun normalizeReturnsNonNegativeForNegativeHashCode() {
        assertTrue(normalizeReminderRequestCode(-123) >= 0)
    }

    @Test
    fun normalizeReturnsSameValueForPositiveHashCode() {
        assertEquals(123, normalizeReminderRequestCode(123))
    }
}
