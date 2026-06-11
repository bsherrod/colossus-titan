package net.sf.colossus.server;

import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Prophetess power: recruit any terrain creature available.
 */
public class ProphetessPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player prophetessPlayer;

    public ProphetessPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        prophetessPlayer = game.createAndAddPlayer("Prophetess", "SimpleAI");
        game.createAndAddPlayer("Opp", "SimpleAI");
        prophetessPlayer.getPowers().addPower(LauerPower.PROPHETESS);
    }

    /**
     * Prophetess: can recruit any terrain creature regardless of recruiters.
     * In Brush with just a Titan: should offer Gargoyle AND Cyclops (all Brush creatures).
     */
    public void testRecruitsAllTerrainCreatures()
    {
        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType titan = variant.getCreatureByName("Titan");

        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, prophetessPlayer, game, titan);
        prophetessPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);

        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");

        assertTrue("Prophetess can recruit Gargoyle in Brush",
            recruits.contains(gargoyle));
        assertTrue("Prophetess can recruit Cyclops in Brush",
            recruits.contains(cyclops));
    }

    /**
     * Without Prophetess: Titan alone cannot recruit in Brush.
     */
    public void testWithoutProphetessCannotRecruitWithoutRecruiters()
    {
        prophetessPlayer.getPowers().removePower(LauerPower.PROPHETESS);

        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType titan = variant.getCreatureByName("Titan");

        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, prophetessPlayer, game, titan);
        prophetessPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);

        CreatureType cyclops = variant.getCreatureByName("Cyclops");
        assertFalse("Without Prophetess, Titan cannot recruit Cyclops",
            recruits.contains(cyclops));
    }
}
