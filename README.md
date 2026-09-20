# Sudomnia

A Sudoku for Android tablets. Prototype (0.4.3).

Generates its own puzzles — each one is **guaranteed to have exactly one solution**,
because it is dug out of a finished grid and checked for uniqueness after every removed
cell. No puzzle database. The game itself is offline; the network is used exclusively
for the update check (see "Updates").

## Status

Playable: grid, digit pad, notes (pencil candidates), undo/redo, branches (trial
attempts), timer with pause, solved detection, four measured difficulty levels, app
icon, signed release build, auto-update.

**Input: the cell first, then the decision.** Below the board are two rows — the large
digits on top (tap to enter, tap again to clear), the small note buttons below (on/off).
There is no mode: noting three candidates is three taps. A filled small button means
"this note is set in the selected cell", so the row doubles as the readout. With no cell
selected, both rows are grey.

Tapping a cell highlights every cell with the same digit — entered **and** noted. Works
on the given clues too.

**Branch**, for when no cell is unique anymore: "Start branch" sets a marker; from then
on every digit entered is only a trial and shown in **yellow** on the grid. If the
attempt works out, "Commit" makes it permanent; if it doesn't, "Discard" clears all of
it in one step — including the notes that vanished from neighbouring cells along the
way. While the branch is open, "Undo" stops at its start, so you don't slip past it by
accident. Whether the attempt has already failed is something the hint will tell you
too: it reports that the puzzle no longer works out.

**Hint**, for when you're stuck — and it always says *why*. The first press highlights
what's involved, the second names the technique and the reason ("Locked candidates: in
box 5 the 7 fits only in cells of column 3 — so it can go from the rest of the column"),
the third enters the digit. Where several steps are needed to get there, the hint walks
you through the chain. Candidates it strikes out are shown crossed out in the grid, even
if you never noted them. If you've already gone wrong, the app tells you *that* the
puzzle no longer works out, but not where.

**Pause** in the header: the clock stops, the board is hidden (not veiled — it simply
isn't drawn), and a tap anywhere resumes. The clock also stops on its own as soon as the
app is no longer visible — screen off, app switcher, home button — and resumes when you
come back.

**Statistics** per level: solved, started, best time, plus the "no aids" and "no hint"
badges.

The **running game is saved** — digits, notes, undo stack, an open branch and the
elapsed time all survive closing the app.

**Every aid can be switched off** (header → "Aids"): conflict marking, highlighting the
same digit, highlighting row/column/box, dimming finished digits. The most important is
conflict marking — it tells you instantly whether a digit is even possible in a cell,
taking half the thinking off your hands. It also applies to **notes**: a candidate whose
digit already appears in the row, column or box turns red. Off means the app stays
quiet, and you only notice when the grid doesn't work out. The settings survive an app
restart.

**Four levels, and they are measured.** Before it hands a puzzle over, the app solves it
the way a person would — using only techniques that can be explained in one sentence —
and the hardest technique needed is the level:

| Level | What it requires |
|---|---|
| **Easy** | singles only, and at least 36 clues stay on the board |
| **Medium** | singles only, but dug as deep as it goes |
| **Hard** | locked candidates or a pair/triple |
| **Expert** | X-Wing, simple colouring or XY-Wing |

**No puzzle requires guessing.** That's new, and it wasn't always true: measured across
150 puzzles from the old "Hard" level, **53% could not be solved by any human
technique**. If you got stuck in one, there was no way to tell whether you were missing
something or whether there was nothing to see. The generator now only digs as deep as
the technique ladder can still follow.

The **interface is available in English and German**; English is the default, German
comes automatically on a German-language device.

Not there yet: explanations beyond singles — that would need the technique ladder.

## Building

Java 17 is mandatory — newer JDKs crash the Kotlin compiler.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=$HOME/android-sdk
cd android-app
./gradlew test                     # solver, generator, game logic
./gradlew test -DsudokuDeep=1      # the same tests with 10x as many puzzles
./gradlew assemblePlayDebug        # app/build/outputs/apk/play/debug/…
./gradlew lintPlayRelease          # NewApi & co. -- deliberately not run in the release path
./gradlew bundlePlayRelease        # the AAB for Google Play
./gradlew assembleSelfhostedRelease # the APK for the house server
```

**Two flavours.** `play` is the store build: no self-update and **not a single
permission** — Google forbids apps from the store updating themselves any other way.
`selfhosted` is the build for the devices at home, which fetch their updates from the
EnergyControl server; only this one needs the client certificate, and it won't build
without it. A fresh clone compiles `play` without any secret.

The release build is signed via a **not-checked-in** `android-app/keystore.properties`
(template: `keystore.properties.example`). If the file is missing, an unsigned APK
results — exactly what a fork or a CI run wants.

The version lives in exactly one place: `version.properties`.

## Releasing

Two paths, and they are not the same:

```bash
cd android-app && ./gradlew bundlePlayRelease   # the AAB for Google Play
./deploy.sh --notes "What's new"                # the APK for the devices at home
```

The Play build contains no self-update and holds **not a single permission**; the
selfhosted build fetches its updates from the EnergyControl server at home. The full
process, including verification of the uploaded bundle, is in `RELEASING.md`; the store
texts are in `store/`.

## Updates

**From the Play Store**, updates arrive like for any other app. The Play build has no
update path of its own, and must not have one.

**The build for the devices at home** (`selfhosted`) polls
`https://<configured host>:8443/api/v1/sudomnia/app/latest.json` on startup and every 15
minutes after that. If a higher `version_code` is listed there, a banner "Version x.y.z
is available" appears above the board; tapping it downloads the APK, checks its SHA-256
and hands it to the package installer. The same function sits at the bottom of the aids
dialog, there as "check now". The hostname itself is not in the repo but in each
machine's own, not-committed `local.properties` (`sudomnia.updateHost`, see
RELEASING.md).

