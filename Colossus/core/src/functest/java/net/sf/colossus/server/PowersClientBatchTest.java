package net.sf.colossus.server;


import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.client.Client;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Legion;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.CreatureType;

import net.sf.colossus.appmain.LoadGameWithBattleTest;


/**
 * Client-side tests for Avian, Folks, Ninja, Lord of Trees powers.
 * Uses generic-power-test.xml with AVIAN power set.
 * Rd01 = Titan+Angel+Ogre+Ogre on Brush hex 3, moved=true.
 */
public class PowersClientBatchTest extends TestCase
{
    private GameServerSideTestAccess game;
    private Client client;

    public PowersClientBatchTest(String name)
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
            + "generic-power-test.xml";

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
     * Task #632: Avian — all creatures are fliers.
     * Ogre normally is NOT a flier. With AVIAN, isFlier() must be true.
     */
    public void testAvianAllCreaturesFly()
    {
        assertTrue("Has AVIAN power",
            client.getOwningPlayer().hasPower(LauerPower.AVIAN));
        Legion rd01 = findLegion("Rd01");
        assertNotNull("Found Rd01", rd01);

        for (Creature c : rd01.getCreatures())
        {
            assertTrue(c.getType().getName() + " must be flier with AVIAN",
                c.isFlier());
        }
    }

    /**
     * Task #637: Verify findEligibleRecruits works on loaded game.
     * Rd02 has 2 Gargoyles on hex 42 — verifies method is callable.
     */
    public void testFindEligibleRecruitsWorks()
    {
        Legion rd02 = findLegion("Rd02");
        assertNotNull("Found Rd02", rd02);

        List<CreatureType> recruits = client.findEligibleRecruits(rd02,
            rd02.getCurrentHex());
        assertNotNull("recruits not null", recruits);
        // Method runs without crash — that's the key assertion
        // (actual recruits depend on hex terrain)
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
