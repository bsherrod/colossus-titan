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
 * Scripted tests for Avian power: all creatures fly at speed 4.
 */
public class AvianPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player avianPlayer;
    private Player opponent;

    public AvianPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        avianPlayer = game.createAndAddPlayer("Avian", "SimpleAI");
        opponent = game.createAndAddPlayer("Opp", "SimpleAI");
        avianPlayer.getPowers().addPower(LauerPower.AVIAN);
    }

    private CreatureServerSide createCreatureInBattle(CreatureType type)
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, avianPlayer, game, type);
        CreatureType troll = variant.getCreatureByName("Troll");
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, opponent, game, troll);
        avianPlayer.addLegion(attacker);
        opponent.addLegion(defender);
        attacker.setEntrySide(EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        return game.getBattleSS().getAttackingLegion().getCritter(0);
    }

    /**
     * Non-flier (Ogre) with Avian becomes a flier.
     */
    public void testNonFlierBecomesFlier()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        assertFalse("Ogre is not naturally a flier", ogre.isFlier());

        CreatureServerSide creature = createCreatureInBattle(ogre);
        assertTrue("Ogre with Avian IS a flier", creature.isFlier());
    }

    /**
     * Without Avian, non-flier stays non-flier.
     */
    public void testWithoutAvianNoFlight()
    {
        avianPlayer.getPowers().removePower(LauerPower.AVIAN);

        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureServerSide creature = createCreatureInBattle(ogre);
        assertFalse("Ogre without Avian is NOT a flier", creature.isFlier());
    }

    /**
     * Natural flier (Gorgon) stays a flier with Avian.
     */
    public void testNaturalFlierStaysFlier()
    {
        CreatureType gorgon = variant.getCreatureByName("Gorgon");
        assertTrue("Gorgon is naturally a flier", gorgon.isFlier());

        CreatureServerSide creature = createCreatureInBattle(gorgon);
        assertTrue("Gorgon with Avian still a flier", creature.isFlier());
    }
}
