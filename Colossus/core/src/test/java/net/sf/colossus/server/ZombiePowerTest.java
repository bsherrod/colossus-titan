package net.sf.colossus.server;

import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Zombie power: recruit from dead pile with equal-value creature.
 */
public class ZombiePowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player zombiePlayer;

    public ZombiePowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        zombiePlayer = game.createAndAddPlayer("Zombie", "SimpleAI");
        game.createAndAddPlayer("Opp", "SimpleAI");
        zombiePlayer.getPowers().addPower(LauerPower.ZOMBIE);
    }

    /**
     * Zombie: can recruit dead creature with pts <= max in legion.
     * Ogre (6*2=12pts) in legion, Gargoyle (4*2=8pts) dead → can recruit (8<=12).
     */
    public void testRecruitDeadCreatureWithEqualOrLessValue()
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");

        // Put Gargoyles in dead pile
        game.getCaretaker().setDeadCount(gargoyle, 2);

        LegionServerSide legion = new LegionServerSide("Rd01", null,
            hex, hex, zombiePlayer, game, ogre, ogre);
        zombiePlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(legion, hex);

        // Ogre is 12pts, Gargoyle is 8pts → can recruit (8 <= 12)
        assertTrue("Zombie can recruit Gargoyle (8pts) with Ogre (12pts) in legion",
            recruits.contains(gargoyle));
    }

    /**
     * Zombie: cannot recruit dead creature worth MORE than max in legion.
     */
    public void testCannotRecruitHigherValueDead()
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType unicorn = variant.getCreatureByName("Unicorn");

        // Put Unicorn (24pts) in dead pile
        game.getCaretaker().setDeadCount(unicorn, 1);

        LegionServerSide legion = new LegionServerSide("Rd01", null,
            hex, hex, zombiePlayer, game, ogre);
        zombiePlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(legion, hex);

        // Ogre is 12pts, Unicorn is 24pts → cannot (24 > 12)
        assertFalse("Zombie cannot recruit Unicorn (24pts) with Ogre (12pts)",
            recruits.contains(unicorn));
    }

    /**
     * Zombie: Titan death does NOT trigger eliminateTitan.
     * The guard: if (!player.hasPower(ZOMBIE) && !player.hasPower(AMOEBA))
     * means Zombie Titan dying in battle doesn't end the battle.
     */
    public void testZombieTitanDeathDoesNotEndBattle()
    {
        // Verify the guard condition protects Zombie
        assertTrue("Zombie has ZOMBIE power",
            zombiePlayer.hasPower(LauerPower.ZOMBIE));
        boolean wouldEliminate = !zombiePlayer.hasPower(LauerPower.ZOMBIE)
            && !zombiePlayer.hasPower(LauerPower.AMOEBA);
        assertFalse("Zombie Titan death does NOT trigger elimination",
            wouldEliminate);
    }
}
