package name.lechners.sudomnia.ui.game

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import name.lechners.sudomnia.data.GameSnapshot
import name.lechners.sudomnia.data.Settings
import name.lechners.sudomnia.data.Stats
import name.lechners.sudomnia.data.SudomniaPrefs
import name.lechners.sudomnia.game.SudokuGame
import name.lechners.sudomnia.rules.Hint
import name.lechners.sudomnia.rules.HintFinder
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.rules.PuzzleFactory
import name.lechners.sudomnia.rules.Units
import name.lechners.sudomnia.ui.board.BoardState

class SudokuViewModel(private val prefs: SudomniaPrefs) : ViewModel() {

    private val factory = PuzzleFactory()
    private val hintFinder = HintFinder()
    private var game: SudokuGame? = null

    // Read synchronously, before the first frame -- see SudomniaPrefs.
    private var settings: Settings = prefs.load()
    private var stats: Stats = prefs.loadStats()

    private val _ui = MutableStateFlow(GameUiState(settings = settings, stats = stats))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _timer = MutableStateFlow(TimerState())
    val timer: StateFlow<TimerState> = _timer.asStateFlow()

    private var selected = -1
    private var accumulatedMs = 0L
    private var startedAt = 0L

    /**
     * Identifies the currently valid ticker. Without it, starting a new game while
     * the previous ticker is inside its `delay` leaves that coroutine alive: it
     * wakes up, sees `running == true` again, and keeps ticking alongside the new
     * one. The display would still be right -- elapsed time is derived, not counted
     * -- but the tickers would accumulate, one per game played.
     */
    private var timerToken = 0

    /** Per game: has this win already been counted? Survives undo/redo across the last move. */
    private var countedSolved = false
    /** Per game: has the first move been counted as "started"? */
    private var countedStart = false
    private var hintsUsed = 0
    /** Cleared for good the moment any aid is on -- switching them off at the end earns nothing. */
    private var aidsCleanRun = true
    /** A hint is being computed. Guards against a double tap counting two hints. */
    private var hintPending = false

    /**
     * The two reasons the clock may stand still, kept apart on purpose.
     *
     * [manuallyPaused] is the player's decision and hides the grid; [visible] follows
     * the activity's start/stop, stops the clock and changes nothing else. Returning
     * to the app therefore just carries on -- an extra tap for every notification the
     * player answers would be a toll, not a feature.
     */
    private var manuallyPaused = false
    private var visible = true

    init {
        // Read synchronously so the very first frame already shows the game in
        // progress, rather than flashing a freshly generated one. Same reason the
        // settings are read here and not collected from a flow.
        if (!restoreSavedGame()) newGame(Level.EASY)
    }

    private fun restoreSavedGame(): Boolean {
        val saved = prefs.loadGame() ?: return false
        val restore = saved.toGame() ?: run { prefs.clearGame(); return false }
        val g = SudokuGame(restore.puzzle)
        if (!g.importHistory(restore.history, restore.applied, restore.branchAt)) {
            prefs.clearGame()
            return false
        }
        game = g
        selected = -1
        countedSolved = saved.counted
        countedStart = true
        hintsUsed = saved.hintsUsed
        aidsCleanRun = !saved.aidsUsed
        accumulatedMs = saved.elapsedMs
        publish()
        syncTimer()
        return true
    }

    fun updateSettings(new: Settings) {
        settings = new
        prefs.save(new)
        publish()
        saveGame()
    }

    fun resetStats() {
        stats = Stats.EMPTY
        prefs.saveStats(stats)
        publish()
    }

    fun newGame(level: Level) {
        _ui.value = GameUiState(generating = true, level = level, settings = settings)
        stopTimer()
        manuallyPaused = false
        viewModelScope.launch {
            // Generation is 50-150 ms of pure computation. On the main thread that
            // would be a visible stall on every new puzzle.
            val puzzle = withContext(Dispatchers.Default) { factory.generate(level) }
            game = SudokuGame(puzzle)
            selected = -1
            accumulatedMs = 0L
            countedSolved = false
            countedStart = false
            hintsUsed = 0
            aidsCleanRun = true
            hintPending = false
            publish()
            saveGame()
            syncTimer()
        }
    }

