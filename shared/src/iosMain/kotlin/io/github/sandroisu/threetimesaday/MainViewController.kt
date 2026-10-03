package io.github.sandroisu.threetimesaday

import androidx.compose.ui.window.ComposeUIViewController
import io.github.sandroisu.threetimesaday.app.App
import io.github.sandroisu.threetimesaday.core.notification.iosReminderModule
import io.github.sandroisu.threetimesaday.core.notification.IosAlarmBridge
import io.github.sandroisu.threetimesaday.core.storage.NsUserDefaultsKeyValueStorage

fun MainViewController(alarmBridge: IosAlarmBridge? = null) = ComposeUIViewController {
    App(
        keyValueStorage = NsUserDefaultsKeyValueStorage(),
        platformModule = iosReminderModule(alarmBridge)
    )
}
