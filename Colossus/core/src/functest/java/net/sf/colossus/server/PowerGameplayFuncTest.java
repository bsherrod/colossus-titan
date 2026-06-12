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
 * Real full-game smoke tests for Lauer Powers.
 * Runs AI games through first battle with each power active.
 * If a game crashes or produces SEVERE errors, the power is BROKEN.
 *
 *   ant singlefunctest -Dfunctest.class=net.sf.colossus.server.PowerGameplayFuncTest
 */
public class PowerGameplayFuncTest extends TestCase
{
    public PowerGameplayFuncTest(String name)
    {
        super(name);
    }

    private void runGameWithPower(String power)
    {
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
        serverOptions.setOption(Options.playerName + "0", "Power0");
        serverOptions.setOption(Options.playerType + "0", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "0", power);
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

        assertFalse(power + " game had SEVERE errors",
            ErrorUtils.checkErrorDuringFunctionalTest());
    }

    public void testAdministrator() { runGameWithPower("ADMINISTRATOR"); }
    public void testArcher() { runGameWithPower("ARCHER"); }
    public void testArchmage() { runGameWithPower("ARCHMAGE"); }
    public void testArmourer() { runGameWithPower("ARMOURER"); }
    public void testAvian() { runGameWithPower("AVIAN"); }
    public void testBerserker() { runGameWithPower("BERSERKER"); }
    public void testDemon() { runGameWithPower("DEMON"); }
    public void testFolks() { runGameWithPower("THE_FOLKS"); }
    public void testHealer() { runGameWithPower("HEALER"); }
    public void testKnight() { runGameWithPower("KNIGHT"); }
    public void testLivingDead() { runGameWithPower("LIVING_DEAD"); }
    public void testLordOfTrees() { runGameWithPower("LORD_OF_THE_TREES"); }
}
