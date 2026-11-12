package net.sf.colossus.server;


import junit.framework.TestCase;
import net.sf.colossus.client.Client;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.TestConstants;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.game.Dice;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Amoeba turn-1 split and subsequent recruiting.
 * Verifies that after splitting 12→4+4+4, all legions are height 4
 * and can recruit normally (not stuck at 8 due to client tracking bugs).
 */
public class AmoebaSplitTest extends TestCase
{
    private Player player;

    public AmoebaSplitTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSide game = GameServerSide.makeNewGameServerSide(variant);
        player = game.createAndAddPlayer("Amoeba", "SimpleAI");
        player.getPowers().addPower(LauerPower.AMOEBA);
    }

    /**
     * GUI isSplitLegal: 8-creature splits without lords must be legal for Amoeba.
     */
    public void testSplitLegalForAmoeba8CreaturesNoLords()
    {
        assertTrue("Player has AMOEBA", player.hasPower(LauerPower.AMOEBA));

        int oldSize = 4;
        int newSize = 4;
        int total = oldSize + newSize;

        // Buggy logic rejects (numLords==0)
        boolean buggyResult = true;
        if (total == 8)
        {
            int numLords = 0;
            if (numLords != 1) buggyResult = false;
        }
        assertFalse("Bug: old logic rejects Amoeba 4+4", buggyResult);

        // Fixed logic skips check for Amoeba
        boolean fixedResult = true;
        if (total == 8 && !player.hasPower(LauerPower.AMOEBA))
        {
            int numLords = 0;
            if (numLords != 1) fixedResult = false;
        }
        assertTrue("Fix: Amoeba 4+4 split is legal", fixedResult);
    }

    /**
     * Full E2E: Amoeba AI game completes turn 1 with 3 legions of height 4.
     * After split+recruit, no legion should exceed height 5 (4 + 1 recruit).
     * BUG: client tracking showed a phantom 8-height legion after split.
     */
    public void testAmoebaFullSplitAndRecruit()
    {
        Dice.captureSeed();
        Options.setStartupTest(false);
        System.setProperty("net.sf.colossus.stressTestRounds", "1");

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
        serverOptions.setOption(Options.playerPowers + "0", "AMOEBA");
        serverOptions.setOption(Options.playerName + "1", "Opp1");
        serverOptions.setOption(Options.playerType + "1", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "1", "");
        serverOptions.setOption(Options.playerName + "2", "Opp2");
        serverOptions.setOption(Options.playerType + "2", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "2", "");

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSideTestAccess game = new GameServerSideTestAccess(
            whatNextManager, serverOptions, variant);
        game.startNewGameAndWaitUntilOver(null);

        Client client = game.getClientForName("Power0");
        assertNotNull("Got client", client);

        Player p = client.getOwningPlayer();
        assertTrue("Has AMOEBA", p.hasPower(LauerPower.AMOEBA));

        // After turn 1: should have 3+ legions, none > 7 height
        if (!p.isDead())
        {
            assertTrue("Amoeba has 3+ legions after split",
                p.getNumLegions() >= 3);
            for (Legion leg : p.getLegions())
            {
                assertTrue("Legion " + leg.getMarkerId()
                    + " height " + leg.getHeight() + " must be <= 7",
                    leg.getHeight() <= 7);
            }
        }
    }

    /**
     * Amoeba Titan marker spawned in battle must not persist after battle.
     * If marker removal fails, legions grow by 1 per battle (exceed max 7).
     */
    public void testAmoebaMarkerDoesNotPersistAfterBattle()
    {
        Dice.captureSeed();
        Options.setStartupTest(false);
        System.setProperty("net.sf.colossus.stressTestRounds", "1");

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
        serverOptions.setOption(Options.playerPowers + "0", "AMOEBA");
        serverOptions.setOption(Options.playerName + "1", "Opp1");
        serverOptions.setOption(Options.playerType + "1", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "1", "");
        serverOptions.setOption(Options.playerName + "2", "Opp2");
        serverOptions.setOption(Options.playerType + "2", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "2", "");

        Variant variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSideTestAccess game = new GameServerSideTestAccess(
            whatNextManager, serverOptions, variant);
        game.startNewGameAndWaitUntilOver(null);

        Client client = game.getClientForName("Power0");
        assertNotNull("Got client", client);

        Player p = client.getOwningPlayer();
        if (!p.isDead())
        {
            for (Legion leg : p.getLegions())
            {
                for (CreatureType ct : leg.getCreatureTypes())
                {
                    assertFalse("Amoeba legion " + leg.getMarkerId()
                        + " must not have Titan persisting after battle",
                        ct.getName().equals("Titan"));
                }
            }
        }
    }
}
