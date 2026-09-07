package name.lechners.sudomnia.rules

/**
 * Constraint propagation plus MRV backtracking.
 *
 * The one operation everything else is built on is *counting* solutions with an
 * early exit:
 *
 *  - `countSolutions(p, limit = 1)` answers "is this solvable?" and yields the solution
 *  - `countSolutions(p, limit = 2)` answers "is this *uniquely* solvable?"
 *
 * The second form is what makes puzzle generation possible at all: a puzzle is
 * carved out of a finished grid, and after every removed cell we ask whether the
 * solution is still unique.
 *
 * Branching picks the cell with the fewest remaining candidates (MRV) and breaks
 * ties by cell index -- deterministically, so that a given puzzle always explores
 * the same tree and results are reproducible.
 *
 * Not thread-safe: one instance is a reusable scratchpad. Use one per thread.
 */
class Solver {

    private val grid = Grid()
    private var found = 0
    private var limit = 1
    private var out: IntArray? = null

    /** Number of branch points visited by the last call -- a rough difficulty signal. */
    var guessNodes = 0
        private set

    /**
     * @param givens 81 cells, 0 for empty
     * @param limit stop as soon as this many solutions are found
     * @param out if given, receives the first solution
     * @return the number of solutions found, capped at [limit]
     */
    fun countSolutions(givens: IntArray, limit: Int = 2, out: IntArray? = null): Int {
        this.limit = limit
        this.out = out
        found = 0
        guessNodes = 0
        if (!grid.load(givens)) return 0
        search()
        return found
    }

    /** Convenience: the unique solution, or null if there is none or more than one. */
    fun solveUnique(givens: IntArray): IntArray? {
        val solution = IntArray(Units.CELLS)
        return if (countSolutions(givens, limit = 2, out = solution) == 1) solution else null
    }

    private fun search() {
        if (!grid.propagate(useHidden = true)) return

        if (grid.filled == Units.CELLS) {
            if (found == 0) out?.let { grid.copyDigitsInto(it) }
            found++
            return
        }

        val cell = grid.bestBranchCell()
        guessNodes++
        Bits.forEach(grid.cand[cell]) { digit ->
            grid.push()
            if (grid.place(cell, digit)) search()
            grid.pop()
            if (found >= limit) return
        }
    }
}
