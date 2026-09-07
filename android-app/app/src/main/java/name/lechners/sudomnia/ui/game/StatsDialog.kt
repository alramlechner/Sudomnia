package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.data.LevelStats
import name.lechners.sudomnia.data.Stats
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary

@Composable
fun StatsDialog(stats: Stats, onReset: () -> Unit, onDismiss: () -> Unit) {
    var askReset by remember { mutableStateOf(false) }

    if (askReset) {
        AlertDialog(
            onDismissRequest = { askReset = false },
            title = { Text(stringResource(R.string.stats_reset_ask)) },
            confirmButton = {
                TextButton(onClick = { askReset = false; onReset() }) {
                    Text(stringResource(R.string.stats_reset_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { askReset = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stats)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (level in Level.entries) {
                    LevelBlock(level, stats[level])
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                }
                Text(
                    text = stringResource(R.string.stats_total, stats.totalSolved),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_done)) }
        },
        dismissButton = {
            TextButton(onClick = { askReset = true }) { Text(stringResource(R.string.stats_reset)) }
        },
    )
}

@Composable
private fun LevelBlock(level: Level, s: LevelStats) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(levelLabel(level), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = s.solved.toString(),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Line(stringResource(R.string.stats_solved), s.solved.toString())
        Line(stringResource(R.string.stats_started), s.started.toString())
        Line(stringResource(R.string.stats_best), formatBest(s.bestMs))
        Line(stringResource(R.string.stats_no_aids), s.solvedNoAids.toString())
        Line(stringResource(R.string.stats_no_hints), s.solvedNoHints.toString())
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun formatBest(ms: Long): String {
    if (ms <= 0L) return stringResource(R.string.stats_none)
    val total = ms / 1000
    val m = total / 60
    val sec = total % 60
    return if (m >= 60) "%d:%02d:%02d".format(m / 60, m % 60, sec) else "%d:%02d".format(m, sec)
}
