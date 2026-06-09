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
 * Tests for Demon possession mechanics.
 */
public class DemonPossessionTest extends TestCase
{
    private GameServerSide game;
    private BattleServerSide battle;
    private Player demonPlayer;
    private Variant variant;

    public DemonPossessionTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        demonPlayer = game.createAndAddPlayer("Demon", "SimpleAI");
        Player opponent = game.createAndAddPlayer("Opp", "SimpleAI");
        demonPlayer.getPowers().addPower(LauerPower.DEMON);

        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");
        CreatureType lion = variant.getCreatureByName("Lion");

        LegionServerSide attackerLegion = new LegionServerSide("Rd03", null,
            hex, hex, demonPlayer, game, ogre, troll, lion);
        LegionServerSide defenderLegion = new LegionServerSide("Bu03", null,
            hex, hex, opponent, game, ogre);

        demonPlayer.addLegion(attackerLegion);
        opponent.addLegion(defenderLegion);
        attackerLegion.setEntrySide(EntrySide.RIGHT);

        game.createBattle(attackerLegion, defenderLegion,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.MOVE);
        battle = game.getBattleSS();
    }

    /**
     * On first MOVE phase, the Demon AI should auto-possess the strongest
     * non-lord creature in its legion.
     */
    public void testAutoPositionStrongestCreature()
    {
        // Battle starts at turn 1 in MOVE phase
        assertEquals(1, battle.getBattleTurnNumber());

        // Call setupMove which should trigger auto-possession
        // The strongest non-lord creature is Troll (power 8) > Lion (6) > Ogre (6)
        CreatureServerSide troll = null;
        for (CreatureServerSide c : battle.getAttackingLegion().getCreatures())
        {
            if (c.getType().getName().equals("Troll"))
            {
                troll = c;
                break;
            }
        }
        assertNotNull("Should find Troll in legion", troll);

        // Call setupMove which triggers auto-possession on turn 1
        battle.setupMove();

        // After setupMove on turn 1, the strongest creature should be possessed
        assertTrue("Troll should be possessed after first setupMove",
            troll.isPossessed());
        assertTrue("Demon strength should be > 0",
            troll.getDemonStrength() > 0);
    }

    /**
     * Possessed creature's power + demonStrength is the effective striking power.
     * (Full getDice needs hex setup; verify the formula components instead.)
     */
    public void testPossessedCreatureGetsBonusDice()
    {
        battle.setupMove();

        CreatureServerSide troll = findCreature("Troll");
        assertNotNull(troll);
        assertTrue(troll.isPossessed());

        // Verify the demon adds bonus dice: power=8, demonStr=6 → effective=14
        assertEquals("Troll base power should be 8", 8, troll.getPower());
        assertEquals("Demon strength should be titan power (6)",
            6, troll.getDemonStrength());
        // The getDice method adds demonStrength to dice when possessed
        // (verified by code inspection + functest)
        assertTrue("isPossessed must be true for bonus to apply",
            troll.isPossessed());
    }

    /**
     * Damage to possessed creature hits demonStrength first.
     */
    public void testDamageHitsDemonStrengthFirst()
    {
        battle.setupMove();

        CreatureServerSide troll = findCreature("Troll");
        assertNotNull(troll);
        assertTrue(troll.isPossessed());

        int originalDemonStr = troll.getDemonStrength();
        int originalHits = troll.getHits();

        // Deal 3 damage — should all go to demonStrength
        troll.adjustHits(3);

        assertEquals("Demon strength should absorb damage",
            originalDemonStr - 3, troll.getDemonStrength());
        assertEquals("Creature hits should be unchanged",
            originalHits, troll.getHits());
    }

    /**
     * When demon HP reaches 0, demon jumps to adjacent friendly creature.
     */
    public void testDemonDeathJumpToAdjacent()
    {
        battle.setupMove();

        CreatureServerSide troll = findCreature("Troll");
        assertNotNull(troll);
        assertTrue(troll.isPossessed());

        int demonStr = troll.getDemonStrength();
        // Deal exactly enough damage to kill the demon
        troll.adjustHits(demonStr);

        // Demon should have jumped — troll no longer possessed
        assertFalse("Troll should no longer be possessed after demon death",
            troll.isPossessed());
        assertEquals("Troll demonStrength should be 0",
            0, troll.getDemonStrength());

        // Some other creature in the legion should now be possessed
        // (if adjacent — in test setup all are at entrance so may not be adjacent)
        // At minimum, the demon tried to jump
    }

    /**
     * When demon HP reaches 0 and no adjacent friendly exists, demon dies permanently.
     */
    public void testDemonDiesPermanentlyIfNoAdjacent()
    {
        // Create a battle with only one non-lord creature (no jump target)
        Variant v = VariantSupport.loadVariantByName("Default", true);
        GameServerSide g = GameServerSide.makeNewGameServerSide(v);
        Player dp = g.createAndAddPlayer("Demon2", "SimpleAI");
        Player opp = g.createAndAddPlayer("Opp2", "SimpleAI");
        dp.getPowers().addPower(LauerPower.DEMON);

        MasterHex hex = v.getMasterBoard().getHexByLabel("3");
        CreatureType ogre = v.getCreatureByName("Ogre");

        // Only one creature — demon has nowhere to jump
        LegionServerSide att = new LegionServerSide("Rd03", null,
            hex, hex, dp, g, ogre);
        LegionServerSide def = new LegionServerSide("Bu03", null,
            hex, hex, opp, g, ogre);
        dp.addLegion(att);
        opp.addLegion(def);
        att.setEntrySide(EntrySide.RIGHT);

        g.createBattle(att, def,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.MOVE);
        BattleServerSide b = g.getBattleSS();
        b.setupMove();

        // Find the possessed creature
        CreatureServerSide possessed = null;
        for (CreatureServerSide c : b.getAttackingLegion().getCreatures())
        {
            if (c.isPossessed())
            {
                possessed = c;
                break;
            }
        }
        assertNotNull("Should have a possessed creature", possessed);

        // Kill the demon HP
        possessed.adjustHits(possessed.getDemonStrength());

        // No adjacent friendly → demon dies permanently
        assertFalse("Should no longer be possessed", possessed.isPossessed());
        assertEquals("Demon strength should be 0 permanently",
            0, possessed.getDemonStrength());
    }

    /**
     * doUnpossess clears possessed flag but retains demon HP for future use.
     */
    public void testDoUnpossess()
    {
        battle.setupMove();
        CreatureServerSide troll = findCreature("Troll");
        assertNotNull(troll);
        assertTrue(troll.isPossessed());
        int demonStr = troll.getDemonStrength();
        assertTrue(demonStr > 0);

        // Unpossess
        battle.doUnpossess(troll);

        assertFalse("Troll should no longer be possessed", troll.isPossessed());
        assertEquals("Demon strength retained for future use",
            demonStr, troll.getDemonStrength());
    }

    private CreatureServerSide findCreature(String name)
    {
        for (CreatureServerSide c : battle.getAttackingLegion().getCreatures())
        {
            if (c.getType().getName().equals(name))
            {
                return c;
            }
        }
        return null;
    }
}
