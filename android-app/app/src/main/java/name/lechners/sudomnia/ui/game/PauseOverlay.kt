package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.ui.theme.AppBackground
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary

/**
 * The paused screen. Unlike [SolvedOverlay] it is **opaque**: a stopped clock in
 * front of a legible grid would be free thinking time, and thinking is the game.
 *
 * It covers the keypad as well, which also carries information -- the note row is a
 * readout of the selected cell's candidates.
 *
 * The whole surface resumes, not just the button: the way back has to be at least as
 * easy as the way in, and there is nothing else here to hit by mistake.
 */
@Composable
fun PauseOverlay(
    level: Level,
    elapsed: String,
    onResume: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .clickable(interactionSource = interaction, indication = null) { onResume() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.paused_title),
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.solved_body, levelLabel(level), elapsed),
                color = TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )
            Button(onClick = onResume) { Text(stringResource(R.string.paused_resume)) }
        }
    }
}
