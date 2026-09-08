# Sudomnia

Ein Sudoku für Android-Tablets. Prototyp (0.4.3).

Erzeugt seine Rätsel selbst — jedes hat **garantiert genau eine Lösung**, weil es aus
einem fertigen Gitter herausgegraben und nach jedem entfernten Feld auf Eindeutigkeit
geprüft wird. Keine Puzzle-Datenbank. Das Spiel selbst ist offline; das Netz wird
ausschließlich für die Update-Prüfung benutzt (siehe „Updates").

## Stand

Spielbar: Gitter, Ziffernpad, Notizen (Bleistift-Kandidaten), Undo/Redo, Zweige
(Versuch auf Probe), Timer mit Pause, Gelöst-Erkennung, drei Schwierigkeitsstufen, App-Icon,
signierter Release-Build, Auto-Update.

**Eingabe: erst das Feld, dann die Entscheidung.** Unter dem Brett stehen zwei Reihen —
oben die großen Ziffern (eintragen, nochmal tippen löscht wieder), darunter die kleinen
Notiz-Tasten (an/aus). Einen Modus gibt es nicht: drei Kandidaten notieren sind drei
Taps. Eine gefüllte kleine Taste heißt „diese Notiz steht im gewählten Feld", die Reihe
ist also zugleich die Anzeige. Ohne gewähltes Feld sind beide Reihen grau.

Ein Feld antippen hebt alle Felder mit derselben Ziffer hervor — gesetzte **und**
notierte. Funktioniert auch auf den vorgegebenen Zahlen.

**Zweig**, wenn kein Feld mehr eindeutig ist: „Zweig beginnen" setzt eine Marke, ab da
ist jede eingetragene Ziffer nur ein Versuch und steht **gelb** im Gitter. Geht der
Versuch auf, macht „Übernehmen" ihn endgültig; geht er nicht auf, räumt „Verwerfen"
alles davon in einem Schritt weg — auch die Notizen, die dabei bei den Nachbarn
verschwunden sind. Solange der Zweig offen ist, hält „Rückgängig" an seinem Anfang an,
damit man nicht versehentlich darunter rutscht. Ob der Versuch schon gescheitert ist,
sagt übrigens der Tipp: er meldet dann, dass das Rätsel nicht mehr aufgeht.

**Tipp**, wenn es klemmt: der erste Druck hebt ein Feld hervor, der zweite nennt die
Ziffer und — wenn möglich — den Grund („in Block 5 ist nur hier Platz für die 7"), der
dritte trägt sie ein. Hat man sich schon verrannt, sagt die App, *dass* das Rätsel nicht
mehr aufgeht, aber nicht wo.

**Pause** in der Kopfzeile: die Uhr steht, das Brett wird ausgeblendet (kein Schleier,
es wird nicht gezeichnet), ein Tipp irgendwohin macht weiter. Die Uhr hält außerdem
von selbst an, sobald die App nicht mehr sichtbar ist — Bildschirm aus, App-Umschalter,
Home-Taste —, und läuft beim Zurückkommen weiter.

**Statistik** pro Stufe: gelöst, begonnen, Bestzeit, dazu die Abzeichen „ohne Hilfen" und
„ohne Tipp".

Das **laufende Spiel wird gespeichert** — Ziffern, Notizen, Undo-Stack, ein offener
Zweig und die Spielzeit überleben das Beenden der App.

**Alle Hilfen sind abschaltbar** (Kopfzeile → „Hilfen"): Konfliktanzeige, Hervorhebung
gleicher Ziffern, Hervorhebung von Zeile/Spalte/Block, Ausgrauen fertiger Ziffern.
Die wichtigste ist die Konfliktanzeige — sie sagt dir sofort, ob eine Ziffer im Feld
überhaupt möglich ist, und nimmt dir damit die halbe Denkarbeit ab. Aus heißt: die App
schweigt, und du merkst es erst, wenn das Gitter nicht aufgeht. Die Einstellungen
überleben den App-Neustart.

**Die Stufen sind vorläufig.** Sie kommen aus einem Solver, der nur „Singles" kennt.
Die echte Bewertung braucht eine Bibliothek menschlicher Lösetechniken (Naked/Hidden
Pairs, Locked Candidates, X-Wing, XY-Wing, Colouring, Unique Rectangle …); die ist
noch nicht gebaut. Bis dahin sagt die App im Kopfbereich ehrlich „vorläufig".

Noch nicht da: Begründungen jenseits von Singles (dafür bräuchte es die Technikleiter),
Englisch.

## Bauen

Java 17 ist Pflicht — neuere JDKs bringen den Kotlin-Compiler zum Absturz.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=$HOME/android-sdk
cd android-app
./gradlew test                  # Solver, Generator, Spiellogik
./gradlew test -DsudokuDeep=1   # dieselben Tests mit 10x so vielen Rätseln
./gradlew assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
./gradlew lintRelease           # NewApi & Co. -- laeuft bewusst nicht im Release-Pfad
./gradlew assembleRelease       # signiert, ~1,7 MB
```

Der Release-Build wird über eine **nicht eingecheckte** `android-app/keystore.properties`
signiert (Vorlage: `keystore.properties.example`). Fehlt die Datei, entsteht ein
unsigniertes APK — genau das, was ein Fork oder ein CI-Lauf will.

Die Version steht an genau einer Stelle: `version.properties`.

## Veröffentlichen

```bash
./deploy.sh --notes "Was neu ist"     # Version +1, Release-Build, ab auf den Server
./deploy.sh --no-bump                 # wenn die Version schon von Hand erhöht wurde
```

Legt die APK unter `/var/lib/sudomnia/apk/` ab und schreibt das Manifest `latest.json`,
das die installierten Apps abfragen. Ein Neustart des EnergyControl-Servers ist dafür
nicht nötig.

## Updates

Die App fragt beim Start und danach alle 15 Minuten
`https://sudomnia.invalid:8443/api/v1/sudomnia/app/latest.json` ab. Ist dort ein höherer
`version_code` hinterlegt, erscheint über dem Brett ein Streifen „Version x.y.z ist da";
ein Tipp darauf lädt die APK, prüft ihre SHA-256 und übergibt sie dem Paketinstaller.
Dieselbe Funktion steckt unten im Hilfen-Dialog, dort auch als „jetzt nachsehen".

Der Weg läuft über den mTLS-Port des Servers — **das Update funktioniert deshalb auch
von unterwegs, ohne VPN.** Das dafür nötige Client-Zertifikat liegt fest in der App
(`res/raw/sudomnia_client.p12`, nicht im Repo); es ist serverseitig auf genau diesen
einen Download beschränkt und öffnet sonst nichts. Wenn es klemmt, gibt es im Heimnetz den Notweg
`http://sudomnia.invalid:8082/sudomnia/app/download` im Browser.

Deshalb hat die App zwei Berechtigungen: `INTERNET` und `REQUEST_INSTALL_PACKAGES`.
Beide werden ausschließlich hierfür verwendet — das Spiel selbst kennt keinen Server,
sammelt nichts und sendet nichts.

## Wenn etwas klemmt

Hilfen-Dialog → **Fehlerbericht → Teilen**. Das öffnet die normale Teilen-Auswahl mit
einem Textbericht: Gerät, Android-Version, App-Version und das Protokoll der letzten
Fehler samt Stacktrace. Abstürze landen automatisch darin.

Verschickt wird nichts von selbst — die App hat keinen Upload-Weg. Wohin der Bericht
geht, entscheidet jedes Mal der Teilen-Dialog.

## Struktur

| Verzeichnis | Inhalt |
|---|---|
| `android-app/app/src/main/java/…/rules/` | Solver, Generator, Digger — reines Kotlin, ohne Android-Importe, vollständig auf der JVM testbar |
| `…/game/` | Laufende Partie: Eingaben, Notizen, Undo-Historie |
| `…/data/` | Einstellungen, SharedPreferences mit Migrationskette |
| `…/ui/` | Compose-Oberfläche, ein Screen |
| `…/update/` | Update-Prüfung: Manifest, mTLS-Client, eigenes ViewModel |
| `tools/` | `generate_app_icon.py` — erzeugt die drei Icon-Ebenen aus einer Quelle |

Details und die Begründungen hinter den Entwurfsentscheidungen: **ARCHITECTURE.md**.

## Lizenz

Apache-2.0 — siehe [LICENSE](LICENSE) und [NOTICE](NOTICE).

| Was | Herkunft | Lizenz |
|---|---|---|
| Rätsel | Keine fremden. Der Generator erzeugt jedes selbst | — |
| App-Icon | Eigene Arbeit, erzeugt von `tools/generate_app_icon.py` | Apache-2.0 |
| Referenzrätsel im Test | Project Euler 96 Nr. 1, ein 17-Vorgaben-Rätsel, „AI Escargot" | Faktenlage¹ |
| Bibliotheken | AndroidX (Core, Activity, Lifecycle), Jetpack Compose | Apache-2.0 |

¹ Einzelne Sudoku-Gitter sind keine schutzfähigen Werke. Sie liegen ohnehin nur im
Testpfad und werden nicht mit der App ausgeliefert.

## Zwei Dateien fehlen im Repo

Beide absichtlich (siehe [RELEASING.md](RELEASING.md)):

- `android-app/app/src/main/res/raw/sudomnia_client.p12` — das Client-Zertifikat für
  die Update-Prüfung gegen meinen Heimserver. Ein privater Schlüssel gehört nicht auf
  GitHub. **Ohne diese Datei übersetzt das Projekt nicht** (`R.raw.sudomnia_client`
  existiert dann nicht). Das ist so gewollt: ein fehlender Schlüssel soll beim Bauen
  auffallen, an einer offensichtlichen Stelle, statt auf irgendeinem Tablet.
  Wer forkt, braucht die Update-Funktion ohnehin nicht — der Server dahinter steht nur
  bei mir. `update/` hängt an keiner anderen Schicht und lässt sich samt den beiden
  Zeilen in `MainActivity` ersatzlos entfernen.
- `android-app/keystore.properties` — ohne sie entsteht ein **unsigniertes** Release.

## Mitarbeit

Siehe [CONTRIBUTING.md](CONTRIBUTING.md). Datenschutz: [PRIVACY.md](PRIVACY.md).
Änderungen: [CHANGELOG.md](CHANGELOG.md).
