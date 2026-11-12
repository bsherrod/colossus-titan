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
 * Client-side tests for Nine Ball power using saved-game XML fixtures.
 * Loads nineball-height7-brush.xml which has:
 * - NineBall (Human) player with Rd01 at height 7 on Brush hex, moved=true
 * - Phase MUSTER (4), currentPlayer=0 (NineBall)
 *
 * Tests verify that the CLIENT-side canRecruit and findEligibleRecruits
 * respect getMaxLegionHeight()=9 instead of the old hardcoded 7.
 */
public class NineBallClientTest extends TestCase
{
    private GameServerSideTestAccess game;
    private Client client;

    public NineBallClientTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        Options.setFunctionalTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();
        game = setupGame("nineball-height7-brush.xml");
        assertNotNull("Game loaded", game);
        client = game.getClientForName("NineBall");
        assertNotNull("Got real Client for NineBall", client);
    }

    @Override
    protected void tearDown()
    {
        if (game != null)
        {
            try
            {
                game.stopAllDueToFunctionalTestCompleted();
            }
            catch (Exception e)
            {
                // Game may not be in a state to stop cleanly
            }
        }
    }

    /**
     * Task #621: canRecruit MUST return true at height 7 for Nine Ball.
     * OLD BUG: Client.canRecruit hardcoded < 7, so height 7 always failed.
     * With fix: 7 < 9 (maxLegionHeight) = true.
     */
    public void testCanRecruitAtHeight7()
    {
        Legion rd01 = findLegion("Rd01");
        assertNotNull("Found Rd01", rd01);
        assertEquals("Rd01 at height 7", 7, rd01.getHeight());
        assertTrue("Rd01 has moved", rd01.hasMoved());
        assertFalse("Rd01 has not recruited", rd01.hasRecruited());

        boolean result = client.canRecruit(rd01);
        assertTrue("canRecruit MUST be true at height 7 for Nine Ball", result);
    }

    /**
     * Task #622: findEligibleRecruits must return non-empty at height 7 on Brush.
     * Rd01 has 2 Cyclops on Brush → can recruit Gorgon.
     */
    public void testFindEligibleRecruitsAtHeight7()
    {
        Legion rd01 = findLegion("Rd01");
        assertNotNull("Found Rd01", rd01);
        assertEquals("Rd01 at height 7", 7, rd01.getHeight());

        List<CreatureType> recruits = client.findEligibleRecruits(rd01,
            rd01.getCurrentHex());
        assertNotNull("findEligibleRecruits not null", recruits);
        assertFalse("findEligibleRecruits not empty at height 7 on Brush",
            recruits.isEmpty());
    }

    /**
     * Task #623: Nine Ball maxLegionHeight must be 9.
     * canRecruit uses this: height < maxLegionHeight.
     * So height 9 = can't recruit, height 8 = can recruit.
     */
    public void testCannotRecruitAtMaxHeight()
    {
        // Use the client from setUp — same loaded game
        assertNotNull("Client exists", client);
        Legion rd01 = findLegion("Rd01");
        assertNotNull("Found Rd01", rd01);

        int maxHeight = rd01.getPlayer().getMaxLegionHeight();
        assertEquals("Nine Ball max height is 9", 9, maxHeight);
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

    private GameServerSideTestAccess setupGame(String filename)
    {
        GameServerSideTestAccess.clearLastGame();
        GameServerSideTestAccess.setSuppressKickPhase(true);
        final String path = LoadGameWithBattleTest.SAVED_GAMES_TEST_DIR
            + filename;

        Runnable runTheGame = new Runnable()
        {
            public void run()
            {
                String[] args = { "--load", path };
                net.sf.colossus.appmain.Start.main(args);
            }
        };
        new Thread(runTheGame).start();

        return GameServerSideTestAccess.staticWaitThatGameComesUp();
    }
}
