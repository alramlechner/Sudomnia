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
     * The one that matters. Whatever the hint says, it must agree with the unique
     * solution: a placement names the digit that belongs there, an elimination strikes
     * one that does not. Otherwise the app confidently tells the player something
     * false, on request, which is the worst bug this feature can have.
     */
    @Test
    fun aHintNeverSaysSomethingFalse() {
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
                        is Hint.Deduce -> for (step in hint.steps) {
                            if (step.technique.places) {
                                assertEquals(
                                    "$level, $moves entered: wrong digit in cell ${step.cell}",
                                    puzzle.solution[step.cell], step.digit,
                                )
                                assertEquals("hint on an occupied cell", 0, board[step.cell])
                            }
                            for (e in step.eliminations) {
                                assertTrue(
                                    "$level, $moves entered: ${step.technique} strikes the correct ${e.digit}",
                                    puzzle.solution[e.cell] != e.digit,
                                )
                            }
                        }
                        is Hint.Reveal -> {
                            assertEquals(
                                "$level, $moves entered: wrong digit in cell ${hint.cell}",
                                puzzle.solution[hint.cell], hint.digit,
                            )
                            assertEquals("hint on an occupied cell", 0, board[hint.cell])
                        }
                        Hint.DeadEnd -> throw AssertionError(
                            "$level, $moves correct moves: board wrongly declared dead"
                        )
                    }
                }
            }
        }
    }

    /**
     * **The payoff of the technique ladder.** Every hint, on every level, at every
     * point of the game, comes with a reason -- a bare reveal never happens.
     *
     * Before the ladder this was measurably false: on the hard band *every* puzzle ran
     * dry of singles by construction, so the hint at the sticking point could only
     * uncover a digit and say nothing. The old test in this file measured how far one
     * such uncovering carried (17 further cells, ~2 per puzzle); that number was the
     * argument for building the ladder, and this test is what replaced it.
     */
    @Test
    fun everyHintIsExplainable() {
        val factory = PuzzleFactory()
        val rnd = Random(31)
        var hints = 0
        var longest = 0
        val lengths = HashMap<Int, Int>()

        for (level in Level.entries) {
            repeat(perLevel) {
                val puzzle = factory.generate(level, rnd)
                val board = puzzle.givens.copyOf()
                // Walk the puzzle the way the hint button does: apply what it says to
                // enter, and keep asking until the grid is full.
                var guard = 0
                while (board.any { it == 0 } && guard++ < 200) {
                    val hint = finder.find(board, puzzle.solution)
                    assertTrue(
                        "$level: blank reveal instead of a reason",
                        hint is Hint.Deduce,
                    )
                    val chain = (hint as Hint.Deduce).steps
                    longest = maxOf(longest, chain.size)
                    lengths[chain.size] = (lengths[chain.size] ?: 0) + 1
                    val last = hint.last
                    assertTrue(
                        "the chain must end in a placement (length ${chain.size})",
                        last.technique.places,
                    )
                    board[last.cell] = last.digit
                    hints++
                }
                assertTrue("$level: puzzle not played through", board.none { it == 0 })
            }
        }
        println("hint chains: longest $longest, distribution ${lengths.toSortedMap()}")
        assertTrue(hints > 100)
    }

    /**
     * A chain is only worth showing if it ends somewhere: the last step writes a
     * digit, everything before it strikes candidates. A chain of pure eliminations
     * would leave the player with the same board and the same hint on the next press.
     */
    @Test
    fun aHintChainEndsWithSomethingToEnter() {
        val factory = PuzzleFactory()
        val rnd = Random(37)
        var chained = 0

        repeat(perLevel * 2) {
            val puzzle = factory.generate(Level.EXPERT, rnd)
            val hint = finder.find(puzzle.givens, puzzle.solution)
            if (hint !is Hint.Deduce) return@repeat
            assertTrue("the chain ends in a placement", hint.last.technique.places)
            for (step in hint.steps.dropLast(1)) {
                assertTrue("only the last step places", !step.technique.places)
                assertTrue("an intermediate step must strike something", step.eliminations.isNotEmpty())
            }
            if (hint.steps.size > 1) chained++
        }
        println("EXPERT: $chained of ${perLevel * 2} puzzles already need more than one step on the first hint")
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
            val a = finder.find(board, puzzle.solution)
            val b = finder.find(board, puzzle.solution)
            assertEquals(a::class, b::class)
            if (a is Hint.Deduce && b is Hint.Deduce) {
                assertEquals(a.steps.size, b.steps.size)
                assertEquals(a.last.technique, b.last.technique)
                assertEquals(a.last.cell, b.last.cell)
                assertEquals(a.last.digit, b.last.digit)
            }
        }
    }
}
