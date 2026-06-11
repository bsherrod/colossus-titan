package net.sf.colossus.server;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Nine Ball power: stacking limit 9.
 */
public class NineBallPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player nineBallPlayer;

    public NineBallPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        nineBallPlayer = game.createAndAddPlayer("NineBall", "SimpleAI");
        game.createAndAddPlayer("Opp", "SimpleAI");
        nineBallPlayer.getPowers().addPower(LauerPower.NINE_BALL);
    }

    public void testMaxLegionHeight9()
    {
        assertEquals("Nine Ball max height = 9", 9,
            nineBallPlayer.getMaxLegionHeight());
    }

    public void testWithoutNineBallMaxHeight7()
    {
        nineBallPlayer.getPowers().removePower(LauerPower.NINE_BALL);
        assertEquals("Normal max height = 7", 7,
            nineBallPlayer.getMaxLegionHeight());
    }

    public void testCanAddUpTo9Creatures()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("100");
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            hex, hex, nineBallPlayer, game, ogre, ogre, ogre, ogre);
        nineBallPlayer.addLegion(legion);

        // Start at 4, add up to 9
        assertTrue("Add 5th", legion.addCreature(ogre, false));
        assertTrue("Add 6th", legion.addCreature(ogre, false));
        assertTrue("Add 7th", legion.addCreature(ogre, false));
        assertTrue("Add 8th", legion.addCreature(ogre, false));
        assertTrue("Add 9th", legion.addCreature(ogre, false));
        assertEquals("Height = 9", 9, legion.getHeight());

        // 10th should be rejected
        assertFalse("Cannot add 10th", legion.addCreature(ogre, false));
        assertEquals("Still 9", 9, legion.getHeight());
    }
}
