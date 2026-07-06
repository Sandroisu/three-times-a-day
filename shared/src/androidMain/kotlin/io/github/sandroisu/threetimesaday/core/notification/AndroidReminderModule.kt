package io.github.sandroisu.threetimesaday.core.notification

import android.content.Context
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidReminderModule(
    context: Context,
    permissionController: AndroidNotificationPermissionController
): Module = module {
    single<MedicationReminderScheduler> {
        AndroidMedicationReminderScheduler(context, permissionController, get())
    }
}

fun previewReminderModule(): Module = module {
    single<MedicationReminderScheduler> { NoOpMedicationReminderScheduler() }
}
