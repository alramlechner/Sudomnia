# Veröffentlichen

Sudomnia hat **zwei Auslieferungswege**, und sie sind nicht dasselbe:

| Weg | Variante | Womit |
|---|---|---|
| Google Play | `play` | `bundlePlayRelease` → AAB, hochgeladen |
| Geräte im Haus | `selfhosted` | `./deploy.sh` → APK auf den EnergyControl-Server |

Der Unterschied ist keine Einstellung zur Laufzeit: die Play-Variante enthält die
Selbst-Aktualisierung **nicht** und hält deshalb keine einzige Berechtigung.
Google verbietet Apps aus dem Store, sich auf einem anderen Weg selbst zu
aktualisieren. Ein `selfhosted`-Build im Store wäre der eine Fehler in diesem
Aufbau, der sich nicht still zurücknehmen lässt — deswegen sind seine
Play-Tasks in `build.gradle.kts` abgeschaltet, zusätzlich zu diesem Absatz.

---

## Voraussetzungen

| | |
|---|---|
| JDK | **17.** Neuere JDKs bringen den Kotlin-Compiler in diesem Projekt zum Absturz. |
| Android SDK | `ANDROID_HOME` gesetzt, build-tools 35 vorhanden |
| Signieren | `android-app/keystore.properties`, nicht im Repo — Vorlage: `keystore.properties.example` |
| Play-Upload | `android-app/play-service-account.json`, nicht im Repo — Vorlage: `play-service-account.json.example`. Nur für den automatisierten Upload nötig. |
| selfhosted | `android-app/app/src/selfhosted/res/raw/{sudomnia_client.p12,server_cert.pem}`, nicht im Repo (siehe unten) |
| selfhosted | `android-app/local.properties`: `sudomnia.updateHost=<dein-hostname>` (siehe unten) |

Der Upload-Schlüssel gehört **nicht** ins Repository. Ihn zu verlieren kostet nicht
die App (Play App Signing hält den eigentlichen Signaturschlüssel), aber Google
muss den Upload-Schlüssel dann vor dem nächsten Release zurücksetzen.

---

## 1. Version

`version.properties` im Wurzelverzeichnis ist die einzige Quelle. Beide Zahlen
wandern gemeinsam:

```properties
VERSION_CODE=10
VERSION_NAME=0.5.0
```

`VERSION_CODE` muss **echt größer** sein als alles, was Play je gesehen hat.
Lücken sind egal, Rückwärtsgehen nicht, und es lässt sich nicht rückgängig machen
— Play merkt sich einen Versionscode auch für ein verworfenes Release.

Jeder Build, der diese Maschine verlässt, bekommt eine eigene Version, auch
Testbauten. Zwei Artefakte mit derselben Version sind der Weg, einen Fehler zu
suchen, der nie installiert war.

## 2. Erzeugtes neu erzeugen

Nur wenn die Quelle sich geändert hat:

```bash
python3 tools/generate_app_icon.py       # App-Icon
python3 tools/render_store_assets.py     # Play-Icon 512 und Feature-Grafiken
```

## 3. Prüfen und bauen

```bash
cd android-app
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
./gradlew test                  # die ganze Testsuite, beide Varianten
./gradlew test -DsudokuDeep=1   # ~2.500 Rätsel, jedes erzeugt und benotet
./gradlew lintPlayRelease
./gradlew bundlePlayRelease
```

Das Artefakt ist `app/build/outputs/bundle/playRelease/app-play-release.aab`.

⚠️ Play will das **Bundle**, nicht ein APK. `assemblePlayRelease` erzeugt ein APK
zum Sideloaden auf ein Testgerät; hochgeladen wird das nicht.

## 4. Das Artefakt prüfen

Geprüft wird, was hochgeht — nicht, was übersetzt wurde. „BUILD SUCCESSFUL" ist
kein Beweis.

