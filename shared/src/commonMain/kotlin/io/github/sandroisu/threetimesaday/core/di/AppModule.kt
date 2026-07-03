package io.github.sandroisu.threetimesaday.core.di

import io.github.sandroisu.threetimesaday.core.network.HttpClientFactory
import io.github.sandroisu.threetimesaday.core.time.SystemTimeProvider
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.medication.data.InMemoryMedicationRepository
import io.github.sandroisu.threetimesaday.feature.medication.domain.MedicationRepository
import io.github.sandroisu.threetimesaday.feature.schedule.data.InMemoryDailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.schedule.domain.DailyScheduleRepository
import io.github.sandroisu.threetimesaday.feature.schedule.presentation.ScheduleEditorViewModel
import io.github.sandroisu.threetimesaday.feature.today.domain.GenerateMedicationIntakeEventsForDateUseCase
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonAppModule = module {
    single { HttpClientFactory.create() }
    single<TimeProvider> { SystemTimeProvider() }
    single<DailyScheduleRepository> { InMemoryDailyScheduleRepository() }
    single<MedicationRepository> { InMemoryMedicationRepository() }
    single { GenerateMedicationIntakeEventsForDateUseCase() }
    viewModelOf(::TodayViewModel)
    viewModelOf(::ScheduleEditorViewModel)
}
