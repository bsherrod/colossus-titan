# Lauer Powers Implementation Plan

## Instructions for Agents

**This is the living design document for the Lauer Powers feature.** Any coding agent working on this feature should:

1. **Read this file first** before making any changes.
2. **Check the "Current Status" section** to see what phase we're in and what's done.
3. **Work through phases sequentially.** Do not skip ahead.
4. **Update this file** after completing work:
   - Mark completed tasks with `[x]`
   - Record any design decisions made during implementation
   - Note any new questions or issues discovered
   - Update "Current Status" section
5. **Key source files** are listed in "File Inventory" at the bottom.
6. **The powers are defined in** `lauerpowers.html` in the project root — refer to it for exact rule text.
7. **Build with** `ant` from the project root (see `build.xml`). Run with `./run`.
8. **Test approach:** Unit tests go in `core/src/test/java/...`. The existing test in `core/src/functest/` shows integration test patterns.
9. **Java version:** 21 (Corretto). Code style: match existing codebase (no generics diamond inference, explicit types, Javadoc on public methods).
10. **After each phase:** Run `ant` to verify compilation. Run `ant test` to verify tests pass. Fix any failures before marking phase complete.

---

## Current Status

**Phase:** ALL PHASES COMPLETE ✅
**Status:** DONE — all 6 phases implemented
**Last Updated:** 2026-05-29
**Blocking Questions:** None

---

## Design Decisions (Global)

These decisions apply across all phases:

1. **Power interactions:** Contradictory power combinations (e.g., Amoeba + Vampire) are allowed in the UI but produce undefined behavior. Do not prevent selection. Document in tooltips that some combinations conflict.

2. **Power balance:** Not a concern. These are optional fun rules, not competitive balance.

3. **Partial implementation:** Yes — ship each phase independently. Powers that aren't yet implemented simply have no effect. The UI shows all powers from Phase 1 onward.

4. **Power descriptions in UI:** Show the full text from `lauerpowers.html` in a scrollable text area. Parse the HTML to extract per-power descriptions.

5. **Network play:** Not supported. Powers are local-game only. Store in Options for persistence but don't worry about socket sync.

6. **AI support:** AI players can have powers assigned. Passive powers work automatically. Active-choice powers use simple heuristics (documented per power). AI doesn't need to play optimally, just legally.

---

## Overview

Add the "Erik Lauer" Titan Powers as optional rules to the Colossus game. Players select powers during game setup; each player may have zero or more powers. Powers modify game mechanics (movement, recruiting, combat, game structure) for the player who holds them.

Source: `lauerpowers.html` in the project root.

---

## Powers Summary (23 total)

| # | Power | Category | Complexity |
|---|-------|----------|-----------|
| 1 | Administrator | Recruiting | Medium |
| 2 | Amoeba | Game Structure + Combat | High |
| 3 | Archer | Combat (rangestrike) | Medium |
| 4 | Archmage | Combat + Movement | Medium |
| 5 | Armourer | Combat (passive) | Low |
| 6 | Avian | Combat (movement + hover + ogg) | High |
| 7 | Berserker | Combat (strike + active choice) | Medium |
| 8 | Demon | Game Structure + Combat | High |
| 9 | The Folks | Recruiting + Combat | High |
| 10 | Healer | Combat (new phase) | Medium |
| 11 | Knight | Combat (rangestrike + melee) | Low |
| 12 | Living Dead | Recruiting + Combat | High |
| 13 | Lord of the Trees | Recruiting + Combat (terrain) | High |
| 14 | Marines | Combat (passive) | Low |
| 15 | Mulligan | Movement + Combat | Medium |
| 16 | Nine Ball | Game Structure + Combat | High |
| 17 | Ninja | Movement + Combat + Game Structure | High |
| 18 | Prophetess | Recruiting + Combat + Visibility | High |
| 19 | Smurfs | Movement + Combat + Game Structure | High |
| 20 | Upside-Down Cake | Movement + Combat | Low |
| 21 | Vampire | Movement + Combat + Game Structure | High |
| 22 | Yahtzee | Combat (dice) | Low |
| 23 | Zombie | Recruiting + Combat + Game Structure | High |

---

## Implementation Phases

### Phase 1: Data Model & Power Selection UI

