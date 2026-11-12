#!/bin/bash
# Run all Lauer Power AI games to COMPLETION (not END_AFTER_FIRST_BATTLE).
# Saves full logs to a test corpus directory.
# Usage: ./run-full-games.sh [POWER1 POWER2 ...]

cd /Users/sherrodz/src/colossus-titan/Colossus

ALL_POWERS="ADMINISTRATOR ARCHER ARCHMAGE ARMOURER AVIAN BERSERKER DEMON THE_FOLKS HEALER KNIGHT LIVING_DEAD LORD_OF_THE_TREES MARINES MULLIGAN NINE_BALL NINJA PROPHETESS UPSIDE_DOWN_CAKE VAMPIRE YAHTZEE ZOMBIE AMOEBA"

POWERS="${@:-$ALL_POWERS}"
LOGDIR="/Users/sherrodz/src/colossus-titan/Colossus/test-corpus"
mkdir -p "$LOGDIR"
RESULTS="$LOGDIR/results.txt"
> "$RESULTS"

echo "=== Full AI Game Corpus ==="
echo "Log dir: $LOGDIR"
echo "Games run to COMPLETION (elimination victory)"
echo ""

for POWER in $POWERS; do
    LOGFILE="$LOGDIR/${POWER}.log"
    > "$LOGFILE"

    echo -n "Starting $POWER... "

    # Launch game in background — full game to completion
    java -cp "Colossus.jar" net.sf.colossus.tools.PowerGameRunner "$POWER" > "$LOGFILE" 2>&1 &
    PID=$!

    # Monitor: check every 30s, max 20 minutes (full games can be long)
    ELAPSED=0
    MAX_WAIT=1200
    LAST_SIZE=0
    STALL_COUNT=0

    while kill -0 $PID 2>/dev/null; do
        sleep 30
        ELAPSED=$((ELAPSED + 30))

        # Check if log is growing
        CURR_SIZE=$(wc -c < "$LOGFILE" 2>/dev/null || echo 0)
        if [ "$CURR_SIZE" -eq "$LAST_SIZE" ]; then
            STALL_COUNT=$((STALL_COUNT + 1))
        else
            STALL_COUNT=0
        fi
        LAST_SIZE=$CURR_SIZE

        # If log hasn't grown in 120s (4 checks), likely hung
        if [ $STALL_COUNT -ge 4 ]; then
            echo "  HUNG after ${ELAPSED}s (no log activity for 120s)"
            kill $PID 2>/dev/null; sleep 1; kill -9 $PID 2>/dev/null
            echo "HUNG $POWER (${ELAPSED}s, killed)" >> "$RESULTS"
            break
        fi

        # Hard timeout
        if [ $ELAPSED -ge $MAX_WAIT ]; then
            echo "  TIMEOUT after ${MAX_WAIT}s"
            kill $PID 2>/dev/null; sleep 1; kill -9 $PID 2>/dev/null
            echo "TIMEOUT $POWER (${MAX_WAIT}s)" >> "$RESULTS"
            break
        fi

        echo -n "."
    done

    # Check exit status if process ended naturally
    if ! kill -0 $PID 2>/dev/null; then
        wait $PID
        EXIT=$?
        if [ $EXIT -eq 0 ]; then
            # Count events from recording
            EVENTS=$(grep -c "\"seq\"" "$LOGFILE" 2>/dev/null || echo "?")
            echo "  OK (${ELAPSED}s)"
            echo "PASS $POWER (${ELAPSED}s)" >> "$RESULTS"
        else
            echo "  CRASHED (exit=$EXIT, ${ELAPSED}s)"
            echo "CRASH $POWER (exit=$EXIT, ${ELAPSED}s)" >> "$RESULTS"
        fi
    fi
done

echo ""
echo "=== RESULTS ==="
cat "$RESULTS"
echo ""
echo "Full logs in $LOGDIR/<POWER>.log"
echo "Recordings in ~/.colossus/recordings/"
