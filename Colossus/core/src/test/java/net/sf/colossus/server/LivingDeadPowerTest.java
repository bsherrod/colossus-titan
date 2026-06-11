package net.sf.colossus.server;

import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Living Dead power: recruit from dead pile.
 */
public class LivingDeadPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player livingDeadPlayer;

    public LivingDeadPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        livingDeadPlayer = game.createAndAddPlayer("LivingDead", "SimpleAI");
        game.createAndAddPlayer("Opp", "SimpleAI");
        livingDeadPlayer.getPowers().addPower(LauerPower.LIVING_DEAD);
    }

    /**
     * Living Dead: can recruit a creature from dead pile when caretaker is empty.
     */
    public void testRecruitFromDeadPileWhenCaretakerEmpty()
    {
        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");

        // Deplete caretaker for Cyclops and put some in dead pile
        int available = game.getCaretaker().getAvailableCount(cyclops);
        for (int i = 0; i < available; i++)
        {
            game.getCaretaker().takeOne(cyclops);
        }
        game.getCaretaker().setDeadCount(cyclops, 3);

        // Create legion with 2 gargoyles in Brush (normally can recruit Cyclops with 2 Gargoyles)
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, livingDeadPlayer, game,
            gargoyle, gargoyle);
        livingDeadPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);

        // With Living Dead + dead pile available: Cyclops should be recruitable
        assertTrue("Living Dead can recruit Cyclops from dead pile",
            recruits.contains(cyclops));
    }

    /**
     * Without Living Dead: cannot recruit when caretaker is empty.
     */
    public void testWithoutLivingDeadCannotRecruitFromDeadPile()
    {
        livingDeadPlayer.getPowers().removePower(LauerPower.LIVING_DEAD);

        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");

        // Deplete caretaker for Cyclops
        int available = game.getCaretaker().getAvailableCount(cyclops);
        for (int i = 0; i < available; i++)
        {
            game.getCaretaker().takeOne(cyclops);
        }
        game.getCaretaker().setDeadCount(cyclops, 3);

        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, livingDeadPlayer, game,
            gargoyle, gargoyle);
        livingDeadPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);

        // Without Living Dead: Cyclops NOT recruitable even with dead pile
        assertFalse("Cannot recruit from dead pile without Living Dead",
            recruits.contains(cyclops));
    }
}
