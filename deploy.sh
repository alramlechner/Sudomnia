#!/bin/bash
# Sudomnia release script: version bump + build + drop onto the EnergyControl server.
# Usage: ./deploy.sh [--notes "release text"] [--no-bump]
#
# --no-bump  don't change the version number (e.g. if already bumped by hand)
# --notes    text for release_notes in latest.json
#
# Order as in Oystra: version.properties is committed and pushed BEFORE building.
# Otherwise a failed push could leave behind an APK whose version is in no commit.
# A failed build, conversely, only costs a skipped number, which has no consequences.
# As long as Sudomnia isn't a git repo, the whole block is skipped -- it kicks in
# automatically once it becomes one.

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
        *) echo "Unknown parameter: $1"; exit 1 ;;
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

    echo "Version bump: $CURRENT_NAME ($CURRENT_CODE) -> $NEW_NAME ($NEW_CODE)"
    # The comment lines in the file are preserved -- they explain why the version
    # lives only here and not in build.gradle.kts.
    sed -i -e "s/^VERSION_CODE=.*/VERSION_CODE=${NEW_CODE}/" \
           -e "s/^VERSION_NAME=.*/VERSION_NAME=${NEW_NAME}/" "$VERSION_FILE"
else
    NEW_CODE=$CURRENT_CODE
    NEW_NAME=$CURRENT_NAME
    echo "No version bump: staying at $NEW_NAME ($NEW_CODE)"
fi

# --- commit and push version.properties, if this is a repo --------------------------
cd "$SCRIPT_DIR"
if git rev-parse --git-dir >/dev/null 2>&1; then
    if ! git diff --quiet -- "$VERSION_FILE"; then
        echo ""
        echo "==> Committing and pushing version.properties ($NEW_NAME)..."
        # Path-scoped: other open changes in the working tree are none of the release's business.
        git commit -q -m "App: Version $NEW_NAME ($NEW_CODE)" -- "$VERSION_FILE"
        if git remote | grep -q .; then
            if ! git push -q 2>/dev/null; then
                echo "    Push rejected, trying rebase onto the current state..."
                if ! git pull -q --rebase --autostash || ! git push -q; then
                    echo ""
                    echo "ERROR: could not push version.properties."
                    echo "NOT building - otherwise an APK would result whose version is in"
                    echo "no commit. Please resolve with 'git pull' and run again."
                    exit 1
                fi
            fi
            echo "    $(git rev-parse --short HEAD) pushed"
        fi
    fi
else
    echo "(not a git repo - commit/push skipped)"
fi

echo ""
echo "==> Building APK (Java 17 is mandatory, newer JDKs break the Kotlin compiler)..."
cd "$GRADLE_DIR"
# The selfhosted flavour: only it contains the self-update that this script even
# delivers. The play flavour has neither the code nor the permissions for it --
# see app/build.gradle.kts and RELEASING.md.
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 \
  PATH=/usr/lib/jvm/java-17-openjdk-arm64/bin:$PATH \
  ANDROID_HOME=/home/pi/android-sdk \
  ./gradlew assembleSelfhostedRelease --no-daemon

APK_SRC="$GRADLE_DIR/app/build/outputs/apk/selfhosted/release/app-selfhosted-release.apk"
APK_DST="$APK_DIR/sudomnia-${NEW_NAME}.apk"

# An unsigned release can't update an installed version -- better to abort here
# than on the tablet with INSTALL_PARSE_FAILED_NO_CERTIFICATES.
if [ ! -f "$APK_SRC" ]; then
    echo "ERROR: $APK_SRC missing (keystore.properties present?)"
    exit 1
fi

echo ""
echo "==> Deploying $APK_DST..."
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
echo "  external: https://<sudomnia.updateHost>:8443/api/v1/sudomnia/app/latest.json (mTLS)"
echo "  on the LAN: http://<sudomnia.updateHost>:8082/sudomnia/app/download"
