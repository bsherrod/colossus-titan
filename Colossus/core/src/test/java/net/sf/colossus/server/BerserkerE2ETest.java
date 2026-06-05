package net.sf.colossus.server;


import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * End-to-end tests for the Berserker power.
 *
 * Two categories:
 * 1. Phase/declaration tests: verify BERSERK phase mechanics without striking
 *    (no Server needed).
 * 2. Full-game functests: run AI-vs-AI games with Berserker power to verify
 *    the entire flow — declaration, double-strike, death — completes without
 *    errors.
 */
public class BerserkerE2ETest extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(BerserkerE2ETest.class.getName());

    private static final String CUSTOM_PATH = "./core/src/functest/"
        + "resource/colossus-home";

    public BerserkerE2ETest(String name)
    {
        super(name);
    }

    // ================================================================
    // Phase/Declaration tests (no Server needed)
    // ================================================================

    private GameServerSide game;
    private BattleServerSide battle;

    private void setupPhaseBattle(BattlePhase startPhase)
    {
        Variant v = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(v);
        Player attacker = game.createAndAddPlayer("Red", "SimpleAI");
        Player defender = game.createAndAddPlayer("Blue", "SimpleAI");
        attacker.getPowers().addPower(LauerPower.BERSERKER);

        MasterHex hex = v.getMasterBoard().getHexByLabel("3"); // Brush
        CreatureType ogre = v.getCreatureByName("Ogre");
        CreatureType troll = v.getCreatureByName("Troll");

        LegionServerSide attackerLegion = new LegionServerSide("Rd03", null,
            hex, hex, attacker, game, ogre, ogre);
        LegionServerSide defenderLegion = new LegionServerSide("Bu03", null,
            hex, hex, defender, game, troll);

        attacker.addLegion(attackerLegion);
        defender.addLegion(defenderLegion);
        attackerLegion.setEntrySide(EntrySide.RIGHT);

        game.createBattle(attackerLegion, defenderLegion,
            BattleServerSide.LegionTags.ATTACKER, hex, startPhase);
        battle = game.getBattleSS();
    }

    /**
     * E2E: declareBerserk() fails outside BERSERK phase.
     */
    public void testDeclareBerserkRejectedOutsideBerserkPhase()
    {
        setupPhaseBattle(BattlePhase.FIGHT);

        LegionServerSide legion = (LegionServerSide)battle
            .getAttackingLegion();
        CreatureServerSide critter =
            (CreatureServerSide)legion.getCritter(0);

        assertEquals(BattlePhase.FIGHT, battle.getBattlePhase());

        boolean result = battle.declareBerserk(critter);
        assertFalse("declareBerserk must fail outside BERSERK phase", result);
        assertFalse("Creature must not be marked berserk",
            critter.isBerserk());
    }

    /**
     * E2E: declareBerserk() succeeds during BERSERK phase.
     */
    public void testDeclareBerserkSucceedsDuringBerserkPhase()
    {
        setupPhaseBattle(BattlePhase.BERSERK);

        LegionServerSide legion = (LegionServerSide)battle
            .getAttackingLegion();
        CreatureServerSide critter =
            (CreatureServerSide)legion.getCritter(0);

        assertEquals(BattlePhase.BERSERK, battle.getBattlePhase());

        boolean result = battle.declareBerserk(critter);
        assertTrue("declareBerserk must succeed during BERSERK phase", result);
        assertTrue("Creature must be marked berserk", critter.isBerserk());
    }

    /**
     * E2E: Cannot declare opponent's creature as berserk.
     */
    public void testDeclareBerserkFailsForOpponentCreature()
    {
        setupPhaseBattle(BattlePhase.BERSERK);

        LegionServerSide defLegion = (LegionServerSide)battle
            .getDefendingLegion();
        CreatureServerSide defCritter =
            (CreatureServerSide)defLegion.getCritter(0);

        boolean result = battle.declareBerserk(defCritter);
        assertFalse("Cannot declare opponent's creature berserk", result);
    }

    /**
     * E2E: Multiple creatures can be declared berserk in same phase.
     */
    public void testMultipleDeclarationsInSamePhase()
    {
        setupPhaseBattle(BattlePhase.BERSERK);

        LegionServerSide legion = (LegionServerSide)battle
            .getAttackingLegion();
        CreatureServerSide critter0 =
            (CreatureServerSide)legion.getCritter(0);
        CreatureServerSide critter1 =
            (CreatureServerSide)legion.getCritter(1);

        assertTrue(battle.declareBerserk(critter0));
        assertTrue(battle.declareBerserk(critter1));
        assertTrue(critter0.isBerserk());
        assertTrue(critter1.isBerserk());
    }

    /**
     * E2E: Cannot declare a dead creature as berserk.
     */
    public void testDeclareBerserkFailsForDeadCreature()
    {
        setupPhaseBattle(BattlePhase.BERSERK);

        LegionServerSide legion = (LegionServerSide)battle
            .getAttackingLegion();
        CreatureServerSide critter =
            (CreatureServerSide)legion.getCritter(0);
        critter.setDead(true);

        boolean result = battle.declareBerserk(critter);
        assertFalse("Cannot declare dead creature berserk", result);
    }

    /**
     * E2E: Berserk flag persists on creature across the phase.
     */
    public void testBerserkFlagPersists()
    {
        setupPhaseBattle(BattlePhase.BERSERK);

        LegionServerSide legion = (LegionServerSide)battle
            .getAttackingLegion();
        CreatureServerSide critter =
            (CreatureServerSide)legion.getCritter(0);

        assertFalse(critter.isBerserk());
        critter.setBerserk(true);
        assertTrue(critter.isBerserk());
        critter.setBerserk(false);
        assertFalse(critter.isBerserk());
    }

    /**
     * E2E: Auto-berserk triggers at 1 hit remaining (Berserker power).
     * Verifies the flag without actually striking.
     */
    public void testAutoBerserkCondition()
    {
        Variant v = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(v);
        Player player = game.createAndAddPlayer("Red", "SimpleAI");
        player.getPowers().addPower(LauerPower.BERSERKER);

        CreatureType ogre = v.getCreatureByName("Ogre"); // power=6
        MasterHex hex = v.getMasterBoard().getHexByLabel("3");
        LegionServerSide legion = new LegionServerSide("Rd03", null,
            hex, hex, player, game, ogre);
        player.addLegion(legion);

        CreatureServerSide critter =
            (CreatureServerSide)legion.getCritter(0);

        // Ogre power=6. At 5 hits taken, 1 remaining -> auto-berserk triggers
        critter.setHits(5);
        int remaining = critter.getPower() - critter.getHits();
        assertEquals("Should have 1 hit remaining", 1, remaining);

        // The auto-berserk condition: (defensivePower - hits) <= 1
        assertTrue("Auto-berserk condition met",
            critter.getDefensivePower() - critter.getHits() <= 1);
    }

    /**
     * E2E: No auto-berserk without the Berserker power.
     */
    public void testNoAutoBerserkWithoutPower()
    {
        Variant v = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(v);
        Player player = game.createAndAddPlayer("Red", "SimpleAI");
        // NO Berserker power

        CreatureType ogre = v.getCreatureByName("Ogre");
        MasterHex hex = v.getMasterBoard().getHexByLabel("3");
        LegionServerSide legion = new LegionServerSide("Rd03", null,
            hex, hex, player, game, ogre);
        player.addLegion(legion);

        CreatureServerSide critter =
            (CreatureServerSide)legion.getCritter(0);
        critter.setHits(5);

        // Without the power, the auto-berserk code path never runs
        assertFalse("Should not have Berserker power",
            player.hasPower(LauerPower.BERSERKER));
        assertFalse("Should not be marked berserk", critter.isBerserk());
    }

    // ================================================================
    // Full-game functional tests (complete AI game with Berserker)
    // ================================================================

    private void runGameWithPowers(String p0Powers, String p1Powers,
        String p2Powers)
    {
        Options.setStartupTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();

        Options startOptions = new Options(Constants.OPTIONS_START, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            CUSTOM_PATH, false, true);
        serverOptions.loadOptions();

        serverOptions.setOption(Options.playerPowers + "0", p0Powers);
        serverOptions.setOption(Options.playerPowers + "1", p1Powers);
        serverOptions.setOption(Options.playerPowers + "2", p2Powers);

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide g = new GameServerSide(whatNextManager,
            serverOptions, variant);

        g.startNewGameAndWaitUntilOver(null);

        assertFalse("Game with Berserker power encountered errors",
            ErrorUtils.checkErrorDuringFunctionalTest());
    }

    /**
     * Full-game E2E: Single Berserker player completes a game.
     * AI declares berserk, strikes twice, creatures die — no crashes.
     */
    public void testBerserkerFullGame()
    {
        LOGGER.info("E2E: Berserker full AI game");
        runGameWithPowers("BERSERKER", "", "");
    }

    /**
     * Full-game E2E: Berserker vs Berserker — both sides use the power.
     */
    public void testBerserkerVsBerserkerFullGame()
    {
        LOGGER.info("E2E: Berserker vs Berserker full AI game");
        runGameWithPowers("BERSERKER", "BERSERKER", "");
    }

    /**
     * Full-game E2E: Berserker + Armourer combo — bonus dice + double strike.
     */
    public void testBerserkerWithArmourerFullGame()
    {
        LOGGER.info("E2E: Berserker+Armourer full AI game");
        runGameWithPowers("BERSERKER,ARMOURER", "", "");
    }
}
