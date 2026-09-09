package name.lechners.sudomnia.rules

/**
 * Solves the way a person does: only with techniques that can be explained in a
 * sentence, and never by guessing.
 *
 * This is the grader ([Grader]) and the hint ([HintFinder]) in one piece of code,
 * because they must not be able to disagree. If the hint could justify a step the
 * grader did not count, the displayed difficulty would describe a different puzzle
 * than the one being played.
 *
 * ### It never branches
 *
 * [Solver] backtracks and answers "how many solutions"; this one refuses to try
 * things out. That is the whole point: a puzzle it cannot finish is one that a
 * player could only finish by trial and error, and [Digger] does not publish those.
 * The trial branch in the app exists for the player's own choice to guess, not
 * because the puzzle demands it.
 *
 * ### Steps are found, not applied, one at a time
 *
 * [nextStep] answers "what can be deduced from *this* board", which is exactly the
 * question a stuck player asks. It works from the candidates the board itself
 * implies, never from the player's pencil marks -- see [HintFinder].
 *
 * Not thread-safe: one instance per thread. Allocations per step are small and
 * bounded; grading a puzzle runs the whole ladder repeatedly, which is why the
 * cheap techniques are tried first and the expensive ones only when nothing else
 * bites.
 */
class HumanSolver {

    private val grid = Grid()

    /**
     * What the ladder made of a puzzle.
     *
     * @param solved     did it finish without ever guessing?
     * @param hardest    the most expensive technique that was actually needed
     * @param score      the sum over all steps -- separates "three hard steps" from "one"
     * @param steps      how many deductions the path took
     */
    class Result(
        val solved: Boolean,
        val hardest: Technique?,
        val score: Int,
        val steps: Int,
    )

    /**
     * Runs the ladder to the end.
     *
     * @param allow the hardest technique that may be used; anything above it is not
     *        even looked for. Passing the band's ceiling turns "how hard is this?"
     *        into the much cheaper "is this within the band?", which is what the
     *        digger asks after every single removal.
     */
    fun solve(
        givens: IntArray,
        allow: Technique = Technique.entries.last(),
        /**
         * Every step as it is taken. The tests use it to hold each deduction against
         * the known solution -- an elimination that strikes the true digit is the one
         * failure mode a solver like this can have, and it would be invisible in the
         * end result as long as some other technique cleaned up afterwards.
         */
        trace: ((Step) -> Unit)? = null,
    ): Result {
        if (!grid.load(givens)) return Result(false, null, 0, 0)

        var hardest: Technique? = null
        var score = 0
        var steps = 0

        while (grid.filled < Units.CELLS) {
            val step = findStep(allow) ?: return Result(false, hardest, score, steps)
            trace?.invoke(step)
            if (!apply(step)) return Result(false, hardest, score, steps)
            steps++
            score += step.technique.score
            if (hardest == null || step.technique.score > hardest.score) hardest = step.technique
        }
        return Result(true, hardest, score, steps)
    }

    /**
     * The single next deduction available on [board], or null if the ladder is stuck
     * (or the board already contradicts itself).
     *
     * The board is read as-is: a step is only offered if it follows from what is
     * written down right now. A hidden single that exists solely because of an
     * X-Wing three moves ago is *not* offered as a hidden single -- the X-Wing is,
     * and the single comes on the next press. That is what makes the chain of hints
     * followable instead of magic.
     */
    fun nextStep(board: IntArray): Step? {
        if (!grid.load(board)) return null
        return findStep(Technique.entries.last())
    }

