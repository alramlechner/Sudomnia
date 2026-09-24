# Sudomnia — Architecture

Status 0.4.3 (prototype). Sister project to [Chessomnia](https://github.com/alramlechner/Chessomnia);
build setup, package layering and test philosophy are carried over from there.

---

## 1. Layers

```
rules/   pure Kotlin, no Android imports   -> testable on the JVM
  |
game/    running game (inputs, notes, undo)
  |
data/    settings, statistics, save state (SharedPreferences)
  |
ui/      Compose, one screen, one ViewModel

update/  update check  -- depends on none of the other layers
diag/    error log -- writable from anywhere, depends on nothing
```

One Gradle module, namespace `name.lechners.sudomnia`, minSdk 30 / targetSdk 36, JDK 17.
Dependencies: core-ktx, Compose BOM, activity-compose, lifecycle-viewmodel-compose, junit.
No Room, no DI framework, no HTTP library.

**Two flavours, and the difference is not a switch.** `play` is the Google Play build:
**not a single permission**, no network code, no certificate. `selfhosted` is the build
for the devices at home and contains the self-update, with `INTERNET` and
`REQUEST_INSTALL_PACKAGES` (§11). Google forbids apps from the store updating themselves
any other way — the Play build therefore doesn't just have that code disabled, it
**doesn't contain it at all**. That is exactly what `update/` was always meant for: its
own package with no connection to `rules/`, `game/` or `data/`.

The split happens via Gradle source directories, not an `if`:

```
src/main/…/update/UpdateState.kt        pure data that the UI renders
src/main/…/update/UpdateController.kt   what the UI needs -- three members
src/play/…/update/UpdateSupport.kt      returns null. That's the whole file.
src/selfhosted/…/update/UpdateSupport.kt + UpdateClient/UpdateViewModel/ReleaseInfo
```

`UpdateSupport` exists exactly once per flavour and nowhere in `main` — it is the only
place that knows which of the two is being built. The side effect matters just as much as
the store rule: **a fresh clone compiles `play` without any secret.** Before, it failed on
`R.raw.sudomnia_client`, and for an open-source project that is not a minor detail but the
barrier to entry.

---

## 2. How a puzzle is made

Placing random digits doesn't work — that almost always yields an unsolvable or an
ambiguous grid. Instead, the puzzle is **dug out**:

1. `GridGenerator` produces a complete valid grid. Speed-up: boxes 1, 5 and 9 share no
   row, column or box with each other, so they can be filled up front as three
   independent random permutations of 1–9. After that, the search practically never
   backtracks. ~50–200 µs.
2. `Digger` empties cells in random order and undoes any removal that destroys
   uniqueness. **Uniqueness is thereby guaranteed by construction, not checked
   afterwards.**
3. `PuzzleFactory` combines both and delivers a `Puzzle`.

### Uniqueness check

`Solver` is constraint propagation (naked + hidden singles) plus MRV backtracking,
allocation-free: candidates are 9-bit masks in `IntArray`s, and backtracking restores a
pre-allocated snapshot instead of replaying a change trail.

The one operation everything is built on:

```kotlin
fun countSolutions(givens: IntArray, limit: Int = 2, out: IntArray? = null): Int
```

`limit = 2` answers "is the solution unique?"

The digger, however, doesn't ask it that way; it asks: *"is there a solution with a
different digit in this cell?"* — one run with `limit = 1` per candidate. Almost all of
them end in a contradiction after a few propagation steps, because the remaining grid is
massively overdetermined. Roughly a factor of 3 faster than counting solutions fully.

**Why bitmasks and not `Set<Int>`:** a puzzle costs ~80 solver calls; with a `HashSet` per
cell that would be a six-figure number of allocations per puzzle. `Integer.bitCount` and
`numberOfTrailingZeros` are single ARM64 instructions under ART.

**Why not Dancing Links:** DLX is faster on the hardest puzzles, but needs ~3,240 node
objects per run. At 80 runs per puzzle that's ~260,000 objects — GC pressure while a
Compose UI is drawing next to it. DLX is intended as an *independent test oracle*, not for
production.

---

## 3. Difficulty

The clue count is a poor indicator. Measured against this generator, maximally dug
singles-only puzzles and puzzles that need real techniques land **on the same decimal
place** at ~24.5 clues. The number doesn't separate them at all.

The level therefore comes from the **technique ladder**: `HumanSolver` solves the puzzle
the way a person would — using only techniques that can be explained in one sentence, and
**never by guessing**. The highest rung needed is the level (`Grader`).

| Rung | What you see |
|---|---|
| Hidden Single | in this unit, only one place is left for the 5 |
| Naked Single | in this cell, only one digit is still possible |
| Locked Candidates | in the box, the 7 sits only in one row → out of the rest of the row |
| Naked Pair/Triple | two (three) cells share two (three) digits |
| Hidden Pair/Triple | two (three) digits can only go in two (three) cells |
| X-Wing, Swordfish | the same digit in the same two (three) columns of two (three) rows |
| Simple Colouring | two-colour chains of one digit, followed by colour |
| XY-Wing | a pivot {x,y} with two wings {x,z} and {y,z} |

### The four bands

| Level | Rule | Avg. clues | Avg. steps | ms/puzzle |
|---|---|---|---|---|
| **Easy** | singles suffice, ≥ 36 clues stay on the board | 36.0 | 45 | 2.3 |
| **Medium** | singles suffice, dug out to the max | 24.8 | 56 | 3.3 |
| **Hard** | locked candidates or a subset needed | 24.4 | 61 | 13.6 |
| **Expert** | X-Wing, colouring or XY-Wing needed | 24.8 | 63 | 25.5 |

Measured over 100 puzzles each with `generator-cli --mode grade` on a Raspberry Pi 5. The
tool is why there are numbers here instead of guesses — after any change to `Digger`,
`Technique` or `HumanSolver` it should be run again.

### What the ladder actually changed

Not the labels — the puzzles. The old "Hard" level meant "singles don't suffice" and
otherwise took anything that was unique. Measuring across 150 such puzzles: **53% could
not be solved by any technique on the ladder** — they required forcing chains or, in
practice, guessing. The player had no way to tell whether they were missing something or
whether there was nothing to see.

The digger now undoes a removal as soon as the ladder can no longer finish the puzzle.
**Every delivered puzzle is solvable without guessing** — that is the promise that makes
"branch" (§4) make sense at all as a *voluntary* tool.

### What that costs

The boundary is also the shortcut: after each removal, the digger only checks against the
*ceiling of the targeted band*. Easy and Medium therefore stay on the fast singles oracle
(`SinglesSolver` with `Grid.propagate`, one pass instead of step by step), and only Expert
pays for the full ladder — 25 ms per puzzle, generation included.

### Discarded: "naked singles" vs. "hidden singles" as the level boundary

The first draft separated Easy (naked singles only) from Medium (hidden singles needed).
That is **backwards** from a human point of view: a naked single ("this cell has only one
candidate left") requires checking all 20 neighbours and ruling out eight digits. A hidden
single ("in this box, only one place is left for the 5") is found by scanning three lines
— it is the first technique every guide shows. Cheap for a machine and cheap for a human
run against each other here. The same reasoning determines the order in `Technique`.

### Discarded: forcing chains, nice loops, ALS

They solve more puzzles — but the explanation for such a step is a paragraph, not a
sentence. A hint nobody can follow is worse than an honest "nothing is forced right now."
Puzzles that need them are not delivered.

Uniqueness techniques (unique rectangle) are missing for a different reason: they argue
from "the puzzle has exactly one solution" — a fact about the setter, not about the grid.
They would also make the solver useless as a *checker* of uniqueness, which is exactly
what the generator needs it for.

## 4. The running game

`SudokuGame` holds inputs, notes and the undo history.

**What gets persisted later is the starting state plus the edit list**, not a copy of the
state — the same principle as Chessomnia's move list. That's why an edit (`Edit`) is
already a *list* of cell changes: setting a digit also clears it from the notes of all 20
neighbours, and undo has to reverse both.

### Branches: a trial attempt

On "Hard", it's common for no cell to be forced anymore (§5) — the way forward is to
assume a digit and follow the consequences. `beginBranch()` sets a marker in the edit list
for this; everything after it is provisional until `commitBranch()` keeps it or
`discardBranch()` undoes the whole attempt.

**A branch is only this marker** — no second board, no copy of the state. Discarding is
undo down to the marker followed by truncating the list. This restores notes and the
candidates struck from neighbours exactly as precisely as the undo button, without a
single extra line having to be written for it; if the branch were a copy of the board,
there would be two truths about the same save state.

Three rules follow from this:

- **Undo stops at the marker.** Otherwise the branch would stand open around edits that
  no longer belong to it, and "undo everything" would have no defined scope. The boundary
  disappears immediately once the branch is committed.
- **The redo stack is discarded when a branch is *opened***, not when it's discarded. That
  keeps discarding a pure truncation to the marker, and it can never revive edits from
  *before* the branch.
- **Branches don't nest.** A second `beginBranch()` has no effect. A stack would be cheap
  to build, but "which branch am I discarding right now?" is a question the UI would then
  have to answer — and the case this is about is one assumption and its consequences.

`trialCells()` returns the cells whose **digit** has changed since the marker — that is
what turns yellow. Notes deliberately don't count: setting a digit clears it from the
notes of up to 20 neighbours, and colouring all of those would spread the attempt over a
quarter of the board. A cell that's emptied again drops out of the list, because nothing
of the attempt is left in it.

**Whether the attempt has failed is answered by the existing hint**: it already checks
first whether the board is still solvable (§5), and reports exactly that otherwise. The
branch needs no check of its own for this — and above all no automatic "that was wrong"
that would take away the work the player was trying to do.

**Conflicts, not errors.** What gets marked is a digit that appears twice in a row,
column or box — a statement about the rules that the player could work out themselves. A
comparison against the stored solution would be something else entirely: the app would
be silently solving the puzzle along with the player. This boundary is deliberate — and it
still holds without exception in `SudokuGame`. Only the player can cross it, by turning on
the warning for wrong entries (§7); the comparison then lives in the ViewModel, not in the
model.

**Notes too.** `noteConflicts()` marks a pencil candidate whose digit is already set in
the neighbourhood — the same statement about the same rule, just about a note instead of
an entry. Two *notes* of the same digit in one unit, on the other hand, are not a
conflict: both are allowed to be candidates, that's what notes are for.

The gap only existed because setting a digit clears it from the notes of all 20
neighbours — an impossible candidate can therefore only arise if it is *noted
afterwards*. That's exactly the moment you want to know about it.

`isSolved()` checks "full and conflict-free" and likewise doesn't consult the solution —
for a uniquely solvable puzzle, that's the same thing.

---

## 5. Hint

The hint is **one step of the technique ladder, taken from the board as it stands**
(`HumanSolver.nextSteps`) — the same code that measures the level. It can therefore always
say *why*, and it only ever says things the player could have seen themselves.

`rules/Hint.kt` answers "what now?" in this order:

1. `grid.load(board)` fails → **dead** (two equal digits in one unit)
2. `solver.countSolutions(board, limit = 1) == 0` → **dead**
3. the ladder's chain up to the next settable digit
4. otherwise `bestBranchCell()` → reveal a blank

### Why a chain and not one step

An elimination doesn't change the board. Show the player a single one, and the next press
shows the same one again — forever. The chain therefore runs up to the step that actually
sets a digit: "cross this out, then that, and now the 7 is forced." Each press advances one
step, each step has two stages (*where* — then *why*), and it ends with "enter."

Measured across 2,563 hints in all four bands: **2,514 chains are a single step**, 49 are
longer, the longest was 20. The upper bound in `nextSteps` is accordingly not a designed
length but only a cutoff condition.

### What the hint does not do

**It does not touch the player's notes.** An elimination is shown and then closed off;
there is no "enter" for it. Crossing out candidates the player never noted would be a
change they can't see — and the notes are theirs.

**Notes don't feed into it either**, `find()` doesn't even accept them. Notes are
incomplete, go stale and can be wrong; a hint that relied on them would be provably wrong,
and the player would have no way to notice — to them, the app is the authority. The
candidate state from `Grid.load(board)` is canonical instead.

**Step 2 is a precondition, not an extra feature.** On a dead board, any derivation would
be an argument inside a contradiction. The test is exact, not heuristic: the puzzle has
exactly one solution, so the board is dead exactly when one entry deviates from it. Only
*that* is shown, never *where* — the way out is the existing undo button.

### Revealing a blank is a leftover, not a continuing need

It has become unreachable for puzzles from this generator: every one is solvable by the
ladder (§3), and the test `everyHintIsExplainable` plays every puzzle of every level
through using only the hint button, without ever seeing a reveal. It remains for two cases
that are not hypothetical: a save from an older version whose puzzle was dug out without
this guarantee, and a board the player has brought, through their own correct moves, into
a position the ladder can't crack.

### What this replaced

Before, the hint could justify exactly two things — hidden and naked single — and
otherwise revealed a blank. On "Hard", **every** puzzle got stuck with singles alone, that
was the definition of the level; the hint there could therefore only ever reveal. What was
measured back then was how far a reveal carries (17 further forced cells, roughly two
reveals per puzzle). That very number was the argument for building the technique ladder.

## 6. Statistics and save state

Counting rules in `data/Stats.kt` as **pure functions** — in the ViewModel they would be
untested, because the project has no Robolectric.

- `started` on the first move, not on generation: merely flipping through the levels
  should not inflate the count.
- `solved` exactly once. This required a cleanup first: "solved" used to be computed
  independently in **two** places (in the ViewModel and in `SudokuGame`). Now
  `SudokuGame.isSolved()` is the single definition, and
  `SudokuViewModel.onBoardChanged()` the single transition point — further guarded by
  `countedSolved`, so that solve → undo → redo doesn't count twice.
- The *no aids* badge: all four toggles were off for the **entire game**. `aidsCleanRun`
  is irrevocably cleared the moment any aid was on — flipping it back off just before the
  last cell earns nothing.

`data/GameSnapshot.kt` stores **givens + edit list**, not a copy of the state. That means
digits, notes and undo stack come back in one step and can never drift apart. `decode()`
is nullable and validated (solution valid, givens match it); on any inconsistency, the
save is discarded rather than loaded half-broken. It's read **synchronously in the
ViewModel constructor**, for the same reason as the settings — otherwise a new puzzle
would briefly flash on startup.

An open branch is a single number in this record (the marker), the mistake counter for
the warning (§7) a second one; that's format version 3. Versions 1 and 2 are still read
and get "no branch" and "no mistakes yet" respectively — otherwise anyone mid-puzzle
during an update would lose it over a field that didn't exist back then.

`onBoardChanged()` is the single funnel for board changes and owns three things that must
not be scattered: the hint expires (one computed against an older board isn't just stale,
it can be wrong), the win is counted, the game is saved.

### Play history and the rating (1.1.0)

The counters cannot answer "am I getting better", so each finished game is also one
`GameRecord` line in `History` (`data/History.kt`, pure functions, `HistoryTest`). The
game's *score* is the `Grader` sum of technique costs -- the amount of reasoning in the
puzzle -- computed once at the end on a background dispatcher (`recordEnd`), so nothing
extra is stored with the running game. `recordedEnd` guards it like `countedSolved`
guards the win; on restore it is derived from the snapshot (`counted` or lost).

Per-game rating = `1000 + 400 * log2(score/minute / 8)`: doubling the pace is always
+400, and ~8 points/minute is a comfortable pace at *every* level, which is why no
per-level factor is needed. Discarded alternative: fixed reference times per level --
simpler, but hand-picked constants that would have to be re-tuned with every change to
the digger. Hints x0.8 each, mistakes x0.9 each, aids x0.9; lost = 200; abandoned = 400,
but only after two minutes, or abandoning a bad start would be free. The overall rating
is an exponential moving average (alpha 0.15), provisional below ten games.

Not done: a shared daily puzzle for comparing with others (needs a seedable generator),
and a global leaderboard -- the Play build has no INTERNET permission by promise.

---

## 7. Aids that can be switched off

Four displays take work off the player's hands, and each can be switched off individually
(`data/Settings.kt`): show conflicts, highlight the same digit, highlight row/column/box,
grey out finished digits on the digit pad.

The first is the real one: it tells you instantly whether a digit is even possible in a
cell, doing half the thinking for you. Off means the app stays quiet.

### The fifth aid is of a different kind

`warnOnWrong` compares every entry against the stored solution and immediately says when
it deviates. The other four talk about the *rules* — what's on the board, the player could
verify themselves. This one reads the *answer*. Everything else follows from that:

- **It's the only one off by default.** An aid that solves along with you is not something
  you hand to someone who hasn't asked for it.
- **It costs something.** Three wrong entries end the game (`game/MistakeTally.kt`,
  `LIMIT = 3`). Without a cost, it wouldn't be a compromise but a solver with extra steps:
  you'd tap through until it stays green. The hint text in the settings dialog therefore
  states the cost, and the warning counts down the remaining attempts out loud — the third
  one must not be a surprise.
- **It counts toward `allAidsOff`**, so it forfeits the "no aids" badge like any other.

**It's counted in `onDigit`, not in `onBoardChanged`.** Undo, redo and discarding a branch
also pass through that funnel — but replaying a move back is not the same as making it.
For the same reason, the counter only goes up: if an attempt could be bought back via
undo, the cost wouldn't be one. Undo and redo are therefore dead on a lost board, but
still not on a solved one.

**No check happens inside a branch.** A branch (§4) is explicitly an assumption that is
allowed to be wrong — that's its whole purpose. Deducting an attempt for it would put the
two features at odds with each other.

**One `finished` instead of two flags.** The clock, digit pad, hint, pause and branch bar
no longer ask `solved`, but `GameUiState.finished` (`solved || lost`). Two flags in six
places is exactly the setup where one gets forgotten and the digit pad keeps working on a
lost board.

The counter lives in the save state (`GameSnapshot`, format version 3). Otherwise three
fresh attempts would be just an app restart away. Version 2 is still read and gets "no
mistakes yet" — anyone mid-puzzle during an update should get to keep it.

**There is exactly one gate.** Conflicts are always computed — solved detection needs
them — but `SudokuViewModel.publish()` hands the board a uniformly empty array whenever
the display is off; the board never learns the difference. Note conflicts go through the
same gate and, when the display is off, are not even computed: unlike the ones for
entries, they're not good for anything else. Making this decision in the ViewModel rather
than in the drawing code means there's exactly one place where the app could give away the
solution, instead of one per render pass.

One side effect had to be handled separately: with conflict marking off, a grid that's
full but wrong would otherwise get **no feedback at all**, which feels like a bug in the
app. `fullButWrong` then shows a line saying *that* something is wrong, without saying
*where* — exactly the boundary this setting is about.

Saved via `SudomniaPrefs` in SharedPreferences, read **synchronously in the ViewModel
constructor**: the very first frame already shows your own settings. A DataStore flow
would draw one frame with the defaults and then correct itself — visible as a flicker
right at the switch someone just flipped. The `settings_version` field exists so a
default that changes later can be told apart from a value the user deliberately set.

---

## 8. Icon

`tools/generate_app_icon.py` generates the background, foreground and monochrome layers
from one source. The mark is a 3×3 block — the box structure by which a Sudoku is
recognised; the full 9×9 grid would be grey mush at launcher size.

The three cells on the diagonal are filled in. That's not an arbitrary pattern: boxes 1,
5 and 9 are the only three that share no unit with each other — which is exactly why
`GridGenerator` fills them first with three independent random permutations. The icon
shows the one structural fact the generator is built on.

Launcher masks expose the 72dp circle around the centre of the 108dp grid, so a square
mark can be at most 72/√2 = 50.9dp wide. The block is 50dp. The colour of the empty cells
was **chosen as rendered at 48dp**, not judged at full size: one step above the background
dissolves at small sizes, two steps above and the empty cells start competing with the
filled ones, and the diagonal is lost.

---

## 9. UI

One screen, no navigation graph; the level picker is a dialog.

### Input has no mode

The first draft had a digit pad plus a "notes" toggle. That forces the order *decide →
cell → digit*. Players think the other way around: they look at a cell and only then know
whether they have the answer or want to collect candidates. And the most expensive case
was the most common one — noting three candidates meant toggling, three taps, toggling
back, and anyone who forgot to toggle back entered a note instead of a digit on the next
cell.

Now **two rows sit permanently** under the board: the large digits on top, the flat note
buttons below. What a tap means is now decided by *which* button is hit, not by a
previously set state. Three notes are three taps.

Three details hang off this:

- **The note buttons are stateful.** A filled button means "this note is set in the
  selected cell." The row is therefore also the readout of the candidate state, and
  removing a note again is the same tap as setting it.
- **Both rows are visibly dead while no editable cell is selected.** Before, a tap into
  empty space simply did nothing — exactly the feedback the new order doesn't otherwise
  convey. The note row additionally goes dark as soon as a cell holds a digit:
  `SudokuGame.toggleNote` ignores that case anyway, and silently swallowed taps are worse
  than grey buttons.
- **The digit in the selected cell is shown as a pressed button.** Tapping it again clears
  the cell — that toggle already lives in `SudokuGame.setDigit`; the highlight only makes
  it visible.

`SudokuGame` stayed unchanged for this: `setDigit` / `toggleNote` / `clearCell` were
always cell-scoped. What's gone is only the mode in the ViewModel — the only state that
ever existed for it.

- **The grid is a single `Canvas`**, not 81 composables. Drawing and tap handling both go
  through the same `BoardGeometry` — derived separately, they'd drift, and taps would land
  a cell off at the edge.
- Text via `nativeCanvas` with reused `Paint` objects instead of `TextMeasurer`: up to 81
  digits plus 9 notes per cell, per frame.
- **`BoardState` writes `equals`/`hashCode` by hand.** As a data class, the `IntArray`
  fields would be compared by identity, Compose would skip the redraw, and the board would
  stand still. This exact bug happened to Chessomnia once.
- **The colouring of the highlights is measured, not guessed.** The tint for row/column/box
  covers 21 cells, the same-digit highlight at most nine — and the latter is what you're
  actually looking for. With the original pair (cross `#E2EDF4`, green `#CFE6B8`), the
  board read as "one big blue cross" and the matching digits disappeared into it.
  Determined by rebuilding the drawing code and rendering real positions, not by judging
  the source. Now: the cross is weaker (`#EDF3F8`), the green stronger (`#A9D98A`).
- **Provisional digits are marked twice**: a yellow cell *and* a dark-yellow digit. The
  cell colour alone isn't enough — a branch cell that happens to be selected or carries
  the highlighted digit is drawn in *that* colour instead, and "this is only a trial" must
  not disappear underneath it. In the priority order of cell colours, yellow sits above
  the same-digit highlight, by the same rule as there: whoever colours fewer cells wins.
- **The highlighted digit is also highlighted in the notes** (bold, dark green). Without
  this, the entered digits light up, but the *noted* ones — usually exactly the ones being
  thought about — have to be hunted for by eye.
- **The timer has its own `StateFlow`.** If it lived in the board state, the 81-cell
  canvas would redraw twice a second.
- **Whether the clock runs is decided by exactly one function** (`SudokuViewModel.syncTimer`):
  a game is loaded, it's not solved, the app is visible, the player hasn't paused. Every
  caller changes one of those facts and asks again. Scattered `startTimer()` calls were
  exactly the bug that had the clock keep running overnight on a dark screen — it was only
  stopped on "new game" and "solved," nobody accounted for lifecycle events.
  `startTimer()` now also bails out if the clock is already running: otherwise
  `startedAt` would move forward while `accumulatedMs` held the old value, and the time in
  between would be lost.
- **Two reasons for a stopped clock, deliberately kept separate.** The player's pause
  hides the board; "app not visible" (`MainActivity.onStart`/`onStop`) only stops the
  clock and resolves itself on its own when you come back — a tap-to-acknowledge for
  every answered notification would be a toll. `onStop` also saves: in the background the
  process can die, and otherwise only the time up to the last board change would survive.
- **Pause really hides the board**, rather than leaving it behind a semi-transparent veil:
  a stopped clock in front of a readable grid is free thinking time, and thinking is the
  whole game. The digit pad is hidden too — the note row is the candidate readout of the
  selected cell.
- The board's side length is computed as `min(maxWidth, maxHeight)`, **not** as
  `fillMaxHeight().aspectRatio(1f)` — the latter derives width from height and will
  happily produce a board wider than the screen.
- The elapsed time is *derived* from `SystemClock.elapsedRealtime()`, not counted up per
  tick — so a late tick can't shift the display.

---

## 10. How correctness is ensured

Only JVM unit tests, no Robolectric, no instrumented tests. `rules/` and `game/` have no
Android imports.

1. **Published puzzles with known solutions** (`ReferencePuzzles`): Project Euler 96 no.
   1, a 17-clue puzzle, AI Escargot. The solutions come from an independent
   Norvig-style solver, not from this code — this is the one place where correctness
   doesn't depend on this implementation itself.
2. **A mathematical invariant:** if two digits are entirely missing from the givens, they
   are interchangeable in every solution, so there are at least two solutions. A solver
   that reports "unique" here is broken — and this exact bug would ship unsolvable
   puzzles.
3. **Every generated puzzle has exactly one solution**, across all levels.
4. **Minimality:** no further given can be removed from a Hard puzzle without losing
   uniqueness.
5. **Undo restores digits *and* notes exactly**, across random move sequences.
6. **The centre of every cell finds its way back to that cell** — drawing and tapping
   agree.
7. **A hint never names the wrong digit** — checked across many puzzles and many save
   states. If that ever broke, the app would enter a guaranteed-wrong number at the tap
   of a button.
8. **Whatever is forced is justified**, instead of revealed blank.
9. **A wrong entry is recognised as dead** before anything is given away.
10. **A save state survives encoding and playback** including notes and undo depth;
    corrupted data returns `null` instead of throwing on startup. This also holds for an
    open branch — and a save in the old format keeps loading.
11. **A discarded branch restores digits *and* notes exactly**, across random move
    sequences — the same check as for undo, because it's the same mechanism.

`-DsudokuDeep=1` runs the same tests with ten times as many puzzles: ~2,500 generated
puzzles, on a Raspberry Pi 5 in under 90 seconds including compilation. Generation is
therefore clearly fast enough to run on-device — a precomputation on a PC is only needed
for calibrating the real grader and for the shipped puzzle package.

**Known gap:** without the DLX second oracle, the generator test checks uniqueness with
the same solver that produced it — partially circular. Cushioned by the reference puzzles
and the invariant from point 2; samples were additionally checked against an independent
Python implementation. The full cross-check against Dancing Links is still outstanding.

---

## 11. Update

The app fetches new versions itself from the EnergyControl server at home. On startup and
every 15 minutes after that, `update/UpdateViewModel`.

### Why mTLS and not the simple way

The sister projects (Oystra, MyMoney) load their APK over `http://…:8082`. That doesn't
work here: the update is supposed to work **from outside the house too, without a VPN**,
and from outside exactly one port is open on the router — 8443, the mTLS connector. So the
app needs a client certificate, and there's nowhere for it to fetch one (there's no login
and no pairing like in MyMoney): it's baked into the APK,
`res/raw/sudomnia_client.p12`, issued once by the server's MiniCa.

**A key pair shipped inside the APK is extractable** — the rest of the design follows from
that. It deliberately does *not* live in MyMoney's `device` table, because an entry there
would be full access to the MyMoney REST API with all financial data. Instead, a dedicated
filter on the server (`StaticCertAuthFilter`) only checks `/api/v1/sudomnia/*` against a
list of allowed serial numbers. The key thus opens exactly one thing: the download of this
APK. Losing it costs one line in a text file and a restart.

### Three things that are not negotiable

- **Hostname, not IP.** Jetty checks SNI against the server certificate, whose SAN names
  only the one configured hostname (`sudomnia.updateHost` in `local.properties`, not in
  the repo -- see RELEASING.md). An IP address gets HTTP 400 before any filter or servlet
  even runs. The name resolves both inside and outside the house.
- **The trust anchor is the pinned server certificate** (`res/raw/server_cert.pem`), not
  the system trust store: the CA is private, Android doesn't know it. Side effect: a
  compromised public CA cannot impersonate the server.
- **The SHA-256 from `latest.json` is checked.** Oystra and MyMoney write the hash and
  never look at it again; that was acceptable on the LAN, not over the open internet. On a
  mismatch, the file is deleted, so the package installer is never handed a half download.

### An empty PKCS12 password is not a password on Android

The first attempt stored the client certificate with an empty password — that's how
MyMoney does it, and it works on the JVM. On the tablet, the update button failed with
`IllegalArgumentException: password empty`. Reason: since 8u301, the JDK writes PKCS12
with PBES2/PBKDF2, and Android's BouncyCastle rejects a zero-length password in PBKDF2.
MyMoney doesn't hit this because its P12 is created *on the device*, with the old PKCS12
scheme.

Two consequences, both in `SudomniaClientCertTool`: the password isn't empty (`sudomnia`
— it ships in every APK and protects nothing, access control is the serial-number list on
the server), and the file is deliberately written with the *old* PKCS12 scheme
(3DES/RC2-40/HmacSHA1), because this can't be tested on a device here and the old scheme
is readable by every Android version. Cryptographically that costs nothing, because the
file is public anyway.

