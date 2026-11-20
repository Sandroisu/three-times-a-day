package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.logging.ErrorLogger
import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.storageJson
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.data.PersistentMedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.data.PersistentDailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.today.data.PersistentMedicationIntakeRecordRepository
import io.github.sandroisu.threetimesaday.feature.today.domain.ApplyMedicationIntakeRecordsUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.BuildMedicationIntakeRecordUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.ChangeMedicationIntakeStatusUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.GenerateMedicationIntakeEventsForDateUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.HandleMedicationReminderActionUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeEvent
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationReminderAction
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationReminderActionResult
import io.github.sandroisu.threetimesaday.feature.today.domain.RescheduleMedicationRemindersUseCase
import kotlinx.coroutines.CancellationException

internal const val MEDICATION_REMINDER_TAKEN_ACTION_ID = "medication-reminder-taken"
internal const val MEDICATION_REMINDER_POSTPONE_ACTION_ID = "medication-reminder-postpone"

internal fun notificationActions(notificationId: String): List<MedicationReminderAction> =
    if (notificationId.startsWith(MEDICATION_REMINDER_ID_PREFIX)) {
        MedicationReminderAction.entries
    } else {
        emptyList()
    }

internal fun medicationReminderActionFromId(actionId: String): MedicationReminderAction? = when (actionId) {
    MEDICATION_REMINDER_TAKEN_ACTION_ID -> MedicationReminderAction.Taken
    MEDICATION_REMINDER_POSTPONE_ACTION_ID -> MedicationReminderAction.Postpone
    else -> null
}

internal fun medicationReminderActionId(action: MedicationReminderAction): String = when (action) {
    MedicationReminderAction.Taken -> MEDICATION_REMINDER_TAKEN_ACTION_ID
    MedicationReminderAction.Postpone -> MEDICATION_REMINDER_POSTPONE_ACTION_ID
}

internal class MedicationReminderActionHandler(
    private val handleMedicationReminderAction: HandleMedicationReminderActionUseCase,
    private val errorLogger: ErrorLogger,
) {

    suspend fun handle(notificationId: String, action: MedicationReminderAction) {
        if (!notificationId.startsWith(MEDICATION_REMINDER_ID_PREFIX)) {
            logSkippedAction("Not a medication reminder: $notificationId", action)
            return
        }
        val eventId = notificationId.removePrefix(MEDICATION_REMINDER_ID_PREFIX)
        try {
            when (handleMedicationReminderAction(eventId, action)) {
                MedicationReminderActionResult.Applied,
                MedicationReminderActionResult.AlreadyResolved -> Unit

                MedicationReminderActionResult.EventNotFound ->
                    logSkippedAction("Intake not found: $eventId", action)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (actionFailure: Exception) {
            errorLogger.logError(LOG_TAG, "Could not apply $action to $eventId", actionFailure)
        }
    }

    // ErrorLogger needs a Throwable, and here nothing was thrown.
    private fun logSkippedAction(reason: String, action: MedicationReminderAction) {
        val message = "Skipped $action. $reason"
        errorLogger.logError(LOG_TAG, message, IllegalStateException(message))
    }

    private companion object {
        const val LOG_TAG = "MedicationReminderAction"
    }
}

internal fun createMedicationReminderActionHandler(
    keyValueStorage: KeyValueStorage,
    medicationReminderScheduler: MedicationReminderScheduler,
    errorLogger: ErrorLogger,
    timeProvider: TimeProvider,
    buildReminderMessage: suspend (MedicationIntakeEvent) -> String,
): MedicationReminderActionHandler {
    val json = storageJson
    val dailyScheduleRepository = PersistentDailyScheduleRepository(keyValueStorage, json)
    val medicationRepository = PersistentMedicationRepository(keyValueStorage, json)
    val medicationIntakeRecordRepository = PersistentMedicationIntakeRecordRepository(keyValueStorage, json)
    val generateMedicationIntakeEventsForDate = GenerateMedicationIntakeEventsForDateUseCase()
    val applyMedicationIntakeRecords = ApplyMedicationIntakeRecordsUseCase()
    val handleMedicationReminderAction = HandleMedicationReminderActionUseCase(
        timeProvider = timeProvider,
        dailyScheduleRepository = dailyScheduleRepository,
        medicationRepository = medicationRepository,
        medicationIntakeRecordRepository = medicationIntakeRecordRepository,
        generateMedicationIntakeEventsForDate = generateMedicationIntakeEventsForDate,
        applyMedicationIntakeRecords = applyMedicationIntakeRecords,
        buildMedicationIntakeRecord = BuildMedicationIntakeRecordUseCase(timeProvider),
        changeMedicationIntakeStatus = ChangeMedicationIntakeStatusUseCase(
            medicationRepository,
            medicationIntakeRecordRepository,
        ),
        rescheduleMedicationReminders = RescheduleMedicationRemindersUseCase(
            dailyScheduleRepository = dailyScheduleRepository,
            medicationRepository = medicationRepository,
            medicationIntakeRecordRepository = medicationIntakeRecordRepository,
            generateMedicationIntakeEventsForDate = generateMedicationIntakeEventsForDate,
            applyMedicationIntakeRecords = applyMedicationIntakeRecords,
            medicationReminderScheduler = medicationReminderScheduler,
            timeProvider = timeProvider,
            buildReminderMessage = buildReminderMessage,
        ),
    )
    return MedicationReminderActionHandler(handleMedicationReminderAction, errorLogger)
}
