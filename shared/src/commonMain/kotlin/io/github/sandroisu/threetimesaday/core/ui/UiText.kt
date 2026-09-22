package io.github.sandroisu.threetimesaday.core.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

internal data class UiText(
    val resource: StringResource,
    val arguments: List<Any> = emptyList(),
) {
    @Composable
    fun asString(): String {
        val resolvedArguments = arguments.map { argument ->
            if (argument is UiText) argument.asString() else argument
        }
        return stringResource(resource, *resolvedArguments.toTypedArray())
    }

    suspend fun resolve(): String {
        val resolvedArguments = arguments.map { argument ->
            if (argument is UiText) argument.resolve() else argument
        }
        return getString(resource, *resolvedArguments.toTypedArray())
    }
}
