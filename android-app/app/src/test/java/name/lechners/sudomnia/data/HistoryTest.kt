package name.lechners.sudomnia.data

import java.time.LocalDate
import java.time.ZoneOffset
import name.lechners.sudomnia.rules.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTest {

    private fun game(
        outcome: Outcome = Outcome.SOLVED,
        level: Level = Level.EASY,
        score: Int = 80,
        minutes: Double = 10.0,
        hints: Int = 0,
        mistakes: Int = 0,
        clean: Boolean = true,
        at: Long = 0L,
    ) = GameRecord(at, level, score, (minutes * 60_000).toLong(), mistakes, hints, clean, outcome)

    @Test
    fun recordSurvivesEncodeAndDecode() {
        val r = game(hints = 2, mistakes = 1, clean = false, at = 1234L)
        assertEquals(r, GameRecord.decode(r.encode()))
    }

    @Test
    fun garbageLinesAreSkippedNotFatal() {
        val h = History.decode("nonsense\n" + game().encode() + "\n1;NOPE;1;1;1;1;1;s")
        assertEquals(1, h.games.size)
    }

    @Test
    fun referencePaceIsWorthTheBaseRating() {
        assertEquals(1000.0, game(score = 80, minutes = 10.0).rated!!, 0.001)
    }

    @Test
    fun doublingThePaceAddsAFixedStep() {
        val slow = game(score = 80, minutes = 10.0).rated!!
        val fast = game(score = 80, minutes = 5.0).rated!!
        assertEquals(400.0, fast - slow, 0.001)
    }

    @Test
    fun crutchesCostPoints() {
        val base = game().rated!!
        assertTrue(game(hints = 1).rated!! < base)
        assertTrue(game(mistakes = 1).rated!! < base)
        assertTrue(game(clean = false).rated!! < base)
    }

    @Test
    fun abandoningEarlyIsNotRatedButLaterIs() {
        assertNull(game(Outcome.ABANDONED, minutes = 1.0).rated)
        assertNotNull(game(Outcome.ABANDONED, minutes = 3.0).rated)
    }

    @Test
    fun lostGamesPullTheRatingDown() {
        val h = History(listOf(game(), game(), game(Outcome.LOST)))
        assertTrue(h.ratingCurve().last() < h.ratingCurve().first())
    }

    @Test
    fun ratingIsProvisionalUntilTenGames() {
        assertTrue(History(List(9) { game() }).provisional())
        assertTrue(!History(List(10) { game() }).provisional())
    }

    @Test
    fun trendNeedsTwentyGamesAndShowsImprovement() {
        assertNull(History(List(19) { game() }).trend())
        val h = History(List(10) { game(minutes = 20.0) } + List(10) { game(minutes = 10.0) })
        assertEquals(400, h.trend())
    }

    @Test
    fun streaksBreakOnAnythingButAWin() {
        val h = History(listOf(game(), game(), game(Outcome.LOST), game(), game(), game()))
        assertEquals(3, h.currentStreak())
        assertEquals(3, h.longestStreak())
    }

    @Test
    fun medianOfSolveTimes() {
        val h = History(listOf(game(minutes = 4.0), game(minutes = 20.0), game(minutes = 6.0)))
        assertEquals(6 * 60_000L, h.medianSolveMs())
        assertNull(History().medianSolveMs())
    }

    @Test
    fun levelFilterOnlyLooksAtThatLevel() {
        val h = History(listOf(game(level = Level.EASY), game(level = Level.HARD)))
        assertEquals(1, h.solved(Level.HARD).size)
        assertEquals(2, h.solved().size)
    }

    @Test
    fun dayStreakSurvivesUntilTheDayIsOver() {
        val z = ZoneOffset.UTC
        fun at(d: LocalDate) = d.atStartOfDay(z).toInstant().toEpochMilli() + 3_600_000
        val today = LocalDate.of(2026, 9, 24)
        val h = History(listOf(game(at = at(today.minusDays(2))), game(at = at(today.minusDays(1)))))
        assertEquals(2, h.dayStreak(today, z))                       // nothing yet today: still alive
        assertEquals(3, h.plus(game(at = at(today))).dayStreak(today, z))
        assertEquals(0, h.dayStreak(today.plusDays(3), z))
    }

    @Test
    fun fasterThanShareIgnoresTheGameItself() {
        val h = History(listOf(game(minutes = 20.0), game(minutes = 10.0), game(minutes = 5.0)))
        assertEquals(100, h.fasterThanShare(Level.EASY, 5 * 60_000L))
        assertNull(History(listOf(game())).fasterThanShare(Level.EASY, 1L))
    }

    @Test
    fun historyIsCapped() {
        val h = History(List(History.MAX_GAMES) { game() }).plus(game(at = 99))
        assertEquals(History.MAX_GAMES, h.games.size)
        assertEquals(99L, h.games.last().at)
    }

    @Test
    fun titlesFollowTheRating() {
        assertEquals(Title.BEGINNER, History.titleOf(100))
        assertEquals(Title.SKILLED, History.titleOf(1000))
        assertEquals(Title.GRANDMASTER, History.titleOf(2500))
    }
}
