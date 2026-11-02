package io.github.sandroisu.threetimesaday.core.logging

import platform.Foundation.NSLog

internal class IosErrorLogger : ErrorLogger {

    override fun logError(tag: String, message: String, error: Throwable) {
        // The text goes as an argument: a "%" inside it must not be read as a format sign.
        NSLog("%@", "[$tag] $message: ${error.stackTraceToString()}")
    }
}
