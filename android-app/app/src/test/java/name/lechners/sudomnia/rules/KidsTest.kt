package name.lechners.sudomnia.rules

import name.lechners.sudomnia.data.GameRecord
import name.lechners.sudomnia.data.GameSnapshot
import name.lechners.sudomnia.data.History
import name.lechners.sudomnia.data.Outcome
import name.lechners.sudomnia.game.SudokuGame
import name.lechners.sudomnia.ui.board.BoardGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The children's 6x6 board: the geometry, and that the shared solver, generator and
 * hint code really work on it and not just on nine.
 */
class KidsTest {

    private val six = Units.SIX
    private val deep = System.getProperty("sudokuDeep") != null
    private val rounds = if (deep) 500 else 60

    @Test
    fun geometryOfTheSixBoard() {
        assertEquals(36, six.cells)
        assertEquals(18, six.unitCount)
        assertEquals(63, six.allMask)
        // Every unit has six distinct cells, and every cell sits in exactly three units.
        for (u in 0 until six.unitCount) assertEquals(6, six.cellsOfUnit[u].toSet().size)
        val membership = IntArray(six.cells)
        for (u in 0 until six.unitCount) for (c in six.cellsOfUnit[u]) membership[c]++
        assertTrue(membership.all { it == 3 })
        // 5 in the row + 5 in the column + the 2 box cells that are in neither (a 2x3 box
        // has 5 others, 2 of them in the cell's row and 1 in its column).
        assertTrue(six.peers.all { it.size == 12 })
    }

    @Test
    fun boxesAreTwoRowsByThreeColumns() {
        val box0 = six.cellsOfUnit[six.boxUnit(0)].toSet()
        assertEquals(setOf(0, 1, 2, 6, 7, 8), box0)
        val box1 = six.cellsOfUnit[six.boxUnit(1)].toSet()
        assertEquals(setOf(3, 4, 5, 9, 10, 11), box1)
        val box5 = six.cellsOfUnit[six.boxUnit(5)].toSet()
        assertEquals(setOf(27, 28, 29, 33, 34, 35), box5)
    }

    @Test
    fun fullGridsAreValidSolutions() {
        val generator = GridGenerator(six)
        val rnd = Random(1)
        repeat(rounds) { assertTrue(Grid.isValidSolution(generator.fullGrid(rnd), six)) }
    }

    @Test
    fun aNineGridIsNotASixSolution() {
        val nine = GridGenerator().fullGrid(Random(1))
        assertFalse(Grid.isValidSolution(nine, six))
    }

    @Test
    fun everyKidsPuzzleIsUniqueAndSolvableBySingles() {
        val factory = PuzzleFactory()
        val solver = Solver(six)
        val singles = SinglesSolver(six)
        val rnd = Random(2)
        repeat(rounds) {
            val p = factory.generate(Level.KIDS, rnd)
            assertEquals(Level.KIDS, p.level)
            assertEquals(36, p.givens.size)
            assertEquals(1, solver.countSolutions(p.givens, limit = 2))
            assertTrue("needs more than singles: ${p.toLine()}", singles.solves(p.givens, useHidden = true))
            assertTrue(Grid.isValidSolution(p.solution, six))
            assertTrue("too empty: ${p.clueCount}", p.clueCount >= Digger.KIDS_MIN_CLUES)
            for (c in 0 until 36) if (p.givens[c] != 0) assertEquals(p.solution[c], p.givens[c])
        }
    }

    @Test
    fun kidsPuzzlesStopAtTheClueFloor() {
        val factory = PuzzleFactory()
        val rnd = Random(3)
        val counts = List(40) { factory.generate(Level.KIDS, rnd).clueCount }
        println("kids clue counts: min=${counts.min()} avg=${counts.average()} max=${counts.max()}")
        assertTrue(counts.all { it >= Digger.KIDS_MIN_CLUES })
    }

    @Test
    fun theHumanLadderFinishesKidsPuzzlesAndNeverStrikesTheSolution() {
        val factory = PuzzleFactory()
        val human = HumanSolver(six)
        val rnd = Random(4)
        repeat(rounds) {
            val p = factory.generate(Level.KIDS, rnd)
            val result = human.solve(p.givens, trace = { step ->
                if (step.technique.places) {
                    assertEquals(p.solution[step.cell], step.digit)
                } else {
                    for (e in step.eliminations) assertTrue(p.solution[e.cell] != e.digit)
                }
            })
            assertTrue("ladder stuck on ${p.toLine()}", result.solved)
        }
    }

