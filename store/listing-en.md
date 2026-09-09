# Google Play listing — English

## App name (max 30 characters)

```
Sudomnia
```

## Short description (max 80 characters)

```
Sudoku that never needs guessing. No ads, no account, not one permission.
```

## Full description (max 4000 characters)

```
Sudomnia is a Sudoku for people who want to solve the puzzle, not fight the app.

Every puzzle is generated on your device and checked before you ever see it: it has exactly one solution, and that solution can be reached by reasoning alone.


NO PUZZLE EVER NEEDS GUESSING

This is the promise the whole app is built on, and it is checked, not claimed. Before a puzzle is handed to you, Sudomnia solves it the way a person would — hidden and naked singles, locked candidates, naked and hidden pairs and triples, X-Wing, Swordfish, simple colouring, XY-Wing — and never by trying a digit to see what happens. If the puzzle cannot be finished that way, it is not published to you.

Most generators do not do this. They check that the solution is unique and stop there. We measured our own earlier version: of the puzzles it called "hard", 53 % could not be finished by any of those techniques. If you got stuck in one, there was no way to tell whether you were missing something or whether there was nothing to see.


FOUR LEVELS THAT MEAN SOMETHING

The level is not a guess about how many clues are left. It is the hardest technique the puzzle actually requires, measured on that exact grid:

• Easy — singles only, and at least 36 clues stay on the board
• Medium — singles only, but dug as deep as uniqueness allows
• Hard — locked candidates or a pair or triple
• Expert — an X-Wing, colouring or an XY-Wing

Clue count alone would tell you nothing: maximally dug easy puzzles and puzzles needing real techniques both bottom out at about 24 clues.


HINTS THAT EXPLAIN THEMSELVES

The hint does not reveal a digit and leave you none the wiser. It names the technique and the reason: "Locked candidates: in box 5 the 7 fits only in cells that also lie in column 3, so it can go from the rest of column 3." Candidates it strikes out are shown crossed out in the grid. Where several deductions are needed before a digit can be written, it walks you through them one at a time.

The last press enters the digit — but by then you know why it belongs there.


FREE. NO ADS. NO ACCOUNT. NO TRACKING.

None of these has an asterisk.

The app holds no Android permission that lets it do anything — not even internet access. It cannot send anything anywhere, because it has nothing to send with. Your games never leave the device. Verifiable: Sudomnia is open source.

There is no server side. Puzzles are made on your device, in a few milliseconds each.


THE AIDS ARE YOURS TO SWITCH OFF

Conflict marking, highlighting the same digit, highlighting row, column and box, dimming finished digits — each one separately. The first is the important one: it tells you instantly whether a digit is possible at all, which is half the thinking. Switch it off and the app stays quiet.

A game solved with every aid off earns a badge in the statistics. Turning them off just before the last cell earns nothing.


TRY A BRANCH INSTEAD OF GUESSING

When you want to assume a digit and follow it, "Start branch" marks that point. Everything entered afterwards is provisional and shown in yellow. If it works out, keep it; if it does not, discard the whole attempt in one step — including the pencil marks it cleared along the way.


BUILT FOR SITTING WITH IT

Two permanent rows under the grid: digits above, pencil marks below. No mode to switch — three candidates are three taps, and the note row doubles as a readout of the selected cell.

Pause stops the clock and hides the grid. The clock also stops on its own when the app is not on screen. The running game survives closing the app: digits, pencil marks, the undo stack, an open branch and the time.


OPEN SOURCE

Apache-2.0. Read it, build it, fork it:
https://github.com/alramlechner/Sudomnia
```

## Categorisation

| Field | Value |
|---|---|
| App or game | Game |
| Category | Puzzle |
| Tags | Sudoku, Puzzle, Logic |
| Contains ads | No |
| In-app purchases | No |
| Target audience | 13+ (avoids the additional Families-programme requirements; the app has no content concerns at any age) |
| Content rating | Complete the questionnaire — no violence, no user interaction, no data collection, no purchases |

## Data safety declaration

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | n/a — nothing is transmitted |
| Do you provide a way for users to request that their data is deleted? | n/a — nothing is collected |

Supporting evidence, should a reviewer ask: the release manifest of the `play`
flavour requests no permission that grants the app any capability — the single
`uses-permission` line is `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a
signature-level permission AndroidX declares for the app about itself. There is
no `INTERNET` permission, and the app contains no analytics, advertising or
crash-reporting SDK. The self-update that the `selfhosted` flavour has is not
part of this build at all — see RELEASING.md, which verifies both facts against
the uploaded bundle.

## Privacy policy URL

```
https://alramlechner.github.io/Sudomnia/privacy.html
```

## Assets

| Asset | File | Status |
|---|---|---|
| App icon, 512×512 PNG | `store/play-icon-512.png` | ready |
| Feature graphic, 1024×500 PNG | `store/play-feature-1024x500-en.png` | ready |
| Phone screenshots (2–8, min 320px) | — | **must be taken on a device** |
| 7" tablet screenshots (up to 8) | — | **must be taken on a device** |
| 10" tablet screenshots (up to 8) | — | **must be taken on a device** |

Suggested set, in this order:

1. A grid mid-game with pencil marks, one digit highlighted.
2. A hint showing its reasoning, with the struck-out candidates visible.
3. An open branch: the yellow cells and the bar with Discard / Keep.
4. The aids dialog, showing that every one of them can be switched off.
5. The statistics with the four levels.
