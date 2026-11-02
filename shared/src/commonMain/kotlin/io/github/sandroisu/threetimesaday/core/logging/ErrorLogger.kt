package io.github.sandroisu.threetimesaday.core.logging

internal interface ErrorLogger {

    fun logError(tag: String, message: String, error: Throwable)
}
