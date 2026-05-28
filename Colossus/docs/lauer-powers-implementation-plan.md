# Lauer Powers Implementation Plan

## Instructions for Agents

**This is the living design document for the Lauer Powers feature.** Any coding agent working on this feature should:

1. **Read this file first** before making any changes.
2. **Check the "Current Status" section** to see what phase we're in and what's done.
3. **Do not start a new phase** until its STOP questions are answered (answers will be recorded inline).
4. **Update this file** after completing work:
   - Mark completed tasks with `[x]`
   - Record any design decisions made during implementation
   - Note any new questions or issues discovered
   - Update "Current Status" section
5. **Key source files** are listed in "File Inventory" at the bottom.
6. **The powers are defined in** `lauerpowers.html` in the project root — refer to it for exact rule text.
7. **Build with** `ant` from the project root (see `build.xml`). Run with `./run`.
8. **Test approach:** Unit tests go in `core/src/test/java/...`. The existing test in `core/src/functest/` shows integration test patterns.

---

## Current Status

**Phase:** 1 — Data Model & Power Selection UI
**Status:** NOT STARTED — answering Phase 1 design questions
**Last Updated:** 2026-05-28
**Blocking Questions:** See Phase 1 questions below (being discussed)

---

## Overview

This document describes the plan to add the "Erik Lauer" Titan Powers as optional rules to the Colossus game. Players select powers during game setup; each player may have zero or more powers. Powers modify game mechanics (movement, recruiting, combat, game structure) for the player who holds them.

Source: `lauerpowers.html` in the project root.

---

## Network Play Note

The network/web client code exists in the codebase (`Server.java`, `ClientHandler.java`, `WebServer.java`, etc.) and the docs say it was "not perfectly stable" as of 2012. The architecture is client-server with socket communication. **For this feature, we will not worry about network synchronization of powers.** The local game (human + AI in same JVM) is the target. If network play is ever revived, powers would need to be transmitted via the existing Options sync mechanism, which should work since we store powers as Options strings.

## AI Support

