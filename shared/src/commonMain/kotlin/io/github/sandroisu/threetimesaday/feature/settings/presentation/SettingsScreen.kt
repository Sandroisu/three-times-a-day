package io.github.sandroisu.threetimesaday.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.PrivacyPolicyDialog
import io.github.sandroisu.threetimesaday.core.ui.ScreenHeader
import io.github.sandroisu.threetimesaday.core.ui.SectionHeader
import io.github.sandroisu.threetimesaday.core.ui.UiLabels

@Composable
internal fun SettingsScreen(onBackClick: () -> Unit) {
    var isPrivacyPolicyVisible by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.standard),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
    ) {
        ScreenHeader(SettingsLabels.title.asString(), onBackClick = onBackClick)
        SectionHeader(SettingsLabels.about.asString())
        TextButton(onClick = { isPrivacyPolicyVisible = true }) {
            Text(UiLabels.privacyPolicy.asString())
        }
    }
    if (isPrivacyPolicyVisible) {
        PrivacyPolicyDialog(onDismissRequest = { isPrivacyPolicyVisible = false })
    }
}