### The key does not live in the repo

`sudomnia_client.p12` is git-ignored. A fresh clone therefore does not compile --
deliberately: the reference stays an ordinary `R.raw.sudomnia_client`, so a missing key
shows up while building, in an obvious place, rather than on some tablet. The alternative
would have been a runtime lookup; that would have traded compile-time safety for
convenience for forks that can't use the update feature anyway -- the server behind it
only exists here. The pinned server certificate is public and stays in the repo.

### State, not text

`UpdateState` is a sealed interface, not a rendered sentence plus a busy flag. Oystra had
the latter and, for five releases (1.0.53–1.0.57), reported updates nobody could install
— a button can't be derived from a string. The one question the UI asks is
`installableVersion`.

Two rules that came out of operating this, both commented in the code: a failed
**background** check does not overwrite an already-found result (the tablet regularly
loses Wi-Fi, and the button must not vanish under someone's finger), and before the
package installer launches, the state goes back to "available" — if that gets cancelled,
the button is there again.

`ReleaseInfo.parse` is the one testable part of the whole thing and deliberately carved
out for it: a pure function, `org.json`, no Android type. What's tested isn't the happy
path but that an incomplete manifest, or none at all, returns `null` — the app must not
act on a document it didn't understand.

### Releasing

`deploy.sh` in the repo root: bump the version in `version.properties`, build a signed
release, put the APK in `/var/lib/sudomnia/apk/`, write `latest.json`. The order (commit
and push first, then build) is carried over from Oystra and has a concrete reason there:
that way every shipped APK corresponds to a commit that also exists on the remote. As long
as Sudomnia isn't a git repo, the script skips that step.

---

## 12. When something goes wrong: `diag/`

A tablet in the living room has no logcat. "The update button says
IllegalArgumentException" is worthless as a bug report — that's exactly what cost the
first update attempt a whole day.

`DiagnosticsLog` is therefore a text file in `filesDir` plus a button in the aids dialog
that hands it to the share sheet. Crashes land in it automatically via an
`UncaughtExceptionHandler`; the previous handler is **chained, not replaced** — swallowing
it would leave the app hanging instead of dying, and that's worse than the crash.

**There is deliberately no upload path in this class.** A crash reporter would be a
network dependency and a privacy story for a single-player Sudoku; here the player sees
every time what leaves the device, and chooses the destination themselves.

`EXTRA_EMAIL` suggests `sudomnia@lechners.name` as the recipient, regardless of which
mail app is chosen from the share sheet — only a suggestion that the receiving app may
pre-fill, not a fixed destination address. Any other kind of app from the chooser simply
ignores the field.

The file is a ring buffer: past 64 KB, the older half is discarded. And `log()` catches
its own exceptions — diagnostics must never be what breaks the app.
