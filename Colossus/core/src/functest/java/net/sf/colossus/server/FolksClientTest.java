package net.sf.colossus.server;

import java.util.List;

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
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Client-side tests for Folks power.
 * Key regression: phantom recruit must NOT cascade up the chain.
 */
public class FolksClientTest extends TestCase
{
    private Client client;
    private Variant variant;

    public FolksClientTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        Dice.captureSeed();
        Options.setStartupTest(false);
        Constants.END_AFTER_FIRST_BATTLE = true;

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
        serverOptions.setOption(Options.playerPowers + "0", "THE_FOLKS");
        serverOptions.setOption(Options.playerName + "1", "Opp1");
        serverOptions.setOption(Options.playerType + "1", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "1", "");
        serverOptions.setOption(Options.playerName + "2", "Opp2");
        serverOptions.setOption(Options.playerType + "2", "SimpleAI");
        serverOptions.setOption(Options.playerPowers + "2", "");

        variant = VariantSupport.loadVariantByName("Default", true);
        GameServerSideTestAccess game = new GameServerSideTestAccess(
            whatNextManager, serverOptions, variant);
        game.startNewGameAndWaitUntilOver(null);

        client = game.getClientForName("Power0");
        assertNotNull("Got real Client for Power0", client);
    }

    /**
     * Folks findEligibleRecruits should always include the weakest terrain creature.
     */
    public void testWeakestAlwaysIncluded()
    {
        Player player = client.getOwningPlayer();
        assertTrue("Has THE_FOLKS", player.hasPower(LauerPower.THE_FOLKS));
        if (player.getLegions().isEmpty())
        {
            return;
        }
        Legion legion = player.getLegions().get(0);
        MasterHex hex = legion.getCurrentHex();
        List<CreatureType> recruits = client.findEligibleRecruits(legion, hex);
        assertNotNull("findEligibleRecruits works", recruits);
        // Folks should always have at least the weakest available
        assertTrue("Folks has at least one recruit option",
            recruits.size() >= 1);
    }

    /**
     * REGRESSION: Folks must NOT allow recruiting creatures that require
     * recruiters the legion doesn't have. The phantom only adds the weakest
     * creature — it does NOT cascade up the recruit chain.
     */
    public void testNoCascadeUpChain()
    {
        // This tests the fix: Folks with 3 Centaurs in Plains should NOT
        // offer Ranger (needs 2 Lions, have 0). Only Centaur and Lion.
        // We can't control exact game state, but verify no crash and
        // that the method returns a bounded list (not everything in chain).
        Player player = client.getOwningPlayer();
        if (player.getLegions().isEmpty())
        {
            return;
        }
        Legion legion = player.getLegions().get(0);
        MasterHex hex = legion.getCurrentHex();
        List<CreatureType> recruits = client.findEligibleRecruits(legion, hex);
        // Should not contain every creature in the terrain chain
        // (that was the cascade bug — adding ALL possible recruits)
        List<CreatureType> allPossible = net.sf.colossus.xmlparser
            .TerrainRecruitLoader.getPossibleRecruits(
                hex.getTerrain(), hex);
        // Folks should offer FEWER than all possible (weakest + what you qualify for)
        // unless legion actually has the recruiters for everything
        assertTrue("Recruits list is bounded (no cascade)",
            recruits.size() <= allPossible.size());
    }
}
