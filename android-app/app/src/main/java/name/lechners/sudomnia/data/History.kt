package name.lechners.sudomnia.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ln
import name.lechners.sudomnia.rules.Level

/** How a game that was started ended. */
enum class Outcome(val code: Char) {
    SOLVED('s'), LOST('l'), ABANDONED('a');

    companion object {
        fun of(code: Char): Outcome? = entries.firstOrNull { it.code == code }
    }
}

/**
 * One finished game.
 *
 * [score] is the Grader's sum of technique costs for the puzzle -- the objective
 * amount of reasoning it takes -- which is what lets a rating compare an easy and an
 * expert game without hand-picked per-level factors.
 */
data class GameRecord(
    val at: Long,
    val level: Level,
    val score: Int,
    val durationMs: Long,
    val mistakes: Int,
    val hints: Int,
    val aidsClean: Boolean,
    val outcome: Outcome,
) {
    fun encode(): String = listOf(
        at, level.name, score, durationMs, mistakes, hints,
        if (aidsClean) 1 else 0, outcome.code,
    ).joinToString(";")

    /** A game done without a single aid, hint or mistake. */
    val clean: Boolean get() = outcome == Outcome.SOLVED && aidsClean && hints == 0 && mistakes == 0

    /**
     * Rating this one game is worth. Solved: reasoning per minute, on a log scale so
     * that doubling the pace is always the same number of points, minus a penalty for
     * every crutch. A lost game is worth a fixed low value, and an abandoned one only
     * counts once it had lasted long enough to be a real attempt -- otherwise
     * abandoning a bad start would be free.
     */
    val rated: Double? get() = when (outcome) {
        Outcome.SOLVED -> {
            val minutes = (durationMs / 60_000.0).coerceAtLeast(0.5)
            var perf = score / minutes
            perf *= Math.pow(HINT_FACTOR, hints.toDouble())
            perf *= Math.pow(MISTAKE_FACTOR, mistakes.toDouble())
            if (!aidsClean) perf *= AIDS_FACTOR
            (BASE + PER_DOUBLING * ln(perf.coerceAtLeast(0.01) / REF_PACE) / ln(2.0))
                .coerceIn(0.0, MAX)
        }
        Outcome.LOST -> LOST_VALUE
        Outcome.ABANDONED -> if (durationMs >= ABANDON_MIN_MS) ABANDONED_VALUE else null
    }

    companion object {
        fun decode(line: String): GameRecord? {
            val p = line.split(';')
            if (p.size != 8) return null
            return try {
                GameRecord(
                    at = p[0].toLong(),
                    level = Level.valueOf(p[1]),
                    score = p[2].toInt(),
                    durationMs = p[3].toLong(),
                    mistakes = p[4].toInt(),
                    hints = p[5].toInt(),
                    aidsClean = p[6] == "1",
                    outcome = Outcome.of(p[7].firstOrNull() ?: return null) ?: return null,
                )
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        // 8 score points per minute is a comfortable pace at every level (an easy
        // puzzle is ~55 points, an expert one several hundred) and maps to 1000.
        const val REF_PACE = 8.0
        const val BASE = 1000.0
        const val PER_DOUBLING = 400.0
        const val MAX = 3000.0
        const val HINT_FACTOR = 0.8
        const val MISTAKE_FACTOR = 0.9
        const val AIDS_FACTOR = 0.9
        const val LOST_VALUE = 200.0
        const val ABANDONED_VALUE = 400.0
        const val ABANDON_MIN_MS = 120_000L
    }
}

enum class Title { BEGINNER, ADVANCED, SKILLED, MASTER, GRANDMASTER }

/**
 * The play history and everything derived from it -- as plain data with pure
 * functions, like [Stats], so a JVM test covers every rule.
 */
data class History(val games: List<GameRecord> = emptyList()) {

    fun plus(record: GameRecord): History = History((games + record).takeLast(MAX_GAMES))

    fun forLevel(level: Level?): List<GameRecord> =
        if (level == null) games else games.filter { it.level == level }

    /** The rating after each rated game, oldest first (an exponential moving average). */
    fun ratingCurve(level: Level? = null): List<Double> {
        var current = Double.NaN
        val out = ArrayList<Double>()
        for (v in forLevel(level).mapNotNull { it.rated }) {
            current = if (current.isNaN()) v else current + ALPHA * (v - current)
            out += current
        }
        return out
    }

    fun rating(level: Level? = null): Int? = ratingCurve(level).lastOrNull()?.toInt()

    /** Below this many rated games the number is a first impression, not a rating. */
    fun provisional(level: Level? = null): Boolean = ratingCurve(level).size < PROVISIONAL_BELOW

    /**
     * Points gained between the ten rated games before the latest ten and the latest
     * ten. Null until there is something to compare.
     */
    fun trend(level: Level? = null): Int? {
        val v = forLevel(level).mapNotNull { it.rated }
        if (v.size < 2 * TREND_WINDOW) return null
        val last = v.takeLast(TREND_WINDOW).average()
        val before = v.dropLast(TREND_WINDOW).takeLast(TREND_WINDOW).average()
        return (last - before).toInt()
    }

    fun solved(level: Level? = null): List<GameRecord> =
        forLevel(level).filter { it.outcome == Outcome.SOLVED }

    fun medianSolveMs(level: Level? = null): Long? {
        val t = solved(level).map { it.durationMs }.sorted()
        if (t.isEmpty()) return null
        return if (t.size % 2 == 1) t[t.size / 2] else (t[t.size / 2 - 1] + t[t.size / 2]) / 2
    }

    /** Games in a row solved, counted back from the latest one. */
    fun currentStreak(level: Level? = null): Int =
        forLevel(level).asReversed().takeWhile { it.outcome == Outcome.SOLVED }.size

    fun longestStreak(level: Level? = null): Int {
        var best = 0
        var run = 0
        for (g in forLevel(level)) {
            run = if (g.outcome == Outcome.SOLVED) run + 1 else 0
            if (run > best) best = run
        }
        return best
    }

    fun count(outcome: Outcome, level: Level? = null): Int =
        forLevel(level).count { it.outcome == outcome }

    fun cleanSolves(level: Level? = null): Int = forLevel(level).count { it.clean }

    fun totalPlayMs(level: Level? = null): Long = forLevel(level).sumOf { it.durationMs }

    /** Days played -> games that day. */
    fun perDay(zone: ZoneId = ZoneId.systemDefault()): Map<LocalDate, Int> =
        games.groupingBy { day(it.at, zone) }.eachCount()

    /** Consecutive days with a game, ending today -- or yesterday, so the day is not lost before it is played. */
    fun dayStreak(today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int {
        val days = perDay(zone).keys
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        while (d in days) { n++; d = d.minusDays(1) }
        return n
    }

    /** Share of earlier solves in the level that were slower than [ms]; null if there is nothing to compare. */
    fun fasterThanShare(level: Level, ms: Long): Int? {
        val others = solved(level).dropLast(1)
        if (others.isEmpty()) return null
        return (100 * others.count { it.durationMs > ms } / others.size)
    }

    companion object {
        const val MAX_GAMES = 2000
        const val ALPHA = 0.15
        const val PROVISIONAL_BELOW = 10
        const val TREND_WINDOW = 10

        fun day(at: Long, zone: ZoneId): LocalDate =
            Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

        fun titleOf(rating: Int): Title = when {
            rating < 700 -> Title.BEGINNER
            rating < 1000 -> Title.ADVANCED
            rating < 1300 -> Title.SKILLED
            rating < 1700 -> Title.MASTER
            else -> Title.GRANDMASTER
        }

        fun encode(h: History): String = h.games.joinToString("\n") { it.encode() }

        fun decode(s: String?): History =
            History(s.orEmpty().lineSequence().mapNotNull { GameRecord.decode(it) }.toList())
    }
}
