#!/bin/bash
# Sudomnia Release-Skript: Version-Bump + Build + Ablage auf dem EnergyControl-Server.
# Usage: ./deploy.sh [--notes "release text"] [--no-bump]
#
# --no-bump  Versionsnummer nicht ändern (z.B. wenn schon manuell erhöht)
# --notes    Text für release_notes in latest.json
#
# Reihenfolge wie bei Oystra: version.properties wird committet und gepusht, BEVOR
# gebaut wird. Sonst kann ein fehlgeschlagener Push eine APK zurücklassen, deren Version
# in keinem Commit steht. Ein fehlgeschlagener Build kostet umgekehrt nur eine
# übersprungene Nummer, und das ist folgenlos. Solange Sudomnia kein Git-Repo ist, wird
# der ganze Block übersprungen -- er greift automatisch, sobald es eins wird.

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VERSION_FILE="$SCRIPT_DIR/version.properties"
GRADLE_DIR="$SCRIPT_DIR/android-app"
APK_DIR="/var/lib/sudomnia/apk"

NOTES=""
BUMP=true

while [[ $# -gt 0 ]]; do
    case "$1" in
        --notes) NOTES="$2"; shift 2 ;;
        --no-bump) BUMP=false; shift ;;
        *) echo "Unbekannter Parameter: $1"; exit 1 ;;
    esac
done

CURRENT_CODE=$(grep 'VERSION_CODE=' "$VERSION_FILE" | cut -d= -f2)
CURRENT_NAME=$(grep 'VERSION_NAME=' "$VERSION_FILE" | cut -d= -f2)

if $BUMP; then
    MAJOR=$(echo "$CURRENT_NAME" | cut -d. -f1)
    MINOR=$(echo "$CURRENT_NAME" | cut -d. -f2)
    PATCH=$(echo "$CURRENT_NAME" | cut -d. -f3)
    NEW_CODE=$((CURRENT_CODE + 1))
    NEW_NAME="${MAJOR}.${MINOR}.$((PATCH + 1))"

    echo "Version-Bump: $CURRENT_NAME ($CURRENT_CODE) -> $NEW_NAME ($NEW_CODE)"
    # Die Kommentarzeilen der Datei bleiben erhalten -- sie erklären, warum die Version
    # nur hier steht und nicht in build.gradle.kts.
    sed -i -e "s/^VERSION_CODE=.*/VERSION_CODE=${NEW_CODE}/" \
           -e "s/^VERSION_NAME=.*/VERSION_NAME=${NEW_NAME}/" "$VERSION_FILE"
else
    NEW_CODE=$CURRENT_CODE
    NEW_NAME=$CURRENT_NAME
    echo "Kein Version-Bump: bleibt $NEW_NAME ($NEW_CODE)"
fi

# --- version.properties committen und pushen, falls das hier ein Repo ist -----------
cd "$SCRIPT_DIR"
if git rev-parse --git-dir >/dev/null 2>&1; then
    if ! git diff --quiet -- "$VERSION_FILE"; then
        echo ""
        echo "==> Committe und pushe version.properties ($NEW_NAME)..."
        # Pfadbegrenzt: andere offene Änderungen im Arbeitsbaum gehen den Release nichts an.
        git commit -q -m "App: Version $NEW_NAME ($NEW_CODE)" -- "$VERSION_FILE"
        if git remote | grep -q .; then
            if ! git push -q 2>/dev/null; then
                echo "    Push abgelehnt, versuche rebase auf den aktuellen Stand..."
                if ! git pull -q --rebase --autostash || ! git push -q; then
                    echo ""
                    echo "FEHLER: version.properties konnte nicht gepusht werden."
                    echo "Es wird NICHT gebaut - sonst entstünde eine APK, deren Version in"
                    echo "keinem Commit steht. Bitte 'git pull' klären und erneut aufrufen."
                    exit 1
                fi
            fi
            echo "    $(git rev-parse --short HEAD) gepusht"
        fi
    fi
else
    echo "(kein Git-Repo - Commit/Push übersprungen)"
fi

echo ""
echo "==> Baue APK (Java 17 ist Pflicht, neuere JDKs zerlegen den Kotlin-Compiler)..."
cd "$GRADLE_DIR"
# Die selfhosted-Variante: nur sie enthaelt die Selbst-Aktualisierung, die dieses
# Skript ueberhaupt beliefert. Die play-Variante hat weder den Code noch die
# Berechtigungen dafuer -- siehe app/build.gradle.kts und RELEASING.md.
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 \
  PATH=/usr/lib/jvm/java-17-openjdk-arm64/bin:$PATH \
  ANDROID_HOME=/home/pi/android-sdk \
  ./gradlew assembleSelfhostedRelease --no-daemon

APK_SRC="$GRADLE_DIR/app/build/outputs/apk/selfhosted/release/app-selfhosted-release.apk"
APK_DST="$APK_DIR/sudomnia-${NEW_NAME}.apk"

# Ein unsigniertes Release kann keine installierte Version aktualisieren -- lieber hier
# abbrechen als auf dem Tablet mit INSTALL_PARSE_FAILED_NO_CERTIFICATES.
if [ ! -f "$APK_SRC" ]; then
    echo "FEHLER: $APK_SRC fehlt (keystore.properties vorhanden?)"
    exit 1
fi

echo ""
echo "==> Deploye $APK_DST..."
mkdir -p "$APK_DIR"
cp "$APK_SRC" "$APK_DST"
SHA=$(sha256sum "$APK_DST" | cut -d' ' -f1)

cat > "$APK_DIR/latest.json" <<EOF
{
  "version_code": ${NEW_CODE},
  "version_name": "${NEW_NAME}",
  "filename": "sudomnia-${NEW_NAME}.apk",
  "sha256": "${SHA}",
  "released_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "release_notes": "${NOTES}"
}
EOF

echo ""
echo "✓ sudomnia-${NEW_NAME}.apk deployed"
echo "  SHA256: $SHA"
echo "  latest.json: $APK_DIR/latest.json"
echo "  extern:  https://<sudomnia.updateHost>:8443/api/v1/sudomnia/app/latest.json (mTLS)"
echo "  im LAN:  http://<sudomnia.updateHost>:8082/sudomnia/app/download"