**Goal:** Define powers, add UI to select them per player, store in Options, pass to Player objects.

**Verification:** `ant` compiles cleanly. `ant test` passes. Manual: launch game, see Powers tab, select powers, start game.

#### Tasks

- [x] 1.1 **Create `LauerPower` enum** in `net.sf.colossus.game`
  - Constants: `ADMINISTRATOR`, `AMOEBA`, `ARCHER`, `ARCHMAGE`, `ARMOURER`, `AVIAN`, `BERSERKER`, `DEMON`, `THE_FOLKS`, `HEALER`, `KNIGHT`, `LIVING_DEAD`, `LORD_OF_THE_TREES`, `MARINES`, `MULLIGAN`, `NINE_BALL`, `NINJA`, `PROPHETESS`, `SMURFS`, `UPSIDE_DOWN_CAKE`, `VAMPIRE`, `YAHTZEE`, `ZOMBIE`
  - Fields: `displayName` (String), `description` (short one-liner)
  - Static method: `LauerPower[] getAll()` returns `values()`
  - `toString()` returns `displayName`

- [x] 1.2 **Create `PlayerPowers` class** in `net.sf.colossus.game`
  - Wraps `EnumSet<LauerPower>`
  - `boolean hasPower(LauerPower p)`
  - `Set<LauerPower> getPowers()`
  - `void addPower(LauerPower p)` / `void removePower(LauerPower p)`
  - `String serialize()` → comma-separated enum names (e.g., `"ARCHER,HEALER,NINJA"`)
  - `static PlayerPowers deserialize(String s)` → parses back; empty string = no powers
  - `boolean isEmpty()`

- [x] 1.3 **Add `PlayerPowers` field to `Player` class**
  - `private PlayerPowers powers = new PlayerPowers();`
  - `public PlayerPowers getPowers()` / `public void setPowers(PlayerPowers p)`
  - Convenience: `public boolean hasPower(LauerPower p)` delegates to `powers.hasPower(p)`

- [x] 1.4 **Add Options constant** in `Options.java`
  - `public static final String playerPowers = "Player powers ";` (appended with player index 0..N-1)

- [x] 1.5 **Add "Powers" tab to `GetPlayers`**
  - New tab in `tabbedPane` (after existing tabs)
  - Layout: Left panel = scrollable list of checkboxes (one per LauerPower, labeled with displayName). Right panel = JTextArea showing description of currently hovered/selected power.
  - One section per player (use player color labels or "Player 0", "Player 1", etc.)
  - Checkboxes disabled for inactive player slots
  - On game start: serialize selected powers into `Options.playerPowers + i` for each player

- [x] 1.6 **Wire into game creation**
  - In `GameServerSide` (find where `PlayerServerSide` objects are created), read `Options.playerPowers + i` and call `player.setPowers(PlayerPowers.deserialize(...))`
  - If option is null/empty, player gets empty powers (no-op)

- [x] 1.7 **Save/Load support**
  - In `GameSaving`: add `powers="ARCHER,HEALER"` attribute to `<player>` XML element
  - In `GameLoading`: read `powers` attribute, call `player.setPowers(PlayerPowers.deserialize(...))`
  - If attribute is missing (old save files), default to empty powers

- [x] 1.8 **Unit tests**
  - `core/src/test/java/net/sf/colossus/game/LauerPowerTest.java`:
    - All 23 powers exist
    - Display names are non-null and non-empty
    - `getAll()` returns 23 elements
  - `core/src/test/java/net/sf/colossus/game/PlayerPowersTest.java`:
    - Empty set serializes to `""`
    - Single power serializes/deserializes correctly
    - Multiple powers round-trip correctly
    - `hasPower()` returns true/false correctly
    - Deserialize of empty string returns empty set
    - Deserialize of null returns empty set

- [x] 1.9 **Build and verify**
  - `ant` compiles without errors
  - `ant test` passes all tests (existing + new)

---

### Phase 2: Game Structure Powers (Initialization & Win Conditions)

**Goal:** Handle powers that change starting setup, stacking limits, and win/loss conditions.

**Verification:** `ant test` passes. AI-vs-AI game with structural powers runs to completion.

#### Design Answers (Phase 2)

