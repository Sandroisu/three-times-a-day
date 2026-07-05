package io.github.sandroisu.threetimesaday

import androidx.compose.ui.window.ComposeUIViewController
import io.github.sandroisu.threetimesaday.app.App
import io.github.sandroisu.threetimesaday.core.notification.iosReminderModule
import io.github.sandroisu.threetimesaday.core.storage.NsUserDefaultsKeyValueStorage

fun MainViewController() = ComposeUIViewController {
    App(
        keyValueStorage = NsUserDefaultsKeyValueStorage(),
        platformModule = iosReminderModule()
    )
}
