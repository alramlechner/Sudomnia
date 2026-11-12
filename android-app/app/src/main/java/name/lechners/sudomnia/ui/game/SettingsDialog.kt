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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import name.lechners.sudomnia.data.ThemeMode
import name.lechners.sudomnia.ui.theme.BackIcon
import androidx.compose.foundation.layout.Spacer
import name.lechners.sudomnia.ui.theme.LogoBlue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import android.content.Intent
import android.net.Uri
import name.lechners.sudomnia.ui.theme.GithubIcon
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

private const val REPO_URL = "https://github.com/alramlechner/Sudomnia"

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
    var showLicenses by remember { mutableStateOf(false) }
    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(stringResource(R.string.settings_licenses)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.licenses_body),
                        color = TextPrimary,
                        fontSize = 14.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) {
                    Text(stringResource(R.string.settings_done))
                }
            },
        )
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Icon(
                            BackIcon,
                            contentDescription = null,
                            tint = LogoBlue,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            stringResource(R.string.back),
                            modifier = Modifier.padding(start = 4.dp),
                            fontSize = 16.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.settings),
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    SectionTitle(stringResource(R.string.settings_section_display))
                    ThemeChoice(settings.themeMode) { onChange(settings.copy(themeMode = it)) }
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
                    SectionTitle(stringResource(R.string.settings_section_aids))
                    Text(
                        text = stringResource(R.string.settings_intro),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    AidSwitch(
                        title = stringResource(R.string.setting_conflicts),
                        hint = stringResource(R.string.setting_conflicts_hint),
                        checked = settings.showConflicts,
                        onChange = { onChange(settings.copy(showConflicts = it)) },
                    )
                    // Behind a rule of its own: the others comment on the rules, this
                    // one reads the answer, and switching it on makes the game losable.
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    AidSwitch(
                        title = stringResource(R.string.setting_warn_wrong),
                        hint = stringResource(R.string.setting_warn_wrong_hint, MistakeTally.LIMIT),
                        checked = settings.warnOnWrong,
                        onChange = { onChange(settings.copy(warnOnWrong = it)) },
                    )
                    SectionTitle(stringResource(R.string.settings_section_app))
                    UpdateRow(update = update, onInstall = onInstallUpdate, onCheck = onCheckUpdate)
                    DiagnosticsRow()
                    val context = LocalContext.current
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) {
                        Icon(GithubIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.settings_github),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    TextButton(onClick = { showLicenses = true }) {
                        Text(stringResource(R.string.settings_licenses))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = LogoBlue,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun ThemeChoice(current: ThemeMode, onPick: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(stringResource(R.string.settings_theme), color = TextPrimary, fontSize = 16.sp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == current,
                    onClick = { onPick(mode) },
                    label = {
                        Text(
                            stringResource(
                                when (mode) {
                                    ThemeMode.SYSTEM -> R.string.settings_theme_auto
                                    ThemeMode.LIGHT -> R.string.settings_theme_light
                                    ThemeMode.DARK -> R.string.settings_theme_dark
                                }
                            ),
                            maxLines = 1,
                        )
                    },
                )
            }
        }
    }
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
