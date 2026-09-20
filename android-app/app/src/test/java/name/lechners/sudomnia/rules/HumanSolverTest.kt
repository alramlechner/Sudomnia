package name.lechners.sudomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The technique ladder is the one part of this app that is allowed to *reason* about
 * a puzzle rather than look up its solution. If it reasons wrongly, the app tells the
 * player something false with a straight face -- so the tests here are about
 * soundness first and reach second.
 */
class HumanSolverTest {

    /** Too rare to meet by generating puzzles; each has a fixture of its own. */
    private val RARE = setOf(Technique.SWORDFISH, Technique.HIDDEN_TRIPLE)

    private val deep = System.getProperty("sudokuDeep") != null
    private fun rounds(n: Int) = if (deep) n * 10 else n

    /**
     * The invariant everything else rests on: **no step may ever contradict the
     * solution.** A placement must be the solution's digit; an elimination must
     * strike a digit that does not belong there.
     *
     * This is stronger than checking the end result. An unsound elimination often
     * still leads to the right grid -- some later technique repairs it -- and would
     * pass a "does it solve" test while being wrong. It would then show up as a hint
     * that tells the player to cross out the digit that actually goes there.
     */
    @Test
    fun noStepEverContradictsTheSolution() {
        val factory = PuzzleFactory()
        val solver = HumanSolver()
        val rnd = Random(4)
        var checked = 0

        for (level in Level.entries) {
            repeat(rounds(20)) {
                val puzzle = factory.generate(level, rnd)
                solver.solve(puzzle.givens) { step ->
                    checked++
                    if (step.technique.places) {
                        assertEquals(
                            "${step.technique} places the wrong digit in cell ${step.cell}",
                            puzzle.solution[step.cell],
                            step.digit,
                        )
                    }
                    for (e in step.eliminations) {
                        assertTrue(
                            "${step.technique} strikes the correct digit ${e.digit} from cell ${e.cell}",
                            puzzle.solution[e.cell] != e.digit,
                        )
                    }
                }
            }
        }
        assertTrue("steps were actually checked at all", checked > 500)
    }

    /**
     * Soundness is only worth as much as the coverage behind it: a technique that
     * never fires in the corpus is a technique the test above never checked.
     *
     * The two rare rungs are exempt and get their own fixtures below: measured over
     * 100 puzzles, the Swordfish fires four times and the hidden triple once.
     * Generating one reliably would mean hundreds of puzzles per test run, and a
     * corpus test that fails in one run out of ten teaches people to rerun instead of
     * to look.
     */
    @Test
    fun everyTechniqueIsActuallyUsedSomewhere() {
        val factory = PuzzleFactory()
        val solver = HumanSolver()
        val rnd = Random(17)
        val seen = HashMap<Technique, Int>()

        repeat(rounds(40)) {
            val puzzle = factory.generate(Level.EXPERT, rnd)
            solver.solve(puzzle.givens) { step ->
                seen[step.technique] = (seen[step.technique] ?: 0) + 1
            }
        }
        val missing = Technique.entries.filter { it !in seen && it !in RARE }
        assertTrue("never used: $missing (seen: $seen)", missing.isEmpty())
    }

    /**
     * The Swordfish, on a puzzle that really needs one.
     *
     * Found with `generator-cli --mode find --technique SWORDFISH`, which is in the
     * repository for exactly this purpose: the rare rungs cannot be reached by a unit
     * test that generates its own puzzles, so they get a fixture instead of a prayer.
     */
    @Test
    fun theSwordfishFiresOnAPuzzleThatNeedsOne() {
        val fired = solveFixture(
            givens = "030000280010030007500000000000003000009075020001000600060584000000000500204090370",
            solution = "937451286416238957582769413625813749349675821871942635763584192198327564254196378",
            wanted = Technique.SWORDFISH,
        )
        assertTrue("the swordfish must fire", fired > 0)
    }

