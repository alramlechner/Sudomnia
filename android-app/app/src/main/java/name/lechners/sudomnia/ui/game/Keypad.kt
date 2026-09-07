package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import name.lechners.sudomnia.rules.Bits
import name.lechners.sudomnia.ui.theme.AppOutline
import name.lechners.sudomnia.ui.theme.AppSurface
import name.lechners.sudomnia.ui.theme.AppSurfaceHigh
import name.lechners.sudomnia.ui.theme.LogoBlue
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary

/**
 * The two input rows: digits on top, pencil marks below. **There is no mode.**
 *
 * The earlier version had one row plus a "notes" switch, which forced the order
 * *decide, then pick a cell, then pick a digit*. Players think the other way round:
 * they look at a cell first and only then work out whether they know the answer or
 * want to jot down candidates. Two permanent rows remove the decision entirely --
 * and three pencil marks are three taps instead of switch, tap, tap, tap, switch
 * back.
 *
 * Both rows are visibly disabled until an editable cell is selected. That is what
 * teaches the new order; previously a tap into the void simply did nothing.
 */
@Composable
fun DigitPad(
    remaining: List<Int>,
    /** The digit in the selected cell, 0 if empty -- its key reads as pressed. */
    selectedDigit: Int,
    enabled: Boolean,
    dimCompleted: Boolean,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (d in 1..9) {
            // With the aid off the key stays fully lit and usable: counting the
            // nines is then the player's job, and a ninth 5 must still be typeable.
            val done = dimCompleted && remaining.getOrElse(d) { 0 } <= 0
            val active = enabled && !done
            val set = enabled && d == selectedDigit
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.78f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (set) LogoBlue else AppSurface)
                    .border(
                        width = if (set) 1.5.dp else 1.dp,
                        color = if (set) LogoBlue else AppOutline,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable(enabled = active) { onDigit(d) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = d.toString(),
                    color = if (active) TextPrimary else TextSecondary.copy(alpha = 0.35f),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(2.dp),
                )
            }
        }
    }
}

/**
 * Pencil marks for the selected cell.
 *
 * The keys are *stateful*: a filled key means that mark is currently on the cell, so
 * the row doubles as a readout of what has been noted there. Tapping toggles, which
 * is why several marks in a row cost one tap each and taking one back costs one more.
 */
@Composable
fun NotePad(
    /** The selected cell's pencil marks as a 9-bit mask. */
    notes: Int,
    enabled: Boolean,
    onNote: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.pad_notes),
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (d in 1..9) {
                val on = enabled && Bits.contains(notes, d)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) LogoBlue else AppSurfaceHigh)
                        .border(
                            width = 1.dp,
                            color = if (on) LogoBlue else AppOutline,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable(enabled = enabled) { onNote(d) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = d.toString(),
                        color = when {
                            on -> TextPrimary
                            enabled -> TextSecondary
                            else -> TextSecondary.copy(alpha = 0.35f)
                        },
                        fontSize = 15.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
