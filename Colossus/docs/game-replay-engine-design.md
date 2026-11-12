# Colossus Game Notation & Replay Test Harness Design

## Problem

We have two incomplete replay systems:
1. **XML Redo** — handles masterboard actions (Split, Move, Recruit) through full server→client protocol. No battle events.
2. **JSON GameRecorder** — captures ALL client→server messages (including battle). No replay engine.

Neither provides: "play to event N, then assert game state."

## Goal: Complete Game Notation + Test Harness

A single format that captures every game decision, from start to finish, and can be replayed
deterministically through the real server→client protocol with assertions at any point.

## Design Decision: Extend JSON (not XML)

**Why JSON over XML:**
- GameRecorder already captures ALL events (50+ action types including all battle actions)
- JSON is easier to read/write/parse in tests
- XML Redo would need 20+ new event types added to History.java
- JSON format is append-only (simple to generate during gameplay)

**The gap to close:** Build a replay engine that feeds JSON events back through `server.doXxx()`,
just like `History.processRedoLog()` does for XML.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  GameRecorder (already exists, always-on)                            │
│  Captures: method, args, player, turn, phase for every callMethod()  │
│  Output: ~/.colossus/recordings/game-<timestamp>.json                │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ produces
┌─────────────────────────────────────────────────────────────────────┐
│  Recording JSON                                                      │
│  {header: {variant, players[], powers[]},                            │
│   events: [{seq, turn, phase, player, method, args}, ...]}           │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ consumed by
┌─────────────────────────────────────────────────────────────────────┐
│  GameReplayEngine (NEW)                                              │
│                                                                      │
│  1. Creates Server + Clients from header (same as Start.main)        │
│  2. Feeds events one-by-one through server.doXxx() methods           │
│  3. After each event: optional assertion callback fires              │
│  4. Can pause at any seq number for state inspection                 │
│  5. Provides handles to Server, Game, Client objects for assertions   │
└─────────────────────────────────────────────────────────────────────┘
```

## Event → Server Method Mapping

The replay engine maps JSON `method` field to server calls:

| JSON method | Server call | Category |
|-------------|-------------|----------|
| `doSplit` | `server.doSplit(parent, childId, creatures)` | Masterboard |
| `doMove` | `server.doMove(legion, hex, side, teleport, lord)` | Masterboard |
| `doRecruit` | `server.doRecruit(recruitment)` | Masterboard |
| `doneWithSplits` | `server.doneWithSplits()` | Phase advance |
| `doneWithMoves` | `server.doneWithMoves()` | Phase advance |
| `doneWithRecruits` | `server.doneWithRecruits()` | Phase advance |
| `engage` | `server.engage(attacker, defender)` | Battle start |
| `fight` | `server.fight(hex)` | Battle start |
| `flee` | `server.flee(legion)` | Battle start |
| `doNotFlee` | `server.doNotFlee(legion)` | Battle start |
| `concede` | `server.concede(legion)` | Battle |
| `doBattleMove` | `server.doBattleMove(creature, hex)` | Battle |
| `doneWithBattleMoves` | `server.doneWithBattleMoves()` | Battle |
| `strike` | `server.strike(creature, target)` | Battle |
| `doneWithStrikes` | `server.doneWithStrikes()` | Battle |
| `applyCarries` | `server.applyCarries(target)` | Battle |
| `leaveCarryMode` | `server.leaveCarryMode()` | Battle |
| `declareBerserk` | `server.declareBerserk(creature)` | Battle |
| `doneWithBerserkDeclarations` | `server.doneWithBerserkDeclarations()` | Battle |
| `acquireAngel` | `server.acquireAngel(legion, creature)` | Post-battle |
| `doSummon` | `server.doSummon(legion, donor, creature)` | Post-battle |
| `mulligan` | `server.mulligan(player, choice)` | Special |

## Dice Determinism

For replay to be deterministic, dice must produce the same results:

**Option A: Record dice results in the JSON** (preferred)
- Extend GameRecorder to also capture each `Dice.rollDie()` result
- Replay engine feeds these via `Dice.setScriptedRolls()`
- Advantage: exact reproduction regardless of Random seed

**Option B: Record Random seed**
- Store `Dice.random` seed at game start
- Reset seed on replay
- Disadvantage: any code change that adds/removes a roll breaks all recordings

**Recommendation: Option A.** Add a `dice` field to strike/movement events that records
the actual roll values. The replay engine queues them before each event.

## Assertion Harness

```java
public class GameReplayTest extends TestCase {
    
    private GameReplayEngine engine;
    
    public void testAdministratorPhantomBug() {
        engine = GameReplayEngine.load("recordings/admin-recruit-brush.json");
        
        // Play to event 47 (the doRecruit call)
        engine.advanceTo(47);
        
        // Assert server-side state
        LegionServerSide legion = engine.getLegion("Rd04");
        assertEquals(5, legion.getHeight());
        
        // Assert protocol value
        int numRecruiters = engine.getGame().computeNumRecruitersForClient(
            legion, gargoyle, cyclops);
        assertEquals(1, numRecruiters); // NOT 2
        
        // Assert client-side state (if clients are running)
        Client client = engine.getClient("admin");
        LegionClientSide clientLegion = client.getLegion("Rd04");
        assertEquals(5, clientLegion.getHeight()); // NOT 6 (no phantom)
    }
    
