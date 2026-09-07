package name.lechners.sudomnia.update

/**
 * Where the update check stands right now.
 *
 * Deliberately a state *machine* and not a rendered sentence plus a busy flag. Oystra
 * shipped that shape and it cost five releases (1.0.53–1.0.57): the UI could not derive
 * "is there something to install" from a string, so the install button was unreachable
 * while the app happily reported that an update existed. [installableVersion] is the
 * one question the UI actually asks.
 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Current : UpdateState
    data class Available(val versionName: String) : UpdateState
    data class Downloading(val versionName: String) : UpdateState
    data class Failed(val message: String, val versionName: String? = null) : UpdateState

    /** The version that could be installed, or null if there is nothing to install. */
    val installableVersion: String?
        get() = when (this) {
            is Available -> versionName
            is Downloading -> versionName
            is Failed -> versionName
            else -> null
        }

    val busy: Boolean get() = this is Checking || this is Downloading
}
