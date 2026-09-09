package name.lechners.sudomnia.update

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable

/**
 * The Play flavour: there is no self-update, so there is no controller.
 *
 * Returning null is the whole implementation. The caller renders no banner and the
 * settings dialog shows the version without a check button -- updates arrive through
 * the store, which is both Google's rule and, for a store install, the thing the
 * player expects.
 *
 * The point of doing it this way rather than with a runtime flag: the networking code,
 * the pinned certificate and the `INTERNET` permission are not merely switched off in
 * this build, they are **not in it**. That is a claim the manifest and the APK can be
 * checked against -- see the verification step in RELEASING.md.
 */
object UpdateSupport {

    /** No update mechanism in this flavour. */
    const val AVAILABLE = false

    @Composable
    fun rememberController(activity: ComponentActivity): UpdateController? = null
}
