package name.lechners.sudomnia.ui.board

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import name.lechners.sudomnia.rules.Bits
import name.lechners.sudomnia.rules.Units
import name.lechners.sudomnia.ui.theme.GridLine
import name.lechners.sudomnia.ui.theme.GridLineStrong
import name.lechners.sudomnia.ui.theme.InkConflict
import name.lechners.sudomnia.ui.theme.InkEntry
import name.lechners.sudomnia.ui.theme.InkGiven
import name.lechners.sudomnia.ui.theme.InkNote
import name.lechners.sudomnia.ui.theme.InkNoteHighlight
import name.lechners.sudomnia.ui.theme.InkTrial
import name.lechners.sudomnia.ui.theme.PaperBase
import name.lechners.sudomnia.ui.theme.PaperConflict
import name.lechners.sudomnia.ui.theme.PaperGivenCell
import name.lechners.sudomnia.ui.theme.PaperHint
import name.lechners.sudomnia.ui.theme.PaperHintUnit
import name.lechners.sudomnia.ui.theme.PaperPeer
import name.lechners.sudomnia.ui.theme.PaperSameDigit
import name.lechners.sudomnia.ui.theme.PaperSelected
import name.lechners.sudomnia.ui.theme.PaperTrial

/**
 * The whole grid in one [Canvas], not 81 composables.
 *
 * 81 child composables would mean 81 layout nodes recomposing on every tap, and
 * cell backgrounds, digits, notes and the heavy box borders would each need their
 * own z-ordering rules. One canvas draws them in the order they belong in, and hit
 * testing goes through the same [BoardGeometry] the drawing uses.
 *
 * Text goes through `nativeCanvas` with reused [Paint] objects rather than a
 * `TextMeasurer`: up to 81 digits plus 9 pencil marks per cell are laid out per
 * frame, and measuring those through Compose text allocates on every pass.
 */
@Composable
fun SudokuBoard(
    state: BoardState,
    onCellTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paints = remember { BoardPaints() }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val geo = BoardGeometry(size.width.toFloat())
                val cell = geo.cellAt(offset.x, offset.y)
                if (cell >= 0) onCellTap(cell)
            }
        },
    ) {
        val geo = BoardGeometry(size.minDimension)
        drawCellBackgrounds(geo, state)
        drawGlyphs(geo, state, paints)
        drawGridLines(geo)
    }
}

private class BoardPaints {
    val digit = textPaint(Typeface.DEFAULT)
    val given = textPaint(Typeface.DEFAULT_BOLD)
    val note = textPaint(Typeface.DEFAULT)
    val noteHighlight = textPaint(Typeface.DEFAULT_BOLD)

    /** A pencil mark that clashes with a placed digit. */
    val noteConflict = textPaint(Typeface.DEFAULT_BOLD)

    /** Struck-out candidates from a hint -- drawn with a line through them. */
    val noteStruck = textPaint(Typeface.DEFAULT_BOLD).apply { strokeWidth = 2f }

    private fun textPaint(face: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = face
    }
}

private fun DrawScope.drawCellBackgrounds(geo: BoardGeometry, state: BoardState) {
    drawRect(PaperBase, size = Size(geo.sizePx, geo.sizePx))

    val peersLit = state.highlightPeers && state.selected >= 0
    val selRow = if (peersLit) Units.rowOf[state.selected] else -1
    val selCol = if (peersLit) Units.colOf[state.selected] else -1
    val selBox = if (peersLit) Units.boxOf[state.selected] else -1

    for (cell in 0 until Units.CELLS) {
        val row = Units.rowOf[cell]
        val col = Units.colOf[cell]

        // Earlier entries win. A conflict outranks everything -- a hard error beats a
        // voluntary hint. The hint outranks the selection, because it is the answer to
        // the button the player just pressed. The hint's unit sits below the same-digit
        // highlight for the reason PaperPeer does: it covers nine cells, not one.
        // A trial cell ranks above the same-digit highlight by the same rule: an
        // attempt is a handful of cells, the highlight up to nine.
        val color = when {
            state.conflicts[cell] -> PaperConflict
            state.hintCells[cell] -> PaperHint
            cell == state.selected -> PaperSelected
            state.trial[cell] -> PaperTrial
            state.highlightDigit != 0 && state.values[cell] == state.highlightDigit -> PaperSameDigit
            Units.unitsOfCell[cell].any { state.hintUnits and (1 shl it) != 0 } -> PaperHintUnit
            row == selRow || col == selCol || Units.boxOf[cell] == selBox -> PaperPeer
            state.givens[cell] -> PaperGivenCell
            else -> null
        }
        if (color != null) {
            drawRect(
                color = color,
                topLeft = Offset(geo.left(col), geo.top(row)),
                size = Size(geo.cellSize, geo.cellSize),
            )
        }
    }
}

