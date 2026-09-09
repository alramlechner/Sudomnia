package name.lechners.sudomnia.rules

/**
 * Solves using nothing but singles -- and reports whether that was enough.
 *
 * This is the oracle the digger uses for the two singles-only bands (see [Digger]).
 * "Can a human finish this by only ever spotting a forced cell?" is exactly the
 * line between the easy tiers and everything above them.
 *
 * [HumanSolver] capped at [Technique.NAKED_SINGLE] answers the same question, and
 * this class stays anyway: it runs [Grid.propagate], which chases the consequences
 * of a placement in one pass instead of rescanning the whole grid for one step at a
 * time. The digger asks the question once per removed cell, ~80 times per puzzle, so
 * the difference is the generator's response time on a tablet.
 *
 * Not thread-safe -- one instance per thread.
 */
class SinglesSolver {

    private val grid = Grid()

    /**
     * @param useHidden false = naked singles only (a cell with one candidate left);
     *                  true = also hidden singles (a digit with one possible cell in a unit)
     * @return true if the puzzle is solved completely by those techniques alone
     */
    fun solves(givens: IntArray, useHidden: Boolean): Boolean {
        if (!grid.load(givens)) return false
        if (!grid.propagate(useHidden)) return false
        return grid.filled == Units.CELLS
    }
}
