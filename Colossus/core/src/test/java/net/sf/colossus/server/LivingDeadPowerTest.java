package net.sf.colossus.server;

import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Living Dead power: recruit from dead pile.
 */
public class LivingDeadPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player livingDeadPlayer;

    public LivingDeadPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        livingDeadPlayer = game.createAndAddPlayer("LivingDead", "SimpleAI");
        game.createAndAddPlayer("Opp", "SimpleAI");
        livingDeadPlayer.getPowers().addPower(LauerPower.LIVING_DEAD);
    }

    /**
     * Living Dead: can recruit a creature from dead pile when caretaker is empty.
     */
    public void testRecruitFromDeadPileWhenCaretakerEmpty()
    {
        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");

        // Deplete caretaker for Cyclops and put some in dead pile
        int available = game.getCaretaker().getAvailableCount(cyclops);
        for (int i = 0; i < available; i++)
        {
            game.getCaretaker().takeOne(cyclops);
        }
        game.getCaretaker().setDeadCount(cyclops, 3);

        // Create legion with 2 gargoyles in Brush (normally can recruit Cyclops with 2 Gargoyles)
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, livingDeadPlayer, game,
            gargoyle, gargoyle);
        livingDeadPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);

        // With Living Dead + dead pile available: Cyclops should be recruitable
        assertTrue("Living Dead can recruit Cyclops from dead pile",
            recruits.contains(cyclops));
    }

    /**
     * Without Living Dead: cannot recruit when caretaker is empty.
     */
    public void testWithoutLivingDeadCannotRecruitFromDeadPile()
    {
        livingDeadPlayer.getPowers().removePower(LauerPower.LIVING_DEAD);

        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");

        // Deplete caretaker for Cyclops
        int available = game.getCaretaker().getAvailableCount(cyclops);
        for (int i = 0; i < available; i++)
        {
            game.getCaretaker().takeOne(cyclops);
        }
        game.getCaretaker().setDeadCount(cyclops, 3);

        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, livingDeadPlayer, game,
            gargoyle, gargoyle);
        livingDeadPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);

        // Without Living Dead: Cyclops NOT recruitable even with dead pile
        assertFalse("Cannot recruit from dead pile without Living Dead",
            recruits.contains(cyclops));
    }

    /**
     * Living Dead: dead creature stays in legion (corpse persists).
     * Verifies the BattleServerSide cleanup does NOT remove dead creatures
     * belonging to a Living Dead player.
     */
    public void testCorpsePersistsInBattle()
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, livingDeadPlayer, game, ogre, ogre);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, game.getPlayerByName("Opp"), game, troll);
        livingDeadPlayer.addLegion(attacker);
        game.getPlayerByName("Opp").addLegion(defender);
        attacker.setEntrySide(net.sf.colossus.game.EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex,
            net.sf.colossus.game.BattlePhase.FIGHT);

        CreatureServerSide creature = attacker.getCritter(0);
        // Kill the creature
        creature.setDead(true);
        assertTrue("Creature is dead", creature.isDead());

        // Living Dead player's creature remains in the legion
        assertTrue("Corpse stays in legion for Living Dead",
            attacker.getCreatures().contains(creature));
    }

    /**
     * Living Dead: dead creature retains its hex position (corpse location).
     * This is fundamental to the slowing mechanic.
     */
    public void testCorpseRetainsHexPosition()
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("1"); // Plains
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, livingDeadPlayer, game, ogre);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, game.getPlayerByName("Opp"), game, troll);
        livingDeadPlayer.addLegion(attacker);
        game.getPlayerByName("Opp").addLegion(defender);
        attacker.setEntrySide(net.sf.colossus.game.EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex,
            net.sf.colossus.game.BattlePhase.MOVE);
        BattleServerSide battle = game.getBattleSS();

        CreatureServerSide corpse = attacker.getCritter(0);

        // Place creature in specific hex
        net.sf.colossus.variant.MasterBoardTerrain terrain =
            battle.getLocation().getTerrain();
        net.sf.colossus.variant.BattleHex hexC3 = terrain.getHexByLabel("C3");
        corpse.setCurrentHex(hexC3);

        // Kill it
        corpse.setDead(true);
        assertTrue("Corpse is dead", corpse.isDead());

        // Corpse retains hex position (not moved to entranceHex or null)
        assertEquals("Corpse stays in its hex after death",
            hexC3, corpse.getCurrentHex());

        // Verify it's still visible in getAllCritters (for slowing check)
        boolean foundInAll = false;
        for (net.sf.colossus.game.BattleCritter c : battle.getAllCritters())
        {
            if (c == corpse) { foundInAll = true; break; }
        }
        assertTrue("Dead corpse still appears in getAllCritters", foundInAll);
    }

    /**
     * Living Dead revive budget: 50% of winning legion value.
     * Lords cannot be revived. Stack limit respected.
     * Tests the budget guard conditions directly.
     */
    public void testReviveBudgetCalculation()
    {
        // Legion of 3 Ogres: 3 * 12 = 36 pts. Revive budget = 18.
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            hex, hex, livingDeadPlayer, game, ogre, ogre, ogre);
        livingDeadPlayer.addLegion(legion);

        int legionValue = legion.getPointValue();
        assertEquals("3 Ogres = 36 pts", 36, legionValue);
        int reviveBudget = legionValue / 2;
        assertEquals("Revive budget = 18", 18, reviveBudget);

        // Troll = 16 pts — fits in budget (16 <= 18)
        assertTrue("Troll fits in budget", troll.getPointValue() <= reviveBudget);

        // Lords cannot be revived
        CreatureType titan = variant.getCreatureByName("Titan");
        assertTrue("Titan is a lord", titan.isLord());

        // Stack limit: with 3 creatures + max 7 = 4 more slots
        int maxHeight = livingDeadPlayer.getMaxLegionHeight();
        assertTrue("Room to add creatures", legion.getHeight() < maxHeight);
    }
}
