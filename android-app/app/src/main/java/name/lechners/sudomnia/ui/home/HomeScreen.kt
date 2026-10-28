package name.lechners.sudomnia.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.data.Settings
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.ui.game.GameUiState
import name.lechners.sudomnia.ui.game.NewGameDialog
import name.lechners.sudomnia.ui.game.SettingsDialog
import name.lechners.sudomnia.ui.game.StatsDialog
import name.lechners.sudomnia.ui.game.UpdateBanner
import name.lechners.sudomnia.ui.game.levelLabel
import name.lechners.sudomnia.ui.theme.AppBackground
import name.lechners.sudomnia.ui.theme.LogoBlue
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary
import name.lechners.sudomnia.update.UpdateState

/**
 * The start screen. A running game is only ever left waiting here: "Resume" comes
 * first, and starting another game or giving up are the two ways to end it -- both ask.
 */
@Composable
fun HomeScreen(
    state: GameUiState,
    elapsed: String,
    versionName: String,
    update: UpdateState?,
    onResume: () -> Unit,
    onNewGame: (Level) -> Unit,
    onAnalyze: () -> Unit,
    onSettingsChange: (Settings) -> Unit,
    onResetStats: () -> Unit,
    onInstallUpdate: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLevelPicker by remember { mutableStateOf(false) }
    var confirmNew by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

    val running = state.gameRunning && !state.generating
    val width = Modifier.fillMaxWidth().widthIn(max = 420.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = LogoBlue,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.home_tagline),
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        UpdateBanner(state = update, onInstall = onInstallUpdate)

        Spacer(Modifier.height(32.dp))

        if (running) {
            Button(onClick = onResume, modifier = width.height(64.dp)) {
                Text(stringResource(R.string.home_resume), fontSize = 18.sp)
            }
            Text(
                text = stringResource(R.string.home_running, levelLabel(state.level), elapsed),
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = { confirmNew = true }, modifier = width.height(56.dp)) {
                Text(stringResource(R.string.new_game))
            }
        } else {
            Button(onClick = { showLevelPicker = true }, modifier = width.height(64.dp)) {
                Text(stringResource(R.string.new_game), fontSize = 18.sp)
            }
        }

        if (state.canAnalyze) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onAnalyze, modifier = width.height(56.dp)) {
                Text(stringResource(R.string.home_analyze))
            }
        }
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = { showStats = true }, modifier = width) { Text(stringResource(R.string.stats)) }
        TextButton(onClick = { showSettings = true }, modifier = width) { Text(stringResource(R.string.settings)) }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.home_version, versionName),
            color = TextSecondary,
            fontSize = 12.sp,
        )
    }

    if (showLevelPicker) {
        NewGameDialog(
            onPick = { showLevelPicker = false; onNewGame(it) },
            onDismiss = { showLevelPicker = false },
        )
    }

    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text(stringResource(R.string.home_new_confirm_title)) },
            text = { Text(stringResource(R.string.home_give_up_body)) },
            confirmButton = {
                TextButton(onClick = { confirmNew = false; showLevelPicker = true }) {
                    Text(stringResource(R.string.new_game))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmNew = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showSettings) {
        SettingsDialog(
            settings = state.settings,
            update = update,
            onChange = onSettingsChange,
            onInstallUpdate = onInstallUpdate,
            onCheckUpdate = onCheckUpdate,
            onDismiss = { showSettings = false },
        )
    }

    if (showStats) {
        StatsDialog(
            stats = state.stats,
            history = state.history,
            onReset = onResetStats,
            onDismiss = { showStats = false },
        )
    }
}
