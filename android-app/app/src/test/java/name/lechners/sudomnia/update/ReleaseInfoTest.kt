package name.lechners.sudomnia.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The release manifest is the one piece of the update path that is pure data, so it is
 * the one piece that can be tested without a device. Everything it guards against has
 * the same shape: the app must not act on a document it did not understand.
 */
class ReleaseInfoTest {

    private val real = """
        {
          "version_code": 5,
          "version_name": "0.5.0",
          "filename": "sudomnia-0.5.0.apk",
          "sha256": "ABCDEF0123",
          "released_at": "2026-09-07T08:00:00Z",
          "release_notes": "Zwei Eingabereihen"
        }
    """.trimIndent()

    @Test
    fun readsAPublishedManifest() {
        val info = ReleaseInfo.parse(real)!!
        assertEquals(5, info.versionCode)
        assertEquals("0.5.0", info.versionName)
        assertEquals("sudomnia-0.5.0.apk", info.filename)
        // Lower-cased on the way in, so the comparison against the computed digest
        // cannot fail on capitalisation alone.
        assertEquals("abcdef0123", info.sha256)
    }

    @Test
    fun toleratesAManifestWithoutAHash() {
        val info = ReleaseInfo.parse(
            """{"version_code":4,"version_name":"0.4.0","filename":"sudomnia-0.4.0.apk"}"""
        )!!
        assertEquals("", info.sha256)
    }

    @Test
    fun rejectsIncompleteAndBrokenDocuments() {
        // No version_code: comparing against it would silently mean "0", i.e. never update.
        assertNull(ReleaseInfo.parse("""{"version_name":"0.5.0","filename":"a.apk"}"""))
        assertNull(ReleaseInfo.parse("""{"version_code":5,"filename":"a.apk"}"""))
        assertNull(ReleaseInfo.parse("""{"version_code":5,"version_name":"0.5.0"}"""))
        // Anything that is not the manifest at all -- a login page, an error body, garbage.
        assertNull(ReleaseInfo.parse("<html>404</html>"))
        assertNull(ReleaseInfo.parse(""))
    }
}
