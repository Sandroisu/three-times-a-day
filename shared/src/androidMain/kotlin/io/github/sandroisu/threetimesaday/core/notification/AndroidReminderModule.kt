package io.github.sandroisu.threetimesaday.core.notification

import android.content.Context
import io.github.sandroisu.threetimesaday.core.logging.AndroidErrorLogger
import io.github.sandroisu.threetimesaday.core.logging.ErrorLogger
import io.github.sandroisu.threetimesaday.core.settings.AndroidAppSettingsOpener
import io.github.sandroisu.threetimesaday.core.settings.AppSettingsOpener
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidReminderModule(
    context: Context,
    permissionController: AndroidNotificationPermissionController
): Module = module {
    single<MedicationReminderScheduler> {
        AndroidMedicationReminderScheduler(context, permissionController, get())
    }
    single<AppSettingsOpener> { AndroidAppSettingsOpener(context) }
    single<ErrorLogger> { AndroidErrorLogger() }
}

fun previewReminderModule(): Module = module {
    single<MedicationReminderScheduler> { NoOpMedicationReminderScheduler() }
    single<AppSettingsOpener> { object : AppSettingsOpener {
        override fun openAppSettings() = Unit
        override fun openExactReminderSettings() = Unit
    } }
    single<ErrorLogger> { AndroidErrorLogger() }
}
