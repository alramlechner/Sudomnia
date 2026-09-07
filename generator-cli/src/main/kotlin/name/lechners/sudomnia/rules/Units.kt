package name.lechners.sudomnia.rules

/**
 * The static geometry of a 9x9 Sudoku: which cells share a row, a column or a box.
 *
 * Everything here is computed once at class-init and never changes. Cells are
 * indexed 0..80 in reading order, so `cell = row * 9 + col`.
 *
 * A "unit" is one of the 27 groups that must contain each digit exactly once:
 * units 0..8 are the rows, 9..17 the columns, 18..26 the boxes.
 */
object Units {

    const val SIZE = 9
    const val CELLS = 81
    const val UNITS = 27

    val rowOf = IntArray(CELLS) { it / SIZE }
    val colOf = IntArray(CELLS) { it % SIZE }
    val boxOf = IntArray(CELLS) { (it / SIZE / 3) * 3 + (it % SIZE) / 3 }

    /** The 27 units, each as the 9 cell indices it contains. */
    val cellsOfUnit: Array<IntArray> = Array(UNITS) { u ->
        when {
            u < 9 -> IntArray(SIZE) { u * SIZE + it }
            u < 18 -> IntArray(SIZE) { (u - 9) + it * SIZE }
            else -> {
                val b = u - 18
                val r0 = (b / 3) * 3
                val c0 = (b % 3) * 3
                IntArray(SIZE) { (r0 + it / 3) * SIZE + (c0 + it % 3) }
            }
        }
    }

    /** For each cell, the three units it belongs to: row, column, box. */
    val unitsOfCell: Array<IntArray> = Array(CELLS) { c ->
        intArrayOf(rowOf[c], 9 + colOf[c], 18 + boxOf[c])
    }

    /**
     * For each cell, the 20 other cells that share a unit with it. Placing a digit
     * only ever needs to touch these -- that is what makes propagation cheap.
     */
    val peers: Array<IntArray> = Array(CELLS) { c ->
        val seen = BooleanArray(CELLS)
        var n = 0
        for (u in unitsOfCell[c]) for (p in cellsOfUnit[u]) {
            if (p != c && !seen[p]) { seen[p] = true; n++ }
        }
        val out = IntArray(n)
        var i = 0
        for (p in 0 until CELLS) if (seen[p]) out[i++] = p
        out
    }
}
