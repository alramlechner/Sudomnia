# Sudomnia

Ein Sudoku für Android-Tablets. Prototyp (0.4.3).

Erzeugt seine Rätsel selbst — jedes hat **garantiert genau eine Lösung**, weil es aus
einem fertigen Gitter herausgegraben und nach jedem entfernten Feld auf Eindeutigkeit
geprüft wird. Keine Puzzle-Datenbank. Das Spiel selbst ist offline; das Netz wird
ausschließlich für die Update-Prüfung benutzt (siehe „Updates").

## Stand

Spielbar: Gitter, Ziffernpad, Notizen (Bleistift-Kandidaten), Undo/Redo, Zweige
(Versuch auf Probe), Timer mit Pause, Gelöst-Erkennung, vier gemessene
Schwierigkeitsstufen, App-Icon, signierter Release-Build, Auto-Update.

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

**Tipp**, wenn es klemmt — und er sagt immer *warum*. Der erste Druck hebt hervor,
worum es geht, der zweite nennt die Technik und den Grund („Eingesperrte Kandidaten:
in Block 5 passt die 7 nur in Felder der Spalte 3 — im Rest der Spalte fällt sie weg"),
der dritte trägt die Ziffer ein. Braucht es mehrere Schritte bis dahin, führt der Tipp
durch die Kette. Gestrichene Kandidaten werden im Gitter durchgestrichen gezeigt, auch
wenn du sie nie notiert hattest. Hat man sich schon verrannt, sagt die App, *dass* das
Rätsel nicht mehr aufgeht, aber nicht wo.

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

**Vier Stufen, und sie sind gemessen.** Die App löst jedes Rätsel vor der Ausgabe so,
wie ein Mensch es täte — nur mit Techniken, die sich in einem Satz erklären lassen —
und die höchste dabei nötige Technik ist die Stufe:

| Stufe | Was sie verlangt |
|---|---|
| **Leicht** | nur Singles, und mindestens 36 Vorgaben bleiben stehen |
| **Mittel** | nur Singles, aber so tief ausgegraben, wie es geht |
| **Schwer** | Locked Candidates oder ein Paar/Tripel |
| **Experte** | X-Wing, Simple Colouring oder XY-Wing |

**Kein Rätsel verlangt Raten.** Das ist neu und war vorher nicht so: gemessen an 150
Rätseln der alten „Schwer"-Stufe waren **53 % mit keiner menschlichen Technik lösbar**.
Wer dort feststeckte, konnte nicht wissen, ob er etwas übersieht oder ob es nichts zu
sehen gibt. Der Generator gräbt jetzt nur so tief, wie die Technikleiter noch mitkommt.

Die **Oberfläche gibt es auf Englisch und Deutsch**; Englisch ist der Standard, Deutsch
kommt automatisch auf einem deutschsprachigen Gerät.

Noch nicht da: Begründungen jenseits von Singles — dafür bräuchte es die Technikleiter.

## Bauen

Java 17 ist Pflicht — neuere JDKs bringen den Kotlin-Compiler zum Absturz.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=$HOME/android-sdk
cd android-app
./gradlew test                     # Solver, Generator, Spiellogik
./gradlew test -DsudokuDeep=1      # dieselben Tests mit 10x so vielen Rätseln
./gradlew assemblePlayDebug        # app/build/outputs/apk/play/debug/…
./gradlew lintPlayRelease          # NewApi & Co. -- laeuft bewusst nicht im Release-Pfad
./gradlew bundlePlayRelease        # das AAB für Google Play
./gradlew assembleSelfhostedRelease # die APK für den Haus-Server
```

**Zwei Varianten.** `play` ist die Fassung für den Store: ohne Selbst-Aktualisierung
und **ohne eine einzige Berechtigung** — Google verbietet Apps aus dem Store, sich auf
einem anderen Weg selbst zu aktualisieren. `selfhosted` ist die Fassung für die
Geräte im Haus, die ihre Updates vom EnergyControl-Server holt; nur sie braucht das
Client-Zertifikat und ist ohne dieses nicht baubar. Ein frischer Clone übersetzt
`play` ohne jedes Geheimnis.

Der Release-Build wird über eine **nicht eingecheckte** `android-app/keystore.properties`
signiert (Vorlage: `keystore.properties.example`). Fehlt die Datei, entsteht ein
unsigniertes APK — genau das, was ein Fork oder ein CI-Lauf will.

Die Version steht an genau einer Stelle: `version.properties`.

## Veröffentlichen

Zwei Wege, und sie sind nicht dasselbe:

```bash
cd android-app && ./gradlew bundlePlayRelease   # das AAB für Google Play
./deploy.sh --notes "Was neu ist"               # die APK für die Geräte im Haus
```

Die Play-Fassung enthält die Selbst-Aktualisierung nicht und hält **keine einzige
Berechtigung**; die selfhosted-Fassung holt sich ihre Updates vom EnergyControl-Server
im Haus. Der ganze Ablauf samt Prüfung des hochgeladenen Bundles steht in
`RELEASING.md`, die Store-Texte in `store/`.

## Updates

**Aus dem Play Store** kommen sie wie bei jeder anderen App. Die Play-Fassung hat
keinen eigenen Update-Weg und darf auch keinen haben.

**Die Fassung für die Geräte im Haus** (`selfhosted`) fragt beim Start und danach alle
15 Minuten `https://sudomnia.invalid:8443/api/v1/sudomnia/app/latest.json` ab. Ist dort
ein höherer `version_code` hinterlegt, erscheint über dem Brett ein Streifen „Version
x.y.z ist da"; ein Tipp darauf lädt die APK, prüft ihre SHA-256 und übergibt sie dem
Paketinstaller. Dieselbe Funktion steckt unten im Hilfen-Dialog, dort auch als „jetzt
nachsehen".

Der Weg läuft über den mTLS-Port des Servers — **das Update funktioniert deshalb auch
von unterwegs, ohne VPN.** Das dafür nötige Client-Zertifikat liegt fest in der App
(`app/src/selfhosted/res/raw/sudomnia_client.p12`, nicht im Repo); es ist serverseitig
auf genau diesen einen Download beschränkt und öffnet sonst nichts. Wenn es klemmt, gibt
es im Heimnetz den Notweg `http://sudomnia.invalid:8082/sudomnia/app/download` im
Browser.

Nur diese Fassung hat deshalb zwei Berechtigungen: `INTERNET` und
`REQUEST_INSTALL_PACKAGES`. Beide werden ausschließlich hierfür verwendet — das Spiel
selbst kennt keinen Server, sammelt nichts und sendet nichts.

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

## Drei Dateien fehlen im Repo

Alle drei absichtlich (siehe [RELEASING.md](RELEASING.md)). **Der `play`-Build braucht
keine davon** — ein frischer Clone übersetzt ihn ohne jedes Geheimnis:

- `android-app/app/src/selfhosted/res/raw/sudomnia_client.p12` — das Client-Zertifikat
  für die Update-Prüfung gegen meinen Heimserver. Ein privater Schlüssel gehört nicht
  auf GitHub. **Ohne diese Datei übersetzt die `selfhosted`-Variante nicht**
  (`R.raw.sudomnia_client` existiert dann nicht). Das ist so gewollt: ein fehlender
  Schlüssel soll beim Bauen auffallen, an einer offensichtlichen Stelle, statt auf
  irgendeinem Tablet. Wer forkt, braucht diese Variante ohnehin nicht — der Server
  dahinter steht nur bei mir.
- `android-app/keystore.properties` — ohne sie entsteht ein **unsigniertes** Release.
- `android-app/play-service-account.json` — nur für den automatisierten Play-Upload.
  Ohne sie laufen alle übrigen Tasks; nur `publish*` scheitert, mit klarer Meldung.

## Mitarbeit

Siehe [CONTRIBUTING.md](CONTRIBUTING.md). Datenschutz: [PRIVACY.md](PRIVACY.md).
Änderungen: [CHANGELOG.md](CHANGELOG.md). Projektseite:
[alramlechner.github.io/Sudomnia](https://alramlechner.github.io/Sudomnia/).
