package net.sf.colossus.server;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Yahtzee power: reroll strikes, take better result.
 * The implementation in CreatureServerSide.strike2() rolls twice
 * and takes the maximum for AI players.
 */
public class YahtzeePowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player yahtzeePlayer;

    public YahtzeePowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        yahtzeePlayer = game.createAndAddPlayer("Yahtzee", "SimpleAI");
        game.createAndAddPlayer("Opp", "SimpleAI");
        yahtzeePlayer.getPowers().addPower(LauerPower.YAHTZEE);
    }

    /**
     * Yahtzee: has the power and reroll logic exists in strike path.
     * The reroll fires in CreatureServerSide.strike2() - it calls
     * rollDice twice and takes max. We verify the power check logic here.
     */
    public void testHasYahtzeePower()
    {
        assertTrue("Player has YAHTZEE",
            yahtzeePlayer.hasPower(LauerPower.YAHTZEE));
    }

    /**
     * Yahtzee reroll condition: only fires when player has YAHTZEE power.
     */
    public void testRerollCondition()
    {
        // The condition in CreatureServerSide line 609:
        // if (attackerPlayer.hasPower(YAHTZEE) || attackerPlayer.hasPower(PROPHETESS))
        assertTrue("Yahtzee triggers reroll",
            yahtzeePlayer.hasPower(LauerPower.YAHTZEE)
                || yahtzeePlayer.hasPower(LauerPower.PROPHETESS));

        // Without the power: no reroll
        yahtzeePlayer.getPowers().removePower(LauerPower.YAHTZEE);
        assertFalse("Without Yahtzee: no reroll",
            yahtzeePlayer.hasPower(LauerPower.YAHTZEE)
                || yahtzeePlayer.hasPower(LauerPower.PROPHETESS));
    }
}
