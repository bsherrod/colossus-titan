# Colossus Power Testing Strategy

## The Problem We Kept Hitting

Formula tests pass while powers are unplayable. Examples:
- Nine Ball: `getMaxLegionHeight()` returns 9 ✓ — but game crashes on turn-1 split because extras aren't added and turn-1 guard rejects non-4 splits
- Administrator: `findEligibleRecruits` returns correct list ✓ — but client shows phantom creatures because `numRecruiters` sent over protocol is wrong
- All powers: `LauerPowersFuncTest` "passes" ✓ — but it uses `startupTest=true` which exits after 2 seconds without ever playing a turn

**The lesson:** A power is "working" only if a player can start a game with that power and play multiple turns without crashing, hanging, or seeing incorrect state.

## Testing Levels

### Level 1: Full-Game Smoke (THE MINIMUM — every power must have this)

A real AI game that plays to completion (or at least 3+ turns) with the power active. No `startupTest`. No `autoStop`. The game must survive:

1. Game start (power-specific creature setup)
2. Turn 1 split (power-specific: Nine Ball 12→6+6, Amoeba 12→4+4+4)
3. Movement (power-specific: teleport, pass-through, etc.)
4. Recruiting (power-specific: -1 recruiter, dead pile, etc.)
5. At least one battle (phase transitions, strike bonuses)
6. Game completion without SEVERE errors

**Infrastructure:** `GameRecorderFuncTest` pattern:
```java
Options.setStartupTest(false);
Options.autoStop = false;
Options.autoPlay = true;
Options.autoPickColor = true;
// Powers assigned to player 0
// Game runs to completion (~60-120 seconds)
// Assert: no SEVERE errors in ErrorUtils
```

**If the game crashes or hangs, the power is broken.** Period.

### Level 2: Formula Verification (confirms specific values are correct)

Direct logic tests using `makeNewGameServerSide`. Good for:
- Strike dice calculations (getDice)
- Defensive power (getDefensivePower)
- Recruit eligibility (findEligibleRecruits)
- Guard conditions (canFlee, canTitanTeleport, minSplitSize)

**These are INSUFFICIENT alone.** A formula can be correct while the power is broken.

### Level 3: Protocol-Value Verification

Tests that verify what the server SENDS to clients is correct. Catches:
- Phantom creature bugs (wrong numRecruiters)
- Wrong phase announcements
- Stale client state

**Pattern:** Call the same computation the server uses in its protocol code, assert it matches game logic.

### Level 4: Recording Replay (for specific bug reproduction)

Load a game recording, replay to a specific event, assert state. Used AFTER a human finds a bug — not for proactive testing.

## What "Power Works" Means (Acceptance Criteria)

For EACH of the 22 powers, ALL must be true:

| Check | How to verify |
|-------|---------------|
| Game starts | Full-game functest doesn't crash during setup |
| Turn 1 split works | AI successfully splits (recording has doSplit events) |
| Movement works | AI moves legions (recording has doMove events) |
| Power-specific recruit works | AI recruits using the power (or recording shows recruit events) |
| Battles work | Game has engage/fight events in recording |
| Game completes | Game reaches natural end (game over) without timeout |
| No SEVERE errors | ErrorUtils.checkErrorDuringFunctionalTest() is false |

## Per-Power Specific Checks

