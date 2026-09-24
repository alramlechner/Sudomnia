package name.lechners.sudomnia.ui.game

import name.lechners.sudomnia.data.History
import name.lechners.sudomnia.data.Settings
import name.lechners.sudomnia.data.Stats
import name.lechners.sudomnia.rules.Hint
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.rules.Step
import name.lechners.sudomnia.ui.board.BoardState

/**
 * Everything on screen except the clock.
 *
 * `remaining` is a `List<Int>`, not an `IntArray`, so this can stay a data class:
 * an array field would give it identity equality and break recomposition. The
 * arrays that really have to be arrays live in [BoardState], which compares them
 * by content.
 */
data class GameUiState(
    val board: BoardState? = null,
    val generating: Boolean = true,
    val level: Level = Level.EASY,
    val clueCount: Int = 0,
    /** A cell is selected and it is not a clue -- both input rows hang off this. */
    val selectionEditable: Boolean = false,
    /** The digit in the selected cell, 0 if empty or nothing is selected. */
    val selectedDigit: Int = 0,
    /** The selected cell's pencil marks as a 9-bit mask. */
    val selectedNotes: Int = 0,
    val remaining: List<Int> = List(10) { 0 },
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    /**
     * The player paused. The clock stands and the grid is not drawn -- see
     * [SudokuViewModel.onPause]. Leaving the app stops the clock too, but that is not
     * this flag: it changes nothing on screen and undoes itself on return.
     */
    val paused: Boolean = false,
    /** A trial branch is open: entries are provisional until kept or discarded. */
    val inBranch: Boolean = false,
    /** How many cells carry a trial digit -- what the branch bar counts out. */
    val branchCells: Int = 0,
    val solved: Boolean = false,
    /** Wrong entries this game, counted only while the warning aid is on. */
    val mistakes: Int = 0,
    /** The third wrong entry has been made: the game is over and nothing more can be entered. */
    val lost: Boolean = false,
    /**
     * The cell the last entry got wrong, -1 if the last change was fine.
     *
     * It lives here rather than in [BoardState] because it is a message, not a colour:
     * the board already draws the entry, and pointing at the cell would turn a one-line
     * "that is not it" into a permanent "here is what to fix".
     */
    val wrongCell: Int = -1,
    val settings: Settings = Settings(),
    val stats: Stats = Stats.EMPTY,
    val history: History = History(),
    val lastSolve: SolveSummary? = null,
    val hint: HintState? = null,
    /** Asked for a hint, but the entries so far can no longer lead to a solution. */
    val hintDeadEnd: Boolean = false,
    val hintsUsed: Int = 0,
    /** No aid was on at any point in this game -- drives the "no aids" badge. */
    val aidsCleanRun: Boolean = true,
    /**
     * Every cell filled, but the grid does not obey the rules.
     *
     * Only reachable with conflict marking switched off -- with it on, the offending
     * cells are already red. It exists so that finishing a grid wrongly is not met
     * with silence: the app says *that* something is wrong without saying *where*,
     * which is the line the setting is about.
     */
    val fullButWrong: Boolean = false,
) {
    /**
     * The game is over, one way or the other.
     *
     * Everything that used to ask `solved` in order to go dead -- clock, keypad, hint,
     * pause, branch bar -- asks this instead. Two flags to check in six places is how
     * one of them gets forgotten and the keypad stays live on a lost board.
     */
    val finished: Boolean get() = solved || lost
}

/** What the win just achieved, shown on the overlay. Null fields: nothing to compare yet. */
data class SolveSummary(val ratingDelta: Int?, val fasterThanPercent: Int?)

/** How much of the current step has been revealed: where it is, then why. */
enum class HintStage { LOCATE, REVEAL }

/**
 * A hint being shown.
 *
 * `Hint` is a sealed interface of data classes, so this can be a data class too --
 * no arrays involved, unlike [BoardState].
 */
data class HintState(
    val hint: Hint,
    val stage: HintStage,
    /** How far along the chain of deductions the player has pressed. */
    val index: Int = 0,
) {
    /** The step being shown, or null for a bare reveal / dead end. */
    val step: Step? get() = (hint as? Hint.Deduce)?.steps?.getOrNull(index)

    /** Is this the last step of the chain -- the one that may be entered? */
    val isLast: Boolean get() = hint !is Hint.Deduce || index == hint.steps.size - 1
}

/**
 * The clock lives in its own flow.
 *
 * It ticks a few times a second. If it shared a state object with the board, every
 * tick would recompose the 81-cell canvas -- the same reason Chessomnia keeps its
 * chess clock out of the board state.
 */
data class TimerState(
    val elapsedMs: Long = 0L,
    val running: Boolean = false,
) {
    fun format(): String {
        val total = elapsedMs / 1000
        val m = total / 60
        val s = total % 60
        return if (m >= 60) "%d:%02d:%02d".format(m / 60, m % 60, s) else "%d:%02d".format(m, s)
    }
}