AI players will be able to have powers assigned. The approach:
- Powers that are **passive** (e.g., Armourer's extra hits, Berserker's +strength) apply automatically via the game engine — AI benefits without needing to "know" about them.
- Powers that require **active choices** (e.g., Berserker's "go berserk" decision, Healer's targeting, Ninja's death strike) will need AI decision hooks. For v1, the AI will use simple heuristics (e.g., never go berserk, heal the most-damaged creature, etc.). The AI doesn't need to play optimally, just legally.
- Powers that change **game structure** (e.g., Amoeba's 3 starting legions, Demon's possession) will be handled by the engine; AI just plays with whatever setup it gets.

---

## Powers Summary (22 total)

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

#### Tasks

1. **Create `LauerPower` enum** in `net.sf.colossus.game`
   - Constants: `ADMINISTRATOR`, `AMOEBA`, `ARCHER`, ... `ZOMBIE`
   - Fields: `displayName`, `description` (short), `fullDescription` (from HTML)
   - Static method: `LauerPower[] getAll()`

2. **Create `PlayerPowers` class** in `net.sf.colossus.game`
   - Wraps `EnumSet<LauerPower>`
   - `boolean hasPower(LauerPower p)`
   - `Set<LauerPower> getPowers()`
   - `String serialize()` → comma-separated names
   - `static PlayerPowers deserialize(String s)`

3. **Add `PlayerPowers` field to `Player` class**
   - `private PlayerPowers powers = new PlayerPowers()`
   - Getter/setter
   - Initialize from Options during game creation in `GameServerSide`

4. **Add Options constants** in `Options.java`
   - `public static final String playerPowers = "Player powers ";` (appended with player index)

5. **Add "Powers" tab to `GetPlayers`**
   - New tab in `tabbedPane` between "Options" and "Variant README"
   - Layout: one section per player (labeled "Player 1", "Player 2", etc.)
   - Each section: scrollable panel of checkboxes, one per power
   - Checkboxes enable/disable based on whether player slot is active
   - On "New Game", serialize selected powers into Options

6. **Wire into game creation**
   - In `GameServerSide` (or wherever `PlayerServerSide` objects are created), read `Options.playerPowers + i` and call `player.setPowers(PlayerPowers.deserialize(...))`

7. **Save/Load support**
   - Add powers to the XML snapshot format (in the player element)
   - Read powers back on load

#### Questions — Phase 1 (ANSWERED)

- **Q1.1:** Maximum powers per player? **ANSWER: Unlimited.**
- **Q1.2:** UI for descriptions? **ANSWER: Checkboxes on left, full description text area on right showing hovered/selected power. Iterate from there.**
- **Q1.3:** Save/load format? **ANSWER: `powers="Archer,Healer,Ninja"` attribute on `<player>` element.**
- **Q1.4:** Presets? **ANSWER: No. Just manual checkboxes.**

---

### Phase 2: Game Structure Powers (Initialization & Win Conditions)

**Goal:** Handle powers that change starting setup, stacking limits, and win/loss conditions.

Affected powers:
- **Amoeba** — 3 starting legions of 4 creatures each; no single-Titan death; legion marker fights as Titan
- **Demon** — Start with 2 angels instead of Titan; possession mechanic; destroyed when demon reaches 0 strength or no legions
- **Nine Ball** — 9-creature legions; extra starting creatures; nineball demilord; 80% point value
- **Vampire** — Two Titans; Titan is a lord; gain Titan strength every 50 points; recruit new Titans in tower
- **Ninja** — 1-character legions allowed; combine groups on same space
- **Avian** — Start with 2 extra gargoyles
- **Lord of the Trees** — Start with 2 extra centaurs
- **Zombie** — Start with specific creatures in dead pile

#### Tasks

1. Modify `GameServerSide.initPlayers()` (or equivalent) to check each player's powers and adjust:
   - Starting creatures
   - Starting legion count/composition
   - Stacking limit (Nine Ball: 9)
2. Modify win/loss condition checks:
   - Amoeba: not dead until ALL legions destroyed
   - Demon: dead when demon strength = 0 or no legions
   - Vampire: dead when both Titans killed
3. Modify `Caretaker` for Zombie's starting dead pile creatures
4. Modify stacking limit checks to be per-player (Nine Ball = 9, others = 7)

#### Questions to Answer Before Starting Phase 2

> **⚠️ STOP and answer these before coding Phase 2:**

- **Q2.1:** For Amoeba — the legion marker "fights as a Titan" in battle. Does this mean we create a special creature type, or modify the existing Titan creature behavior for that player?
- **Q2.2:** For Demon — "Your demon fights with the strength of a Titan" — does this mean the demon's strength tracks the player's Titan power level (6 + score/100)?
- **Q2.3:** For Vampire — "Your Titans are [(x+1) * 4] lords" — what is x? Is it the number of 50-point increments earned?
- **Q2.4:** For Nine Ball — "Your characters are only worth 80% the normal amount" — does this affect angel summoning thresholds too?
- **Q2.5:** Which powers should we implement first in this phase? Suggest starting with simpler structural changes (Avian's extra gargoyles, Lord of Trees' extra centaurs, Zombie's dead pile) before complex ones (Amoeba, Demon, Vampire).

---

### Phase 3: Movement Powers (Masterboard)

**Goal:** Modify masterboard movement for powers that affect it.

Affected powers:
- **Mulligan** — Roll 2 dice; groups can use either number; first turn all groups teleport within 6
- **Upside-Down Cake** — May use 7-roll (7 minus rolled number); one group may use the other number
- **Ninja** — Move over enemy groups; reroll movement; combine groups; move groups non-sequentially
- **Archmage** — On roll of 5, Titan group may teleport 5 spaces ignoring arrows
- **Smurfs** — All groups move one less than rolled (unless teleporting)
- **Vampire** — Overrun mechanic (if group value ≥ 2.5× enemy, enemy dies and group continues)

#### Tasks

1. Modify movement roll handling in `GameServerSide`:
   - Mulligan: roll 2 dice, present both options
   - Upside-Down Cake: offer 7-N as alternative
   - Ninja: reroll option
   - Smurfs: subtract 1 from movement
2. Modify movement validation:
   - Ninja: allow moving through/over enemy groups
   - Archmage: teleport on 5 for Titan group
   - Mulligan: first-turn teleport
3. Modify stacking/combining rules:
   - Ninja: allow combining non-split groups on same hex
4. Add overrun mechanic (Vampire):
   - Compare group values when landing on enemy
   - If 2.5× or more, destroy enemy, continue movement
5. AI hooks:
   - AI needs to choose which die to use (Mulligan, Upside-Down Cake)
   - AI needs to decide whether to reroll (Ninja)
   - AI needs to decide whether to overrun (Vampire)

#### Questions to Answer Before Starting Phase 3

> **⚠️ STOP and answer these before coding Phase 3:**

- **Q3.1:** For Mulligan's two dice — does the UI need to show both dice and let the player pick per-group? Or pick one number for all groups?
  - (Answer from rules: "Each of your groups may use either number" — so per-group choice)
- **Q3.2:** For Ninja's "move groups non-sequentially" — the current code moves groups one at a time. How deep a refactor is needed? Can we just remove the ordering constraint for Ninja players?
- **Q3.3:** For Vampire overrun — if the group continues movement after overrun, can it overrun again? (Rules say "your group continues movement" and "you may not overrun if the hex your group will reach after movement is already occupied by one of your groups")

---

### Phase 4: Recruiting Powers

**Goal:** Modify recruitment logic for powers that change what/how creatures can be recruited.

Affected powers:
- **Administrator** — Need one fewer creature for upward recruiting; recruit with any creature worth ≥25% more; recruit before battle (defender) or before battle (attacker)
- **The Folks** — Always recruit weakest creature in terrain, even from dead pile
- **Living Dead** — Recruit from dead pile; recruit into dead pile (even if full)
- **Lord of the Trees** — Recruit as if having extra warbear/minotaur/troll/cyclops in group
- **Prophetess** — Recruit with any creature as if it's any creature it can downward-recruit to; any creature recruits as tower creature
- **Zombie** — Recruit any creature from dead pile with any creature worth ≥ points

#### Tasks

1. Modify recruit eligibility logic (likely in `VariantSupport` or `GameServerSide.doRecruit()`):
   - Administrator: reduce required count by 1 for upward recruiting
   - Lord of the Trees: add phantom creature to group for recruit calculation
   - Prophetess: expand what each creature "counts as" for recruiting
   - Zombie: any creature worth enough points can recruit anything available
2. Modify `Caretaker` interaction:
   - The Folks, Living Dead, Zombie, Lord of the Trees: allow recruiting from dead pile
3. Add pre-battle recruitment:
   - Administrator (defender): recruit before battle starts
   - Administrator (attacker): recruit before battle in addition to summoning
4. AI hooks:
   - AI needs to choose what to recruit (expanded options)
   - For Administrator's pre-battle recruit, AI needs to decide what to recruit

#### Questions to Answer Before Starting Phase 4

> **⚠️ STOP and answer these before coding Phase 4:**

- **Q4.1:** The recruiting logic is variant-dependent (recruit trees defined per variant). Should powers override the variant's recruit tree, or layer on top of it?
- **Q4.2:** For Administrator's "recruit with any creature worth ≥25% more" — does this bypass the normal terrain-based recruiting entirely, or is it in addition to normal recruiting?
- **Q4.3:** For Prophetess — "recruit with any character as if it is any creature it may downward recruit to" — does this mean a Ranger (which recruits from Lion, Centaur, Troll, Ogre) can act as any of those for recruiting purposes? This seems extremely powerful.

---

### Phase 5: Combat Powers

**Goal:** Modify battle mechanics. This is the largest and most complex phase.

#### Sub-phase 5A: Passive Strike Modifiers (Low complexity)

Powers that modify strike calculations without requiring player decisions:
- **Armourer** — Extra hits to kill: non-flying +floor(2*str/3), flying +floor(str/3)
- **Berserker** — +floor(strength/4) to all strikes (passive part)
- **Knight** — +floor(strength/2) melee strikes
- **Marines** — +floor(strength/4) melee; +ceil(strength/5) extra hits; no terrain penalties
- **Smurfs** — Strikes do only ceil(2/3) damage

Tasks:
1. Modify `BattleStrikeServerSide` strike calculation to check attacker's/defender's powers
2. Modify hit point calculation to account for Armourer/Marines extra hits
3. Modify terrain penalty logic for Marines

#### Sub-phase 5B: Dice Modifiers (Low complexity)

- **Yahtzee** — Reroll any dice, take second result
- **Upside-Down Cake** — Use 7-roll for entire strike (choose before seeing result? or after?)
- **Mulligan** — After rolling, may instead do ceil(average hits) damage
- **Prophetess** — Roll twice, take either result (may change target for reroll)

Tasks:
1. Add post-roll decision point in strike resolution
2. AI hooks: simple heuristic (reroll if below average, take better result)

#### Sub-phase 5C: Rangestrike Modifiers (Medium complexity)

- **Archer** — All creatures rangestrike; rangestrikers fire twice; no range penalties; rangestrike through obstacles at -1/hex; skill-3 creatures rangestrike at range 4; rangestrike during counterstrike; may recruit gorgons/rangers from dead pile
- **Knight** — All creatures get one rangestrike per combat (crossbow)
- **Archmage** — Titan rangestrikes as warlock

Tasks:
1. Modify rangestrike eligibility (who can rangestrike)
2. Modify rangestrike penalty calculations
3. Add "second rangestrike" for Archer's rangestrikers
4. Track "used crossbow" per creature for Knight

#### Sub-phase 5D: Active Combat Choices (Medium complexity)

- **Berserker** — Declare creatures going berserk (strike twice, then die)
- **Healer** — Healing strikes against own creatures before strike phase
- **Ninja** — Death strike (creature dies, gets +remaining_hits dice); ninja-move phase after strikes

Tasks:
1. Add pre-strike-phase decision point for Healer and Berserker
2. Add post-strike-phase movement for Ninja
3. AI hooks for each decision

#### Sub-phase 5E: Flying/Movement in Battle (High complexity)

- **Avian** — All creatures fly at speed 4; flying creatures hover; ogg attacks before movement
- **Smurfs** — Stack 2 creatures per hex

Tasks:
1. Modify battle movement to grant flying
2. Implement hover mechanic (not affected by terrain, can't strike while enemies in hex)
3. Implement ogg attack phase
4. Modify hex occupancy for Smurfs (2 per hex)

#### Sub-phase 5F: Special Battle Mechanics (High complexity)

- **Archmage** — Fire spell (each enemy takes floor(str/3) damage)
- **The Folks** — Bring extra creatures onto battle map each movement phase
- **Living Dead** — Corpses remain; slow enemies; revive enemy dead after battle
- **Demon** — Possession mechanic
- **Nine Ball** — Nineball demilord appears
- **Smurfs** — Helper group mechanic
- **Vampire** — No flee; Titan can leave battle; half points for own dead if win

Tasks:
1. Each of these is essentially a mini-feature requiring new game state and phases
2. Implement one at a time, starting with simplest (Vampire's no-flee, Archmage's fire spell)

#### Sub-phase 5G: Post-Battle Effects (Medium complexity)

- **Zombie** — Raise dead creatures from battle into winning group
- **Living Dead** — Revive enemy corpses
- **Vampire** — Half points for own dead

Tasks:
1. Add post-battle phase for creature acquisition
2. Modify point scoring

#### Questions to Answer Before Starting Phase 5

> **⚠️ STOP and answer these before coding Phase 5:**

- **Q5.1:** For Armourer — "non-* creatures take floor(2*str/3) extra hits" — the `*` notation in Titan means "flying creature" (marked with * in creature lists). Confirm this interpretation.
- **Q5.2:** For Healer — healing happens "before the mover's and the other person's strike phase." Is this a completely new phase in the battle turn sequence? Where exactly does it fit?
- **Q5.3:** For Avian's hover — "two characters may be in the same hex if one is hovering" — this fundamentally changes hex occupancy rules. How does this interact with Smurfs' double-stacking?
- **Q5.4:** For Berserker's berserk — "dies automatically immediately and counts as a kill for opponent's angel summoning" — does the creature's berserk strike resolve before it dies?
- **Q5.5:** For the fire spell (Archmage) — "during any one strike phase you may declare you are casting the fire spell" — is this once per battle, or once per strike phase? (Rules say "any one" suggesting once per battle.)
- **Q5.6:** Strike modifications stack — if a player has both Berserker (+str/4) and Knight (+str/2 melee), do both apply to melee strikes?

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

## Test Plan

### Unit Tests

#### Phase 1 Tests
- `LauerPowerTest` — Enum has all 22 powers; display names correct
- `PlayerPowersTest` — Serialize/deserialize round-trips correctly; `hasPower()` works; empty set serializes to empty string
- `GetPlayersTest` — Power selections stored in Options correctly (extend existing test)
- Save/load test — Game with powers saves and loads with powers intact

#### Phase 2 Tests
- Starting setup tests per power:
  - Amoeba player starts with 3 legions of 4
  - Avian player starts with 2 extra gargoyles
  - Lord of Trees starts with 2 extra centaurs
  - Zombie has specific creatures in dead pile
  - Nine Ball has extra starting creatures and 9-creature limit
- Win condition tests:
  - Amoeba not dead until all legions gone
  - Vampire not dead until both Titans killed
  - Demon dead when demon strength = 0

#### Phase 3 Tests
- Movement tests:
  - Mulligan player gets two dice, groups can use either
  - Upside-Down Cake can use 7-N
  - Ninja can move through enemy groups
  - Smurfs move one less
  - Archmage teleports on 5
  - Vampire overrun triggers at 2.5× value

#### Phase 4 Tests
- Recruiting tests:
  - Administrator needs one fewer creature
  - The Folks always recruits weakest
  - Living Dead/Zombie recruit from dead pile
  - Lord of Trees gets bonus recruiting
  - Prophetess flexible recruiting

#### Phase 5 Tests
- Strike calculation tests (per sub-phase):
  - Armourer creatures take correct extra hits
  - Berserker +str/4 applied
  - Knight +str/2 melee applied
  - Marines bonuses applied
  - Smurfs 2/3 damage applied
- Dice modifier tests:
  - Yahtzee reroll mechanics
  - Mulligan average damage option
- Rangestrike tests:
  - Archer all creatures can rangestrike
  - Knight one-time crossbow
- Active choice tests:
  - Berserker berserk kills creature after double strike
  - Healer removes hits from friendly
  - Ninja death strike adds dice equal to remaining hits

### Integration Tests

- Full game with 2 AI players, each with 1-2 simple powers (Yahtzee, Armourer), runs to completion without errors
- Full game with human + AI, powers selected, game plays normally for non-power-affected actions
- Save game with powers → load game → powers still active and affecting gameplay
- All 22 powers can be selected simultaneously without crash (even if interactions are undefined)

### Manual Testing Checklist

- [ ] GetPlayers UI: Powers tab appears, checkboxes work, disabled for inactive player slots
- [ ] Powers persist across GetPlayers open/close (saved in Options)
- [ ] Game starts correctly with powers assigned
- [ ] Each power's effect is observable in gameplay (one test per power)
- [ ] AI plays legally with each power (doesn't crash, makes valid moves)
- [ ] Power tooltips/descriptions are readable and accurate

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

### Modified Files (Later Phases)
- `core/src/main/java/net/sf/colossus/server/BattleStrikeServerSide.java` — Strike modifiers
- `core/src/main/java/net/sf/colossus/server/BattleServerSide.java` — Battle phases, flee, etc.
- `core/src/main/java/net/sf/colossus/server/BattleMovementServerSide.java` — Battle movement
- `core/src/main/java/net/sf/colossus/game/MovementServerSide.java` — Masterboard movement
- `core/src/main/java/net/sf/colossus/server/GameServerSide.java` — Recruiting, game flow
- `core/src/main/java/net/sf/colossus/ai/SimpleAI.java` (and other AIs) — Power-aware decisions
- `core/src/main/java/net/sf/colossus/ai/AI.java` — New methods for power decisions
- `core/src/main/java/net/sf/colossus/server/CreatureServerSide.java` — Modified creature stats
- `core/src/main/java/net/sf/colossus/game/Caretaker.java` — Dead pile recruiting

---

## Open Design Questions (Global)

These should be answered before significant implementation begins:

1. **Power interactions:** If a player has both Amoeba (no Titan) and Vampire (two Titans), what happens? Suggest: document that certain combinations are undefined/unsupported, but don't prevent selection. Let the player deal with contradictions.

2. **Power balance:** These powers are clearly not balanced against each other. Is that acceptable? (Yes — they're optional fun rules, not competitive.)

3. **Partial implementation:** Is it acceptable to ship Phase 1 (UI + data model) with no powers actually doing anything yet, then add power effects incrementally? (Suggested: yes, this allows iterative development and testing.)

4. **Power descriptions in UI:** Should we show the full HTML description from `lauerpowers.html`, or write shorter summaries? Full descriptions are long but complete.
