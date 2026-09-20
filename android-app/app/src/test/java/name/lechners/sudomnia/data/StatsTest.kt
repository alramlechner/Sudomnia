package name.lechners.sudomnia.data

import name.lechners.sudomnia.rules.Level
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {

    @Test
    fun anUntouchedLevelReadsAsAllZeroes() {
        val s = Stats.EMPTY
        assertEquals(LevelStats(), s[Level.HARD])
        assertEquals(0, s.totalSolved)
    }

    @Test
    fun countersAreKeptPerLevel() {
        val s = Stats.EMPTY
            .withSolved(Level.HARD, 1000, noAids = false, noHints = false)
            .withSolved(Level.EASY, 500, noAids = false, noHints = false)
            .withSolved(Level.HARD, 900, noAids = false, noHints = false)

        assertEquals(2, s[Level.HARD].solved)
        assertEquals(1, s[Level.EASY].solved)
        assertEquals(0, s[Level.MEDIUM].solved)
        assertEquals(3, s.totalSolved)
    }

    /** A best time may only ever improve -- a slow game must not overwrite a fast one. */
    @Test
    fun theBestTimeOnlyEverGoesDown() {
        var s = Stats.EMPTY.withSolved(Level.MEDIUM, 5_000, false, false)
        assertEquals(5_000, s[Level.MEDIUM].bestMs)

        s = s.withSolved(Level.MEDIUM, 9_000, false, false)
        assertEquals("a slower game must not replace the best time", 5_000, s[Level.MEDIUM].bestMs)

        s = s.withSolved(Level.MEDIUM, 3_000, false, false)
        assertEquals(3_000, s[Level.MEDIUM].bestMs)
    }

    @Test
    fun theFirstSolveSetsTheBestTimeEvenIfSlow() {
        val s = Stats.EMPTY.withSolved(Level.EASY, 999_999, false, false)
        assertEquals(999_999, s[Level.EASY].bestMs)
    }

    @Test
    fun badgesAreOnlyCountedWhenEarned() {
        val s = Stats.EMPTY
            .withSolved(Level.HARD, 100, noAids = true, noHints = true)
            .withSolved(Level.HARD, 100, noAids = true, noHints = false)
            .withSolved(Level.HARD, 100, noAids = false, noHints = true)
            .withSolved(Level.HARD, 100, noAids = false, noHints = false)

        assertEquals(4, s[Level.HARD].solved)
        assertEquals(2, s[Level.HARD].solvedNoAids)
        assertEquals(2, s[Level.HARD].solvedNoHints)
    }

    @Test
    fun startedAndSolvedAreCountedIndependently() {
        val s = Stats.EMPTY
            .withStarted(Level.EASY)
            .withStarted(Level.EASY)
            .withSolved(Level.EASY, 100, false, false)

        assertEquals(2, s[Level.EASY].started)
        assertEquals(1, s[Level.EASY].solved)
    }
}
