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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.rules.Bits
import name.lechners.sudomnia.rules.Hint
import name.lechners.sudomnia.rules.Step
import name.lechners.sudomnia.rules.Technique
import name.lechners.sudomnia.ui.theme.AppSurfaceHigh
import name.lechners.sudomnia.ui.theme.InkConflict
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary

/** "row 4" / "column 7" / "box 5" -- units are numbered from 1 for the player. */
@Composable
fun unitLabel(unit: Int): String = when {
    unit < 9 -> stringResource(R.string.unit_row, unit + 1)
    unit < 18 -> stringResource(R.string.unit_col, unit - 9 + 1)
    else -> stringResource(R.string.unit_box, unit - 18 + 1)
}

@Composable
fun techniqueLabel(technique: Technique): String = stringResource(
    when (technique) {
        Technique.HIDDEN_SINGLE -> R.string.technique_hidden_single
        Technique.NAKED_SINGLE -> R.string.technique_naked_single
        Technique.LOCKED_CANDIDATES -> R.string.technique_locked_candidates
        Technique.NAKED_PAIR -> R.string.technique_naked_pair
        Technique.HIDDEN_PAIR -> R.string.technique_hidden_pair
        Technique.NAKED_TRIPLE -> R.string.technique_naked_triple
        Technique.HIDDEN_TRIPLE -> R.string.technique_hidden_triple
        Technique.X_WING -> R.string.technique_x_wing
        Technique.SWORDFISH -> R.string.technique_swordfish
        Technique.SIMPLE_COLOURING -> R.string.technique_simple_colouring
        Technique.XY_WING -> R.string.technique_xy_wing
    }
)

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

        // The technique is named only once the reasoning is on screen. In the first
        // stage the name alone would be the answer to a puzzle the player is still
        // being invited to see for themselves.
        val step = state!!.step
        if (state.stage == HintStage.REVEAL && step != null) {
            Text(
                text = techniqueLabel(step.technique),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Text(text = hintText(state), color = TextPrimary, fontSize = 15.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.hint_dismiss)) }
            // Nothing to press on the last stage of an elimination: the app does not
            // reach into the player's pencil marks, so there is no "apply" for it.
            val advance = when {
                state.stage == HintStage.LOCATE -> R.string.hint_show_why
                !state.isLast -> R.string.hint_next
                state.hint is Hint.Reveal -> R.string.hint_apply
                step != null && step.technique.places -> R.string.hint_apply
                else -> null
            }
            if (advance != null) {
                TextButton(onClick = onAdvance) { Text(stringResource(advance)) }
            }
        }
    }
}

/**
 * The first stage says only *where*, the second says *why* -- and the why names the
 * digit. It cannot be otherwise: "in box 5 only this cell can take a 7" *is* the
 * answer, so stage one has to stop at "this cell is decidable".
 */
@Composable
private fun hintText(state: HintState): String = when (val h = state.hint) {
    is Hint.Deduce -> {
        val step = state.step ?: h.last
        when (state.stage) {
            HintStage.LOCATE ->
                if (step.technique.places) stringResource(R.string.hint_locate_place)
                else stringResource(R.string.hint_locate_strike)
            HintStage.REVEAL -> whyText(step)
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

/** One sentence per rung. The wording is the whole point of the ladder. */
@Composable
private fun whyText(step: Step): String = when (step.technique) {
    Technique.HIDDEN_SINGLE ->
        stringResource(R.string.why_hidden_single, unitLabel(step.units[0]), step.digit)

    Technique.NAKED_SINGLE ->
        stringResource(R.string.why_naked_single, step.digit)

    Technique.LOCKED_CANDIDATES -> stringResource(
        R.string.why_locked_candidates,
        unitLabel(step.units[0]),
        step.digit,
        unitLabel(step.units[1]),
    )

    Technique.NAKED_PAIR, Technique.NAKED_TRIPLE ->
        stringResource(R.string.why_naked_subset, unitLabel(step.units[0]), digitList(step.digitMask))

    Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE ->
        stringResource(R.string.why_hidden_subset, unitLabel(step.units[0]), digitList(step.digitMask))

    Technique.X_WING, Technique.SWORDFISH ->
        stringResource(R.string.why_fish, step.digit)

    Technique.SIMPLE_COLOURING ->
        stringResource(R.string.why_simple_colouring, step.digit)

    Technique.XY_WING ->
        stringResource(R.string.why_xy_wing, step.digit)
}

/** "3, 7" -- the digits of a subset, in the player's reading order. */
private fun digitList(mask: Int): String = buildString {
    Bits.forEach(mask) { d ->
        if (isNotEmpty()) append(", ")
        append(d)
    }
}
