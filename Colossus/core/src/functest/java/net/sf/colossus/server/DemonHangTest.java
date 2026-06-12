package net.sf.colossus.server;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.TestConstants;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.Dice;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.Variant;


/**
 * Isolated test for Demon power hang bug.
 */
public class DemonHangTest extends TestCase
{
    public DemonHangTest(String name)
    {
        super(name);
    }

    public void testDemonGameCompletes() throws Exception
    {
        // Enable file logging to diagnose hang
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        java.util.logging.FileHandler fh = new java.util.logging.FileHandler("/tmp/demon-game.log");
        fh.setFormatter(new java.util.logging.SimpleFormatter());
        rootLogger.addHandler(fh);
        rootLogger.setLevel(java.util.logging.Level.INFO);

        Dice.captureSeed();
        Options.setStartupTest(false);
        Constants.END_AFTER_FIRST_BATTLE = true;
        ErrorUtils.clearErrorDuringFunctionalTest();

        Options startOptions = new Options(Constants.OPTIONS_START, true);
        startOptions.setOption(Options.autoPickColor, true);
        startOptions.setOption(Options.autoPlay, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            TestConstants.TEST_COLOSSUS_HOME, false, true);
        serverOptions.loadOptions();
        serverOptions.setOption(Options.autoQuit, true);
        serverOptions.setOption(Options.autoStop, false);
        serverOptions.setOption(Options.playerName + "0", "Demon0");
        serverOptions.setOption(Options.playerType + "0", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "0", "DEMON");
        serverOptions.setOption(Options.playerName + "1", "Opp1");
        serverOptions.setOption(Options.playerType + "1", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "1", "");
        serverOptions.setOption(Options.playerName + "2", "Opp2");
        serverOptions.setOption(Options.playerType + "2", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "2", "");

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);
        game.startNewGameAndWaitUntilOver(null);

        assertFalse("No errors", ErrorUtils.checkErrorDuringFunctionalTest());
    }
}
