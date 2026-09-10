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

    // --- Zweig -------------------------------------------------------------

    /** The whole point of the feature: an attempt comes back in one step, exactly. */
    @Test
    fun discardingABranchRestoresDigitsAndNotesExactly() {
        val game = newGame()
        val rnd = Random(3)
        val empties = (0 until Units.CELLS).filter { !game.isGiven(it) }
        repeat(15) {
            val cell = empties.random(rnd)
            if (rnd.nextBoolean()) game.setDigit(cell, rnd.nextInt(1, 10))
            else game.toggleNote(cell, rnd.nextInt(1, 10))
        }
        val entriesBefore = game.entries.copyOf()
        val notesBefore = game.notes.copyOf()
        val depthBefore = game.appliedCount

        game.beginBranch()
        assertTrue(game.inBranch)
        repeat(15) {
            val cell = empties.random(rnd)
            if (rnd.nextBoolean()) game.setDigit(cell, rnd.nextInt(1, 10))
            else game.toggleNote(cell, rnd.nextInt(1, 10))
        }
        game.discardBranch()

        assertFalse(game.inBranch)
        assertTrue("Ziffern", entriesBefore.contentEquals(game.entries))
        assertTrue("Notizen -- auch die von Nachbarn gestrichenen", notesBefore.contentEquals(game.notes))
        assertEquals(depthBefore, game.appliedCount)
        assertFalse("ein verworfener Zweig darf nicht wiederholbar sein", game.canRedo)
    }

    @Test
    fun committingABranchKeepsEverythingAndClearsTheMark() {
        val game = newGame()
        val cell = firstEmpty(game)
        game.beginBranch()
        game.setDigit(cell, 4)
        game.commitBranch()

        assertFalse(game.inBranch)
        assertEquals(4, game.valueAt(cell))
        assertTrue("nach dem Übernehmen ist es ein Zug wie jeder andere", game.canUndo)
        game.undo()
        assertEquals(0, game.valueAt(cell))
    }

    /** Undo would otherwise leave the branch open around edits that left it. */
    @Test
    fun undoStopsAtTheBranchStart() {
        val game = newGame()
        val a = firstEmpty(game)
        val b = (0 until Units.CELLS).first { !game.isGiven(it) && it != a }
        game.setDigit(a, 1)
        game.beginBranch()
        game.setDigit(b, 2)

        assertTrue(game.undo())
        assertFalse("die Grenze haelt", game.canUndo)
        assertFalse(game.undo())
        assertEquals("der Zug vor dem Zweig steht noch", 1, game.valueAt(a))

        game.commitBranch()
        assertTrue("nach dem Übernehmen faellt die Grenze", game.canUndo)
    }

    @Test
    fun trialCellsAreTheOnesWhoseDigitChangedInTheBranch() {
        val game = newGame()
        val empties = (0 until Units.CELLS).filter { !game.isGiven(it) }
        val kept = empties[0]
        val tried = empties[1]
        val takenBack = empties[2]
        val noted = empties[3]

        game.setDigit(kept, 1)
        game.beginBranch()
        game.setDigit(tried, 2)
        game.setDigit(takenBack, 3)
        game.setDigit(takenBack, 3)        // dieselbe Ziffer nochmal: Feld wieder leer
        game.toggleNote(noted, 5)

        val trial = game.trialCells()
        assertTrue(trial[tried])
        assertFalse("vor dem Zweig gesetzt", trial[kept])
        assertFalse("wieder geleert, es steht nichts vom Versuch drin", trial[takenBack])
        assertFalse("nur eine Notiz", trial[noted])
        assertEquals(1, trial.count { it })

        game.discardBranch()
        assertTrue("ausserhalb eines Zweigs ist nichts vorlaeufig", game.trialCells().none { it })
    }

    @Test
    fun aBranchDoesNotNest() {
        val game = newGame()
        val a = firstEmpty(game)
        val b = (0 until Units.CELLS).first { !game.isGiven(it) && it != a }
        game.beginBranch()
        game.setDigit(a, 1)
        game.beginBranch()                 // ohne Wirkung
        game.setDigit(b, 2)
        game.discardBranch()

        assertEquals("beide Versuche sind weg", 0, game.valueAt(a))
        assertEquals(0, game.valueAt(b))
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

    // --- Konflikte in Notizen ------------------------------------------------

    /**
     * A pencil mark that clashes with a digit already on the board is impossible, and
     * with conflict marking on the app says so. Placing a digit clears it from the
     * peers' notes, so the only way to get one is to write it afterwards -- which is
     * exactly the mistake worth catching.
     */
    @Test
    fun aNoteThatClashesWithAPlacedDigitIsReported() {
        val game = newGame()
        val cell = firstEmpty(game)
        val peer = Units.peers[cell].first { !game.isGiven(it) && it != cell }

        game.setDigit(cell, 6)
        game.toggleNote(peer, 6)
        // Eine Ziffer, die in der Nachbarschaft des Feldes wirklich noch frei ist --
        // sonst prueft der zweite Teil des Tests nur die Vorgaben des Raetsels.
        val free = (1..9).first { d -> d != 6 && Units.peers[peer].none { game.valueAt(it) == d } }
        game.toggleNote(peer, free)

        val bad = game.noteConflicts()
        assertTrue("die 6 ist in dieser Einheit vergeben", Bits.contains(bad[peer], 6))
        assertFalse("die $free nicht", Bits.contains(bad[peer], free))
    }

    /** Two pencil marks of the same digit in one unit are both still possible. */
    @Test
    fun twoNotesOfTheSameDigitInAUnitAreNotAConflict() {
        val game = newGame()
        val cells = (0 until Units.CELLS).filter { !game.isGiven(it) }
        val a = cells.first()
        val b = Units.peers[a].first { !game.isGiven(it) }
        // A digit that is not yet placed anywhere near either cell.
        val digit = (1..9).first { d ->
            Units.peers[a].none { game.valueAt(it) == d } && Units.peers[b].none { game.valueAt(it) == d }
        }
        game.toggleNote(a, digit)
        game.toggleNote(b, digit)

        val bad = game.noteConflicts()
        assertEquals(0, bad[a])
        assertEquals(0, bad[b])
    }

    /** A given counts as a placed digit, exactly like a player's entry. */
    @Test
    fun aNoteThatClashesWithAGivenIsReported() {
        val game = newGame()
        val clue = (0 until Units.CELLS).first { game.isGiven(it) }
        val empty = Units.peers[clue].first { !game.isGiven(it) }
        game.toggleNote(empty, game.valueAt(clue))
        assertTrue(Bits.contains(game.noteConflicts()[empty], game.valueAt(clue)))
    }

    /** Undo takes the mark back, so the conflict has to go with it. */
    @Test
    fun theNoteConflictDisappearsWithTheNote() {
        val game = newGame()
        val cell = firstEmpty(game)
        val peer = Units.peers[cell].first { !game.isGiven(it) && it != cell }
        game.setDigit(cell, 3)
        game.toggleNote(peer, 3)
        assertTrue(game.noteConflicts()[peer] != 0)

        game.undo()
        assertEquals(0, game.noteConflicts()[peer])
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
