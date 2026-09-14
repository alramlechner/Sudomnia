package name.lechners.sudomnia.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MistakeTallyTest {

    @Test
    fun aFreshTallyHasEveryAttemptLeft() {
        val t = MistakeTally()
        assertEquals(0, t.count)
        assertEquals(MistakeTally.LIMIT, t.remaining)
        assertFalse(t.lost)
    }

    @Test
    fun onlyAWrongDigitCosts() {
        val t = MistakeTally()
        assertEquals(0, t.after(placed = 5, correct = 5).count)
        assertEquals(1, t.after(placed = 4, correct = 5).count)
    }

    /**
     * Tapping the digit that is already there clears the cell -- `SudokuGame.setDigit`
     * toggles. An empty cell is not an answer, so it cannot be a wrong one; without
     * this, erasing would burn an attempt.
     */
    @Test
    fun clearingACellIsNotAMistake() {
        assertEquals(0, MistakeTally().after(placed = 0, correct = 5).count)
    }

    @Test
    fun theThirdMistakeEndsIt() {
        var t = MistakeTally()
        repeat(MistakeTally.LIMIT - 1) {
            t = t.after(placed = 1, correct = 2)
            assertFalse(t.lost)
        }
        t = t.after(placed = 1, correct = 2)
        assertTrue(t.lost)
        assertEquals(0, t.remaining)
    }

    /** Past the limit nothing goes negative -- the UI counts attempts down from this. */
    @Test
    fun remainingStopsAtZero() {
        var t = MistakeTally()
        repeat(MistakeTally.LIMIT + 3) { t = t.after(placed = 1, correct = 2) }
        assertEquals(0, t.remaining)
        assertTrue(t.lost)
    }
}
