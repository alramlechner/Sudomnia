# Changelog

Format nach [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
Versionierung nach [SemVer](https://semver.org/lang/de/).

## [Unveroeffentlicht] – Vorbereitung auf Veröffentlichung

### Neu
- **Die Technikleiter ist da.** `HumanSolver` loest ein Raetsel so, wie ein Mensch es
  taete: Hidden/Naked Single, Locked Candidates, Naked/Hidden Pair und Triple, X-Wing,
  Swordfish, Simple Colouring, XY-Wing -- und **nie durch Raten**. Die hoechste dabei
  noetige Technik ist die Stufe (`Grader`), gemessen statt geschaetzt.
- **Der Tipp begruendet jetzt jeden Schritt.** Er nimmt den naechsten Schritt der
  Leiter vom Brett, wie es dasteht, nennt die Technik und den Grund und fuehrt noetigen-
  falls durch eine Kette von Schritten bis zu der Ziffer, die man eintragen kann.
  Gestrichene Kandidaten stehen durchgestrichen im Gitter. Die blanke Aufdeckung ohne
  Begruendung ist damit fuer Raetsel aus diesem Generator verschwunden.
- **Vier Stufen** statt drei: Leicht, Mittel, Schwer, Experte. "vorlaeufig" ist aus der
  Kopfzeile verschwunden, dafuer steht dort jetzt die Zahl der Vorgaben.

### Behoben
- **Raetsel, die Raten verlangten.** Die alte Stufe "Schwer" hiess nur "Singles
  genuegen nicht" und nahm sonst alles, was eindeutig war -- gemessen an 150 solchen
  Raetseln waren **53 % mit keiner menschlichen Technik loesbar**. Der Digger nimmt
  eine Entfernung jetzt zurueck, sobald die Leiter das Raetsel nicht mehr zu Ende
  bringt.

### Neu (Veröffentlichung)
- **Englisch als Standardsprache**, Deutsch als Uebersetzung (`values-de`). Die App
  war bis hier nur deutsch.
- **Zwei Varianten** (`play`, `selfhosted`). Die Play-Fassung enthaelt die
  Selbst-Aktualisierung nicht -- Google verbietet Apps aus dem Store, sich auf einem
  anderen Weg selbst zu aktualisieren -- und haelt damit **keine einzige
  Berechtigung**. Die selfhosted-Fassung bleibt wie bisher; `deploy.sh` baut sie.

### Geaendert
- **Ein frischer Clone uebersetzt jetzt** (`assemblePlayDebug`). Bisher scheiterte er
  am fehlenden Client-Zertifikat, das absichtlich nicht im Repo liegt.

## [0.4.5] – 2026-09-08

### Neu
- **Pause** in der Kopfzeile: haelt die Uhr an und blendet das Brett aus (es wird
  nicht gezeichnet, kein durchscheinender Schleier). Weiter geht es mit einem Tipp
  irgendwohin.

### Behoben
- **Die Uhr lief weiter, wenn die App nicht sichtbar war** -- Bildschirm aus,
  App-Umschalter, Home-Taste. Angehalten wurde sie bisher nur bei "neues Spiel" und
  "geloest"; Lebenszyklus-Ereignisse kannte das ViewModel gar nicht, und die
  verstrichene Zeit kommt aus `elapsedRealtime()`, die auch im Tiefschlaf laeuft.
  Jetzt entscheidet eine Stelle, ob die Uhr laufen darf. `onStop` speichert
  zusaetzlich den Spielstand -- im Hintergrund kann der Prozess sterben, und bisher
  ueberlebte nur die Zeit bis zur letzten Brettaenderung.

## [0.4.4] – 2026-09-08

### Neu
- **Zweig** – ein Versuch auf Probe fuer den Punkt, an dem kein Feld mehr erzwungen
  ist. "Zweig beginnen" setzt eine Marke, alles danach Eingetragene ist vorlaeufig
  und steht **gelb** im Gitter (Zelle und Ziffer). "Verwerfen" nimmt den ganzen
  Versuch in einem Schritt zurueck, "Uebernehmen" macht ihn endgueltig. Rueckgaengig
  haelt an der Marke an, solange der Zweig offen ist; der Zweig ueberlebt das
  Beenden der App und wird beim Loesen automatisch uebernommen.

## [0.4.3] – 2026-09-07

### Behoben
- Der Aktualisieren-Knopf scheiterte mit `IllegalArgumentException: password empty`.
  Das JDK schreibt PKCS12 seit 8u301 mit PBES2/PBKDF2, und Androids BouncyCastle
  lehnt dort ein Passwort der Laenge 0 ab. Das Client-Zertifikat hat jetzt ein
  Passwort und wird im alten PKCS12-Format geschrieben.

### Neu
- **Fehlerbericht teilen** (Hilfen-Dialog): Protokoll, Abstuerze und Stacktraces
  gehen ueber die normale Teilen-Auswahl raus. Kein Upload-Weg in der App.

## [0.4.2] – 2026-09-07

### Geaendert
- **Die Eingabe hat keinen Modus mehr.** Statt Schalter "Notizen" stehen zwei
  Reihen unter dem Brett: oben die Ziffern, darunter die Notiz-Tasten. Erst das
  Feld waehlen, dann entscheiden -- drei Kandidaten sind drei Taps. Die
  Notiz-Tasten zeigen den Kandidatenstand des gewaehlten Feldes an; ohne
  bearbeitbares Feld sind beide Reihen sichtbar tot.

### Neu
- **Auto-Update** ueber den mTLS-Port des Haus-Servers, funktioniert damit auch
  ohne VPN. Banner ueber dem Brett, Versionszeile im Hilfen-Dialog,
  SHA-256-Pruefung des Downloads.

## [0.4.0] – 2026-09-03

Erster spielbarer Stand: Generator mit garantiert eindeutiger Loesung, drei
Stufen, Notizen, Undo/Redo, Timer, Tipp-Funktion, Statistik, gespeicherter
Spielstand, abschaltbare Hilfen, App-Icon, signierter Release-Build.
