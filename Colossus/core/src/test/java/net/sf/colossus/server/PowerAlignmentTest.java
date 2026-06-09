package net.sf.colossus.server;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerPowers;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.Variant;


/**
 * Test that Lauer Powers assigned in GetPlayers end up on the correct
 * player after game initialization.
 */
public class PowerAlignmentTest extends TestCase
{
    private static final String CUSTOM_PATH = "./core/src/functest/"
        + "resource/colossus-home";

    public PowerAlignmentTest(String name)
    {
        super(name);
    }

    /**
     * BUG: Power assigned to player 0 in UI ends up on wrong player.
     * Simulates: 3 players, MULLIGAN assigned to player 0 only.
     * Verifies player 0 (first in getPlayers()) has MULLIGAN.
     */
    public void testPowerAssignedToFirstPlayerLandsOnFirstPlayer()
    {
        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = GameServerSide.makeNewGameServerSide(variant);

        // Create players directly (as addPlayersFromOptions would)
        Player p0 = game.createAndAddPlayer("Red", "SimpleAI");
        Player p1 = game.createAndAddPlayer("Blue", "SimpleAI");
        Player p2 = game.createAndAddPlayer("Green", "SimpleAI");

        // Simulate what the server does: read powers from options
        // Options.playerPowers + "0" = "MULLIGAN" (player 0 has it)
        Options opts = new Options("UnitTest", true);
        opts.setOption(Options.playerPowers + "0", "MULLIGAN");

        // Apply powers the same way GameServerSide.addPlayersFromOptions does
        int playerIdx = 0;
        for (Player player : game.getPlayers())
        {
            String powersStr = opts.getStringOption(
                Options.playerPowers + playerIdx);
            if (powersStr != null && !powersStr.isEmpty())
            {
                player.setPowers(PlayerPowers.deserialize(powersStr));
            }
            playerIdx++;
        }

        // THE KEY ASSERTION: player 0 must have MULLIGAN
        assertTrue("Player 0 (Red) must have MULLIGAN",
            p0.hasPower(LauerPower.MULLIGAN));
        assertFalse("Player 1 (Blue) must NOT have MULLIGAN",
            p1.hasPower(LauerPower.MULLIGAN));
        assertFalse("Player 2 (Green) must NOT have MULLIGAN",
            p2.hasPower(LauerPower.MULLIGAN));
    }

    /**
     * Test with power on player 1 (not 0) - verify alignment.
     */
    public void testPowerAssignedToSecondPlayerLandsOnSecondPlayer()
    {
        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = GameServerSide.makeNewGameServerSide(variant);

        Player p0 = game.createAndAddPlayer("Red", "SimpleAI");
        Player p1 = game.createAndAddPlayer("Blue", "SimpleAI");
        Player p2 = game.createAndAddPlayer("Green", "SimpleAI");

        Options opts = new Options("UnitTest", true);
        opts.setOption(Options.playerPowers + "1", "MULLIGAN");

        int playerIdx = 0;
        for (Player player : game.getPlayers())
        {
            String powersStr = opts.getStringOption(
                Options.playerPowers + playerIdx);
            if (powersStr != null && !powersStr.isEmpty())
            {
                player.setPowers(PlayerPowers.deserialize(powersStr));
            }
            playerIdx++;
        }

        assertFalse("Player 0 (Red) must NOT have MULLIGAN",
            p0.hasPower(LauerPower.MULLIGAN));
        assertTrue("Player 1 (Blue) must have MULLIGAN",
            p1.hasPower(LauerPower.MULLIGAN));
        assertFalse("Player 2 (Green) must NOT have MULLIGAN",
            p2.hasPower(LauerPower.MULLIGAN));
    }

    /**
     * BUG FIX: clearPlayerInfo() must also clear playerPowers entries.
     * Without this fix, stale powers from a previous game persist in the
     * options and get assigned to the wrong player on the next game.
     */
    public void testClearPlayerInfoAlsoClearsPowers()
    {
        Options opts = new Options("UnitTest", true);

        // Simulate: previous game left these in the config
        opts.setOption(Options.playerPowers + "0", "BERSERKER");
        opts.setOption(Options.playerPowers + "1", "MULLIGAN");
        opts.setOption(Options.playerName + "0", "OldRed");
        opts.setOption(Options.playerType + "0", "SimpleAI");

        // Verify they're set
        assertEquals("BERSERKER",
            opts.getStringOption(Options.playerPowers + "0"));

        // clearPlayerInfo should clear ALL player data including powers
        opts.clearPlayerInfo();

        // Powers must be gone
        String p0power = opts.getStringOption(Options.playerPowers + "0");
        assertTrue("clearPlayerInfo must clear playerPowers entries",
            p0power == null || p0power.isEmpty());
        String p1power = opts.getStringOption(Options.playerPowers + "1");
        assertTrue("clearPlayerInfo must clear playerPowers entries",
            p1power == null || p1power.isEmpty());
    }

