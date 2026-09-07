#!/bin/bash
# Startet den Rätsel-Generator im Hintergrund mit niedrigster CPU-/IO-Priorität,
# damit EnergyControl und alle anderen Dienste auf dem Pi jederzeit vorgehen.
set -euo pipefail
cd "$(dirname "$0")/.."

PIDFILE="run/generator.pid"
LOGFILE="run/generator.log"
# installDist erzeugt ein Startskript mit korrektem Classpath (Kotlin-Stdlib etc.)
# -- lieber das verwenden als "java -jar" auf ein Jar ohne Klassenpfad.
LAUNCHER="build/install/sudomnia-generator-cli/bin/sudomnia-generator-cli"

if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "Läuft bereits (PID $(cat "$PIDFILE"))."
    exit 1
fi

if [ ! -f "$LAUNCHER" ]; then
    echo "Nicht gebaut -- führe zuerst './gradlew installDist' aus."
    exit 1
fi

nohup nice -n 19 ionice -c 3 "$LAUNCHER" "$@" > "$LOGFILE" 2>&1 &
echo $! > "$PIDFILE"
echo "Gestartet (PID $(cat "$PIDFILE")), Log: $LOGFILE"
