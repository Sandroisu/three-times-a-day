package io.github.sandroisu.threetimesaday.core.notification

import kotlinx.coroutines.CompletableDeferred

class AndroidNotificationPermissionController {

    private var launchRequest: (() -> Unit)? = null
    private var pendingResult: CompletableDeferred<Boolean>? = null

    fun attachLauncher(launcher: () -> Unit) {
        launchRequest = launcher
    }

    fun onPermissionResult(isGranted: Boolean) {
        pendingResult?.complete(isGranted)
        pendingResult = null
    }

    suspend fun requestPermission(): Boolean {
        val currentLauncher = launchRequest ?: return false
        val deferredResult = CompletableDeferred<Boolean>()
        pendingResult = deferredResult
        currentLauncher()
        return deferredResult.await()
    }
}
