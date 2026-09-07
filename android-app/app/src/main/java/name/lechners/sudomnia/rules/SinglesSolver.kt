package name.lechners.sudomnia.rules

/**
 * Solves using nothing but singles -- and reports whether that was enough.
 *
 * This is the oracle the digger uses to aim at a difficulty band (see [Digger]).
 * "Can a human finish this by only ever spotting a forced cell?" is exactly the
 * line between the easy tiers and everything above them.
 *
 * In the full build this class becomes the bottom rung of the real technique
 * ladder, so none of it is throwaway code.
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
