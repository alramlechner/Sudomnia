package name.lechners.sudomnia.data

import name.lechners.sudomnia.game.SudokuGame
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.rules.PuzzleFactory
import name.lechners.sudomnia.rules.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameSnapshotTest {

    private fun snapshotOf(game: SudokuGame, elapsed: Long = 1234L) = GameSnapshot(
        givens = game.puzzle.toLine(),
        solution = game.puzzle.solution.joinToString("") { it.toString() },
        level = game.puzzle.level,
        edits = game.exportHistory().map { GameSnapshot.encodeEdit(it) },
        applied = game.appliedCount,
        elapsedMs = elapsed,
        hintsUsed = 2,
        aidsUsed = true,
        counted = false,
    )

    /**
     * The invariant the whole feature rests on: a saved game must come back with the
     * digits, the pencil marks *and* the undo stack intact. Restoring only the visible
     * board would look right and lose every step of history behind it.
     */
    @Test
    fun aRoundTripRestoresDigitsNotesAndTheUndoStack() {
        val puzzle = PuzzleFactory().generate(Level.MEDIUM, Random(5))
        val game = SudokuGame(puzzle)
        val rnd = Random(9)
        val empties = (0 until Units.CELLS).filter { !game.isGiven(it) }

        repeat(30) {
            val cell = empties.random(rnd)
            if (rnd.nextBoolean()) game.setDigit(cell, rnd.nextInt(1, 10))
            else game.toggleNote(cell, rnd.nextInt(1, 10))
        }
        game.undo(); game.undo()   // leave a redo branch dangling, that has to survive too

        val text = snapshotOf(game).encode()
        val restore = GameSnapshot.decode(text)!!.toGame()!!
        val revived = SudokuGame(restore.puzzle)
        assertTrue(revived.importHistory(restore.history, restore.applied))

        assertTrue("Ziffern", game.entries.contentEquals(revived.entries))
        assertTrue("Notizen", game.notes.contentEquals(revived.notes))
        assertEquals("Undo-Tiefe", game.appliedCount, revived.appliedCount)
        assertEquals(game.canUndo, revived.canUndo)
        assertEquals("Redo-Zweig", game.canRedo, revived.canRedo)

        // and the revived history really is walkable
        while (revived.canUndo) revived.undo()
        assertTrue(revived.entries.all { it == 0 })
        assertTrue(revived.notes.all { it == 0 })
    }

    @Test
    fun aFreshGameRoundTripsToo() {
        val game = SudokuGame(PuzzleFactory().generate(Level.EASY, Random(1)))
        val back = GameSnapshot.decode(snapshotOf(game).encode())!!
        assertEquals(0, back.applied)
        assertTrue(back.edits.isEmpty())
        assertEquals(Level.EASY, back.level)
        assertTrue(back.toGame() != null)
    }

    @Test
    fun theScalarFieldsSurvive() {
        val game = SudokuGame(PuzzleFactory().generate(Level.HARD, Random(2)))
        val back = GameSnapshot.decode(snapshotOf(game, elapsed = 98_765L).encode())!!
        assertEquals(98_765L, back.elapsedMs)
        assertEquals(2, back.hintsUsed)
        assertTrue(back.aidsUsed)
        assertFalse(back.counted)
    }

    /** Anything unreadable must be dropped, never thrown -- it runs at app startup. */
    @Test
    fun garbageIsDiscardedRatherThanThrown() {
        assertNull(GameSnapshot.decode(null))
        assertNull(GameSnapshot.decode(""))
        assertNull(GameSnapshot.decode("nonsense"))
        assertNull(GameSnapshot.decode("1|too|few|fields"))
        assertNull(GameSnapshot.decode("99|" + "1".repeat(81) + "|x|EASY|0|0|0|0|0|"))
    }

    @Test
    fun aMalformedEditRowIsRejected() {
        val game = SudokuGame(PuzzleFactory().generate(Level.EASY, Random(3)))
        game.setDigit((0 until Units.CELLS).first { !game.isGiven(it) }, 4)
        val broken = snapshotOf(game).copy(edits = listOf("1:2>3"))   // vier statt fuenf Feldern
        assertNull(GameSnapshot.decode(broken.encode())!!.toGame())
    }

    /** Givens and solution from two different games must not be spliced together. */
    @Test
    fun aSolutionThatDoesNotMatchTheGivensIsRejected() {
        val a = PuzzleFactory().generate(Level.EASY, Random(4))
        val b = PuzzleFactory().generate(Level.EASY, Random(6))
        val game = SudokuGame(a)
        val mixed = snapshotOf(game).copy(solution = b.solution.joinToString("") { it.toString() })
        assertNull(GameSnapshot.decode(mixed.encode())!!.toGame())
    }
}
