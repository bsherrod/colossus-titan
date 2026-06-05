
## Ninja (Death Strike + Ninja Move)

**Combat phase sequence (Ninja's half-round):**
1. MOVE -- move creatures as normal, Done
2. DEATH_STRIKE -- single-click own creatures to toggle death-strike mark (skull/red overlay). Click again to unmark. Done to commit. Skippable with D if no death strikes desired.
3. FIGHT -- strikes resolve. Death-strikers get bonus dice = remaining hits (tree hits excluded), then die. Applies to both melee and rangestrike.
4. NINJA_MOVE -- any unengaged creatures that did NOT move in phase 1 may move. Same movement UI as MOVE. Auto-skip if no creatures eligible (all moved or all engaged). Done to advance.

**UI details:**
- Death-strike toggle is undoable within the phase (same pattern as Berserker)
- Death-strike visual: skull overlay or red highlight on marked creatures
- Phase banner shows "Death Strike" / "Ninja Move" respectively
- NINJA_MOVE highlights only eligible creatures (unengaged + didn't move in phase 1)

## Vampire Titan Leave + Stasis

**Trigger:** During Vampire player's movement phase, click own Titan on battle map.

**Dialog:** "Stasis" | "Flee to legion..."

**Flee to legion:**
- Opens masterboard view with eligible legions highlighted (non-engaged, has room)
- Click a legion → Titan joins it, disappears from battle, opponent scores points

**Stasis:**
- Titan disappears from battle hex
- Stasis area in battle info panel shows Titan chit grayed/translucent with label "Stasis"
- Each subsequent Vampire movement phase: click stasis Titan → "Summon back" | "Stay in stasis"
- Summon back: Titan reappears fully healed, player places on valid entry hex (same as reinforcement placement)

**After battle ends (if still in stasis):**
- Won: prompt "Send Titan to which legion?" (masterboard picker)
- Lost: Titan dies (no prompt)

**Rules enforced:**
- Opponent scores points for Titan the moment it leaves battle (stasis or flee)
- Summoned-back Titan is fully healed
- Only available during Vampire player's movement phase

## Avian: Ogg Attack + Hover

**Combat phase sequence (Avian's half-round):**
1. **OGG** -- sequential resolution. Click unengaged `*` creature (attacker) → click enemy non-lord reachable in 1 move (target) → dice resolve immediately (+strength bonus, no carry, no counterstrike) → attacker dies → optionally click another attacker or Done. Phase banner: "Ogg Attack". Skippable with Done if no oggs desired.
2. **MOVE** -- normal movement. Hover toggle available for `*` creatures (see below).
3. FIGHT / STRIKEBACK as normal.

**Hover UI:**
- When a `*` creature is selected (clicked) during MOVE phase, a "Hover" button appears off to the side of the board/info panel.
- Clicking the Hover button toggles that creature's hover state.
- Visual: hovering creature is offset diagonally ~1/4 of its tile size (up-right) from hex center, making it visually distinct from grounded creatures.
- Two creatures can share a hex if one is hovering and one is grounded.
- Hover state persists until changed on a subsequent movement phase.

**Hover combat rules:**
- Hovering creature: not affected by terrain in its hex, enemies don't get terrain bonus striking it (except volcano extra dice).
- Hovering creature cannot strike while an enemy in the same hex has hits remaining.
- Not penalized for swinging upslope or dune.

**Ogg rules:**
- Only unengaged `*` creatures can ogg.
- Target must be non-lord, reachable in one move by the ogging creature.
- Attacker dies immediately, strike resolves at +strength dice.
- No carry, no counterstrike.
- Multiple oggs per phase allowed, declared sequentially after each resolves.

## The Folks: Remove Legion + Extra Battle Creatures

**1. Remove Legion (masterboard, before movement):**
- During masterboard MOVE phase, a "Remove Legion" button appears.
- Click button, then click one of your legions to remove it.
- Button disappears after any legion moves. Undo brings it back.
- Optional -- player can skip and proceed to normal movement.

**2. Extra Creatures in Battle:**
- At battle start, determine total folk creatures: cheapest creature in that terrain, enough to fill 50% of original legion point value.
- Each of your movement phases, one folk creature appears off-board (same as angel summon / turn-4 recruit). Player must move it onto the board during MOVE phase.
- If >7 folks worth of budget, bring 2 per round for the overflow rounds (per rules formula).
- On turn 4: folk creature AND angel/turn-4 recruit both appear off-board, both must be placed.
- Folk creatures don't count against stacking limit.
- Folk creatures are worth 0 points to opponent.
- At end of battle, if too many creatures in group, remove weakest until legal.

**UI: same as existing angel summon / turn-4 recruit pattern** -- creature chit appears in the off-board staging area, highlighted as needing placement during MOVE. No new UI chrome needed beyond the remove-legion button.

## Nine Ball: Leave Characters Off

**No new UI needed.** Uses existing "creatures not yet on board" pattern (same as turn-1 placement, angel summon, turn-4 recruit, folks creatures).

**Logic only:**
- Creatures not placed during first movement phase persist off-board until second movement phase.
- If still not placed at end of second movement phase, they die.
- Player may also voluntarily declare them dead during first movement phase (existing "declare dead" action).

## Demon: Possession

**Visual:** Demon (angel chit) stacked on top of creature, offset diagonally so both visible in one hex. Like Avian hover: two entities in one hex.

**Possession (turn 1 only):**
- During first movement phase, demon/angel appears off-board (like angel summon).
- Player moves it onto a friendly creature's hex → possession begins. Beast formed.

**Beast behavior:**
- Moves like the underlying creature.
- Two separate HP pools, two separate skill levels. Treat as two separate creatures in one hex (like Avian hover stacking).
- Strike phase: popup to select which strikes -- "Strike with: [Creature] | [Demon]". Each is independent, either order. Carry rules apply per-strike.
- When enemy strikes beast: popup to disambiguate target -- "[Creature] | [Demon]". Separate attack targets.

**Unpossessing (any subsequent movement phase):**
- "Demon Flees" button during MOVE phase. Demon leaves creature, appears as standalone angel in the hex.
- If demon has hits left, it can move: click demon → highlights all reachable hexes (auto-routes through adjacent friendly creatures, can chain multiple hops in one move). Click destination creature to possess it, or click empty adjacent hex to just move there.

**Demon death rules:**
- If beast is killed (both creature AND demon reach 0 HP), demon permanently loses 1 strength.
- If demon reaches strength 0, battle ends as if Titan killed (game over for Demon player).
- No new UI for strength loss -- just update displayed angel strength.

**Other:**
- Demon player starts with 2 angels (no Titans). Any group with angel may teleport.
- Demon player destroyed only if: demon at strength 0, OR no remaining legions.

## Living Dead: Corpses + Revive

**Corpse visual:**
- Use existing X'ed out dead-creature visual (already in codebase for creatures dead but not yet removed).
- Corpse offset slightly in hex so it's not obscured by enemy creature on top of it and remains clickable.
- Corpses do not block hex (enemies can move into/through).

**Corpse attacks:**
- Corpses appear as valid strikers during FIGHT phase (same as live creatures). They can attack enemies in same hex.

**Slowing:**
- Enemies leaving a hex with a corpse cost extra movement. Pure logic, no special UI.

**Revive (one-time action per battle):**
- "Revive" button available during MOVE phase (like Folks "Remove Legion" button).
- Clicking opens a revive picker UI: shows all dead enemy creatures from this battle. Player selects which to revive (up to 50% original group value, respecting stack limit). Confirm.
- Revived creatures move to "off the board, needing to move on" state (same as turn-1 placement / angel summon). Player places them during MOVE.
- If not used during battle, revive picker pops up after battle ends (if player won). Revived creatures go directly into the legion.
- Player can always open revive and select nothing (skip it).
- Revive can only be used once per battle.

## Healer: Healing Phase

**Phase sequence (current player's half-round, before their strikes):**
1. **HEAL** -- click your healer creature → click your target creature → dice resolve as healing (removes hits). Repeat or Done. Skippable with Done.
2. BERSERK (if applicable)
3. FIGHT (current player strikes)

**UI:** Same sequential-resolution pattern as Ogg (click source → click target → resolve → repeat or Done). Phase banner: "Heal".

**Rules enforced:**
- Only lords can heal lords
- A creature cannot heal itself
- Target not protected by terrain (healer still penalized by bramble)
- Overheal grants temp hits (expire end of strike phase, absorb damage first)
- Healing occurs before Berserker declaration
- Player declares temp hit allocation before carrying

## Nine Ball: Nineball Demilord

**No new UI pattern needed.** Uses existing off-board summon placement.
- Nineball appears off-board at battle start (or second movement phase), placed during MOVE like angel summon.
- **Visual: uses the legion marker chit** as its battle board representation.
- Demilord (str = 9 - group size, skill 4), worth 0 pts, doesn't count against stacking.

## Amoeba: Legion Marker in Battle

**No new UI pattern needed.** Uses existing off-board placement.
- **Visual: uses the legion marker chit** as its battle board representation.
- Fights as a Titan lord (0 pts), group destroyed only if marker is last creature remaining.
- All legions may flee (despite having a "lord"). No angels earned.

## Ninja: Masterboard Movement

**No new UI needed.** Just show valid moves (which now include more hexes due to Ninja rules).
- Move over enemy groups: enemy hexes are valid pass-through (not shown as attack destinations unless you stop there).
- Extra space if would land on enemy: movement calculation adds +1 when final hex is enemy-occupied.
- Combine groups: own-occupied hexes shown as valid destinations. System handles as merge (not attack). No recruit that phase.
- Switch places: non-sequential movement allowed -- both legions can move through each other's starting positions.
- 1-creature legion split: existing split UI, just remove the minimum-2 restriction for Ninja.

## Lord of Trees: Saplings

**Placement (first movement phase only, terrains without natural trees):**
- After a creature moves to a valid hex, "Plant Sapling" button appears.
- Valid: saplings remaining < 3, no adjacent sapling already placed.
- Visual: green tree icon overlay in hex corner, with small HP indicator.

**Sapling behavior:**
- Acts as a tree (creatures inside get tree benefits, regen, rangestrike-through).
- Has HP = planting creature's power. Can be targeted and destroyed by enemies.
- Doesn't move, strike, or count for stacking/group existence.
- If planted by hovering creature, sapling hovers (creature must hover to use it).

**Sapling as target:**
- Enemies can click sapling during strike phase. Clickable entity with HP.
- Creature "inside" sapling: same offset/stack visual as Demon possession. Click to disambiguate target.

**Titan-as-sapling:**
- No visual change. Titan just has sapling properties (creature can be inside, both regen). Pure logic.

## Prophetess: Look at Any Legion

**No new UI needed.** Remove ownership check on legion reveal -- Prophetess player can always click any legion to see its contents.
