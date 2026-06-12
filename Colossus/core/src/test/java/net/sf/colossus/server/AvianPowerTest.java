package net.sf.colossus.server;

import junit.framework.TestCase;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Scripted tests for Avian power: all creatures fly at speed 4,
 * and * (rangestriker) creatures may hover (terrain immunity).
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

    // === HOVER TESTS ===

    /**
     * Rangestriker (*) creature with Avian can hover.
     * Hovering makes it native in ALL terrain (terrain immunity).
     */
    public void testRangestrikerCanHover()
    {
        CreatureType gorgon = variant.getCreatureByName("Gorgon");
        assertTrue("Gorgon is a rangestriker (*)", gorgon.isRangestriker());

        CreatureServerSide creature = createCreatureInBattle(gorgon);
        creature.setHover(true);
        assertTrue("Rangestriker with Avian can hover", creature.isHover());

        // Hovering creature is native in ALL terrain
        assertTrue("Hovering: native in Brambles",
            creature.isNativeIn(HazardTerrain.BRAMBLES));
        assertTrue("Hovering: native in Stone",
            creature.isNativeIn(HazardTerrain.STONE));
        assertTrue("Hovering: native in Volcano",
            creature.isNativeIn(HazardTerrain.VOLCANO));
    }

    /**
     * Non-hovering creature is NOT native in non-native terrain.
     * Ogre is NOT native in Brambles normally.
     */
    public void testNonHoveringNotNativeInBrambles()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureServerSide creature = createCreatureInBattle(ogre);
        assertFalse("Hover defaults to false", creature.isHover());
        // Ogre is not naturally native in Brambles
        assertFalse("Non-hovering Ogre not native in Brambles",
            creature.isNativeIn(HazardTerrain.BRAMBLES));
    }

    /**
     * Enemy striking at a hovering creature does NOT get terrain bonus.
     * In Brambles, a non-native attacker normally gets -1 skill penalty.
     * When target hovers, the attacker is still non-native so the penalty
     * depends on whether target "native" status affects the calculation.
     *
     * Rule: "Enemy characters never gain dice or skill for terrain benefits
     * when striking at a hovering character."
     */
    public void testStrikeAtHoveringCreatureNoTerrainBonus()
    {
        // Use Jungle (hex 40) for Brambles terrain in battle hexes
        MasterHex hex = variant.getMasterBoard().getHexByLabel("40");
        CreatureType gorgon = variant.getCreatureByName("Gorgon");
        CreatureType troll = variant.getCreatureByName("Troll");

        // Avian player attacks with Gorgon (hovering)
        // Opponent defends with Troll (non-native non-flier)
        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, avianPlayer, game, gorgon);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, opponent, game, troll);
        avianPlayer.addLegion(attacker);
        opponent.addLegion(defender);
        attacker.setEntrySide(EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        BattleServerSide battle = game.getBattleSS();

        CreatureServerSide hoverGorgon = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide enemyTroll = battle.getDefendingLegion()
            .getCritter(0);

        // Place in adjacent hexes (melee)
        MasterBoardTerrain terrain = battle.getLocation().getTerrain();
        BattleHex hexC3 = terrain.getHexByLabel("C3");
        BattleHex hexC4 = terrain.getHexByLabel("C4");
        hoverGorgon.setCurrentHex(hexC3);
        enemyTroll.setCurrentHex(hexC4);

        // Set Gorgon to hover
        hoverGorgon.setHover(true);

        BattleStrikeServerSide strike = new BattleStrikeServerSide(game);

        // Get strike number for Troll striking at hovering Gorgon
        int strikeAtHover = strike.getStrikeNumber(enemyTroll, hoverGorgon,
            false);

        // Now compare: unhover the Gorgon on plain terrain
        hoverGorgon.setHover(false);

        // Move both to plain hex to get baseline (no terrain effects)
        BattleHex hexA1 = terrain.getHexByLabel("A1");
        BattleHex hexA2 = terrain.getHexByLabel("A2");
        hoverGorgon.setCurrentHex(hexA1);
        enemyTroll.setCurrentHex(hexA2);
        int strikeOnPlain = strike.getStrikeNumber(enemyTroll, hoverGorgon,
            false);

        // Hovering should mean the attacker gets no terrain benefits —
        // strike number at hovering creature should be same as on plains
        // (no bonus from Brambles native status given to target)
        // Base: 4 - attackerSkill + defenderSkill = 4 - 2 + 3 = 5
        assertEquals("Strike at hovering creature = no terrain effect",
            strikeOnPlain, strikeAtHover);
    }

    /**
     * Non-rangestriker (Ogre) should NOT hover even with Avian.
     * Only * creatures may hover per the rules.
     * Note: current implementation allows setHover(true) on any creature
     * (the restriction isn't enforced). This test documents expected behavior.
     */
    public void testNonRangestrikerCannotHover()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        assertFalse("Ogre is NOT a rangestriker", ogre.isRangestriker());

        CreatureServerSide creature = createCreatureInBattle(ogre);
        // Per rules, only * creatures may hover.
        // The isHover field has no enforcement — test that Ogre doesn't
        // start hovering by default.
        assertFalse("Ogre does not hover by default", creature.isHover());
    }

    /**
     * OGG phase exists in BattlePhase enum.
     * Note: OGG attack logic is NOT YET IMPLEMENTED — only the phase
     * declaration and doneWithOgg() advance method exist.
     * This test verifies the infrastructure is in place.
     */
    public void testOggPhaseExists()
    {
        assertNotNull("OGG phase exists", BattlePhase.OGG);
        assertEquals("OGG phase name", "Ogg Attack",
            BattlePhase.OGG.toString());
    }
}
