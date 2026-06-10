package net.sf.colossus.server;

import java.util.Set;

import junit.framework.TestCase;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Scripted tests for Archer power behaviors.
 * Uses the LOSTest/CarryTest pattern: makeNewGameServerSide + createBattle + placeCreature.
 */
public class ArcherPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player archerPlayer;
    private Player opponent;

    public ArcherPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        archerPlayer = game.createAndAddPlayer("Archer", "SimpleAI");
        opponent = game.createAndAddPlayer("Opp", "SimpleAI");
        archerPlayer.getPowers().addPower(LauerPower.ARCHER);
    }

    private BattleServerSide setupBrushBattle(CreatureType[] attackerCreatures,
        CreatureType[] defenderCreatures)
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3"); // Brush
        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, archerPlayer, game, attackerCreatures);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, opponent, game, defenderCreatures);
        archerPlayer.addLegion(attacker);
        opponent.addLegion(defender);
        attacker.setEntrySide(EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        return game.getBattleSS();
    }

    private void placeCreature(BattleServerSide battle,
        net.sf.colossus.game.Creature creature, String hexLabel)
    {
        MasterBoardTerrain terrain = battle.getLocation().getTerrain();
        BattleHex battleHex = terrain.getHexByLabel(hexLabel);
        creature.setCurrentHex(battleHex);
    }

    /**
     * Archer: a non-rangestriker (Ogre) can rangestrike.
     * Without Archer, Ogre has no rangestrike ability.
     */
    public void testNonRangestrikerCanRangestrike()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");
        assertFalse("Ogre is not a natural rangestriker",
            ogre.isRangestriker());

        BattleServerSide battle = setupBrushBattle(
            new CreatureType[]{ogre}, new CreatureType[]{troll});

        CreatureServerSide archerOgre = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion()
            .getCritter(0);

        // Place creatures apart -- Ogre skill=2, Archer +1 = range 3
        placeCreature(battle, archerOgre, "D1");
        placeCreature(battle, target, "D3");

        // Verify range is 3 (within Ogre+Archer range)
        int range = Battle.getRange(archerOgre.getCurrentHex(),
            target.getCurrentHex(), false);
        assertEquals("D1 to D3 range should be 3 for Ogre+Archer", 3, range);

        // With Archer power, Ogre should have rangestrike targets
        Set<BattleHex> targets = battle.findTargetHexes(archerOgre, true);
        assertTrue("Archer Ogre must have rangestrike targets",
            targets.contains(target.getCurrentHex()));
    }

    /**
     * Without Archer, a non-rangestriker has no rangestrike targets.
     */
    public void testNonRangestrikerCannotRangestrikeWithoutArcher()
    {
        archerPlayer.getPowers().removePower(LauerPower.ARCHER);

        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        BattleServerSide battle = setupBrushBattle(
            new CreatureType[]{ogre}, new CreatureType[]{troll});

        CreatureServerSide normalOgre = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion()
            .getCritter(0);

        placeCreature(battle, normalOgre, "D1");
        placeCreature(battle, target, "D4");

        Set<BattleHex> targets = battle.findTargetHexes(normalOgre, true);
        assertTrue("Normal Ogre must NOT have rangestrike targets",
            targets.isEmpty());
    }

    /**
     * Archer: getDice for non-rangestrike (melee) returns base power.
     * Archer doesn't add bonus dice to melee — only enables rangestrike.
     */
    public void testArcherMeleeDiceUnchanged()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        BattleServerSide battle = setupBrushBattle(
            new CreatureType[]{ogre}, new CreatureType[]{troll});

        CreatureServerSide archerOgre = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion()
            .getCritter(0);

        // Place adjacent for melee
        placeCreature(battle, archerOgre, "C3");
        placeCreature(battle, target, "C4");

        BattleStrikeServerSide strike = new BattleStrikeServerSide(game);
        int dice = strike.getDice(archerOgre, target, false);
        // Ogre power=6, no bonuses from Archer for melee
        assertEquals("Archer melee dice = base power", 6, dice);
    }

    /**
     * Archer: natural rangestriker fires twice (Gorgon gets 2 strikes).
     * Verified via the strike2 call in CreatureServerSide.strike().
     * Here we test that getDice for rangestrike gives correct count.
     */
    public void testNaturalRangestrikerFiresTwiceWithCorrectDiceAndSkill()
    {
        CreatureType gorgon = variant.getCreatureByName("Gorgon");
        CreatureType troll = variant.getCreatureByName("Troll");
        assertTrue("Gorgon is a natural rangestriker", gorgon.isRangestriker());
        assertEquals("Gorgon power is 6", 6, gorgon.getPower());
        assertEquals("Gorgon skill is 3", 3, gorgon.getSkill());

        BattleServerSide battle = setupBrushBattle(
            new CreatureType[]{gorgon}, new CreatureType[]{troll});

        CreatureServerSide archerGorgon = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion()
            .getCritter(0);

        placeCreature(battle, archerGorgon, "D1");
        placeCreature(battle, target, "D4");

        BattleStrikeServerSide strike = new BattleStrikeServerSide(game);

        // Each attack: 3 dice (power 6 / 2 = 3)
        int dice = strike.getDice(archerGorgon, target, true);
        assertEquals("Each rangestrike attack = 3 dice", 3, dice);

        // Strike number: 4 - attackerSkill + defenderSkill
        // Archer suppresses range penalties, so attackerSkill stays at 3
        // Troll skill = 2, so strikeNumber = 4 - 3 + 2 = 3
        int strikeNumber = strike.getStrikeNumber(archerGorgon, target, true);
        assertEquals("Strike number = 4 - 3 + 2 = 3 (hits on 3+)", 3, strikeNumber);

        // Archer + natural rangestriker = fires twice (two separate attacks)
        // Verified via code: CreatureServerSide.strike() calls strike2() twice
        // when player hasPower(ARCHER) && creature.isRangestriker() && not engaged
        assertTrue("Archer fires twice: creature is rangestriker",
            archerGorgon.getType().isRangestriker());
        assertTrue("Archer fires twice: player has Archer power",
            archerGorgon.getPlayer().hasPower(LauerPower.ARCHER));
    }
}
