package io.github.sandroisu.threetimesaday.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.sandroisu.threetimesaday.core.di.commonAppModule
import io.github.sandroisu.threetimesaday.feature.schedule.presentation.ScheduleEditorScreen
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayScreen
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayViewModel
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel

private enum class AppScreen {
    Today,
    ScheduleEditor
}

@Composable
fun App() {
    KoinApplication(application = { modules(commonAppModule) }) {
        MaterialTheme {
            var currentScreen by remember { mutableStateOf(AppScreen.Today) }
            val todayViewModel: TodayViewModel = koinViewModel()

            when (currentScreen) {
                AppScreen.Today -> TodayScreen(
                    onEditScheduleClick = { currentScreen = AppScreen.ScheduleEditor },
                    todayViewModel = todayViewModel
                )

                AppScreen.ScheduleEditor -> ScheduleEditorScreen(
                    onBackClick = { currentScreen = AppScreen.Today },
                    onScheduleSaved = {
                        todayViewModel.loadToday()
                        currentScreen = AppScreen.Today
                    }
                )
            }
        }
    }
}
