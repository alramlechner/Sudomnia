package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.background
import name.lechners.sudomnia.ui.theme.LogoBlue
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import name.lechners.sudomnia.R
import name.lechners.sudomnia.data.Settings
import name.lechners.sudomnia.game.MistakeTally
import name.lechners.sudomnia.rules.Level
import name.lechners.sudomnia.ui.board.SudokuBoard
import name.lechners.sudomnia.ui.theme.AppBackground
import name.lechners.sudomnia.ui.theme.AppSurface
import name.lechners.sudomnia.ui.theme.InkConflict
import name.lechners.sudomnia.ui.theme.TextSecondary
import name.lechners.sudomnia.update.UpdateState

@Composable
fun GameScreen(
    state: GameUiState,
    timer: TimerState,
    update: UpdateState?,
    onCellTap: (Int) -> Unit,
    onCellLongPress: (Int) -> Unit,
    onDigitLongPress: (Int) -> Unit,
    onNoteLongPress: (Int) -> Unit,
    onDigit: (Int) -> Unit,
    onNote: (Int) -> Unit,
    onErase: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onBeginBranch: () -> Unit,
    onCommitBranch: () -> Unit,
    onDiscardBranch: () -> Unit,
    onDeadEndBranch: () -> Unit,
    onHome: () -> Unit,
    onGiveUp: () -> Unit,
    onSettingsChange: (Settings) -> Unit,
    onHint: () -> Unit,
    onDismissHint: () -> Unit,
    onResetStats: () -> Unit,
    onInstallUpdate: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmGiveUp by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

    // fontScale only scales sp, never the dp width a reader's screen actually has --
    // so someone who turned the system font up past the "Larger" step gets the
    // benefit of that choice only if the layout also gives up its side padding and
    // its tablet width cap on the board. Below that step, phones keep the margin.
    val fontScale = LocalDensity.current.fontScale
    val fullWidthBoard = fontScale >= 1.3f

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(AppBackground)) {
        val landscape = maxWidth > maxHeight
        val header: @Composable ColumnScope.() -> Unit = {
        TopBar(
            level = state.level,
            clueCount = state.clueCount,
            elapsed = timer.format(),
            settings = state.settings,
            canPause = !state.generating && !state.finished && !state.paused,
            onPause = onPause,
            onHome = onHome,
            onSettings = { showSettings = true },
            onStats = { showStats = true },
            canUndo = state.canUndo && !state.lost,
            canRedo = state.canRedo && !state.lost,
            canErase = state.selectionEditable && !state.generating && !state.finished && !state.paused,
            canHint = !state.generating && !state.finished && !state.paused,
            onErase = onErase,
            onHint = onHint,
            onUndo = onUndo,
            onRedo = onRedo,
            onThemeToggle = {
                onSettingsChange(state.settings.copy(themeMode = state.settings.themeMode.next()))
            },
        )

        UpdateBanner(state = update, onInstall = onInstallUpdate)

        HintCard(
            state = state.hint,
            deadEnd = state.hintDeadEnd,
            onAdvance = onHint,
            onDismiss = onDismissHint,
        )

        // The wrong-entry warning, and only while the entry is still the last
        // thing that happened. It counts the strikes out loud: a warning that did
        // not say what it costs would spring the third one as a surprise.
        if (state.wrongCell >= 0 && !state.lost) {
            val left = MistakeTally.LIMIT - state.mistakes
            Text(
                text = pluralStringResource(R.plurals.wrong_entry, left, left),
                color = InkConflict,
                fontSize = 14.sp,
            )
        }

        // Only ever visible with conflict marking off. Says that something is
        // wrong without saying where -- otherwise finishing a grid incorrectly
        // is met with nothing at all, which reads as a broken app.
        if (state.fullButWrong) {
            Text(
                text = stringResource(R.string.full_but_wrong),
                color = InkConflict,
                fontSize = 14.sp,
            )
        }
        }

        val controls: @Composable ColumnScope.() -> Unit = {
        // Directly under the board: it comments on the yellow cells up there.
        BranchBar(
            inBranch = state.inBranch,
            cells = state.branchCells,
            enabled = !state.generating && !state.finished && !state.paused,
            onBegin = onBeginBranch,
            onCommit = onCommitBranch,
            onDiscard = onDiscardBranch,
            onDeadEnd = onDeadEndBranch,
        )

        // Two permanent rows, no mode switch: pick a cell, then decide. The
        // rows go dead without an editable cell, which is how the order is
        // taught -- see Keypad.kt.
        val canEdit = state.selectionEditable && !state.generating && !state.finished &&
            !state.paused
        DigitPad(
            remaining = state.remaining,
            selectedDigit = state.selectedDigit,
            armed = state.armedDigit,
            // With nothing selected the row arms a digit for the board taps.
            enabled = if (state.selectionEditable) canEdit
            else (state.board?.selected ?: -1) < 0 && !state.generating && !state.finished &&
                !state.paused,
            dimCompleted = state.settings.dimCompletedDigits,
            onDigit = onDigit,
            onLongPress = onDigitLongPress,
            modeEnabled = !state.generating && !state.finished && !state.paused,
        )

        NotePad(
            notes = state.selectedNotes,
            armed = state.armedNote,
            // A filled cell cannot hold pencil marks; SudokuGame.toggleNote
            // ignores it, so the row says so instead of swallowing taps. With
            // nothing selected the row arms a digit for the board instead.
            enabled = if (state.selectionEditable) canEdit && state.selectedDigit == 0
            else !state.generating && !state.finished && !state.paused,
            onNote = onNote,
            onLongPress = onNoteLongPress,
            modeEnabled = !state.generating && !state.finished && !state.paused,
        )

        if (state.gameRunning && !state.paused) {
            TextButton(onClick = { confirmGiveUp = true }) {
                Text(stringResource(R.string.home_give_up), color = TextSecondary)
            }
        }
        }

            // The board side used to be min(width, height) inside a
            // weight(1f) box: whatever vertical space the fixed rows around it left
            // over. On a column that isn't allowed to scroll, "leftover" can be zero
            // -- taller system-bar insets or a longer translation in one of the fixed
            // rows is enough to starve the board down to nothing while everything
            // else keeps rendering fine. Deriving the side from width instead avoids
            // that: width doesn't depend on how much the rows above and below claim.
            // (It must not derive from height via fillMaxHeight().aspectRatio(1f)
            // either -- that's the reverse bug, an overly-wide board, and the exact
            // one Chessomnia hit.) The surrounding verticalScroll is the safety net
            // for the case where width-sized content still doesn't fit vertically.
        val boardBox: @Composable (Modifier) -> Unit = { sizing ->
          Box(
            modifier = sizing
                .clip(RoundedCornerShape(6.dp))
                .then(
                    if (state.armedNote != 0 || state.armedDigit != 0) {
                        Modifier.border(3.dp, LogoBlue, RoundedCornerShape(6.dp))
                    } else Modifier,
                ),
            contentAlignment = Alignment.Center,
          ) {
            val board = state.board
            if (state.paused) {
                // Nothing is drawn while paused -- the overlay below is opaque,
                // but not drawing the grid at all is the honest version of
                // "the board is hidden".
                Box(modifier = Modifier.fillMaxSize().background(AppSurface))
            } else if (board == null || state.generating) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.generating),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            } else {
                SudokuBoard(
                    state = board,
                    onCellTap = onCellTap,
                    onCellLongPress = onCellLongPress,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        }

        if (landscape) {
            val side = minOf(maxHeight - 24.dp, maxWidth * 0.6f)
            Row(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                boardBox(Modifier.size(side))
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    header()
                    controls()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (fullWidthBoard) 0.dp else 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                header()
                boardBox(
                    Modifier
                        .fillMaxWidth()
                        .then(if (fullWidthBoard) Modifier else Modifier.widthIn(max = 560.dp))
                        .aspectRatio(1f)
                )
                controls()
            }
        }

        // Above everything, including the dialogs' trigger buttons: while it is up,
        // the only thing on screen is the way back.
        if (state.paused) {
            PauseOverlay(level = state.level, elapsed = timer.format(), onResume = onResume)
        }

        if (state.lost) {
            LostOverlay(
                level = state.level,
                elapsed = timer.format(),
                onHome = onHome,
            )
        }

        if (state.solved) {
            SolvedOverlay(
                level = state.level,
                elapsed = timer.format(),
                hintsUsed = state.hintsUsed,
                noAids = state.aidsCleanRun,
                summary = state.lastSolve,
                onHome = onHome,
            )
        }

        if (confirmGiveUp) {
            AlertDialog(
                onDismissRequest = { confirmGiveUp = false },
                title = { Text(stringResource(R.string.home_give_up_title)) },
                text = {
                    Text(
                        stringResource(
                            if (state.settings.warnOnWrong) R.string.home_give_up_body
                            else R.string.home_give_up_body_analyze
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = { confirmGiveUp = false; onGiveUp() }) {
                        Text(stringResource(R.string.home_give_up))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmGiveUp = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        if (showSettings) {
            SettingsDialog(
                settings = state.settings,
                update = update,
                onChange = onSettingsChange,
                onInstallUpdate = onInstallUpdate,
                onCheckUpdate = onCheckUpdate,
                onDismiss = { showSettings = false },
            )
        }

        if (showStats) {
            StatsDialog(
                stats = state.stats,
                history = state.history,
                onReset = onResetStats,
                onDismiss = { showStats = false },
            )
        }
    }
}
