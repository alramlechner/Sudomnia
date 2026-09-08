package name.lechners.sudomnia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import name.lechners.sudomnia.diag.DiagnosticsLog
import name.lechners.sudomnia.ui.game.GameScreen
import name.lechners.sudomnia.ui.game.SudokuViewModel
import name.lechners.sudomnia.update.UpdateViewModel
import name.lechners.sudomnia.ui.theme.SudomniaTheme

/**
 * One screen, so no navigation graph and no back stack -- the level picker is a
 * dialog. When settings, statistics and a hint panel arrive this grows into the
 * same hand-written enum navigation Chessomnia uses; there is nothing to gain from
 * navigation-compose at this size.
 */
class MainActivity : ComponentActivity() {

    /**
     * Held by the activity, not fetched inside `setContent`, because the lifecycle
     * callbacks below need it. It is the same instance either way -- the activity is
     * the ViewModelStoreOwner in both cases.
     */
    private lateinit var vm: SudokuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Before anything else: from here on crashes land in a file the player can share.
        DiagnosticsLog.init(applicationContext)
        enableEdgeToEdge()
        // The grid is stared at for minutes without a touch; letting the screen
        // blank mid-puzzle would be the single most annoying possible bug.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        vm = ViewModelProvider(this, SudokuViewModel.factory(applicationContext))[
            SudokuViewModel::class.java
        ]

        setContent {
            SudomniaTheme {
                val state by vm.ui.collectAsState()
                val timer by vm.timer.collectAsState()

                // Zweites ViewModel mit Absicht: SudokuViewModel ist die Partie und soll
                // keinen Netzwerkcode bekommen.
                val updateVm: UpdateViewModel = ViewModelProvider(
                    this@MainActivity,
                    UpdateViewModel.factory(applicationContext),
                )[UpdateViewModel::class.java]
                val update by updateVm.state.collectAsState()

                GameScreen(
                    state = state,
                    timer = timer,
                    update = update,
                    onCellTap = vm::onCellTap,
                    onDigit = vm::onDigit,
                    onNote = vm::onNote,
                    onErase = vm::onErase,
                    onUndo = { vm.onUndo() },
                    onRedo = { vm.onRedo() },
                    onPause = vm::onPause,
                    onResume = vm::onResume,
                    onBeginBranch = vm::onBeginBranch,
                    onCommitBranch = vm::onCommitBranch,
                    onDiscardBranch = vm::onDiscardBranch,
                    onNewGame = vm::newGame,
                    onSettingsChange = vm::updateSettings,
                    onHint = vm::onHint,
                    onDismissHint = vm::onDismissHint,
                    onResetStats = vm::resetStats,
                    onInstallUpdate = updateVm::install,
                    onCheckUpdate = updateVm::checkNow,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars),
                )
            }
        }
    }

    // The clock must not run while nobody is looking at the board. onStart/onStop is
    // the right pair for that: it covers the screen going off, the app switcher and
    // the home button alike, and unlike onPause/onResume it does not fire for a
    // dialog or a half-visible split-screen window.
    override fun onStart() {
        super.onStart()
        vm.onVisibilityChanged(true)
    }

    override fun onStop() {
        vm.onVisibilityChanged(false)
        super.onStop()
    }
}
