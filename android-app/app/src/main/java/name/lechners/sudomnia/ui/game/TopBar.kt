package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.material3.IconButton
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
import name.lechners.sudomnia.data.ThemeMode
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.ui.theme.AidsIcon
import name.lechners.sudomnia.ui.theme.EraseIcon
import name.lechners.sudomnia.ui.theme.HintIcon
import name.lechners.sudomnia.ui.theme.NewGameIcon
import name.lechners.sudomnia.ui.theme.PauseIcon
import name.lechners.sudomnia.ui.theme.StatsIcon
import name.lechners.sudomnia.ui.theme.LogoBlue
import name.lechners.sudomnia.ui.theme.RedoIcon
import name.lechners.sudomnia.ui.theme.UndoIcon
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary
import name.lechners.sudomnia.ui.theme.icon

@Composable
fun levelLabel(level: Level): String = stringResource(
    when (level) {
        Level.EASY -> R.string.level_easy
        Level.MEDIUM -> R.string.level_medium
        Level.HARD -> R.string.level_hard
        Level.EXPERT -> R.string.level_expert
    }
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    onThemeToggle: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canErase: Boolean,
    canHint: Boolean,
    onErase: () -> Unit,
    onHint: () -> Unit,
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
            IconButton(onClick = onPause, enabled = canPause, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = PauseIcon,
                    contentDescription = stringResource(R.string.paused_pause),
                    tint = if (canPause) LogoBlue else TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Row(
            modifier = Modifier.align(Alignment.CenterVertically),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val themeLabel = stringResource(
                when (settings.themeMode) {
                    ThemeMode.SYSTEM -> R.string.theme_auto
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                }
            )
            IconButton(onClick = onErase, enabled = canErase, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = EraseIcon,
                    contentDescription = stringResource(R.string.erase),
                    tint = if (canErase) LogoBlue else TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onHint, enabled = canHint, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = HintIcon,
                    contentDescription = stringResource(R.string.hint),
                    tint = if (canHint) LogoBlue else TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = UndoIcon,
                    contentDescription = stringResource(R.string.undo),
                    tint = if (canUndo) LogoBlue else TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = RedoIcon,
                    contentDescription = stringResource(R.string.redo),
                    tint = if (canRedo) LogoBlue else TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
            val hint = rememberTooltipState()
            var firstMode by remember { mutableStateOf(true) }
            LaunchedEffect(settings.themeMode) {
                if (firstMode) firstMode = false
                else if (settings.themeMode == ThemeMode.SYSTEM) {
                    hint.show()
                    delay(3000)
                    hint.dismiss()
                }
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Below
                ),
                tooltip = { PlainTooltip { Text(stringResource(R.string.theme_auto_hint)) } },
                state = hint,
            ) {
                IconButton(onClick = onThemeToggle, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = settings.themeMode.icon,
                        contentDescription = themeLabel,
                        tint = LogoBlue,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            IconButton(onClick = onStats, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = StatsIcon,
                    contentDescription = stringResource(R.string.stats),
                    tint = LogoBlue,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = AidsIcon,
                    contentDescription = stringResource(R.string.settings),
                    tint = LogoBlue,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onNewGame, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = NewGameIcon,
                    contentDescription = stringResource(R.string.new_game),
                    tint = LogoBlue,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