    fun onCellTap(cell: Int) {
        game ?: return
        selected = if (selected == cell) -1 else cell
        publish()
    }

    /**
     * Places a digit in the selected cell. There is no note mode any more: the
     * keypad has a second, separate row for pencil marks, so what a tap means is
     * decided by *which* key was hit, not by a state the player set beforehand.
     *
     * Tapping the digit that is already there clears the cell -- that toggle lives
     * in [SudokuGame.setDigit] and is what makes the pressed-looking key honest.
     */
    fun onDigit(digit: Int) {
        val g = game ?: return
        val cell = selected
        if (cell < 0 || g.isGiven(cell)) return
        g.setDigit(cell, digit)
        onBoardChanged()
    }

    /** Adds or removes a single pencil mark on the selected cell. */
    fun onNote(digit: Int) {
        val g = game ?: return
        val cell = selected
        if (cell < 0 || g.isGiven(cell)) return
        g.toggleNote(cell, digit)
        onBoardChanged()
    }

    fun onErase() {
        val g = game ?: return
        if (selected < 0) return
        g.clearCell(selected)
        onBoardChanged()
    }

    fun onUndo() = game?.let { it.undo(); onBoardChanged() }

    fun onRedo() = game?.let { it.redo(); onBoardChanged() }

    // --- Zweig --------------------------------------------------------------

    /**
     * Opens a trial branch. Nothing on the board changes, so this does *not* go
     * through [onBoardChanged]: opening a branch must not count as the first move of
     * the game, and the standing hint is still valid for the position it was computed
     * against.
     */
    fun onBeginBranch() {
        val g = game ?: return
        if (g.isSolved()) return
        g.beginBranch()
        publish()
        saveGame()
    }

    /** Keeps the attempt. Again no board change -- only the provisional mark goes. */
    fun onCommitBranch() {
        val g = game ?: return
        g.commitBranch()
        publish()
        saveGame()
    }

    /** Throws the attempt away; the board really moves, so the usual funnel applies. */
    fun onDiscardBranch() {
        val g = game ?: return
        g.discardBranch()
        onBoardChanged()
    }

    // --- Pause ---------------------------------------------------------------

    /**
     * The player stops the clock. The grid goes away with it -- a paused clock in
     * front of a readable board is an invitation to think on for free, and thinking
     * is the whole of the game.
     */
    fun onPause() {
        val g = game ?: return
        if (g.isSolved()) return
        manuallyPaused = true
        syncTimer()
        publish()
        saveGame()      // the accumulated time is now final until the player is back
    }

    fun onResume() {
        manuallyPaused = false
        syncTimer()
        publish()
    }

    /**
     * The activity became visible or stopped. Called from `MainActivity.onStart` /
     * `onStop`, so it covers the screen going off, the app switcher and any other way
     * of leaving -- without this the clock ran all night on a dark screen.
     *
     * Leaving also saves: the process can be killed while stopped, and only the time
     * up to the last board change would otherwise survive.
     */
    fun onVisibilityChanged(nowVisible: Boolean) {
        if (visible == nowVisible) return
        visible = nowVisible
        syncTimer()
        publish()
        if (!nowVisible) saveGame()
    }

    // --- Tipp ---------------------------------------------------------------

    /**
     * One press advances the hint: locate, then reveal, then enter it.
     *
     * The digit only appears in the second step because the justification names it --
     * "only this cell in block 5 can take a 7" is already the answer.
     */
    fun onHint() {
        val g = game ?: return
        if (g.isSolved()) return
        when (_ui.value.hint?.stage) {
            null -> if (!hintPending) requestHint(g)
            HintStage.LOCATE -> {
                _ui.value = _ui.value.copy(hint = _ui.value.hint!!.copy(stage = HintStage.REVEAL))
                publish()
            }
            // Mid-chain: on to the next deduction. The board has not moved, so the
            // chain computed for it is still the truth.
            HintStage.REVEAL -> if (!_ui.value.hint!!.isLast) {
                val h = _ui.value.hint!!
                _ui.value = _ui.value.copy(hint = h.copy(stage = HintStage.LOCATE, index = h.index + 1))
                publish()
            } else {
                // Only a step that *places* something can be entered. An elimination
                // is shown and then dismissed: the app does not reach into the
                // player's pencil marks, and striking a candidate they never wrote
                // down would be a change they cannot see.
                when (val h = _ui.value.hint!!.hint) {
                    is Hint.Deduce -> if (h.last.technique.places) g.setDigit(h.last.cell, h.last.digit)
                    is Hint.Reveal -> g.setDigit(h.cell, h.digit)
                    Hint.DeadEnd -> Unit
                }
                selected = -1
                onBoardChanged()
            }
        }
    }

