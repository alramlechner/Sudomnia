package name.lechners.sudomnia.update

import org.json.JSONObject

/**
 * The server's `latest.json`, as written by `deploy.sh`.
 *
 * Parsing lives here rather than in [UpdateClient] so it can be tested on the JVM --
 * the project has no Robolectric, so anything that has to be checked must be free of
 * Android types. `org.json` is part of the platform, so this costs no dependency in the
 * app (the unit tests pull in the reference implementation instead of the android.jar
 * stub, see build.gradle.kts).
 */
data class ReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val filename: String,
    /** Lower-case hex, empty if the manifest carries none. */
    val sha256: String,
) {
    companion object {
        /** @return null if the document is not a release manifest -- callers report a failure. */
        fun parse(json: String): ReleaseInfo? = try {
            val o = JSONObject(json)
            val name = o.optString("version_name")
            val file = o.optString("filename")
            if (!o.has("version_code") || name.isEmpty() || file.isEmpty()) null
            else ReleaseInfo(
                versionCode = o.getInt("version_code"),
                versionName = name,
                filename = file,
                sha256 = o.optString("sha256").lowercase(),
            )
        } catch (e: Exception) {
            null
        }
    }
}
