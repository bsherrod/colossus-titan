package net.sf.colossus.server;


import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.client.Client;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Legion;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.CreatureType;

import net.sf.colossus.appmain.LoadGameWithBattleTest;


/**
 * Client-side test for Lord of Trees +1 phantom recruiter.
 * Loads lot-1cyclops-brush.xml: LORD_OF_THE_TREES player with Rd01
 * containing Titan+Angel+Cyclops+Gargoyle on Brush hex 3.
 * With LoT: 1 Cyclops + phantom = effective 2 → recruits Gorgon.
 * Without LoT: 1 Cyclops is not enough for Gorgon (needs 2).
 */
public class LordOfTreesClientTest extends TestCase
{
    private GameServerSideTestAccess game;
    private Client client;

    public LordOfTreesClientTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        Options.setFunctionalTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();
        GameServerSideTestAccess.clearLastGame();
        GameServerSideTestAccess.setSuppressKickPhase(true);

        final String path = LoadGameWithBattleTest.SAVED_GAMES_TEST_DIR
            + "lot-1cyclops-brush.xml";

        new Thread(new Runnable()
        {
            public void run()
            {
                String[] args = { "--load", path };
                net.sf.colossus.appmain.Start.main(args);
            }
        }).start();

        game = GameServerSideTestAccess.staticWaitThatGameComesUp();
        assertNotNull("Game loaded", game);
        client = game.getClientForName("TestPower");
        assertNotNull("Got real Client for TestPower", client);
    }

    @Override
    protected void tearDown()
    {
        if (game != null)
        {
            try { game.stopAllDueToFunctionalTestCompleted(); }
            catch (Exception e) { /* ignore */ }
        }
    }

    /**
     * Task #637: LoT +1 phantom makes 1 Cyclops recruit Gorgon on Brush.
     * Brush recruits: 2 Cyclops → Gorgon. LoT gives +1 phantom Cyclops.
     */
    public void testOneCyclopsRecruitsGorgonWithLoT()
    {
        assertTrue("Has LORD_OF_THE_TREES",
            client.getOwningPlayer().hasPower(LauerPower.LORD_OF_THE_TREES));

        Legion rd01 = findLegion("Rd01");
        assertNotNull("Found Rd01", rd01);

        List<CreatureType> recruits = client.findEligibleRecruits(rd01,
            rd01.getCurrentHex());
        assertNotNull("recruits not null", recruits);

        boolean hasGorgon = false;
        for (CreatureType ct : recruits)
        {
            if (ct.getName().equals("Gorgon"))
            {
                hasGorgon = true;
                break;
            }
        }
        assertTrue("LoT +1 phantom: 1 Cyclops should recruit Gorgon on Brush",
            hasGorgon);
    }

    private Legion findLegion(String markerId)
    {
        for (Legion leg : client.getOwningPlayer().getLegions())
        {
            if (leg.getMarkerId().equals(markerId))
            {
                return leg;
            }
        }
        return null;
    }
}
