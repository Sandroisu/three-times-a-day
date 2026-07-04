package io.github.sandroisu.threetimesaday.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.sandroisu.threetimesaday.core.di.commonAppModule
import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.feature.medication.presentation.MedicationEditorScreen
import io.github.sandroisu.threetimesaday.feature.medication.presentation.MedicationListScreen
import io.github.sandroisu.threetimesaday.feature.schedule.presentation.ScheduleEditorScreen
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayScreen
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayViewModel
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.module

private enum class AppScreen {
    Today,
    ScheduleEditor,
    MedicationList,
    MedicationEditor
}

@Composable
fun App(keyValueStorage: KeyValueStorage) {
    val storageModule = module {
        single { keyValueStorage }
    }
    KoinApplication(application = { modules(commonAppModule, storageModule) }) {
        MaterialTheme {
            var currentScreen by remember { mutableStateOf(AppScreen.Today) }
            var selectedMedicationId by remember { mutableStateOf<String?>(null) }
            val todayViewModel: TodayViewModel = koinViewModel()

            when (currentScreen) {
                AppScreen.Today -> TodayScreen(
                    onEditScheduleClick = { currentScreen = AppScreen.ScheduleEditor },
                    onEditMedicationsClick = { currentScreen = AppScreen.MedicationList },
                    todayViewModel = todayViewModel
                )

                AppScreen.ScheduleEditor -> ScheduleEditorScreen(
                    onBackClick = { currentScreen = AppScreen.Today },
                    onScheduleSaved = {
                        todayViewModel.loadToday()
                        currentScreen = AppScreen.Today
                    }
                )

                AppScreen.MedicationList -> MedicationListScreen(
                    onBackClick = {
                        todayViewModel.loadToday()
                        currentScreen = AppScreen.Today
                    },
                    onAddMedicationClick = {
                        selectedMedicationId = null
                        currentScreen = AppScreen.MedicationEditor
                    },
                    onMedicationClick = { medicationId ->
                        selectedMedicationId = medicationId
                        currentScreen = AppScreen.MedicationEditor
                    }
                )

                AppScreen.MedicationEditor -> MedicationEditorScreen(
                    medicationId = selectedMedicationId,
                    onBackClick = { currentScreen = AppScreen.MedicationList },
                    onMedicationSaved = { currentScreen = AppScreen.MedicationList },
                    onMedicationDeleted = { currentScreen = AppScreen.MedicationList }
                )
            }
        }
    }
}
