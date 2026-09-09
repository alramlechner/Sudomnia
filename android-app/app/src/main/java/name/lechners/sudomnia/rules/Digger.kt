package name.lechners.sudomnia.rules

import kotlin.random.Random

/**
 * Carves a puzzle out of a finished grid.
 *
 * Cells are emptied in random order; a cell stays empty only if the puzzle still
 * has exactly one solution afterwards. Uniqueness is therefore guaranteed by
 * construction, never checked after the fact.
 *
 * ### Aiming at a difficulty band without throwing puzzles away
 *
 * The naive approach -- dig as deep as possible, grade the result, discard it if it
 * landed in the wrong band -- means rolling the dice until it comes up right, which
 * for the hardest band costs seconds. Instead the digger *stops digging when a
 * bound breaks*: a removal is taken back as soon as singles would no longer finish
 * the puzzle, and for [Level.EASY] digging also stops at [EASY_MIN_CLUES]. Every
 * level is a single pass with no retries at all.
 *
 * Not thread-safe -- one instance per thread.
 */
class Digger {

    private val solver = Solver()
    private val singles = SinglesSolver()
    private val grader = Grader()
    private val order = IntArray(Units.CELLS) { it }

    fun dig(full: IntArray, level: Level, rnd: Random): IntArray {
        val puzzle = full.copyOf()
        var clues = Units.CELLS
        shuffleOrder(rnd)

        for (cell in order) {
            if (level == Level.EASY && clues <= EASY_MIN_CLUES) break

            val saved = puzzle[cell]
            puzzle[cell] = 0

            val stillFine = !hasAlternativeSolution(puzzle, cell, saved) && allows(puzzle, level)
            if (stillFine) clues-- else puzzle[cell] = saved
        }
        return puzzle
    }

    /**
     * Classifies an existing puzzle by solving it the way a person would.
     *
     * Used to label what actually came out of [dig], so the level shown to the player
     * is measured rather than intended. Falls back to [Level.EXPERT] for a puzzle the
     * ladder cannot finish -- [dig] does not produce those any more, but a puzzle
     * restored from an old saved game might be one.
     */
    fun classify(givens: IntArray): Level = grader.grade(givens).level ?: Level.EXPERT

    /**
     * May this puzzle keep the cell that was just emptied?
     *
     * The bound is the band's ceiling, so the check is as cheap as the band is easy:
     * the singles-only bands never leave the fast propagation oracle, and only the
     * expert band pays for the whole ladder on every removal.
     *
     * For [Level.EXPERT] the bound is not a ceiling but a floor of a different kind:
     * the full ladder must still finish the puzzle. That is what keeps guessing out
     * of the game -- more than half of the maximally dug puzzles this generator used
     * to hand out as "hard" could not be finished by any technique at all.
     */
    private fun allows(puzzle: IntArray, level: Level): Boolean = when (level) {
        Level.EASY, Level.MEDIUM -> singles.solves(puzzle, useHidden = true)
        Level.HARD -> grader.solvesWithin(puzzle, Technique.HIDDEN_TRIPLE)
        Level.EXPERT -> grader.solvesWithin(puzzle, Technique.entries.last())
    }

    /**
     * Is there a solution with some *other* digit in [cell]? If so the puzzle is
     * ambiguous and the cell has to go back.
     *
     * This is about three times faster than counting up to two solutions: each trial
     * fixes a wrong digit into a heavily over-constrained grid, so propagation alone
     * usually finds the contradiction without ever branching.
     *
     * Leaves `puzzle[cell]` empty.
     */
    private fun hasAlternativeSolution(puzzle: IntArray, cell: Int, correct: Int): Boolean {
        for (d in 1..9) {
            if (d == correct) continue
            puzzle[cell] = d
            val solvable = solver.countSolutions(puzzle, limit = 1) > 0
            puzzle[cell] = 0
            if (solvable) return true
        }
        return false
    }

    companion object {
        /**
         * Where the easy band stops digging. Both easy and medium puzzles fall to
         * singles alone; what separates them is how many deductions are left to make.
         * 36 clues means fewer than half the grid is blank.
         */
        const val EASY_MIN_CLUES = 36
    }

    private fun shuffleOrder(rnd: Random) {
        for (i in order.indices) order[i] = i
        for (i in order.size - 1 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val t = order[i]; order[i] = order[j]; order[j] = t
        }
    }
}
