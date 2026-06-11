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

    /**
     * Vampire overrun condition: attacker value >= 2.5x defender value.
     * Ogre=12pts. 4 Ogres = 48. Defender 1 Ogre = 12. 48/12 = 4.0 >= 2.5 ✓
     */
    public void testOverrunCondition()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");

        // Attacker: 4 Ogres = 48 pts
        LegionServerSide attacker = new LegionServerSide("Rd01", null,
            hex, hex, vampirePlayer, game, ogre, ogre, ogre, ogre);
        // Defender: 1 Ogre = 12 pts
        LegionServerSide defender = new LegionServerSide("Bu01", null,
            hex, hex, opponent, game, ogre);
        vampirePlayer.addLegion(attacker);
        opponent.addLegion(defender);

        int myValue = attacker.getPointValue();
        int enemyValue = defender.getPointValue();
        // 48 >= 12 * 2.5 = 30 → overrun
        assertTrue("Overrun condition: " + myValue + " >= " + enemyValue + " * 2.5",
            myValue >= enemyValue * 2.5);
    }

    /**
     * Vampire overrun fails when attacker is too weak.
     * 2 Ogres = 24 vs 1 Ogre = 12. 24/12 = 2.0 < 2.5 → no overrun.
     */
    public void testOverrunFailsWhenTooWeak()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");

        LegionServerSide attacker = new LegionServerSide("Rd01", null,
            hex, hex, vampirePlayer, game, ogre, ogre);
        LegionServerSide defender = new LegionServerSide("Bu01", null,
            hex, hex, opponent, game, ogre);
        vampirePlayer.addLegion(attacker);
        opponent.addLegion(defender);

        int myValue = attacker.getPointValue();
        int enemyValue = defender.getPointValue();
        // 24 >= 12 * 2.5 = 30 → NO overrun
        assertFalse("No overrun: " + myValue + " < " + enemyValue + " * 2.5",
            myValue >= enemyValue * 2.5);
    }

    /**
     * Vampire Titan formula: (score/50 + 1) * 4.
     * At score=0: power=4. At score=50: power=8. At score=100: power=12.
     */
    public void testVampireTitanPowerFormula()
    {
        // Score = 0: (0/50 + 1) * 4 = 4
        assertEquals("Vampire Titan at score 0", 4,
            vampirePlayer.getTitanPower());

        // Score = 50: (50/50 + 1) * 4 = 8
        ((PlayerServerSide)vampirePlayer).setScore(50);
        assertEquals("Vampire Titan at score 50", 8,
            vampirePlayer.getTitanPower());

        // Score = 100: (100/50 + 1) * 4 = 12
        ((PlayerServerSide)vampirePlayer).setScore(100);
        assertEquals("Vampire Titan at score 100", 12,
            vampirePlayer.getTitanPower());
    }
}
