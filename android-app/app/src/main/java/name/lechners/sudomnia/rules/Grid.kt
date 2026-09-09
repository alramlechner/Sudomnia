package name.lechners.sudomnia.rules

import name.lechners.sudomnia.rules.Units.CELLS
import name.lechners.sudomnia.rules.Units.UNITS

/**
 * A mutable working grid: digits, candidate masks, and per-unit "digits already
 * placed" masks, kept consistent at all times.
 *
 * This class is the shared substrate of every algorithm in this package -- the
 * solver, the singles-only oracle, the full-grid generator. It is deliberately
 * *not* thread-safe and *not* a value object: it is a scratchpad that gets reset
 * and reused, so that generating a puzzle allocates nothing after warm-up.
 */
class Grid {

    /** 0 = empty, 1..9 = placed. */
    val digits = IntArray(CELLS)

    /** 9-bit candidate mask per cell; always 0 for a filled cell. */
    val cand = IntArray(CELLS)

    /** Per unit, the mask of digits already placed in it. */
    val unitMask = IntArray(UNITS)

    var filled = 0
        private set

    // Backtracking undoes state by restoring a snapshot rather than by replaying a
    // trail of individual changes. Copying 189 ints costs well under a microsecond,
    // and the stack is allocated once, so search stays allocation-free -- while the
    // undo logic stays impossible to get subtly wrong.
    private val stackDigits = Array(MAX_DEPTH) { IntArray(CELLS) }
    private val stackCand = Array(MAX_DEPTH) { IntArray(CELLS) }
    private val stackUnit = Array(MAX_DEPTH) { IntArray(UNITS) }
    private val stackFilled = IntArray(MAX_DEPTH)
    private var top = 0

    fun clear() {
        digits.fill(0)
        cand.fill(Bits.ALL)
        unitMask.fill(0)
        filled = 0
        top = 0
    }

    /** Saves the current state so that a failed branch can be taken back. */
    fun push() {
        check(top < MAX_DEPTH) { "search deeper than $MAX_DEPTH -- impossible on a 9x9 grid" }
        digits.copyInto(stackDigits[top])
        cand.copyInto(stackCand[top])
        unitMask.copyInto(stackUnit[top])
        stackFilled[top] = filled
        top++
    }

    /** Restores the state saved by the matching [push]. */
    fun pop() {
        top--
        stackDigits[top].copyInto(digits)
        stackCand[top].copyInto(cand)
        stackUnit[top].copyInto(unitMask)
        filled = stackFilled[top]
    }

    /**
     * Places [digit] in [cell] and removes it from every peer.
     *
     * @return false if that leaves some empty cell with no candidate at all, i.e.
     *         the position is now provably dead. The caller must undo.
     */
    fun place(cell: Int, digit: Int): Boolean {
        val bit = Bits.of(digit)
        digits[cell] = digit
        cand[cell] = 0
        filled++
        for (u in Units.unitsOfCell[cell]) unitMask[u] = unitMask[u] or bit
        for (p in Units.peers[cell]) {
            if (cand[p] and bit != 0) {
                cand[p] = cand[p] and bit.inv()
                if (cand[p] == 0 && digits[p] == 0) return false
            }
        }
        return true
    }

    /**
     * Strikes a single candidate. @return true if it was still there.
     *
     * The counterpart to [place] for the human techniques: they argue a digit *out*
     * of a cell without knowing what goes in instead. Leaving an empty candidate
     * mask behind is a contradiction, but it is not this method's job to judge that
     * -- [HumanSolver] checks it, because only the caller knows whether an empty
     * cell means "the player wrote something wrong" or "this branch is refuted".
     */
    fun removeCandidate(cell: Int, digit: Int): Boolean {
        val bit = Bits.of(digit)
        if (digits[cell] != 0 || cand[cell] and bit == 0) return false
        cand[cell] = cand[cell] and bit.inv()
        return true
    }

    /** True if [digit] may legally go into the (empty) [cell]. */
    fun canPlace(cell: Int, digit: Int): Boolean =
        digits[cell] == 0 && Bits.contains(cand[cell], digit)

    /**
     * Loads a puzzle. @return false if the givens already contradict each other.
     */
    fun load(givens: IntArray): Boolean {
        clear()
        for (c in 0 until CELLS) {
            val d = givens[c]
            if (d != 0) {
                if (!canPlace(c, d)) return false
                if (!place(c, d)) return false
            }
        }
        return true
    }

    /**
     * Applies naked singles (a cell with one candidate left) and, if [useHidden],
     * hidden singles (a digit with only one possible cell in a unit) until nothing
     * changes any more.
     *
     * @return false if a contradiction surfaced.
     */
    fun propagate(useHidden: Boolean): Boolean {
        var progress = true
        while (progress) {
            progress = false

            for (c in 0 until CELLS) {
                if (digits[c] != 0) continue
                val m = cand[c]
                if (m == 0) return false
                if (Bits.count(m) == 1) {
                    if (!place(c, Bits.lowest(m))) return false
                    progress = true
                }
            }
            if (progress || !useHidden) continue

            for (u in 0 until UNITS) {
                val cells = Units.cellsOfUnit[u]
                val missing = Bits.ALL and unitMask[u].inv()
                var m = missing
                while (m != 0) {
                    val bit = m and (-m)
                    m = m xor bit
                    var spot = -1
                    var n = 0
                    for (c in cells) {
                        if (digits[c] == 0 && cand[c] and bit != 0) {
                            n++
                            if (n > 1) break
                            spot = c
                        }
                    }
                    if (n == 0) return false
                    if (n == 1) {
                        if (!place(spot, Integer.numberOfTrailingZeros(bit) + 1)) return false
                        progress = true
                    }
                }
            }
        }
        return true
    }

    /** The empty cell with the fewest candidates, or -1 if the grid is full. */
    fun bestBranchCell(): Int {
        var best = -1
        var bestCount = 10
        for (c in 0 until CELLS) {
            if (digits[c] != 0) continue
            val n = Bits.count(cand[c])
            if (n < bestCount) {
                bestCount = n
                best = c
                if (n <= 2) break
            }
        }
        return best
    }

    fun copyDigitsInto(out: IntArray) = digits.copyInto(out)

    companion object {
        /** A 9x9 search can never branch more than 81 times. */
        const val MAX_DEPTH = 81

        /** True if [digits] is a complete, rule-conforming solution. */
        fun isValidSolution(digits: IntArray): Boolean {
            if (digits.size != CELLS) return false
            for (u in 0 until UNITS) {
                var seen = 0
                for (c in Units.cellsOfUnit[u]) {
                    val d = digits[c]
                    if (d !in 1..9) return false
                    val bit = Bits.of(d)
                    if (seen and bit != 0) return false
                    seen = seen or bit
                }
            }
            return true
        }
    }
}
