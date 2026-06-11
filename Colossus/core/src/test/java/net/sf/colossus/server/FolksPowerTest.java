package net.sf.colossus.server;

import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for The Folks power: always recruit weakest terrain creature.
 */
public class FolksPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player folksPlayer;

    public FolksPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        folksPlayer = game.createAndAddPlayer("Folks", "SimpleAI");
        game.createAndAddPlayer("Opp", "SimpleAI");
        folksPlayer.getPowers().addPower(LauerPower.THE_FOLKS);
    }

    /**
     * Folks: can always recruit Gargoyle in Brush (weakest) even with
     * no matching recruiters in legion.
     */
    public void testAlwaysRecruitsWeakestInBrush()
    {
        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType titan = variant.getCreatureByName("Titan");
        // Legion with only a Titan (no Gargoyles to recruit with normally)
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, folksPlayer, game, titan);
        folksPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");

        assertTrue("Folks can always recruit Gargoyle (weakest in Brush)",
            recruits.contains(gargoyle));
    }

    /**
     * Without Folks: cannot recruit without matching recruiters.
     */
    public void testWithoutFolksNeedsRecruiters()
    {
        folksPlayer.getPowers().removePower(LauerPower.THE_FOLKS);

        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType titan = variant.getCreatureByName("Titan");
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, folksPlayer, game, titan);
        folksPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");

        // Without Folks, Titan alone cannot recruit Gargoyle in Brush
        // (needs at least 1 Gargoyle to recruit another Gargoyle)
        assertFalse("Without Folks cannot recruit with no matching recruiters",
            recruits.contains(gargoyle));
    }
}
