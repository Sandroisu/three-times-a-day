package io.github.sandroisu.threetimesaday.core.settings

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

class IosAppSettingsOpener : AppSettingsOpener {

    override fun openAppSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        val application = UIApplication.sharedApplication
        if (application.canOpenURL(settingsUrl)) {
            application.openURL(settingsUrl, emptyMap<Any?, Any?>(), null)
        }
    }
}
