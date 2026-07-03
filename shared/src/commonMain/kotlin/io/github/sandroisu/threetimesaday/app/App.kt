package io.github.sandroisu.threetimesaday.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import io.github.sandroisu.threetimesaday.core.di.commonAppModule
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayScreen
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = { modules(commonAppModule) }) {
        MaterialTheme {
            TodayScreen()
        }
    }
}
