# Screenshots

Taken on a 10" tablet (1840 × 2944, ~340 dpi, Lenovo TB370FU) with
`adb exec-out screencap`. Nothing here is rendered from the vector sources:
Google requires screenshots of the real app, and a mockup would misrepresent it.

## Tablet (`tablet/`)

Language switch for the app only, not the device, via
`cmd locale set-app-locales name.lechners.sudomnia --user 0 --locales <de|en|es>`
(Android 13+, no reboot, no device language switch needed).

| File | Shows |
|---|---|
| `01-notes-conflict-de.png` | Notes in a cell, one marked red — a conflict with a digit already set in the row/column/box |
| `02-hint-reasoning-de.png` | A hint with the reasoning spelled out ("Verstecktes Single — in Zeile 7 ist nur hier noch Platz für die 2") — **the one image that sets this app apart from others** |
| `03-branch-en.png` / `-de.png` / `-es.png` | An open branch: the provisional digit (yellow/gold), the bar with discard/commit — in all three languages |
| `04-aids-en.png` / `-de.png` / `-es.png` | The aids dialog, each switch individually toggleable — in all three languages |
| `05-stats-es.png` | The statistics with the four levels, real values from previous games |

**Aspect ratio:** all captures are 1840 × 2944 (ratio 1.6:1), which stays under
Play's 2:1 limit — no cropping needed.

## Phone (`phone/`)

Taken on a Pixel 9 Pro (960 × 2142 at the WM-reported size, ~360 dpi) over wireless
ADB, in all three languages via the same `cmd locale set-app-locales` switch as the
tablet set. The raw capture is 2.23:1 and fails Play's 2:1 limit, so the status bar
and the gesture-nav strip are cropped off (they're OS chrome, not app content
anyway): 960 × 1890, ratio 1.97:1.

| File | Shows |
|---|---|
| `01-notes-conflict-en.png` / `-de.png` / `-es.png` | Same situation as the tablet set: notes in a cell, one in red for a conflict with the row/column/box — in all three languages |
| `02-hint-reasoning-en.png` / `-de.png` / `-es.png` | A hint with the reasoning spelled out (a hidden single this time) — in all three languages |
| `03-aids-en.png` / `-de.png` / `-es.png` | The aids dialog, also showing the app version in the same shot — in all three languages |
| `04-stats-en.png` / `-de.png` / `-es.png` | The statistics screen with real values from previous games — in all three languages |

Capturing the Spanish set is what found the TopBar overflow bug fixed in
`ui/game/TopBar.kt` (a plain `Row` let "Nueva partida" run off the right edge on
phone width instead of wrapping) — these screenshots are from the build that
already has the fix, not the one that shipped the bug.

## What's still missing

- **7" tablet set.** Play distinguishes 7" from 10"; everything above is 10"-class.

## What Play requires

| Form factor | Count | Size |
|---|---|---|
| Phone | 2–8 | min. 320 px short edge |
| 7" tablet | up to 8 | min. 1080 px long edge |
| 10" tablet | up to 8 | min. 1080 px long edge |

**Aspect ratio:** the long edge may be at most twice as long as the short edge. A
raw image from a modern phone is often around 2.23:1 and fails this — crop it in
that case.

## Capturing

On a device with ADB:

```bash
adb exec-out screencap -p > store/screenshots/phone-1-grid.png
```

The header area shows the level, the clue count and the clock — before capturing
it's worth checking whether it shows something you don't want to publish (a clock
reading 4:13:24 tells its own story).
