package name.lechners.sudomnia.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import name.lechners.sudomnia.data.Outcome
import name.lechners.sudomnia.game.WrongMove
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.ui.board.BoardState
import name.lechners.sudomnia.ui.board.SudokuBoard
import name.lechners.sudomnia.ui.game.levelLabel
import name.lechners.sudomnia.ui.theme.AppBackground
import name.lechners.sudomnia.ui.theme.InkConflict
import name.lechners.sudomnia.ui.theme.TextPrimary
import name.lechners.sudomnia.ui.theme.TextSecondary

/** One position of an ended game, as the analysis shows it. */
class AnalysisUi(
    val board: BoardState,
    val level: Level,
    val outcome: Outcome,
    /** Edits applied in the position shown; 0 is the starting grid. */
    val step: Int,
    val total: Int,
    val errorCount: Int,
    /** Per move: 2 wrong digit, 1 correct digit, 0 other. */
    val marks: IntArray,
    /** Set when the move that led to this position was a wrong digit. */
    val focus: WrongMove?,
    val hasPreviousError: Boolean,
    val hasNextError: Boolean,
)

/**
 * Looks back over an ended game: the board as it stood after any move, opening on the
 * first wrong digit. Cells whose digit differs from the solution are drawn red, the
 * cell just written is framed.
 */
@Composable
fun AnalysisScreen(
    ui: AnalysisUi,
    onStep: (Int) -> Unit,
    onGoTo: (Int) -> Unit,
    onPreviousError: () -> Unit,
    onNextError: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.analysis_title),
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = levelLabel(ui.level),
            color = TextSecondary,
            fontSize = 14.sp,
        )

        SudokuBoard(
            state = ui.board,
            onCellTap = {},
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp)),
        )

        val focus = ui.focus
        if (ui.errorCount == 0) {
            Text(stringResource(R.string.analysis_no_error), color = TextPrimary, fontSize = 15.sp)
        } else if (focus != null) {
            Text(
                text = stringResource(
                    R.string.analysis_wrong_move,
                    ui.step, ui.total, focus.row, focus.col, focus.digit, focus.correct,
                ),
                color = InkConflict,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Text(
                text = if (ui.step == 0) stringResource(R.string.analysis_start)
                else stringResource(R.string.analysis_move, ui.step, ui.total),
                color = TextSecondary,
                fontSize = 15.sp,
            )
        }
        if (ui.errorCount > 0) {
            Text(
                text = stringResource(R.string.analysis_error_count, ui.errorCount),
                color = TextSecondary,
                fontSize = 13.sp,
            )
        }

        if (ui.total > 0) {
            val good = androidx.compose.ui.graphics.Color(0xFF2E9E4F)
            val bad = InkConflict
            val neutral = TextSecondary.copy(alpha = 0.25f)
            val marks = ui.marks
            androidx.compose.foundation.Canvas(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().height(22.dp).padding(horizontal = 10.dp)
            ) {
                val w = size.width
                val tick = (w / ui.total).coerceIn(2f, 6f)
                // neutral first, so a colored tick is never hidden by a neighbour
                for (pass in 0..2) for (i in marks.indices) {
                    val m = marks[i]
                    val order = when (m) { 0 -> 0; 1 -> 1; else -> 2 }
                    if (order != pass) continue
                    val x = w * (i + 1) / ui.total
                    val h = if (m == 0) size.height * 0.4f else size.height
                    drawRect(
                        color = when (m) { 1 -> good; 2 -> bad; else -> neutral },
                        topLeft = androidx.compose.ui.geometry.Offset(x - tick / 2, size.height - h),
                        size = androidx.compose.ui.geometry.Size(tick, h),
                    )
                }
            }
            Slider(
                value = ui.step.toFloat(),
                onValueChange = { onGoTo(it.toInt()) },
                valueRange = 0f..ui.total.toFloat(),
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            )
            Text(stringResource(R.string.analysis_legend), color = TextSecondary, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onPreviousError, enabled = ui.hasPreviousError, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.analysis_prev_error), maxLines = 1)
            }
            OutlinedButton(onClick = { onStep(-1) }, enabled = ui.step > 0, modifier = Modifier.weight(0.6f)) {
                Text("◀")
            }
            OutlinedButton(onClick = { onStep(1) }, enabled = ui.step < ui.total, modifier = Modifier.weight(0.6f)) {
                Text("▶")
            }
            OutlinedButton(onClick = onNextError, enabled = ui.hasNextError, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.analysis_next_error), maxLines = 1)
            }
        }

        Button(onClick = onHome) { Text(stringResource(R.string.home_back)) }
    }
}
