package name.lechners.sudomnia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import name.lechners.sudomnia.update.UpdateState

@Composable
fun GameScreen(
    state: GameUiState,
    timer: TimerState,
    update: UpdateState?,
    onCellTap: (Int) -> Unit,
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
    onNewGame: (Level) -> Unit,
    onSettingsChange: (Settings) -> Unit,
    onHint: () -> Unit,
    onDismissHint: () -> Unit,
    onResetStats: () -> Unit,
    onInstallUpdate: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLevelPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

    // fontScale only scales sp, never the dp width a reader's screen actually has --
    // so someone who turned the system font up past the "Larger" step gets the
    // benefit of that choice only if the layout also gives up its side padding and
    // its tablet width cap on the board. Below that step, phones keep the margin.
    val fontScale = LocalDensity.current.fontScale
    val fullWidthBoard = fontScale >= 1.3f

    Box(modifier = modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (fullWidthBoard) 0.dp else 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TopBar(
                level = state.level,
                clueCount = state.clueCount,
                elapsed = timer.format(),
                settings = state.settings,
                canPause = !state.generating && !state.finished && !state.paused,
                onPause = onPause,
                onNewGame = { showLevelPicker = true },
                onSettings = { showSettings = true },
                onStats = { showStats = true },
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fullWidthBoard) Modifier else Modifier.widthIn(max = 560.dp))
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp)),
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Directly under the board: it comments on the yellow cells up there.
            BranchBar(
                inBranch = state.inBranch,
                cells = state.branchCells,
                enabled = !state.generating && !state.finished && !state.paused,
                onBegin = onBeginBranch,
                onCommit = onCommitBranch,
                onDiscard = onDiscardBranch,
            )

            // Two permanent rows, no mode switch: pick a cell, then decide. The
            // rows go dead without an editable cell, which is how the order is
            // taught -- see Keypad.kt.
            val canEdit = state.selectionEditable && !state.generating && !state.finished &&
                !state.paused
            DigitPad(
                remaining = state.remaining,
                selectedDigit = state.selectedDigit,
                enabled = canEdit,
                dimCompleted = state.settings.dimCompletedDigits,
                onDigit = onDigit,
            )

            NotePad(
                notes = state.selectedNotes,
                // A filled cell cannot hold pencil marks; SudokuGame.toggleNote
                // ignores it, so the row says so instead of swallowing taps.
                enabled = canEdit && state.selectedDigit == 0,
                onNote = onNote,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onErase, enabled = canEdit) {
                    Text(stringResource(R.string.erase))
                }
                TextButton(
                    onClick = onHint,
                    enabled = !state.generating && !state.finished && !state.paused,
                ) {
                    Text(stringResource(R.string.hint))
                }
                // Undo stays live on a solved board -- unsolving it is allowed, and the
                // win is counted once either way. On a lost one it does not: three
                // wrong entries you can take back are not three strikes.
                TextButton(onClick = onUndo, enabled = state.canUndo && !state.lost) {
                    Text(stringResource(R.string.undo))
                }
                TextButton(onClick = onRedo, enabled = state.canRedo && !state.lost) {
                    Text(stringResource(R.string.redo))
                }
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
                onNewGame = { showLevelPicker = true },
            )
        }

        if (state.solved) {
            SolvedOverlay(
                level = state.level,
                elapsed = timer.format(),
                hintsUsed = state.hintsUsed,
                noAids = state.aidsCleanRun,
                onNewGame = { showLevelPicker = true },
            )
        }

        if (showLevelPicker) {
            NewGameDialog(
                onPick = { showLevelPicker = false; onNewGame(it) },
                onDismiss = { showLevelPicker = false },
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
                onReset = onResetStats,
                onDismiss = { showStats = false },
            )
        }
    }
}
