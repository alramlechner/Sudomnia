# Releasing

Sudomnia has **two distribution paths**, and they are not the same:

| Path | Flavour | Via |
|---|---|---|
| Google Play | `play` | `bundlePlayRelease` → AAB, uploaded |
| Devices at home | `selfhosted` | `./deploy.sh` → APK to the EnergyControl server |

The difference is not a runtime setting: the Play flavour does **not** contain the
self-update and therefore holds not a single permission. Google forbids apps from
the store updating themselves any other way. A `selfhosted` build in the store would
be the one mistake in this setup that can't be quietly taken back — which is why its
Play tasks are disabled in `build.gradle.kts`, in addition to this paragraph.

---

## Prerequisites

| | |
|---|---|
| JDK | **17.** Newer JDKs crash the Kotlin compiler in this project. |
| Android SDK | `ANDROID_HOME` set, build-tools 35 present |
| Signing | `android-app/keystore.properties`, not in the repo — template: `keystore.properties.example` |
| Play upload | `android-app/play-service-account.json`, not in the repo — template: `play-service-account.json.example`. Only needed for automated upload. |
| selfhosted | `android-app/app/src/selfhosted/res/raw/{sudomnia_client.p12,server_cert.pem}`, not in the repo (see below) |
| selfhosted | `android-app/local.properties`: `sudomnia.updateHost=<your-hostname>` (see below) |

The upload key does **not** belong in the repository. Losing it doesn't cost the app
itself (Play App Signing holds the actual signing key), but Google then has to reset
the upload key before the next release.

---

## 1. Version

`version.properties` in the root directory is the single source. Both numbers move
together:

```properties
VERSION_CODE=10
VERSION_NAME=0.5.0
```

`VERSION_CODE` must be **strictly greater** than anything Play has ever seen. Gaps
don't matter, going backwards does, and it can't be undone — Play remembers a
version code even for a discarded release.

Every build that leaves this machine gets its own version, including test builds.
Two artifacts with the same version are the way to go chasing a bug that was never
installed.

## 2. Regenerating generated files

Only when the source has changed:

```bash
python3 tools/generate_app_icon.py       # app icon
python3 tools/render_store_assets.py     # Play icon 512 and feature graphics
```

## 3. Check and build

```bash
cd android-app
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
./gradlew test                  # the full test suite, both flavours
./gradlew test -DsudokuDeep=1   # ~2,500 puzzles, each generated and graded
./gradlew lintPlayRelease
./gradlew bundlePlayRelease
```

The artifact is `app/build/outputs/bundle/playRelease/app-play-release.aab`.

⚠️ Play wants the **bundle**, not an APK. `assemblePlayRelease` produces an APK for
sideloading onto a test device; that's not what gets uploaded.

## 4. Verify the artifact

What's checked is what ships — not what compiled. "BUILD SUCCESSFUL" is not proof.

```bash
AAB=app/build/outputs/bundle/playRelease/app-play-release.aab

# Signed with the upload key, not a debug key
jarsigner -verify -verbose:summary -certs "$AAB" | grep "Signed by"

# All three languages are really in there (one string each that only occurs there)
unzip -p "$AAB" base/resources.pb | grep -ac "Verstecktes Single"   # de
unzip -p "$AAB" base/resources.pb | grep -ac "Hidden single"        # en
unzip -p "$AAB" base/resources.pb | grep -ac "Único oculto"         # es

# The self-update is NOT in there
unzip -p "$AAB" base/dex/classes.dex | grep -ac "sudomnia/update/UpdateClient"   # must be 0
```

And in the merged manifest, where a dependency can quietly bring back a permission:

```bash
grep -oE '<uses-permission[^>]*>' \
  app/build/intermediates/merged_manifest/playRelease/*/AndroidManifest.xml
```

