# Datenschutz

Sudomnia sammelt nichts, speichert nichts über dich und hat kein Konto.

Die im Play Store ausgelieferte Fassung hält **keine einzige Android-Berechtigung**
— nicht einmal Internetzugriff. Sie kann technisch gar nichts irgendwohin senden.

Diese Seite auf Deutsch; die für Google Play hinterlegte englische Fassung steht
unter <https://alramlechner.github.io/Sudomnia/privacy.html> und sagt dasselbe.

## Was auf dem Gerät bleibt

Einstellungen, Statistik und der laufende Spielstand liegen in den privaten
SharedPreferences der App. Sie verlassen das Gerät nie und verschwinden mit der
Deinstallation.

## Netzwerk

Es gibt keine Serverseite. Die Rätsel entstehen auf dem Gerät.

Das Repository enthält daneben eine zweite Variante (`selfhosted`), die der Autor
für die Geräte im eigenen Haushalt baut und die sich ihre Updates von einem
privaten Server holt. Sie wird **nicht** über Google Play verteilt und braucht
dafür `INTERNET` und `REQUEST_INSTALL_PACKAGES`. Auch sie überträgt nichts, was
über die HTTP-Anfrage selbst hinausgeht — keine Kennungen, keine Spielstände,
keine Statistik. Welche Variante ein Build ist, lässt sich am Manifest ablesen;
`RELEASING.md` prüft genau das am hochgeladenen Bundle.

## Fehlerberichte

Abstürze und Fehler landen in einer Textdatei im privaten Speicher der App. Sie
wird **nur** verschickt, wenn du im Hilfen-Dialog auf „Teilen" tippst — dann
wählst du selbst das Ziel. Die App hat keinen Upload-Weg; es gibt keinen
Crash-Reporter und keine Analytics.

Wählst du eine Mail-App, wird ihr `sudomnia@lechners.name` als Empfänger
vorgeschlagen — ein Vorschlag, den die App übernehmen oder ignorieren kann,
keine feste Zieladresse. Jede andere Art von App aus der Teilen-Auswahl
ignoriert das komplett.

## Kinder

Die App enthält keine Werbung, keine Käufe, keine nutzergenerierten Inhalte und
keine Kommunikationsfunktionen. Sie erhebt von niemandem Daten, in keinem Alter.

## Kontakt

`sudomnia@lechners.name` — oder Fragen zu diesem Text bzw. ein Fehler darin als Issue:
<https://github.com/alramlechner/Sudomnia/issues>