    @Test
    fun hintsWorkOnTheSixBoard() {
        val factory = PuzzleFactory()
        val finder = HintFinder(six)
        val rnd = Random(5)
        repeat(20) {
            val p = factory.generate(Level.KIDS, rnd)
            val hint = finder.find(p.givens, p.solution)
            assertTrue("expected a deduction, got $hint", hint is Hint.Deduce)
            val step = (hint as Hint.Deduce).last
            assertEquals(p.solution[step.cell], step.digit)
            assertTrue(step.units.all { it in 0 until six.unitCount })
        }
    }

    @Test
    fun lineRoundTripUsesTheBoardSize() {
        val p = PuzzleFactory().generate(Level.KIDS, Random(6))
        assertEquals(36, p.toLine().length)
        assertEquals(p.givens.toList(), Puzzle.parse(p.toLine(), six).toList())
    }

    @Test
    fun theDifficultyPickerListsOnlyTheClassicBands() {
        assertEquals(listOf(Level.EASY, Level.MEDIUM, Level.HARD, Level.EXPERT), Level.classic)
        assertTrue(Level.entries.contains(Level.KIDS))
    }

    // --- the game layer on the small board ------------------------------------

    @Test
    fun aKidsGameCountsSixDigitsAndPlaysToTheEnd() {
        val puzzle = PuzzleFactory().generate(Level.KIDS, Random(7))
        val game = SudokuGame(puzzle)
        assertEquals(7, game.remaining().size)
        assertEquals(36, game.entries.size)
        for (c in 0 until 36) if (!game.isGiven(c)) game.setDigit(c, puzzle.solution[c])
        assertTrue(game.isFull())
        assertTrue(game.isSolved())
        assertTrue(game.remaining().drop(1).all { it == 0 })
    }

    @Test
    fun aKidsGameSpotsConflictsAcrossABox() {
        val puzzle = PuzzleFactory().generate(Level.KIDS, Random(8))
        val game = SudokuGame(puzzle)
        val a = (0 until 36).first { !game.isGiven(it) }
        val b = six.peers[a].first { !game.isGiven(it) }
        val d = (1..6).first { x -> six.peers[a].none { game.valueAt(it) == x } }
        game.setDigit(a, d)
        game.setDigit(b, d)
        assertTrue(game.conflicts()[a])
        assertTrue(game.conflicts()[b])
    }

    @Test
    fun aKidsGameSurvivesTheSnapshotRoundTrip() {
        val puzzle = PuzzleFactory().generate(Level.KIDS, Random(9))
        val game = SudokuGame(puzzle)
        val cell = (0 until 36).first { !game.isGiven(it) }
        game.setDigit(cell, puzzle.solution[cell])
        game.toggleNote((0 until 36).first { !game.isGiven(it) && it != cell }, 5)
        val snapshot = GameSnapshot(
            givens = puzzle.toLine(), solution = puzzle.solution.joinToString("") { it.toString() },
            level = Level.KIDS,
            edits = game.exportHistory().map { GameSnapshot.encodeEdit(it) },
            applied = game.appliedCount, elapsedMs = 1000L, hintsUsed = 0, aidsUsed = false, counted = true,
        )
        val back = GameSnapshot.decode(snapshot.encode())!!.toGame()!!
        assertEquals(Level.KIDS, back.puzzle.level)
        val revived = SudokuGame(back.puzzle)
        assertTrue(revived.importHistory(back.history, back.applied, back.branchAt))
        assertEquals(game.entries.toList(), revived.entries.toList())
        assertEquals(game.notes.toList(), revived.notes.toList())
    }

    @Test
    fun aSixSnapshotWithNineCellsIsRejected() {
        val nine = PuzzleFactory().generate(Level.EASY, Random(10))
        val snapshot = GameSnapshot(
            givens = nine.toLine(), solution = nine.solution.joinToString("") { it.toString() },
            level = Level.KIDS, edits = emptyList(), applied = 0, elapsedMs = 0L,
            hintsUsed = 0, aidsUsed = false, counted = false,
        )
        assertEquals(null, snapshot.toGame())
    }

    @Test
    fun kidsGamesAreNeverRated() {
        val kids = GameRecord(0L, Level.KIDS, 20, 120_000L, 0, 0, true, Outcome.SOLVED)
        assertEquals(null, kids.rated)
        val easy = GameRecord(0L, Level.EASY, 20, 120_000L, 0, 0, true, Outcome.SOLVED)
        assertTrue(easy.rated != null)
        val history = History().plus(kids).plus(kids)
        assertEquals(null, history.rating())
    }

    @Test
    fun theBoardGeometryHitTestsTheSmallBoard() {
        val geo = BoardGeometry(600f, 6)
        assertEquals(100f, geo.cellSize, 0.001f)
        assertEquals(0, geo.cellAt(10f, 10f))
        assertEquals(35, geo.cellAt(599f, 599f))
        assertEquals(6 + 2, geo.cellAt(250f, 150f))
        assertEquals(-1, geo.cellAt(600f, 10f))
    }
}