The only line allowed is `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which AndroidX
declares about the app itself. In particular, no `INTERNET`. This is exactly what
the CI workflow also checks on every push.

## 5. Commit and tag

```bash
git commit -am "Version 0.5.0 (10)"
git tag -a v0.5.0 -m "Version 0.5.0"
git push && git push --tags
```

## 6. Upload

### Automated

Once `android-app/play-service-account.json` exists:

```bash
cd android-app
./gradlew publishPlayBundle                     # internal track
./gradlew publishPlayBundle --track production  # explicit; never the default
```

The defaults are deliberately harmless: a bare `publishPlayBundle` goes to the
**internal** channel — a named list of at most 100 testers, not the store. Without
the key file, all other tasks keep working, and only the `publish*` tasks fail,
with a message that says so.

⚠️ The Developer API **cannot create the first release of an app.** Google requires
that a bundle be uploaded once by hand through the Console for this package name.
Until then, the manual path applies.

### By hand

Play Console: **Test and release → Production** (or a test channel) → *Create new
release* → upload the `.aab` → release notes → roll out.

Release notes come from `CHANGELOG.md`, shortened. Play allows 500 characters per
language, and `en-US`, `de-DE` and `es-ES` each need their own text.

The store listing itself lives in `store/listing-en.md`, `store/listing-de.md` and
`store/listing-es.md` — including the data-safety answers and the categorisation
(which applies once, not per language). Keep these files in sync with what's
actually entered in the Console, or the next release gets edited from a stale
source.

---

## First release only

Happens once and isn't part of the routine:

1. **Identity verification** of the developer account.
2. **Create the app in the Console**: name, default language, "app" vs. "game", free.
3. **Closed testing with 12 testers over 14 consecutive days** — a requirement for
   new personal developer accounts before production is unlocked. By far the
   slowest step, and it depends on people, not code. Start early.
4. **Play App Signing**: accept on the first upload. For Sudomnia, this should
   upload the **existing key** (Play Console → App integrity → "Export and upload a
   key from a Java keystore"). Reason: the devices at home have the `selfhosted`
   flavour installed with the same `applicationId` and this key. Only if Play signs
   with the same key can the store version update them, instead of forcing a
   reinstall with lost statistics and lost save state.
5. **Screenshots**: phone, 7" and 10" tablet. They must show the real app, so they
   have to be taken on a device — see `store/screenshots/README.md`.

---

## The second path: the devices at home

```bash
./deploy.sh --notes "What's new"      # version +1, release build, ship to the server
./deploy.sh --no-bump                 # if the version was already bumped by hand
```

Builds `assembleSelfhostedRelease`, drops the APK under `/var/lib/sudomnia/apk/` and
writes `latest.json`, which the installed apps poll. No restart of the
EnergyControl server is needed for this; the apps see the new version within 15
minutes.

### The hostname

`UpdateClient.BASE_URL` no longer names a hostname in the source code. It comes via
`BuildConfig.UPDATE_HOST` from the untracked `android-app/local.properties`:

```properties
sudomnia.updateHost=<your-hostname>
```

If the entry is missing, the build falls back to `sudomnia.invalid` (RFC 2606,
deliberately resolves nowhere) — so the `selfhosted` flavour still compiles without
this line, but then can't reach a server. A fresh clone therefore never reveals
which host is behind it.

### The client certificate and the pinned server certificate

`android-app/app/src/selfhosted/res/raw/sudomnia_client.p12` **and**
`.../server_cert.pem` are git-ignored. **Without them, the selfhosted flavour
won't compile** — `UpdateClient` references `R.raw.sudomnia_client` and
`R.raw.server_cert` normally. That's intentional: a missing key should show up
while building, not only on the device. The `play` flavour needs neither, so a
fresh clone compiles it without any secret.

Re-issuing them (on the server host; this also writes the serial number to the
server's allow-list and the pinned server certificate to `res/raw/`):

```bash
cd /home/pi/projects/EnergyControl
mvn -o -q dependency:build-classpath -Dmdep.outputFile=/tmp/ec-cp.txt
java -cp "target/classes:$(cat /tmp/ec-cp.txt)" \
     name.lechners.energycontrol.tools.SudomniaClientCertTool
```

Then restart the EnergyControl server — the allow-list is read on startup. Remove
old serial numbers from `/var/lib/sudomnia/allowed-serials.txt` by hand.

Don't use `mvn exec:java`: `pom.xml` hardcodes `Daemon` as `mainClass`,
`-Dexec.mainClass` gets overridden by that, and a second server process starts.

### Pitfalls

- **Never an empty PKCS12 password.** Android's BouncyCastle rejects a zero-length
  password in PBKDF2 with `IllegalArgumentException: password empty`. The value
  lives in `UpdateClient.P12_PASSWORD` and must match `SudomniaClientCertTool`.
- **Change the version only in `version.properties`.** `build.gradle.kts` reads it
  from there; a second place would be immediately inconsistent.
- **The key is the same as for Play.** Both paths sign with `keystore.properties`
  — that's exactly why the tablets can switch between the flavours (see point 4
  above).
