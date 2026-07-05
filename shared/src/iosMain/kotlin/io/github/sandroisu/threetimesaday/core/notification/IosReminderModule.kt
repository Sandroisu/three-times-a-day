package io.github.sandroisu.threetimesaday.core.notification

import org.koin.core.module.Module
import org.koin.dsl.module

fun iosReminderModule(): Module = module {
    single<MedicationReminderScheduler> { IosMedicationReminderScheduler() }
}
