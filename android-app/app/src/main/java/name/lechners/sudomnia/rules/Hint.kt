package name.lechners.sudomnia.rules

/**
 * What the app can tell a stuck player.
 *
 * Every hint is a **step of the technique ladder taken from the board as it stands**
 * ([HumanSolver.nextStep]) -- the same code that grades the puzzle. So the hint can
 * always say *why*, and it can only ever say something the player could have seen
 * themselves.
 *
 * ### Why the reasoning is not "worked out backwards from the solution"
 *
 * The solution is known -- it falls out of generation and is kept in
 * [Puzzle.solution] -- so revealing a digit costs nothing. It also teaches nothing,
 * and it cannot be checked: a player who is told "the 7 goes here" learns only that
 * the app knows more than they do. A hint that names the argument can be followed,
 * disagreed with, and next time found unaided.
 *
 * [Reveal] survives for the one case the ladder cannot handle -- see there.
 */
sealed interface Hint {

    /**
     * The chain of deductions up to the next digit that can be written down.
     *
     * A chain rather than one step, because an elimination does not change the board:
     * shown a single one, the player would get the very same hint on the next press.
     * The last step places a digit -- unless the ladder ran out first, which for a
     * puzzle from this generator does not happen.
     */
    data class Deduce(val steps: List<Step>) : Hint {
        val last: Step get() = steps.last()
    }

    /**
     * The ladder found nothing. Reveals the cell with the fewest possibilities, with
     * no reasoning attached.
     *
     * This should now be unreachable for a puzzle from this app's generator -- every
     * one of those is solvable by the ladder ([Digger]). It stays for two cases that
     * are not hypothetical: a saved game from an older version, whose puzzle was dug
     * without that guarantee, and a board where the player's own correct-but-unlucky
     * entries have left a position the ladder cannot crack.
     */
    data class Reveal(val cell: Int, val digit: Int, val candidateCount: Int) : Hint

    /** The entries made so far can no longer be completed to a solution. */
    data object DeadEnd : Hint
}

/**
 * Picks the hint to give for a board.
 *
 * ### The player's pencil marks are not consulted
 *
 * [find] does not even take them. Notes are incomplete (most players only mark where
 * they are currently thinking), they go stale, and they can simply be wrong. A hint
 * derived from them would be provably wrong, and the player would have no way to
 * notice -- to them the app is the authority. The candidate state from
 * `Grid.load(board)` is canonical instead: the same board yields the same hint for
 * everyone.
 *
 * Not thread-safe -- one instance per thread, called off the main thread.
 */
class HintFinder {

    private val grid = Grid()
    private val solver = Solver()
    private val human = HumanSolver()

    /**
     * @param board 81 cells, givens and player entries together, 0 for empty
     * @param solution the puzzle's unique solution
     */
    fun find(board: IntArray, solution: IntArray): Hint {
        // Two ways the board can already be dead. The check has to come first: on a
        // dead board every deduction is an argument inside a contradiction, and the
        // reveal below would place a digit from the solution that collides with
        // whatever the player got wrong -- they would see an inexplicable conflict.
        if (!grid.load(board)) return Hint.DeadEnd
        if (solver.countSolutions(board, limit = 1) == 0) return Hint.DeadEnd

        human.nextSteps(board).takeIf { it.isNotEmpty() }?.let { return Hint.Deduce(it) }

        val cell = grid.bestBranchCell()
        if (cell < 0) return Hint.DeadEnd   // full board; the caller should not have asked
        return Hint.Reveal(cell, solution[cell], Bits.count(grid.cand[cell]))
    }
}
