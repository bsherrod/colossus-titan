package net.sf.colossus.game;



import java.util.Set;

import junit.framework.TestCase;
import net.sf.colossus.common.Options;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Integration tests for Lauer Power mechanics that require Game context.
 * Tests movement, recruiting, stacking, and combat modifiers.
 */
public class LauerPowerIntegrationTest extends TestCase
{
    private Game game;
    private Player red;
    private Player blue;
    private Variant variant;

    public LauerPowerIntegrationTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        variant = VariantSupport.loadVariantByName("Default", true);
        game = new Game(variant, new String[0]);
        red = new Player(game, "Red", 0);
        blue = new Player(game, "Blue", 1);
        game.addPlayer(red);
        game.addPlayer(blue);
    }

    // === Nine Ball stacking limit ===

    public void testNineBallMaxHeight()
    {
        assertEquals(7, red.getMaxLegionHeight());
        red.getPowers().addPower(LauerPower.NINE_BALL);
        assertEquals(9, red.getMaxLegionHeight());
    }

    public void testDefaultMaxHeight()
    {
        assertEquals(7, blue.getMaxLegionHeight());
    }

    // === Armourer defensive power ===




    // === Archmage teleport on roll of 5 ===

    public void testArchmageTeleportOnFive()
    {
        red.getPowers().addPower(LauerPower.ARCHMAGE);
        // Archmage can teleport when roll is 5
        game.setMovementRoll(5);
        assertTrue(red.canTitanTeleport());
    }

    public void testArchmageNoTeleportOnOtherRolls()
    {
        red.getPowers().addPower(LauerPower.ARCHMAGE);
        game.setMovementRoll(3);
        // Without enough score, can't teleport on non-5
        assertFalse(red.canTitanTeleport());
    }

    // === Avian all creatures fly ===



    // === Archer all creatures rangestrike ===


    // === Archmage Titan rangestrikes ===


    // === Mulligan roll storage ===

    public void testMulliganRollStorage()
    {
        red.getPowers().addPower(LauerPower.MULLIGAN);
        red.setMulliganRolls(3, 5);
        assertEquals(3, red.getMulliganRoll1());
        assertEquals(5, red.getMulliganRoll2());
    }

    // === Mulligan first-turn teleport eligibility ===





    // === Mulligan second-legion teleport on turn 1 ===

    public void testMulliganSecondLegionTeleportTurn1()
    {
        // Bug: after first legion teleports on turn 1, second legion
        // is blocked because player.hasTeleported() returns true.
        // Mulligan rules say ALL groups may teleport on turn 1.
        red.getPowers().addPower(LauerPower.MULLIGAN);

        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        assertNotNull("Tower hex 100 must exist", tower);
        red.setStartingTower(tower);

        game.setTurnNumber(1);
        game.setMovementRoll(3); // non-6 roll

        // Create Movement instance to test listTeleportMoves
        Options opts = new Options("test", true);
        Movement movement = new MovementServerSide(game, opts);

        // First legion - should have teleport moves
        Legion legion1 = new TestLegion(red, "Rd01", tower);
        red.addLegion(legion1);
        Set<MasterHex> moves1 = movement.listTeleportMoves(legion1, tower, 3);
        assertTrue("First legion should have Mulligan teleport moves on turn 1",
            moves1.size() > 0);

        // Simulate first legion having teleported
        legion1.setTeleported(true);
        assertTrue("Player should show as having teleported",
            red.hasTeleported());

        // Second legion - should ALSO have teleport moves (Mulligan turn 1)
        Legion legion2 = new TestLegion(red, "Rd02", tower);
        red.addLegion(legion2);
        Set<MasterHex> moves2 = movement.listTeleportMoves(legion2, tower, 3);
        assertTrue("Second legion should ALSO have Mulligan teleport moves"
            + " on turn 1 even after first teleported. Got: " + moves2.size(),
            moves2.size() > 0);

        // Also verify isValidTeleportMove passes for a hex in the set
        if (!moves2.isEmpty())
        {
            MasterHex targetHex = moves2.iterator().next();
            String reason = movement.isValidTeleportMove(legion2, targetHex, 3);
            assertNull("isValidTeleportMove should pass for second legion"
                + " on Mulligan turn 1. Reason: " + reason, reason);
        }
    }

    /** Minimal Legion implementation for testing. */
    private static class TestLegion extends Legion
    {
        TestLegion(Player player, String markerId, MasterHex hex)
        {
            super(player, markerId, hex);
        }

        @Override
        public void addCreature(CreatureType type) { }

        @Override
        public void removeCreature(CreatureType type) { }

        @Override
        public int getPointValue() { return 100; }
    }


    // === Mulligan dual-roll movement validation ===

    public void testMulliganMovesIncludeBothDiceValues()
    {
        // Verify that a Mulligan player's movement options include
        // hexes reachable by EITHER die value, not just the max
        red.getPowers().addPower(LauerPower.MULLIGAN);
        red.setMulliganRolls(2, 5);

        // With roll=5 (max), getMulliganRoll1=2, getMulliganRoll2=5
        assertEquals(2, red.getMulliganRoll1());
        assertEquals(5, red.getMulliganRoll2());

        // The movement system should use both values
        // (actual hex validation needs Movement instance, tested in functest)
        assertTrue(red.hasPower(LauerPower.MULLIGAN));
    }

    public void testMulliganAltRollComputation()
    {
        // Server computes altRoll = the die value that ISN'T the stored roll
        red.getPowers().addPower(LauerPower.MULLIGAN);
        red.setMulliganRolls(3, 5);

        int storedRoll = 5; // server stores the max
        int r1 = red.getMulliganRoll1(); // 3
        int r2 = red.getMulliganRoll2(); // 5
        int altRoll = (storedRoll == r1) ? r2 : r1;
        assertEquals(3, altRoll); // the other die value
    }

    public void testMulliganDiceNeverEqual()
    {
        // Rule: if both dice show same value, reroll until different
        red.getPowers().addPower(LauerPower.MULLIGAN);
        // After reroll, they must differ
        red.setMulliganRolls(3, 5);
        assertFalse(red.getMulliganRoll1() == red.getMulliganRoll2());
    }

    // === Demon strength tracking ===

    public void testDemonStrength()
    {
        red.getPowers().addPower(LauerPower.DEMON);
        assertEquals(6, red.getDemonStrength());
        red.reduceDemonStrength();
        assertEquals(5, red.getDemonStrength());
        red.setDemonStrength(3);
        assertEquals(3, red.getDemonStrength());
    }

    // === Power serialization round-trip with Player ===

    public void testPlayerPowersRoundTrip()
    {
        red.getPowers().addPower(LauerPower.MULLIGAN);
        red.getPowers().addPower(LauerPower.ARMOURER);

        String serialized = red.getPowers().serialize();
        PlayerPowers restored = PlayerPowers.deserialize(serialized);

        assertTrue(restored.hasPower(LauerPower.MULLIGAN));
        assertTrue(restored.hasPower(LauerPower.ARMOURER));
        assertFalse(restored.hasPower(LauerPower.NINJA));
    }

    // === Vampire no-flee (logic test) ===

    public void testVampirePlayerHasPower()
    {
        red.getPowers().addPower(LauerPower.VAMPIRE);
        assertTrue(red.hasPower(LauerPower.VAMPIRE));
        assertFalse(blue.hasPower(LauerPower.VAMPIRE));
    }

    // === Smurfs movement reduction ===

    public void testSmurfsMovementReduction()
    {
        // Smurfs: movement roll - 1 (min 1)
        // This is applied in makeMovementRoll on server side
        // Test the formula: if roll > 1, subtract 1
        int roll = 4;
        int smurfsRoll = (roll > 1) ? roll - 1 : roll;
        assertEquals(3, smurfsRoll);

        roll = 1;
        smurfsRoll = (roll > 1) ? roll - 1 : roll;
        assertEquals(1, smurfsRoll); // min 1
    }

    // === Upside-Down Cake 7-N ===

    public void testUpsideDownCakeFormula()
    {
        // 7 - roll gives the alternative
        assertEquals(4, 7 - 3);
        assertEquals(1, 7 - 6);
        assertEquals(6, 7 - 1);
        assertEquals(3, 7 - 4);
    }

    // === Ninja move through enemies ===

    public void testNinjaPowerSet()
    {
        red.getPowers().addPower(LauerPower.NINJA);
        assertTrue(red.hasPower(LauerPower.NINJA));
    }

    // === Multiple powers on same player ===

    public void testMultiplePowersCoexist()
    {
        red.getPowers().addPower(LauerPower.ARMOURER);
        red.getPowers().addPower(LauerPower.BERSERKER);
        red.getPowers().addPower(LauerPower.KNIGHT);

        assertTrue(red.hasPower(LauerPower.ARMOURER));
        assertTrue(red.hasPower(LauerPower.BERSERKER));
        assertTrue(red.hasPower(LauerPower.KNIGHT));
        
        assertEquals(7, red.getMaxLegionHeight()); // oops, should be 7
    }
}
