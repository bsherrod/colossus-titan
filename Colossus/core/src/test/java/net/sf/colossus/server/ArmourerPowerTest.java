package net.sf.colossus.server;

import junit.framework.TestCase;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Scripted tests for Armourer power: extra hits to kill.
 * Rule: non-* (non-flier) gets floor(2*str/3) extra hits.
 *       * (flier) gets floor(str/3) extra hits.
 */
public class ArmourerPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player armourerPlayer;
    private Player opponent;

    public ArmourerPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        armourerPlayer = game.createAndAddPlayer("Armourer", "SimpleAI");
        opponent = game.createAndAddPlayer("Opp", "SimpleAI");
        armourerPlayer.getPowers().addPower(LauerPower.ARMOURER);
    }

    private CreatureServerSide createCreatureInBattle(CreatureType type)
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, armourerPlayer, game, type);
        CreatureType troll = variant.getCreatureByName("Troll");
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, opponent, game, troll);
        armourerPlayer.addLegion(attacker);
        opponent.addLegion(defender);
        attacker.setEntrySide(EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        return game.getBattleSS().getAttackingLegion().getCritter(0);
    }

    /**
     * Non-flier Ogre (str=6): defensivePower = 6 + floor(2*6/3) = 10
     */
    public void testNonFlierOgreExtraHits()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        assertFalse("Ogre is not a flier", ogre.isFlier());
        CreatureServerSide creature = createCreatureInBattle(ogre);
        assertEquals("Ogre+Armourer defensivePower", 10,
            creature.getDefensivePower());
    }

    /**
     * Non-flier Troll (str=8): defensivePower = 8 + floor(2*8/3) = 13
     */
    public void testNonFlierTrollExtraHits()
    {
        CreatureType troll = variant.getCreatureByName("Troll");
        assertFalse("Troll is not a flier", troll.isFlier());
        CreatureServerSide creature = createCreatureInBattle(troll);
        assertEquals("Troll+Armourer defensivePower", 13,
            creature.getDefensivePower());
    }

    /**
     * Flier Gorgon (str=6): defensivePower = 6 + floor(6/3) = 8
     */
    public void testFlierGorgonExtraHits()
    {
        CreatureType gorgon = variant.getCreatureByName("Gorgon");
        assertTrue("Gorgon is a flier", gorgon.isFlier());
        CreatureServerSide creature = createCreatureInBattle(gorgon);
        assertEquals("Gorgon+Armourer defensivePower", 8,
            creature.getDefensivePower());
    }

    /**
     * Without Armourer: defensivePower = base power only.
     */
    public void testWithoutArmourerNoBonus()
    {
        armourerPlayer.getPowers().removePower(LauerPower.ARMOURER);
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureServerSide creature = createCreatureInBattle(ogre);
        assertEquals("Ogre without Armourer = base 6", 6,
            creature.getDefensivePower());
    }
}
