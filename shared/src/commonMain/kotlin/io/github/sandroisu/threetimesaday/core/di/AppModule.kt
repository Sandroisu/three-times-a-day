package io.github.sandroisu.threetimesaday.core.di

import io.github.sandroisu.threetimesaday.core.network.HttpClientFactory
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
import io.github.sandroisu.threetimesaday.feature.today.domain.GenerateMedicationIntakeEventsForDateUseCase
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayViewModel
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
    single<DailyScheduleRepository> { PersistentDailyScheduleRepository(get(), get()) }
    single<MedicationRepository> { PersistentMedicationRepository(get(), get()) }
    single<MedicationIdGenerator> { IncrementingMedicationIdGenerator() }
    single { GenerateMedicationIntakeEventsForDateUseCase() }
    viewModelOf(::TodayViewModel)
    viewModelOf(::ScheduleEditorViewModel)
    viewModelOf(::MedicationListViewModel)
    viewModelOf(::MedicationEditorViewModel)
}