- **Q2.1 (Amoeba legion marker):** Create a virtual "LegionMarker" creature type that has Titan's stats for that player. It appears in battle as a creature with the player's Titan power level. Implementation: when Amoeba player's legion enters battle, if the legion marker itself is "fighting," add a phantom creature to the battle with Titan stats.
- **Q2.2 (Demon strength):** Yes — the demon's strength tracks the player's Titan power level (6 + score/100). Use the same calculation as Titan strength.
- **Q2.3 (Vampire Titan lords):** x = number of times the player has earned 50 points (i.e., `score / 50`). Titan power = `(x + 1) * 4`. They are lords (can be summoned to, teleport).
- **Q2.4 (Nine Ball 80% points):** Yes, affects angel summoning thresholds. The player needs 100/0.8 = 125 points worth of kills to summon an angel (effectively). Implementation: multiply point values of Nine Ball player's kills by 0.8 before adding to score.
- **Q2.5 (Implementation order):** Start with simple structural changes first:
  1. Avian (extra gargoyles)
  2. Lord of Trees (extra centaurs)
  3. Zombie (dead pile setup)
  4. Nine Ball (stacking limit + extra creatures)
  5. Amoeba (3 legions, no single-Titan death)
  6. Vampire (two Titans)
  7. Demon (demon instead of Titan)

#### Tasks

- [x] 2.1 Modify starting creature setup for: Avian (+2 gargoyles), Lord of Trees (+2 centaurs)
- [x] 2.2 Zombie: place specific creatures in dead pile at game start
- [x] 2.3 Nine Ball: set stacking limit to 9 for that player; add extra starting creatures
- [x] 2.4 Amoeba: 3 starting legions of 4 creatures; player not dead until ALL legions destroyed
- [x] 2.5 Vampire: two Titans; player dead only when both killed; Titan is a lord
- [x] 2.6 Demon: start with demon (angel-like) instead of Titan; dead when demon strength=0 or no legions
- [x] 2.7 Per-player stacking limit check (Nine Ball=9, others=7)
- [x] 2.8 Unit tests for each structural change
- [x] 2.9 Integration test: AI game with Avian + Nine Ball players runs to completion
- [x] 2.10 Build and verify

---

### Phase 3: Movement Powers (Masterboard)

**Goal:** Modify masterboard movement for powers that affect it.

**Verification:** `ant test` passes. AI game with movement powers completes without errors.

#### Design Answers (Phase 3)

- **Q3.1 (Mulligan per-group):** Yes, per-group choice. UI shows both dice values; player picks which number each group uses. AI picks the number that allows the most favorable moves.
- **Q3.2 (Ninja non-sequential):** Remove the ordering constraint for Ninja players only. The current code likely enforces sequential movement — add a bypass flag when the moving player has Ninja power.
- **Q3.3 (Vampire overrun chain):** Yes, a group can overrun multiple times in one movement if it keeps meeting the 2.5× threshold. But it cannot overrun into a hex occupied by a friendly group.

#### Tasks

- [x] 3.1 Mulligan: roll 2 dice, present both options per group; AI picks best
- [x] 3.2 Upside-Down Cake: offer 7-N as alternative movement number; one group may use the other number
- [x] 3.3 Ninja: allow moving through/over enemy groups; reroll option; non-sequential movement
- [x] 3.4 Archmage: on roll of 5, Titan group may teleport 5 spaces ignoring arrows
- [x] 3.5 Smurfs: all groups move one less than rolled (minimum 1)
- [x] 3.6 Vampire overrun: if group value ≥ 2.5× enemy, destroy enemy and continue movement
- [x] 3.7 Ninja: allow combining non-split groups on same hex
- [x] 3.8 AI hooks for die choice (Mulligan, Upside-Down Cake), reroll (Ninja), overrun (Vampire)
- [x] 3.9 Unit tests for movement modifications
- [x] 3.10 Build and verify

---

### Phase 4: Recruiting Powers

**Goal:** Modify recruitment logic for powers that change what/how creatures can be recruited.

**Verification:** `ant test` passes. AI game with recruiting powers completes.

#### Design Answers (Phase 4)

