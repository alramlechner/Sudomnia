package name.lechners.sudomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The properties that matter for a puzzle generator. Slow runs are gated behind
 * `-DsudokuDeep=1` so the everyday loop stays fast.
 */
class GeneratorTest {

    private val deep = System.getProperty("sudokuDeep") != null
    private val perLevel = if (deep) 200 else 20

    private val solver = Solver()
    private val generator = GridGenerator()
    private val digger = Digger()
    private val singles = SinglesSolver()

    @Test
    fun fullGridsAreValidSolutions() {
        val rnd = Random(1)
        repeat(if (deep) 500 else 60) {
            assertTrue(Grid.isValidSolution(generator.fullGrid(rnd)))
        }
    }

    @Test
    fun successiveFullGridsDiffer() {
        val rnd = Random(2)
        val a = generator.fullGrid(rnd)
        val b = generator.fullGrid(rnd)
        assertNotEquals(a.toList(), b.toList())
    }

    /** The whole point of the generator: never ship an ambiguous puzzle. */
    @Test
    fun everyGeneratedPuzzleHasExactlyOneSolution() {
        val factory = PuzzleFactory()
        val rnd = Random(3)
        for (level in Level.entries) {
            repeat(perLevel) {
                val puzzle = factory.generate(level, rnd)
                assertEquals(
                    "$level puzzle with ${puzzle.clueCount} clues is not unique",
                    1, solver.countSolutions(puzzle.givens, limit = 2),
                )
                assertTrue(Grid.isValidSolution(puzzle.solution))
            }
        }
    }

    /**
     * **The promise of the whole app**: no puzzle it hands out needs guessing.
     *
     * Before the technique ladder existed this was false and nobody could see it --
     * the old "hard" band accepted any puzzle with a unique solution, and a
     * measurement over 150 of them found that **53 % could not be finished by any
     * human technique**. A player who got one had no way of telling the difference
     * between "I am not seeing it" and "there is nothing to see".
     */
    @Test
    fun everyGeneratedPuzzleIsSolvableWithoutGuessing() {
        val factory = PuzzleFactory()
        val grader = Grader()
        val rnd = Random(4711)
        for (level in Level.entries) {
            repeat(perLevel) {
                val puzzle = factory.generate(level, rnd)
                val grade = grader.grade(puzzle.givens)
                assertTrue(
                    "$level with ${puzzle.clueCount} clues needs guessing: ${puzzle.toLine()}",
                    grade.solvableWithoutGuessing,
                )
            }
        }
    }

    /**
     * The four bands must actually be different, not just differently labelled --
     * and each one must be what [Level] says it is. This is what would break first
     * if the directed digging in [Digger] regressed.
     */
    @Test
    fun theLevelBandsAreDistinct() {
        val factory = PuzzleFactory()
        val grader = Grader()
        val rnd = Random(5)
        repeat(perLevel) {
            val easy = factory.generate(Level.EASY, rnd)
            if (easy.level == Level.EASY) {
                assertTrue("easy must fall to singles", singles.solves(easy.givens, true))
                assertTrue("easy keeps clues", easy.clueCount >= Digger.EASY_MIN_CLUES)
            }

            val medium = factory.generate(Level.MEDIUM, rnd)
            if (medium.level == Level.MEDIUM) {
                assertTrue("medium must fall to singles", singles.solves(medium.givens, true))
                assertTrue("medium is dug deeper", medium.clueCount < Digger.EASY_MIN_CLUES)
            }

            val hard = factory.generate(Level.HARD, rnd)
            if (hard.level == Level.HARD) {
                val hardest = grader.grade(hard.givens).hardest!!
                assertTrue("hard must outlast singles", hardest.score > Technique.NAKED_SINGLE.score)
                assertTrue(
                    "hard must not need more than a subset, needed $hardest",
                    hardest.score <= Technique.HIDDEN_TRIPLE.score,
                )
            }

            val expert = factory.generate(Level.EXPERT, rnd)
            if (expert.level == Level.EXPERT) {
                val hardest = grader.grade(expert.givens).hardest!!
                assertTrue(
                    "expert must need a fish, colouring or a wing, needed $hardest",
                    hardest.score >= Technique.X_WING.score,
                )
            }
        }
    }

    /** The label is measured, never assumed -- so it must survive re-measuring. */
    @Test
    fun theReportedLevelIsTheMeasuredLevel() {
        val factory = PuzzleFactory()
        val rnd = Random(6)
        for (level in Level.entries) {
            repeat(perLevel) {
                val puzzle = factory.generate(level, rnd)
                assertEquals(puzzle.level, digger.classify(puzzle.givens))
            }
        }
    }

    /**
     * Records what the generator actually produces, and pins the only clue-count
     * property that the digging strategy really guarantees.
     *
     * Deliberately *not* asserted: that medium has more clues than hard. The first
     * version of this test did, and it failed -- measured means were 24.4 vs 25.1.
     * Puzzles that need real techniques and maximally dug singles-only puzzles both
     * bottom out around 25 clues, so clue count simply does not separate them. That
     * is the whole reason the full build needs a technique-based grader.
     */
    @Test
    fun clueCountsAreRecordedAndEasyStandsApart() {
        val factory = PuzzleFactory()
        val rnd = Random(7)
        val counts = mutableMapOf<Level, MutableList<Int>>()
        for (level in Level.entries) {
            counts[level] = mutableListOf()
            repeat(perLevel) { counts[level]!! += factory.generate(level, rnd).clueCount }
        }
        val avg = counts.mapValues { (_, v) -> v.average() }
        println("clue counts: " + avg.entries.joinToString { "${it.key}=%.1f".format(it.value) })

        assertTrue("easy must keep clearly more clues", avg[Level.EASY]!! > avg[Level.MEDIUM]!! + 5)
        assertTrue("17 is the proven minimum", counts.values.flatten().min() >= 17)
    }

    /** Dumps a few puzzles so they can be checked against an outside implementation. */
    @Test
    fun dumpSamplesForExternalCrossCheck() {
        val factory = PuzzleFactory()
        val rnd = Random(99)
        for (level in Level.entries) {
            repeat(3) { println("SAMPLE $level " + factory.generate(level, rnd).toLine()) }
        }
    }

    /**
     * Nothing more can be taken out.
     *
     * Minimality used to mean "removing any clue breaks uniqueness", and that is no
     * longer the claim: the digger stops when the *band* breaks, which happens well
     * before ambiguity does. The honest statement, and the one the digger really
     * guarantees, is that every remaining clue is load-bearing for one of the two
     * reasons -- take it away and the puzzle either stops having one solution or
     * stops being solvable without guessing.
     *
     * Checked on [Level.EXPERT], the band dug to the very limit of the ladder.
     */
    @Test
    fun noFurtherClueCanBeRemovedFromAnExpertPuzzle() {
        val factory = PuzzleFactory()
        val grader = Grader()
        val rnd = Random(8)
        repeat(if (deep) 20 else 3) {
            val puzzle = factory.generate(Level.EXPERT, rnd)
            val givens = puzzle.givens.copyOf()
            for (c in 0 until 81) {
                if (givens[c] == 0) continue
                val saved = givens[c]
                givens[c] = 0
                val ambiguous = solver.countSolutions(givens, limit = 2) > 1
                val needsGuessing = !grader.grade(givens).solvableWithoutGuessing
                assertTrue(
                    "cell $c could have been removed too -- neither ambiguous nor unsolvable",
                    ambiguous || needsGuessing,
                )
                givens[c] = saved
            }
        }
    }
}
