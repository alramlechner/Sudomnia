package name.lechners.sudomnia.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import name.lechners.sudomnia.rules.Level

/**
 * Settings storage.
 *
 * SharedPreferences rather than DataStore, for one concrete reason: it can be read
 * synchronously in the ViewModel's constructor, so the very first frame is already
 * drawn with the player's settings. A DataStore flow would render one frame with
 * the defaults and then correct itself, which is visible as a flicker on exactly
 * the setting someone just turned off.
 *
 * The version field exists so that changing a default later can be told apart from
 * a value the player chose. Without it, "conflicts default to off from now on"
 * would silently flip the setting for everyone who had deliberately left it on.
 */
class SudomniaPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    init {
        migrate()
    }

    fun load(): Settings {
        val d = Settings()
        return Settings(
            showConflicts = prefs.getBoolean(KEY_CONFLICTS, d.showConflicts),
            highlightSameDigit = prefs.getBoolean(KEY_SAME_DIGIT, d.highlightSameDigit),
            highlightPeers = prefs.getBoolean(KEY_PEERS, d.highlightPeers),
            dimCompletedDigits = prefs.getBoolean(KEY_DIM_DIGITS, d.dimCompletedDigits),
        )
    }

    fun save(settings: Settings) {
        prefs.edit {
            putBoolean(KEY_CONFLICTS, settings.showConflicts)
            putBoolean(KEY_SAME_DIGIT, settings.highlightSameDigit)
            putBoolean(KEY_PEERS, settings.highlightPeers)
            putBoolean(KEY_DIM_DIGITS, settings.dimCompletedDigits)
            putInt(KEY_VERSION, CURRENT_VERSION)
        }
    }

    // --- Statistik ---------------------------------------------------------

    fun loadStats(): Stats {
        val map = Level.entries.associateWith { level ->
            LevelStats(
                started = prefs.getInt(statKey(level, "started"), 0),
                solved = prefs.getInt(statKey(level, "solved"), 0),
                bestMs = prefs.getLong(statKey(level, "best"), 0L),
                solvedNoAids = prefs.getInt(statKey(level, "noaids"), 0),
                solvedNoHints = prefs.getInt(statKey(level, "nohints"), 0),
            )
        }
        return Stats(map)
    }

    fun saveStats(stats: Stats) {
        prefs.edit {
            for (level in Level.entries) {
                val s = stats[level]
                putInt(statKey(level, "started"), s.started)
                putInt(statKey(level, "solved"), s.solved)
                putLong(statKey(level, "best"), s.bestMs)
                putInt(statKey(level, "noaids"), s.solvedNoAids)
                putInt(statKey(level, "nohints"), s.solvedNoHints)
            }
        }
    }

    private fun statKey(level: Level, field: String) = "stat_${level.name}_$field"

    // --- Laufendes Spiel ---------------------------------------------------

    fun loadGame(): GameSnapshot? = GameSnapshot.decode(prefs.getString(KEY_GAME, null))

    fun saveGame(snapshot: GameSnapshot) {
        prefs.edit { putString(KEY_GAME, snapshot.encode()) }
    }

    fun clearGame() {
        prefs.edit { remove(KEY_GAME) }
    }

    private fun migrate() {
        val from = prefs.getInt(KEY_VERSION, 0)
        if (from == CURRENT_VERSION) return
        // Version 1 is the first release; nothing to move yet. Later steps go here,
        // one `if (from < n)` block each, in order.
        prefs.edit { putInt(KEY_VERSION, CURRENT_VERSION) }
    }

    private companion object {
        const val FILE = "sudomnia"
        // 1: nur Einstellungen. 2: Statistik und laufendes Spiel kamen dazu -- beides
        // war vorher nicht vorhanden, es ist also nichts zu wandeln.
        const val CURRENT_VERSION = 2

        const val KEY_VERSION = "settings_version"
        const val KEY_CONFLICTS = "show_conflicts"
        const val KEY_SAME_DIGIT = "highlight_same_digit"
        const val KEY_PEERS = "highlight_peers"
        const val KEY_DIM_DIGITS = "dim_completed_digits"
        const val KEY_GAME = "current_game"
    }
}
