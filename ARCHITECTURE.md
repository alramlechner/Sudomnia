# Sudomnia — Architektur

Stand 0.4.3 (Prototyp). Schwesterprojekt zu [Chessomnia](https://github.com/alramlechner/Chessomnia);
Build-Setup, Paketschichtung und Testphilosophie sind von dort übernommen.

---

## 1. Schichten

```
rules/   reines Kotlin, keine Android-Importe   -> auf der JVM testbar
  |
game/    laufende Partie (Eingaben, Notizen, Undo)
  |
data/    Einstellungen, Statistik, Spielstand (SharedPreferences)
  |
ui/      Compose, ein Screen, ein ViewModel

update/  Update-Prüfung  -- hängt an keiner der anderen Schichten
diag/    Fehlerprotokoll -- von überall beschreibbar, hängt an nichts
```

Ein Gradle-Modul, Namespace `name.lechners.sudomnia`, minSdk 30 / targetSdk 36, JDK 17.
Abhängigkeiten: core-ktx, Compose BOM, activity-compose, lifecycle-viewmodel-compose, junit.
Kein Room, kein DI-Framework, keine HTTP-Bibliothek.

**Zwei Varianten, und der Unterschied ist nicht ein Schalter.** `play` ist die Fassung
für Google Play: **keine einzige Berechtigung**, kein Netzwerkcode, kein Zertifikat.
`selfhosted` ist die Fassung für die Geräte im Haus und enthält die
Selbst-Aktualisierung mit `INTERNET` und `REQUEST_INSTALL_PACKAGES` (§11). Google
verbietet Apps aus dem Store, sich auf einem anderen Weg selbst zu aktualisieren — der
Play-Build enthält den Code deshalb nicht bloß deaktiviert, sondern **gar nicht**.
Genau das war `update/` immer schon zugedacht: ein eigenes Paket ohne Verbindung zu
`rules/`, `game/` oder `data/`.

Getrennt wird über Gradle-Quellverzeichnisse, nicht über ein `if`:

```
src/main/…/update/UpdateState.kt        reine Daten, die die Oberfläche zeichnet
src/main/…/update/UpdateController.kt   was die Oberfläche braucht -- drei Mitglieder
src/play/…/update/UpdateSupport.kt      liefert null. Das ist die ganze Datei.
src/selfhosted/…/update/UpdateSupport.kt + UpdateClient/UpdateViewModel/ReleaseInfo
```

`UpdateSupport` gibt es genau einmal pro Variante und nirgends in `main` — es ist die
einzige Stelle, die weiß, welche der beiden gebaut wird. Der Nebeneffekt ist genauso
wichtig wie die Store-Regel: **ein frischer Clone übersetzt `play` ohne jedes
Geheimnis.** Vorher scheiterte er an `R.raw.sudomnia_client`, und das ist für ein
quelloffenes Projekt keine Kleinigkeit, sondern die Eintrittsschwelle.

---

## 2. Wie ein Rätsel entsteht

Zufällig Zahlen hinsetzen funktioniert nicht — das ergibt fast immer ein unlösbares
oder ein mehrdeutiges Gitter. Stattdessen wird **weggegraben**:

1. `GridGenerator` erzeugt ein vollständiges gültiges Gitter. Beschleunigung: die
   Boxen 1, 5 und 9 teilen keine Zeile, Spalte oder Box, lassen sich also vorab als
   drei unabhängige Zufallspermutationen von 1–9 füllen. Danach backtrackt die Suche
   praktisch nicht mehr. ~50–200 µs.
2. `Digger` leert Felder in zufälliger Reihenfolge und nimmt jede Entfernung zurück,
   die die Eindeutigkeit zerstört. **Eindeutigkeit ist damit konstruktiv garantiert,
   nicht nachträglich geprüft.**
3. `PuzzleFactory` setzt beides zusammen und liefert ein `Puzzle`.

### Eindeutigkeitsprüfung

`Solver` ist Constraint-Propagation (Naked + Hidden Singles) plus MRV-Backtracking,
allokationsfrei: Kandidaten sind 9-Bit-Masken in `IntArray`s, Backtracking stellt
einen vorab angelegten Snapshot wieder her statt einen Änderungs-Trail abzuspielen.

Die eine Operation, auf der alles aufbaut:

```kotlin
fun countSolutions(givens: IntArray, limit: Int = 2, out: IntArray? = null): Int
```

`limit = 2` beantwortet „ist die Lösung eindeutig?".

Der Digger fragt allerdings nicht so, sondern: *„gibt es eine Lösung mit einer anderen
Ziffer in diesem Feld?"* — für jeden Kandidaten ein Lauf mit `limit = 1`. Fast alle
enden nach wenigen Propagationsschritten im Widerspruch, weil das Restgitter massiv
überbestimmt ist. Grob Faktor 3 schneller als volles Zählen.

**Warum Bitmasken und nicht `Set<Int>`:** ein Rätsel kostet ~80 Solver-Aufrufe; mit
einem `HashSet` pro Feld wären das sechsstellige Allokationszahlen pro Rätsel.
`Integer.bitCount` und `numberOfTrailingZeros` sind unter ART einzelne ARM64-Befehle.

**Warum kein Dancing Links:** DLX ist auf den härtesten Rätseln schneller, braucht aber
~3.240 Knotenobjekte pro Lauf. Bei 80 Läufen pro Rätsel sind das ~260.000 Objekte —
GC-Druck, während daneben eine Compose-UI zeichnet. DLX ist als *unabhängiges
Test-Orakel* vorgesehen, nicht für die Produktion.

---

## 3. Schwierigkeit

Die Anzahl der Vorgaben ist ein schlechter Indikator. Gemessen an diesem Generator
landen maximal ausgegrabene Singles-Rätsel und Rätsel, die echte Techniken brauchen,
**auf dieselbe Kommastelle** bei ~24,5 Vorgaben. Die Zahl trennt sie überhaupt nicht.

Die Stufe kommt deshalb aus der **Technikleiter**: `HumanSolver` löst das Rätsel so,
wie ein Mensch es täte — nur mit Techniken, die sich in einem Satz erklären lassen,
und **ohne je zu raten**. Die höchste Sprosse, die dabei gebraucht wird, ist die
Stufe (`Grader`).

| Sprosse | Was man sieht |
|---|---|
| Hidden Single | in dieser Einheit ist nur noch ein Platz für die 5 |
| Naked Single | in diesem Feld ist nur noch eine Ziffer möglich |
| Locked Candidates | im Block liegt die 7 nur in einer Zeile → raus aus dem Rest der Zeile |
| Naked Pair/Triple | zwei (drei) Felder teilen sich zwei (drei) Ziffern |
| Hidden Pair/Triple | zwei (drei) Ziffern können nur in zwei (drei) Felder |
| X-Wing, Swordfish | dieselbe Ziffer in denselben zwei (drei) Spalten zweier (dreier) Zeilen |
| Simple Colouring | Zweierketten einer Ziffer, zweifarbig verfolgt |
| XY-Wing | Angelpunkt {x,y} mit zwei Flügeln {x,z} und {y,z} |

### Die vier Bänder

| Stufe | Regel | Ø Vorgaben | Ø Schritte | ms/Rätsel |
|---|---|---|---|---|
| **Leicht** | Singles genügen, ≥ 36 Vorgaben bleiben stehen | 36,0 | 45 | 2,3 |
| **Mittel** | Singles genügen, maximal ausgegraben | 24,8 | 56 | 3,3 |
| **Schwer** | Locked Candidates oder ein Subset nötig | 24,4 | 61 | 13,6 |
| **Experte** | X-Wing, Colouring oder XY-Wing nötig | 24,8 | 63 | 25,5 |

Gemessen über je 100 Rätsel mit `generator-cli --mode grade` auf einem Raspberry Pi 5.
Das Werkzeug ist der Grund, dass hier Zahlen und keine Vermutungen stehen — nach jeder
Änderung an `Digger`, `Technique` oder `HumanSolver` gehört es neu laufen gelassen.

### Was die Leiter wirklich geändert hat

Nicht die Etiketten, sondern die Rätsel. Die alte „Schwer"-Stufe hieß „Singles
genügen nicht" und nahm sonst alles, was eindeutig war. Die Messung über 150 solcher
Rätsel: **53 % waren mit keiner Technik der Leiter lösbar** — sie verlangten
Forcing Chains oder in der Praxis Raten. Der Spieler konnte nicht unterscheiden, ob er
etwas übersieht oder ob es nichts zu sehen gibt.

Der Digger nimmt eine Entfernung jetzt zurück, sobald die Leiter das Rätsel nicht mehr
zu Ende bringt. **Jedes ausgelieferte Rätsel ist ohne Raten lösbar** — das ist die
Zusage, auf der der „Zweig" (§4) als *freiwilliges* Werkzeug überhaupt erst Sinn ergibt.

### Was das kostet

Die Schranke ist zugleich die Abkürzung: der Digger prüft nach jeder Entfernung nur
gegen die *Decke des angepeilten Bandes*. Leicht und Mittel bleiben deshalb beim
schnellen Singles-Orakel (`SinglesSolver` mit `Grid.propagate`, ein Durchlauf statt
Schritt für Schritt), und nur Experte bezahlt die volle Leiter — 25 ms je Rätsel,
mit Erzeugung.

### Verworfen: „Naked Singles" vs. „Hidden Singles" als Stufengrenze

Der erste Entwurf trennte Leicht (nur Naked Singles) von Mittel (Hidden Singles
nötig). Das ist aus menschlicher Sicht **falsch herum**: ein Naked Single („dieses
Feld hat nur noch einen Kandidaten") verlangt, alle 20 Nachbarn zu prüfen und acht
Ziffern auszuschließen. Ein Hidden Single („in diesem Block ist nur noch ein Platz für
die 5") findet man durch Abscannen von drei Linien — es ist die erste Technik, die
jede Anleitung zeigt. Maschinell billig und menschlich billig laufen hier
gegeneinander. Dieselbe Überlegung bestimmt die Reihenfolge in `Technique`.

### Verworfen: Forcing Chains, Nice Loops, ALS

Sie lösen mehr Rätsel — aber die Erklärung für einen solchen Schritt ist ein Absatz,
kein Satz. Ein Tipp, dem niemand folgen kann, ist schlechter als ein ehrliches „hier
ist gerade nichts erzwungen". Rätsel, die sie brauchen, werden nicht ausgeliefert.

Uniqueness-Techniken (Unique Rectangle) fehlen aus einem anderen Grund: sie
argumentieren mit „das Rätsel hat genau eine Lösung" — eine Tatsache über den
Setzenden, nicht über das Gitter. Sie würden den Solver außerdem als *Prüfer* der
Eindeutigkeit unbrauchbar machen, und genau dafür braucht ihn der Generator.

## 4. Die laufende Partie

`SudokuGame` hält Eingaben, Notizen und die Undo-Historie.

**Persistiert wird später der Startzustand plus die Bearbeitungsliste**, nicht eine
Zustandskopie — dasselbe Prinzip wie Chessomnias Zugliste. Deshalb ist eine
Bearbeitung (`Edit`) schon jetzt eine *Liste* von Feldänderungen: das Setzen einer
Ziffer löscht sie zugleich aus den Notizen aller 20 Nachbarn, und Undo muss beides
zurücknehmen.

### Zweige: ein Versuch auf Probe

Auf „Schwer" ist regelmäßig kein Feld mehr erzwungen (§5) — der Weg weiter ist, eine
Ziffer anzunehmen und die Folgen zu verfolgen. `beginBranch()` setzt dazu eine Marke
in die Bearbeitungsliste; alles danach ist vorläufig, bis `commitBranch()` es behält
oder `discardBranch()` den ganzen Versuch zurücknimmt.

**Ein Zweig ist nur diese Marke** — kein zweites Brett, keine Zustandskopie. Verwerfen
ist Undo bis zur Marke und danach ein Abschneiden der Liste. Damit stellt es Notizen
und die bei Nachbarn gestrichenen Kandidaten genauso exakt wieder her wie der
Rückgängig-Knopf, ohne dass dafür eine Zeile geschrieben werden musste; wäre der Zweig
eine Kopie des Bretts, wären es zwei Wahrheiten über denselben Spielstand.

Drei Regeln hängen daran:

- **Undo hält an der Marke.** Sonst stünde der Zweig um Bearbeitungen herum offen, die
  gar nicht mehr zu ihm gehören, und „alles zurück" hätte keinen definierten Umfang.
  Nach dem Übernehmen fällt die Grenze sofort weg.
- **Der Redo-Zweig wird beim *Öffnen* verworfen**, nicht beim Verwerfen. Dann ist
  Verwerfen ein reines Abschneiden auf die Marke und kann keine Bearbeitungen von
  *vor* dem Zweig wiederbeleben.
- **Zweige verschachteln nicht.** Ein zweites `beginBranch()` ist wirkungslos. Ein
  Stapel wäre billig zu haben, aber „welchen Zweig verwerfe ich gerade?" ist eine
  Frage, die die Oberfläche dann beantworten müsste — und der Fall, um den es geht,
  ist eine Annahme und ihre Folgen.

`trialCells()` liefert die Felder, deren **Ziffer** sich seit der Marke geändert hat —
das ist, was gelb wird. Notizen zählen bewusst nicht: eine gesetzte Ziffer streicht
sich aus den Notizen von bis zu 20 Nachbarn, die alle einzufärben verteilte den
Versuch über ein Viertel des Bretts. Eine wieder geleerte Zelle fällt aus der Liste
heraus, weil dann nichts vom Versuch mehr darin steht.

**Ob der Versuch gescheitert ist, sagt der vorhandene Tipp**: er prüft ohnehin als
Erstes, ob das Brett noch lösbar ist (§5), und meldet sonst genau das. Der Zweig
braucht dafür keine eigene Prüfung — und vor allem kein automatisches „das war
falsch", das dem Spieler die Arbeit abnähme, die er gerade tun wollte.

**Konflikte, keine Fehler.** Markiert wird eine Ziffer, die in Zeile, Spalte oder Block
doppelt vorkommt — eine Aussage über die Regeln, die der Spieler selbst treffen könnte.
Ein Abgleich mit der gespeicherten Lösung wäre etwas anderes: die App würde das Rätsel
still mitlösen. Diese Grenze ist Absicht — und sie gilt in `SudokuGame` weiterhin
ausnahmslos. Überschreiten kann sie nur der Spieler selbst, indem er die Warnung vor
falschen Eingaben einschaltet (§7); der Vergleich steht dann im ViewModel, nicht im
Modell.

**Auch Notizen.** `noteConflicts()` markiert einen Bleistift-Kandidaten, dessen Ziffer
in der Nachbarschaft schon gesetzt ist — dieselbe Aussage über dieselbe Regel, nur über
eine Notiz statt über eine Eingabe. Zwei *Notizen* derselben Ziffer in einer Einheit
sind dagegen kein Konflikt: beide dürfen Kandidaten sein, dafür sind Notizen da.

Die Lücke gab es nur, weil das Setzen einer Ziffer sie aus den Notizen aller 20
Nachbarn streicht — ein unmöglicher Kandidat kann also nur entstehen, wenn er
*nachträglich* notiert wird. Genau in dem Moment will man es wissen.

`isSolved()` prüft „voll und konfliktfrei" und konsultiert die Lösung ebenfalls nicht —
bei einem eindeutig lösbaren Rätsel ist das dasselbe.

---

## 5. Tipp

Der Tipp ist **ein Schritt der Technikleiter, genommen vom Brett, wie es dasteht**
(`HumanSolver.nextSteps`) — derselbe Code, der die Stufe misst. Er kann deshalb immer
sagen *warum*, und er sagt nur Dinge, die der Spieler selbst hätte sehen können.

`rules/Hint.kt` beantwortet „was jetzt?" in dieser Reihenfolge:

1. `grid.load(board)` scheitert → **tot** (zwei gleiche Ziffern in einer Einheit)
2. `solver.countSolutions(board, limit = 1) == 0` → **tot**
3. die Kette der Leiter bis zur nächsten setzbaren Ziffer
4. sonst `bestBranchCell()` → blank aufdecken

### Warum eine Kette und nicht ein Schritt

Eine Elimination ändert das Brett nicht. Zeigt man dem Spieler eine einzelne, kommt
beim nächsten Druck dieselbe wieder — für immer. Die Kette läuft deshalb bis zu dem
Schritt, der wirklich eine Ziffer setzt: „streich das weg, dann das, und jetzt ist die
7 erzwungen." Jeder Druck rückt einen Schritt weiter, jeder Schritt hat zwei Stufen
(*wo* — dann *warum*), und am Ende steht „Eintragen".

Gemessen über 2.563 Tipps in allen vier Bändern: **2.514 Ketten sind ein einziger
Schritt**, 49 sind länger, die längste war 20. Die Obergrenze in `nextSteps` ist
entsprechend keine gestaltete Länge, sondern nur eine Abbruchbedingung.

### Was der Tipp nicht tut

**Er fasst die Notizen des Spielers nicht an.** Eine Elimination wird gezeigt und dann
geschlossen; es gibt kein „Eintragen" dafür. Kandidaten wegzustreichen, die der Spieler
nie notiert hat, wäre eine Änderung, die er nicht sehen kann — und die Notizen sind
seine.

**Die Notizen fließen auch nicht ein**, `find()` nimmt sie gar nicht entgegen. Notizen
sind unvollständig, veralten und können falsch sein; ein Tipp, der darauf rechnet, wäre
beweisbar falsch, und der Spieler hätte keine Chance das zu merken — für ihn ist die
App die Autorität. Der Kandidatenstand aus `Grid.load(board)` ist dagegen kanonisch.

**Schritt 2 ist Vorbedingung, keine Zusatzfunktion.** Auf einem toten Brett wäre jede
Herleitung ein Argument innerhalb eines Widerspruchs. Der Test ist exakt, nicht
heuristisch: das Rätsel hat genau eine Lösung, also ist das Brett genau dann tot, wenn
eine Eingabe abweicht. Angezeigt wird nur *dass*, nie *wo* — der Ausweg ist der
vorhandene Rückgängig-Knopf.

### Die blanke Aufdeckung ist übrig geblieben, nicht geblieben nötig

Sie ist für Rätsel aus diesem Generator unerreichbar geworden: jedes ist mit der Leiter
lösbar (§3), und der Test `everyHintIsExplainable` spielt jedes Rätsel jeder Stufe
allein über den Tipp-Knopf durch, ohne je eine Aufdeckung zu sehen. Sie bleibt für zwei
Fälle, die nicht hypothetisch sind: ein Spielstand aus einer älteren Version, dessen
Rätsel ohne diese Zusage ausgegraben wurde, und ein Brett, das der Spieler mit eigenen
korrekten Zügen in eine Stellung gebracht hat, die die Leiter nicht knackt.

### Was das ersetzt hat

Vorher konnte der Tipp genau zwei Dinge begründen — Hidden und Naked Single — und
deckte sonst blank auf. Auf „Schwer" lief **jedes** Rätsel mit Singles allein fest, das
war die Definition der Stufe; der Tipp konnte dort also grundsätzlich nur aufdecken.
Gemessen wurde damals, wie weit eine Aufdeckung trägt (17 weitere erzwungene Felder,
rund zwei Aufdeckungen je Rätsel). Genau diese Zahl war das Argument, die Technikleiter
zu bauen.

## 6. Statistik und Spielstand

Zählregeln in `data/Stats.kt` als **reine Funktionen** — im ViewModel wären sie ungetestet,
weil das Projekt kein Robolectric hat.

- `started` beim ersten Spielzug, nicht bei der Erzeugung: bloßes Durchblättern der Stufen
  soll die Zahl nicht aufblähen.
- `solved` genau einmal. Voraussetzung dafür war eine Aufräumarbeit: „gelöst" wurde vorher
  an **zwei** Stellen unabhängig berechnet (im ViewModel und in `SudokuGame`). Jetzt ist
  `SudokuGame.isSolved()` die einzige Definition, und `SudokuViewModel.onBoardChanged()`
  der einzige Übergangspunkt — zusätzlich abgesichert durch `countedSolved`, damit
  Lösen → Rückgängig → Wiederholen nicht doppelt zählt.
- Abzeichen *ohne Hilfen*: alle vier Schalter waren die **ganze Partie** über aus.
  `aidsCleanRun` wird unwiderruflich gelöscht, sobald eine Hilfe an war — kurz vor dem
  letzten Feld umzuschalten erschleicht nichts.

`data/GameSnapshot.kt` speichert **Vorgaben + Bearbeitungsliste**, nicht eine Zustandskopie.
Damit kommen Ziffern, Notizen und Undo-Stack in einem Schritt zurück und können nicht
auseinanderlaufen. `decode()` ist nullbar und wird validiert (Lösung gültig, Vorgaben passen
dazu); bei jeder Unstimmigkeit wird der Spielstand verworfen statt halb kaputt geladen.
Gelesen wird **synchron im ViewModel-Konstruktor**, aus demselben Grund wie die
Einstellungen — sonst blitzt beim Start kurz ein neues Rätsel auf.

Ein offener Zweig ist eine einzelne Zahl in diesem Datensatz (die Marke), der Fehlerzähler
der Warnung (§7) eine zweite; das ist Format Version 3. Die Versionen 1 und 2 werden
weiterhin gelesen und bekommen „kein Zweig" beziehungsweise „noch keine Fehler" — wer beim
Update mitten im Rätsel steckt, verliert es sonst für ein Feld, das es damals nicht gab.

`onBoardChanged()` ist der einzige Trichter für Brettänderungen und besitzt drei Dinge, die
nicht verstreut werden dürfen: der Tipp verfällt (einer gegen ein älteres Brett gerechnet
ist nicht bloß veraltet, er kann falsch sein), der Sieg wird gezählt, das Spiel gespeichert.

---

## 7. Abschaltbare Hilfen

Vier Anzeigen nehmen dem Spieler Arbeit ab, und jede ist einzeln abschaltbar
(`data/Settings.kt`): Konflikte anzeigen, gleiche Ziffer hervorheben, Zeile/Spalte/Block
hervorheben, fertige Ziffern im Ziffernpad ausgrauen.

Die erste ist die eigentliche: sie sagt sofort, ob eine Ziffer im Feld überhaupt möglich
ist, und erledigt damit die halbe Denkarbeit. Aus heißt, dass die App schweigt.

### Die fünfte Hilfe ist von anderer Art

`warnOnWrong` vergleicht jede Eingabe mit der gespeicherten Lösung und sagt sofort, wenn
sie abweicht. Die vier anderen reden über die *Regeln* — was dasteht, kann der Spieler
selbst nachprüfen. Diese liest die *Antwort*. Daraus folgt alles Übrige:

- **Sie ist als einzige standardmäßig aus.** Eine Hilfe, die mitlöst, gibt man niemandem,
  der nicht danach gefragt hat.
- **Sie kostet etwas.** Drei falsche Eingaben beenden die Partie (`game/MistakeTally.kt`,
  `LIMIT = 3`). Ohne Preis wäre sie kein Kompromiss, sondern ein Solver mit Extraschritten:
  man tippt durch, bis es grün bleibt. Der Hinweistext im Einstellungsdialog nennt den
  Preis deshalb mit, und die Warnung zählt die verbleibenden Versuche laut mit — die
  dritte darf keine Überraschung sein.
- **Sie zählt in `allAidsOff` mit**, also verwirkt sie das Abzeichen „ohne Hilfen" wie
  jede andere.

**Gezählt wird in `onDigit`, nicht in `onBoardChanged`.** Durch den Trichter laufen auch
Rückgängig, Wiederholen und das Verwerfen eines Zweiges — einen Zug zurückzuspielen ist
aber nicht, ihn zu machen. Aus demselben Grund geht der Zähler nur nach oben: ließe sich
ein Versuch per Rückgängig zurückkaufen, wäre der Preis keiner. Auf einem verlorenen Brett
sind Rückgängig und Wiederholen deshalb tot, auf einem gelösten weiterhin nicht.

**Im Zweig wird nicht geprüft.** Ein Zweig (§4) ist ausdrücklich eine Annahme, die falsch
sein darf — das ist sein Zweck. Dafür einen Versuch abzuziehen, ließe die beiden Funktionen
einander widersprechen.

**Ein `finished` statt zweier Flags.** Uhr, Ziffernpad, Tipp, Pause und Zweigleiste fragen
nicht mehr `solved`, sondern `GameUiState.finished` (`solved || lost`). Zwei Flags an sechs
Stellen sind genau die Konstruktion, bei der eine davon vergessen wird und das Ziffernpad
auf einem verlorenen Brett weiterläuft.

Der Zähler steht im Spielstand (`GameSnapshot`, Format Version 3). Sonst wären drei neue
Versuche nur einen App-Neustart entfernt. Version 2 wird weiter gelesen und bekommt „noch
keine Fehler" — wer beim Update mitten im Rätsel steckt, soll es behalten.

**Es gibt genau ein Gate.** Die Konflikte werden immer berechnet — die Gelöst-Erkennung
braucht sie —, aber `SudokuViewModel.publish()` reicht dem Brett bei abgeschalteter
Anzeige ein durchweg leeres Array; das Brett erfährt den Unterschied nie. Die
Notiz-Konflikte laufen durch dasselbe Gate und werden bei abgeschalteter Anzeige gar
nicht erst berechnet: sie sind, anders als die der Eingaben, für nichts anderes gut. Diese
Entscheidung im ViewModel zu treffen statt im Zeichencode bedeutet, dass es genau eine
Stelle gibt, an der die App die Lösung verraten könnte, statt einer pro Zeichendurchgang.

Ein Nebeneffekt musste eigens behandelt werden: mit abgeschalteter Konfliktanzeige wird
ein voll, aber falsch ausgefülltes Gitter sonst mit **gar nichts** quittiert, was sich
wie ein Fehler der App anfühlt. `fullButWrong` blendet dann eine Zeile ein, die sagt
*dass* etwas nicht stimmt, ohne zu sagen *wo* — genau die Grenze, um die es bei der
Einstellung geht.

Gespeichert wird über `SudomniaPrefs` in SharedPreferences, **synchron im
ViewModel-Konstruktor** gelesen: schon der erste Frame zeigt die eigenen Einstellungen.
Ein DataStore-Flow würde einen Frame mit den Standardwerten zeichnen und sich dann
korrigieren — sichtbar als Flackern genau an dem Schalter, den jemand gerade umgelegt
hat. Das Feld `settings_version` existiert, damit sich eine später geänderte Vorgabe von
einem bewusst gesetzten Wert unterscheiden lässt.

---

## 8. Icon

`tools/generate_app_icon.py` erzeugt Hinter-, Vordergrund- und Monochrom-Ebene aus einer
Quelle. Das Zeichen ist ein 3×3-Block — die Box-Struktur, an der man ein Sudoku erkennt;
das volle 9×9-Gitter wäre bei Launcher-Größe grauer Brei.

Gefüllt sind die drei Zellen auf der Diagonalen. Das ist kein beliebiges Muster: die
Boxen 1, 5 und 9 sind die einzigen drei, die keine Einheit miteinander teilen — genau
deshalb füllt `GridGenerator` sie zuerst mit drei unabhängigen Zufallspermutationen. Das
Icon zeigt die eine strukturelle Tatsache, auf der der Generator aufgebaut ist.

Launcher-Masken geben den 72dp-Kreis um die Mitte des 108dp-Rasters frei, ein
quadratisches Zeichen darf also höchstens 72/√2 = 50,9dp breit sein. Der Block ist 50dp.
Die Farbe der leeren Zellen wurde **bei 48dp gerendert ausgewählt**, nicht bei voller
Größe beurteilt: eine Stufe über dem Hintergrund löst sich klein auf, zwei Stufen
darüber konkurrieren die leeren Zellen mit den gefüllten und die Diagonale verliert.

---

## 9. Oberfläche

Ein Screen, kein Navigationsgraph; die Stufenwahl ist ein Dialog.

### Die Eingabe hat keinen Modus

Der erste Entwurf hatte ein Ziffernpad plus einen Schalter „Notizen". Das erzwingt die
Reihenfolge *entscheiden → Feld → Ziffer*. Spieler denken andersherum: sie schauen auf ein
Feld und wissen erst dann, ob sie die Lösung haben oder Kandidaten sammeln wollen. Und der
teuerste Fall war der häufigste — drei Kandidaten notieren hieß umschalten, drei Taps,
zurückschalten, und wer das Zurückschalten vergaß, trug beim nächsten Feld eine Notiz statt
einer Ziffer ein.

Jetzt stehen **zwei Reihen dauerhaft** unter dem Brett: oben die großen Ziffern, darunter
die flachen Notiz-Tasten. Was ein Tap bedeutet, entscheidet damit *welche* Taste getroffen
wird, nicht ein vorher gesetzter Zustand. Drei Notizen sind drei Taps.

Drei Details, die daran hängen:

- **Die Notiz-Tasten sind zustandsbehaftet.** Eine gefüllte Taste heißt „diese Notiz steht
  im gewählten Feld". Die Reihe ist damit zugleich die Anzeige des Kandidatenstands, und
  eine Notiz wieder wegzunehmen ist derselbe Tap wie sie zu setzen.
- **Beide Reihen sind sichtbar tot, solange kein bearbeitbares Feld gewählt ist.** Vorher
  passierte bei einem Tap ins Leere einfach nichts — das ist genau die Rückmeldung, die die
  neue Reihenfolge nicht vermittelt. Die Notizreihe geht zusätzlich aus, sobald im Feld eine
  Ziffer steht: `SudokuGame.toggleNote` ignoriert diesen Fall ohnehin, und stillschweigend
  geschluckte Taps sind schlimmer als graue Tasten.
- **Die Ziffer im gewählten Feld wird als gedrückte Taste gezeigt.** Sie nochmal zu tippen
  löscht das Feld — dieser Toggle steckt schon in `SudokuGame.setDigit`, die Hervorhebung
  macht ihn nur sichtbar.

`SudokuGame` blieb dabei unverändert: `setDigit` / `toggleNote` / `clearCell` waren immer
schon feldbezogen. Weg ist nur der Modus im ViewModel — der einzige Zustand, den es dafür je
gab.

- **Das Gitter ist ein einziges `Canvas`**, nicht 81 Composables. Zeichnen und
  Antippen gehen durch dasselbe `BoardGeometry` — getrennt hergeleitet driften sie,
  und Tipps landen am Rand ein Feld daneben.
- Text über `nativeCanvas` mit wiederverwendeten `Paint`-Objekten statt `TextMeasurer`:
  bis zu 81 Ziffern plus 9 Notizen je Feld pro Frame.
- **`BoardState` schreibt `equals`/`hashCode` von Hand aus.** Als data class würden die
  `IntArray`-Felder per Identität verglichen, Compose würde die Neuzeichnung
  überspringen und das Brett stünde still. Genau dieser Bug ist Chessomnia einmal
  passiert.
- **Die Farbgebung der Hervorhebungen ist gemessen, nicht geschätzt.** Der Tint für
  Zeile/Spalte/Block deckt 21 Felder ab, die Gleiche-Ziffer-Hervorhebung höchstens
  neun — und Letztere ist das, wonach gesucht wird. Mit dem ursprünglichen Paar
  (Kreuz `#E2EDF4`, Grün `#CFE6B8`) las sich das Brett als „großes blaues Kreuz" und
  die gleichen Ziffern verschwanden darin. Ermittelt durch Nachbau des Zeichencodes
  und Rendern echter Stellungen, nicht durch Beurteilen am Quelltext. Jetzt: Kreuz
  schwächer (`#EDF3F8`), Grün kräftiger (`#A9D98A`).
- **Vorläufige Ziffern werden zweifach markiert**: gelbe Zelle *und* dunkelgelbe
  Ziffer. Die Zellfarbe allein reicht nicht — ein Zweigfeld, das gerade gewählt ist
  oder die hervorgehobene Ziffer trägt, wird in *deren* Farbe gezeichnet, und „das ist
  nur ein Versuch" darf dabei nicht verschwinden. In der Rangfolge der Zellfarben steht
  Gelb über der Gleiche-Ziffer-Hervorhebung, nach derselben Regel wie dort: wer weniger
  Felder färbt, gewinnt.
- **Die hervorgehobene Ziffer wird auch in den Notizen hervorgehoben** (fett, dunkelgrün).
  Ohne das leuchten die gesetzten Ziffern auf, aber die *notierten* — meist genau die,
  über die gerade nachgedacht wird — muss man mit dem Auge suchen.
- **Der Timer hat einen eigenen `StateFlow`.** Läge er im Brett-Zustand, würde das
  81-Feld-Canvas zweimal pro Sekunde neu gezeichnet.
- **Ob die Uhr läuft, entscheidet genau eine Funktion** (`SudokuViewModel.syncTimer`):
  ein Spiel ist geladen, es ist nicht gelöst, die App ist sichtbar, der Spieler hat
  nicht pausiert. Jeder Aufrufer ändert eine dieser Tatsachen und fragt neu. Verstreute
  `startTimer()`-Aufrufe waren genau der Fehler, aus dem die Uhr nachts auf dunklem
  Bildschirm weiterlief — sie wurde nur bei „neues Spiel" und „gelöst" angehalten,
  Lebenszyklus-Ereignisse kannte niemand. `startTimer()` steigt jetzt zusätzlich aus,
  wenn die Uhr schon läuft: sonst würde `startedAt` vorrücken, während `accumulatedMs`
  den alten Stand hält, und die Zeit dazwischen wäre weg.
- **Zwei Gründe für eine stehende Uhr, absichtlich getrennt.** Die Pause des Spielers
  blendet das Brett aus; „App nicht sichtbar" (`MainActivity.onStart`/`onStop`) hält
  nur die Uhr an und löst sich beim Zurückkommen von selbst auf — eine Tippquittung
  für jede beantwortete Benachrichtigung wäre eine Maut. `onStop` speichert außerdem:
  im Hintergrund kann der Prozess sterben, und sonst überlebte nur die Zeit bis zur
  letzten Brettänderung.
- **Die Pause blendet das Brett wirklich aus**, statt es hinter einem halbdurchsichtigen
  Schleier zu lassen: eine stehende Uhr vor einem lesbaren Gitter ist geschenkte
  Denkzeit, und Denken ist das ganze Spiel. Verdeckt wird auch das Ziffernpad — die
  Notizreihe ist der Kandidatenstand des gewählten Feldes.
- Die Brettseite wird als `min(maxWidth, maxHeight)` ausgeschrieben, **nicht** als
  `fillMaxHeight().aspectRatio(1f)` — letzteres leitet die Breite aus der Höhe ab und
  liefert bereitwillig ein Brett breiter als der Bildschirm.
- Die verstrichene Zeit wird aus `SystemClock.elapsedRealtime()` *abgeleitet*, nicht je
  Tick hochgezählt — ein verspäteter Tick kann die Anzeige damit nicht verschieben.

---

## 10. Wie Korrektheit sichergestellt wird

Nur JVM-Unit-Tests, kein Robolectric, keine Instrumented-Tests. `rules/` und `game/`
haben keine Android-Importe.

1. **Veröffentlichte Rätsel mit bekannter Lösung** (`ReferencePuzzles`): Project Euler
   96 Nr. 1, ein 17-Vorgaben-Rätsel, AI Escargot. Die Lösungen stammen aus einem
   unabhängigen Norvig-artigen Solver, nicht aus diesem Code — das ist die eine Stelle,
   an der Korrektheit nicht von der eigenen Implementierung abhängt.
2. **Eine mathematische Invariante:** fehlen zwei Ziffern vollständig aus den Vorgaben,
   sind sie in jeder Lösung vertauschbar, es gibt also mindestens zwei Lösungen. Ein
   Solver, der hier „eindeutig" meldet, ist kaputt — und genau dieser Fehler würde
   unlösbare Rätsel ausliefern.
3. **Jedes erzeugte Rätsel hat genau eine Lösung**, über alle Stufen.
4. **Minimalität:** aus einem Schwer-Rätsel lässt sich keine weitere Vorgabe entfernen,
   ohne die Eindeutigkeit zu verlieren.
5. **Undo stellt Ziffern *und* Notizen exakt wieder her**, über zufällige Zugfolgen.
6. **Mittelpunkt jedes Feldes findet sein Feld zurück** — Zeichnen und Antippen stimmen
   überein.
7. **Ein Tipp nennt nie die falsche Ziffer** — über viele Rätsel und viele Spielstände
   geprüft. Fiele das je um, setzte die App auf Knopfdruck eine garantiert falsche Zahl.
8. **Ist etwas zwingend, wird es begründet** statt blank aufgedeckt.
9. **Eine falsche Eingabe wird als tot erkannt**, bevor irgendetwas verraten wird.
10. **Ein Spielstand übersteht Kodieren und Zurückspielen** samt Notizen und Undo-Tiefe;
    kaputte Daten liefern `null`, statt beim Start zu werfen. Das gilt auch für einen
    offenen Zweig — und ein Spielstand im alten Format lädt weiterhin.
11. **Ein verworfener Zweig stellt Ziffern *und* Notizen exakt wieder her**, über
    zufällige Zugfolgen — dieselbe Prüfung wie für Undo, weil es dieselbe Mechanik ist.

`-DsudokuDeep=1` lässt dieselben Tests mit dem Zehnfachen an Rätseln laufen: ~2.500
erzeugte Rätsel, auf einem Raspberry Pi 5 in unter 90 Sekunden inklusive Kompilieren.
Erzeugung ist damit klar schnell genug, um auf dem Gerät zu laufen — eine
Vorab-Berechnung auf einem PC wird erst für die Kalibrierung des echten Graders und
für das ausgelieferte Rätsel-Paket gebraucht.

**Bekannte Lücke:** ohne das DLX-Zweitorakel prüft der Generatortest die Eindeutigkeit
mit demselben Solver, der sie erzeugt hat — teilweise zirkulär. Abgefedert durch die
Referenzrätsel und die Invariante aus Punkt 2; Stichproben wurden zusätzlich gegen eine
unabhängige Python-Implementierung geprüft. Der volle Kreuzvergleich gegen Dancing
Links steht aus.

---

## 11. Update

Die App holt sich neue Versionen selbst vom EnergyControl-Server im Haus. Beim Start und
danach alle 15 Minuten, `update/UpdateViewModel`.

### Warum mTLS und nicht der einfache Weg

Die Schwesterprojekte (Oystra, MyMoney) laden ihr APK über `http://…:8082`. Das geht hier
nicht: das Update soll **auch von unterwegs ohne VPN** funktionieren, und von außen ist am
Router genau ein Port offen — 8443, der mTLS-Connector. Also braucht die App ein
Client-Zertifikat, und das kann sie sich nirgends abholen (es gibt keinen Login und kein
Pairing wie bei MyMoney): es liegt fest in der APK, `res/raw/sudomnia_client.p12`,
ausgestellt einmalig von der MiniCa des Servers.

**Ein in der APK ausgeliefertes Schlüsselpaar ist extrahierbar** — daraus folgt der Rest des
Entwurfs. Es steht bewusst *nicht* in MyMoneys `device`-Tabelle, denn dort eingetragen wäre
es ein Vollzugang zur MyMoney-REST-API mit sämtlichen Finanzdaten. Stattdessen prüft auf dem
Server ein eigener Filter (`StaticCertAuthFilter`) nur auf `/api/v1/sudomnia/*` gegen eine
Liste zugelassener Seriennummern. Der Schlüssel öffnet damit genau eine Sache: den Download
dieses APKs. Verlieren wir ihn, kostet das eine Zeile in einer Textdatei und einen Neustart.

### Drei Dinge, die nicht verhandelbar sind

- **Hostname statt IP.** Jetty prüft SNI gegen das Serverzertifikat, dessen SAN nur
  `sudomnia.invalid` enthält. Eine IP-Adresse wird mit HTTP 400 beantwortet, bevor Filter
  oder Servlet überhaupt laufen. Der Name löst innen wie außen auf.
- **Der Trust-Anker ist das gepinnte Serverzertifikat** (`res/raw/server_cert.pem`), nicht
  der System-Truststore: die CA ist privat, Android kennt sie nicht. Nebeneffekt: eine
  kompromittierte öffentliche CA kann den Server nicht nachbauen.
- **Die SHA-256 aus `latest.json` wird geprüft.** Oystra und MyMoney schreiben den Hash und
  sehen ihn nie an; im LAN war das vertretbar, über das offene Internet nicht. Bei
  Abweichung wird die Datei gelöscht, damit dem Paketinstaller nie ein halber Download
  vorgelegt wird.

### Ein leeres PKCS12-Passwort ist auf Android kein Passwort

Der erste Wurf legte das Client-Zertifikat mit leerem Passwort ab — MyMoney macht das so,
und auf der JVM funktioniert es. Auf dem Tablet scheiterte der Aktualisieren-Knopf mit
`IllegalArgumentException: password empty`. Grund: das JDK schreibt PKCS12 seit 8u301 mit
PBES2/PBKDF2, und Androids BouncyCastle lehnt in PBKDF2 ein Passwort der Länge 0 ab.
MyMoney fällt das nicht auf, weil dessen P12 *auf dem Gerät* entsteht, mit dem alten
PKCS12-Verfahren.

Zwei Konsequenzen, beide in `SudomniaClientCertTool`: das Passwort ist nicht leer
(`sudomnia` — es liegt in jeder APK und schützt nichts, die Zugangskontrolle ist die
Seriennummern-Liste auf dem Server), und die Datei wird bewusst mit den *alten*
PKCS12-Verfahren geschrieben (3DES/RC2-40/HmacSHA1), weil sich das hier nicht auf einem
Gerät testen lässt und die alten Verfahren jedes Android liest. Kryptografisch kostet das
nichts, weil die Datei ohnehin öffentlich ist.

### Der Schluessel liegt nicht im Repo

`sudomnia_client.p12` ist git-ignoriert. Damit uebersetzt ein frischer Clone nicht --
bewusst: die Referenz bleibt ein normales `R.raw.sudomnia_client`, ein fehlender
Schluessel faellt also beim Bauen auf, an einer offensichtlichen Stelle, statt auf
irgendeinem Tablet. Die Alternative waere ein Nachschlagen zur Laufzeit gewesen; das
haette Compile-Zeit-Sicherheit gegen Bequemlichkeit fuer Forks getauscht, die die
Update-Funktion ohnehin nicht brauchen koennen -- der Server dahinter steht nur hier.
Das gepinnte Serverzertifikat ist oeffentlich und bleibt im Repo.

### Zustand statt Text

`UpdateState` ist eine sealed interface, kein gerenderter Satz plus Busy-Flag. Oystra hatte
Letzteres und hat dafür fünf Releases lang (1.0.53–1.0.57) Updates gemeldet, die niemand
installieren konnte — aus einem String lässt sich der Knopf nicht ableiten. Die eine Frage,
die die Oberfläche stellt, heißt `installableVersion`.

Zwei Regeln, die aus dem Betrieb kommen und beide im Code kommentiert sind: ein
fehlgeschlagener **Hintergrund**-Check überschreibt einen bereits gefundenen Fund nicht (das
Tablet verliert regelmäßig das WLAN, der Knopf darf nicht unter dem Finger verschwinden),
und vor dem Start des Paketinstallers geht der Zustand zurück auf „verfügbar" — bricht man
dort ab, steht der Knopf wieder da.

`ReleaseInfo.parse` ist die einzige testbare Stelle des Ganzen und deshalb bewusst
freigeschnitten: reine Funktion, `org.json`, kein Android-Typ. Getestet wird nicht das
Glückliche, sondern dass ein unvollständiges oder gar kein Manifest `null` liefert — die App
darf auf ein Dokument, das sie nicht verstanden hat, nicht handeln.

### Veröffentlichen

`deploy.sh` im Repo-Wurzelverzeichnis: Version in `version.properties` hochzählen, signiertes
Release bauen, APK nach `/var/lib/sudomnia/apk/` legen, `latest.json` schreiben. Die
Reihenfolge (erst committen/pushen, dann bauen) ist von Oystra übernommen und hat dort einen
konkreten Grund: so gehört zu jeder ausgelieferten APK ein Commit, den es auch im Remote
gibt. Solange Sudomnia kein Git-Repo ist, überspringt das Skript den Block.

---

## 12. Wenn etwas klemmt: `diag/`

Ein Tablet im Wohnzimmer hat kein Logcat. „Der Aktualisieren-Knopf sagt
IllegalArgumentException" ist als Fehlerbericht wertlos — genau daran hat der erste
Update-Versuch einen Tag verloren.

`DiagnosticsLog` ist deshalb eine Textdatei in `filesDir` plus ein Knopf im Hilfen-Dialog,
der sie an die Teilen-Auswahl übergibt. Abstürze landen über einen
`UncaughtExceptionHandler` automatisch darin; der vorherige Handler wird **verkettet, nicht
ersetzt** — ihn zu schlucken würde die App hängen lassen statt sterben, und das ist
schlimmer als der Absturz.

**Es gibt bewusst keinen Upload-Weg in dieser Klasse.** Ein Crash-Reporter wäre für ein
Einzelspieler-Sudoku eine Netzwerkabhängigkeit und eine Datenschutzgeschichte; hier sieht
der Spieler jedes Mal, was das Gerät verlässt, und wählt das Ziel selbst.

Die Datei ist ein Ringpuffer: über 64 KB wird die ältere Hälfte verworfen. Und
`log()` fängt seine eigenen Ausnahmen — die Diagnose darf nie das sein, was die App
kaputtmacht.
