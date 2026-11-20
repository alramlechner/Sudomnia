package name.lechners.sudomnia.rules

/**
 * The static geometry of a Sudoku board: which cells share a row, a column or a box.
 *
 * Two boards exist: the classic [NINE] (9x9, 3x3 boxes) and the small [SIX] (6x6,
 * boxes two rows high and three columns wide) used for the children's variant. Every
 * algorithm in this package takes a `Units` instead of assuming nine, so the same
 * solver, generator and hint code serves both.
 *
 * Everything here is computed once at construction and never changes. Cells are
 * indexed in reading order, so `cell = row * size + col`.
 *
 * A "unit" is one of the `3 * size` groups that must contain each digit exactly once:
 * units `0 until size` are the rows, the next `size` the columns, the last `size` the
 * boxes.
 */
class Units(val size: Int, val boxRows: Int, val boxCols: Int) {

    init {
        require(size == boxRows * boxCols) { "a box must hold $size cells" }
    }

    val cells = size * size
    val unitCount = 3 * size

    /** All digits possible, as a candidate mask. */
    val allMask = (1 shl size) - 1

    /** Pencil marks sit in a grid shaped like a box: 3x3 for nine digits, 3x2 for six. */
    val noteCols = boxCols
    val noteRows = boxRows

    /** How many boxes sit side by side. */
    private val boxesAcross = size / boxCols

    val rowOf = IntArray(cells) { it / size }
    val colOf = IntArray(cells) { it % size }
    val boxOf = IntArray(cells) { (it / size / boxRows) * boxesAcross + (it % size) / boxCols }

    fun rowUnit(row: Int) = row
    fun colUnit(col: Int) = size + col
    fun boxUnit(box: Int) = 2 * size + box

    /** The units, each as the cell indices it contains. */
    val cellsOfUnit: Array<IntArray> = Array(unitCount) { u ->
        when {
            u < size -> IntArray(size) { u * size + it }
            u < 2 * size -> IntArray(size) { (u - size) + it * size }
            else -> {
                val b = u - 2 * size
                val r0 = (b / boxesAcross) * boxRows
                val c0 = (b % boxesAcross) * boxCols
                IntArray(size) { (r0 + it / boxCols) * size + (c0 + it % boxCols) }
            }
        }
    }

    /** For each cell, the three units it belongs to: row, column, box. */
    val unitsOfCell: Array<IntArray> = Array(cells) { c ->
        intArrayOf(rowOf[c], size + colOf[c], 2 * size + boxOf[c])
    }

    /**
     * For each cell, the other cells that share a unit with it. Placing a digit only
     * ever needs to touch these -- that is what makes propagation cheap.
     */
    val peers: Array<IntArray> = Array(cells) { c ->
        val seen = BooleanArray(cells)
        var n = 0
        for (u in unitsOfCell[c]) for (p in cellsOfUnit[u]) {
            if (p != c && !seen[p]) { seen[p] = true; n++ }
        }
        val out = IntArray(n)
        var i = 0
        for (p in 0 until cells) if (seen[p]) out[i++] = p
        out
    }

    companion object {
        /** The classic 9x9 board with 3x3 boxes. */
        val NINE = Units(9, 3, 3)

        /** The 6x6 board of the children's variant, with 2x3 boxes. */
        val SIX = Units(6, 2, 3)
    }
}
