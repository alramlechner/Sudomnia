# Privacy

Sudomnia collects nothing, stores nothing about you and has no account.

The version distributed on the Play Store holds **not a single Android permission**
— not even internet access. It technically cannot send anything anywhere.

This page is in English; the English version on file with Google Play is at
<https://alramlechner.github.io/Sudomnia/privacy.html> and says the same thing.

## What stays on the device

Settings, statistics and the running save state live in the app's private
SharedPreferences. They never leave the device and disappear on uninstall.

## Network

There is no server side. Puzzles are generated on the device.

The repository also contains a second flavour (`selfhosted`), which the author builds
for the devices in his own household and which fetches its updates from a private
server. It is **not** distributed via Google Play and needs `INTERNET` and
`REQUEST_INSTALL_PACKAGES` for that. Even this one transmits nothing beyond the HTTP
request itself — no identifiers, no save states, no statistics. Which flavour a build
is can be read off the manifest; `RELEASING.md` checks exactly that against the
uploaded bundle.

## Error reports

Crashes and errors land in a text file in the app's private storage. It is **only**
sent when you tap "Share" in the aids dialog — you then choose the destination
yourself. The app has no upload path; there is no crash reporter and no analytics.

If you pick a mail app, it is suggested `sudomnia@lechners.name` as the recipient —
a suggestion the app can adopt or ignore, not a fixed destination address. Any other
kind of app from the share chooser ignores this entirely.

## Children

The app contains no advertising, no purchases, no user-generated content and no
communication features. It collects data from nobody, at any age.

## Contact

`sudomnia@lechners.name` — or questions about this text, or an error in it, as an issue:
<https://github.com/alramlechner/Sudomnia/issues>
