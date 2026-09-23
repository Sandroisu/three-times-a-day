package io.github.sandroisu.threetimesaday.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.savedstate.read
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.sandroisu.threetimesaday.core.di.commonAppModule
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderLaunchRepository
import io.github.sandroisu.threetimesaday.core.storage.KeyValueStorage
import io.github.sandroisu.threetimesaday.core.ui.AppIcons
import io.github.sandroisu.threetimesaday.core.ui.AppTheme
import io.github.sandroisu.threetimesaday.feature.medication.presentation.MedicationEditorScreen
import io.github.sandroisu.threetimesaday.feature.medication.presentation.MedicationListScreen
import io.github.sandroisu.threetimesaday.feature.reminder.presentation.ReminderEditorScreen
import io.github.sandroisu.threetimesaday.feature.reminder.presentation.ReminderLabels
import io.github.sandroisu.threetimesaday.feature.reminder.presentation.ReminderListScreen
import io.github.sandroisu.threetimesaday.feature.schedule.presentation.ScheduleEditorScreen
import io.github.sandroisu.threetimesaday.feature.today.presentation.MedicationUpcomingIntakes
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayLabels
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayScreen
import io.github.sandroisu.threetimesaday.feature.today.presentation.TodayViewModel
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

private object AppDestination {
    const val Today = "today"
    const val ScheduleEditor = "schedule-editor"
    const val MedicationList = "medications"
    const val NewMedication = "medication/new"
    const val MedicationEditor = "medication/{medicationId}"
    const val ReminderList = "reminders"
    const val NewReminder = "reminder/new"
    const val ReminderEditor = "reminder/{reminderId}"
}

@Composable
fun App(keyValueStorage: KeyValueStorage, platformModule: Module) {
    val storageModule = module {
        single { keyValueStorage }
    }
    KoinApplication(application = { modules(commonAppModule, storageModule, platformModule) }) {
        AppTheme {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val todayViewModel: TodayViewModel = koinViewModel()
            val launchRepository = koinInject<MedicationReminderLaunchRepository>()
            val launchHandlingScope = rememberCoroutineScope()

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                launchHandlingScope.launch {
                    val launchData = launchRepository.consumeLaunchData()
                    if (launchData != null) {
                        todayViewModel.highlightEvent(launchData.eventId)
                        navController.navigateToTopLevel(AppDestination.Today)
                    }
                }
            }

            Scaffold(
                bottomBar = {
                    if (currentRoute == AppDestination.Today || currentRoute == AppDestination.ReminderList) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentRoute == AppDestination.Today,
                                onClick = { navController.navigateToTopLevel(AppDestination.Today) },
                                icon = { Icon(AppIcons.Medication, contentDescription = null) },
                                label = { Text(TodayLabels.title.asString()) },
                            )
                            NavigationBarItem(
                                selected = currentRoute == AppDestination.ReminderList,
                                onClick = { navController.navigateToTopLevel(AppDestination.ReminderList) },
                                icon = { Icon(AppIcons.Clock, contentDescription = null) },
                                label = { Text(ReminderLabels.reminders.asString()) },
                            )
                        }
                    }
                },
            ) { contentPadding ->
                Box(Modifier.padding(contentPadding)) {
                    NavHost(navController = navController, startDestination = AppDestination.Today) {
                        composable(AppDestination.Today) {
                            TodayScreen(
                                onEditScheduleClick = { navController.navigate(AppDestination.ScheduleEditor) },
                                onEditMedicationsClick = { navController.navigate(AppDestination.MedicationList) },
                                todayViewModel = todayViewModel,
                            )
                        }
                        composable(AppDestination.ScheduleEditor) {
                            ScheduleEditorScreen(
                                onBackClick = { navController.popBackStack() },
                                onScheduleSaved = {
                                    todayViewModel.loadToday()
                                    navController.popBackStack()
                                },
                            )
                        }
                        composable(AppDestination.MedicationList) {
                            MedicationListScreen(
                                onBackClick = {
                                    todayViewModel.loadToday()
                                    navController.popBackStack()
                                },
                                onAddMedicationClick = { navController.navigate(AppDestination.NewMedication) },
                                onMedicationClick = { medicationId -> navController.navigate("medication/$medicationId") },
                            )
                        }
                        composable(AppDestination.NewMedication) {
                            MedicationEditorScreen(
                                medicationId = null,
                                reminderContent = { medicationId -> MedicationUpcomingIntakes(medicationId, todayViewModel) },
                                onBackClick = { navController.popBackStack() },
                                onMedicationSaved = {
                                    todayViewModel.loadToday()
                                    navController.popBackStack()
                                },
                                onMedicationDeleted = {
                                    todayViewModel.loadToday()
                                    navController.popBackStack()
                                },
                            )
                        }
                        composable(AppDestination.MedicationEditor) { backStackEntry: NavBackStackEntry ->
                            MedicationEditorScreen(
                                medicationId = backStackEntry.arguments?.read { getStringOrNull("medicationId") },
                                reminderContent = { medicationId -> MedicationUpcomingIntakes(medicationId, todayViewModel) },
                                onBackClick = { navController.popBackStack() },
                                onMedicationSaved = {
                                    todayViewModel.loadToday()
                                    navController.popBackStack()
                                },
                                onMedicationDeleted = {
                                    todayViewModel.loadToday()
                                    navController.popBackStack()
                                },
                            )
                        }
                        composable(AppDestination.ReminderList) {
                            ReminderListScreen(
                                onAddReminderClick = { navController.navigate(AppDestination.NewReminder) },
                                onReminderClick = { reminderId -> navController.navigate("reminder/$reminderId") },
                            )
                        }
                        composable(AppDestination.NewReminder) {
                            ReminderEditorScreen(
                                reminderId = null,
                                onBackClick = { navController.popBackStack() },
                                onReminderSaved = { navController.popBackStack() },
                                onReminderDeleted = { navController.popBackStack() },
                            )
                        }
                        composable(AppDestination.ReminderEditor) { backStackEntry: NavBackStackEntry ->
                            ReminderEditorScreen(
                                reminderId = backStackEntry.arguments?.read { getStringOrNull("reminderId") },
                                onBackClick = { navController.popBackStack() },
                                onReminderSaved = { navController.popBackStack() },
                                onReminderDeleted = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
