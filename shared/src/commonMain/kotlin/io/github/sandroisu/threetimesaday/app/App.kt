package io.github.sandroisu.threetimesaday.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.sandroisu.threetimesaday.core.di.commonAppModule
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderLaunchRepository
import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.feature.medication.presentation.MedicationEditorScreen
import io.github.sandroisu.threetimesaday.feature.medication.presentation.MedicationListScreen
import io.github.sandroisu.threetimesaday.feature.schedule.presentation.ScheduleEditorScreen
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayScreen
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayViewModel
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

private enum class AppScreen {
    Today,
    ScheduleEditor,
    MedicationList,
    MedicationEditor
}

@Composable
fun App(keyValueStorage: KeyValueStorage, platformModule: Module) {
    val storageModule = module {
        single { keyValueStorage }
    }
    KoinApplication(application = { modules(commonAppModule, storageModule, platformModule) }) {
        MaterialTheme {
            var currentScreen by remember { mutableStateOf(AppScreen.Today) }
            var selectedMedicationId by remember { mutableStateOf<String?>(null) }
            val todayViewModel: TodayViewModel = koinViewModel()
            val launchRepository = koinInject<MedicationReminderLaunchRepository>()
            val launchHandlingScope = rememberCoroutineScope()

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                launchHandlingScope.launch {
                    val launchData = launchRepository.consumeLaunchData()
                    if (launchData != null) {
                        todayViewModel.highlightEvent(launchData.eventId)
                        currentScreen = AppScreen.Today
                    }
                }
            }

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
                    onMedicationSaved = {
                        todayViewModel.loadToday()
                        currentScreen = AppScreen.MedicationList
                    },
                    onMedicationDeleted = {
                        todayViewModel.loadToday()
                        currentScreen = AppScreen.MedicationList
                    }
                )
            }
        }
    }
}
