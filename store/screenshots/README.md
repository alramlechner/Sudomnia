# Screenshots

**Noch keine.** Sie fehlen als Einzige unter den Play-Assets, und sie sind das
Einzige, was hier nicht erzeugt werden kann: Google verlangt Screenshots der
echten App, und ein aus den Vektorquellen gerendertes Modell würde sie falsch
darstellen. Sie müssen auf einem Gerät aufgenommen werden.

## Was Play verlangt

| Formfaktor | Anzahl | Größe |
|---|---|---|
| Handy | 2–8 | mind. 320 px kurze Kante |
| Tablet 7" | bis 8 | mind. 1080 px lange Kante |
| Tablet 10" | bis 8 | mind. 1080 px lange Kante |

**Seitenverhältnis:** die lange Kante darf höchstens doppelt so lang sein wie die
kurze. Ein Rohbild moderner Handys liegt oft bei 2,23:1 und fällt damit durch —
dann zuschneiden.

## Vorgeschlagene Reihenfolge

1. Ein Gitter mitten im Spiel, mit Notizen, eine Ziffer hervorgehoben.
2. Ein Tipp mit seiner Begründung, die gestrichenen Kandidaten sichtbar
   durchgestrichen. **Das ist das Bild, das diese App von anderen unterscheidet** —
   es sollte an zweiter Stelle stehen, nicht weiter hinten.
3. Ein offener Zweig: die gelben Felder und die Leiste mit Verwerfen / Übernehmen.
4. Der Hilfen-Dialog, der zeigt, dass sich jede Anzeige abschalten lässt.
5. Die Statistik mit den vier Stufen.

## Aufnehmen

Auf einem Gerät mit ADB:

```bash
adb exec-out screencap -p > store/screenshots/phone-1-grid.png
```

Der Kopfbereich zeigt die Stufe, die Zahl der Vorgaben und die Uhr — vor der
Aufnahme lohnt ein Blick darauf, ob dort etwas steht, das man nicht
veröffentlichen will (eine Uhr bei 4:13:24 erzählt eine eigene Geschichte).
