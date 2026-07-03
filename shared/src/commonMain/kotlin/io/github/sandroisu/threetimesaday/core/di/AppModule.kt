package io.github.sandroisu.threetimesaday.core.di

import io.github.sandroisu.threetimesaday.core.network.HttpClientFactory
import io.github.sandroisu.threetimesaday.core.time.SystemTimeProvider
import io.github.sandroisu.threetimesaday.core.time.TimeProvider
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonAppModule = module {
    single { HttpClientFactory.create() }
    single<TimeProvider> { SystemTimeProvider() }
    viewModelOf(::TodayViewModel)
}
