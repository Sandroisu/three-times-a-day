package io.github.sandroisu.threetimesaday

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import io.github.sandroisu.threetimesaday.app.App
import io.github.sandroisu.threetimesaday.core.notification.AndroidNotificationPermissionController
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderLaunchData
import io.github.sandroisu.threetimesaday.core.notification.MedicationReminderReceiver
import io.github.sandroisu.threetimesaday.core.notification.androidMedicationReminderLaunchRepository
import io.github.sandroisu.threetimesaday.core.notification.androidReminderModule
import io.github.sandroisu.threetimesaday.core.notification.previewReminderModule
import io.github.sandroisu.threetimesaday.core.storage.AndroidKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionController = AndroidNotificationPermissionController()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionController.onPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        permissionController.attachLauncher {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                permissionController.onPermissionResult(true)
            }
        }

        persistLaunchData(intent)

        setContent {
            App(
                keyValueStorage = AndroidKeyValueStorage(applicationContext),
                platformModule = androidReminderModule(applicationContext, permissionController)
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        persistLaunchData(intent)
    }

    private fun persistLaunchData(launchIntent: Intent?) {
        val notificationId = launchIntent
            ?.getStringExtra(MedicationReminderReceiver.LAUNCH_EXTRA_NOTIFICATION_ID)
            ?: return
        val eventId = launchIntent
            .getStringExtra(MedicationReminderReceiver.LAUNCH_EXTRA_EVENT_ID)
            ?: return
        launchIntent.removeExtra(MedicationReminderReceiver.LAUNCH_EXTRA_NOTIFICATION_ID)
        launchIntent.removeExtra(MedicationReminderReceiver.LAUNCH_EXTRA_EVENT_ID)
        val launchData = MedicationReminderLaunchData(notificationId = notificationId, eventId = eventId)
        val launchRepository = androidMedicationReminderLaunchRepository(applicationContext)
        lifecycleScope.launch {
            launchRepository.saveLaunchData(launchData)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        keyValueStorage = InMemoryKeyValueStorage(),
        platformModule = previewReminderModule()
    )
}
