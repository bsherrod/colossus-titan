# Scripted Power Test Harness Design

## Problem

Current tests either (a) poke at isolated methods (unit tests) or (b) run full AI games
that never actually play (startupTest=true, exits after 2 seconds of setup). Neither can
verify: "a player with Administrator, holding 1 Gargoyle in Brush, recruits a Cyclops and
the client does NOT get a phantom Gargoyle."

## Solution: Two Test Levels

### Level 1: Direct Logic Tests (unit tests, existing)
- Use `GameServerSide.makeNewGameServerSide(variant)`
- Create legions, call `findEligibleRecruits`, `getDice`, `getDefensivePower`
- **Good for:** formula verification, guard conditions, server-side logic
- **Cannot:** verify what clients receive, advance through full game phases
- **Examples:** AdministratorRecruitTest, ArcherPowerTest, KnightPowerTest

### Level 2: Game Recording Replay (NEW — "Colossus Notation")

Like chess PGN notation, a JSON recording captures the **complete game** — every player
action, every dice roll — and can be replayed through the real server+client protocol
with assertions at any point.

**The workflow:**
1. Run a real AI game → recording is captured automatically (GameRecorder, always-on)
2. When a bug is reported → copy the recording to test resources
3. Write test: load recording, advance to event N, assert state
4. Fix bug → test goes from FAIL to PASS
5. Recording is permanent regression test

**What the recording captures:**
- Header: variant, players, powers, colors
- Every client→server command: doSplit, doMove, doRecruit, doBattleMove, strike, etc.
- Dice rolls for each event (via Dice.DiceRollListener callback)
- Turn, phase, player for each event

**What the replay engine does:**
1. Creates Server + Clients from recording header
2. Feeds events one-by-one through `server.doXxx()` (same as ClientHandler.callMethod)
3. Before each event: `Dice.setScriptedRolls(event.dice)` for determinism
4. Can pause at any event for state inspection
5. Provides handles to Server, Game, and Client objects for assertions

**Good for:**
- Protocol-value bugs (server sends wrong data to client)
- Phase ordering bugs (BERSERK fires at wrong time)
- Multi-step scenarios (split → move → recruit → battle)
- Any bug a human player can trigger (just use their recording)

**Example test:**
```java
public void testAdministratorNoPhantom() {
    GameReplayEngine engine = GameReplayEngine.load(
        "recordings/admin-recruit-brush.json");
    
    engine.advanceToMethod("doRecruit");  // play to the recruit action
    
    // Assert server state
    LegionServerSide legion = engine.getLegion("Rd04");
    assertEquals(5, legion.getHeight());
    
    // Assert protocol value (what client receives)
    int numRecruiters = engine.getGame().computeNumRecruitersForClient(
        legion, gargoyle, cyclops);
    assertEquals(1, numRecruiters);  // NOT 2 (phantom bug)
    
    // Assert client state
    assertEquals(5, engine.getClientLegionHeight("admin", "Rd04"));
}
```

