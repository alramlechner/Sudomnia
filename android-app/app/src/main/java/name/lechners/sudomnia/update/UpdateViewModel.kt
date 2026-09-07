package name.lechners.sudomnia.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import name.lechners.sudomnia.diag.DiagnosticsLog
import java.io.File

/**
 * The update check, kept out of `SudokuViewModel` on purpose: that one is the game and
 * has no business knowing about sockets.
 */
class UpdateViewModel(private val context: Context) : ViewModel() {

    private val client = UpdateClient(context)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var latest: ReleaseInfo? = null

    init {
        check(manual = false)
        viewModelScope.launch {
            while (true) {
                delay(CHECK_INTERVAL_MS)
                check(manual = false)
            }
        }
    }

    /** "Sudomnia 0.4.0 (4)" for the settings dialog. */
    @Suppress("DEPRECATION")
    fun versionLabel(): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return "${info.versionName ?: "?"} (${currentVersionCode()})"
    }

    fun checkNow() = check(manual = true)

    private fun check(manual: Boolean) {
        if (_state.value.busy) return
        if (manual) _state.value = UpdateState.Checking
        viewModelScope.launch {
            try {
                val release = withContext(Dispatchers.IO) { client.fetchLatest() }
                latest = release
                _state.value =
                    if (release.versionCode > currentVersionCode()) UpdateState.Available(release.versionName)
                    else UpdateState.Current
            } catch (e: Exception) {
                DiagnosticsLog.log("Update", "Pruefung fehlgeschlagen", e)
                // A failed *background* check must not wipe out a find: the tablet drops
                // off the wifi regularly, and the install button should not vanish under
                // the player's finger because a poll happened to time out.
                if (manual) {
                    _state.value = UpdateState.Failed(
                        e.message ?: e.javaClass.simpleName,
                        _state.value.installableVersion,
                    )
                }
            }
        }
    }

    /**
     * Downloads and hands the APK to the package installer.
     *
     * The state goes back to [UpdateState.Available] *before* the installer is launched,
     * so cancelling in the system dialog leaves a working button behind rather than a
     * screen that claims to be downloading forever.
     */
    fun install() {
        val release = latest ?: return
        if (_state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Downloading(release.versionName)

        viewModelScope.launch {
            val dest = File(context.cacheDir, "sudomnia-update.apk")
            try {
                withContext(Dispatchers.IO) { client.download(release, dest) }
                _state.value = UpdateState.Available(release.versionName)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            } catch (e: Exception) {
                DiagnosticsLog.log("Update", "Installation von ${release.versionName} fehlgeschlagen", e)
                _state.value = UpdateState.Failed(
                    e.message ?: e.javaClass.simpleName,
                    release.versionName,
                )
            }
        }
    }

    // The PackageInfoFlags overload only exists from API 33; minSdk here is 30.
    @Suppress("DEPRECATION")
    private fun currentVersionCode(): Int =
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()

    companion object {
        private const val CHECK_INTERVAL_MS = 15 * 60_000L

        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    UpdateViewModel(context.applicationContext) as T
            }
    }
}
