package io.github.sandroisu.threetimesaday.core.logging

import android.util.Log

internal class AndroidErrorLogger : ErrorLogger {

    override fun logError(tag: String, message: String, error: Throwable) {
        Log.e(tag, message, error)
    }
}
