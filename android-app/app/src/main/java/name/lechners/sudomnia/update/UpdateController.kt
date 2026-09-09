package name.lechners.sudomnia.update

import kotlinx.coroutines.flow.StateFlow

/**
 * What the user interface needs from the self-update, and nothing else.
 *
 * The interface exists because the update **is not part of every build**. Google Play
 * forbids an app distributed through it from updating itself by any other route, so
 * the `play` flavour ships without the whole mechanism -- no network code, no
 * certificate, and no `INTERNET` permission to go with it. The `selfhosted` flavour,
 * which the house server hands out directly, keeps it.
 *
 * Everything the two flavours share therefore talks to this interface, and the one
 * place that knows which of the two is being built is `UpdateSupport` -- a file that
 * exists once per flavour source set and nowhere in `main`.
 *
 * [UpdateState] itself stays in `main`: it is plain data, the banner and the settings
 * row render it, and duplicating it per flavour would mean two shapes of the same
 * state machine.
 */
interface UpdateController {
    val state: StateFlow<UpdateState>

    /** Downloads the known release and hands it to the package installer. */
    fun install()

    /** Checks now, on the player's request rather than on the timer. */
    fun checkNow()
}
