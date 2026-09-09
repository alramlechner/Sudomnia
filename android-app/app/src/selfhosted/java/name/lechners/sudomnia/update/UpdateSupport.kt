package name.lechners.sudomnia.update

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider

/**
 * The self-hosted flavour: the app fetches its own updates from the house server.
 *
 * A second ViewModel on purpose -- [name.lechners.sudomnia.ui.game.SudokuViewModel] is
 * the game and has no business knowing about sockets.
 */
object UpdateSupport {

    const val AVAILABLE = true

    @Composable
    fun rememberController(activity: ComponentActivity): UpdateController? =
        ViewModelProvider(
            activity,
            UpdateViewModel.factory(activity.applicationContext),
        )[UpdateViewModel::class.java]
}
