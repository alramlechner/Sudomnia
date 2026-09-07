package name.lechners.sudomnia.diag

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import name.lechners.sudomnia.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A log the player can hand over.
 *
 * The app has no crash reporter and is not going to get one -- it would mean a network
 * dependency and a privacy story for a single-player Sudoku. But "the update button says
 * IllegalArgumentException" is not something anyone can act on, and a tablet in the
 * living room has no logcat. So: everything interesting is appended to one text file,
 * and a button in the settings hands that file to the share sheet. Where it goes from
 * there -- Proton Drive, mail, whatever -- is the player's decision, every single time.
 *
 * Nothing is ever sent by itself. There is no upload path in this class on purpose.
 */
object DiagnosticsLog {

    private const val FILE_NAME = "sudomnia-log.txt"
    private const val REPORT_NAME = "sudomnia-report.txt"

    /** Above this the file is halved. A ring buffer in a file, cheaply. */
    private const val MAX_BYTES = 64 * 1024

    private val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY)

    private var file: File? = null

    /**
     * Points the log at the app's private storage and routes crashes into it.
     *
     * The previous handler is chained, not replaced: swallowing it would leave the app
     * hanging instead of dying, which is worse than the crash.
     */
    fun init(context: Context) {
        if (file != null) return
        file = File(context.filesDir, FILE_NAME)
        log("App", "Start ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) " +
            "auf ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            log("Absturz", "in Thread ${thread.name}", error)
            previous?.uncaughtException(thread, error)
        }
    }

    @Synchronized
    fun log(tag: String, message: String, error: Throwable? = null) {
        val f = file ?: return
        val text = buildString {
            append(timestamp.format(Date())).append("  ").append(tag).append(": ").append(message)
            if (error != null) {
                append('\n')
                // The stack trace is the whole point -- a message alone rarely says which
                // of five layers threw.
                append(StringWriter().also { error.printStackTrace(PrintWriter(it)) })
            }
            append('\n')
        }
        try {
            f.appendText(text)
            if (f.length() > MAX_BYTES) {
                val kept = f.readText().let { it.substring(it.length / 2) }
                f.writeText("[…älterer Teil des Protokolls entfernt…]\n" + kept.substringAfter('\n'))
            }
        } catch (ignored: Exception) {
            // Diagnostics must never be the thing that breaks the app.
        }
    }

    /** True if there is anything worth sharing. */
    fun hasEntries(): Boolean = (file?.length() ?: 0L) > 0L

    /**
     * Writes the report to the cache and returns a share intent for it.
     *
     * The text goes into the file *and* into EXTRA_TEXT: some targets take the
     * attachment, some only the body, and which one the player picks is not knowable here.
     */
    fun shareIntent(context: Context): Intent {
        val body = report()
        val out = File(context.cacheDir, REPORT_NAME)
        out.writeText(body)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", out)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Sudomnia ${BuildConfig.VERSION_NAME} — Fehlerbericht")
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, body.takeLast(4000))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Fehlerbericht teilen")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun report(): String = buildString {
        append("Sudomnia ").append(BuildConfig.VERSION_NAME)
        append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
        append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
        append(", Android ").append(Build.VERSION.RELEASE)
        append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        append("Erstellt: ").append(timestamp.format(Date())).append("\n\n")
        append(file?.takeIf { it.exists() }?.readText() ?: "(kein Protokoll)")
    }
}
