package io.github.sandroisu.threetimesaday.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PrivacyPolicyDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(UiLabels.privacyPolicyTitle.asString()) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(UiLabels.privacyPolicyText.asString())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(UiLabels.confirm.asString()) }
        },
    )
}
