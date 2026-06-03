#!/bin/bash
# test-all-powers.sh -- Run headless AI games for all implemented powers
# Tests each power individually + a multi-power combo
cd "$(dirname "$0")"

PASS=0
FAIL=0
POWERS="MULLIGAN ARMOURER YAHTZEE KNIGHT MARINES BERSERKER ARCHER NINE_BALL AVIAN VAMPIRE NINJA SMURFS HEALER UPSIDE_DOWN_CAKE PROPHETESS LORD_OF_THE_TREES ADMINISTRATOR THE_FOLKS ZOMBIE LIVING_DEAD AMOEBA"

echo "=== Testing individual powers ==="
for power in $POWERS; do
    result=$(timeout 65 ./test-power.sh $power 2>&1 | tail -1)
    if echo "$result" | grep -q "PASS"; then
        echo "  ✅ $power"
        PASS=$((PASS + 1))
    else
        echo "  ❌ $power: $result"
        FAIL=$((FAIL + 1))
    fi
done

echo ""
echo "=== Testing multi-power combos ==="
COMBOS="ARMOURER,KNIGHT,YAHTZEE MULLIGAN,BERSERKER VAMPIRE,ARCHER SMURFS,HEALER,AVIAN"
for combo in $COMBOS; do
    powers_args=$(echo $combo | tr ',' ' ')
    result=$(timeout 65 ./test-power.sh $powers_args 2>&1 | tail -1)
    if echo "$result" | grep -q "PASS"; then
        echo "  ✅ $combo"
        PASS=$((PASS + 1))
    else
        echo "  ❌ $combo: $result"
        FAIL=$((FAIL + 1))
    fi
done

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
[ $FAIL -eq 0 ] && exit 0 || exit 1
