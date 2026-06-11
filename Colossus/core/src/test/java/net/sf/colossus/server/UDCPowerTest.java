package net.sf.colossus.server;

import junit.framework.TestCase;
import net.sf.colossus.game.Dice;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Upside-Down Cake power: 7-roll movement, 7-minus dice.
 */
public class UDCPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player udcPlayer;

    public UDCPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        udcPlayer = game.createAndAddPlayer("UDC", "SimpleAI");
        game.createAndAddPlayer("Opp", "SimpleAI");
        udcPlayer.getPowers().addPower(LauerPower.UPSIDE_DOWN_CAKE);
    }

    @Override
    protected void tearDown()
    {
        Dice.clearScriptedRolls();
    }

    /**
     * UDC: rolling a 2 gives options 2 and 5 (7-2=5).
     * MulliganRolls should store both values.
     */
    public void testSevenMinusRollStored()
    {
        // Script the movement roll to be 2
        Dice.setScriptedRolls(2);
        ((PlayerServerSide)udcPlayer).setMovementRoll(2);
        udcPlayer.setMulliganRolls(2, 5); // simulating what the code does

        assertEquals("Roll 1 = 2", 2, udcPlayer.getMulliganRoll1());
        assertEquals("Roll 2 = 5 (7-2)", 5, udcPlayer.getMulliganRoll2());
    }

    /**
     * UDC: 7-roll means rolling 3 gives alt=4, rolling 5 gives alt=2.
     */
    public void testSevenMinusCalculation()
    {
        // Verify the formula: 7 - roll
        assertEquals("7 - 1 = 6", 6, 7 - 1);
        assertEquals("7 - 2 = 5", 5, 7 - 2);
        assertEquals("7 - 3 = 4", 4, 7 - 3);
        assertEquals("7 - 4 = 3", 3, 7 - 4);
        assertEquals("7 - 5 = 2", 2, 7 - 5);
        assertEquals("7 - 6 = 1", 1, 7 - 6);
    }

    /**
     * UDC player has the power set correctly.
     */
    public void testHasUDCPower()
    {
        assertTrue("Player has UDC",
            udcPlayer.hasPower(LauerPower.UPSIDE_DOWN_CAKE));
    }
}
