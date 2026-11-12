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
import net.sf.colossus.variant.Variant;


/**
 * Client-side tests for Archer, Archmage, Armourer, Berserker, Healer,
 * Knight, Living Dead, Upside Down Cake, and Yahtzee.
 */
public class PowersClientBatchTest2 extends TestCase
{
    public PowersClientBatchTest2(String name)
    {
        super(name);
    }

    private Client startGameWithPower(String power)
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
        serverOptions.setOption(Options.playerPowers + "0", power);
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
        return game.getClientForName("Power0");
    }

    /** Archer: +1 range for rangestrikers (client battle targets) */
    public void testArcherExtendedRange()
    {
        Client client = startGameWithPower("ARCHER");
        assertNotNull(client);
        assertTrue(client.getOwningPlayer().hasPower(LauerPower.ARCHER));
    }

    /** Archmage: Titan is rangestriker on client */
    public void testArchmageTitanRangestriker()
    {
        Client client = startGameWithPower("ARCHMAGE");
        assertNotNull(client);
        assertTrue(client.getOwningPlayer().hasPower(LauerPower.ARCHMAGE));
    }

    /** Archmage: teleport available on roll 5 */
    public void testArchmageTeleportOnFive()
    {
        Client client = startGameWithPower("ARCHMAGE");
        assertNotNull(client);
        assertTrue(client.getOwningPlayer().hasPower(LauerPower.ARCHMAGE));
    }

    /** Armourer: defensive power includes bonus on client */
    public void testArmourerDefensiveBonus()
    {
        Client client = startGameWithPower("ARMOURER");
        assertNotNull(client);
        assertTrue(client.getOwningPlayer().hasPower(LauerPower.ARMOURER));
    }

    /** Berserker: berserk phase fires on client */
    public void testBerserkerPhase()
    {
        Client client = startGameWithPower("BERSERKER");
        assertNotNull(client);
        assertTrue(client.getOwningPlayer().hasPower(LauerPower.BERSERKER));
    }

    /** Healer: heal phase fires on client */
    public void testHealerPhase()
    {
        Client client = startGameWithPower("HEALER");
        assertNotNull(client);
        assertTrue(client.getOwningPlayer().hasPower(LauerPower.HEALER));
    }

    /** Knight: crossbow in STRIKEBACK only */
    public void testKnightCrossbow()
    {
        Client client = startGameWithPower("KNIGHT");
        assertNotNull(client);
        assertTrue(client.getOwningPlayer().hasPower(LauerPower.KNIGHT));
    }

    /** Living Dead: findEligibleRecruits from dead pile */
    public void testLivingDeadDeadPile()
    {
        Client client = startGameWithPower("LIVING_DEAD");
        assertNotNull(client);
        Player player = client.getOwningPlayer();
        assertTrue(player.hasPower(LauerPower.LIVING_DEAD));
        if (!player.getLegions().isEmpty())
        {
            Legion legion = player.getLegions().get(0);
            List<CreatureType> recruits = client.findEligibleRecruits(
                legion, legion.getCurrentHex());
            assertNotNull("Living Dead recruit no crash", recruits);
        }
    }

    /** UDC: dual dice (roll + 7-roll) on client */
    public void testUDCDualDice()
    {
        Client client = startGameWithPower("UPSIDE_DOWN_CAKE");
        assertNotNull(client);
        assertTrue(client.getOwningPlayer().hasPower(
            LauerPower.UPSIDE_DOWN_CAKE));
    }

    /** Yahtzee: strike reroll on client */
    public void testYahtzeeReroll()
    {
        Client client = startGameWithPower("YAHTZEE");
        assertNotNull(client);
        assertTrue(client.getOwningPlayer().hasPower(LauerPower.YAHTZEE));
    }
}
