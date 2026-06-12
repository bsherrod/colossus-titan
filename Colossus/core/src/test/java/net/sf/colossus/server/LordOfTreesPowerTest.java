package net.sf.colossus.server;

import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Lord of the Trees: +1 phantom recruiter in woods/hills/swamp/jungle.
 */
public class LordOfTreesPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player lotPlayer;

    public LordOfTreesPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        lotPlayer = game.createAndAddPlayer("LoT", "SimpleAI");
        game.createAndAddPlayer("Opp", "SimpleAI");
        lotPlayer.getPowers().addPower(LauerPower.LORD_OF_THE_TREES);
    }

    /**
     * In Woods: with 1 Warbear, can recruit Unicorn (normally needs 2 Warbears).
     * Phantom +1 makes available=2, meeting the needed=2 requirement.
     */
    public void testPhantomWarbearRecruitsUnicornInWoods()
    {
        // Find a Woods hex
        MasterHex woodsHex = findHexByTerrain("Woods");
        assertNotNull("Found a Woods hex", woodsHex);

        CreatureType warbear = variant.getCreatureByName("Warbear");
        CreatureType unicorn = variant.getCreatureByName("Unicorn");
        CreatureType titan = variant.getCreatureByName("Titan");

        // Legion with 1 Warbear in Woods (normally needs 2 for Unicorn)
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            woodsHex, woodsHex, lotPlayer, game, titan, warbear);
        lotPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, woodsHex);
        assertTrue("LoT with 1 Warbear recruits Unicorn in Woods",
            recruits.contains(unicorn));
    }

    /**
     * Without Lord of Trees: 1 Warbear cannot recruit Unicorn (needs 2).
     */
    public void testWithoutLotCannotRecruitUnicornWith1Warbear()
    {
        lotPlayer.getPowers().removePower(LauerPower.LORD_OF_THE_TREES);

        MasterHex woodsHex = findHexByTerrain("Woods");
        CreatureType warbear = variant.getCreatureByName("Warbear");
        CreatureType unicorn = variant.getCreatureByName("Unicorn");
        CreatureType titan = variant.getCreatureByName("Titan");

        LegionServerSide legion = new LegionServerSide("Rd01", null,
            woodsHex, woodsHex, lotPlayer, game, titan, warbear);
        lotPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, woodsHex);
        assertFalse("Without LoT, 1 Warbear cannot recruit Unicorn",
            recruits.contains(unicorn));
    }

    private MasterHex findHexByTerrain(String terrainName)
    {
        for (MasterHex hex : variant.getMasterBoard().getAllHexes())
        {
            if (hex.getTerrain().getId().equals(terrainName))
            {
                return hex;
            }
        }
        return null;
    }

    /**
     * Lord of Trees: can plant sapling in non-tree battle terrain.
     * Sapling HP = planting creature's power. Max 3 saplings.
     */
    public void testPlantSaplingInBattle()
    {
        // Plains hex (hex 1) — no trees
        MasterHex hex = variant.getMasterBoard().getHexByLabel("1");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, lotPlayer, game, ogre);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, game.getPlayerByName("Opp"), game, troll);
        lotPlayer.addLegion(attacker);
        game.getPlayerByName("Opp").addLegion(defender);
        attacker.setEntrySide(net.sf.colossus.game.EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex,
            net.sf.colossus.game.BattlePhase.MOVE);
        BattleServerSide battle = game.getBattleSS();

        CreatureServerSide planter = attacker.getCritter(0);
        net.sf.colossus.variant.MasterBoardTerrain terrain =
            battle.getLocation().getTerrain();
        net.sf.colossus.variant.BattleHex hexC3 = terrain.getHexByLabel("C3");
        planter.setCurrentHex(hexC3);

        // Plant sapling
        boolean planted = battle.plantSapling(planter, hexC3);
        assertTrue("Sapling planted successfully", planted);
        assertTrue("Hex now has sapling", battle.hasSapling(hexC3));
    }

    /**
     * Lord of Trees: no two saplings may be adjacent.
     */
    public void testNoAdjacentSaplings()
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("1");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, lotPlayer, game, ogre, ogre);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, game.getPlayerByName("Opp"), game, troll);
        lotPlayer.addLegion(attacker);
        game.getPlayerByName("Opp").addLegion(defender);
        attacker.setEntrySide(net.sf.colossus.game.EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex,
            net.sf.colossus.game.BattlePhase.MOVE);
        BattleServerSide battle = game.getBattleSS();

        CreatureServerSide planter = attacker.getCritter(0);
        net.sf.colossus.variant.MasterBoardTerrain terrain =
            battle.getLocation().getTerrain();
        net.sf.colossus.variant.BattleHex hexC3 = terrain.getHexByLabel("C3");
        net.sf.colossus.variant.BattleHex hexC4 = terrain.getHexByLabel("C4");
        planter.setCurrentHex(hexC3);

        // Plant first sapling
        assertTrue("First sapling OK", battle.plantSapling(planter, hexC3));

        // Try adjacent hex — should fail
        planter.setCurrentHex(hexC4);
        boolean adjacentOk = battle.plantSapling(planter, hexC4);
        assertFalse("Cannot plant adjacent to existing sapling", adjacentOk);
    }

    /**
     * Lord of Trees: max 3 saplings per battle.
     */
    public void testMaxThreeSaplings()
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("1");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, lotPlayer, game, ogre);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, game.getPlayerByName("Opp"), game, troll);
        lotPlayer.addLegion(attacker);
        game.getPlayerByName("Opp").addLegion(defender);
        attacker.setEntrySide(net.sf.colossus.game.EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex,
            net.sf.colossus.game.BattlePhase.MOVE);
        BattleServerSide battle = game.getBattleSS();

        CreatureServerSide planter = attacker.getCritter(0);
        net.sf.colossus.variant.MasterBoardTerrain terrain =
            battle.getLocation().getTerrain();

        // Plant 3 saplings at non-adjacent hexes
        net.sf.colossus.variant.BattleHex h1 = terrain.getHexByLabel("A1");
        net.sf.colossus.variant.BattleHex h2 = terrain.getHexByLabel("A3");
        net.sf.colossus.variant.BattleHex h3 = terrain.getHexByLabel("C1");
        net.sf.colossus.variant.BattleHex h4 = terrain.getHexByLabel("E1");

        planter.setCurrentHex(h1);
        assertTrue("Sapling 1 OK", battle.plantSapling(planter, h1));
        planter.setCurrentHex(h2);
        assertTrue("Sapling 2 OK", battle.plantSapling(planter, h2));
        planter.setCurrentHex(h3);
        assertTrue("Sapling 3 OK", battle.plantSapling(planter, h3));

        // 4th should fail
        planter.setCurrentHex(h4);
        assertFalse("4th sapling rejected (max 3)",
            battle.plantSapling(planter, h4));
    }

    /**
     * Lord of Trees: creature in tree/sapling hex is considered in-tree
     * (used by regen and isNativeIn checks).
     */
    public void testCreatureInSaplingTreatedAsTree()
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("1"); // Plains
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, lotPlayer, game, ogre);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, game.getPlayerByName("Opp"), game, troll);
        lotPlayer.addLegion(attacker);
        game.getPlayerByName("Opp").addLegion(defender);
        attacker.setEntrySide(net.sf.colossus.game.EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex,
            net.sf.colossus.game.BattlePhase.MOVE);
        BattleServerSide battle = game.getBattleSS();

        CreatureServerSide creature = attacker.getCritter(0);
        net.sf.colossus.variant.MasterBoardTerrain terrain =
            battle.getLocation().getTerrain();
        net.sf.colossus.variant.BattleHex hexC3 = terrain.getHexByLabel("C3");
        creature.setCurrentHex(hexC3);

        // Plant sapling at creature's hex
        battle.plantSapling(creature, hexC3);

        // Creature should now be "in tree or sapling"
        assertTrue("Creature on sapling hex is in tree/sapling",
            creature.isInTreeOrSapling());
    }

    /**
     * Lord of Trees: regen formula is floor(str/6).
     * Ogre (str=6): floor(6/6) = 1 hit regenerated.
     */
    public void testRegenFormula()
    {
        // Regen formula: floor(power/6)
        CreatureType ogre = variant.getCreatureByName("Ogre");
        // Ogre power = 6. floor(6/6) = 1
        assertEquals("Ogre regen = 1", 1, ogre.getPower() / 6);

        CreatureType troll = variant.getCreatureByName("Troll");
        // Troll power = 8. floor(8/6) = 1
        assertEquals("Troll regen = 1", 1, troll.getPower() / 6);

        CreatureType centaur = variant.getCreatureByName("Centaur");
        // Centaur power = 2. floor(2/6) = 0 (no regen!)
        assertEquals("Centaur regen = 0 (too weak)", 0,
            centaur.getPower() / 6);
    }
}