```bash
AAB=app/build/outputs/bundle/playRelease/app-play-release.aab

# Mit dem Upload-Schluessel signiert, nicht mit einem Debug-Schluessel
jarsigner -verify -verbose:summary -certs "$AAB" | grep "Signed by"

# Alle drei Sprachen wirklich drin (je ein String, der nur dort vorkommt)
unzip -p "$AAB" base/resources.pb | grep -ac "Verstecktes Single"   # de
unzip -p "$AAB" base/resources.pb | grep -ac "Hidden single"        # en
unzip -p "$AAB" base/resources.pb | grep -ac "Único oculto"         # es

# Die Selbst-Aktualisierung ist NICHT drin
unzip -p "$AAB" base/dex/classes.dex | grep -ac "sudomnia/update/UpdateClient"   # muss 0 sein
```

Und im gemergten Manifest, wo eine Abhängigkeit still eine Berechtigung
zurückbringen kann:

```bash
grep -oE '<uses-permission[^>]*>' \
  app/build/intermediates/merged_manifest/playRelease/*/AndroidManifest.xml
```

Die einzige Zeile darf `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` sein, die
AndroidX über die App selbst deklariert. Insbesondere kein `INTERNET`. Genau das
prüft auch der CI-Workflow bei jedem Push.

## 5. Committen und taggen

```bash
git commit -am "Version 0.5.0 (10)"
git tag -a v0.5.0 -m "Version 0.5.0"
git push && git push --tags
```

## 6. Hochladen

### Automatisiert

Sobald `android-app/play-service-account.json` existiert:

```bash
cd android-app
./gradlew publishPlayBundle                     # internal track
./gradlew publishPlayBundle --track production  # ausdruecklich; nie als Vorgabe
```

Die Vorgaben sind absichtlich harmlos: ein blankes `publishPlayBundle` geht in den
**internen** Kanal — eine benannte Liste von höchstens 100 Testern, nicht der
Store. Ohne die Schlüsseldatei laufen alle übrigen Tasks weiter, und nur die
`publish*`-Tasks scheitern, mit einer Meldung, die das auch sagt.

⚠️ Die Developer-API **kann das erste Release einer App nicht anlegen.** Google
verlangt, dass für diesen Paketnamen einmal von Hand ein Bundle über die Console
hochgeladen wurde. Bis dahin gilt der manuelle Weg.

### Von Hand

Play Console: **Test and release → Production** (oder ein Testkanal) → *Create new
release* → das `.aab` hochladen → Release-Notes → ausrollen.

Release-Notes kommen aus `CHANGELOG.md`, gekürzt. Play erlaubt 500 Zeichen je
Sprache, und `en-US`, `de-DE` und `es-ES` brauchen jeweils einen eigenen Text.

Der Store-Eintrag selbst steht in `store/listing-en.md`, `store/listing-de.md`
und `store/listing-es.md` — einschließlich der Data-Safety-Antworten und der
Einordnung (die nur einmal gilt, nicht je Sprache). Diese Dateien mit dem
gleichziehen, was in der Console wirklich eingetragen ist, sonst wird das
nächste Release aus einer veralteten Quelle bearbeitet.

---

## Nur beim ersten Release

Passiert einmal und gehört nicht zum Alltag:

1. **Identitätsprüfung** des Entwicklerkontos.
2. **App in der Console anlegen**: Name, Standardsprache, „App" vs. „Spiel", gratis.
3. **Geschlossener Test mit 12 Testern über 14 zusammenhängende Tage** — für neue
   private Entwicklerkonten Voraussetzung, bevor die Produktion freigeschaltet
   wird. Das ist mit Abstand der langsamste Schritt und hängt an Menschen, nicht
   an Code. Früh anfangen.