    /**
     * END-TO-END: Full game with Human player having MULLIGAN.
     * Uses autoPickColor + autoPlay so no GUI dialogs block.
     * Verifies the Human player object ends up with the power after
     * tower sort reorders players.
     */
    public void testEndToEndHumanPlayerGetsPowerAfterTowerSort()
    {
        Options.setStartupTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();

        Options startOptions = new Options(Constants.OPTIONS_START, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            CUSTOM_PATH, false, true);
        serverOptions.loadOptions();

        // clearPlayerInfo removes stale powers (the fix)
        serverOptions.clearPlayerInfo();

        // Set up 3 players: Human + 2 AI
        serverOptions.setOption(Options.playerName + "0", "sherrodz");
        serverOptions.setOption(Options.playerType + "0", "Human");
        serverOptions.setOption(Options.playerName + "1", "Rational1");
        serverOptions.setOption(Options.playerType + "1", "RationalAI");
        serverOptions.setOption(Options.playerName + "2", "Rational2");
        serverOptions.setOption(Options.playerType + "2", "RationalAI");

        // Assign MULLIGAN to player 0 (Human)
        serverOptions.setOption(Options.playerPowers + "0", "MULLIGAN");

        // Script Human behavior: auto-pick color, auto-play all phases
        serverOptions.setOption(Options.autoPickColor, "true");
        serverOptions.setOption(Options.autoPlay, "true");

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);
        game.startNewGameAndWaitUntilOver(null);

        // Find the human player by name (tower sort reorders players)
        Player humanPlayer = null;
        for (Player p : game.getPlayers())
        {
            if (p.getName().equals("sherrodz"))
            {
                humanPlayer = p;
                break;
            }
        }

        assertNotNull("sherrodz must be in the game", humanPlayer);
        assertTrue("Human player (sherrodz) must have MULLIGAN after "
            + "tower sort. Powers: " + humanPlayer.getPowers(),
            humanPlayer.hasPower(LauerPower.MULLIGAN));

        // No other player should have MULLIGAN
        for (Player p : game.getPlayers())
        {
            if (!p.getName().equals("sherrodz"))
            {
                assertFalse(p.getName() + " must NOT have MULLIGAN",
                    p.hasPower(LauerPower.MULLIGAN));
            }
        }

        assertFalse("Game encountered errors",
            ErrorUtils.checkErrorDuringFunctionalTest());
    }

    /**
     * BUG #2: Powers assigned to Human player leak to AI players.
     * Scenario: Human gets BERSERKER+MULLIGAN, 2 AIs get nothing.
     * After game, ONLY the human should have powers.
     *
     * Root cause: post-sort resync only sends non-empty power entries
     * but never clears stale entries on clients. If pre-sort sync set
     * playerPowers0=BERSERKER,MULLIGAN for original index 0, and after
     * sort the human moves to index 1, the resync writes index 1 correctly
     * but never clears index 0 on the client.
     */
    public void testPowersDoNotLeakToAIPlayers()
    {
        Options.setStartupTest(true);
        ErrorUtils.clearErrorDuringFunctionalTest();

        Options startOptions = new Options(Constants.OPTIONS_START, true);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME,
            CUSTOM_PATH, false, true);
        serverOptions.loadOptions();
        serverOptions.clearPlayerInfo();

        // Human with BERSERKER+MULLIGAN, 2 AIs with nothing
        serverOptions.setOption(Options.playerName + "0", "sherrodz");
        serverOptions.setOption(Options.playerType + "0", "Human");
        serverOptions.setOption(Options.playerName + "1", "AI_One");
        serverOptions.setOption(Options.playerType + "1", "RationalAI");
        serverOptions.setOption(Options.playerName + "2", "AI_Two");
        serverOptions.setOption(Options.playerType + "2", "RationalAI");

        serverOptions.setOption(Options.playerPowers + "0",
            "BERSERKER,MULLIGAN");
        // Explicitly NO powers for indices 1 and 2

        serverOptions.setOption(Options.autoPickColor, "true");
        serverOptions.setOption(Options.autoPlay, "true");

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = new GameServerSide(whatNextManager,
            serverOptions, variant);
        game.startNewGameAndWaitUntilOver(null);

        // Verify: ONLY sherrodz has powers
        for (Player p : game.getPlayers())
        {
            if (p.getName().equals("sherrodz"))
            {
                assertTrue("sherrodz must have BERSERKER",
                    p.hasPower(LauerPower.BERSERKER));
                assertTrue("sherrodz must have MULLIGAN",
                    p.hasPower(LauerPower.MULLIGAN));
            }
            else
            {
                assertFalse(p.getName() + " must NOT have MULLIGAN "
                    + "(powers leaked!). Powers: " + p.getPowers(),
                    p.hasPower(LauerPower.MULLIGAN));
                assertFalse(p.getName() + " must NOT have BERSERKER "
                    + "(powers leaked!). Powers: " + p.getPowers(),
                    p.hasPower(LauerPower.BERSERKER));
            }
        }

        assertFalse("Game encountered errors",
            ErrorUtils.checkErrorDuringFunctionalTest());
    }

    /**
     * BUG #1: Human player should get a color picker (or at minimum,
     * autoPickColor must NOT be set unless explicitly configured).
     * Verifies that after clearPlayerInfo + fresh setup with NO autoPickColor,
     * the autoPickColor option is NOT set.
     */
    public void testAutoPickColorNotSetByDefault()
    {
        Options serverOptions = new Options("UnitTest", true);
        serverOptions.clearPlayerInfo();

        serverOptions.setOption(Options.playerName + "0", "sherrodz");
        serverOptions.setOption(Options.playerType + "0", "Human");
        serverOptions.setOption(Options.playerName + "1", "AI_One");
        serverOptions.setOption(Options.playerType + "1", "RationalAI");

        // User did NOT set autoPickColor — it should be false
        assertFalse("autoPickColor must NOT be set by default for Human",
            serverOptions.getOption(Options.autoPickColor));
        assertFalse("autoPlay must NOT be set by default for Human",
            serverOptions.getOption(Options.autoPlay));
    }

}
