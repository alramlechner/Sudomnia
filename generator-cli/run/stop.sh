#!/bin/bash
# Aborts a running generator run early (clean flush via the shutdown hook in
# Main.kt). Not normally needed -- the process ends by itself once the target
# is reached.
set -euo pipefail
cd "$(dirname "$0")/.."

PIDFILE="run/generator.pid"

if [ ! -f "$PIDFILE" ] || ! kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "Not running."
    rm -f "$PIDFILE"
    exit 0
fi

kill "$(cat "$PIDFILE")"
echo "Stopped (PID $(cat "$PIDFILE"))."
rm -f "$PIDFILE"
