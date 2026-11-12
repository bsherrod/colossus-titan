package net.sf.colossus.server;


import junit.framework.TestCase;
import net.sf.colossus.client.Client;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Legion;
import net.sf.colossus.util.ErrorUtils;

import net.sf.colossus.appmain.LoadGameWithBattleTest;


/**
 * Client-side tests for Amoeba power using saved-game XML.
 * Loads amoeba-3legions-brush.xml: Amoeba player with 3 legions
 * (Rd01 on Brush hex 3, Rd02 on hex 42, Rd03 on Tower hex 100),
 * all height 4, no lords, moved=true.
 */
public class AmoebaClientTest extends TestCase
{
    private GameServerSideTestAccess game;
    private Client client;

    public AmoebaClientTest(String name)
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
            + "amoeba-3legions-brush.xml";

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
        client = game.getClientForName("Amoeba");
        assertNotNull("Got real Client for Amoeba", client);
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
     * Task #629: Amoeba legions can flee despite having no standard lord.
     * canFlee() has AMOEBA override returning true.
     */
    public void testCanFlee()
    {
        Legion rd01 = findLegion("Rd01");
        assertNotNull("Found Rd01", rd01);
        assertTrue("Amoeba player has AMOEBA power",
            rd01.getPlayer().hasPower(LauerPower.AMOEBA));
        assertTrue("Amoeba legion can flee", rd01.canFlee());
    }

    /**
     * Task #630: Amoeba has 3 legions (split twice on turn 1).
     */
    public void testThreeLegionsAfterSetup()
    {
        assertEquals("Amoeba has 3 legions", 3,
            client.getOwningPlayer().getNumLegions());
    }

    /**
     * Task #631: Amoeba maxLegionHeight is 7 (standard).
     */
    public void testMaxLegionHeight()
    {
        assertEquals("Amoeba max height is 7", 7,
            client.getOwningPlayer().getMaxLegionHeight());
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
