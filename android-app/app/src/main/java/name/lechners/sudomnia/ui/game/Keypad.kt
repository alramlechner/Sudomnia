package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DigitPad(
    remaining: List<Int>,
    /** The digit in the selected cell, 0 if empty -- its key reads as pressed. */
    selectedDigit: Int,
    /** The digit armed for board taps (no cell selected), 0 if none. */
    armed: Int,
    enabled: Boolean,
    dimCompleted: Boolean,
    onDigit: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val textPressure = textPressure()
    val big = textPressure > 1.15f
    val keyAspect = if (big) 0.78f / (textPressure * 1.2f).coerceAtMost(1.9f) else 0.78f
    Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = if (armed != 0) stringResource(R.string.pad_digits_armed, armed)
        else stringResource(R.string.pad_digits),
        color = if (armed != 0) LogoBlue else TextSecondary,
        fontSize = if (armed != 0) 12.sp else 11.sp,
        fontWeight = if (armed != 0) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (d in 1..9) {
            // With the aid off the key stays fully lit and usable: counting the
            // nines is then the player's job, and a ninth 5 must still be typeable.
            val done = dimCompleted && remaining.getOrElse(d) { 0 } <= 0
            val isArmed = d == armed
            val active = enabled && (!done || isArmed)
            val set = enabled && d == selectedDigit
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(keyAspect)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (set) LogoBlue else AppSurface)
                    .border(
                        width = if (set) 1.5.dp else 1.dp,
                        color = if (set) LogoBlue else AppOutline,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .dashedFrame(isArmed, 10.dp)
                    .combinedClickable(
                        enabled = modeEnabled && !done || active,
                        onLongClick = { onLongPress(d) },
                        onClick = { if (active) onDigit(d) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val left = remaining.getOrElse(d) { 0 }
                val countText = if (dimCompleted && left > 0) left.toString() else "\u00A0"
                val countColor = if (set) TextPrimary.copy(alpha = 0.8f) else TextSecondary
                val digit: @Composable () -> Unit = {
                    Text(
                        text = d.toString(),
                        color = if (active) TextPrimary else TextSecondary.copy(alpha = 0.35f),
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
                if (big) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        digit()
                        Text(text = countText, color = countColor, fontSize = 12.sp, lineHeight = 14.sp)
                    }
                } else {
                    digit()
                    if (dimCompleted && left > 0) {
                        Text(
                            text = countText,
                            color = countColor,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 4.dp, bottom = 2.dp),
                        )
                    }
                }
            }
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotePad(
    /** The selected cell's pencil marks as a 9-bit mask. */
    notes: Int,
    /** The digit armed for board taps (no cell selected), 0 if none. */
    armed: Int,
    enabled: Boolean,
    onNote: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val noteScale = textPressure()
    val noteHeight = if (noteScale > 1.15f) 34.dp * noteScale.coerceAtMost(1.6f) else 34.dp
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (armed != 0) stringResource(R.string.pad_notes_armed, armed)
            else stringResource(R.string.pad_notes),
            color = if (armed != 0) LogoBlue else TextSecondary,
            fontSize = if (armed != 0) 12.sp else 11.sp,
            fontWeight = if (armed != 0) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (d in 1..9) {
                val on = enabled && Bits.contains(notes, d)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(noteHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) LogoBlue else AppSurfaceHigh)
                        .border(
                            width = 1.dp,
                            color = if (on) LogoBlue else AppOutline,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .dashedFrame(d == armed, 8.dp)
                        .combinedClickable(
                            enabled = modeEnabled,
                            onLongClick = { onLongPress(d) },
                            onClick = { if (enabled) onNote(d) },
                        ),
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

/** The "armed" marker: a dashed blue frame inside the key, independent of its fill. */
private fun Modifier.dashedFrame(show: Boolean, radius: androidx.compose.ui.unit.Dp): Modifier =
    if (!show) this else this.drawWithContent {
        drawContent()
        val w = 2.5.dp.toPx()
        drawRoundRect(
            color = LogoBlue,
            topLeft = androidx.compose.ui.geometry.Offset(w * 1.5f, w * 1.5f),
            size = androidx.compose.ui.geometry.Size(size.width - w * 3f, size.height - w * 3f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = w,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(w * 2.2f, w * 1.6f)
                ),
            ),
        )
    }

/**
 * How much bigger text is relative to the keys than on a standard phone: font scale
 * times how much narrower the screen is in dp than the 411 dp reference. Both the
 * "font size" and the "display size" setting make digits crowd their keys.
 */
@Composable
private fun textPressure(): Float {
    val widthDp = LocalConfiguration.current.screenWidthDp.coerceAtLeast(1)
    return LocalDensity.current.fontScale * (411f / widthDp).coerceAtLeast(1f)
}
