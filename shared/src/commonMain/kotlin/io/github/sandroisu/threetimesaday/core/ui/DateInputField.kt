package io.github.sandroisu.threetimesaday.core.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.github.sandroisu.threetimesaday.core.time.dateFromUtcEpochMillis
import io.github.sandroisu.threetimesaday.core.time.formatDateInput
import io.github.sandroisu.threetimesaday.core.time.parseDateInput
import io.github.sandroisu.threetimesaday.core.time.utcEpochMillisOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateInputField(
    label: String,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
) {
    var isPickerVisible by remember { mutableStateOf(false) }
    FormField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        error = error,
        hint = UiLabels.dateHint.asString(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        trailingIcon = {
            IconButton(onClick = { isPickerVisible = true }) {
                Icon(AppIcons.Calendar, contentDescription = "${UiLabels.chooseDate.asString()}: $label")
            }
        },
    )
    if (isPickerVisible) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateInput(value)?.let(::utcEpochMillisOf),
        )
        DatePickerDialog(
            onDismissRequest = { isPickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selectedMillis ->
                            onValueChange(formatDateInput(dateFromUtcEpochMillis(selectedMillis)))
                        }
                        isPickerVisible = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text(UiLabels.confirm.asString()) }
            },
            dismissButton = {
                TextButton(onClick = { isPickerVisible = false }) { Text(UiLabels.cancel.asString()) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
