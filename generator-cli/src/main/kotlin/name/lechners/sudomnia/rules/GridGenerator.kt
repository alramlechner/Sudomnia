package name.lechners.sudomnia.rules

import kotlin.random.Random

/**
 * Produces a complete, valid 9x9 grid uniformly at random-ish, and fast.
 *
 * The shortcut that makes it fast: boxes 1, 5 and 9 (top-left, centre,
 * bottom-right) share no row, column or box with each other, so any three
 * permutations of 1..9 can be dropped into them without checking anything. That
 * seeds 27 of the 81 cells for free and leaves so little freedom that the
 * remaining search almost never backtracks.
 *
 * Not thread-safe -- one instance per thread.
 */
class GridGenerator {

    private val grid = Grid()
    private val digitOrder = IntArray(9)

    /** @return a filled 81-cell grid. */
    fun fullGrid(rnd: Random): IntArray {
        while (true) {
            grid.clear()
            if (seedDiagonalBoxes(rnd) && fill(rnd)) {
                return grid.digits.copyOf()
            }
            // Cannot actually happen -- the diagonal seeding never over-constrains
            // the rest -- but a generator that could loop silently is worse than one
            // that retries visibly.
        }
    }

    private fun seedDiagonalBoxes(rnd: Random): Boolean {
        for (box in intArrayOf(0, 4, 8)) {
            shuffleDigits(rnd)
            val cells = Units.cellsOfUnit[18 + box]
            for (i in 0 until 9) {
                if (!grid.place(cells[i], digitOrder[i])) return false
            }
        }
        return true
    }

    private fun fill(rnd: Random): Boolean {
        if (!grid.propagate(useHidden = true)) return false
        if (grid.filled == Units.CELLS) return true

        val cell = grid.bestBranchCell()
        shuffleDigits(rnd)
        for (d in digitOrder) {
            if (!grid.canPlace(cell, d)) continue
            grid.push()
            if (grid.place(cell, d) && fill(rnd)) return true
            grid.pop()
        }
        return false
    }

    private fun shuffleDigits(rnd: Random) {
        for (i in 0 until 9) digitOrder[i] = i + 1
        for (i in 8 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val t = digitOrder[i]; digitOrder[i] = digitOrder[j]; digitOrder[j] = t
        }
    }
}
