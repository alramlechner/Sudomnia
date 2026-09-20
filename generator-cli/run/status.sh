#!/bin/bash
# Shows process liveness and progress per level.
set -euo pipefail
cd "$(dirname "$0")/.."

PIDFILE="run/generator.pid"
OUTDIR="generated-puzzles"

if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    echo "Running (PID $(cat "$PIDFILE"))."
else
    echo "Not running."
fi

for f in easy medium hard; do
    path="$OUTDIR/$f.txt"
    if [ -f "$path" ]; then
        echo "  $f: $(wc -l < "$path") puzzles"
    else
        echo "  $f: 0 puzzles (no file yet)"
    fi
done

[ -f "$OUTDIR/manifest.txt" ] && echo "--- manifest.txt ---" && cat "$OUTDIR/manifest.txt"
