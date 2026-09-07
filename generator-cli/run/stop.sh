#!/bin/bash
# Bricht einen laufenden Generator-Lauf vorzeitig ab (sauberer Flush über den
# Shutdown-Hook in Main.kt). Im Normalfall nicht nötig -- der Prozess beendet sich
# selbst, sobald das Ziel erreicht ist.
set -euo pipefail
cd "$(dirname "$0")/.."

PIDFILE="run/generator.pid"

if [ ! -f "$PIDFILE" ] || ! kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "Läuft nicht."
    rm -f "$PIDFILE"
    exit 0
fi

kill "$(cat "$PIDFILE")"
echo "Beendet (PID $(cat "$PIDFILE"))."
rm -f "$PIDFILE"
