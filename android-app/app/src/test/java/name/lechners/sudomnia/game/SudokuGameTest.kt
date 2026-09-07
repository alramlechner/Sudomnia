package name.lechners.sudomnia.game

import name.lechners.sudomnia.rules.Bits
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.rules.Puzzle
import name.lechners.sudomnia.rules.PuzzleFactory
import name.lechners.sudomnia.rules.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuGameTest {

    private fun newGame(level: Level = Level.EASY, seed: Int = 42) =
        SudokuGame(PuzzleFactory().generate(level, Random(seed)))

    private fun firstEmpty(game: SudokuGame) = (0 until Units.CELLS).first { !game.isGiven(it) }

    @Test
    fun cluesCannotBeOverwritten() {
        val game = newGame()
        val clue = (0 until Units.CELLS).first { game.isGiven(it) }
        val before = game.valueAt(clue)
        game.setDigit(clue, if (before == 9) 1 else 9)
        assertEquals(before, game.valueAt(clue))
        assertFalse(game.canUndo)
    }

    @Test
    fun placingTheSameDigitTwiceClearsTheCell() {
        val game = newGame()
        val cell = firstEmpty(game)
        game.setDigit(cell, 5)
        assertEquals(5, game.valueAt(cell))
        game.setDigit(cell, 5)
        assertEquals(0, game.valueAt(cell))
    }

    @Test
    fun placingADigitRemovesItFromPeerNotes() {
        val game = newGame()
        val cell = firstEmpty(game)
        val peer = Units.peers[cell].first { !game.isGiven(it) && it != cell }

        game.toggleNote(peer, 7)
        game.toggleNote(peer, 3)
        assertTrue(Bits.contains(game.notes[peer], 7))

        game.setDigit(cell, 7)
        assertFalse("the 7 must be gone from the peer's notes", Bits.contains(game.notes[peer], 7))
        assertTrue("unrelated notes must survive", Bits.contains(game.notes[peer], 3))
    }

    /** The invariant that makes undo trustworthy: digits *and* notes come back. */
    @Test
    fun undoRestoresDigitsAndNotesExactly() {
        val game = newGame()
        val rnd = Random(7)
        val empties = (0 until Units.CELLS).filter { !game.isGiven(it) }

        repeat(40) {
            val cell = empties.random(rnd)
            if (rnd.nextBoolean()) game.setDigit(cell, rnd.nextInt(1, 10))
            else game.toggleNote(cell, rnd.nextInt(1, 10))
        }
        val entriesAfter = game.entries.copyOf()
        val notesAfter = game.notes.copyOf()

        var steps = 0
        while (game.undo()) steps++
        assertTrue(steps > 0)
        assertTrue("all entries back to empty", game.entries.all { it == 0 })
        assertTrue("all notes back to empty", game.notes.all { it == 0 })

        while (game.redo()) Unit
        assertTrue(entriesAfter.contentEquals(game.entries))
        assertTrue(notesAfter.contentEquals(game.notes))
    }

    @Test
    fun aNewActionDiscardsTheRedoBranch() {
        val game = newGame()
        val cell = firstEmpty(game)
        game.setDigit(cell, 1)
        game.undo()
        assertTrue(game.canRedo)
        game.setDigit(cell, 2)
        assertFalse(game.canRedo)
        assertEquals(2, game.valueAt(cell))
    }

    @Test
    fun conflictsAreDetectedInRowColumnAndBox() {
        val game = newGame()
        val cell = firstEmpty(game)
        val peer = Units.peers[cell].first { !game.isGiven(it) }
        game.setDigit(cell, 4)
        game.setDigit(peer, 4)

        val bad = game.conflicts()
        assertTrue(bad[cell])
        assertTrue(bad[peer])
    }

    @Test
    fun remainingCountsDownAsDigitsArePlaced() {
        val game = newGame()
        val before = game.remaining()
        val cell = firstEmpty(game)
        game.setDigit(cell, 6)
        assertEquals(before[6] - 1, game.remaining()[6])
    }

    @Test
    fun fillingInTheSolutionSolvesTheGame() {
        val puzzle = PuzzleFactory().generate(Level.MEDIUM, Random(11))
        val game = SudokuGame(puzzle)
        assertFalse(game.isSolved())
        for (c in 0 until Units.CELLS) {
            if (!game.isGiven(c)) game.setDigit(c, puzzle.solution[c])
        }
        assertTrue(game.isSolved())
        assertTrue(game.conflicts().none { it })
    }

    @Test
    fun aFullButWrongGridIsNotSolved() {
        val puzzle = PuzzleFactory().generate(Level.EASY, Random(12))
        val game = SudokuGame(puzzle)
        val empties = (0 until Units.CELLS).filter { !game.isGiven(it) }
        for (c in empties) game.setDigit(c, puzzle.solution[c])
        // Swap two of the player's own digits: the grid stays full, but breaks the rules.
        val a = empties[0]
        val b = empties.first { puzzle.solution[it] != puzzle.solution[a] }
        game.setDigit(a, puzzle.solution[b])
        assertFalse(game.isSolved())
    }

    @Test
    fun notesAreIgnoredOnAFilledCell() {
        val game = newGame()
        val cell = firstEmpty(game)
        game.setDigit(cell, 3)
        game.toggleNote(cell, 8)
        assertEquals(0, game.notes[cell])
    }

    @Test
    fun parseAndRenderRoundTrip() {
        val line = "003020600900305001001806400008102900700000008006708200002609500800203009005010300"
        assertEquals(line.replace('0', '.'), Puzzle(Puzzle.parse(line), IntArray(81), Level.EASY).toLine())
    }
}
