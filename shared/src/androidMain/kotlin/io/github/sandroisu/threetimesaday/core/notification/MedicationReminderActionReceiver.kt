package io.github.sandroisu.threetimesaday.core.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.sandroisu.threetimesaday.core.logging.AndroidErrorLogger
import io.github.sandroisu.threetimesaday.core.storage.AndroidKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.storageJson
import io.github.sandroisu.threetimesaday.core.time.SystemTimeProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class MedicationReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action?.let(::medicationReminderActionFromId) ?: return
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return
        val applicationContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val storage = AndroidKeyValueStorage(applicationContext)
                val reminderScheduler = AndroidMedicationReminderScheduler(
                    context = applicationContext,
                    permissionController = AndroidNotificationPermissionController(),
                    reminderRegistry = PersistentMedicationReminderRegistry(storage, storageJson),
                )
                createMedicationReminderActionHandler(
                    keyValueStorage = storage,
                    medicationReminderScheduler = reminderScheduler,
                    errorLogger = AndroidErrorLogger(),
                    timeProvider = SystemTimeProvider(),
                    buildReminderMessage = { event -> event.dosageText },
                ).handle(notificationId, action)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (actionFailure: Exception) {
                Log.e(LOG_TAG, "Could not handle reminder action", actionFailure)
            } finally {
                applicationContext.getSystemService(NotificationManager::class.java)
                    ?.cancel(reminderRequestCode(notificationId))
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        private const val LOG_TAG = "MedicationReminderAction"
    }
}
