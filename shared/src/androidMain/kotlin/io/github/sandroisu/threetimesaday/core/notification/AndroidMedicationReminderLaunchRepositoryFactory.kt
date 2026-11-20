package io.github.sandroisu.threetimesaday.core.notification

import android.content.Context
import io.github.sandroisu.threetimesaday.core.storage.AndroidKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.storageJson

fun androidMedicationReminderLaunchRepository(context: Context): MedicationReminderLaunchRepository =
    PersistentMedicationReminderLaunchRepository(
        AndroidKeyValueStorage(context.applicationContext),
        storageJson,
    )
