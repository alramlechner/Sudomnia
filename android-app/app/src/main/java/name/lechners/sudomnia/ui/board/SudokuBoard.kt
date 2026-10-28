package name.lechners.sudomnia.ui.board

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import name.lechners.sudomnia.ui.theme.LogoBlue
import androidx.compose.runtime.remember
import name.lechners.sudomnia.ui.theme.LocalPalette
import name.lechners.sudomnia.ui.theme.Palette
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import name.lechners.sudomnia.rules.Bits
import name.lechners.sudomnia.rules.Units

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
    onCellLongPress: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val paints = remember { BoardPaints() }
    val pal = LocalPalette.current

    // 0 -> 1 over the life of the effect; one run per write, replayed by flashId.
    val flash = remember { Animatable(1f) }
    LaunchedEffect(state.flashId) {
        if (state.flashCell >= 0) {
            flash.snapTo(0f)
            flash.animateTo(1f, tween(FLASH_MS, easing = LinearEasing))
        }
    }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { offset ->
                    val cell = BoardGeometry(size.width.toFloat()).cellAt(offset.x, offset.y)
                    if (cell >= 0) onCellLongPress(cell)
                },
                onTap = { offset ->
                    val cell = BoardGeometry(size.width.toFloat()).cellAt(offset.x, offset.y)
                    if (cell >= 0) onCellTap(cell)
                },
            )
        },
    ) {
        val geo = BoardGeometry(size.minDimension)
        drawCellBackgrounds(geo, state, pal)
        drawGlyphs(geo, state, paints, pal)
        drawGridLines(geo, pal)
        drawTrialRoot(geo, state, pal)
        drawSelectionFrame(geo, state)
        if (state.flashCell >= 0) drawFlash(geo, state, pal, flash.value)
    }
}

private const val FLASH_MS = 2500

/** Marks the first digit of the open branch: a solid frame plus a corner flag. */
private fun DrawScope.drawTrialRoot(geo: BoardGeometry, state: BoardState, pal: Palette) {
    val c = state.trialRoot
    if (c < 0) return
    val x = geo.left(Units.colOf[c])
    val y = geo.top(Units.rowOf[c])
    val w = geo.cellSize * 0.06f
    val ink = pal.inkTrial.copy(alpha = 1f)
    drawRect(
        color = ink,
        topLeft = Offset(x + w / 2f, y + w / 2f),
        size = Size(geo.cellSize - w, geo.cellSize - w),
        style = Stroke(width = w),
    )
    val flag = geo.cellSize * 0.3f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(x, y)
        lineTo(x + flag, y)
        lineTo(x, y + flag)
        close()
    }
    drawPath(path, ink)
}

/** Dashed frame on the selected cell: says "this one" without touching the fill. */
private fun DrawScope.drawSelectionFrame(geo: BoardGeometry, state: BoardState) {
    val c = state.selected
    if (c < 0) return
    val w = geo.cellSize * 0.055f
    val inset = w * 1.2f
    drawRoundRect(
        color = LogoBlue,
        topLeft = Offset(geo.left(Units.colOf[c]) + inset, geo.top(Units.rowOf[c]) + inset),
        size = Size(geo.cellSize - 2 * inset, geo.cellSize - 2 * inset),
        cornerRadius = CornerRadius(geo.cellSize * 0.1f),
        style = Stroke(
            width = w,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 2.6f, w * 1.8f)),
        ),
    )
}

/**
 * What was just written: a ring that settles in from slightly outside (the "pop"),
 * then holds and fades. For a pencil mark the ring and a glow sit on that one slot,
 * big enough to be seen around a fingertip.
 */
