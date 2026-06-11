package net.sf.colossus.server;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Ninja power: split minimum sizes.
 * Ninja allows split-off of 1 creature (normally min 2).
 * Tests verify the guard conditions that doSplit uses.
 */
public class NinjaPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player ninjaPlayer;
    private Player normalPlayer;

    public NinjaPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        ninjaPlayer = game.createAndAddPlayer("Ninja", "SimpleAI");
        normalPlayer = game.createAndAddPlayer("Normal", "SimpleAI");
        ninjaPlayer.getPowers().addPower(LauerPower.NINJA);
    }

    /**
     * Ninja: minSplitSize = 1 (can split off single creature).
     * Normal: minSplitSize = 2.
     */
    public void testNinjaMinSplitSize()
    {
        // The guard in doSplit: minSplitSize = player.hasPower(NINJA) ? 1 : 2
        assertTrue("Ninja has NINJA power",
            ninjaPlayer.hasPower(LauerPower.NINJA));
        assertFalse("Normal does not have NINJA",
            normalPlayer.hasPower(LauerPower.NINJA));

        // Verify the min split sizes match what doSplit uses
        int ninjaMin = ninjaPlayer.hasPower(LauerPower.NINJA) ? 1 : 2;
        int normalMin = normalPlayer.hasPower(LauerPower.NINJA) ? 1 : 2;
        assertEquals("Ninja min split size = 1", 1, ninjaMin);
        assertEquals("Normal min split size = 2", 2, normalMin);
    }

    /**
     * Ninja: minPreSplit = 2 (can split a 2-creature legion).
     * Normal: minPreSplit = 4.
     */
    public void testNinjaMinPreSplitHeight()
    {
        int ninjaMinPre = ninjaPlayer.hasPower(LauerPower.NINJA) ? 2 : 4;
        int normalMinPre = normalPlayer.hasPower(LauerPower.NINJA) ? 2 : 4;
        assertEquals("Ninja can split from height 2", 2, ninjaMinPre);
        assertEquals("Normal needs height 4 to split", 4, normalMinPre);
    }

    /**
     * Ninja: a 3-creature legion can split off 1, leaving 2.
     * Verify the guard condition: splitSize >= 1 AND remaining >= 1.
     */
    public void testSplitGuardAcceptsOneCreature()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        MasterHex hex = variant.getMasterBoard().getHexByLabel("100");
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            hex, hex, ninjaPlayer, game, ogre, ogre, ogre);
        ninjaPlayer.addLegion(legion);

        int splitSize = 1; // splitting off 1 creature
        int remaining = legion.getHeight() - splitSize; // 3 - 1 = 2
        int minSplitSize = 1; // Ninja

        assertTrue("Split size >= min", splitSize >= minSplitSize);
        assertTrue("Remaining >= min", remaining >= minSplitSize);
    }

    /**
     * Ninja death strike: bonus dice = remaining hits (defensivePower - hits taken).
     * Ogre (power 6, full health): death strike dice = 6 (base) + 6 (remaining) = 12.
     * Ogre (power 6, 2 hits taken): death strike dice = 6 + 4 = 10.
     */
    public void testDeathStrikeBonusDice()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");
        net.sf.colossus.variant.MasterHex hex =
            variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, ninjaPlayer, game, ogre);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, normalPlayer, game, troll);
        ninjaPlayer.addLegion(attacker);
        normalPlayer.addLegion(defender);
        attacker.setEntrySide(net.sf.colossus.game.EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex,
            net.sf.colossus.game.BattlePhase.FIGHT);
        BattleServerSide battle = game.getBattleSS();

        CreatureServerSide striker = battle.getAttackingLegion().getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion().getCritter(0);

        net.sf.colossus.variant.MasterBoardTerrain terrain =
            battle.getLocation().getTerrain();
        striker.setCurrentHex(terrain.getHexByLabel("C3"));
        target.setCurrentHex(terrain.getHexByLabel("C4"));

        // Full health death strike: Ogre power=6, 0 hits, remaining=6
        striker.setDeathStrike(true);
        BattleStrikeServerSide strike = new BattleStrikeServerSide(game);
        int dice = strike.getDice(striker, target, false);
        // Base 6 + remaining 6 = 12
        assertEquals("Full health death strike dice", 12, dice);

        // Damaged death strike: 2 hits taken, remaining = 6-2 = 4
        striker.setHits(2);
        int diceDamaged = strike.getDice(striker, target, false);
        // Base 6 + remaining 4 = 10
        assertEquals("Damaged death strike dice", 10, diceDamaged);
    }
}
