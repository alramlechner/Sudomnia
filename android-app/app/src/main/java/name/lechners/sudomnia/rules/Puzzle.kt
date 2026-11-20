package name.lechners.sudomnia.rules

/**
 * A generated puzzle: the clues the player sees, the solution behind them, and the
 * measured difficulty band.
 *
 * [level] is what the puzzle *turned out* to be (see [Digger.classify]), not what
 * was asked for. The two agree in almost every case, and where they do not, the
 * player is told the truth.
 *
 * `equals`/`hashCode` are written out because the fields are arrays: the default
 * ones would compare identities, and a puzzle that never compares equal to itself
 * makes Compose re-run work forever.
 */
class Puzzle(
    val givens: IntArray,
    val solution: IntArray,
    val level: Level,
) {
    /** The board geometry of this puzzle, decided by its level. */
    val units: Units get() = level.units

    val clueCount: Int get() = givens.count { it != 0 }

    fun isGiven(cell: Int): Boolean = givens[cell] != 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Puzzle) return false
        return level == other.level &&
            givens.contentEquals(other.givens) &&
            solution.contentEquals(other.solution)
    }

    override fun hashCode(): Int =
        31 * (31 * givens.contentHashCode() + solution.contentHashCode()) + level.hashCode()

    /** One character per cell, '.' for an empty cell -- the usual interchange format. */
    fun toLine(): String = buildString(units.cells) {
        for (d in givens) append(if (d == 0) '.' else '0' + d)
    }

    companion object {
        /**
         * Like [parse], but returns null instead of throwing.
         *
         * For anything read back from storage: a corrupted saved game must be
         * discarded quietly, not crash the app on startup.
         */
        fun parseOrNull(line: String, units: Units = Units.NINE): IntArray? =
            try { parse(line, units) } catch (e: IllegalArgumentException) { null }

        /** Parses the one-character-per-cell form; accepts '.', '0' and ' ' as empty. */
        fun parse(line: String, units: Units = Units.NINE): IntArray {
            val cells = line.filter { it in "0123456789." || it == ' ' }
            require(cells.length == units.cells) { "expected ${units.cells} cells, got ${cells.length}" }
            return IntArray(units.cells) { i ->
                val ch = cells[i]
                if (ch in '1'..'0' + units.size) ch - '0' else 0
            }
        }
    }
}
