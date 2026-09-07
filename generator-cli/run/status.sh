#!/bin/bash
# Zeigt Prozess-Lebendigkeit und Fortschritt je Level.
set -euo pipefail
cd "$(dirname "$0")/.."

PIDFILE="run/generator.pid"
OUTDIR="generated-puzzles"

if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "Läuft (PID $(cat "$PIDFILE"))."
else
    echo "Läuft nicht."
fi

for f in easy medium hard; do
    path="$OUTDIR/$f.txt"
    if [ -f "$path" ]; then
        echo "  $f: $(wc -l < "$path") Rätsel"
    else
        echo "  $f: 0 Rätsel (noch keine Datei)"
    fi
done

[ -f "$OUTDIR/manifest.txt" ] && echo "--- manifest.txt ---" && cat "$OUTDIR/manifest.txt"