    public void testBerserkerPhaseOnTurn2() {
        engine = GameReplayEngine.load("recordings/berserker-battle.json");
        
        // Advance to first battle, turn 2
        engine.advanceToPhase(BattlePhase.FIGHT, 2); // battle turn 2
        
        // The phase BEFORE fight should have been BERSERK
        assertEquals(BattlePhase.BERSERK, engine.getPreviousPhase());
    }
}
```

### Assertion Helpers

```java
// State inspection
engine.getLegion(markerId)               → LegionServerSide
engine.getCreature(legionId, index)      → CreatureServerSide  
engine.getClient(playerName)             → Client
engine.getGame()                         → GameServerSide
engine.getBattle()                       → BattleServerSide

// Navigation
engine.advanceTo(seqNumber)              → play to specific event
engine.advanceToMethod(methodName)       → play to next occurrence of method
engine.advanceToPhase(phase)             → play to phase change
engine.advanceToTurn(turnNumber)         → play to start of turn
engine.advanceAll()                      → play entire game

// Protocol inspection (what was sent to clients)
engine.getLastProtocolMessage(clientName, messageType) → args[]
engine.getProtocolMessages(clientName, messageType)    → List<args[]>
```

## Extending XML Redo (for backward compatibility)

Add these event types to `History.java` so XML saves also capture battle state:

```xml
<Redo>
  <MovementRoll playerName="admin" roll="5" />
  <Split parentId="Rd01" childId="Rd04" turn="1">
    <splitoffs><creature>Ogre</creature><creature>Ogre</creature></splitoffs>
  </Split>
  <Move markerId="Rd04" newHex="9" entrySide="RIGHT" teleport="false" />
  <Recruit markerId="Rd04" recruit="Cyclops" recruiter="Gargoyle" />
  
  <!-- NEW: Battle events -->
  <Engage attackerId="Rd04" defenderId="Bu01" />
  <DoNotFlee markerId="Bu01" />
  <BattleMove creatureTag="5" hex="C3" />
  <BattleMove creatureTag="8" hex="D3" />
  <DoneWithBattleMoves />
  <DeclareBerserk creatureTag="5" />
  <DoneWithBerserkDeclarations />
  <Strike strikerTag="5" targetTag="8" dice="4,5,6,2,3,6,1" hits="3" />
  <DoneWithStrikes />
  <Strike strikerTag="8" targetTag="5" dice="3,4,5,6,2,1,4,5" hits="4" />
  <DoneWithStrikes />
  <!-- battle continues... -->
</Redo>
```

The `dice` attribute on Strike records actual roll values for determinism.

## Implementation Plan

### Phase 1: Enhance GameRecorder (capture dice)
1. Add dice roll capture to GameRecorder (each `Dice.rollDie()` appends to a per-event buffer)
2. Flush buffer with each event as a `dice: [4,5,6,2,3,6,1]` field
3. Verify recordings now include dice values

### Phase 2: Build JSON Replay Engine
1. `GameReplayEngine.load(path)` — parse JSON, create Server+Clients from header
2. `engine.advanceTo(seq)` — feed events through server methods up to seq N
3. `engine.getGame()` / `engine.getClient()` — expose handles for assertions
4. Before each event with dice: call `Dice.setScriptedRolls(event.dice)`

### Phase 3: Add Battle Events to XML Redo
1. Add `battleMoveEvent()`, `strikeEvent()`, `engageEvent()` to History.java
2. Add corresponding `fireEventFromElement()` cases for each
3. Hook into BattleServerSide to emit these during gameplay
4. `processRedoLog()` handles them like existing events

### Phase 4: Assertion Helpers + Test Migration
1. Build the assertion helpers on `GameReplayEngine`
2. Port existing Level 1/2 tests to use replay where it adds value
3. Write new tests for complex scenarios (multi-turn battles, phase interactions)

## Test Workflow with Replay

1. **Play a game** (human or AI) — recording is automatic
2. **Spot a bug** — note the turn/event where it occurs
3. **Copy the recording** to `core/src/functest/resource/recordings/`
4. **Write test**: load recording, `advanceTo(bugEvent)`, assert expected vs actual
5. **Fix the bug** — test goes from FAIL to PASS
6. **Recording is permanent regression test** — any future change that reintroduces the bug fails

## Comparison to Chess Notation

| Chess (PGN) | Colossus (JSON Recording) |
|-------------|---------------------------|
| Starting position | `header.players[]` + `header.variant` |
| Each move | Each `event` (doMove, doSplit, strike, etc.) |
| Clock times | `event.seq` + timestamps |
| Result | Final event (player elimination / game over) |
| Annotations | Could add `comment` field to events |
| Variations | Could fork recordings at any point |

The JSON recording IS Colossus notation. The missing piece is the replay engine.