private fun DrawScope.drawGlyphs(geo: BoardGeometry, state: BoardState, paints: BoardPaints) {
    val canvas = drawContext.canvas.nativeCanvas
    paints.digit.textSize = geo.cellSize * 0.62f
    paints.given.textSize = geo.cellSize * 0.62f
    paints.note.textSize = geo.cellSize * 0.24f
    paints.note.color = InkNote.toArgb()
    paints.noteHighlight.textSize = geo.cellSize * 0.26f
    paints.noteHighlight.color = InkNoteHighlight.toArgb()
    paints.noteConflict.textSize = geo.cellSize * 0.26f
    paints.noteConflict.color = InkConflict.toArgb()

    // The candidates a hint strikes out, drawn in the note grid whether or not the
    // player pencilled them in.
    paints.noteStruck.textSize = geo.cellSize * 0.26f
    paints.noteStruck.color = InkConflict.toArgb()
    for (packed in state.hintStrikes) {
        val cell = packed / 16
        val d = packed % 16
        if (state.values[cell] != 0) continue
        val step = geo.cellSize / 3f
        val nx = geo.left(Units.colOf[cell]) + ((d - 1) % 3 + 0.5f) * step
        val ny = geo.top(Units.rowOf[cell]) + ((d - 1) / 3 + 0.5f) * step -
            (paints.noteStruck.descent() + paints.noteStruck.ascent()) / 2f
        canvas.drawText(d.toString(), nx, ny, paints.noteStruck)
        val half = paints.noteStruck.measureText(d.toString()) * 0.75f
        val mid = ny + (paints.noteStruck.descent() + paints.noteStruck.ascent()) / 2f
        canvas.drawLine(nx - half, mid, nx + half, mid, paints.noteStruck)
    }

    for (cell in 0 until Units.CELLS) {
        val row = Units.rowOf[cell]
        val col = Units.colOf[cell]
        val cx = geo.centerX(col)
        val value = state.values[cell]

        if (value != 0) {
            val paint = if (state.givens[cell]) paints.given else paints.digit
            paint.color = when {
                state.conflicts[cell] -> InkConflict.toArgb()
                state.givens[cell] -> InkGiven.toArgb()
                // Provisional, so not the settled blue of a kept entry -- and unlike
                // the cell tint this survives being selected or highlighted.
                state.trial[cell] -> InkTrial.toArgb()
                else -> InkEntry.toArgb()
            }
            // Centre on the glyph body, not on the baseline.
            val baseline = geo.centerY(row) - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(value.toString(), cx, baseline, paint)
        } else if (state.notes[cell] != 0) {
            val step = geo.cellSize / 3f
            Bits.forEach(state.notes[cell]) { d ->
                // Skip what the hint has already drawn struck out in this very slot.
                if (state.hintStrikes.any { it == cell * 16 + d }) return@forEach
                // A pencil mark for the highlighted digit gets the same treatment as
                // a placed one. Without it, selecting a 2 lights up the placed 2s but
                // leaves the pencilled 2s -- usually the ones actually being reasoned
                // about -- to be hunted for by eye.
                // A mark that is already impossible is shown as one -- same red as a
                // clashing entry, and it outranks the same-digit highlight: "this is
                // wrong" beats "this is what you were looking for".
                val paint = when {
                    Bits.contains(state.noteConflicts[cell], d) -> paints.noteConflict
                    d == state.highlightDigit -> paints.noteHighlight
                    else -> paints.note
                }
                val nCol = (d - 1) % 3
                val nRow = (d - 1) / 3
                val nx = geo.left(col) + (nCol + 0.5f) * step
                val ny = geo.top(row) + (nRow + 0.5f) * step -
                    (paint.descent() + paint.ascent()) / 2f
                canvas.drawText(d.toString(), nx, ny, paint)
            }
        }
    }
}

private fun DrawScope.drawGridLines(geo: BoardGeometry) {
    val thin = geo.sizePx * 0.0022f
    val thick = geo.sizePx * 0.0075f

    for (i in 0..9) {
        val strong = i % 3 == 0
        val w = if (strong) thick else thin
        val color = if (strong) GridLineStrong else GridLine
        val p = i * geo.cellSize
        // Nudge the outer lines inwards so they are not half-clipped by the edge.
        val v = p.coerceIn(w / 2f, geo.sizePx - w / 2f)
        drawLine(color, Offset(v, 0f), Offset(v, geo.sizePx), strokeWidth = w)
        drawLine(color, Offset(0f, v), Offset(geo.sizePx, v), strokeWidth = w)
    }
}
