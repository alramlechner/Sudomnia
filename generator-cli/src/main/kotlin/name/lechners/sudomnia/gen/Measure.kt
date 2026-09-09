package name.lechners.sudomnia.gen

import name.lechners.sudomnia.rules.HumanSolver
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.rules.PuzzleFactory
import name.lechners.sudomnia.rules.Technique
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Measuring the technique ladder, not producing puzzles.
 *
 * The difficulty bands are supposed to be *measured* rather than asserted, and that
 * cannot be done on a phone: it takes thousands of puzzles to see the tail of the
 * distribution. This is where the numbers in ARCHITECTURE.md come from -- rerun it
 * after touching Digger, Technique or HumanSolver, and put the new numbers there.
 */
internal fun grade(count: Int) {
    val solver = HumanSolver()
    for (level in Level.entries) {
        val factory = PuzzleFactory()
        val rnd = Random(1234)
        val hardest = HashMap<Technique?, Int>()
        val usage = HashMap<Technique, Int>()
        var clues = 0
        var score = 0
        var steps = 0
        var unsolved = 0

        val ms = measureTimeMillis {
            repeat(count) {
                val puzzle = factory.generate(level, rnd)
                clues += puzzle.clueCount
                val r = solver.solve(puzzle.givens) { usage[it.technique] = (usage[it.technique] ?: 0) + 1 }
                if (!r.solved) unsolved++
                hardest[r.hardest] = (hardest[r.hardest] ?: 0) + 1
                score += r.score
                steps += r.steps
            }
        }

        println("--- $level ($count Raetsel, ${ms / count.toDouble()} ms je Raetsel)")
        println("    Vorgaben:      ${"%.1f".format(clues.toDouble() / count)}")
        println("    Schritte:      ${"%.1f".format(steps.toDouble() / count)}")
        println("    Punkte:        ${"%.1f".format(score.toDouble() / count)}")
        println("    ohne Raten unloesbar: $unsolved")
        println("    hoechste noetige Technik:")
        for (t in Technique.entries) hardest[t]?.let { println("        ${t.name.padEnd(18)} $it") }
        hardest[null]?.let { println("        (gar keine)        $it") }
        println("    Anwendungen gesamt:")
        for (t in Technique.entries) usage[t]?.let { println("        ${t.name.padEnd(18)} $it") }
    }
}

/**
 * Hunts for a puzzle whose solution path actually uses [wanted] and prints it in the
 * 81-character format, ready to be checked in as a test fixture.
 *
 * The rare rungs -- Swordfish above all -- turn up in well under one puzzle in a
 * thousand, because something cheaper almost always bites first. A unit test cannot
 * generate its way to one in reasonable time, so it gets a fixture found here.
 */
internal fun find(wanted: Technique, tries: Int) {
    val factory = PuzzleFactory()
    val solver = HumanSolver()
    val rnd = Random(System.nanoTime())

    repeat(tries) { i ->
        val puzzle = factory.generate(Level.EXPERT, rnd)
        var hit = false
        solver.solve(puzzle.givens) { if (it.technique == wanted) hit = true }
        if (hit) {
            println("gefunden nach ${i + 1} Raetseln:")
            println("givens   = \"${puzzle.toLine().replace('.', '0')}\"")
            println("solution = \"${puzzle.solution.joinToString("") { d -> d.toString() }}\"")
            return
        }
    }
    println("$wanted in $tries Raetseln nicht aufgetreten")
}
