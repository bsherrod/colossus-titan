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
 * Client-side tests for Administrator power.
 * Loads admin-1gargoyle-brush.xml: Admin player with Rd01 containing
 * Titan+Angel+Gargoyle (height 3) on Brush hex 3, moved=true.
 *
 * Administrator: needs 1 fewer creature for upward recruiting.
 * Normal Brush: 2 Gargoyles → Cyclops. With Admin: 1 Gargoyle → Cyclops.
 */
public class AdministratorClientTest extends TestCase
{
    private GameServerSideTestAccess game;
    private Client client;

    public AdministratorClientTest(String name)
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
            + "admin-1gargoyle-brush.xml";

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
        client = game.getClientForName("Admin");
        assertNotNull("Got real Client for Admin", client);
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
     * Task #624: With Administrator, 1 Gargoyle recruits Cyclops in Brush.
     * Normal: 2 Gargoyles needed. Admin -1 = 1 needed.
     */
    public void testOneGargoyleRecruitsCyclopsInBrush()
    {
        Legion rd01 = findLegion("Rd01");
        assertNotNull("Found Rd01", rd01);
        assertEquals("Rd01 at height 3", 3, rd01.getHeight());
        assertTrue("Rd01 has moved", rd01.hasMoved());

        List<CreatureType> recruits = client.findEligibleRecruits(rd01,
            rd01.getCurrentHex());
        assertNotNull("recruits not null", recruits);

        // Find Cyclops in recruit list
        boolean hasCyclops = false;
        for (CreatureType ct : recruits)
        {
            if (ct.getName().equals("Cyclops"))
            {
                hasCyclops = true;
                break;
            }
        }
        assertTrue("Admin with 1 Gargoyle on Brush MUST recruit Cyclops",
            hasCyclops);
    }

    /**
     * Task #625: Admin -1 reduces Gargoyle base recruit to 0 needed.
     * On Brush, Gargoyle is the base recruit (needs 1 of nothing = free).
     * With Administrator: 1-1 = 0 → Gargoyle available with NO Gargoyles.
     * Rd01 has 1 Gargoyle so this is trivially true, but verify the
     * null-recruiter path works: canRecruit should be true.
     */
    public void testCanRecruitWithAdminPower()
    {
        Legion rd01 = findLegion("Rd01");
        assertNotNull("Found Rd01", rd01);
        assertTrue("canRecruit is true for Admin legion on Brush",
            client.canRecruit(rd01));
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
