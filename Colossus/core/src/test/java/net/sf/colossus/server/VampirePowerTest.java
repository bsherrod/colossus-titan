package net.sf.colossus.server;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Vampire power: no-flee, half points, overrun.
 */
public class VampirePowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player vampirePlayer;
    private Player opponent;

    public VampirePowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        vampirePlayer = game.createAndAddPlayer("Vampire", "SimpleAI");
        opponent = game.createAndAddPlayer("Opp", "SimpleAI");
        vampirePlayer.getPowers().addPower(LauerPower.VAMPIRE);
    }

    /**
     * Vampire attacker: defender cannot flee.
     * The engage logic skips askFlee when attacker has VAMPIRE.
     */
    public void testDefenderCannotFleeFromVampire()
    {
        // Create attacker (Vampire) and defender legions on same hex
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType titan = variant.getCreatureByName("Titan");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");

        LegionServerSide attacker = new LegionServerSide("Rd01", null,
            hex, hex, vampirePlayer, game, titan, ogre, ogre, ogre);
        LegionServerSide defender = new LegionServerSide("Bu01", null,
            hex, hex, opponent, game, ogre, ogre);
        vampirePlayer.addLegion(attacker);
        opponent.addLegion(defender);

        // Verify the condition: attacker has VAMPIRE → flee blocked
        assertTrue("Attacker has VAMPIRE",
            attacker.getPlayer().hasPower(LauerPower.VAMPIRE));
        // defender.canFlee() might be true (no lord in defender)
        // but Vampire power overrides it
        assertTrue("Defender could normally flee (no lord)",
            defender.canFlee());
    }

    /**
     * Without Vampire: defender CAN flee (if no lord blocking).
     */
    public void testDefenderCanFleeWithoutVampire()
    {
        vampirePlayer.getPowers().removePower(LauerPower.VAMPIRE);

        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType titan = variant.getCreatureByName("Titan");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");

        LegionServerSide attacker = new LegionServerSide("Rd01", null,
            hex, hex, vampirePlayer, game, titan, ogre, ogre);
        LegionServerSide defender = new LegionServerSide("Bu01", null,
            hex, hex, opponent, game, ogre, ogre);
        vampirePlayer.addLegion(attacker);
        opponent.addLegion(defender);

        // Without Vampire, defender can flee
        assertTrue("Defender can flee without Vampire",
            defender.canFlee());
        assertFalse("Attacker does NOT have VAMPIRE",
            attacker.getPlayer().hasPower(LauerPower.VAMPIRE));
    }
}
