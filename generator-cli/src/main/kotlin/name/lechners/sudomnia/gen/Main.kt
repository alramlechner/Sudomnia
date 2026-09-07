package name.lechners.sudomnia.gen

import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.rules.PuzzleFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Batch-generates a fixed number of puzzles per [Level] with the existing
 * PuzzleFactory/Digger (unchanged rules/ code, copied from android-app) and exits
 * once the target is reached. One line per puzzle: "givens;solution;level", where
 * givens/solution use the 81-character format already established by
 * rules/Puzzle.kt (toLine()/parse()).
 *
 * Not resumable by design: reruns overwrite the per-level files. At the default
 * 10,000-puzzle scale a run finishes in minutes, so resuming was not worth the
 * extra bookkeeping.
 */

private class LevelSink(file: File) {
    private val writer: BufferedWriter = BufferedWriter(FileWriter(file, false))
    private val written = AtomicInteger(0)

    @Synchronized
    fun writeLine(line: String) {
        writer.write(line)
        writer.newLine()
        written.incrementAndGet()
    }

    val count: Int get() = written.get()

    @Synchronized
    fun close() = writer.close()
}

private fun intArg(args: Array<String>, name: String, default: Int): Int {
    val idx = args.indexOf(name)
    return if (idx != -1 && idx + 1 < args.size) args[idx + 1].toIntOrNull() ?: default else default
}

private fun stringArg(args: Array<String>, name: String, default: String): String {
    val idx = args.indexOf(name)
    return if (idx != -1 && idx + 1 < args.size) args[idx + 1] else default
}

fun main(args: Array<String>) {
    val totalCount = intArg(args, "--count", 10_000)
    val threadCount = intArg(args, "--threads", (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1))
    val outDir = File(stringArg(args, "--out", "generated-puzzles")).apply { mkdirs() }

    val levels = Level.entries.toList()
    val base = totalCount / levels.size
    val remainder = totalCount % levels.size
    val targets = levels.mapIndexed { i, level -> level to (base + if (i < remainder) 1 else 0) }.toMap()
    val remaining = levels.associateWith { AtomicInteger(targets.getValue(it)) }
    val sinks = levels.associateWith { LevelSink(File(outDir, "${it.name.lowercase()}.txt")) }

    val manifest = File(outDir, "manifest.txt")
    val startedAt = Instant.now()
    manifest.writeText(buildString {
        appendLine("Sudomnia generator-cli -- Rätsel-Vorrat")
        appendLine("Format je Zeile: givens;solution;level")
        appendLine("  givens/solution: 81 Zeichen, '.' = leer (kompatibel mit rules/Puzzle.kt toLine()/parse())")
        appendLine("  level: die GEMESSENE Stufe (Digger.classify), kann selten vom angepeilten Level abweichen")
        appendLine("Gestartet: $startedAt")
        appendLine("Ziel gesamt: $totalCount  (${targets.entries.joinToString { "${it.key}=${it.value}" }})")
        appendLine("Threads: $threadCount")
    })

    Runtime.getRuntime().addShutdownHook(Thread {
        sinks.values.forEach { runCatching { it.close() } }
    })

    println("Starte Generierung: $totalCount Rätsel, $threadCount Threads, Ziel je Level: $targets")

    val workers = (1..threadCount).map { threadIndex ->
        Thread {
            val factory = PuzzleFactory()
            val rnd = Random(System.nanoTime() xor (threadIndex.toLong() shl 32))
            while (true) {
                val level = levels.firstOrNull { lvl ->
                    remaining.getValue(lvl).getAndUpdate { n -> if (n > 0) n - 1 else n } > 0
                } ?: break
                val puzzle = factory.generate(level, rnd)
                val solutionLine = puzzle.solution.joinToString("") { it.toString() }
                try {
                    sinks.getValue(level).writeLine("${puzzle.toLine()};$solutionLine;${puzzle.level}")
                } catch (e: java.io.IOException) {
                    // Sink closed underneath us -- only happens when run/stop.sh
                    // (SIGTERM) races with an in-flight write. Nothing to recover:
                    // the shutdown hook is already flushing, so just stop this worker.
                    break
                }
            }
        }.apply { start() }
    }

    val progress = Thread {
        while (workers.any { it.isAlive }) {
            Thread.sleep(5000)
            val done = sinks.values.sumOf { it.count }
            println("Fortschritt: $done / $totalCount")
        }
    }.apply { isDaemon = true; start() }

    workers.forEach { it.join() }
    sinks.values.forEach { it.close() }

    val finishedAt = Instant.now()
    val totalWritten = sinks.values.sumOf { it.count }
    manifest.appendText(buildString {
        appendLine("Beendet: $finishedAt")
        appendLine("Dauer: ${Duration.between(startedAt, finishedAt)}")
        appendLine("Erzeugt gesamt: $totalWritten  (${levels.joinToString { "${it}=${sinks.getValue(it).count}" }})")
    })
    println("Fertig: $totalWritten Rätsel in ${outDir.absolutePath}")
}