    /**
     * The chain of deductions from [board] up to and including the next one that
     * actually writes a digit.
     *
     * The hint needs the chain, not a single step, for a plain reason: an elimination
     * does not change the board. Ask again after being shown one and the same step
     * comes back, forever. Walking the chain instead turns the hint into the thing
     * the player was missing -- "cross this out, then that, and now the 7 is forced".
     *
     * @param max a termination guard, not a curated length. Measured over 2.563 hints
     *        across all four bands, **2.514 chains are a single step**; 49 are longer
     *        and the longest ever seen was 20. So the cap is set well above what the
     *        game produces and exists only so that a pathological board cannot spin.
     */
    fun nextSteps(board: IntArray, max: Int = 40): List<Step> {
        if (!grid.load(board)) return emptyList()
        val chain = ArrayList<Step>(max)
        while (chain.size < max) {
            val step = findStep(Technique.entries.last()) ?: break
            chain += step
            if (step.technique.places) break
            if (!apply(step)) break
        }
        return chain
    }

    // --- Der Leiterlauf ------------------------------------------------------

    private fun findStep(allow: Technique): Step? {
        for (technique in Technique.entries) {
            if (technique.score > allow.score) return null
            val step = when (technique) {
                Technique.HIDDEN_SINGLE -> hiddenSingle()
                Technique.NAKED_SINGLE -> nakedSingle()
                Technique.LOCKED_CANDIDATES -> lockedCandidates()
                Technique.NAKED_PAIR -> nakedSubset(2, Technique.NAKED_PAIR)
                Technique.HIDDEN_PAIR -> hiddenSubset(2, Technique.HIDDEN_PAIR)
                Technique.NAKED_TRIPLE -> nakedSubset(3, Technique.NAKED_TRIPLE)
                Technique.HIDDEN_TRIPLE -> hiddenSubset(3, Technique.HIDDEN_TRIPLE)
                Technique.X_WING -> fish(2, Technique.X_WING)
                Technique.SWORDFISH -> fish(3, Technique.SWORDFISH)
                Technique.SIMPLE_COLOURING -> simpleColouring()
                Technique.XY_WING -> xyWing()
            }
            if (step != null) return step
        }
        return null
    }

    /** @return false if the step leads into a contradiction -- the board was already wrong. */
    private fun apply(step: Step): Boolean {
        if (step.technique.places) return grid.place(step.cell, step.digit)
        var changed = false
        for (e in step.eliminations) {
            if (grid.removeCandidate(e.cell, e.digit)) changed = true
            if (grid.cand[e.cell] == 0 && grid.digits[e.cell] == 0) return false
        }
        // A step that strikes nothing would loop forever. Every finder below only
        // reports a pattern once it has an elimination to show, so this is a guard
        // against a future finder, not a known case.
        return changed
    }

    // --- Die Techniken -------------------------------------------------------

