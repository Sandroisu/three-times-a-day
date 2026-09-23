package io.github.sandroisu.threetimesaday.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class MedicationReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }
        val applicationContext = context?.applicationContext ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AndroidMedicationReminderRescheduler.reschedule(applicationContext)
                AndroidReminderRescheduler.reschedule(applicationContext)
            } catch (rescheduleFailure: Exception) {
                Log.e(LOG_TAG, "Could not restore reminders", rescheduleFailure)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val LOG_TAG = "MedicationReminderBoot"
    }
}
