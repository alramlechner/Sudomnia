# Changelog

Format nach [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
Versionierung nach [SemVer](https://semver.org/lang/de/).

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
