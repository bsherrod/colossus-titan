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
 * Scripted tests for Knight power: crossbow (one-shot rangestrike per combat).
 */
public class KnightPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player knightPlayer;
    private Player opponent;

    public KnightPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        knightPlayer = game.createAndAddPlayer("Knight", "SimpleAI");
        opponent = game.createAndAddPlayer("Opp", "SimpleAI");
        knightPlayer.getPowers().addPower(LauerPower.KNIGHT);
    }

    private BattleServerSide setupBattle(BattlePhase phase,
        CreatureType[] attackerCreatures, CreatureType[] defenderCreatures)
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3"); // Brush
        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, knightPlayer, game, attackerCreatures);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, opponent, game, defenderCreatures);
        knightPlayer.addLegion(attacker);
        opponent.addLegion(defender);
        attacker.setEntrySide(EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, phase);
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
     * Knight crossbow: non-rangestriker can rangestrike during STRIKEBACK.
     */
    public void testCrossbowAvailableDuringStrikeback()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        // STRIKEBACK phase -- Knight's crossbow only works here
        BattleServerSide battle = setupBattle(BattlePhase.STRIKEBACK,
            new CreatureType[]{ogre}, new CreatureType[]{troll});

        CreatureServerSide knightOgre = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion()
            .getCritter(0);

        // Range 3 -- within Ogre skill 2 + Knight range? 
        // Actually Knight doesn't extend range. Ogre skill=2, range must be <=2.
        placeCreature(battle, knightOgre, "C3");
        placeCreature(battle, target, "C4");
        // C3-C4 is adjacent -- crossbow shouldn't work when adjacent
        // Let's use range 2
        placeCreature(battle, knightOgre, "D1");
        placeCreature(battle, target, "D2");

        int range = Battle.getRange(knightOgre.getCurrentHex(),
            target.getCurrentHex(), false);
        assertTrue("D1 to D2 range must be <= Ogre skill 2", range <= 2);

        assertFalse("Ogre is not a natural rangestriker", ogre.isRangestriker());
        assertFalse("Crossbow not yet used", knightOgre.isCrossbowUsed());

        Set<BattleHex> targets = battle.findTargetHexes(knightOgre, true);
        assertTrue("Knight Ogre should have crossbow rangestrike target in STRIKEBACK",
            targets.contains(target.getCurrentHex()));
    }

    /**
     * Knight crossbow: after using crossbow once, it's no longer available.
     */
    public void testCrossbowOnlyOncePerCombat()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        BattleServerSide battle = setupBattle(BattlePhase.STRIKEBACK,
            new CreatureType[]{ogre}, new CreatureType[]{troll});

        CreatureServerSide knightOgre = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion()
            .getCritter(0);

        // Range 2 -- non-adjacent but within Ogre skill
        placeCreature(battle, knightOgre, "C2");
        placeCreature(battle, target, "D3");

        int range = Battle.getRange(knightOgre.getCurrentHex(),
            target.getCurrentHex(), false);
        assertEquals("Must be range 2 for Ogre skill", 2, range);

        // Simulate crossbow already used
        knightOgre.markCrossbowUsed();

        assertTrue("Crossbow is marked used", knightOgre.isCrossbowUsed());

        Set<BattleHex> targets = battle.findTargetHexes(knightOgre, true);
        assertFalse("Knight Ogre must NOT have rangestrike after crossbow used",
            targets.contains(target.getCurrentHex()));
    }

    /**
     * Knight crossbow: NOT available during FIGHT phase (only STRIKEBACK).
     */
    public void testCrossbowNotAvailableDuringFight()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        // FIGHT phase -- crossbow should NOT work
        BattleServerSide battle = setupBattle(BattlePhase.FIGHT,
            new CreatureType[]{ogre}, new CreatureType[]{troll});

        CreatureServerSide knightOgre = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion()
            .getCritter(0);

        // Range 2 -- non-adjacent but within Ogre skill
        placeCreature(battle, knightOgre, "C2");
        placeCreature(battle, target, "D3");

        int range = Battle.getRange(knightOgre.getCurrentHex(),
            target.getCurrentHex(), false);
        assertEquals("Must be range 2 for Ogre skill", 2, range);

        assertFalse("Crossbow not used", knightOgre.isCrossbowUsed());

        Set<BattleHex> targets = battle.findTargetHexes(knightOgre, true);
        // Ogre is not a rangestriker and it's FIGHT not STRIKEBACK -- no rangestrike
        assertFalse("Knight Ogre must NOT have rangestrike during FIGHT",
            targets.contains(target.getCurrentHex()));
    }
}