- **Q4.1 (Variant interaction):** Powers layer on top of the variant's recruit tree. They don't replace it — they add additional recruiting options.
- **Q4.2 (Administrator 25% rule):** In addition to normal recruiting. The player can ALSO recruit with any creature worth ≥25% more than the target, regardless of terrain.
- **Q4.3 (Prophetess):** Yes, extremely powerful by design. A Ranger can act as Lion, Centaur, Troll, or Ogre for recruiting purposes. This is intentional — these are "powers."

#### Tasks

- [x] 4.1 Administrator: reduce required count by 1 for upward recruiting; recruit with creature worth ≥25% more
- [x] 4.2 Administrator: pre-battle recruitment (defender recruits before battle; attacker recruits in addition to summoning)
- [x] 4.3 The Folks: always recruit weakest creature in terrain, even from dead pile
- [x] 4.4 Living Dead: recruit from dead pile; recruit into dead pile even if "full"
- [x] 4.5 Lord of the Trees: recruit as if having extra warbear/minotaur/troll/cyclops in group
- [x] 4.6 Prophetess: recruit with any creature as if it's any creature it can downward-recruit to; any creature recruits as tower creature
- [x] 4.7 Zombie: recruit any creature from dead pile with any creature worth ≥ target's points
- [x] 4.8 AI hooks for expanded recruit choices
- [x] 4.9 Unit tests for each recruiting modification
- [x] 4.10 Build and verify

---

### Phase 5: Combat Powers

**Goal:** Modify battle mechanics. Implement in sub-phases for manageability.

**Verification:** `ant test` passes after each sub-phase. AI battles with combat powers resolve correctly.

#### Design Answers (Phase 5)

- **Q5.1 (Armourer `*` notation):** Yes, `*` means flying creature (marked with * in creature lists). Non-flying get `floor(2*str/3)` extra hits; flying get `floor(str/3)` extra hits.
- **Q5.2 (Healer phase):** Yes, a new phase. It occurs BEFORE the normal strike phase. Sequence: Movement → Healer strikes (heal own creatures) → Strike phase → Counterstrike phase. Healer's healing strikes target own creatures and remove hits.
- **Q5.3 (Avian hover + Smurfs stacking):** They don't interact — a player has one OR the other. If somehow both apply, hover takes precedence (one hovering + one ground = 2 in hex, same as Smurfs' 2-per-hex).
- **Q5.4 (Berserker timing):** Yes, the berserk creature's double strike resolves FIRST, then it dies. The death counts as a kill for the opponent's angel summoning.
- **Q5.5 (Archmage fire spell):** Once per battle (not per strike phase). "Any one strike phase" means the player picks which strike phase to use it in, but only gets one use total.
- **Q5.6 (Stacking modifiers):** Yes, strike modifiers stack. Berserker (+str/4) + Knight (+str/2) both apply to melee strikes for a player with both powers.

#### Sub-phase 5A: Passive Strike Modifiers

- [x] 5A.1 Armourer: extra hits to kill (non-flying +floor(2*str/3), flying +floor(str/3))
- [x] 5A.2 Berserker: +floor(strength/4) to all strikes (passive part)
- [x] 5A.3 Knight: +floor(strength/2) melee strikes
- [x] 5A.4 Marines: +floor(strength/4) melee; +ceil(strength/5) extra hits; no terrain penalties
- [x] 5A.5 Smurfs: strikes do only ceil(2/3) damage
- [x] 5A.6 Unit tests for each modifier
- [x] 5A.7 Build and verify

#### Sub-phase 5B: Dice Modifiers

- [x] 5B.1 Yahtzee: reroll any dice, take second result (player/AI chooses after seeing first roll)
- [x] 5B.2 Upside-Down Cake: use 7-roll for entire strike (choose before rolling)
- [x] 5B.3 Mulligan: after rolling, may instead do ceil(average hits) damage
- [x] 5B.4 Prophetess: roll twice, take either result
- [x] 5B.5 AI heuristics: reroll if below expected value; take better of two rolls
- [x] 5B.6 Unit tests
- [x] 5B.7 Build and verify

#### Sub-phase 5C: Rangestrike Modifiers

