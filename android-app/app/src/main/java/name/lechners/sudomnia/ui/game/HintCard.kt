package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.rules.Hint
import name.lechners.sudomnia.ui.theme.AppSurfaceHigh
import name.lechners.sudomnia.ui.theme.InkConflict
import name.lechners.sudomnia.ui.theme.TextPrimary

/** "Zeile 4" / "Spalte 7" / "Block 5" -- units are numbered from 1 for the player. */
@Composable
fun unitLabel(unit: Int): String = when {
    unit < 9 -> stringResource(R.string.unit_row, unit + 1)
    unit < 18 -> stringResource(R.string.unit_col, unit - 9 + 1)
    else -> stringResource(R.string.unit_box, unit - 18 + 1)
}

@Composable
fun HintCard(
    state: HintState?,
    deadEnd: Boolean,
    onAdvance: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state == null && !deadEnd) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (deadEnd) {
            // Says *that* the board is stuck, never *where*. The way out is the undo
            // button that is already there, not a new "find my mistake" feature.
            Text(stringResource(R.string.hint_dead_end), color = InkConflict, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.hint_dismiss)) }
            }
            return@Column
        }

        Text(text = hintText(state!!), color = TextPrimary, fontSize = 15.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.hint_dismiss)) }
            TextButton(onClick = onAdvance) {
                Text(
                    when (state.stage) {
                        HintStage.LOCATE -> stringResource(R.string.hint_show_digit)
                        HintStage.REVEAL -> stringResource(R.string.hint_apply)
                    }
                )
            }
        }
    }
}

/**
 * The digit only appears in the second stage. It cannot be otherwise: the
 * justification names it ("only this cell in block 5 can take a 7" *is* the answer),
 * so stage one has to stay at "this cell is decidable".
 */
@Composable
private fun hintText(state: HintState): String = when (val h = state.hint) {
    is Hint.Forced -> when (state.stage) {
        HintStage.LOCATE -> stringResource(R.string.hint_locate_forced)
        HintStage.REVEAL -> when (h.kind) {
            Hint.Kind.NAKED_SINGLE -> stringResource(R.string.hint_reveal_naked, h.digit)
            Hint.Kind.HIDDEN_SINGLE ->
                stringResource(R.string.hint_reveal_hidden, unitLabel(h.unit), h.digit)
        }
    }

    is Hint.Reveal -> when (state.stage) {
        HintStage.LOCATE -> stringResource(R.string.hint_locate_reveal, h.candidateCount)
        // No reasoning offered. Naming a technique the app cannot point at would be
        // worse than admitting there is nothing to explain.
        HintStage.REVEAL -> stringResource(R.string.hint_reveal_bare, h.digit)
    }

    Hint.DeadEnd -> stringResource(R.string.hint_dead_end)
}