This path goes over the server's mTLS port — **so the update works from outside the
house too, without a VPN.** The client certificate this needs is baked into the app
(`app/src/selfhosted/res/raw/sudomnia_client.p12`, not in the repo); server-side, it is
restricted to exactly this one download and opens nothing else. If it gets stuck,
there's a fallback on the home network via the same host's plain-HTTP port in a browser.

Only this build therefore has two permissions: `INTERNET` and
`REQUEST_INSTALL_PACKAGES`. Both are used exclusively for this — the game itself knows
no server, collects nothing and sends nothing.

## When something goes wrong

Aids dialog → **Error report → Share**. This opens the normal share sheet with a text
report: device, Android version, app version, and the log of recent errors with stack
traces. Crashes land in it automatically.

Nothing is sent on its own — the app has no upload path. Where the report goes is
decided every time by the share dialog.

## Structure

| Directory | Contents |
|---|---|
| `android-app/app/src/main/java/…/rules/` | Solver, generator, digger — pure Kotlin, no Android imports, fully testable on the JVM |
| `…/game/` | Running game: inputs, notes, undo history |
| `…/data/` | Settings, SharedPreferences with a migration chain |
| `…/ui/` | Compose UI, one screen |
| `…/update/` | Update check: manifest, mTLS client, its own ViewModel |
| `tools/` | `generate_app_icon.py` — generates the three icon layers from one source |

Details and the reasoning behind the design decisions: **ARCHITECTURE.md**.

## License

Apache-2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).

| What | Origin | License |
|---|---|---|
| Puzzles | None from outside. The generator creates every one itself | — |
| App icon | Own work, generated by `tools/generate_app_icon.py` | Apache-2.0 |
| Reference puzzles in tests | Project Euler 96 no. 1, a 17-clue puzzle, "AI Escargot" | Facts¹ |
| Libraries | AndroidX (Core, Activity, Lifecycle), Jetpack Compose | Apache-2.0 |

¹ Individual Sudoku grids are not copyrightable works. They live only in the test path
anyway and are not shipped with the app.

## Three files are missing from the repo

All three deliberately (see [RELEASING.md](RELEASING.md)). **The `play` build needs
none of them** — a fresh clone compiles it without any secret:

- `android-app/app/src/selfhosted/res/raw/sudomnia_client.p12` — the client certificate
  for the update check against my home server. A private key doesn't belong on GitHub.
  **Without this file, the `selfhosted` flavour won't compile**
  (`R.raw.sudomnia_client` doesn't exist then). That's intentional: a missing key should
  show up while building, in an obvious place, rather than on some tablet. Anyone
  forking doesn't need this flavour anyway — the server behind it only exists at my
  house.
- `android-app/keystore.properties` — without it, an **unsigned** release results.
- `android-app/play-service-account.json` — only for automated Play upload. Without it,
  all other tasks run fine; only `publish*` fails, with a clear message.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Privacy: [PRIVACY.md](PRIVACY.md).
Changes: [CHANGELOG.md](CHANGELOG.md). Project page:
[alramlechner.github.io/Sudomnia](https://alramlechner.github.io/Sudomnia/).

## Imprint

`sudomnia@lechners.name` — the address the in-app error report suggests as the
recipient, and where everything else about this project goes.
