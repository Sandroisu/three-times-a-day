package io.github.sandroisu.threetimesaday.core.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import io.github.sandroisu.threetimesaday.core.time.formatTimeOfDay
import io.github.sandroisu.threetimesaday.core.time.parseTimeOfDay
import kotlinx.datetime.LocalTime

@Composable
internal fun TimeInputField(
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
        hint = UiLabels.timeHint.asString(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
        trailingIcon = {
            IconButton(onClick = { isPickerVisible = true }) {
                Icon(AppIcons.Clock, contentDescription = "${UiLabels.chooseTime.asString()}: $label")
            }
        },
    )
    if (isPickerVisible) {
        val initialTime = parseTimeOfDay(value) ?: LocalTime(DEFAULT_PICKER_HOUR, 0)
        var selectedHour by remember { mutableStateOf(initialTime.hour) }
        var selectedMinute by remember { mutableStateOf(initialTime.minute) }
        AlertDialog(
            onDismissRequest = { isPickerVisible = false },
            title = { Text(label) },
            text = {
                WheelTimePicker(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onHourChange = { hour -> selectedHour = hour },
                    onMinuteChange = { minute -> selectedMinute = minute },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(formatTimeOfDay(LocalTime(selectedHour, selectedMinute)))
                    isPickerVisible = false
                }) { Text(UiLabels.confirm.asString()) }
            },
            dismissButton = {
                TextButton(onClick = { isPickerVisible = false }) { Text(UiLabels.cancel.asString()) }
            },
        )
    }
}

private const val DEFAULT_PICKER_HOUR = 8
