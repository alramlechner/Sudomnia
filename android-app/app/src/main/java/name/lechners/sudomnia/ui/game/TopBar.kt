package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.data.Settings
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary

@Composable
fun levelLabel(level: Level): String = stringResource(
    when (level) {
        Level.EASY -> R.string.level_easy
        Level.MEDIUM -> R.string.level_medium
        Level.HARD -> R.string.level_hard
        Level.EXPERT -> R.string.level_expert
    }
)

@Composable
fun TopBar(
    level: Level,
    clueCount: Int,
    elapsed: String,
    settings: Settings,
    canPause: Boolean,
    onPause: () -> Unit,
    onNewGame: () -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // FlowRow, not Row: long translations (Spanish especially) don't fit on one line at
    // phone width. A plain Row would let the last child run off the edge instead of
    // wrapping -- this happened for real with "Nueva partida" before this was a FlowRow.
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text(
                text = levelLabel(level),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            // The level is measured, not intended: it names the hardest technique the
            // puzzle actually needs (see Level). Until 0.5.0 this line said
            // "provisional", because the bands came from a singles-only stand-in.
            // Playing without aids is worth showing too: it is a harder game, and
            // nothing else on screen would tell you that afterwards.
            Text(
                text = buildString {
                    append(pluralStringResource(R.plurals.clue_count, clueCount, clueCount))
                    if (settings.allAidsOff) {
                        append(" · ")
                        append(stringResource(R.string.no_aids))
                    }
                },
                color = TextSecondary,
                fontSize = 12.sp,
            )
        }
        // The pause button sits on the clock, because that is what it acts on.
        Row(
            modifier = Modifier.align(Alignment.CenterVertically),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = elapsed, color = TextPrimary, fontSize = 20.sp)
            TextButton(onClick = onPause, enabled = canPause) {
                Text(stringResource(R.string.paused_pause))
            }
        }
        Row(
            modifier = Modifier.align(Alignment.CenterVertically),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onStats) { Text(stringResource(R.string.stats)) }
            TextButton(onClick = onSettings) { Text(stringResource(R.string.settings)) }
            TextButton(onClick = onNewGame) { Text(stringResource(R.string.new_game)) }
        }
    }
}
