#!/bin/bash
# test-power.sh -- Run a headless AI game with specified powers and check for errors
# Usage: ./test-power.sh POWER_NAME [POWER_NAME2...]
# Example: ./test-power.sh MULLIGAN
#          ./test-power.sh ARMOURER KNIGHT
#
# Runs 3 AI players, player 0 gets the specified powers.
# Checks output for SEVERE errors or "illegal move" messages.
# Exit code: 0 = pass, 1 = fail

set -euo pipefail
cd "$(dirname "$0")"

POWERS="${1:-}"
shift 2>/dev/null || true
# Join remaining args with comma
while [ $# -gt 0 ]; do
    POWERS="${POWERS},${1}"
    shift
done

if [ -z "$POWERS" ]; then
    echo "Usage: $0 POWER_NAME [POWER_NAME2...]"
    echo "Available: MULLIGAN, ARMOURER, KNIGHT, MARINES, YAHTZEE, etc."
    exit 2
fi

echo "Testing power(s): $POWERS"

# Set up a temporary config
TMPDIR=$(mktemp -d)
TMPCFG="$TMPDIR/Colossus-server.cfg"
cat > "$TMPCFG" << EOF
Player\ name\ 0=TestPlayer0
Player\ name\ 1=TestAI1
Player\ name\ 2=TestAI2
Player\ type\ 0=SimpleAI
Player\ type\ 1=SimpleAI
Player\ type\ 2=SimpleAI
Player\ powers\ 0=$POWERS
Balanced\ starting\ towers=true
Variant=Default
AI\ time\ limit=5
Autosave=false
EOF

# Run headless game with timeout
LOGFILE="$TMPDIR/game.log"
echo "Running headless game (timeout 60s)..."
timeout 60 java -ea \
    -Dnet.sf.colossus.stressTestRounds=1 \
    -Djava.awt.headless=true \
    -Duser.home="$TMPDIR" \
    -Xmx256M \
    -jar Colossus.jar \
    -i3 -g -q -d1 --variant Default \
    > "$LOGFILE" 2>&1 || true

# Check for errors
ERRORS=0
if grep -q "SEVERE" "$LOGFILE"; then
    echo "❌ SEVERE errors found:"
    grep "SEVERE" "$LOGFILE" | head -5
    ERRORS=1
fi
if grep -qi "illegal move" "$LOGFILE"; then
    echo "❌ Illegal move errors:"
    grep -i "illegal move" "$LOGFILE" | head -5
    ERRORS=1
fi
if grep -qi "IndexOutOfBounds\|NullPointer\|ClassCast" "$LOGFILE"; then
    echo "❌ Exceptions found:"
    grep -i "IndexOutOfBounds\|NullPointer\|ClassCast" "$LOGFILE" | head -5
    ERRORS=1
fi

if [ $ERRORS -eq 0 ]; then
    echo "✅ PASS: Game completed without errors"
    rm -rf "$TMPDIR"
    exit 0
else
    echo ""
    echo "Full log: $LOGFILE"
    echo "Config: $TMPCFG"
    exit 1
fi
