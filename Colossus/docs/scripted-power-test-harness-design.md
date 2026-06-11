# Scripted Power Test Harness Design

## Problem

Current tests either (a) poke at isolated methods (unit tests) or (b) run full AI games with no control (functests). Neither can verify: "a player with Administrator, holding 1 Gargoyle in Brush, recruits a Cyclops."

## Solution: Three Test Levels

### Level 1: Direct Logic Tests (existing)
- Use `GameServerSide.makeNewGameServerSide(variant)`
- Create legions, call `findEligibleRecruits`, `addCreature`, `computeNumRecruitersForClient`
- **Good for:** formula verification, guard conditions
- **Cannot:** advance phases, roll dice, simulate full turns
- **Examples:** AdministratorRecruitTest, DemonPossessionTest

### Level 2: Scripted Scenario Tests (NEW — this design)
- Use `Game` + `Movement` + `BattleServerSide` objects (no Server/Client network)
- Inject predetermined dice via `Dice.setScriptedRolls(int[])`
- Set up specific game state (player on hex, creatures in legion, battle in progress)
- Call game methods directly: `doMove`, `doRecruit`, `strike`
- Assert outcomes deterministically
- **Good for:** verifying each power behavior in isolation with exact inputs
- **Examples:** "Mulligan player rolls 3, verify teleport destinations include all hexes"

### Level 3: Full Game Smoke Tests (existing)
- LauerPowersFuncTest, GameReplayFuncTest
- Full Server+Client, AI-driven, no control
- **Good for:** no-crash regression, recording infrastructure

## Key Components for Level 2

### 1. Dice.setScriptedRolls(int[])

Add to `Dice.java`:
```java
private static int[] scriptedRolls = null;
private static int scriptedIndex = 0;

public static void setScriptedRolls(int[] rolls) {
    scriptedRolls = rolls;
    scriptedIndex = 0;
}

public static void clearScriptedRolls() {
    scriptedRolls = null;
}
```

Modify `rollDie(int size)`:
```java
public static int rollDie(int size) {
    if (scriptedRolls != null && scriptedIndex < scriptedRolls.length) {
        return scriptedRolls[scriptedIndex++];
    }
    return random.nextInt(size) + 1;
}
```

### 2. ScriptedPowerTest Base Class

```java
public abstract class ScriptedPowerTest extends TestCase {
    protected Variant variant;
    protected GameServerSide game;
    
    @Override
    protected void setUp() {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
    }
    
    @Override
    protected void tearDown() {
        Dice.clearScriptedRolls();
    }
    
    // --- Setup helpers ---
    protected Player createPlayerWithPower(String name, LauerPower... powers);
    protected LegionServerSide createLegion(Player p, String hex, CreatureType... creatures);
    protected BattleServerSide createBattle(LegionServerSide attacker, LegionServerSide defender, BattlePhase startPhase);
    
    // --- Dice control ---
    protected void scriptDice(int... rolls);  // queues dice results
    
    // --- Assertion helpers ---
    protected void assertLegionHeight(LegionServerSide legion, int expected);
    protected void assertLegionContains(LegionServerSide legion, String creatureName, int count);
    protected void assertCreaturePossessed(CreatureServerSide creature, boolean expected);
    protected void assertPhase(BattleServerSide battle, BattlePhase expected);
}
```

### 3. What This Enables (example test)

```java
public class ScriptedAdminTest extends ScriptedPowerTest {
    public void testOneGargoyleRecruitsCyclopsInBrush() {
        Player p = createPlayerWithPower("Admin", LauerPower.ADMINISTRATOR);
        LegionServerSide legion = createLegion(p, "3", // Brush hex
            variant.getCreatureByName("Titan"),
            variant.getCreatureByName("Gargoyle"));
        
        List<CreatureType> recruits = game.findEligibleRecruits(legion, 
            variant.getMasterBoard().getHexByLabel("3"));
        
        assertTrue(recruits.contains(variant.getCreatureByName("Cyclops")));
        assertLegionHeight(legion, 2);
        
        // Actually recruit
        legion.addCreature(variant.getCreatureByName("Cyclops"), true);
        assertLegionHeight(legion, 3);
        assertLegionContains(legion, "Cyclops", 1);
        assertLegionContains(legion, "Gargoyle", 1); // NOT 2!
    }
}
```

### 4. For battle tests with dice control:

```java
public void testBerserkerBonusDice() {
    Player p = createPlayerWithPower("Berserk", LauerPower.BERSERKER);
    // ... create battle, place creatures on hexes ...
    BattleServerSide battle = createBattle(attackerLeg, defenderLeg, BattlePhase.FIGHT);
    
    // Verify dice count includes +floor(str/4) bonus
    BattleStrikeServerSide strike = new BattleStrikeServerSide(game);
    CreatureServerSide ogre = getCreature(battle, "Ogre"); // power=6
    int dice = strike.getDice(ogre, target, false);
    assertEquals(6 + 6/4, dice); // 6 base + 1 berserker bonus = 7
}
```

## Implementation Tasks

1. **Dice.setScriptedRolls()** — add static roll queue to Dice.java
2. **ScriptedPowerTest base class** — setup helpers + assertion helpers
3. **Port existing unit tests** — migrate AdministratorRecruitTest etc. to use the base class (optional, existing tests work fine)
4. **Write new Level 2 tests** — one per behavior (the 76 tasks on the board)

## What We Already Have That Works

- `makeNewGameServerSide(variant)` — creates game without network
- `createBattle(attacker, defender, phase)` — sets up battle state  
- `setupMove()` — now package-private, callable from tests
- `BattleStrikeServerSide.getDice()` — testable dice calculation
- `findEligibleRecruits/findEligibleRecruiters` — testable recruit logic
- `Creature.getDefensivePower()` — testable hit points
- `Options.fixedSequenceBattleDice` — existing non-random battle dice

Most behavior tests DON'T need full dice mocking — they test formulas and eligibility. Only movement and strike outcome tests need scripted dice.
