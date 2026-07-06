package io.github.sandroisu.threetimesaday.core.notification

import android.content.Context
import io.github.sandroisu.threetimesaday.core.storage.AndroidKeyValueStorage
import kotlinx.serialization.json.Json

fun androidMedicationReminderLaunchRepository(context: Context): MedicationReminderLaunchRepository =
    PersistentMedicationReminderLaunchRepository(
        AndroidKeyValueStorage(context.applicationContext),
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    )