| Power | Must verify |
|-------|-------------|
| Administrator | Recruit with -1 works; pre-battle recruit dialog for human |
| Amoeba | Starts with 3 legions of 4; all-legions-must-die; marker fights |
| Archer | Non-rangestrike creatures can rangestrike; fire twice |
| Archmage | Titan rangestrikes with magic missile; teleport on 5; fire spell |
| Armourer | Extra hits formula correct |
| Avian | All creatures fly; hover works; OGG phase |
| Berserker | BERSERK phase fires; bonus dice; double-strike-then-die |
| Demon | Auto-possess; bonus dice; damage absorb; death jump |
| Folks | Weakest recruit; remove legion; extra battle creature |
| Healer | HEAL phase; healing strikes; overheal temp hits |
| Knight | Crossbow once in strikeback; melee bonus |
| Living Dead | Dead pile recruit; corpses persist; revive enemy |
| Lord of Trees | Phantom recruiter; sapling; regen in trees |
| Marines | No terrain penalty; melee bonus; extra hits; bog entry |
| Mulligan | Forced 6,6 turn 1; dual dice; average damage |
| Nine Ball | Start with 12 creatures; split freely on turn 1; max 9 stacking |
| Ninja | Pass-through; split-1; merge; death strike; ninja move |
| Prophetess | View legions; split after roll; recruit anything; roll twice |
| UDC | 7-roll movement; 7-minus dice |
| Vampire | No flee; half points; overrun 2.5x; Titan formula |
| Yahtzee | Reroll strikes; recruitment reroll |
| Zombie | Dead pile recruit; discard before move; raise dead; Titan survives |

## Implementation: The Correct Functest

```java
public class PowerGameplayFuncTest extends TestCase {
    
    private void runRealGameWithPower(String power) {
        Options.setStartupTest(false);
        Constants.END_AFTER_FIRST_BATTLE = false;
        ErrorUtils.clearErrorDuringFunctionalTest();
        
        Options startOptions = new Options(Constants.OPTIONS_START, true);
        startOptions.setOption(Options.autoPickColor, true);
        startOptions.setOption(Options.autoPlay, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);
        
        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            CUSTOM_PATH, false, true);
        serverOptions.loadOptions();
        serverOptions.setOption(Options.autoQuit, true);
        serverOptions.setOption(Options.autoStop, false);
        serverOptions.setOption(Options.playerPowers + "0", power);
        
        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);
        game.startNewGameAndWaitUntilOver(null);
        
        assertFalse("Game with " + power + " had errors",
            ErrorUtils.checkErrorDuringFunctionalTest());
        
        // Verify the game actually played
        String recording = GameRecorder.getLastRecordingPath();
        GameReplay replay = GameReplay.load(recording);
        assertTrue(power + " game must have doSplit events",
            replay.findEvents("doSplit").size() > 0);
        assertTrue(power + " game must span multiple turns",
            replay.getMaxTurn() > 2);
    }
    
    public void testNineBall() { runRealGameWithPower("NINE_BALL"); }
    public void testAdministrator() { runRealGameWithPower("ADMINISTRATOR"); }
    public void testArcher() { runRealGameWithPower("ARCHER"); }
    // ... one per power
}
```

## Known Gotchas

- **startupTest=true** → game exits in 2s without playing. NEVER use for real tests.
- **autoStop=true** (default) → exits immediately when only AI remain. Must set false.
- **Movement roll:** `GameServerSide.getMovementRoll()` delegates to active player. Use `((PlayerServerSide)player).setMovementRoll(N)` in unit tests.
- **doSplit needs Server:** Can't call in unit test (NPE on `server.allUpdatePlayerInfo`). Test guards directly instead.
- **Creature point value:** power × skill (not just power). Lion = 5×3 = 15, not 12.
- **Turn-1 guards:** Split enforces size=4 and 1 lord per stack. Powers that start differently (Nine Ball, Amoeba) must bypass this.
- **Dice timing:** Rolls happen server-side between callMethod events. Use seed-based replay (`Dice.setReplaySeed(seed)`) for determinism.

## Recording Infrastructure (for bug reproduction)

- `GameRecorder` — always-on JSON recording in `ClientHandler.callMethod()`
- `Dice.captureSeed()` / `Dice.setReplaySeed(long)` — deterministic replay
- `GameReplayEngine` — loads recording, runs game with same seed
- Recording path: `~/.colossus/recordings/game-<timestamp>.json`
- Test resource recordings: `core/src/functest/resource/recordings/`