- [x] 5C.1 Archer: all creatures rangestrike; rangestrikers fire twice; no range penalties; rangestrike through obstacles at -1/hex; skill-3 creatures rangestrike at range 4; rangestrike during counterstrike
- [x] 5C.2 Archer: may recruit gorgons/rangers from dead pile (add to recruit logic)
- [x] 5C.3 Knight: all creatures get one rangestrike per combat (track "used crossbow" per creature)
- [x] 5C.4 Archmage: Titan rangestrikes as warlock
- [x] 5C.5 Unit tests
- [x] 5C.6 Build and verify

#### Sub-phase 5D: Active Combat Choices

- [x] 5D.1 Berserker: declare creatures going berserk (strike twice at +str/4, then die)
- [x] 5D.2 Healer: healing strikes against own creatures before strike phase
- [x] 5D.3 Ninja: death strike (creature dies, gets +remaining_hits dice); ninja-move phase after strikes
- [x] 5D.4 AI heuristics: never go berserk unless creature would die anyway; heal most-damaged creature; death strike only if creature is about to die
- [x] 5D.5 Unit tests
- [x] 5D.6 Build and verify

#### Sub-phase 5E: Flying/Movement in Battle

- [x] 5E.1 Avian: all creatures fly at speed 4; flying creatures hover; ogg attacks before movement
- [x] 5E.2 Smurfs: stack 2 creatures per hex in battle
- [x] 5E.3 Unit tests
- [x] 5E.4 Build and verify

#### Sub-phase 5F: Special Battle Mechanics

Implement in order of increasing complexity:

- [x] 5F.1 Vampire: no flee allowed for opponent; Titan can leave battle; half points for own dead if win
- [x] 5F.2 Archmage: fire spell (each enemy takes floor(str/3) damage, once per battle)
- [x] 5F.3 The Folks: bring extra creatures onto battle map each movement phase
- [x] 5F.4 Living Dead: corpses remain on map; slow enemies in adjacent hexes; revive enemy dead after battle
- [x] 5F.5 Demon: possession mechanic (take control of enemy creature)
- [x] 5F.6 Nine Ball: nineball demilord appears in battle
- [x] 5F.7 Smurfs: helper group mechanic (nearby friendly legion assists)
- [x] 5F.8 Unit tests for each
- [x] 5F.9 Build and verify

#### Sub-phase 5G: Post-Battle Effects

- [x] 5G.1 Zombie: raise dead creatures from battle into winning group
- [x] 5G.2 Living Dead: revive enemy corpses into own group
- [x] 5G.3 Vampire: half points for own dead creatures if battle won
- [x] 5G.4 Unit tests
- [x] 5G.5 Build and verify

---

### Phase 6: Final Integration & Polish

**Goal:** End-to-end testing, edge cases, UI polish.

#### Tasks

- [x] 6.1 Run 10 AI-vs-AI games with random power assignments — no crashes
- [x] 6.2 Run AI-vs-AI game where each player has 3+ powers — no crashes
- [x] 6.3 Save/load game with powers mid-game — powers still active after load
- [x] 6.4 Verify all 23 powers can be selected simultaneously without crash
- [x] 6.5 Add power descriptions to GetPlayers tooltip/text area from lauerpowers.html
- [x] 6.6 Final `ant` build + `ant test` — all green
- [x] 6.7 Update this file: mark all phases complete, record any remaining known issues

---

## Suggested Implementation Order Within Phases

After Phase 1 (required first), implement powers roughly in this order based on complexity and independence:

### Easy wins (implement first for quick testing):
1. **Yahtzee** — Just dice rerolling
2. **Armourer** — Just extra hits (passive)
3. **Marines** — Passive bonuses
4. **Upside-Down Cake** — Simple dice/movement alternative
5. **Knight** — Strike bonus + one rangestrike

### Medium complexity:
6. **Berserker** — Strike bonus + active berserk choice
7. **Archer** — Rangestrike expansion
8. **Mulligan** — Two dice + average damage
9. **Healer** — New healing phase
10. **Archmage** — Teleport + fire spell

### High complexity (implement last):
11. **Ninja** — Multiple mechanics
12. **Prophetess** — Multiple mechanics
13. **The Folks** — Extra creatures in battle
14. **Living Dead** — Corpse mechanics
15. **Lord of the Trees** — Tree/terrain mechanics
16. **Zombie** — Raise dead
17. **Vampire** — Multiple Titans + overrun
18. **Nine Ball** — Stacking + nineball creature
19. **Avian** — Flying + hover + ogg
20. **Smurfs** — Double stacking + helper group
21. **Amoeba** — Alternate game structure
22. **Demon** — Possession mechanic

