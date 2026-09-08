package name.lechners.sudomnia.data

import name.lechners.sudomnia.rules.Grid
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.rules.Puzzle
import name.lechners.sudomnia.rules.Units

/**
 * A game in progress, as plain data.
 *
 * Stored as **givens plus the edit list**, never as a copy of the board -- replaying
 * the edits brings back the digits, the pencil marks and the undo stack together, and
 * cannot drift out of step with each other the way three separate snapshots would.
 *
 * Encoding is hand-rolled rather than kotlinx.serialization: it keeps the dependency
 * list at zero for one string, and more importantly it stays a pure function that a
 * JVM test can round-trip without a single Android type.
 */
data class GameSnapshot(
    val givens: String,
    val solution: String,
    val level: Level,
    /** One row per edit; each row is `cell:before>after:notesBefore>notesAfter` groups. */
    val edits: List<String>,
    val applied: Int,
    val elapsedMs: Long,
    val hintsUsed: Int,
    val aidsUsed: Boolean,
    val counted: Boolean,
    /** Start of the open trial branch in the edit list, -1 if none. */
    val branchAt: Int = -1,
) {

    fun encode(): String = listOf(
        VERSION.toString(),
        givens,
        solution,
        level.name,
        applied.toString(),
        elapsedMs.toString(),
        hintsUsed.toString(),
        if (aidsUsed) "1" else "0",
        if (counted) "1" else "0",
        branchAt.toString(),
        edits.joinToString(EDIT_SEP),
    ).joinToString(FIELD_SEP)

    /** Rebuilds a playable game, or null if anything about the data is off. */
    fun toGame(): SudokuGameRestore? {
        val g = Puzzle.parseOrNull(givens) ?: return null
        val s = Puzzle.parseOrNull(solution) ?: return null
        if (!Grid.isValidSolution(s)) return null
        // The givens must actually be part of that solution, or the two halves of the
        // snapshot came from different games.
        for (c in 0 until Units.CELLS) if (g[c] != 0 && g[c] != s[c]) return null
        val rows = edits.map { row ->
            row.split(CHANGE_SEP).map { change ->
                val parts = change.split(":", ">")
                if (parts.size != 5) return null
                IntArray(5) { parts[it].toIntOrNull() ?: return null }
            }
        }
        if (branchAt > applied) return null
        return SudokuGameRestore(Puzzle(g, s, level), rows, applied, branchAt)
    }

    companion object {
        private const val VERSION = 2
        private const val FIELD_SEP = "|"
        private const val EDIT_SEP = ";"
        private const val CHANGE_SEP = ","

        /** Encodes one edit as returned by `SudokuGame.exportHistory()`. */
        fun encodeEdit(changes: List<IntArray>): String =
            changes.joinToString(CHANGE_SEP) { "${it[0]}:${it[1]}>${it[2]}:${it[3]}>${it[4]}" }

        /** @return null for anything that is not a snapshot this version understands. */
        fun decode(text: String?): GameSnapshot? {
            if (text.isNullOrEmpty()) return null
            val f = text.split(FIELD_SEP)
            val version = f[0].toIntOrNull() ?: return null
            // Version 1 knew no trial branch and had one field less. It is still read
            // rather than dropped: whoever updates mid-puzzle keeps their game.
            if (version !in 1..VERSION) return null
            if (f.size != if (version == 1) 10 else 11) return null
            val level = Level.entries.firstOrNull { it.name == f[3] } ?: return null
            return GameSnapshot(
                givens = f[1],
                solution = f[2],
                level = level,
                edits = f.last().let { if (it.isEmpty()) emptyList() else it.split(EDIT_SEP) },
                applied = f[4].toIntOrNull() ?: return null,
                elapsedMs = f[5].toLongOrNull() ?: return null,
                hintsUsed = f[6].toIntOrNull() ?: return null,
                aidsUsed = f[7] == "1",
                counted = f[8] == "1",
                branchAt = if (version == 1) -1 else f[9].toIntOrNull() ?: return null,
            )
        }
    }
}

/** What [GameSnapshot.toGame] hands back: enough to rebuild a `SudokuGame`. */
class SudokuGameRestore(
    val puzzle: Puzzle,
    val history: List<List<IntArray>>,
    val applied: Int,
    val branchAt: Int,
)
