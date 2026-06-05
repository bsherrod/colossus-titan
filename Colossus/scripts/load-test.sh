#!/bin/bash
# load-test.sh — Load a saved game XML, optionally assign powers, run to completion
# Usage: ./load-test.sh <save.xml> [POWER_NAME]
# Exit 0 = pass (no SEVEREs), Exit 1 = fail

set -e
cd "$(dirname "$0")/.."

SAVE_FILE="$1"
POWER="${2:-}"

if [ -z "$SAVE_FILE" ]; then
    echo "Usage: $0 <save.xml> [POWER_NAME]"
    echo "  Loads saved game, runs AI to completion, checks for SEVERE errors"
    echo "  Optional POWER_NAME assigned to player 0 (e.g. MULLIGAN, NINJA)"
    exit 1
fi

if [ ! -f "$SAVE_FILE" ]; then
    echo "ERROR: Save file not found: $SAVE_FILE"
    exit 1
fi

# Build if needed
if [ ! -f build/ant/libs/Colossus.jar ]; then
    ant clean jar >/dev/null 2>&1
fi

# Set power in config if specified
if [ -n "$POWER" ]; then
    CFG="$HOME/.colossus/Colossus-server.cfg"
    sed -i '' '/Player powers 0=/d' "$CFG" 2>/dev/null || true
    echo "Player powers 0=$POWER" >> "$CFG"
fi

# Run loaded game
LOG=$(mktemp /tmp/colossus-load-XXXXXX.log)
java -jar build/ant/libs/Colossus.jar \
    -l "$SAVE_FILE" -g -q -d0 \
    2>&1 | tee "$LOG"

# Check results
SEVERES=$(grep -c "SEVERE" "$LOG" 2>/dev/null || echo "0")

if [ "$SEVERES" -gt 0 ]; then
    echo ""
    echo "FAIL: $SEVERES SEVERE errors found"
    grep "SEVERE" "$LOG" | tail -5
    rm -f "$LOG"
    exit 1
else
    echo ""
    echo "PASS: 0 SEVERE errors"
    rm -f "$LOG"
    exit 0
fi
