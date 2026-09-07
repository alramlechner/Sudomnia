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
import androidx.lifecycle.viewmodel.compose.viewModel
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Before anything else: from here on crashes land in a file the player can share.
        DiagnosticsLog.init(applicationContext)
        enableEdgeToEdge()
        // The grid is stared at for minutes without a touch; letting the screen
        // blank mid-puzzle would be the single most annoying possible bug.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            SudomniaTheme {
                val vm: SudokuViewModel = viewModel(
                    factory = SudokuViewModel.factory(applicationContext),
                )
                val state by vm.ui.collectAsState()
                val timer by vm.timer.collectAsState()

                // Zweites ViewModel mit Absicht: SudokuViewModel ist die Partie und soll
                // keinen Netzwerkcode bekommen.
                val updateVm: UpdateViewModel = viewModel(
                    factory = UpdateViewModel.factory(applicationContext),
                )
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
}
