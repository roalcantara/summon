package com.summondemons.safely.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.summondemons.safely.R
import com.summondemons.safely.data.Demon
import com.summondemons.safely.progress.DemonRecord
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.platform.DebugLocale
import com.summondemons.safely.time.CourtOverride
import com.summondemons.safely.ui.GameUiState
import com.summondemons.safely.ui.theme.spacing
import com.summondemons.safely.ui.theme.textRoles
import com.summondemons.safely.ui.theme.upper

/** Debug builds only (§7): move time, override court, bind / unsummon, reset. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DebugSheet(
    state: GameUiState,
    court: CourtOverride,
    demons: List<Demon>,
    onDismiss: () -> Unit,
    onAdvanceDay: () -> Unit,
    onResetDaily: () -> Unit,
    onCourt: (CourtOverride) -> Unit,
    language: String?,
    onLanguage: (String?) -> Unit,
    onBind: (id: String, flawless: Boolean) -> Unit,
    onUnsummon: (String) -> Unit,
    onResetAll: () -> Unit,
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
            Text(stringResource(R.string.debug_chip), style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedButton(onClick = onAdvanceDay) { Text(stringResource(R.string.debug_advance_day)) }
                OutlinedButton(onClick = onResetDaily) { Text(stringResource(R.string.debug_reset_daily)) }
            }
            Text(stringResource(R.string.debug_court).upper(), style = MaterialTheme.textRoles.meta)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                CourtOverride.entries.forEach { option ->
                    FilterChip(selected = court == option, onClick = { onCourt(option) }, label = { Text(courtLabel(option)) })
                }
            }
            Text(stringResource(R.string.debug_language).upper(), style = MaterialTheme.textRoles.meta)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                DebugLocale.options.forEach { tag ->
                    FilterChip(
                        selected = language == tag,
                        onClick = { if (language != tag) onLanguage(tag) },
                        label = { Text(tag?.uppercase() ?: stringResource(R.string.debug_language_device)) },
                    )
                }
            }
            demons.forEach { demon ->
                Column {
                    Text(demon.name, style = MaterialTheme.typography.titleLarge)
                    Text(status(state.record(demon.id), state.today), style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = { onBind(demon.id, false) }) { Text(stringResource(R.string.debug_bind)) }
                        TextButton(onClick = { onBind(demon.id, true) }) { Text(stringResource(R.string.debug_bind_flawless)) }
                        TextButton(onClick = { onUnsummon(demon.id) }) { Text(stringResource(R.string.debug_unsummon)) }
                    }
                }
            }
            OutlinedButton(onClick = onResetAll) { Text(stringResource(R.string.debug_reset_all)) }
            Text(
                stringResource(R.string.debug_tester_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun courtLabel(option: CourtOverride): String = when (option) {
    CourtOverride.Auto -> stringResource(R.string.debug_court_auto)
    CourtOverride.In -> stringResource(R.string.debug_court_in)
    CourtOverride.Out -> stringResource(R.string.debug_court_out)
}

/** "sealed" / "bound · {d}d since visit · restless · flawless" / "escaped (lore kept)". */
@Composable
private fun status(record: DemonRecord, today: Long): String {
    val bound = stringResource(R.string.debug_status_bound, ProgressRules.daysSinceVisit(record, today).toInt())
    val restless = stringResource(R.string.debug_status_restless)
    val flawless = stringResource(R.string.debug_status_flawless)
    val escaped = stringResource(R.string.debug_status_escaped)
    val sealed = stringResource(R.string.debug_status_sealed)
    return when {
        record.bound -> listOfNotNull(
            bound,
            restless.takeIf { ProgressRules.isRestless(record, today) },
            flawless.takeIf { record.flawless },
        ).joinToString(" · ")
        record.revealed -> escaped
        else -> sealed
    }
}
