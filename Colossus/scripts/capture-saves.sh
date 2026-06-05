#!/bin/bash
# capture-saves.sh — Run AI game with autosave enabled to capture save files
# Usage: ./capture-saves.sh [num_games]
# Saves go to ~/.colossus/saves/ (Colossus autosave default location)

set -e
cd "$(dirname "$0")/.."

NUM_GAMES=${1:-3}
SAVE_DIR="$HOME/.colossus/saves"
FIXTURE_DIR="core/src/functest/resource/colossus-home/saves"

echo "=== Capture Save Mode ==="
echo "Running $NUM_GAMES AI games with autosave enabled..."
echo "Saves will appear in: $SAVE_DIR"

# Ensure autosave is enabled in server config
CFG="$HOME/.colossus/Colossus-server.cfg"
if [ -f "$CFG" ]; then
    # Set Autosave=true if not already
    if grep -q "Autosave=" "$CFG"; then
        sed -i '' 's/Autosave=.*/Autosave=true/' "$CFG"
    else
        echo "Autosave=true" >> "$CFG"
    fi
fi

# Count existing saves
BEFORE=$(ls "$SAVE_DIR"/*.xml 2>/dev/null | wc -l | tr -d ' ')

for i in $(seq 1 "$NUM_GAMES"); do
    echo "--- Game $i of $NUM_GAMES ---"
    # Run short AI game: 2 players, headless, quick delay
    java -jar build/ant/libs/Colossus.jar \
        -i2 -g -q -d0 \
        --Autosave=true \
        2>&1 | grep -E "SEVERE|autosave|Saving" | tail -5
    echo "  Game $i complete."
done

# Count new saves
AFTER=$(ls "$SAVE_DIR"/*.xml 2>/dev/null | wc -l | tr -d ' ')
NEW=$((AFTER - BEFORE))

echo ""
echo "=== Results ==="
echo "New save files captured: $NEW"
echo "Total saves in $SAVE_DIR: $AFTER"
echo ""
echo "To use as test fixtures, copy interesting saves:"
echo "  cp $SAVE_DIR/<file>.xml $FIXTURE_DIR/"
