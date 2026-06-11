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
}
