package io.github.sandroisu.threetimesaday.core.notification

import io.github.sandroisu.threetimesaday.core.settings.AppSettingsOpener
import io.github.sandroisu.threetimesaday.core.settings.IosAppSettingsOpener
import org.koin.core.module.Module
import org.koin.dsl.module

fun iosReminderModule(): Module = module {
    single<MedicationReminderScheduler> { IosMedicationReminderScheduler() }
    single<AppSettingsOpener> { IosAppSettingsOpener() }
}
