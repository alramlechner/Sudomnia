# Contributing

Sudomnia is a private project that happens to be public. Pull requests are
welcome, but without any promise on pace or acceptance.

## Building

Java 17 is mandatory; newer JDKs crash the Kotlin compiler.

```bash
cd android-app
./gradlew test
./gradlew assemblePlayDebug
```

**There are two flavours** (`flavorDimensions "distribution"`):

| Flavour | What's in it | What it's for |
|---|---|---|
| `play` | no `update/`, not a single permission | Google Play, and the default while developing |
| `selfhosted` | self-update from the house server, `INTERNET` + `REQUEST_INSTALL_PACKAGES` | the private devices, `deploy.sh` |

A fresh clone compiles `play` completely — it needs no secret. `selfhosted`, on
the other hand, deliberately fails as long as
`android-app/app/src/selfhosted/res/raw/sudomnia_client.p12` is missing (a
private key, see RELEASING.md): a missing certificate should show up while
building, not only on the device. Without `keystore.properties`, the release
build also stays unsigned.

## What matters in the code

- **`rules/` and `game/` stay free of Android imports.** That's the condition
  for correctness to be testable at all — there is no Robolectric and no
  instrumented tests.
- **Comments justify, they don't describe.** Why bitmasks and not `Set<Int>`,
  why hidden before naked single, why `BoardState` writes its `equals` by
  hand. What the code does is in the code.
- **New design decisions belong in ARCHITECTURE.md**, together with the
  discarded alternative next to them.
- English UI strings as the source, German and Spanish as translations, English
  identifiers and code comments.

## Tests

`./gradlew test -DsudokuDeep=1` runs the same tests with ten times as many
puzzles (~2,500 of them). Anyone changing the solver, generator or digger
should run this once.

Note: `rules/` exists **twice** in the repo — once in `android-app`, once in
`generator-cli`. Changes must go into both, or the CLI tool silently drifts
out of sync.