    fun onDismissHint() {
        _ui.value = _ui.value.copy(hint = null, hintDeadEnd = false)
        publish()
    }

    private fun requestHint(g: SudokuGame) {
        val board = IntArray(Units.CELLS) { g.valueAt(it) }
        hintPending = true
        viewModelScope.launch {
            // Proving the board dead runs a full solve. It is sub-millisecond, but it
            // happens on a button press and must not risk a dropped frame.
            val hint = withContext(Dispatchers.Default) {
                hintFinder.find(board, g.puzzle.solution)
            }
            hintPending = false
            if (game !== g) return@launch          // a new game started meanwhile
            if (hint is Hint.DeadEnd) {
                _ui.value = _ui.value.copy(hint = null, hintDeadEnd = true)
            } else {
                hintsUsed++
                _ui.value = _ui.value.copy(hint = HintState(hint, HintStage.LOCATE), hintDeadEnd = false)
            }
            publish()
            saveGame()
        }
    }

    // --- Zustandswechsel ----------------------------------------------------

    /**
     * The single place a board change is funnelled through.
     *
     * It owns three things that must not be spread out: a hint expires (one computed
     * against an older board is not merely stale, it can be wrong), the win is counted
     * exactly once, and the game is saved.
     */
    private fun onBoardChanged() {
        val wasSolved = _ui.value.solved
        _ui.value = _ui.value.copy(hint = null, hintDeadEnd = false)

        val g = game
        if (g != null && !countedStart) {
            countedStart = true
            stats = stats.withStarted(g.puzzle.level)
            prefs.saveStats(stats)
        }

        publish()
        if (!wasSolved && _ui.value.solved) onSolved()
        saveGame()
    }

    private fun onSolved() {
        stopTimer()   // syncTimer would do it too; being explicit here costs nothing
        val g = game ?: return
        // A full, conflict-free grid is the solution, so the open attempt worked --
        // leaving it provisional would offer to discard a finished puzzle.
        g.commitBranch()
        if (countedSolved) return
        countedSolved = true
        stats = stats.withSolved(
            level = g.puzzle.level,
            ms = accumulatedMs,
            noAids = aidsCleanRun,
            noHints = hintsUsed == 0,
        )
        prefs.saveStats(stats)
        publish()
        saveGame()
    }

    private fun saveGame() {
        val g = game ?: return
        prefs.saveGame(
            GameSnapshot(
                givens = g.puzzle.toLine(),
                solution = g.puzzle.solution.joinToString("") { it.toString() },
                level = g.puzzle.level,
                edits = g.exportHistory().map { GameSnapshot.encodeEdit(it) },
                applied = g.appliedCount,
                elapsedMs = if (_timer.value.running) elapsedNow() else accumulatedMs,
                hintsUsed = hintsUsed,
                aidsUsed = !aidsCleanRun,
                counted = countedSolved,
                branchAt = g.branchStart,
            )
        )
    }

