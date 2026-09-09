package name.lechners.sudomnia.rules

/**
 * Turns a puzzle into a difficulty band by solving it the way a person would.
 *
 * One line of reasoning, one place: [Digger] uses it to aim, the app uses it to label,
 * and the hint uses the same [HumanSolver] underneath. Three separate notions of
 * "hard" would be three chances to disagree with each other in front of the player.
 *
 * Not thread-safe -- one instance per thread.
 */
class Grader {

    private val solver = HumanSolver()

    /**
     * @param level    the band, or null if the ladder could not finish the puzzle at
     *                 all -- meaning it can only be completed by guessing
     * @param hardest  the most expensive technique the path actually needed
     * @param score    the sum over all steps: three hard deductions cost more than one
     */
    class Grade(
        val level: Level?,
        val hardest: Technique?,
        val score: Int,
        val steps: Int,
    ) {
        val solvableWithoutGuessing: Boolean get() = level != null
    }

    fun grade(givens: IntArray): Grade {
        val result = solver.solve(givens)
        val clues = givens.count { it != 0 }
        return Grade(
            level = if (result.solved) bandOf(result.hardest, clues) else null,
            hardest = result.hardest,
            score = result.score,
            steps = result.steps,
        )
    }

    /** Can the ladder finish this puzzle using nothing above [ceiling]? */
    fun solvesWithin(givens: IntArray, ceiling: Technique): Boolean =
        solver.solve(givens, allow = ceiling).solved

    companion object {

        /**
         * The band boundaries, in one expression so that they can be read as a whole.
         *
         * The two cuts are where the *kind* of reasoning changes, not where a score
         * threshold happens to fall: from "look at one cell or one unit" to "compare
         * two units" (locked candidates, subsets), and from there to "compare a
         * pattern across the whole grid" (fish, colouring, wings).
         */
        fun bandOf(hardest: Technique?, clues: Int): Level = when {
            hardest == null || hardest.score <= Technique.NAKED_SINGLE.score ->
                if (clues >= Digger.EASY_MIN_CLUES) Level.EASY else Level.MEDIUM
            hardest.score <= Technique.HIDDEN_TRIPLE.score -> Level.HARD
            else -> Level.EXPERT
        }

        /** The hardest technique a band may need. The digger digs against this. */
        fun ceilingOf(level: Level): Technique = when (level) {
            Level.EASY, Level.MEDIUM -> Technique.NAKED_SINGLE
            Level.HARD -> Technique.HIDDEN_TRIPLE
            Level.EXPERT -> Technique.entries.last()
        }
    }
}
