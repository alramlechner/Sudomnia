package name.lechners.sudomnia.data

import name.lechners.sudomnia.rules.Level

/** What has been achieved on one difficulty. */
data class LevelStats(
    val started: Int = 0,
    val solved: Int = 0,
    /** Milliseconds of the fastest solve; 0 means "none yet". */
    val bestMs: Long = 0,
    /** Solved with every visual aid switched off for the whole game. */
    val solvedNoAids: Int = 0,
    /** Solved without asking for a single hint. */
    val solvedNoHints: Int = 0,
)

/**
 * The solve counters.
 *
 * The counting rules live here rather than in the view model on purpose: the project
 * has no Robolectric, so anything inside a view model is untestable. As plain data
 * with pure update functions, every rule below is covered by an ordinary JVM test.
 */
data class Stats(val byLevel: Map<Level, LevelStats> = emptyMap()) {

    operator fun get(level: Level): LevelStats = byLevel[level] ?: LevelStats()

    val totalSolved: Int get() = Level.entries.sumOf { get(it).solved }

    /**
     * Counted on the first move of a game, not when the puzzle is generated -- merely
     * flicking through the difficulties would otherwise inflate it.
     */
    fun withStarted(level: Level): Stats =
        replace(level) { it.copy(started = it.started + 1) }

    /**
     * @param noAids every aid was off for the *whole* game, not just at the end
     * @param noHints no hint was requested
     */
    fun withSolved(level: Level, ms: Long, noAids: Boolean, noHints: Boolean): Stats =
        replace(level) {
            it.copy(
                solved = it.solved + 1,
                bestMs = if (it.bestMs == 0L || ms < it.bestMs) ms else it.bestMs,
                solvedNoAids = it.solvedNoAids + if (noAids) 1 else 0,
                solvedNoHints = it.solvedNoHints + if (noHints) 1 else 0,
            )
        }

    private fun replace(level: Level, edit: (LevelStats) -> LevelStats): Stats =
        Stats(byLevel + (level to edit(get(level))))

    companion object {
        val EMPTY = Stats()
    }
}
