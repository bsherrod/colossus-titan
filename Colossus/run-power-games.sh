#!/bin/bash
# Run all Lauer Power AI games as separate processes, monitor for completion/hang/crash.
# Usage: ./run-power-games.sh [POWER1 POWER2 ...]
# If no args, runs all 22 powers sequentially.

cd /Users/sherrodz/src/colossus-titan/Colossus

ALL_POWERS="ADMINISTRATOR ARCHER ARCHMAGE ARMOURER AVIAN BERSERKER DEMON THE_FOLKS HEALER KNIGHT LIVING_DEAD LORD_OF_THE_TREES MARINES MULLIGAN NINE_BALL NINJA PROPHETESS UPSIDE_DOWN_CAKE VAMPIRE YAHTZEE ZOMBIE AMOEBA"

POWERS="${@:-$ALL_POWERS}"
LOGDIR="/tmp/colossus-power-tests"
mkdir -p "$LOGDIR"
RESULTS="$LOGDIR/results.txt"
> "$RESULTS"

echo "=== Colossus Power Game Tests ==="
echo "Log dir: $LOGDIR"
echo ""

for POWER in $POWERS; do
    LOGFILE="$LOGDIR/${POWER}.log"
    PIDFILE="$LOGDIR/${POWER}.pid"
    > "$LOGFILE"

    echo -n "Starting $POWER... "

    # Launch game in background as a separate JVM
    java -cp "Colossus.jar" net.sf.colossus.tools.PowerGameRunner "$POWER" > "$LOGFILE" 2>&1 &
    PID=$!
    echo "$PID" > "$PIDFILE"
    echo "PID=$PID"

    # Monitor: check every 10s, max 10 minutes (600s)
    ELAPSED=0
    MAX_WAIT=600
    LAST_SIZE=0
    STALL_COUNT=0

    while kill -0 $PID 2>/dev/null; do
        sleep 10
        ELAPSED=$((ELAPSED + 10))

        # Check if log is growing (game is progressing)
        CURR_SIZE=$(wc -c < "$LOGFILE" 2>/dev/null || echo 0)
        if [ "$CURR_SIZE" -eq "$LAST_SIZE" ]; then
            STALL_COUNT=$((STALL_COUNT + 1))
        else
            STALL_COUNT=0
        fi
        LAST_SIZE=$CURR_SIZE

        # If log hasn't grown in 60s (6 checks), likely hung
        if [ $STALL_COUNT -ge 6 ]; then
            echo "  HUNG after ${ELAPSED}s (no log activity for 60s)"
            kill $PID 2>/dev/null
            sleep 1
            kill -9 $PID 2>/dev/null
            echo "HUNG $POWER (${ELAPSED}s, killed)" >> "$RESULTS"
            break
        fi

        # Hard timeout
        if [ $ELAPSED -ge $MAX_WAIT ]; then
            echo "  TIMEOUT after ${MAX_WAIT}s"
            kill $PID 2>/dev/null
            sleep 1
            kill -9 $PID 2>/dev/null
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
            echo "  OK (${ELAPSED}s)"
            echo "PASS $POWER (${ELAPSED}s)" >> "$RESULTS"
        else
            echo "  CRASHED (exit=$EXIT, ${ELAPSED}s)"
            echo "CRASH $POWER (exit=$EXIT, ${ELAPSED}s)" >> "$RESULTS"
        fi
    fi

    rm -f "$PIDFILE"
done

echo ""
echo "=== RESULTS ==="
cat "$RESULTS"
echo ""
echo "Details in $LOGDIR/<POWER>.log"
