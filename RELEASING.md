# Veroeffentlichen

## Kurzfassung

```bash
./deploy.sh --notes "Was neu ist"
```

Das erhoeht `version.properties`, baut das signierte Release, legt die APK unter
`/var/lib/sudomnia/apk/` ab und schreibt `latest.json`. Installierte Apps sehen die
neue Version innerhalb von 15 Minuten.

Java 17 ist Pflicht -- neuere JDKs bringen den Kotlin-Compiler in diesem Setup zum
Absturz. Das Skript setzt `JAVA_HOME` selbst.

## Was der Build-Host braucht

Beides ist **nicht im Repo** und muss auf dem Build-Rechner liegen:

### 1. `android-app/keystore.properties`

Vorlage: `keystore.properties.example`. Fehlt die Datei, entsteht ein unsigniertes
APK -- genau das, was ein Fork oder ein CI-Lauf will, aber es kann keine installierte
Version aktualisieren. **Der Keystore muss ueber alle Releases derselbe bleiben.**

### 2. `android-app/app/src/main/res/raw/sudomnia_client.p12`

Das Client-Zertifikat fuer den Update-Endpunkt. Ein privater Schluessel gehoert nicht
auf GitHub, deshalb ist die Datei git-ignoriert.

**Ohne sie uebersetzt das Projekt nicht** -- `UpdateClient` referenziert
`R.raw.sudomnia_client` ganz normal. Das ist Absicht: ein fehlender Schluessel soll
beim Bauen auffallen und nicht erst auf dem Geraet. Ein frischer Clone braucht die
Datei also, bevor irgendein Gradle-Task laeuft.

Neu ausstellen (auf dem Server-Host, schreibt zugleich die Seriennummer in die
Allow-List des Servers und das gepinnte Serverzertifikat in `res/raw/`):

```bash
cd /home/pi/projects/EnergyControl
mvn -o -q dependency:build-classpath -Dmdep.outputFile=/tmp/ec-cp.txt
java -cp "target/classes:$(cat /tmp/ec-cp.txt)" \
     name.lechners.energycontrol.tools.SudomniaClientCertTool
```

Danach den EnergyControl-Server neu starten -- die Allow-List wird beim Start gelesen.
Alte Seriennummern in `/var/lib/sudomnia/allowed-serials.txt` von Hand entfernen.

Nicht `mvn exec:java` benutzen: in der `pom.xml` steht `Daemon` fest als `mainClass`,
`-Dexec.mainClass` wird davon ueberstimmt und es startet ein zweiter Server-Prozess.

## Fallstricke

- **PKCS12-Passwort nie leer.** Androids BouncyCastle lehnt in PBKDF2 ein Passwort der
  Laenge 0 mit `IllegalArgumentException: password empty` ab. Der Wert steht in
  `UpdateClient.P12_PASSWORD` und muss zu `SudomniaClientCertTool` passen.
- **Version nur in `version.properties` aendern.** `build.gradle.kts` liest sie von
  dort; ein zweiter Ort waere sofort widerspruechlich.

## Vor dem Taggen

```bash
cd android-app
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
./gradlew test                  # Solver, Generator, Spiellogik, Update-Manifest
./gradlew test -DsudokuDeep=1   # dieselben Tests mit 10x so vielen Raetseln
./gradlew lintRelease           # NewApi & Co. -- laeuft bewusst nicht im Release-Pfad
```

`CHANGELOG.md` ergaenzen, dann `deploy.sh`.
