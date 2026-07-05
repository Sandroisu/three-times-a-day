package io.github.sandroisu.threetimesaday

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.sandroisu.threetimesaday.app.App
import io.github.sandroisu.threetimesaday.core.notification.AndroidNotificationPermissionController
import io.github.sandroisu.threetimesaday.core.notification.androidReminderModule
import io.github.sandroisu.threetimesaday.core.notification.previewReminderModule
import io.github.sandroisu.threetimesaday.core.storage.AndroidKeyValueStorage
import io.github.sandroisu.threetimesaday.core.storage.InMemoryKeyValueStorage

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

        setContent {
            App(
                keyValueStorage = AndroidKeyValueStorage(applicationContext),
                platformModule = androidReminderModule(applicationContext, permissionController)
            )
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
