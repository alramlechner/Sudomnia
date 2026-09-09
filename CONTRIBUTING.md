# Mitarbeit

Sudomnia ist ein privates Projekt, das oeffentlich liegt. Pull Requests sind
willkommen, aber ohne Zusage auf Tempo oder Aufnahme.

## Bauen

Java 17 ist Pflicht; neuere JDKs bringen den Kotlin-Compiler zum Absturz.

```bash
cd android-app
./gradlew test
./gradlew assemblePlayDebug
```

**Es gibt zwei Varianten** (`flavorDimensions "distribution"`):

| Variante | Was drin ist | Wofuer |
|---|---|---|
| `play` | kein `update/`, keine einzige Berechtigung | Google Play, und der Standard beim Entwickeln |
| `selfhosted` | Selbst-Aktualisierung vom Haus-Server, `INTERNET` + `REQUEST_INSTALL_PACKAGES` | die privaten Geraete, `deploy.sh` |

Ein frischer Clone uebersetzt `play` vollstaendig — dort wird kein Geheimnis
gebraucht. `selfhosted` dagegen scheitert absichtlich, solange
`android-app/app/src/selfhosted/res/raw/sudomnia_client.p12` fehlt (privater
Schluessel, siehe RELEASING.md): ein fehlendes Zertifikat soll beim Bauen auffallen
und nicht erst auf dem Geraet. Ohne `keystore.properties` bleibt der Release-Build
ausserdem unsigniert.

## Worauf beim Code geachtet wird

- **`rules/` und `game/` bleiben frei von Android-Importen.** Das ist die Bedingung
  dafuer, dass die Korrektheit ueberhaupt getestet werden kann -- es gibt kein
  Robolectric und keine Instrumented-Tests.
- **Kommentare begruenden, sie beschreiben nicht.** Warum Bitmasken und nicht
  `Set<Int>`, warum Hidden vor Naked Single, warum `BoardState` sein `equals` von Hand
  schreibt. Was der Code tut, steht im Code.
- **Neue Entwurfsentscheidungen gehoeren nach ARCHITECTURE.md**, und zwar mit dem
  verworfenen Alternativvorschlag daneben.
- Deutsche UI-Strings, englische Bezeichner und Code-Kommentare.

## Tests

`./gradlew test -DsudokuDeep=1` laesst dieselben Tests mit dem Zehnfachen an Raetseln
laufen (~2.500 Stueck). Wer am Solver, Generator oder Digger etwas aendert, sollte das
einmal durchlaufen lassen.

Achtung: `rules/` liegt **doppelt** im Repo -- einmal in `android-app`, einmal in
`generator-cli`. Aenderungen muessen in beide, sonst driftet das CLI-Werkzeug
unbemerkt weg.
