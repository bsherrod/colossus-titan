package net.sf.colossus.server;


import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.client.Client;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Legion;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.CreatureType;

import net.sf.colossus.appmain.LoadGameWithBattleTest;


/**
 * Client-side tests for Demon power.
 * Loads demon-2angels-tower.xml: Demon player with Rd01 containing
 * 2 Angels + 2 Ogres + 2 Centaurs on Tower hex 100, moved=true.
 * Demon has no Titan — starts with 2 Angels as lords.
 */
public class DemonClientTest extends TestCase
{
    private GameServerSideTestAccess game;
    private Client client;

    public DemonClientTest(String name)
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
            + "demon-2angels-tower.xml";

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
        client = game.getClientForName("Demon");
        assertNotNull("Got real Client for Demon", client);
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
     * Task #627: Demon legion with Angels cannot flee (Angels are lords).
     */
    public void testCanFleeWithAngels()
    {
        Legion rd01 = findLegion("Rd01");
        assertNotNull("Found Rd01", rd01);
        assertFalse("Demon legion with Angels cannot flee", rd01.canFlee());
    }

    /**
     * Task #628: findEligibleRecruits on tower with 2-Angel config
     * must not crash (old NPE in PredictSplitNode).
     */
    public void testFindEligibleRecruitsOnTower()
    {
        Legion rd01 = findLegion("Rd01");
        assertNotNull("Found Rd01", rd01);

        List<CreatureType> recruits = client.findEligibleRecruits(rd01,
            rd01.getCurrentHex());
        assertNotNull("findEligibleRecruits must not crash", recruits);
        // On tower with 2 Centaurs → can recruit Lion (2 Centaurs needed)
        // and with 2 Ogres → can recruit Troll (2 Ogres needed)
        assertFalse("Should have recruits on tower", recruits.isEmpty());
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
