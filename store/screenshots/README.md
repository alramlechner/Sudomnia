# Screenshots

Aufgenommen auf einem 10"-Tablet (1840 × 2944, ~340 dpi, Lenovo TB370FU) mit
`adb exec-out screencap`. Nichts hier ist aus den Vektorquellen gerendert:
Google verlangt Screenshots der echten App, und ein Modell würde sie falsch
darstellen.

## Tablet (`tablet/`)

Sprachwechsel nur der App, nicht des Geräts, über
`cmd locale set-app-locales name.lechners.sudomnia --user 0 --locales <de|en|es>`
(Android 13+, kein Reboot, kein Geräte-Sprachwechsel nötig).

| Datei | Zeigt |
|---|---|
| `01-notes-conflict-de.png` | Notizen in einer Zelle, eine davon rot markiert — Konflikt mit einer bereits gesetzten Ziffer in Zeile/Spalte/Block |
| `02-hint-reasoning-de.png` | Ein Tipp mit ausgeschriebener Begründung ("Verstecktes Single — in Zeile 7 ist nur hier noch Platz für die 2") — **das Bild, das diese App von anderen unterscheidet** |
| `03-branch-en.png` / `-de.png` / `-es.png` | Ein offener Zweig: die provisorische Ziffer (gelb/gold), die Leiste mit Verwerfen/Übernehmen — in allen drei Sprachen |
| `04-aids-en.png` / `-de.png` / `-es.png` | Der Hilfen-Dialog, jeder Schalter einzeln abschaltbar — in allen drei Sprachen |
| `05-stats-es.png` | Die Statistik mit den vier Stufen, echte Werte aus vorherigen Partien |

**Seitenverhältnis:** alle Aufnahmen sind 1840 × 2944 (Verhältnis 1,6:1),
damit unter Plays Grenze von 2:1 — kein Zuschnitt nötig.

## Was noch fehlt

- **Handy-Screenshots.** Play verlangt 2–8 Stück, mind. 320 px kurze Kante;
  keiner der Tablet-Screenshots erfüllt das. Auf einem Handy-Gerät nachholen.
- **7"-Tablet-Satz.** Play unterscheidet 7" von 10"; alles oben ist 10"-Klasse.

## Was Play verlangt

| Formfaktor | Anzahl | Größe |
|---|---|---|
| Handy | 2–8 | mind. 320 px kurze Kante |
| Tablet 7" | bis 8 | mind. 1080 px lange Kante |
| Tablet 10" | bis 8 | mind. 1080 px lange Kante |

**Seitenverhältnis:** die lange Kante darf höchstens doppelt so lang sein wie die
kurze. Ein Rohbild moderner Handys liegt oft bei 2,23:1 und fällt damit durch —
dann zuschneiden.

## Aufnehmen

Auf einem Gerät mit ADB:

```bash
adb exec-out screencap -p > store/screenshots/phone-1-grid.png
```

Der Kopfbereich zeigt die Stufe, die Zahl der Vorgaben und die Uhr — vor der
Aufnahme lohnt ein Blick darauf, ob dort etwas steht, das man nicht
veröffentlichen will (eine Uhr bei 4:13:24 erzählt eine eigene Geschichte).
