package com.summondemons.safely.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.summondemons.safely.R
import com.summondemons.safely.platform.AppLocale
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

/** Settings (RC): language, anonymous analytics switch, plus Debug in debug builds. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    language: String?,
    onLanguage: (String?) -> Unit,
    analyticsEnabled: Boolean,
    onAnalytics: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDebug: (() -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.settings_language).upper(), style = MaterialTheme.textRoles.meta)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                AppLocale.options.forEach { tag ->
                    FilterChip(
                        selected = language == tag,
                        onClick = { if (language != tag) onLanguage(tag) },
                        label = { Text(tag?.let(AppLocale::nativeName) ?: stringResource(R.string.settings_language_device)) },
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().toggleable(value = analyticsEnabled, role = Role.Switch, onValueChange = onAnalytics),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_analytics), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.settings_analytics_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = analyticsEnabled, onCheckedChange = null)
            }
            // Debug builds only: the debug controls live under the menu (one gear on every screen)
            if (onDebug != null) {
                OutlinedButton(onClick = onDebug) { Text(stringResource(R.string.debug_controls)) }
            }
        }
    }
}
