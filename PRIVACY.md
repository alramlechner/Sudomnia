# Datenschutz

Sudomnia sammelt nichts, speichert nichts ueber dich und hat kein Konto.

## Was auf dem Geraet bleibt

Einstellungen, Statistik und der laufende Spielstand liegen in den privaten
SharedPreferences der App. Sie verlassen das Geraet nie und verschwinden mit der
Deinstallation.

## Netzwerk

Das Spiel selbst ist offline. Die App baut genau eine Verbindung auf, und nur wenn
ein Update-Zertifikat einkompiliert ist (das ist in oeffentlichen Builds nicht der
Fall): sie fragt beim Start und danach alle 15 Minuten den privaten Server der
Familie nach der aktuellen Version und laedt auf Knopfdruck das APK. Dabei wird
nichts uebertragen, was ueber die HTTP-Anfrage selbst hinausgeht -- keine
Kennungen, keine Spielstaende, keine Statistik.

## Fehlerberichte

Abstuerze und Fehler landen in einer Textdatei im privaten Speicher der App. Sie
wird **nur** verschickt, wenn du im Hilfen-Dialog auf "Teilen" tippst -- dann
waehlst du selbst das Ziel. Die App hat keinen Upload-Weg; es gibt keinen
Crash-Reporter und keine Analytics.

## Berechtigungen

- `INTERNET` -- ausschliesslich fuer die Update-Pruefung
- `REQUEST_INSTALL_PACKAGES` -- um das heruntergeladene APK dem Paketinstaller
  zu uebergeben
