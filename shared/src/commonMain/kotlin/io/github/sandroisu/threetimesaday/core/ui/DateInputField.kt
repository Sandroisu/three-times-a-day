package io.github.sandroisu.threetimesaday.core.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
internal fun DateInputField(
    label: String,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
) {
    FormField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        error = error,
        hint = UiLabels.dateHint.asString(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
}
