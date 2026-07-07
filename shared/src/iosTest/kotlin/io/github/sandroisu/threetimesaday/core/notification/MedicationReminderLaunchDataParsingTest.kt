package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MedicationReminderLaunchDataParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun returnsLaunchDataWhenBothKeysPresent() {
        val userInfo: Map<Any?, Any?> = mapOf(
            NOTIFICATION_USER_INFO_ID_KEY to "medication-reminder|event-123",
            NOTIFICATION_USER_INFO_EVENT_ID_KEY to "event-123"
        )

        val result = parseMedicationReminderLaunchData(userInfo)

        assertEquals(
            MedicationReminderLaunchData(
                notificationId = "medication-reminder|event-123",
                eventId = "event-123"
            ),
            result
        )
    }

    @Test
    fun returnsNullWhenUserInfoIsNull() {
        assertNull(parseMedicationReminderLaunchData(null))
    }

    @Test
    fun returnsNullWhenUserInfoIsEmpty() {
        assertNull(parseMedicationReminderLaunchData(emptyMap<Any?, Any?>()))
    }

    @Test
    fun returnsNullWhenNotificationIdMissing() {
        val userInfo: Map<Any?, Any?> = mapOf(
            NOTIFICATION_USER_INFO_EVENT_ID_KEY to "event-123"
        )

        assertNull(parseMedicationReminderLaunchData(userInfo))
    }

    @Test
    fun returnsNullWhenEventIdMissing() {
        val userInfo: Map<Any?, Any?> = mapOf(
            NOTIFICATION_USER_INFO_ID_KEY to "medication-reminder|event-123"
        )

        assertNull(parseMedicationReminderLaunchData(userInfo))
    }

    @Test
    fun returnsNullWhenValuesHaveWrongType() {
        val userInfo: Map<Any?, Any?> = mapOf(
            NOTIFICATION_USER_INFO_ID_KEY to 42,
            NOTIFICATION_USER_INFO_EVENT_ID_KEY to listOf("event-123")
        )

        assertNull(parseMedicationReminderLaunchData(userInfo))
    }

    @Test
    fun writeMedicationReminderLaunchDataPersistsUnderSharedKey() {
        val storage = InMemoryKeyValueStorage()
        val launchData = MedicationReminderLaunchData(
            notificationId = "medication-reminder|event-123",
            eventId = "event-123"
        )

        writeMedicationReminderLaunchData(storage, json, launchData)

        val storedJson = storage.getString(MEDICATION_REMINDER_LAUNCH_DATA_KEY)
        assertEquals(json.encodeToString(launchData), storedJson)
    }

    @Test
    fun writeMedicationReminderLaunchDataIsConsumedByPersistentRepository() = runTest {
        val storage = InMemoryKeyValueStorage()
        val launchData = MedicationReminderLaunchData(
            notificationId = "medication-reminder|event-456",
            eventId = "event-456"
        )

        writeMedicationReminderLaunchData(storage, json, launchData)
        val consumed = PersistentMedicationReminderLaunchRepository(storage, json)
            .consumeLaunchData()

        assertEquals(launchData, consumed)
        assertNull(storage.getString(MEDICATION_REMINDER_LAUNCH_DATA_KEY))
    }
}