4. **Play App Signing**: beim ersten Upload annehmen. Für Sudomnia soll dabei der
   **vorhandene Schlüssel hochgeladen** werden (Play Console → App-Integrität →
   „Schlüssel aus Java-Keystore exportieren und hochladen"). Grund: Die Geräte im
   Haus haben die `selfhosted`-Fassung mit derselben `applicationId` und diesem
   Schlüssel installiert. Nur wenn Play mit demselben Schlüssel signiert, kann die
   Store-Fassung sie aktualisieren, statt eine Neuinstallation mit verlorener
   Statistik und verlorenem Spielstand zu erzwingen.
5. **Screenshots**: Handy, 7"- und 10"-Tablet. Sie müssen die echte App zeigen,
   müssen also auf einem Gerät aufgenommen werden — siehe
   `store/screenshots/README.md`.

---

## Der zweite Weg: die Geräte im Haus

```bash
./deploy.sh --notes "Was neu ist"     # Version +1, Release-Build, ab auf den Server
./deploy.sh --no-bump                 # wenn die Version schon von Hand erhoeht wurde
```

Baut `assembleSelfhostedRelease`, legt die APK unter `/var/lib/sudomnia/apk/` ab
und schreibt `latest.json`, das die installierten Apps abfragen. Ein Neustart des
EnergyControl-Servers ist dafür nicht nötig; die Apps sehen die neue Version
innerhalb von 15 Minuten.

### Der Hostname

`UpdateClient.BASE_URL` nennt keinen Hostnamen im Quellcode mehr. Er kommt über
`BuildConfig.UPDATE_HOST` aus der untracked `android-app/local.properties`:

```properties
sudomnia.updateHost=<dein-hostname>
```

Fehlt der Eintrag, fällt der Build auf `sudomnia.invalid` zurück (RFC 2606, löst
absichtlich nirgendwo auf) — die `selfhosted`-Variante übersetzt also auch ohne diese
Zeile, kann dann aber keinen Server erreichen. Ein frischer Clone verrät so nicht,
welcher Host dahintersteckt.

### Das Client-Zertifikat und das gepinnte Serverzertifikat

`android-app/app/src/selfhosted/res/raw/sudomnia_client.p12` **und**
`.../server_cert.pem` sind git-ignoriert. **Ohne sie übersetzt die
selfhosted-Variante nicht** — `UpdateClient` referenziert `R.raw.sudomnia_client` und
`R.raw.server_cert` ganz normal. Das ist Absicht: ein fehlender Schlüssel soll beim
Bauen auffallen und nicht erst auf dem Gerät. Die `play`-Variante braucht keins von
beidem, ein frischer Clone übersetzt die also ohne jedes Geheimnis.

Neu ausstellen (auf dem Server-Host; schreibt zugleich die Seriennummer in die
Allow-List des Servers und das gepinnte Serverzertifikat in `res/raw/`):

```bash
cd /home/pi/projects/EnergyControl
mvn -o -q dependency:build-classpath -Dmdep.outputFile=/tmp/ec-cp.txt
java -cp "target/classes:$(cat /tmp/ec-cp.txt)" \
     name.lechners.energycontrol.tools.SudomniaClientCertTool
```

Danach den EnergyControl-Server neu starten — die Allow-List wird beim Start
gelesen. Alte Seriennummern in `/var/lib/sudomnia/allowed-serials.txt` von Hand
entfernen.

Nicht `mvn exec:java` benutzen: in der `pom.xml` steht `Daemon` fest als
`mainClass`, `-Dexec.mainClass` wird davon überstimmt und es startet ein zweiter
Server-Prozess.

### Fallstricke

- **PKCS12-Passwort nie leer.** Androids BouncyCastle lehnt in PBKDF2 ein Passwort
  der Länge 0 mit `IllegalArgumentException: password empty` ab. Der Wert steht in
  `UpdateClient.P12_PASSWORD` und muss zu `SudomniaClientCertTool` passen.
- **Version nur in `version.properties` ändern.** `build.gradle.kts` liest sie von
  dort; ein zweiter Ort wäre sofort widersprüchlich.
- **Der Schlüssel ist derselbe wie für Play.** Beide Wege signieren mit
  `keystore.properties` — genau deshalb können die Tablets zwischen den Fassungen
  wechseln (siehe Punkt 4 oben).