    /** A digit with exactly one remaining place in a unit. */
    private fun hiddenSingle(): Step? {
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
                    return Step(
                        technique = Technique.HIDDEN_SINGLE,
                        cell = spot,
                        digit = Integer.numberOfTrailingZeros(bit) + 1,
                        pattern = intArrayOf(spot),
                        units = intArrayOf(u),
                    )
                }
            }
        }
        return null
    }

    /** A cell with exactly one candidate left. */
    private fun nakedSingle(): Step? {
        for (c in 0 until Units.CELLS) {
            if (grid.digits[c] != 0) continue
            if (Bits.count(grid.cand[c]) == 1) {
                return Step(
                    technique = Technique.NAKED_SINGLE,
                    cell = c,
                    digit = Bits.lowest(grid.cand[c]),
                    pattern = intArrayOf(c),
                )
            }
        }
        return null
    }

    /**
     * Locked candidates, both directions.
     *
     * *Pointing*: inside a box a digit is confined to one row or column, so it is
     * gone from the rest of that line. *Claiming*: inside a line a digit is confined
     * to one box, so it is gone from the rest of that box.
     *
     * Both are the same observation from two sides, they are found by the same scan,
     * and a player uses them interchangeably -- hence one rung, not two.
     */
    private fun lockedCandidates(): Step? {
        for (u in 0 until Units.UNITS) {
            var m = Bits.ALL and grid.unitMask[u].inv()
            while (m != 0) {
                val bit = m and (-m)
                m = m xor bit
                val digit = Integer.numberOfTrailingZeros(bit) + 1

                val spots = ArrayList<Int>(9)
                for (c in Units.cellsOfUnit[u]) {
                    if (grid.digits[c] == 0 && grid.cand[c] and bit != 0) spots += c
                }
                if (spots.size < 2) continue

                // The other unit that happens to contain all of them.
                for (other in Units.unitsOfCell[spots[0]]) {
                    if (other == u) continue
                    if (spots.any { other !in Units.unitsOfCell[it] }) continue

                    val gone = ArrayList<Elimination>(6)
                    for (c in Units.cellsOfUnit[other]) {
                        if (c in spots) continue
                        if (grid.digits[c] == 0 && grid.cand[c] and bit != 0) gone += Elimination(c, digit)
                    }
                    if (gone.isEmpty()) continue
                    return Step(
                        technique = Technique.LOCKED_CANDIDATES,
                        digit = digit,
                        pattern = spots.toIntArray(),
                        units = intArrayOf(u, other),
                        eliminations = gone,
                    )
                }
            }
        }
        return null
    }

    /**
     * [size] cells in one unit that between them hold exactly [size] digits: those
     * digits belong to those cells, so they leave the rest of the unit.
     */
    private fun nakedSubset(size: Int, technique: Technique): Step? {
        for (u in 0 until Units.UNITS) {
            val open = Units.cellsOfUnit[u].filter { grid.digits[it] == 0 && Bits.count(grid.cand[it]) in 2..size }
            if (open.size < size) continue

            forEachCombination(open.size, size) { pick ->
                var mask = 0
                for (i in pick) mask = mask or grid.cand[open[i]]
                if (Bits.count(mask) != size) return@forEachCombination null

                val members = IntArray(size) { open[pick[it]] }
                val gone = ArrayList<Elimination>(9)
                for (c in Units.cellsOfUnit[u]) {
                    if (grid.digits[c] != 0 || c in members) continue
                    Bits.forEach(grid.cand[c] and mask) { d -> gone += Elimination(c, d) }
                }
                if (gone.isEmpty()) return@forEachCombination null
                Step(
                    technique = technique,
                    pattern = members,
                    units = intArrayOf(u),
                    digitMask = mask,
                    eliminations = gone,
                )
            }?.let { return it }
        }
        return null
    }

    /**
     * [size] digits in one unit that between them can only go into [size] cells:
     * those cells belong to those digits, so everything else leaves the cells.
     *
     * The mirror image of [nakedSubset] -- and the harder one to see, which is why
     * it sits a rung above at each size.
     */
    private fun hiddenSubset(size: Int, technique: Technique): Step? {
        for (u in 0 until Units.UNITS) {
            val cells = Units.cellsOfUnit[u]
            val missing = ArrayList<Int>(9)
            Bits.forEach(Bits.ALL and grid.unitMask[u].inv()) { d ->
                val n = cells.count { grid.digits[it] == 0 && Bits.contains(grid.cand[it], d) }
                if (n in 2..size) missing += d
            }
            if (missing.size < size) continue

            forEachCombination(missing.size, size) { pick ->
                var mask = 0
                for (i in pick) mask = mask or Bits.of(missing[i])
                val members = cells.filter { grid.digits[it] == 0 && grid.cand[it] and mask != 0 }
                if (members.size != size) return@forEachCombination null

                val gone = ArrayList<Elimination>(9)
                for (c in members) {
                    Bits.forEach(grid.cand[c] and mask.inv()) { d -> gone += Elimination(c, d) }
                }
                if (gone.isEmpty()) return@forEachCombination null
                Step(
                    technique = technique,
                    pattern = members.toIntArray(),
                    units = intArrayOf(u),
                    digitMask = mask,
                    eliminations = gone,
                )
            }?.let { return it }
        }
        return null
    }

    /**
     * Fish: [size] rows in which a digit sits in the same [size] columns (or the
     * other way round). The digit then owns those intersections, so it leaves the
     * rest of those columns.
     *
     * size 2 is the X-Wing, size 3 the Swordfish. Same code, and that is the point:
     * they are the same argument at two widths.
     */
    private fun fish(size: Int, technique: Technique): Step? {
        for (digit in 1..9) {
            val bit = Bits.of(digit)
            for (byRow in booleanArrayOf(true, false)) {
                val lines = ArrayList<Int>(9)          // line index 0..8
                val places = ArrayList<IntArray>(9)    // cells of that line holding the digit
                for (line in 0..8) {
                    val u = if (byRow) line else 9 + line
                    val spots = Units.cellsOfUnit[u].filter {
                        grid.digits[it] == 0 && grid.cand[it] and bit != 0
                    }
                    if (spots.size in 2..size) { lines += line; places += spots.toIntArray() }
                }
                if (lines.size < size) continue

                forEachCombination(lines.size, size) { pick ->
                    var crossMask = 0
                    for (i in pick) for (c in places[i]) {
                        crossMask = crossMask or (1 shl if (byRow) Units.colOf[c] else Units.rowOf[c])
                    }
                    if (Integer.bitCount(crossMask) != size) return@forEachCombination null

                    val corners = ArrayList<Int>(size * size)
                    for (i in pick) for (c in places[i]) corners += c
                    val usedLines = pick.map { lines[it] }

                    val gone = ArrayList<Elimination>(9)
                    for (cross in 0..8) {
                        if (crossMask and (1 shl cross) == 0) continue
                        val u = if (byRow) 9 + cross else cross
                        for (c in Units.cellsOfUnit[u]) {
                            val line = if (byRow) Units.rowOf[c] else Units.colOf[c]
                            if (line in usedLines) continue
                            if (grid.digits[c] == 0 && grid.cand[c] and bit != 0) gone += Elimination(c, digit)
                        }
                    }
                    if (gone.isEmpty()) return@forEachCombination null
                    Step(
                        technique = technique,
                        digit = digit,
                        pattern = corners.toIntArray(),
                        units = usedLines.map { if (byRow) it else 9 + it }.toIntArray(),
                        eliminations = gone,
                    )
                }?.let { return it }
            }
        }
        return null
    }

    /**
     * XY-Wing: a pivot {x,y} with two wings {x,z} and {y,z}. Whichever digit the
     * pivot takes, one wing becomes z -- so z is gone from every cell that sees both
     * wings.
     *
     * The first technique here that argues about a *case split* rather than about a
     * pattern, and the last one this app is willing to explain in one sentence.
     */
    private fun xyWing(): Step? {
        val bivalue = (0 until Units.CELLS).filter { grid.digits[it] == 0 && Bits.count(grid.cand[it]) == 2 }
        for (pivot in bivalue) {
            val pm = grid.cand[pivot]
            for (a in bivalue) {
                if (a == pivot || !sees(a, pivot)) continue
                val am = grid.cand[a]
                if (Integer.bitCount(am and pm) != 1) continue
                for (b in bivalue) {
                    if (b == pivot || b == a || !sees(b, pivot)) continue
                    val bm = grid.cand[b]
                    if (Integer.bitCount(bm and pm) != 1) continue
                    if (am and pm == bm and pm) continue          // both wings on the same pivot digit
                    if (am or bm or pm != (am or bm)) continue    // the three digits must close up
                    val zMask = am and bm
                    if (Integer.bitCount(zMask) != 1 || zMask and pm != 0) continue

                    val digit = Bits.lowest(zMask)
                    val gone = ArrayList<Elimination>(8)
                    for (c in 0 until Units.CELLS) {
                        if (c == a || c == b || c == pivot) continue
                        if (grid.digits[c] != 0 || grid.cand[c] and zMask == 0) continue
                        if (sees(c, a) && sees(c, b)) gone += Elimination(c, digit)
                    }
                    if (gone.isEmpty()) continue
                    return Step(
                        technique = Technique.XY_WING,
                        digit = digit,
                        pattern = intArrayOf(pivot, a, b),
                        eliminations = gone,
                    )
                }
            }
        }
        return null
    }

    /**
     * Simple colouring on one digit: follow the units where it has exactly two
     * places left, painting the two ends in opposite colours.
     *
     * Two conclusions come out of that, and both are reported as this one rung:
     * a colour that appears twice in the same unit is false everywhere, and a cell
     * that can see both colours cannot hold the digit at all.
     */
    private fun simpleColouring(): Step? {
        for (digit in 1..9) {
            val bit = Bits.of(digit)
            val colour = IntArray(Units.CELLS) { 0 }      // 0 = unpainted, 1/-1 = the two colours

            for (start in 0 until Units.CELLS) {
                if (colour[start] != 0) continue
                if (grid.digits[start] != 0 || grid.cand[start] and bit == 0) continue
                if (!hasConjugate(start, bit)) continue

                val chain = ArrayList<Int>(16)
                colour[start] = 1
                chain += start
                var i = 0
                while (i < chain.size) {
                    val c = chain[i++]
                    for (u in Units.unitsOfCell[c]) {
                        val partner = conjugatePartner(c, u, bit) ?: continue
                        if (colour[partner] != 0) continue
                        colour[partner] = -colour[c]
                        chain += partner
                    }
                }
                if (chain.size < 4) continue

                // Rule 2: the same colour twice in one unit -- that colour is false.
                for (a in chain) for (b in chain) {
                    if (a >= b || colour[a] != colour[b] || !sees(a, b)) continue
                    val bad = colour[a]
                    val gone = chain.filter { colour[it] == bad }.map { Elimination(it, digit) }
                    return Step(
                        technique = Technique.SIMPLE_COLOURING,
                        digit = digit,
                        pattern = chain.toIntArray(),
                        eliminations = gone,
                    )
                }

                // Rule 4: an outside cell that sees both colours cannot hold the digit.
                val gone = ArrayList<Elimination>(8)
                for (c in 0 until Units.CELLS) {
                    if (colour[c] != 0 || grid.digits[c] != 0 || grid.cand[c] and bit == 0) continue
                    val seesPlus = chain.any { colour[it] == 1 && sees(c, it) }
                    val seesMinus = chain.any { colour[it] == -1 && sees(c, it) }
                    if (seesPlus && seesMinus) gone += Elimination(c, digit)
                }
                if (gone.isNotEmpty()) {
                    return Step(
                        technique = Technique.SIMPLE_COLOURING,
                        digit = digit,
                        pattern = chain.toIntArray(),
                        eliminations = gone,
                    )
                }
            }
        }
        return null
    }

    // --- Kleinkram -----------------------------------------------------------

    private fun sees(a: Int, b: Int): Boolean =
        a != b && (Units.rowOf[a] == Units.rowOf[b] ||
            Units.colOf[a] == Units.colOf[b] ||
            Units.boxOf[a] == Units.boxOf[b])

    private fun hasConjugate(cell: Int, bit: Int): Boolean =
        Units.unitsOfCell[cell].any { conjugatePartner(cell, it, bit) != null }

    /** The other cell, if [cell]'s unit [u] has exactly two places left for the digit. */
    private fun conjugatePartner(cell: Int, u: Int, bit: Int): Int? {
        var partner = -1
        var n = 0
        for (c in Units.cellsOfUnit[u]) {
            if (grid.digits[c] != 0 || grid.cand[c] and bit == 0) continue
            n++
            if (n > 2) return null
            if (c != cell) partner = c
        }
        return if (n == 2 && partner >= 0) partner else null
    }

    /**
     * Every [k]-subset of `0 until n`, stopping at the first one [body] accepts.
     *
     * Written out rather than pulled from a combinatorics helper because the hot
     * path is a 9-choose-3 loop that runs for all 27 units on every single step.
     */
    private inline fun forEachCombination(n: Int, k: Int, body: (IntArray) -> Step?): Step? {
        val pick = IntArray(k) { it }
        while (true) {
            body(pick)?.let { return it }
            var i = k - 1
            while (i >= 0 && pick[i] == n - k + i) i--
            if (i < 0) return null
            pick[i]++
            for (j in i + 1 until k) pick[j] = pick[j - 1] + 1
        }
    }
}
