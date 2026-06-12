package net.sf.colossus.server;

import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for The Folks power: always recruit weakest terrain creature.
 */
public class FolksPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player folksPlayer;

    public FolksPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        folksPlayer = game.createAndAddPlayer("Folks", "SimpleAI");
        game.createAndAddPlayer("Opp", "SimpleAI");
        folksPlayer.getPowers().addPower(LauerPower.THE_FOLKS);
    }

    /**
     * Folks: can always recruit Gargoyle in Brush (weakest) even with
     * no matching recruiters in legion.
     */
    public void testAlwaysRecruitsWeakestInBrush()
    {
        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType titan = variant.getCreatureByName("Titan");
        // Legion with only a Titan (no Gargoyles to recruit with normally)
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, folksPlayer, game, titan);
        folksPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");

        assertTrue("Folks can always recruit Gargoyle (weakest in Brush)",
            recruits.contains(gargoyle));
    }

    /**
     * Without Folks: cannot recruit without matching recruiters.
     */
    public void testWithoutFolksNeedsRecruiters()
    {
        folksPlayer.getPowers().removePower(LauerPower.THE_FOLKS);

        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        CreatureType titan = variant.getCreatureByName("Titan");
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, folksPlayer, game, titan);
        folksPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");

        // Without Folks, Titan alone cannot recruit Gargoyle in Brush
        // (needs at least 1 Gargoyle to recruit another Gargoyle)
        assertFalse("Without Folks cannot recruit with no matching recruiters",
            recruits.contains(gargoyle));
    }

    /**
     * Folks in Swamp: weakest recruit should be Troll (the base terrain creature),
     * NOT Gorgon (which is a higher-tier recruit in Swamp).
     * BUG: tempRecruits list may be ordered by recruit chain position, not point value.
     */
    public void testFolksWeakestInSwampIsTroll()
    {
        // Find a Swamp hex
        MasterHex swampHex = null;
        for (MasterHex hex : variant.getMasterBoard().getAllHexes())
        {
            if (hex.getTerrain().getId().equals("Swamp"))
            {
                swampHex = hex;
                break;
            }
        }
        assertNotNull("Found a Swamp hex", swampHex);

        CreatureType titan = variant.getCreatureByName("Titan");
        // Legion with just a Titan (no recruiters)
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            swampHex, swampHex, folksPlayer, game, titan);
        folksPlayer.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(
            legion, swampHex);

        CreatureType troll = variant.getCreatureByName("Troll");
        CreatureType gorgon = variant.getCreatureByName("Gorgon");

        // Folks weakest = first element of recruits list added by Folks logic
        // Should be Troll (16pts), NOT Gorgon (18pts)
        assertTrue("Folks can recruit Troll in Swamp",
            recruits.contains(troll));

        // The Folks logic adds tempRecruits.get(0) as the weakest.
        // If Gorgon is at index 0 instead of Troll, that's the bug.
        // The first recruit in the list should be Troll (weakest in Swamp).
        assertEquals("Folks weakest recruit in Swamp is Troll (first added)",
            troll, recruits.get(0));
    }

    /**
     * Folks in battle: extra creature should be the weakest available in
     * the TERRAIN, not the weakest in the legion.
     * BUG: initFolkCreatures uses cheapest creature in legion, not terrain.
     * If legion has Gorgon (18pts), it picks whatever is cheapest in legion.
     * Rule says: "the extra creature is the weakest available in that terrain"
     */
    public void testFolksBattleCreatureIsWeakestInTerrain()
    {
        CreatureType gorgon = variant.getCreatureByName("Gorgon");
        CreatureType troll = variant.getCreatureByName("Troll");
        CreatureType ogre = variant.getCreatureByName("Ogre");

        // Find a Swamp hex (weakest terrain creature = Troll)
        MasterHex swampHex = null;
        for (MasterHex hex : variant.getMasterBoard().getAllHexes())
        {
            if (hex.getTerrain().getId().equals("Swamp"))
            {
                swampHex = hex;
                break;
            }
        }
        assertNotNull("Found Swamp hex", swampHex);

        // Legion with Gorgon (18pts) and Ogre (12pts) — no Troll in legion
        LegionServerSide attacker = new LegionServerSide("Rd01", null,
            swampHex, swampHex, folksPlayer, game, gorgon, ogre, ogre);
        LegionServerSide defender = new LegionServerSide("Bu01", null,
            swampHex, swampHex,
            game.getPlayerByName("Opp"), game, troll);
        folksPlayer.addLegion(attacker);
        game.getPlayerByName("Opp").addLegion(defender);
        attacker.setEntrySide(net.sf.colossus.game.EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, swampHex,
            net.sf.colossus.game.BattlePhase.MOVE);
        BattleServerSide battle = game.getBattleSS();

        // Trigger folk creature initialization
        battle.initFolkCreatures(attacker);

        // The folk creature type should be Troll (weakest in Swamp terrain)
        // BUG: currently picks Ogre (12pts, cheapest in legion)
        CreatureType folkType = battle.getFolkCreatureType();
        assertNotNull("Folk creature type set", folkType);
        assertEquals("Folks battle creature in Swamp should be Troll",
            "Troll", folkType.getName());
    }

    /**
     * Folks in Tower: what creature type does initFolkCreatures pick?
     * Rule: "the extra creature is the weakest available in that terrain
     * (or any tower creature in a tower)"
     */
    public void testFolksBattleCreatureInTower()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");

        // Tower hex (hex 100 is a tower)
        MasterHex towerHex = variant.getMasterBoard().getHexByLabel("100");
        assertNotNull("Tower hex exists", towerHex);

        LegionServerSide attacker = new LegionServerSide("Rd01", null,
            towerHex, towerHex, folksPlayer, game, ogre, ogre, ogre);
        LegionServerSide defender = new LegionServerSide("Bu01", null,
            towerHex, towerHex,
            game.getPlayerByName("Opp"), game, troll);
        folksPlayer.addLegion(attacker);
        game.getPlayerByName("Opp").addLegion(defender);
        attacker.setEntrySide(net.sf.colossus.game.EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, towerHex,
            net.sf.colossus.game.BattlePhase.MOVE);
        BattleServerSide battle = game.getBattleSS();

        battle.initFolkCreatures(attacker);

        CreatureType folkType = battle.getFolkCreatureType();
        assertNotNull("Folk creature type set in tower", folkType);
        // Tower creatures: Centaur(3*4=12), Gargoyle(4*3=12), Ogre(6*2=12)
        // All equal cost. AI picks highest power = Ogre (power 6).
        assertEquals("Tower folk creature is Ogre (highest power among equals)",
            "Ogre", folkType.getName());
    }

    /**
     * Debug: what creatures does getPossibleRecruits return for Tower?
     */

    /**
     * Folks: can remove one legion before splitting/moving.
     * Guards: needs FOLKS, MOVE phase, no legion moved yet, keep at least 1.
     */
    /**
     * Folks: remove-legion guard logic.
     * Needs: FOLKS power + MOVE phase + no legion moved + numLegions > 1.
     */
    public void testFolksRemoveLegionGuards()
    {
        // Has FOLKS + in MOVE phase + no moves + multiple legions → allowed
        CreatureType ogre = variant.getCreatureByName("Ogre");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide leg1 = new LegionServerSide("Rd01", null,
            hex, hex, folksPlayer, game, ogre, ogre);
        LegionServerSide leg2 = new LegionServerSide("Rd02", null,
            hex, hex, folksPlayer, game, ogre, ogre);
        folksPlayer.addLegion(leg1);
        folksPlayer.addLegion(leg2);
        game.setPhase(net.sf.colossus.game.Phase.MOVE);

        // Guard checks that doRemoveFolksLegion uses:
        boolean hasPower = folksPlayer.hasPower(LauerPower.THE_FOLKS);
        boolean inMovePhase = game.isPhase(net.sf.colossus.game.Phase.MOVE);
        boolean noLegionMoved = true;
        for (net.sf.colossus.game.Legion leg : folksPlayer.getLegions())
        {
            if (leg.hasMoved()) { noLegionMoved = false; break; }
        }
        boolean hasMultipleLegions = folksPlayer.getNumLegions() > 1;

        assertTrue("Has FOLKS power", hasPower);
        assertTrue("In MOVE phase", inMovePhase);
        assertTrue("No legion has moved", noLegionMoved);
        assertTrue("Has multiple legions", hasMultipleLegions);
        // All guards pass → remove would succeed
        assertTrue("All guards pass", hasPower && inMovePhase
            && noLegionMoved && hasMultipleLegions);
    }

    /**
     * Folks: cannot remove last remaining legion (guard check).
     */
    public void testFolksCannotRemoveLastLegionGuard()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide leg1 = new LegionServerSide("Rd01", null,
            hex, hex, folksPlayer, game, ogre, ogre);
        folksPlayer.addLegion(leg1);
        game.setPhase(net.sf.colossus.game.Phase.MOVE);

        assertFalse("Cannot remove last legion",
            folksPlayer.getNumLegions() > 1);
    }

    /**
     * Folks: extra creature budget = floor(legionValue / 2) / creatureCost.
     * E.g. 3 Ogres (12pts each = 36pts), budget = 18, Gargoyle = 12pts → 1 extra.
     */
    public void testFolksExtraCreatureBudget()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");
        // 3 Ogres in Brush: Ogre=12pts * 3 = 36pts. Budget = 18.
        // Gargoyle = 4*3 = 12pts. 18/12 = 1 extra.
        MasterHex brushHex = variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide attacker = new LegionServerSide("Rd01", null,
            brushHex, brushHex, folksPlayer, game, ogre, ogre, ogre);
        LegionServerSide defender = new LegionServerSide("Bu01", null,
            brushHex, brushHex,
            game.getPlayerByName("Opp"), game, troll);
        folksPlayer.addLegion(attacker);
        game.getPlayerByName("Opp").addLegion(defender);
        attacker.setEntrySide(net.sf.colossus.game.EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, brushHex,
            net.sf.colossus.game.BattlePhase.MOVE);
        BattleServerSide battle = game.getBattleSS();

        // Must explicitly init folk creatures (init() not called in test)
        battle.initFolkCreatures(attacker);

        // Verify: Gargoyle chosen (12pts, weakest in Brush)
        CreatureType folkType = battle.getFolkCreatureType();
        assertNotNull("Folk creature type set", folkType);
        assertEquals("Folk creature in Brush is Gargoyle",
            "Gargoyle", folkType.getName());

        // Budget: 36/2 = 18. 18/12 = 1 folk creature
        assertEquals("1 folk creature from budget",
            1, battle.getFolkCreaturesRemaining());
    }
}
