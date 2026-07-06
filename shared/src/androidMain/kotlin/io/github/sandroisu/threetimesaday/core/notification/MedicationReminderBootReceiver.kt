package io.github.sandroisu.threetimesaday.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sandroisu.threetimesaday.core.storage.AndroidKeyValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class MedicationReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        val applicationContext = context?.applicationContext ?: return
        val registry = PersistentMedicationReminderRegistry(
            AndroidKeyValueStorage(applicationContext),
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        )
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                registry.clear()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