    private fun publish() {
        val g = game
        if (g == null) {
            _ui.value = _ui.value.copy(generating = true, board = null)
            return
        }
        val values = IntArray(Units.CELLS) { g.valueAt(it) }
        val highlight =
            if (settings.highlightSameDigit && selected >= 0) values[selected] else 0
        val editable = selected >= 0 && !g.isGiven(selected)

        // The single gate. Conflicts are computed either way -- solved detection
        // needs them -- but when the aid is off the board is handed an all-false
        // array and never learns that anything was wrong. Deciding this here rather
        // than in the drawing code means there is exactly one place that could ever
        // leak the answer.
        val realConflicts = g.conflicts()
        val shown =
            if (settings.showConflicts) realConflicts else BooleanArray(Units.CELLS)
        // Pencil marks go through the very same gate. They are not computed unless
        // they are shown -- unlike the entries, whose conflicts the solved detection
        // needs either way.
        val shownNoteConflicts =
            if (settings.showConflicts) g.noteConflicts() else IntArray(Units.CELLS)

        val full = g.isFull()
        val solved = g.isSolved()
        val trial = g.trialCells()

        if (!settings.allAidsOff) aidsCleanRun = false

        // What the board shows of the hint. The pattern is visible from the first
        // stage -- that stage *is* "look here" -- while the units and the struck
        // candidates only appear with the reasoning, because they give it away.
        val hint = _ui.value.hint
        val hintCells = BooleanArray(Units.CELLS)
        var hintUnits = 0
        var hintStrikes = IntArray(0)
        val hintStep = hint?.step
        if (hintStep != null) {
            for (c in hintStep.pattern) hintCells[c] = true
            if (hint.stage == HintStage.REVEAL) {
                for (u in hintStep.units) hintUnits = hintUnits or (1 shl u)
                hintStrikes = IntArray(hintStep.eliminations.size) { i ->
                    val e = hintStep.eliminations[i]
                    e.cell * 16 + e.digit
                }
            }
        } else if (hint?.hint is Hint.Reveal) {
            hintCells[(hint.hint as Hint.Reveal).cell] = true
        }

        _ui.value = _ui.value.copy(
            board = BoardState(
                values = values,
                givens = BooleanArray(Units.CELLS) { g.isGiven(it) },
                notes = g.notes.copyOf(),
                conflicts = shown,
                noteConflicts = shownNoteConflicts,
                trial = trial,
                selected = selected,
                highlightDigit = highlight,
                highlightPeers = settings.highlightPeers,
                hintCells = hintCells,
                hintUnits = hintUnits,
                hintStrikes = hintStrikes,
            ),
            generating = false,
            level = g.puzzle.level,
            clueCount = g.puzzle.clueCount,
            remaining = g.remaining().toList(),
            selectionEditable = editable,
            selectedDigit = if (editable) values[selected] else 0,
            selectedNotes = if (editable) g.notes[selected] else 0,
            canUndo = g.canUndo,
            canRedo = g.canRedo,
            paused = manuallyPaused,
            inBranch = g.inBranch,
            branchCells = trial.count { it },
            solved = solved,
            settings = settings,
            stats = stats,
            hintsUsed = hintsUsed,
            aidsCleanRun = aidsCleanRun,
            fullButWrong = full && !solved,
        )
    }

    companion object {
        private const val TICK_MS = 500L

        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SudokuViewModel(SudomniaPrefs(context.applicationContext)) as T
            }
    }

    // --- Clock -------------------------------------------------------------
    // Elapsed time is derived from a monotonic clock rather than counted up per
    // tick, so a missed or late tick cannot make the displayed time drift.

    /**
     * The one place that decides whether the clock runs: a game is loaded, it is not
     * finished, the app is on screen and the player has not paused. Every caller just
     * changes one of those facts and asks again -- scattered start/stop calls are how
     * a clock ends up running in the background.
     */
    private fun syncTimer() {
        val g = game
        if (g != null && !g.isSolved() && visible && !manuallyPaused) startTimer() else stopTimer()
    }

    private fun startTimer() {
        // Restarting a running clock would move startedAt forward while accumulatedMs
        // still holds the old total -- the elapsed time in between would be lost.
        if (_timer.value.running) return
        startedAt = SystemClock.elapsedRealtime()
        val token = ++timerToken
        _timer.value = TimerState(accumulatedMs, running = true)
        viewModelScope.launch {
            while (timerToken == token) {
                delay(TICK_MS)
                if (timerToken != token) break
                _timer.value = TimerState(elapsedNow(), running = true)
            }
        }
    }

    private fun stopTimer() {
        if (_timer.value.running) accumulatedMs = elapsedNow()
        timerToken++
        _timer.value = TimerState(accumulatedMs, running = false)
    }

    private fun elapsedNow(): Long =
        accumulatedMs + (SystemClock.elapsedRealtime() - startedAt)

}
