package io.github.sandroisu.threetimesaday.core.di

import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.feature.today.domain.BuildIntakeHistoryUseCase
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull

class AppModuleTest {

    @Test
    fun providesIntakeHistoryUseCase() {
        val storageModule = module { single<KeyValueStorage> { InMemoryKeyValueStorage() } }
        val koin = koinApplication { modules(commonAppModule, storageModule) }.koin

        assertNotNull(koin.getOrNull<BuildIntakeHistoryUseCase>())
    }
}
