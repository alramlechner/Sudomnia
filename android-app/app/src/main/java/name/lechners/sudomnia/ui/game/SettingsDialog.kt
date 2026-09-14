package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.BuildConfig
import name.lechners.sudomnia.R
import name.lechners.sudomnia.data.Settings
import name.lechners.sudomnia.game.MistakeTally
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary
import androidx.compose.ui.platform.LocalContext
import name.lechners.sudomnia.diag.DiagnosticsLog
import name.lechners.sudomnia.update.UpdateState

@Composable
fun SettingsDialog(
    settings: Settings,
    /** null in a build without self-update -- then the row is just the version. */
    update: UpdateState?,
    onChange: (Settings) -> Unit,
    onInstallUpdate: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.settings_intro),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                AidSwitch(
                    title = stringResource(R.string.setting_conflicts),
                    hint = stringResource(R.string.setting_conflicts_hint),
                    checked = settings.showConflicts,
                    onChange = { onChange(settings.copy(showConflicts = it)) },
                )
                AidSwitch(
                    title = stringResource(R.string.setting_same_digit),
                    hint = stringResource(R.string.setting_same_digit_hint),
                    checked = settings.highlightSameDigit,
                    onChange = { onChange(settings.copy(highlightSameDigit = it)) },
                )
                AidSwitch(
                    title = stringResource(R.string.setting_peers),
                    hint = stringResource(R.string.setting_peers_hint),
                    checked = settings.highlightPeers,
                    onChange = { onChange(settings.copy(highlightPeers = it)) },
                )
                AidSwitch(
                    title = stringResource(R.string.setting_dim_digits),
                    hint = stringResource(R.string.setting_dim_digits_hint),
                    checked = settings.dimCompletedDigits,
                    onChange = { onChange(settings.copy(dimCompletedDigits = it)) },
                )
                // Last, and behind a rule of its own: the four above comment on the
                // rules, this one reads the answer. The hint has to say what it costs,
                // because switching it on is what makes the game losable.
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                AidSwitch(
                    title = stringResource(R.string.setting_warn_wrong),
                    hint = stringResource(
                        R.string.setting_warn_wrong_hint,
                        MistakeTally.LIMIT,
                    ),
                    checked = settings.warnOnWrong,
                    onChange = { onChange(settings.copy(warnOnWrong = it)) },
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                UpdateRow(
                    update = update,
                    onInstall = onInstallUpdate,
                    onCheck = onCheckUpdate,
                )
                DiagnosticsRow()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_done)) }
        },
    )
}

/** The whole row is the target, not just the switch -- it is a tablet, not a mouse. */
@Composable
private fun AidSwitch(
    title: String,
    hint: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, fontSize = 16.sp)
            Text(hint, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/**
 * Version plus the one button the update flow needs.
 *
 * The label of that button is derived from the state, never stored alongside it -- the
 * state alone decides whether the tap checks or installs, which is what keeps the two
 * from drifting apart.
 */
@Composable
private fun UpdateRow(
    update: UpdateState?,
    onInstall: () -> Unit,
    onCheck: () -> Unit,
) {
    val installable = update?.installableVersion
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = stringResource(R.string.update_version, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
                color = TextPrimary,
                fontSize = 16.sp,
            )
            Text(
                text = when {
                    installable != null -> stringResource(R.string.update_available_note, installable)
                    update is UpdateState.Failed -> stringResource(R.string.update_failed, update.message)
                    update is UpdateState.Current -> stringResource(R.string.update_current)
                    else -> ""
                },
                color = TextSecondary,
                fontSize = 12.sp,
            )
        }
        // No controller, no button: in the Play build updates come from the store, and
        // a "check now" that could never find anything would be a lie in a dialog.
        if (update == null) return@Row
        TextButton(
            onClick = if (installable != null) onInstall else onCheck,
            enabled = !update.busy,
        ) {
            Text(
                stringResource(
                    when {
                        update is UpdateState.Downloading -> R.string.update_downloading
                        installable != null -> R.string.update_install
                        update is UpdateState.Checking -> R.string.update_checking
                        else -> R.string.update_check
                    },
                    installable ?: "",
                ),
            )
        }
    }
}

/**
 * Hands the log to the share sheet.
 *
 * Deliberately not "send report to the developer": the app has no upload path, and the
 * player sees what leaves the device and picks the target every time.
 */
@Composable
private fun DiagnosticsRow() {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(R.string.diag_title), color = TextPrimary, fontSize = 16.sp)
            Text(stringResource(R.string.diag_hint), color = TextSecondary, fontSize = 12.sp)
        }
        TextButton(onClick = { context.startActivity(DiagnosticsLog.shareIntent(context)) }) {
            Text(stringResource(R.string.diag_share))
        }
    }
}
