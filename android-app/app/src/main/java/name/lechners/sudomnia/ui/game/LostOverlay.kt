package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.game.MistakeTally
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.ui.theme.AppSurfaceHigh
import name.lechners.sudomnia.ui.theme.InkConflict
import name.lechners.sudomnia.ui.theme.TextSecondary

/**
 * The third wrong entry. Translucent like [SolvedOverlay] rather than opaque like
 * [PauseOverlay]: hiding the grid here would suggest it could still be played, and the
 * board as it stands is the whole explanation of what just happened.
 */
@Composable
fun LostOverlay(
    level: Level,
    elapsed: String,
    onNewGame: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(AppSurfaceHigh.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AppSurfaceHigh)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.lost_title),
                color = InkConflict,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.solved_body, levelLabel(level), elapsed),
                color = TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = stringResource(R.string.lost_body, MistakeTally.LIMIT),
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 18.dp),
            )
            Button(onClick = onNewGame) { Text(stringResource(R.string.solved_new_game)) }
        }
    }
}
