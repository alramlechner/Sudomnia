package name.lechners.sudomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SolverTest {

    private val solver = Solver()

    @Test
    fun solvesPublishedPuzzlesToTheKnownSolution() {
        for (case in ReferencePuzzles.all) {
            val givens = Puzzle.parse(case.givens)
            val expected = Puzzle.parse(case.solution)

            val found = solver.solveUnique(givens)
            assertTrue("${case.name}: expected exactly one solution", found != null)
            assertTrue("${case.name}: wrong solution", expected.contentEquals(found!!))
        }
    }

    @Test
    fun countsAtMostTheRequestedLimit() {
        val givens = Puzzle.parse(ReferencePuzzles.all[0].givens)
        assertEquals(1, solver.countSolutions(givens, limit = 1))
        assertEquals(1, solver.countSolutions(givens, limit = 2))
    }

    @Test
    fun emptyGridIsMassivelyAmbiguous() {
        assertEquals(2, solver.countSolutions(IntArray(81), limit = 2))
    }

    @Test
    fun contradictoryGivensHaveNoSolution() {
        val twiceFiveInARow = IntArray(81).also { it[0] = 5; it[3] = 5 }
        assertEquals(0, solver.countSolutions(twiceFiveInARow, limit = 2))

        val twiceFiveInABox = IntArray(81).also { it[0] = 5; it[10] = 5 }
        assertEquals(0, solver.countSolutions(twiceFiveInABox, limit = 2))
    }

    /**
     * A mathematical fact that holds independently of any solver: if two digits are
     * missing from the clues entirely, they can be swapped throughout any solution,
     * so there are at least two. A solver that reports "unique" here is broken --
     * and this is exactly the failure that would silently produce unsolvable puzzles.
     */
    @Test
    fun aPuzzleMissingTwoDigitsEntirelyIsNeverUnique() {
        val rnd = Random(20260901)
        val generator = GridGenerator()
        repeat(25) {
            val full = generator.fullGrid(rnd)
            val a = rnd.nextInt(1, 10)
            var b = rnd.nextInt(1, 10)
            while (b == a) b = rnd.nextInt(1, 10)

            val givens = IntArray(81) { if (full[it] == a || full[it] == b) 0 else full[it] }
            assertEquals("digits $a and $b removed", 2, solver.countSolutions(givens, limit = 2))
        }
    }

    @Test
    fun theReportedSolutionAlwaysAgreesWithTheGivens() {
        val rnd = Random(4711)
        val factory = PuzzleFactory()
        for (level in Level.entries) {
            val puzzle = factory.generate(level, rnd)
            val solved = solver.solveUnique(puzzle.givens)!!
            assertTrue(Grid.isValidSolution(solved))
            for (c in 0 until 81) {
                if (puzzle.givens[c] != 0) {
                    assertEquals("cell $c", puzzle.givens[c], solved[c])
                }
            }
        }
    }
}
