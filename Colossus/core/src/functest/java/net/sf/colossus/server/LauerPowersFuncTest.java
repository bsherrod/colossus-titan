package net.sf.colossus.server;


import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.TestConstants;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.Variant;


/**
 * Functional test: runs AI-vs-AI games with Lauer Powers assigned.
 * Verifies that powers don't cause crashes during actual gameplay.
 *
 * Each test assigns specific powers to players and runs a full game.
 * If the game completes without exceptions, the power is considered
 * functional at a basic level.
 */
public class LauerPowersFuncTest extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(LauerPowersFuncTest.class.getName());

    private final static String CUSTOM_PATH = TestConstants.TEST_COLOSSUS_HOME;

    public LauerPowersFuncTest(String name)
    {
        super(name);
    }

    private void runGameWithPowers(String player0Powers, String player1Powers,
        String player2Powers)
    {
        Options.setStartupTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();

        Options startOptions = new Options(Constants.OPTIONS_START, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            CUSTOM_PATH, false, true);
        serverOptions.loadOptions();

        // Assign powers to players
        serverOptions.setOption(Options.playerPowers + "0", player0Powers);
        serverOptions.setOption(Options.playerPowers + "1", player1Powers);
        serverOptions.setOption(Options.playerPowers + "2", player2Powers);

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);

        game.startNewGameAndWaitUntilOver(null);

        assertFalse("Game with powers encountered errors",
            ErrorUtils.checkErrorDuringFunctionalTest());
    }

    /**
     * Test: Mulligan power doesn't crash (first-turn teleport, two dice).
     */
    public void testMulliganGame()
    {
        LOGGER.info("test: Mulligan power game");
        runGameWithPowers("MULLIGAN", "", "");
    }

    /**
     * Test: Armourer + Marines passive combat modifiers.
     */
    public void testPassiveCombatPowers()
    {
        LOGGER.info("test: Armourer vs Marines");
        runGameWithPowers("ARMOURER", "MARINES", "");
    }

    /**
     * Test: Yahtzee + Berserker combat dice powers.
     */
    public void testCombatDicePowers()
    {
        LOGGER.info("test: Yahtzee + Berserker");
        runGameWithPowers("YAHTZEE", "BERSERKER", "");
    }

    /**
     * Test: Nine Ball stacking limit.
     */
    public void testNineBallGame()
    {
        LOGGER.info("test: Nine Ball stacking");
        runGameWithPowers("NINE_BALL", "", "");
    }

    /**
     * Test: Archer rangestrike expansion.
     */
    public void testArcherGame()
    {
        LOGGER.info("test: Archer rangestrike");
        runGameWithPowers("ARCHER", "", "");
    }

    /**
     * Test: Avian all-fly + Lord of Trees recruiting.
     */
    public void testAvianAndLordOfTrees()
    {
        LOGGER.info("test: Avian + Lord of Trees");
        runGameWithPowers("AVIAN", "LORD_OF_THE_TREES", "");
    }

    /**
     * Test: Vampire overrun + no-flee.
     */
    public void testVampireGame()
    {
        LOGGER.info("test: Vampire");
        runGameWithPowers("VAMPIRE", "", "");
    }

    /**
     * Test: Multiple powers on same player.
     */
    public void testMultiplePowers()
    {
        LOGGER.info("test: Multiple powers");
        runGameWithPowers("ARMOURER,KNIGHT,YAHTZEE", "MARINES,BERSERKER", "");
    }

    /**
     * Test: Recruiting powers (Administrator, The Folks, Zombie).
     */
    public void testRecruitingPowers()
    {
        LOGGER.info("test: Recruiting powers");
        runGameWithPowers("ADMINISTRATOR", "THE_FOLKS", "ZOMBIE");
    }

    /**
     * Test: Movement powers (Upside-Down Cake, Ninja).
     */
    public void testMovementPowers()
    {
        LOGGER.info("test: Movement powers");
        runGameWithPowers("UPSIDE_DOWN_CAKE", "NINJA", "MULLIGAN");
    }

    /**
     * Test: Berserker declaration phase works end-to-end.
     * AI auto-declares berserk via setupBerserk(), advances to FIGHT,
     * creature strikes twice and dies. Verifies no crashes.
     */
    public void testBerserkerDeclarationPhaseE2E()
    {
        LOGGER.info("test: Berserker declaration phase E2E");
        runGameWithPowers("BERSERKER", "ARMOURER", "");
    }

    /**
     * Test: Zombie remove creatures + raise dead full game.
     */
    public void testZombieFullGame()
    {
        LOGGER.info("test: Zombie full game");
        runGameWithPowers("ZOMBIE", "", "");
    }

    /**
     * Test: Prophetess split-after-roll + carries full game.
     */
    public void testProphetessFullGame()
    {
        LOGGER.info("test: Prophetess full game");
        runGameWithPowers("PROPHETESS", "", "");
    }

    /**
     * Test: Amoeba all-legions-flee + no-angels full game.
     */
    public void testAmoebaFullGame()
    {
        LOGGER.info("test: Amoeba full game");
        runGameWithPowers("AMOEBA", "", "");
    }

    public void testDemonFullGame()
    {
        LOGGER.info("test: Demon full game");
        runGameWithPowers("DEMON", "", "");
    }

    public void testHealerFullGame()
    {
        LOGGER.info("test: Healer full game");
        runGameWithPowers("HEALER", "", "");
    }

    public void testLivingDeadFullGame()
    {
        LOGGER.info("test: Living Dead full game");
        runGameWithPowers("LIVING_DEAD", "", "");
    }

    public void testArchmageFullGame()
    {
        LOGGER.info("test: Archmage full game");
        runGameWithPowers("ARCHMAGE", "", "");
    }
}
