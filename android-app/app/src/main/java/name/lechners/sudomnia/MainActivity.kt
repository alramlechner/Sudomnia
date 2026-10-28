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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import name.lechners.sudomnia.ui.analysis.AnalysisScreen
import name.lechners.sudomnia.ui.home.HomeScreen
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import name.lechners.sudomnia.diag.DiagnosticsLog
import name.lechners.sudomnia.ui.game.GameScreen
import name.lechners.sudomnia.ui.game.SudokuViewModel
import name.lechners.sudomnia.update.UpdateState
import name.lechners.sudomnia.update.UpdateSupport
import name.lechners.sudomnia.ui.theme.SudomniaTheme

/**
 * One screen, so no navigation graph and no back stack -- the level picker is a
 * dialog. When settings, statistics and a hint panel arrive this grows into the
 * same hand-written enum navigation Chessomnia uses; there is nothing to gain from
 * navigation-compose at this size.
 */
private enum class Screen { HOME, GAME, ANALYSIS }

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
            val state by vm.ui.collectAsState()
            SudomniaTheme(mode = state.settings.themeMode) {
                val timer by vm.timer.collectAsState()

                // The self-update only exists in the selfhosted flavour; in the Play
                // build, UpdateSupport returns null and the app contains neither the
                // network code nor the permission for it.
                val updateController = UpdateSupport.rememberController(this@MainActivity)
                var update: UpdateState? = null
                if (updateController != null) update = updateController.state.collectAsState().value

                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                val analysis by vm.analysis.collectAsState()
                androidx.compose.runtime.LaunchedEffect(screen) { vm.onBoardShown(screen == Screen.GAME) }
                // A saved ANALYSIS screen has no model after process death: fall back to home.
                if (screen == Screen.ANALYSIS && analysis == null) screen = Screen.HOME
                androidx.activity.compose.BackHandler(enabled = screen != Screen.HOME) {
                    if (screen == Screen.ANALYSIS) vm.closeAnalysis()
                    screen = Screen.HOME
                }
                val inset = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)
                val versionName = remember {
                    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: ""
                }

                when (screen) {
                Screen.HOME -> HomeScreen(
                    state = state,
                    elapsed = timer.format(),
                    versionName = versionName,
                    update = update,
                    onResume = { screen = Screen.GAME },
                    onNewGame = { level -> vm.newGame(level); screen = Screen.GAME },
                    onAnalyze = { if (vm.openAnalysis()) screen = Screen.ANALYSIS },
                    onSettingsChange = vm::updateSettings,
                    onResetStats = vm::resetStats,
                    onInstallUpdate = { updateController?.install() },
                    onCheckUpdate = { updateController?.checkNow() },
                    modifier = inset,
                )
                Screen.ANALYSIS -> analysis?.let { a ->
                    AnalysisScreen(
                        ui = a,
                        onStep = vm::analysisStep,
                        onGoTo = vm::analysisGoTo,
                        onPreviousError = vm::analysisPreviousError,
                        onNextError = vm::analysisNextError,
                        onHome = { vm.closeAnalysis(); screen = Screen.HOME },
                        modifier = inset,
                    )
                }
                Screen.GAME -> GameScreen(
                    state = state,
                    timer = timer,
                    update = update,
                    onCellTap = vm::onCellTap,
                    onCellLongPress = vm::onCellLongPress,
                    onDigitLongPress = vm::onDigitLongPress,
                    onNoteLongPress = vm::onNoteLongPress,
                    onDigit = vm::onDigit,
                    onNote = vm::onNote,
                    onErase = vm::onErase,
                    onUndo = { vm.onUndo() },
                    onRedo = { vm.onRedo() },
                    onPause = vm::onPause,
                    onResume = vm::onResume,
                    onBeginBranch = vm::onBeginBranch,
                    onCommitBranch = vm::onCommitBranch,
                    onDiscardBranch = { vm.onDiscardBranch() },
                    onDeadEndBranch = { vm.onDiscardBranch(deadEnd = true) },
                    onHome = { screen = Screen.HOME },
                    onGiveUp = { vm.giveUp(); screen = Screen.HOME },
                    onSettingsChange = vm::updateSettings,
                    onHint = vm::onHint,
                    onDismissHint = vm::onDismissHint,
                    onResetStats = vm::resetStats,
                    onInstallUpdate = { updateController?.install() },
                    onCheckUpdate = { updateController?.checkNow() },
                    modifier = inset,
                )
                }
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
