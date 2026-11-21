package name.lechners.sudomnia.rules

import kotlin.random.Random

/**
 * The one entry point the app uses: give me a puzzle of roughly this difficulty.
 *
 * Because [Digger] aims by construction rather than by rejection, the first attempt
 * almost always lands in the requested band. The retry loop exists only for the
 * rare case where a maximally dug grid happens to stay singles-solvable; it is
 * bounded, and if it ever ran out the puzzle is returned anyway -- labelled with
 * the band it actually falls into rather than the one that was asked for.
 *
 * Not thread-safe. Call from a background dispatcher; on a tablet one puzzle takes
 * roughly 50-150 ms.
 */
class PuzzleFactory {

    private val generators = HashMap<Units, GridGenerator>()
    private val diggers = HashMap<Units, Digger>()

    fun generate(level: Level, rnd: Random = Random.Default): Puzzle {
        val units = level.units
        val generator = generators.getOrPut(units) { GridGenerator(units) }
        val digger = diggers.getOrPut(units) { Digger(units) }
        if (level.isKids) {
            // One band only: there is nothing to retry for.
            val solution = generator.fullGrid(rnd)
            return Puzzle(digger.dig(solution, level, rnd), solution, level)
        }
        var last: Puzzle? = null
        repeat(MAX_ATTEMPTS) {
            val solution = generator.fullGrid(rnd)
            val givens = digger.dig(solution, level, rnd)
            val actual = digger.classify(givens)
            val puzzle = Puzzle(givens, solution, actual)
            if (actual == level) return puzzle
            last = puzzle
        }
        return last!!
    }

    private companion object {
        const val MAX_ATTEMPTS = 25
    }
}
