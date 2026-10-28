package name.lechners.sudomnia.game

import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.rules.PuzzleFactory
import name.lechners.sudomnia.rules.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class MistakeAnalysisTest {

    private val puzzle = PuzzleFactory().generate(Level.EASY, Random(3))
    private val empties = (0 until Units.CELLS).filter { !SudokuGame(puzzle).isGiven(it) }

    private fun wrongDigit(cell: Int) = if (puzzle.solution[cell] == 9) 1 else puzzle.solution[cell] + 1

    private fun analysisOf(game: SudokuGame) =
        MistakeAnalysis(puzzle, game.exportHistory(), game.appliedCount)

    @Test
    fun findsOnlyTheWrongDigitsAndTheirPosition() {
        val g = SudokuGame(puzzle)
        g.setDigit(empties[0], puzzle.solution[empties[0]])
        g.setDigit(empties[1], wrongDigit(empties[1]))
        g.setDigit(empties[2], puzzle.solution[empties[2]])
        g.setDigit(empties[3], wrongDigit(empties[3]))

        val a = analysisOf(g)
        assertEquals(listOf(2, 4), a.wrongMoves.map { it.step })
        assertEquals(empties[1], a.wrongMoves[0].cell)
        assertEquals(puzzle.solution[empties[1]], a.wrongMoves[0].correct)
        assertEquals(4, a.nextWrongStep(2)).also { assertEquals(2, a.previousWrongStep(4)) }
        assertNull(a.nextWrongStep(4))
        assertNull(a.previousWrongStep(2))
        assertNotNull(a.wrongMoveAt(2))
        assertNull(a.wrongMoveAt(3))
    }

    @Test
    fun replaysTheBoardAsItWasAtThatMove() {
        val g = SudokuGame(puzzle)
        g.setDigit(empties[0], puzzle.solution[empties[0]])
        g.setDigit(empties[1], wrongDigit(empties[1]))
        g.setDigit(empties[2], puzzle.solution[empties[2]])

        val at = analysisOf(g).positionAt(2)!!
        assertEquals(wrongDigit(empties[1]), at.valueAt(empties[1]))
        assertEquals(0, at.valueAt(empties[2]))
        assertEquals(0, analysisOf(g).positionAt(0)!!.valueAt(empties[0]))
    }

    @Test
    fun aCleanGameHasNoWrongMoves() {
        val g = SudokuGame(puzzle)
        g.setDigit(empties[0], puzzle.solution[empties[0]])
        assertEquals(0, analysisOf(g).wrongMoves.size)
    }

    @Test
    fun marksTellCorrectFromWrongMoves() {
        val g = SudokuGame(puzzle)
        g.setDigit(empties[0], puzzle.solution[empties[0]])
        g.setDigit(empties[1], wrongDigit(empties[1]))
        g.toggleNote(empties[2], 1)
        assertEquals(listOf(1, 2, 0), analysisOf(g).marks.toList())
    }
}
