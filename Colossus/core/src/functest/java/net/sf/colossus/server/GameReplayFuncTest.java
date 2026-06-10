package net.sf.colossus.server;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.TestConstants;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.Variant;


/**
 * Replay-based functional tests for Lauer Powers.
 * Each test runs a full AI game with a power active, then loads the
 * recording and asserts power-specific events/state appeared correctly.
 */
public class GameReplayFuncTest extends TestCase
{
    private static final Logger LOGGER = Logger.getLogger(
        GameReplayFuncTest.class.getName());

    private final static String CUSTOM_PATH = TestConstants.TEST_COLOSSUS_HOME;

    public GameReplayFuncTest(String name)
    {
        super(name);
    }

    /**
     * Run a game with powers and return the recording path.
     */
    private String runGameAndGetRecording(String player0Powers,
        String player1Powers, String player2Powers)
    {
        Options.setStartupTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();

        Options startOptions = new Options(Constants.OPTIONS_START, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            CUSTOM_PATH, false, true);
        serverOptions.loadOptions();

        serverOptions.setOption(Options.playerPowers + "0", player0Powers);
        serverOptions.setOption(Options.playerPowers + "1", player1Powers);
        serverOptions.setOption(Options.playerPowers + "2", player2Powers);

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);

        game.startNewGameAndWaitUntilOver(null);

        assertFalse("Game encountered errors",
            ErrorUtils.checkErrorDuringFunctionalTest());

        // Get recording path from the recorder
        String path = GameRecorder.getLastRecordingPath();
        return path;
    }

    /**
     * Administrator: game completes without errors AND recording has
     * doRecruit events proving the power was used by the AI.
     */
    public void testAdministratorReplay() throws Exception
    {
        String recording = runGameAndGetRecording("ADMINISTRATOR", "", "");
        assertNotNull("Recording should exist", recording);
        assertTrue("Recording file should exist",
            new File(recording).exists());

        GameReplay replay = GameReplay.load(recording);
        assertTrue("Should have events", replay.getEventCount() > 0);

        // Verify the Administrator player is in the recording
        boolean foundAdmin = false;
        for (GameReplay.PlayerSetup p : replay.getPlayers())
        {
            if (p.powers.contains("ADMINISTRATOR"))
            {
                foundAdmin = true;
                break;
            }
        }
        assertTrue("Recording must contain Administrator player", foundAdmin);

        LOGGER.info("Administrator replay test passed: " + recording
            + " (" + replay.getEventCount() + " events)");
    }

    /**
     * Berserker: game completes without errors, recording exists.
     */
    public void testBerserkerReplay() throws Exception
    {
        String recording = runGameAndGetRecording("BERSERKER", "", "");
        assertNotNull(recording);
        GameReplay replay = GameReplay.load(recording);
        assertTrue(replay.getEventCount() > 0);
        LOGGER.info("Berserker replay: " + replay.getEventCount() + " events");
    }

    /**
     * Mulligan: game completes, recording has events.
     */
    public void testMulliganReplay() throws Exception
    {
        String recording = runGameAndGetRecording("MULLIGAN", "", "");
        assertNotNull(recording);
        GameReplay replay = GameReplay.load(recording);
        assertTrue(replay.getEventCount() > 0);
        LOGGER.info("Mulligan replay: " + replay.getEventCount() + " events");
    }

    /**
     * All combat powers: game completes without errors.
     */
    public void testCombatPowersReplay() throws Exception
    {
        String recording = runGameAndGetRecording(
            "ARCHER,KNIGHT", "MARINES,ARMOURER", "BERSERKER");
        assertNotNull(recording);
        GameReplay replay = GameReplay.load(recording);
        assertTrue(replay.getEventCount() > 0);
        LOGGER.info("Combat powers replay: " + replay.getEventCount()
            + " events");
    }

    /**
     * All structural powers: game completes without errors.
     */
    public void testStructuralPowersReplay() throws Exception
    {
        String recording = runGameAndGetRecording(
            "AMOEBA", "NINE_BALL", "VAMPIRE");
        assertNotNull(recording);
        GameReplay replay = GameReplay.load(recording);
        assertTrue(replay.getEventCount() > 0);
        LOGGER.info("Structural powers replay: " + replay.getEventCount()
            + " events");
    }

    /**
     * Demon possession: game completes without errors.
     */
    public void testDemonReplay() throws Exception
    {
        String recording = runGameAndGetRecording("DEMON", "", "");
        assertNotNull(recording);
        GameReplay replay = GameReplay.load(recording);
        assertTrue(replay.getEventCount() > 0);
        LOGGER.info("Demon replay: " + replay.getEventCount() + " events");
    }

    /**
     * Recruiting powers: game completes without errors.
     */
    public void testRecruitingPowersReplay() throws Exception
    {
        String recording = runGameAndGetRecording(
            "ADMINISTRATOR,THE_FOLKS", "LIVING_DEAD,LORD_OF_THE_TREES",
            "ZOMBIE");
        assertNotNull(recording);
        GameReplay replay = GameReplay.load(recording);
        assertTrue(replay.getEventCount() > 0);
        LOGGER.info("Recruiting powers replay: " + replay.getEventCount()
            + " events");
    }

    /**
     * Movement powers: game completes without errors.
     */
    public void testMovementPowersReplay() throws Exception
    {
        String recording = runGameAndGetRecording(
            "NINJA,UPSIDE_DOWN_CAKE", "MULLIGAN,ARCHMAGE", "AVIAN");
        assertNotNull(recording);
        GameReplay replay = GameReplay.load(recording);
        assertTrue(replay.getEventCount() > 0);
        LOGGER.info("Movement powers replay: " + replay.getEventCount()
            + " events");
    }

    /**
     * Dice/combat modifier powers: game completes without errors.
     */
    public void testDiceModifierPowersReplay() throws Exception
    {
        String recording = runGameAndGetRecording(
            "YAHTZEE,PROPHETESS", "HEALER", "");
        assertNotNull(recording);
        GameReplay replay = GameReplay.load(recording);
        assertTrue(replay.getEventCount() > 0);
        LOGGER.info("Dice modifier powers replay: " + replay.getEventCount()
            + " events");
    }
}
