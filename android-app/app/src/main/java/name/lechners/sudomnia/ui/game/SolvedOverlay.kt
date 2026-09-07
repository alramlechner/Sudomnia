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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.ui.theme.AppSurfaceHigh
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary

/**
 * Sits over the finished grid without hiding it -- the point of solving a Sudoku is
 * seeing it complete.
 */
@Composable
fun SolvedOverlay(
    level: Level,
    elapsed: String,
    hintsUsed: Int,
    noAids: Boolean,
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
                text = stringResource(R.string.solved_title),
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.solved_body, levelLabel(level), elapsed),
                color = TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            // The badges earned, named the same way the statistics name them.
            Text(
                text = listOfNotNull(
                    if (noAids) stringResource(R.string.no_aids) else null,
                    if (hintsUsed == 0) stringResource(R.string.solved_no_hints)
                    else pluralStringResource(R.plurals.solved_hints, hintsUsed, hintsUsed),
                ).joinToString(" · "),
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 18.dp),
            )
            Button(onClick = onNewGame) { Text(stringResource(R.string.solved_new_game)) }
        }
    }
}
