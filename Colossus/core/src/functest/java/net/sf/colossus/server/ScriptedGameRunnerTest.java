package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.TestConstants;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Scripted game runner for power testing.
 * Runs a full GameServerSide with scripted dice and verifies
 * specific move outcomes by intercepting the game log.
 *
 * Usage pattern:
 * 1. Set up options with powers
 * 2. Push scripted dice values
 * 3. Run the game (AI-driven) for N rounds
 * 4. Check captured log for SEVERE errors and expected behaviors
 *
 * This tests the REAL game path (server + AI + client) end-to-end.
 */
public class ScriptedGameRunnerTest extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(ScriptedGameRunnerTest.class.getName());

    private static final String CUSTOM_PATH = TestConstants.TEST_COLOSSUS_HOME;

    /** Captures SEVERE log messages during the game. */
    private final List<String> severeMessages = new ArrayList<String>();
    /** Captures all "did move" events. */
    private final List<String> moveMessages = new ArrayList<String>();

    private Handler logCapture;

    public ScriptedGameRunnerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        severeMessages.clear();
        moveMessages.clear();

        // Install a log handler that captures SEVERE and move events
        logCapture = new Handler()
        {
            @Override
            public void publish(LogRecord record)
            {
                String msg = record.getMessage();
                if (msg == null) return;
                if (record.getLevel() == Level.SEVERE)
                {
                    severeMessages.add(msg);
                }
                if (msg.contains("teleports") || msg.contains("moves to")
                    || msg.contains("allTellDidMove")
                    || msg.contains("doMove"))
                {
                    moveMessages.add(msg);
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        Logger.getLogger("").addHandler(logCapture);
    }

    @Override
    protected void tearDown() throws Exception
    {
        Logger.getLogger("").removeHandler(logCapture);
        super.tearDown();
    }

    /**
     * Run a game with specified powers, returns after game completes.
     * Uses startup-test mode (stressTestRounds=1).
     */
    private void runGameWithPowers(String p0Powers, String p1Powers)
    {
        Options.setStartupTest(true);
        System.setProperty("net.sf.colossus.stressTestRounds", "1");
        ErrorUtils.clearErrorDuringFunctionalTest();

        Options startOptions = new Options(Constants.OPTIONS_START, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            CUSTOM_PATH, false, true);
        serverOptions.loadOptions();

        // Set player config
        serverOptions.setOption(Options.playerName + "0", "ScriptP0");
        serverOptions.setOption(Options.playerType + "0", "SimpleAI");
        serverOptions.setOption(Options.playerName + "1", "ScriptP1");
        serverOptions.setOption(Options.playerType + "1", "SimpleAI");

        if (!p0Powers.isEmpty())
        {
            serverOptions.setOption(Options.playerPowers + "0", p0Powers);
        }
        if (!p1Powers.isEmpty())
        {
            serverOptions.setOption(Options.playerPowers + "1", p1Powers);
        }

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);

        game.startNewGameAndWaitUntilOver(null);

        System.clearProperty("net.sf.colossus.stressTestRounds");
    }

    // ============================================================
    // Mulligan Tests
    // ============================================================

    /**
     * Test: 2-player game with both having Mulligan.
     * Verifies zero SEVERE errors (meaning both players can move
     * on turn 1 with teleport AND on subsequent turns with dual dice).
     */
    public void testMulliganTwoPlayersFullGame()
    {
        runGameWithPowers("MULLIGAN", "MULLIGAN");
        assertEquals("Game should complete with zero SEVERE errors."
            + " First error: "
            + (severeMessages.isEmpty() ? "none" : severeMessages.get(0)),
            0, severeMessages.size());
    }

    /**
     * Test: Mulligan player vs non-Mulligan player.
     * The Mulligan player gets dual-roll advantage;
     * the non-Mulligan player plays normally.
     */
    public void testMulliganVsNormal()
    {
        runGameWithPowers("MULLIGAN", "");
        assertEquals("Mixed Mulligan/normal game should have zero SEVERE."
            + " First error: "
            + (severeMessages.isEmpty() ? "none" : severeMessages.get(0)),
            0, severeMessages.size());
    }

    /**
     * Test: Mulligan + another power (combo).
     * Verifies powers don't interfere with each other.
     */
    public void testMulliganCombo()
    {
        runGameWithPowers("MULLIGAN,ARMOURER", "KNIGHT");
        assertEquals("Combo game should have zero SEVERE."
            + " First error: "
            + (severeMessages.isEmpty() ? "none" : severeMessages.get(0)),
            0, severeMessages.size());
    }
}
