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
 * Tests for Healer power: HEAL phase, healing strikes, overheal temp hits.
 */
public class HealerPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player healerPlayer;
    private Player opponent;
    private BattleServerSide battle;

    public HealerPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        healerPlayer = game.createAndAddPlayer("Healer", "SimpleAI");
        opponent = game.createAndAddPlayer("Opp", "SimpleAI");
        healerPlayer.getPowers().addPower(LauerPower.HEALER);

        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, healerPlayer, game, ogre, ogre);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, opponent, game, troll);
        healerPlayer.addLegion(attacker);
        opponent.addLegion(defender);
        attacker.setEntrySide(EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.HEAL);
        battle = game.getBattleSS();
    }

    /**
     * doHeal heals 1 hit from a damaged friendly creature.
     */
    public void testHealRemovesOneHit()
    {
        CreatureServerSide healer = battle.getAttackingLegion().getCritter(0);
        CreatureServerSide target = battle.getAttackingLegion().getCritter(1);

        // Damage the target (add 2 hits)
        target.setHits(2);
        assertEquals("Target has 2 hits before heal", 2, target.getHits());

        boolean result = battle.doHeal(healer, target);
        assertTrue("Heal succeeded", result);
        assertEquals("Target has 1 hit after heal", 1, target.getHits());
    }

    /**
     * doHeal gives temp hit when target is fully healed (overheal).
     */
    public void testOverhealGivesTempHit()
    {
        CreatureServerSide healer = battle.getAttackingLegion().getCritter(0);
        CreatureServerSide target = battle.getAttackingLegion().getCritter(1);

        // Target at full health (0 hits)
        assertEquals("Target fully healed", 0, target.getHits());

        boolean result = battle.doHeal(healer, target);
        assertTrue("Overheal succeeded", result);
        assertEquals("Target has 1 temp hit", 1, target.getTempHits());
    }

    /**
     * A creature cannot heal itself.
     */
    public void testCannotHealSelf()
    {
        CreatureServerSide healer = battle.getAttackingLegion().getCritter(0);
        healer.setHits(2);

        boolean result = battle.doHeal(healer, healer);
        assertFalse("Self-heal rejected", result);
        assertEquals("Hits unchanged", 2, healer.getHits());
    }

    /**
     * Only lords can heal lords.
     */
    public void testNonLordCannotHealLord()
    {
        // Create a battle with Titan + Ogre
        CreatureType titan = variant.getCreatureByName("Titan");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide attacker = new LegionServerSide("Rd04", null,
            hex, hex, healerPlayer, game, titan, ogre);
        LegionServerSide defender = new LegionServerSide("Bu04", null,
            hex, hex, opponent, game, troll);
        healerPlayer.addLegion(attacker);
        opponent.addLegion(defender);
        attacker.setEntrySide(EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.HEAL);
        BattleServerSide battle2 = game.getBattleSS();

        CreatureServerSide titanCritter = battle2.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide ogreCritter = battle2.getAttackingLegion()
            .getCritter(1);

        titanCritter.setHits(2);
        // Ogre (non-lord) tries to heal Titan (lord) → rejected
        boolean result = battle2.doHeal(ogreCritter, titanCritter);
        assertFalse("Non-lord cannot heal lord", result);
    }

    /**
     * doHeal only works during HEAL phase.
     */
    public void testHealRejectedOutsideHealPhase()
    {
        // Change to FIGHT phase
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide attacker = new LegionServerSide("Rd05", null,
            hex, hex, healerPlayer, game, ogre, ogre);
        LegionServerSide defender = new LegionServerSide("Bu05", null,
            hex, hex, opponent, game, troll);
        healerPlayer.addLegion(attacker);
        opponent.addLegion(defender);
        attacker.setEntrySide(EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        BattleServerSide fightBattle = game.getBattleSS();

        CreatureServerSide healer = fightBattle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = fightBattle.getAttackingLegion()
            .getCritter(1);
        target.setHits(2);

        boolean result = fightBattle.doHeal(healer, target);
        assertFalse("Heal rejected in FIGHT phase", result);
    }
}