private fun DrawScope.drawFlash(geo: BoardGeometry, state: BoardState, pal: Palette, t: Float) {
    if (t >= 1f) return
    val c = state.flashCell
    val fade = if (t < 0.6f) 1f else (1f - t) / 0.4f
    val pop = (1f - (t / 0.12f).coerceAtMost(1f))
    val amber = pal.inkTrial.copy(alpha = 1f)
    val col = Units.colOf[c]
    val row = Units.rowOf[c]
    val note = state.flashNote
    if (note == 0) {
        val grow = geo.cellSize * 0.12f * pop
        drawRoundRect(
            color = amber.copy(alpha = 0.28f * fade),
            topLeft = Offset(geo.left(col) - grow, geo.top(row) - grow),
            size = Size(geo.cellSize + 2 * grow, geo.cellSize + 2 * grow),
            cornerRadius = CornerRadius(geo.cellSize * 0.12f),
        )
        drawRoundRect(
            color = amber.copy(alpha = fade),
            topLeft = Offset(geo.left(col) - grow, geo.top(row) - grow),
            size = Size(geo.cellSize + 2 * grow, geo.cellSize + 2 * grow),
            cornerRadius = CornerRadius(geo.cellSize * 0.12f),
            style = Stroke(width = geo.cellSize * 0.07f),
        )
    } else {
        val step = geo.cellSize / 3f
        val cx = geo.left(col) + ((note - 1) % 3 + 0.5f) * step
        val cy = geo.top(row) + ((note - 1) / 3 + 0.5f) * step
        val r = step * (0.52f + 0.35f * pop)
        drawCircle(amber.copy(alpha = 0.35f * fade), radius = r, center = Offset(cx, cy))
        drawCircle(amber.copy(alpha = fade), radius = r, center = Offset(cx, cy),
            style = Stroke(width = step * 0.14f))
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

    /** Notes an abandoned branch ruled out -- muted, with a line through. */
    val noteRuledOut = textPaint(Typeface.DEFAULT).apply { strokeWidth = 2f }

    private fun textPaint(face: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = face
    }
}

private fun DrawScope.drawCellBackgrounds(geo: BoardGeometry, state: BoardState, pal: Palette) {
    drawRect(pal.paperBase, size = Size(geo.sizePx, geo.sizePx))

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
            state.conflicts[cell] -> pal.paperConflict
            state.hintCells[cell] -> pal.paperHint
            cell == state.selected -> pal.paperSelected
            state.trial[cell] -> pal.paperTrial
            state.highlightDigit != 0 && state.values[cell] == state.highlightDigit -> pal.paperSameDigit
            Units.unitsOfCell[cell].any { state.hintUnits and (1 shl it) != 0 } -> pal.paperHintUnit
            row == selRow || col == selCol || Units.boxOf[cell] == selBox -> pal.paperPeer
            state.givens[cell] -> pal.paperGivenCell
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

private fun DrawScope.drawGlyphs(geo: BoardGeometry, state: BoardState, paints: BoardPaints, pal: Palette) {
    val canvas = drawContext.canvas.nativeCanvas
    paints.digit.textSize = geo.cellSize * 0.62f
    paints.given.textSize = geo.cellSize * 0.62f
    paints.note.textSize = geo.cellSize * 0.24f
    paints.note.color = pal.inkNote.toArgb()
    paints.noteHighlight.textSize = geo.cellSize * 0.26f
    paints.noteHighlight.color = pal.inkNoteHighlight.toArgb()
    paints.noteConflict.textSize = geo.cellSize * 0.26f
    paints.noteConflict.color = pal.inkConflict.toArgb()

    // The candidates a hint strikes out, drawn in the note grid whether or not the
    // player pencilled them in.
    paints.noteStruck.textSize = geo.cellSize * 0.26f
    paints.noteStruck.color = pal.inkConflict.toArgb()
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

    paints.noteRuledOut.textSize = geo.cellSize * 0.24f
    paints.noteRuledOut.color = pal.inkNote.copy(alpha = 0.75f).toArgb()
    for (cell in 0 until Units.CELLS) {
        if (state.values[cell] != 0 || state.struck[cell] == 0) continue
        val step = geo.cellSize / 3f
        Bits.forEach(state.struck[cell]) { d ->
            val nx = geo.left(Units.colOf[cell]) + ((d - 1) % 3 + 0.5f) * step
            val ny = geo.top(Units.rowOf[cell]) + ((d - 1) / 3 + 0.5f) * step -
                (paints.noteRuledOut.descent() + paints.noteRuledOut.ascent()) / 2f
            canvas.drawText(d.toString(), nx, ny, paints.noteRuledOut)
            val half = paints.noteRuledOut.measureText(d.toString()) * 0.75f
            val mid = ny + (paints.noteRuledOut.descent() + paints.noteRuledOut.ascent()) / 2f
            canvas.drawLine(nx - half, mid, nx + half, mid, paints.noteRuledOut)
        }
    }

    for (cell in 0 until Units.CELLS) {
        val row = Units.rowOf[cell]
        val col = Units.colOf[cell]
        val cx = geo.centerX(col)
        val value = state.values[cell]

        if (value != 0) {
            val paint = if (state.givens[cell]) paints.given else paints.digit
            paint.color = when {
                state.conflicts[cell] -> pal.inkConflict.toArgb()
                state.givens[cell] -> pal.inkGiven.toArgb()
                // Provisional, so not the settled blue of a kept entry -- and unlike
                // the cell tint this survives being selected or highlighted.
                state.trial[cell] -> pal.inkTrial.toArgb()
                else -> pal.inkEntry.toArgb()
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

private fun DrawScope.drawGridLines(geo: BoardGeometry, pal: Palette) {
    val thin = geo.sizePx * 0.0022f
    val thick = geo.sizePx * 0.0075f

    for (i in 0..9) {
        val strong = i % 3 == 0
        val w = if (strong) thick else thin
        val color = if (strong) pal.gridLineStrong else pal.gridLine
        val p = i * geo.cellSize
        // Nudge the outer lines inwards so they are not half-clipped by the edge.
        val v = p.coerceIn(w / 2f, geo.sizePx - w / 2f)
        drawLine(color, Offset(v, 0f), Offset(v, geo.sizePx), strokeWidth = w)
        drawLine(color, Offset(0f, v), Offset(geo.sizePx, v), strokeWidth = w)
    }
}
