# Changelog

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versioning based on [SemVer](https://semver.org/).

## [1.0.3] – 2026-09-22

### Fixed
- **"Fehler melden" crashed the app on the Play build.** The FileProvider it
  needs to share the diagnostics report was declared only in the selfhosted
  flavour's manifest; the Play flavour had no provider at all, so tapping the
  button threw immediately. Now declared once for both flavours.

## [1.0.2] – 2026-09-21

### Changed
- **The board uses the full screen width past the "Larger" accessibility text
  size** (system font scale >= 1.3), dropping its side padding and its tablet
  width cap. `fontScale` only scales text (sp), never the screen's actual
  width (dp) -- so someone who deliberately turned the system font up gets a
  genuinely bigger 9x9 grid and bigger digits, not just a grid that survives
  the setting.

## [1.0.1] – 2026-09-21

### Fixed
- **Board could render invisible.** Its size was the vertical space left over
  after the fixed rows around it, inside a column that could not scroll -- if
  those rows claimed more height than usual (larger system-bar insets, a
  longer translation), the leftover shrank to zero and the 9x9 grid vanished
  while every other row kept rendering normally. The board's side is now
  derived from the available width instead, and the screen scrolls as a
  fallback if content still doesn't fit vertically.

## [1.0.0] – 2026-09-18

First release.

### Added
- **Spanish** as a third language, alongside English (default) and German --
  app, store listing and feature graphic.

### Changed
- Added contact address `sudomnia@lechners.name` (imprint, privacy, suggestion
  in the error report).
- The update server's hostname is no longer in the repo; `BASE_URL` now comes
  from the untracked `local.properties`.

## [0.6.0] – 2026-09-14

### Added
- **Warning for wrong entries** -- the fifth aid, and the only one **off** by
  default. When enabled, it compares every entered digit against the solution and
  reports immediately. The other four aids talk about the *rules*, this one reads
  the *answer* -- which is why it costs something: **three wrong entries end the
  game.** The warning counts down the remaining attempts out loud, so the third one
  is never a surprise, and the hint in the settings dialog states the cost before
  the switch is flipped.
- No check happens inside a **branch**: an assumption there is *allowed* to be
  wrong. Undo does not refund a used attempt -- otherwise the cost wouldn't be one
  -- and the counter lives in the save state, so restarting the app doesn't give
  fresh attempts either.

## [0.5.1] – 2026-09-10

### Fixed
- **Conflict marking overlooked notes.** It only marked entered digits; a pencil
  candidate whose digit was already set in the row, column or box went unnoticed.
  Now it turns red -- the same statement about the same rule. Two *notes* of the
  same digit in one unit remain allowed, that's what notes are for.

## [0.5.0] – 2026-09-09

### Added
- **The technique ladder is here.** `HumanSolver` solves a puzzle the way a person
  would: hidden/naked single, locked candidates, naked/hidden pair and triple,
  X-Wing, Swordfish, simple colouring, XY-Wing -- and **never by guessing**. The
  highest technique needed is the level (`Grader`), measured instead of guessed.
- **The hint now justifies every step.** It takes the next step of the ladder from
  the board as it stands, names the technique and the reason, and where needed
  walks through a chain of steps up to the digit that can be entered. Struck
  candidates are shown crossed out in the grid. Blank reveals without justification
  are thereby gone for puzzles from this generator.
- **Four levels** instead of three: Easy, Medium, Hard, Expert. "Provisional" has
  disappeared from the header, replaced by the clue count.

### Fixed
- **Puzzles that required guessing.** The old "Hard" level just meant "singles
  don't suffice" and otherwise took anything that was unique -- measured across
  150 such puzzles, **53% could not be solved by any human technique**. The digger
  now undoes a removal as soon as the ladder can no longer finish the puzzle.

### Release preparation
- **English as the default language**, German as a translation (`values-de`). Up
  to this point the app was German-only.
- **Two flavours** (`play`, `selfhosted`). The Play build does not contain the
  self-update -- Google forbids apps from the store updating themselves any other
  way -- and therefore holds **not a single permission**. The selfhosted flavour
  stays as before; `deploy.sh` builds it.
- **A fresh clone now compiles** (`assemblePlayDebug`). Previously it failed on
  the missing client certificate, which deliberately isn't in the repo.
- **Store material and project page**: `store/` with the listing texts in both
  languages, data-safety answers, 512 icon and feature graphics; `docs/` as the
  project page including the privacy policy the Play listing points to;
  `RELEASING.md` for both distribution paths; a CI workflow that checks the
  permissions of the Play flavour.
- **AAB instead of APK for Play** via the Play Publisher plugin. The Play tasks
  of the selfhosted flavour are disabled.

## [0.4.5] – 2026-09-08

### Added
- **Pause** in the header: stops the clock and hides the board (it isn't drawn,
  no translucent veil). Resumes with a tap anywhere.

### Fixed
- **The clock kept running when the app wasn't visible** -- screen off, app
  switcher, home button. It used to be stopped only on "new game" and "solved";
  the ViewModel didn't know about lifecycle events at all, and the elapsed time
  comes from `elapsedRealtime()`, which keeps running even in deep sleep. Now one
  place decides whether the clock may run. `onStop` also saves the game state --
  the process can die in the background, and previously only the time up to the
  last board change survived.

## [0.4.4] – 2026-09-08

### Added
- **Branch** -- a trial attempt for the point where no cell is forced anymore.
  "Start branch" sets a marker, everything entered afterwards is provisional and
  shown **yellow** in the grid (cell and digit). "Discard" undoes the whole
  attempt in one step, "Commit" makes it permanent. Undo stops at the marker
  while the branch is open; the branch survives closing the app and is committed
  automatically on solving.

## [0.4.3] – 2026-09-07

### Fixed
- The update button failed with `IllegalArgumentException: password empty`. Since
  8u301, the JDK writes PKCS12 with PBES2/PBKDF2, and Android's BouncyCastle
  rejects a zero-length password there. The client certificate now has a password
  and is written in the old PKCS12 format.

### Added
- **Share error report** (aids dialog): log, crashes and stack traces go out
  through the normal share sheet. No upload path in the app.

## [0.4.2] – 2026-09-07

### Changed
- **Input no longer has a mode.** Instead of a "notes" toggle, two rows sit under
  the board: digits on top, note buttons below. Select the cell first, then
  decide -- three candidates are three taps. The note buttons show the candidate
  state of the selected cell; with no editable cell, both rows are visibly dead.

### Added
- **Auto-update** over the house server's mTLS port, so it works without a VPN
  too. Banner above the board, version line in the aids dialog, SHA-256 check of
  the download.

## [0.4.0] – 2026-09-03

First playable state: generator with guaranteed unique solution, three levels,
notes, undo/redo, timer, hint function, statistics, saved game state, aids that
can be switched off, app icon, signed release build.
