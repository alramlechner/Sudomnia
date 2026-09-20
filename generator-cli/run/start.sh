#!/bin/bash
# Starts the puzzle generator in the background at the lowest CPU/IO priority,
# so EnergyControl and every other service on the Pi always takes precedence.
set -euo pipefail
cd "$(dirname "$0")/.."

PIDFILE="run/generator.pid"
LOGFILE="run/generator.log"
# installDist produces a launcher script with the correct classpath (Kotlin stdlib
# etc.) -- prefer that over "java -jar" on a jar without a classpath.
LAUNCHER="build/install/sudomnia-generator-cli/bin/sudomnia-generator-cli"

if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "Already running (PID $(cat "$PIDFILE"))."
    exit 1
fi

if [ ! -f "$LAUNCHER" ]; then
    echo "Not built -- run './gradlew installDist' first."
    exit 1
fi

nohup nice -n 19 ionice -c 3 "$LAUNCHER" "$@" > "$LOGFILE" 2>&1 &
echo $! > "$PIDFILE"
echo "Started (PID $(cat "$PIDFILE")), log: $LOGFILE"
