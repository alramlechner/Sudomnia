package name.lechners.sudomnia.game

import name.lechners.sudomnia.rules.Bits
import name.lechners.sudomnia.rules.Puzzle
import name.lechners.sudomnia.rules.Units

/**
 * A puzzle being played: the player's digits, their pencil marks, and the undo
 * history. Knows nothing about Android.
 *
 * ### Conflicts, not mistakes
 *
 * The game marks a digit that appears twice in a row, column or box. It does *not*
 * compare against the stored solution, even though it has it. A conflict is a
 * statement about the rules that the player could have made themselves; flagging a
 * deviation from the solution would be the app quietly solving the puzzle for them.
 * That line is deliberate.
 */
class SudokuGame(val puzzle: Puzzle) {

    /** Player-entered digits; always 0 where the puzzle has a clue. */
    val entries = IntArray(Units.CELLS)

    /** Pencil marks as 9-bit masks. */
    val notes = IntArray(Units.CELLS)

    private val history = ArrayList<Edit>()
    private var applied = 0

    val canUndo: Boolean get() = applied > 0
    val canRedo: Boolean get() = applied < history.size

    /** How many of the recorded edits are currently applied -- the undo cursor. */
    val appliedCount: Int get() = applied

    /**
     * The whole edit history, including the undone tail, as encodable rows.
     *
     * Saving a game means saving the givens plus this list, never a copy of the board:
     * replaying it restores the digits, the pencil marks *and* the undo stack in one
     * step. That is what [Edit] was shaped for.
     */
    fun exportHistory(): List<List<IntArray>> =
        history.map { edit -> edit.changes.map { intArrayOf(it.cell, it.digitBefore, it.digitAfter, it.notesBefore, it.notesAfter) } }

    /**
     * Replays a history from [exportHistory] and leaves [appliedCount] at [applied].
     *
     * @return false if a row is malformed -- the caller then discards the saved game
     *         rather than continuing with a half-restored board.
     */
    fun importHistory(rows: List<List<IntArray>>, applied: Int): Boolean {
        if (applied < 0 || applied > rows.size) return false
        history.clear()
        entries.fill(0)
        notes.fill(0)
        this.applied = 0
        for (row in rows) {
            if (row.isEmpty()) return false
            val changes = row.map {
                if (it.size != 5 || it[0] !in 0 until Units.CELLS) return false
                CellChange(it[0], it[1], it[2], it[3], it[4])
            }
            history += Edit(changes)
        }
        repeat(applied) { if (!redo()) return false }
        return true
    }

    fun isGiven(cell: Int): Boolean = puzzle.isGiven(cell)

    /** The digit shown in a cell, clue or entry, 0 if empty. */
    fun valueAt(cell: Int): Int = if (isGiven(cell)) puzzle.givens[cell] else entries[cell]

    /**
     * Places [digit] in [cell], or clears it if the same digit is already there.
     * Clears the cell's own notes and removes the digit from every peer's notes.
     */
    fun setDigit(cell: Int, digit: Int) {
        if (isGiven(cell)) return
        val target = if (entries[cell] == digit) 0 else digit
        val changes = ArrayList<CellChange>(21)
        changes += CellChange(cell, entries[cell], target, notes[cell], 0)

        if (target != 0) {
            val bit = Bits.of(target)
            for (p in Units.peers[cell]) {
                if (notes[p] and bit != 0) {
                    changes += CellChange(p, entries[p], entries[p], notes[p], notes[p] and bit.inv())
                }
            }
        }
        record(Edit(changes))
    }

    /** Adds or removes a single pencil mark. Ignored on a filled cell. */
    fun toggleNote(cell: Int, digit: Int) {
        if (isGiven(cell) || entries[cell] != 0) return
        val bit = Bits.of(digit)
        val after = notes[cell] xor bit
        record(Edit(listOf(CellChange(cell, 0, 0, notes[cell], after))))
    }

    /** Empties a cell: digit and notes. */
    fun clearCell(cell: Int) {
        if (isGiven(cell)) return
        if (entries[cell] == 0 && notes[cell] == 0) return
        record(Edit(listOf(CellChange(cell, entries[cell], 0, notes[cell], 0))))
    }

    fun undo(): Boolean {
        if (!canUndo) return false
        applied--
        for (c in history[applied].changes.asReversed()) {
            entries[c.cell] = c.digitBefore
            notes[c.cell] = c.notesBefore
        }
        return true
    }

    fun redo(): Boolean {
        if (!canRedo) return false
        for (c in history[applied].changes) {
            entries[c.cell] = c.digitAfter
            notes[c.cell] = c.notesAfter
        }
        applied++
        return true
    }

    /** Cells whose digit repeats within one of their units. */
    fun conflicts(): BooleanArray {
        val bad = BooleanArray(Units.CELLS)
        for (u in 0 until Units.UNITS) {
            val cells = Units.cellsOfUnit[u]
            for (i in cells.indices) {
                val a = valueAt(cells[i])
                if (a == 0) continue
                for (j in i + 1 until cells.size) {
                    if (valueAt(cells[j]) == a) {
                        bad[cells[i]] = true
                        bad[cells[j]] = true
                    }
                }
            }
        }
        return bad
    }

    /**
     * How often each digit 1..9 still has to be placed; index 0 is unused.
     * Drives the "this digit is done" state of the keypad.
     */
    fun remaining(): IntArray {
        val left = IntArray(10) { 9 }
        left[0] = 0
        for (c in 0 until Units.CELLS) {
            val v = valueAt(c)
            if (v != 0) left[v]--
        }
        return left
    }

    /** Every cell carries a digit -- says nothing about whether they are the right ones. */
    fun isFull(): Boolean {
        for (c in 0 until Units.CELLS) if (valueAt(c) == 0) return false
        return true
    }

    /**
     * The grid is full and rule-conforming. Because the puzzle has exactly one
     * solution, that is the same thing as being correct -- no need to consult it.
     *
     * This is the *only* definition of "solved". The view model used to carry a
     * second, independent one; with a solve counter in play that is how you end up
     * counting a win twice or not at all.
     */
    fun isSolved(): Boolean = isFull() && conflicts().none { it }

    private fun record(edit: Edit) {
        if (edit.changes.isEmpty()) return
        // A new action discards anything that was undone -- the usual editor rule.
        while (history.size > applied) history.removeAt(history.size - 1)
        history += edit
        redo()
    }
}
