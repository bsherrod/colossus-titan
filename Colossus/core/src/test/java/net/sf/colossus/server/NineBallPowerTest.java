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

    /**
     * Nine Ball: starting legion should have all 4 extras added.
     * BUG: code caps at 9, only 1 extra added (8+1=9).
     * EXPECTED: all 4 added (total 12), must split on turn 1.
     */
    public void testStartingLegionHasAllExtras()
    {
        CreatureType centaur = variant.getCreatureByName("Centaur");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType titan = variant.getCreatureByName("Titan");
        CreatureType angel = variant.getCreatureByName("Angel");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("100");

        // Normal starting legion: 8 creatures
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            hex, hex, nineBallPlayer, game,
            titan, angel, centaur, centaur, gargoyle, gargoyle, ogre, ogre);
        nineBallPlayer.addLegion(legion);

        // Add extras same way as newGame3 but WITHOUT height cap
        String[] extras = {"Centaur", "Ogre", "Gargoyle", "Gargoyle"};
        int added = 0;
        for (String name : extras)
        {
            CreatureType ct = variant.getCreatureByName(name);
            // The FIX: don't cap at 9 during initial setup - must split later
            if (ct != null)
            {
                legion.getCreatures().add(new CreatureServerSide(ct, legion, game));
                added++;
            }
        }

        assertEquals("All 4 extras added", 4, added);
        assertEquals("Starting height = 12", 12, legion.getHeight());
    }

    /**
     * Nine Ball: turn 1 split of 12 creatures into 6+6 should succeed.
     * BUG: turn-1 guard requires split size == 4, rejecting 6.
     */

    /**
     * Nine Ball: turn 1 split of 12 creatures into 6+6 should NOT be
     * blocked by the turn-1 guard (which normally requires size==4).
     */
    public void testTurn1SplitNotBlockedForNineBall()
    {
        // The turn-1 guard in doSplit:
        //   if (getTurnNumber() == 1 && !player.hasPower(NINE_BALL))
        // Nine Ball should bypass this entirely.
        game.setTurnNumber(1);
        boolean blocked = (game.getTurnNumber() == 1
            && !nineBallPlayer.hasPower(LauerPower.NINE_BALL));
        assertFalse("Nine Ball turn-1 guard should NOT block", blocked);
    }

    /**
     * Normal player IS blocked by turn-1 guard.
     */
    public void testTurn1SplitBlockedForNormalPlayer()
    {
        game.setTurnNumber(1);
        boolean blocked = (game.getTurnNumber() == 1
            && !game.getPlayerByName("Opp").hasPower(LauerPower.NINE_BALL));
        assertTrue("Normal player IS blocked by turn-1 guard", blocked);
    }

    /**
     * Nine Ball: nineball demilord has power=(9-legion_size), skill=4, 0 pts.
     * With a 7-creature legion + nineball = 8 total → nineball power = 9-8 = 1.
     * With a 5-creature legion + nineball = 6 total → nineball power = 9-6 = 3.
     */
    public void testNineballDemilordStats()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        net.sf.colossus.variant.MasterHex hex =
            variant.getMasterBoard().getHexByLabel("100");

        // Legion of 5 creatures
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            hex, hex, nineBallPlayer, game,
            ogre, ogre, ogre, ogre, ogre);
        nineBallPlayer.addLegion(legion);

        // Create a nineball creature in this legion
        CreatureServerSide nineball = new CreatureServerSide(
            ogre, legion, game);
        nineball.setNineball(true);
        legion.getCreatures().add(nineball);
        // Now legion has 6 total (5 + nineball)

        assertEquals("Nineball skill = 4", 4, nineball.getSkill());
        assertEquals("Nineball power = 9 - 6 = 3", 3, nineball.getPower());
        assertEquals("Nineball worth 0 points", 0, nineball.getPointValue());

        // Add more creatures → power decreases
        legion.getCreatures().add(new CreatureServerSide(ogre, legion, game));
        // Now 7 total → power = 9 - 7 = 2
        assertEquals("Nineball power = 9 - 7 = 2", 2, nineball.getPower());
    }
}