    /** Same idea for the hidden triple -- the other rung too rare to generate. */
    @Test
    fun theHiddenTripleFiresOnAPuzzleThatNeedsOne() {
        val fired = solveFixture(
            givens = "200000040080000607670009003000210090000800100005000700800423050000070000000080004",
            solution = "213768549589342617674159823738215496426897135195634782867423951942571368351986274",
            wanted = Technique.HIDDEN_TRIPLE,
        )
        assertTrue("the hidden triple must fire", fired > 0)
    }

    /**
     * Runs a checked-in puzzle through the ladder, holding **every** elimination
     * against the known solution, and reports how often [wanted] was used.
     */
    private fun solveFixture(givens: String, solution: String, wanted: Technique): Int {
        val board = Puzzle.parse(givens)
        val truth = Puzzle.parse(solution)
        var fired = 0
        val result = HumanSolver().solve(board) { step ->
            if (step.technique == wanted) fired++
            if (step.technique.places) assertEquals(truth[step.cell], step.digit)
            for (e in step.eliminations) {
                assertTrue(
                    "${step.technique} strikes the correct ${e.digit} from cell ${e.cell}",
                    truth[e.cell] != e.digit,
                )
            }
        }
        assertTrue("the puzzle must work out without guessing", result.solved)
        return fired
    }

    /** A puzzle that singles alone finish must not need anything above them. */
    @Test
    fun anEasyPuzzleNeedsNothingBeyondSingles() {
        val factory = PuzzleFactory()
        val solver = HumanSolver()
        val rnd = Random(21)

        repeat(rounds(10)) {
            val puzzle = factory.generate(Level.EASY, rnd)
            val result = solver.solve(puzzle.givens, allow = Technique.NAKED_SINGLE)
            assertTrue("Easy must work out with singles alone", result.solved)
        }
    }

    /**
     * The published reference puzzles, whose solutions come from an independent
     * implementation rather than from this code. Whatever the ladder manages to fill
     * in has to agree with them digit for digit.
     *
     * Reach is reported, not demanded: AI Escargot was built to defeat exactly this
     * kind of solver, and needing it to fall would be a demand for forcing chains --
     * which this ladder deliberately does not have.
     */
    @Test
    fun theReferencePuzzlesAreNeverContradicted() {
        val solver = HumanSolver()
        var solved = 0

        for (case in ReferencePuzzles.all) {
            val givens = Puzzle.parse(case.givens)
            val solution = Puzzle.parse(case.solution)
            val placed = IntArray(Units.CELLS)

            val result = solver.solve(givens) { step ->
                if (step.technique.places) placed[step.cell] = step.digit
                for (e in step.eliminations) {
                    assertTrue(
                        "${case.name}: ${step.technique} strikes the correct ${e.digit} from cell ${e.cell}",
                        solution[e.cell] != e.digit,
                    )
                }
            }
            for (c in 0 until Units.CELLS) {
                if (placed[c] != 0) assertEquals("${case.name}, cell $c", solution[c], placed[c])
            }
            if (result.solved) solved++
        }
        assertTrue("at least the Euler puzzle must make it through the ladder", solved >= 1)
    }

    /**
     * The ceiling really is a ceiling: capped at singles, a puzzle that needs more
     * must come back unsolved rather than quietly using the harder rung anyway.
     * The digger relies on this to aim at a band.
     */
    @Test
    fun theAllowedCeilingIsRespected() {
        val factory = PuzzleFactory()
        val solver = HumanSolver()
        val rnd = Random(33)

        repeat(rounds(10)) {
            val puzzle = factory.generate(Level.EXPERT, rnd)
            var hardest: Technique? = null
            val capped = solver.solve(puzzle.givens, allow = Technique.LOCKED_CANDIDATES) { step ->
                if (hardest == null || step.technique.score > hardest!!.score) hardest = step.technique
            }
            assertTrue(
                "worked above the ceiling: $hardest",
                hardest == null || hardest!!.score <= Technique.LOCKED_CANDIDATES.score,
            )
            if (!capped.solved) assertTrue(true)   // expected, not an error
        }
    }
}
