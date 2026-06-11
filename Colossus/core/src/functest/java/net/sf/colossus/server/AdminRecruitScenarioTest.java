package net.sf.colossus.server;


import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.appmain.Start;
import net.sf.colossus.client.Client;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.Legion;
import net.sf.colossus.util.ErrorUtils;


/**
 * Scripted scenario test: loads a saved game whose Redo section
 * executes a recruit through the full Server.doRecruit() ->
 * Server.didRecruit() -> client.didRecruit() -> PredictSplitNode
 * protocol path. After redo completes (before kickPhase), asserts
 * on client-side state.
 *
 * Pattern: LoadGameWithBattleTest + GameServerSideTestAccess.
 * The Redo fires before kickstartGame notifies our test thread.
 * By the time waitThatGameComesUp() returns, the recruit has been
 * processed through the full protocol path and client state reflects it.
 */
public class AdminRecruitScenarioTest extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(AdminRecruitScenarioTest.class.getName());

    private static final String SAVED_GAMES_DIR =
        "./core/src/functest/resource/colossus-home/saves/";

    public AdminRecruitScenarioTest(String name)
    {
        super(name);
    }

    private GameServerSideTestAccess setupGame(String filename)
    {
        WhatNextManager.sleepFor(2000);
        GameServerSideTestAccess.clearLastGame();
        Options.setFunctionalTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();

        final String fullPath = SAVED_GAMES_DIR + filename;
        Thread gameThread = new Thread(new Runnable()
        {
            public void run()
            {
                String[] args = { "--load", fullPath };
                Start.main(args);
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();

        return GameServerSideTestAccess.staticWaitThatGameComesUp();
    }

    /**
     * BUG: Administrator recruit sends wrong numRecruiters to client.
     *
     * Saved game state: Human player "admin" with ADMINISTRATOR power,
     * legion Rd01 = [Ogre, Ogre, Gargoyle] on Hills hex 9, MUSTER phase.
     * Redo section: recruit Minotaur with Ogre.
     *
     * Server.didRecruit() sends numRecruiters to client. If it sends 3
     * (raw, unadjusted), PredictSplitNode.revealCreatures() adds a
     * phantom Ogre. If it sends 2 (adjusted), no phantom.
     *
     * Assert: client-side legion height = 4 (3 original + 1 Minotaur).
     * FAILS before fix (height=5, phantom Ogre).
     * PASSES after fix (height=4).
     */
    public void testAdminRecruitNoPhantomViaProtocol()
    {
        GameServerSideTestAccess game = setupGame("admin-recruit-hills.xml");
        if (game == null)
        {
            fail("Game failed to load from admin-recruit-hills.xml");
            return;
        }

        // By now, redo has already executed the recruit through the
        // full protocol path. Get the client and check state.
        Client adminClient = game.getClientForName("admin");
        assertNotNull("Must have client for 'admin' player", adminClient);

        Legion legion = adminClient.getOwningPlayer()
            .getLegionByMarkerId("Rd01");
        assertNotNull("Client must know about legion Rd01", legion);

        int height = legion.getHeight();
        LOGGER.info("Client-side legion Rd01 height after redo recruit: "
            + height);

        // Quit the game immediately (before kickPhase blocks on Human input)
        game.stopAllDueToFunctionalTestCompleted();

        // THE ASSERTION: height must be 4 (2 Ogres + Gargoyle + Minotaur)
        // BUG: height=5 because PredictSplitNode added phantom Ogre
        assertEquals(
            "After Redo recruits Minotaur (Administrator, 2 Ogres in Hills), "
                + "client-side Rd01 height must be 4 (not 5). "
                + "Height=5 means Server.didRecruit sent numRecruiters=3 "
                + "(raw) causing PredictSplitNode to add a phantom Ogre.",
            4, height);
    }
}