### Level 3: Full Game Smoke Tests (existing)
- LauerPowersFuncTest (startupTest=true — only verifies setup doesn't crash)
- GameReplayFuncTest (verifies recordings are produced)
- **NOT actual gameplay tests** — just safety nets

## Implementation Status

### Done ✅
- `Dice.setScriptedRolls(int...)` — queue dice results for determinism
- `Dice.DiceRollListener` — callback on every roll (for recording)
- `GameRecorder` — always-on JSON recording with dice capture
- Recording hook in `ClientHandler.callMethod()`
- `GameRecorder.stop()` moved after `waitUntilGameFinishes()` (timing fix)

### Critical Discovery: Why Functest Recordings Were Empty
- `LauerPowersFuncTest` sets `Options.setStartupTest(true)`
- This causes `kickstartGame()` to exit after 2 seconds WITHOUT PLAYING
- All 18 "passing" functests only test that game SETUP doesn't crash
- Games never reach split/move/recruit/battle — hence empty recordings
- Fix: use `END_AFTER_FIRST_BATTLE=true` system property for short real games

### To Build (Phase 2)
1. `GameRecorderFuncTest` — runs real AI games with `endAfterFirstBattle=true`
   - All AI players, autoPlay=true, autoPickColor=true
   - Produces recordings with actual gameplay (doSplit, doMove, doRecruit, strike)
   - Verifies recordings have 50+ events including battle actions
2. `GameReplayEngine.java` — the replay engine
   - `load(path)` → parse JSON, create Server+Clients
   - `advanceTo(seq)` → feed events through server.doXxx() up to seq N
   - `advanceToMethod(name)` → play to next occurrence of method
   - `getGame()`, `getClient(name)`, `getLegion(id)` → state inspection
3. First replay-based regression tests for known bugs

## Key Architecture Details

### Recording Format (JSON)
```json
{
  "header": {
    "version": 1,
    "variant": "Default",
    "players": [
      {"name": "Red", "type": "RationalAI", "powers": "ADMINISTRATOR"}
    ]
  },
  "events": [
    {"seq": 1, "turn": 1, "phase": "Split", "player": "Red",
     "method": "doSplit", "args": ["Rd01", "Rd04", "Ogre,Ogre"],
     "dice": []},
    {"seq": 2, "turn": 1, "phase": "Move", "player": "Red",
     "method": "doMove", "args": ["Rd04", "9", "RIGHT", "false", "null"],
     "dice": [4, 2]},
    {"seq": 3, "turn": 1, "phase": "Muster", "player": "Red",
     "method": "doRecruit", "args": ["Rd04", "Cyclops", "Gargoyle"],
     "dice": []}
  ]
}
```

### Event → Server Method Dispatch
| JSON method | Server call |
|-------------|-------------|
| `doSplit` | `server.doSplit(parent, childId, creatures)` |
| `doMove` | `server.doMove(legion, hex, side, teleport, lord)` |
| `doRecruit` | `server.doRecruit(recruitment)` |
| `doneWithSplits` | `server.doneWithSplits()` |
| `doneWithMoves` | `server.doneWithMoves()` |
| `engage` | `server.engage(attacker, defender)` |
| `fight`/`flee` | `server.fight(hex)` / `server.flee(legion)` |
| `doBattleMove` | `server.doBattleMove(creature, hex)` |
| `strike` | `server.strike(creature, target)` |
| `doneWithBattleMoves` | `server.doneWithBattleMoves()` |
| `doneWithStrikes` | `server.doneWithStrikes()` |
| `declareBerserk` | `server.declareBerserk(creature)` |

### Known Gotchas
- **Movement roll:** `GameServerSide` overrides `getMovementRoll()` to read from active
  player. In tests: `((PlayerServerSide)player).setMovementRoll(N)`.
- **startupTest:** Must be FALSE for real games. TRUE means game exits in 2s.
- **endAfterFirstBattle:** System property to limit game length for tests.
- **autoPlay + autoPickColor:** Required for headless AI games (no GUI dialogs).
- **All-AI config:** Functest config must NOT have Human players for headless runs.

## Level 1 Primitives (still valid for direct logic tests)

- `GameServerSide.makeNewGameServerSide(variant)` — game without network
- `game.createAndAddPlayer(name, type) + player.getPowers().addPower(...)` 
- `new LegionServerSide(marker, null, hex, hex, player, game, creatures...)`
- `game.createBattle(attacker, defender, LegionTags.ATTACKER, hex, phase)`
- `creature.setCurrentHex(terrain.getHexByLabel("B3"))`
- `game.findEligibleRecruits(legion, hex)`
- `new BattleStrikeServerSide(game).getDice(striker, target, rangestrike)`
- `creature.getDefensivePower()`
- `battle.findTargetHexes(creature, rangestrike)`
- `Dice.setScriptedRolls(int...)` / `Dice.clearScriptedRolls()`
