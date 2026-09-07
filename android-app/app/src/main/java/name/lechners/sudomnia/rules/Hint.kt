package name.lechners.sudomnia.rules

/**
 * What the app can tell a stuck player.
 *
 * The solution is already known -- it falls out of generation and is kept in
 * [Puzzle.solution] -- so revealing a digit costs nothing. What costs something is
 * *justifying* it, and that is deliberately limited here to the two cases a human
 * can check in seconds: a cell with one candidate left, and a digit with one place
 * left in a unit. Everything harder is revealed without a reason rather than
 * explained badly.
 */
sealed interface Hint {

    /** A cell that is forced right now -- can be revealed *and* justified. */
    data class Forced(
        val cell: Int,
        val digit: Int,
        val kind: Kind,
        /** For [Kind.HIDDEN_SINGLE], the unit that forces it; -1 otherwise. */
        val unit: Int,
    ) : Hint

    /**
     * Nothing is forced. The cell with the fewest possibilities is revealed, with no
     * reasoning attached -- saying "because of a naked triple in column 6" without
     * being able to point at it would be worse than saying nothing.
     */
    data class Reveal(val cell: Int, val digit: Int, val candidateCount: Int) : Hint

    /** The entries made so far can no longer be completed to a solution. */
    data object DeadEnd : Hint

    enum class Kind { HIDDEN_SINGLE, NAKED_SINGLE }
}

/**
 * Picks the hint to give for a board.
 *
 * ### The player's pencil marks are not consulted
 *
 * [find] does not even take them. Notes are incomplete (most players only mark where
 * they are currently thinking), they go stale (placing a digit clears it from the
 * peers' notes, but nothing else does), and they can simply be wrong. A hint derived
 * from them would be provably wrong, and the player would have no way to notice --
 * to them the app is the authority. The candidate state from `Grid.load(board)` is
 * canonical instead: the same board yields the same hint for everyone.
 *
 * Not thread-safe -- one instance per thread, called off the main thread.
 */
class HintFinder {

    private val grid = Grid()
    private val solver = Solver()

    /**
     * @param board 81 cells, givens and player entries together, 0 for empty
     * @param solution the puzzle's unique solution
     */
    fun find(board: IntArray, solution: IntArray): Hint {
        // Two ways the board can already be dead. The check has to come first: on a
        // dead board every deduction is an argument inside a contradiction, and the
        // reveal in step 3 would place a digit from the solution that collides with
        // whatever the player got wrong -- they would see an inexplicable conflict.
        if (!grid.load(board)) return Hint.DeadEnd
        if (solver.countSolutions(board, limit = 1) == 0) return Hint.DeadEnd

        return hiddenSingle() ?: nakedSingle() ?: reveal(solution)
    }

    /**
     * A digit with only one remaining place in some unit.
     *
     * Searched *before* naked singles, the opposite of [Grid.propagate], and for the
     * reason spelled out in [Level]: a hidden single is found by scanning three lines,
     * a naked single by ruling out eight digits across twenty peers. The solver wants
     * the cheaper test first; the player wants the easier one.
     */
    private fun hiddenSingle(): Hint.Forced? {
        for (u in 0 until Units.UNITS) {
            val cells = Units.cellsOfUnit[u]
            var m = Bits.ALL and grid.unitMask[u].inv()
            while (m != 0) {
                val bit = m and (-m)
                m = m xor bit
                var spot = -1
                var n = 0
                for (c in cells) {
                    if (grid.digits[c] == 0 && grid.cand[c] and bit != 0) {
                        n++
                        if (n > 1) break
                        spot = c
                    }
                }
                if (n == 1) {
                    val digit = Integer.numberOfTrailingZeros(bit) + 1
                    return Hint.Forced(spot, digit, Hint.Kind.HIDDEN_SINGLE, u)
                }
            }
        }
        return null
    }

    /** A cell with a single candidate left. */
    private fun nakedSingle(): Hint.Forced? {
        for (c in 0 until Units.CELLS) {
            if (grid.digits[c] != 0) continue
            if (Bits.count(grid.cand[c]) == 1) {
                return Hint.Forced(c, Bits.lowest(grid.cand[c]), Hint.Kind.NAKED_SINGLE, -1)
            }
        }
        return null
    }

    /**
     * Nothing forced: give away the cell that is closest to being decidable. Reusing
     * [Grid.bestBranchCell] is not a shortcut -- "fewest candidates" is exactly the
     * cell a human would also work on next.
     */
    private fun reveal(solution: IntArray): Hint {
        val cell = grid.bestBranchCell()
        if (cell < 0) return Hint.DeadEnd   // full board; the caller should not have asked
        return Hint.Reveal(cell, solution[cell], Bits.count(grid.cand[cell]))
    }
}
