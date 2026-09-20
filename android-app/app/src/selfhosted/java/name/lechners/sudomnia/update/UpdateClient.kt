package name.lechners.sudomnia.update

import android.content.Context
import name.lechners.sudomnia.BuildConfig
import name.lechners.sudomnia.R
import java.io.File
import java.io.InputStream
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

/**
 * Talks to the update endpoint on the EnergyControl server. Blocking; call it off the
 * main thread.
 *
 * ### Why mTLS and not the plain LAN port
 * The APK also sits on the plain HTTP port for a manual browser download, but that port
 * is not reachable from outside the house. The mTLS port is the only one forwarded, and
 * it wants a client certificate -- so the app carries one ([R.raw.sudomnia_client], issued
 * once by `SudomniaClientCertTool`). That key is extractable from the APK, which is why
 * the server accepts it *only* for this endpoint: it is not registered as a MyMoney
 * device and opens nothing else.
 *
 * ### Two things that are not optional
 * - **The host name must match [BuildConfig.UPDATE_HOST].** Jetty checks SNI against the
 *   server certificate's SAN; an IP address is answered with HTTP 400 before any servlet
 *   runs. The name resolves inside the house and outside it (split-horizon DNS).
 * - **The trust anchor is the pinned server certificate** ([R.raw.server_cert]), not the
 *   system trust store -- the server is signed by a private CA that Android has never
 *   heard of. Pinning the leaf also means a stolen public CA cannot impersonate it.
 *
 * No OkHttp: `HttpsURLConnection` does this in a handful of lines, and the app has kept
 * its dependency list at "Compose and nothing else".
 *
 * ### Neither the host name nor the client key is in the repository
 * [BuildConfig.UPDATE_HOST] comes from the untracked `local.properties`
 * (`sudomnia.updateHost`, see RELEASING.md) and falls back to a placeholder that resolves
 * nowhere -- a public repository has no business naming this developer's server.
 * `sudomnia_client.p12` is git-ignored outright -- a private key has no business on GitHub
 * at all, even one that only opens an APK download. A clone therefore does not compile
 * until the file is put back (see RELEASING.md). That is deliberate: keeping the reference
 * a normal `R.raw` constant means a missing key fails at build time, in one obvious place,
 * rather than on someone's tablet.
 */
class UpdateClient(private val context: Context) {

    private val ssl: SSLSocketFactory by lazy { buildSocketFactory() }

    /** The published release, or throws if the server cannot be reached or answers rubbish. */
    fun fetchLatest(): ReleaseInfo {
        val body = open("$BASE_URL/latest.json").use { it.readBytes().decodeToString() }
        return ReleaseInfo.parse(body) ?: error("Unreadable version manifest")
    }

    /**
     * Downloads the current APK to [dest], verifying the SHA-256 from [release].
     *
     * The hash check is the reason this is not just a copy loop. `latest.json` has always
     * carried the digest and neither Oystra nor MyMoney ever looked at it -- on the LAN
     * that was defensible, over the open internet it is not. A mismatch deletes the file,
     * so a broken download can never be handed to the package installer.
     */
    fun download(release: ReleaseInfo, dest: File) {
        val digest = MessageDigest.getInstance("SHA-256")
        open("$BASE_URL/download").use { input ->
            dest.outputStream().use { output ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    digest.update(buf, 0, n)
                    output.write(buf, 0, n)
                }
            }
        }
        if (release.sha256.isNotEmpty()) {
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual != release.sha256) {
                dest.delete()
                error("Checksum mismatch")
            }
        }
    }

    private fun open(url: String): InputStream {
        val con = (URL(url).openConnection() as HttpsURLConnection).apply {
            sslSocketFactory = ssl
            connectTimeout = 5_000
            readTimeout = 30_000
            requestMethod = "GET"
        }
        val code = con.responseCode
        if (code != 200) {
            con.disconnect()
            error("HTTP $code")
        }
        return con.inputStream
    }

    private fun buildSocketFactory(): SSLSocketFactory {
        val clientStore = KeyStore.getInstance("PKCS12").apply {
            context.resources.openRawResource(R.raw.sudomnia_client).use { load(it, P12_PASSWORD) }
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(clientStore, P12_PASSWORD)
        }

        val serverCert = context.resources.openRawResource(R.raw.server_cert).use {
            CertificateFactory.getInstance("X.509").generateCertificate(it)
        }
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("server", serverCert)
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(trustStore)
        }

        return SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, tmf.trustManagers, null)
        }.socketFactory
    }

    companion object {
        /** Host name from BuildConfig, not address -- see the class comment on SNI. */
        val BASE_URL = "https://${BuildConfig.UPDATE_HOST}:8443/api/v1/sudomnia/app"

        /**
         * The PKCS12 password. It ships inside the APK, so it protects nothing -- but it
         * must not be *empty*: the JDK writes modern PKCS12 with PBES2/PBKDF2, and
         * Android's BouncyCastle refuses a zero-length password there with
         * `IllegalArgumentException: password empty`. That is exactly what the update
         * button failed with in 0.4.2. Must match SudomniaClientCertTool.
         */
        private val P12_PASSWORD = "sudomnia".toCharArray()
    }
}
