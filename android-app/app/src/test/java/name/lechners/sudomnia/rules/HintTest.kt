package name.lechners.sudomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HintTest {

    private val deep = System.getProperty("sudokuDeep") != null
    private val perLevel = if (deep) 100 else 12

    private val finder = HintFinder()

    /** Plays [moves] correct digits from the solution into random empty cells. */
    private fun partiallyPlayed(puzzle: Puzzle, moves: Int, rnd: Random): IntArray {
        val board = puzzle.givens.copyOf()
        val empties = (0 until Units.CELLS).filter { board[it] == 0 }.shuffled(rnd)
        for (c in empties.take(moves)) board[c] = puzzle.solution[c]
        return board
    }

    /**
     * The one that matters. Whatever the hint names, it must be the digit the unique
     * solution has in that cell -- otherwise the app confidently places a digit that
     * is guaranteed wrong, on request, which is the worst bug this feature can have.
     */
    @Test
    fun aHintNeverNamesTheWrongDigit() {
        val factory = PuzzleFactory()
        val rnd = Random(20260903)
        for (level in Level.entries) {
            repeat(perLevel) {
                val puzzle = factory.generate(level, rnd)
                val blanks = Units.CELLS - puzzle.clueCount
                // Sample the whole game, from untouched to nearly finished.
                for (moves in 0 until blanks step maxOf(1, blanks / 8)) {
                    val board = partiallyPlayed(puzzle, moves, rnd)
                    when (val hint = finder.find(board, puzzle.solution)) {
                        is Hint.Forced -> {
                            assertEquals(
                                "$level, $moves gesetzt: falsche Ziffer in Zelle ${hint.cell}",
                                puzzle.solution[hint.cell], hint.digit,
                            )
                            assertEquals("Tipp auf ein belegtes Feld", 0, board[hint.cell])
                        }
                        is Hint.Reveal -> {
                            assertEquals(
                                "$level, $moves gesetzt: falsche Ziffer in Zelle ${hint.cell}",
                                puzzle.solution[hint.cell], hint.digit,
                            )
                            assertEquals("Tipp auf ein belegtes Feld", 0, board[hint.cell])
                        }
                        Hint.DeadEnd -> throw AssertionError(
                            "$level, $moves korrekte Zuege: Brett faelschlich fuer tot erklaert"
                        )
                    }
                }
            }
        }
    }

    /** If something is forced, take the explainable route -- never fall back to a bare reveal. */
    @Test
    fun aForcedCellIsAlwaysPreferredOverABareReveal() {
        val factory = PuzzleFactory()
        val rnd = Random(7)
        val singles = SinglesSolver()
        repeat(perLevel * 2) {
            val puzzle = factory.generate(Level.MEDIUM, rnd)
            val board = partiallyPlayed(puzzle, 5, rnd)
            // The board is singles-solvable by construction, so something is forced.
            assertTrue(singles.solves(board, useHidden = true))
            assertTrue(
                "erwartet Forced, war ${finder.find(board, puzzle.solution)}",
                finder.find(board, puzzle.solution) is Hint.Forced,
            )
        }
    }

    /** An easy puzzle is singles-solvable, so its very first hint must be explainable. */
    @Test
    fun theFirstHintOnAnEasyPuzzleIsExplainable() {
        val factory = PuzzleFactory()
        val rnd = Random(11)
        repeat(perLevel) {
            val puzzle = factory.generate(Level.EASY, rnd)
            assertTrue(finder.find(puzzle.givens, puzzle.solution) is Hint.Forced)
        }
    }

    /** A wrong entry kills the board, and the hint has to notice before revealing anything. */
    @Test
    fun aWrongEntryIsReportedAsADeadEnd() {
        val factory = PuzzleFactory()
        val rnd = Random(13)
        repeat(perLevel) {
            val puzzle = factory.generate(Level.MEDIUM, rnd)
            val board = puzzle.givens.copyOf()
            val empty = (0 until Units.CELLS).first { board[it] == 0 }
            val right = puzzle.solution[empty]
            val wrong = (1..9).first { it != right }
            board[empty] = wrong
            assertEquals(Hint.DeadEnd, finder.find(board, puzzle.solution))
        }
    }

    /** A duplicate in a unit is a dead end too, and must not throw on the way there. */
    @Test
    fun aRuleBreakingBoardIsADeadEnd() {
        val puzzle = PuzzleFactory().generate(Level.EASY, Random(17))
        val board = puzzle.givens.copyOf()
        val given = (0 until Units.CELLS).first { board[it] != 0 }
        val peer = Units.peers[given].first { board[it] == 0 }
        board[peer] = board[given]
        assertEquals(Hint.DeadEnd, finder.find(board, puzzle.solution))
    }

    /** Same board, same hint -- otherwise pressing twice would move the highlight around. */
    @Test
    fun theSameBoardAlwaysYieldsTheSameHint() {
        val factory = PuzzleFactory()
        val rnd = Random(19)
        repeat(perLevel) {
            val puzzle = factory.generate(Level.HARD, rnd)
            val board = partiallyPlayed(puzzle, 3, rnd)
            assertEquals(
                finder.find(board, puzzle.solution),
                finder.find(board, puzzle.solution),
            )
        }
    }

    /**
     * Plays every forced cell until none is left -- exactly where a player who only
     * knows singles runs out of road.
     */
    private fun playedToStandstill(puzzle: Puzzle): IntArray {
        val board = puzzle.givens.copyOf()
        while (true) {
            val hint = finder.find(board, puzzle.solution)
            if (hint !is Hint.Forced) return board
            board[hint.cell] = hint.digit
        }
    }

    /**
     * The number that decides whether the technique ladder is worth building.
     *
     * Measured at the sticking point, not at random positions: filling in random
     * correct digits unlocks singles that the player could not have deduced, which
     * flatters the result to ~100%. What matters is the moment the player is actually
     * stuck -- and on HARD that moment has, by construction, no single left, so the
     * hint there is a bare reveal.
     *
     * A bare reveal is not useless: it restarts the puzzle, because one revealed cell
     * usually makes several more forced. This test records how far a single reveal
     * carries, which is the honest measure of what the cheap hint buys.
     */
    @Test
    fun reportsHowFarASingleRevealCarriesOnHardPuzzles() {
        val factory = PuzzleFactory()
        val rnd = Random(23)
        var reveals = 0
        var forcedAfterwards = 0
        var stuckAtStart = 0

        repeat(perLevel * 2) {
            val puzzle = factory.generate(Level.HARD, rnd)
            var board = playedToStandstill(puzzle)
            if (board.count { it == 0 } > 0) stuckAtStart++

            // Break the deadlock once, then see how many cells fall out for free.
            while (board.count { it == 0 } > 0) {
                val hint = finder.find(board, puzzle.solution)
                if (hint !is Hint.Reveal) break
                board[hint.cell] = hint.digit
                reveals++
                val before = board.count { it == 0 }
                board = playedToStandstill(Puzzle(board, puzzle.solution, puzzle.level))
                forcedAfterwards += before - board.count { it == 0 } - 1
            }
        }
        println(
            "HARD: %d von %d Raetseln laufen mit Singles allein fest; ".format(stuckAtStart, perLevel * 2) +
                "%d Aufdeckungen noetig, je Aufdeckung %.1f weitere Felder geschenkt"
                    .format(reveals, forcedAfterwards.toDouble() / reveals)
        )
    }
}
