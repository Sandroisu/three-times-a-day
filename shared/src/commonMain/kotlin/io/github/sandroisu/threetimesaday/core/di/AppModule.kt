package io.github.sandroisu.threetimesaday.core.di

import io.github.sandroisu.threetimesaday.core.network.HttpClientFactory
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderLaunchRepository
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderRegistry
import io.github.sandroisu.threetimesaday.core.notification.PersistentMedicationReminderLaunchRepository
import io.github.sandroisu.threetimesaday.core.notification.PersistentMedicationReminderRegistry
import io.github.sandroisu.threetimesaday.core.time.SystemTimeProvider
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.data.IncrementingMedicationIdGenerator
import io.github.sandroisu.threetimesaday.feature.medication.data.PersistentMedicationRepository
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationIdGenerator
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.medication.presentation.MedicationEditorViewModel
import io.github.sandroisu.threetimesaday.feature.medication.presentation.MedicationListViewModel
import io.github.sandroisu.threetimesaday.feature.schedule.data.PersistentDailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.schedule.presentation.ScheduleEditorViewModel
import io.github.sandroisu.threetimesaday.feature.today.data.PersistentMedicationIntakeRecordRepository
import io.github.sandroisu.threetimesaday.feature.today.domain.ApplyMedicationIntakeRecordsUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.GenerateMedicationIntakeEventsForDateUseCase
import io.github.sandroisu.threetimesaday.feature.today.domain.MedicationIntakeRecordRepository
import io.github.sandroisu.threetimesaday.feature.today.domain.RescheduleMedicationRemindersUseCase
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayViewModel
import io.github.sandroisu.threetimesaday.feature.today.presentation.intakeMomentLabel
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonAppModule = module {
    single { HttpClientFactory.create() }
    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<TimeProvider> { SystemTimeProvider() }
    single<MedicationReminderRegistry> { PersistentMedicationReminderRegistry(get(), get()) }
    single<MedicationReminderLaunchRepository> { PersistentMedicationReminderLaunchRepository(get(), get()) }
    single<DailyScheduleRepository> { PersistentDailyScheduleRepository(get(), get()) }
    single<MedicationRepository> { PersistentMedicationRepository(get(), get()) }
    single<MedicationIdGenerator> { IncrementingMedicationIdGenerator() }
    single<MedicationIntakeRecordRepository> { PersistentMedicationIntakeRecordRepository(get(), get()) }
    single { GenerateMedicationIntakeEventsForDateUseCase() }
    single { ApplyMedicationIntakeRecordsUseCase() }
    single {
        RescheduleMedicationRemindersUseCase(
            dailyScheduleRepository = get(),
            medicationRepository = get(),
            medicationIntakeRecordRepository = get(),
            generateMedicationIntakeEventsForDate = get(),
            applyMedicationIntakeRecords = get(),
            medicationReminderScheduler = get(),
            timeProvider = get(),
            buildReminderMessage = { event ->
                "${event.dosageText} · ${intakeMomentLabel(event.intakeMoment)}"
            }
        )
    }
    viewModelOf(::TodayViewModel)
    viewModelOf(::ScheduleEditorViewModel)
    viewModelOf(::MedicationListViewModel)
    viewModelOf(::MedicationEditorViewModel)
}
