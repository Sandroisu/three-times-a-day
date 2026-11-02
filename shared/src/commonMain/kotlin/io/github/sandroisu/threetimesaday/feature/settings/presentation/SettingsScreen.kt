package io.github.sandroisu.threetimesaday.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sandroisu.threetimesaday.core.ui.AppSpacing
import io.github.sandroisu.threetimesaday.core.ui.Notice
import io.github.sandroisu.threetimesaday.core.ui.PrivacyPolicyDialog
import io.github.sandroisu.threetimesaday.core.ui.ScreenHeader
import io.github.sandroisu.threetimesaday.core.ui.SectionHeader
import io.github.sandroisu.threetimesaday.core.ui.UiLabels
import io.github.sandroisu.threetimesaday.feature.settings.domain.StartScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsScreen(
    onBackClick: () -> Unit,
    settingsViewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
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
        uiState.errorMessage?.let { message -> Notice(message.asString(), isError = true) }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.standard)) {
            SectionHeader(SettingsLabels.startScreen.asString())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.compact)) {
                StartScreen.entries.forEach { startScreen ->
                    FilterChip(
                        selected = uiState.startScreen == startScreen,
                        onClick = { settingsViewModel.onStartScreenSelected(startScreen) },
                        label = { Text(startScreenLabel(startScreen).asString()) },
                    )
                }
            }
            Text(
                SettingsLabels.startScreenHint.asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionHeader(SettingsLabels.about.asString())
        TextButton(onClick = { isPrivacyPolicyVisible = true }) {
            Text(UiLabels.privacyPolicy.asString())
        }
    }
    if (isPrivacyPolicyVisible) {
        PrivacyPolicyDialog(onDismissRequest = { isPrivacyPolicyVisible = false })
    }
}
