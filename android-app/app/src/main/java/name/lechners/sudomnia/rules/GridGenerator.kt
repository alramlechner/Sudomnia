package name.lechners.sudomnia.rules

import kotlin.random.Random

/**
 * Produces a complete, valid grid uniformly at random-ish, and fast.
 *
 * The shortcut that makes it fast: on the 9x9 board boxes 1, 5 and 9 (top-left, centre,
 * bottom-right) share no row, column or box with each other, so any three
 * permutations of 1..9 can be dropped into them without checking anything. That
 * seeds 27 of the 81 cells for free and leaves so little freedom that the
 * remaining search almost never backtracks.
 *
 * Not thread-safe -- one instance per thread.
 */
class GridGenerator(private val units: Units = Units.NINE) {

    private val grid = Grid(units)
    private val digitOrder = IntArray(units.size)

    /**
     * Boxes that share no row and no column with each other (9x9: 1, 5, 9; 6x6: the
     * top-left and the middle-right one). Picked greedily, so it works for any box shape.
     */
    private val independentBoxes: List<Int> = run {
        val chosen = ArrayList<Int>()
        val rows = HashSet<Int>()
        val cols = HashSet<Int>()
        for (b in 0 until units.size) {
            val cells = units.cellsOfUnit[units.boxUnit(b)]
            if (cells.any { units.rowOf[it] in rows || units.colOf[it] in cols }) continue
            chosen += b
            cells.forEach { rows += units.rowOf[it]; cols += units.colOf[it] }
        }
        chosen
    }

    /** @return a filled grid. */
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
        for (box in independentBoxes) {
            shuffleDigits(rnd)
            val cells = units.cellsOfUnit[units.boxUnit(box)]
            for (i in cells.indices) {
                if (!grid.place(cells[i], digitOrder[i])) return false
            }
        }
        return true
    }

    private fun fill(rnd: Random): Boolean {
        if (!grid.propagate(useHidden = true)) return false
        if (grid.filled == units.cells) return true

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
        for (i in digitOrder.indices) digitOrder[i] = i + 1
        for (i in digitOrder.size - 1 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val t = digitOrder[i]; digitOrder[i] = digitOrder[j]; digitOrder[j] = t
        }
    }
}
