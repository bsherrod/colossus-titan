package net.sf.colossus.server;

import java.io.File;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.TestConstants;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.Variant;


/**
 * Functest that runs a REAL AI game (not startupTest) to produce
 * full recordings with actual gameplay events (doSplit, doMove, etc).
 * Uses END_AFTER_FIRST_BATTLE to keep games short (~10-30 seconds).
 */
public class GameRecorderFuncTest extends TestCase
{
    private static final String CUSTOM_PATH = TestConstants.TEST_COLOSSUS_HOME;

    public GameRecorderFuncTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        // Full game - no early termination
        Constants.END_AFTER_FIRST_BATTLE = false;
    }

    @Override
    protected void tearDown()
    {
        Constants.END_AFTER_FIRST_BATTLE = false;
    }

    /**
     * Run a real AI game that ends after first battle.
     * Verify the recording contains actual game events.
     */
    public void testRealGameProducesFullRecording() throws Exception
    {
        Options.setStartupTest(false);
        ErrorUtils.clearErrorDuringFunctionalTest();

        Options startOptions = new Options(Constants.OPTIONS_START, true);
        startOptions.setOption(Options.autoPickColor, true);
        startOptions.setOption(Options.autoPlay, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            CUSTOM_PATH, false, true);
        serverOptions.loadOptions();
        serverOptions.setOption(Options.autoQuit, true);
        serverOptions.setOption(Options.autoStop, false);

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);

        game.startNewGameAndWaitUntilOver(null);

        // Verify recording exists and has real game events
        String path = GameRecorder.getLastRecordingPath();
        assertNotNull("Recording path should exist", path);
        assertTrue("Recording file should exist", new File(path).exists());

        // Parse and verify
        GameReplay replay = GameReplay.load(path);
        assertNotNull("Replay should load", replay);
        assertTrue("Recording should have many events (got "
            + replay.getEventCount() + ")",
            replay.getEventCount() > 50);

        // Must have actual gameplay events including moves and battles
        assertTrue("Recording must contain doSplit events",
            replay.findEvents("doSplit").size() > 0);
        assertTrue("Recording must contain doneWithSplits",
            replay.findEvents("doneWithSplits").size() > 0);
    }
}
