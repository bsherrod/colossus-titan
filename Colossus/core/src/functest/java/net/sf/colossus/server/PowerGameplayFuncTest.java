package net.sf.colossus.server;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.TestConstants;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.Dice;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.Variant;


/**
 * Real full-game smoke tests for all 22 Lauer Powers.
 * Each test runs a complete AI game with the power active.
 * If a game crashes, hangs, or produces SEVERE errors, the power is BROKEN.
 *
 * This is Level 1 testing — the MINIMUM for every power.
 * Formula tests (Level 2) are worthless if this doesn't pass.
 *
 * Each test takes ~60-120 seconds. Run individually:
 *   ant singlefunctest -Dfunctest.class=net.sf.colossus.server.PowerGameplayFuncTest
 */
public class PowerGameplayFuncTest extends TestCase
{
    private static final String CUSTOM_PATH = TestConstants.TEST_COLOSSUS_HOME;

    public PowerGameplayFuncTest(String name)
    {
        super(name);
    }

    private void runGameWithPower(String power) throws IOException
    {
        Dice.captureSeed(); // deterministic
        Options.setStartupTest(false);
        Constants.END_AFTER_FIRST_BATTLE = false;
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
        serverOptions.setOption(Options.playerPowers + "0", power);
        serverOptions.setOption(Options.playerPowers + "1", "");
        serverOptions.setOption(Options.playerPowers + "2", "");

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);

        game.startNewGameAndWaitUntilOver(null);

        // Must complete without errors
        assertFalse(power + " game had SEVERE errors",
            ErrorUtils.checkErrorDuringFunctionalTest());

        // Must have actually played (not exited early)
        String recording = GameRecorder.getLastRecordingPath();
        assertNotNull(power + " must produce a recording", recording);
        assertTrue(power + " recording file exists",
            new File(recording).exists());

        GameReplay replay = GameReplay.load(recording);
        assertTrue(power + " must have doSplit events (game played)",
            replay.findEvents("doSplit").size() > 0);
        // Verify game survived multiple turns
        int maxTurn = 0;
        for (GameReplay.ReplayEvent e : replay.getEvents())
        {
            if (e.turn > maxTurn) maxTurn = e.turn;
        }
        assertTrue(power + " must survive multiple turns (got "
            + maxTurn + ")", maxTurn > 2);
    }

    // === One test per power ===

    public void testAdministrator() throws Exception
    { runGameWithPower("ADMINISTRATOR"); }

    public void testArcher() throws Exception
    { runGameWithPower("ARCHER"); }

    public void testArchmage() throws Exception
    { runGameWithPower("ARCHMAGE"); }

    public void testArmourer() throws Exception
    { runGameWithPower("ARMOURER"); }

    public void testAvian() throws Exception
    { runGameWithPower("AVIAN"); }

    public void testBerserker() throws Exception
    { runGameWithPower("BERSERKER"); }

    public void testDemon() throws Exception
    { runGameWithPower("DEMON"); }

    public void testFolks() throws Exception
    { runGameWithPower("THE_FOLKS"); }

    public void testHealer() throws Exception
    { runGameWithPower("HEALER"); }

    public void testKnight() throws Exception
    { runGameWithPower("KNIGHT"); }

    public void testLivingDead() throws Exception
    { runGameWithPower("LIVING_DEAD"); }

    public void testLordOfTrees() throws Exception
    { runGameWithPower("LORD_OF_THE_TREES"); }

    public void testMarines() throws Exception
    { runGameWithPower("MARINES"); }

    public void testMulligan() throws Exception
    { runGameWithPower("MULLIGAN"); }

    public void testNineBall() throws Exception
    { runGameWithPower("NINE_BALL"); }

    public void testNinja() throws Exception
    { runGameWithPower("NINJA"); }

    public void testProphetess() throws Exception
    { runGameWithPower("PROPHETESS"); }

    public void testUpsideDownCake() throws Exception
    { runGameWithPower("UPSIDE_DOWN_CAKE"); }

    public void testVampire() throws Exception
    { runGameWithPower("VAMPIRE"); }

    public void testYahtzee() throws Exception
    { runGameWithPower("YAHTZEE"); }

    public void testZombie() throws Exception
    { runGameWithPower("ZOMBIE"); }

    public void testAmoeba() throws Exception
    { runGameWithPower("AMOEBA"); }
}
