package name.lechners.sudomnia.game

import name.lechners.sudomnia.rules.Puzzle
import name.lechners.sudomnia.rules.Units

/** A digit that was entered and is not the one the solution has in that cell. */
class WrongMove(
    /** Number of edits applied once the move is made -- the position right after it. */
    val step: Int,
    val cell: Int,
    val digit: Int,
    val correct: Int,
) {
    val row: Int get() = Units.rowOf[cell] + 1
    val col: Int get() = Units.colOf[cell] + 1
}

/**
 * Looks back over a finished game to find where a wrong digit went in, and what the
 * board looked like at that moment.
 *
 * Works on the saved edit list, like the game save itself: a position is the givens
 * plus the first `step` edits. Moves that were undone and replaced are gone from that
 * list for good, so what is found is the mistakes that actually stayed in the game.
 */
class MistakeAnalysis(
    val puzzle: Puzzle,
    private val rows: List<List<IntArray>>,
    /** Edits that were applied when the game ended. */
    val total: Int,
) {
    val wrongMoves: List<WrongMove> = buildList {
        for (i in 0 until total.coerceAtMost(rows.size)) {
            for (c in rows[i]) {
                val digit = c[2]
                if (digit != 0 && digit != c[1] && digit != puzzle.solution[c[0]]) {
                    add(WrongMove(i + 1, c[0], digit, puzzle.solution[c[0]]))
                    break
                }
            }
        }
    }

    /** Per edit (index = step - 1): 2 = wrong digit, 1 = correct digit, 0 = anything else. */
    val marks: IntArray = IntArray(total) { i ->
        var m = 0
        if (i < rows.size) for (c in rows[i]) {
            val digit = c[2]
            if (digit != 0 && digit != c[1]) {
                m = if (digit == puzzle.solution[c[0]]) 1 else 2
                if (m == 2) break
            }
        }
        m
    }

    /** The position after [step] edits, or null if the saved edits do not replay. */
    fun positionAt(step: Int): SudokuGame? {
        val g = SudokuGame(puzzle)
        return if (g.importHistory(rows, step.coerceIn(0, total))) g else null
    }

    /** The wrong move made exactly at [step], if the move that led there was one. */
    fun wrongMoveAt(step: Int): WrongMove? = wrongMoves.firstOrNull { it.step == step }

    fun nextWrongStep(from: Int): Int? = wrongMoves.firstOrNull { it.step > from }?.step

    fun previousWrongStep(from: Int): Int? = wrongMoves.lastOrNull { it.step < from }?.step
}