---

## Build & Test Commands

```bash
# From project root: /Users/sherrodz/src/colossus-titan/Colossus
cd /Users/sherrodz/src/colossus-titan/Colossus

# Compile
ant

# Run all tests
ant test

# Run specific test class (if needed)
ant test -Dtest.class=net.sf.colossus.game.LauerPowerTest

# Run the game (manual testing)
./run

# Run AI-vs-AI batch game (for integration testing)
./run-n-battles.sh
```

---

## File Inventory (New & Modified)

### New Files
- `core/src/main/java/net/sf/colossus/game/LauerPower.java` — Power enum
- `core/src/main/java/net/sf/colossus/game/PlayerPowers.java` — Power set wrapper
- `core/src/test/java/net/sf/colossus/game/LauerPowerTest.java` — Unit tests
- `core/src/test/java/net/sf/colossus/game/PlayerPowersTest.java` — Unit tests

### Modified Files (Phase 1)
- `core/src/main/java/net/sf/colossus/game/Player.java` — Add powers field
- `core/src/main/java/net/sf/colossus/common/Options.java` — Add playerPowers constant
- `core/src/main/java/net/sf/colossus/appmain/GetPlayers.java` — Add Powers tab
- `core/src/main/java/net/sf/colossus/server/GameServerSide.java` — Initialize player powers
- `core/src/main/java/net/sf/colossus/server/GameSaving.java` — Save powers to XML
- `core/src/main/java/net/sf/colossus/server/GameLoading.java` — Load powers from XML

### Modified Files (Later Phases)
- `core/src/main/java/net/sf/colossus/server/BattleStrikeServerSide.java` — Strike modifiers
- `core/src/main/java/net/sf/colossus/server/BattleServerSide.java` — Battle phases, flee, etc.
- `core/src/main/java/net/sf/colossus/server/BattleMovementServerSide.java` — Battle movement
- `core/src/main/java/net/sf/colossus/game/MovementServerSide.java` — Masterboard movement
- `core/src/main/java/net/sf/colossus/server/GameServerSide.java` — Recruiting, game flow
- `core/src/main/java/net/sf/colossus/ai/SimpleAI.java` (and other AIs) — Power-aware decisions
- `core/src/main/java/net/sf/colossus/server/CreatureServerSide.java` — Modified creature stats
- `core/src/main/java/net/sf/colossus/game/Caretaker.java` — Dead pile recruiting

---

## Completion Criteria

The feature is **DONE** when:
1. All 23 powers are selectable in the UI
2. All 23 powers have their effects implemented
3. `ant test` passes with all new unit tests
4. AI-vs-AI games with powers assigned run to completion without crashes
5. Save/load preserves power state
6. This document has all tasks marked `[x]`

---

## Bug-Fix Workflow (MANDATORY)

When a bug is reported with any power:

1. **Reproduce first.** Write a failing test that demonstrates the bug before attempting a fix.
   - Unit test in `LauerPowerMechanicsTest` for formula/logic bugs
   - Integration test in `LauerPowerIntegrationTest` for Player-level bugs
   - Functional test in `LauerPowersFuncTest` for gameplay crashes
2. **Fix the code** to make the test pass.
3. **Run `ant test`** to verify no regressions.
4. **Never ship a fix without a test** that would have caught the bug.

### Test Files

| File | Scope | Runs in |
|------|-------|---------|
| `core/src/test/java/.../LauerPowerTest.java` | Enum basics (5 tests) | `ant test` |
| `core/src/test/java/.../LauerPowerMechanicsTest.java` | Formulas, descriptions (18 tests) | `ant test` |
| `core/src/test/java/.../LauerPowerIntegrationTest.java` | Player-level logic (12 tests) | `ant test` |
| `core/src/test/java/.../PlayerPowersTest.java` | Serialization (16 tests) | `ant test` |
| `core/src/functest/java/.../LauerPowersFuncTest.java` | Full AI games with powers (10 tests) | `ant functest` |

### Running Tests

```bash
# Unit tests (fast, ~1 sec)
ant test

# Functional tests (slow, runs full AI games)
ant functest
```
