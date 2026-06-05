package net.sf.colossus.server;


import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Movement;
import net.sf.colossus.game.MovementClientSide;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Client-side simulation test for Mulligan double teleport.
 * Simulates the exact code path Client.listTeleportMoves uses:
 * - GameClientSide state (movementRoll, mulliganDice, turnNumber)
 * - MovementClientSide.listTeleportMoves
 * - Client's union logic for both dice
 *
 * This catches bugs where server tests pass but client state is wrong.
 */
public class ScriptedClientTeleportTest extends TestCase
{
    private Game game;
    private Variant variant;
    private Movement movement;
    private Options opts;

    public ScriptedClientTeleportTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        variant = VariantSupport.loadVariantByName("Default", true);
        opts = new Options("test", true);
        game = new Game(variant, new String[0]);
        // Use MovementClientSide - the same class the real client uses
        movement = new MovementClientSide(game, opts);
    }

    /**
     * Simulate Client.tellMovementRoll parsing logic.
     */
    private void simulateTellMovementRoll(int roll, String reason,
        Player player)
    {
        game.setMovementRoll(roll);

        if (reason != null && reason.contains("Mulligan: "))
        {
            try
            {
                int idx = reason.indexOf("Mulligan: ") + 10;
                int andIdx = reason.indexOf(" and ", idx);
                int endIdx = reason.indexOf(")", andIdx);
                int die1 = Integer.parseInt(
                    reason.substring(idx, andIdx).trim());
                int die2 = Integer.parseInt(
                    reason.substring(andIdx + 5, endIdx).trim());
                game.setMulliganDice(die1, die2);
                if (player != null)
                {
                    player.setMulliganRolls(die1, die2);
                }
            }
            catch (Exception e)
            {
                // Parse failure - same as real client
                fail("Failed to parse Mulligan reason: " + reason
                    + " error: " + e.getMessage());
            }
        }
        else
        {
            game.setMulliganDice(0, 0);
        }
    }

    /**
     * Simulate Client.listTeleportMoves - the exact same logic.
     */
    private Set<MasterHex> clientListTeleportMoves(Legion legion)
    {
        MasterHex hex = legion.getCurrentHex();
        Set<MasterHex> result = movement.listTeleportMoves(legion, hex,
            game.getMovementRoll());
        // Mulligan/UDC: also include moves for the other die value
        if (game.getMulliganDie1() > 0)
        {
            int otherRoll = (game.getMovementRoll() == game.getMulliganDie1())
                ? game.getMulliganDie2() : game.getMulliganDie1();
            result.addAll(movement.listTeleportMoves(legion, hex, otherRoll));
        }
        return result;
    }

    // =================================================================
    // TEST: Both legions can teleport on turn 1 with Mulligan (rolls 2,3)
    // =================================================================

    public void testDoubleTeleportTurn1BothLegions()
    {
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        assertNotNull("Tower 100 must exist", tower);

        Player player = new Player(game, "MulliganPlayer", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        player.setStartingTower(tower);
        game.addPlayer(player);

        TestLegion legion1 = new TestLegion(player, "Rd01", tower);
        TestLegion legion2 = new TestLegion(player, "Rd02", tower);
        player.addLegion(legion1);
        player.addLegion(legion2);

        game.setTurnNumber(1);

        // Simulate server sending tellMovementRoll with max=3, reason includes
        // both dice
        simulateTellMovementRoll(3, "rolled 3 (Mulligan: 2 and 3)", player);

        // Verify client state is correct
        assertEquals("movementRoll should be 3", 3, game.getMovementRoll());
        assertEquals("mulliganDie1 should be 2", 2, game.getMulliganDie1());
        assertEquals("mulliganDie2 should be 3", 3, game.getMulliganDie2());
        assertEquals("turnNumber should be 1", 1, game.getTurnNumber());

        // Legion 1: should have teleport moves
        Set<MasterHex> teleport1 = clientListTeleportMoves(legion1);
        assertTrue("Legion 1 should have teleport moves on turn 1: "
            + teleport1.size(), teleport1.size() > 0);

        // Legion 1 teleports
        legion1.setTeleported(true);
        assertTrue("player.hasTeleported() should be true",
            player.hasTeleported());

        // Legion 2: should STILL have teleport moves (the bug!)
        Set<MasterHex> teleport2 = clientListTeleportMoves(legion2);
        assertTrue("Legion 2 should STILL have teleport moves after "
            + "legion 1 teleported (hasTeleported=" + player.hasTeleported()
            + ", turn=" + game.getTurnNumber()
            + ", roll=" + game.getMovementRoll()
            + ", mulDie1=" + game.getMulliganDie1()
            + "): got " + teleport2.size(),
            teleport2.size() > 0);
    }

    public void testDoubleTeleportWithRoll6And1()
    {
        // The user's exact scenario: rolled 6 and 1
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        Player player = new Player(game, "MulliganPlayer", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        player.setStartingTower(tower);
        game.addPlayer(player);

        TestLegion legion1 = new TestLegion(player, "Rd01", tower);
        TestLegion legion2 = new TestLegion(player, "Rd02", tower);
        player.addLegion(legion1);
        player.addLegion(legion2);

        game.setTurnNumber(1);
        simulateTellMovementRoll(6, "rolled 6 (Mulligan: 6 and 1)", player);

        assertEquals(6, game.getMovementRoll());
        assertEquals(6, game.getMulliganDie1());
        assertEquals(1, game.getMulliganDie2());

        // Legion 1 teleports (using roll 6 - normal teleport condition)
        Set<MasterHex> teleport1 = clientListTeleportMoves(legion1);
        assertTrue("Legion 1 should teleport on roll 6: " + teleport1.size(),
            teleport1.size() > 0);

        legion1.setTeleported(true);

        // Legion 2: should still get teleport (Mulligan bypass)
        Set<MasterHex> teleport2 = clientListTeleportMoves(legion2);
        assertTrue("Legion 2 should teleport after first teleported (roll 6,1): "
            + teleport2.size(), teleport2.size() > 0);
    }

    public void testDoubleTeleportWithNoSixRolled()
    {
        // Neither die is 6 — pure Mulligan bypass
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        Player player = new Player(game, "MulliganPlayer", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        player.setStartingTower(tower);
        game.addPlayer(player);

        TestLegion legion1 = new TestLegion(player, "Rd01", tower);
        TestLegion legion2 = new TestLegion(player, "Rd02", tower);
        player.addLegion(legion1);
        player.addLegion(legion2);

        game.setTurnNumber(1);
        simulateTellMovementRoll(4, "rolled 4 (Mulligan: 4 and 2)", player);

        // First teleport
        Set<MasterHex> teleport1 = clientListTeleportMoves(legion1);
        assertTrue("Legion 1 teleport on roll 4,2: " + teleport1.size(),
            teleport1.size() > 0);
        legion1.setTeleported(true);

        // Second teleport
        Set<MasterHex> teleport2 = clientListTeleportMoves(legion2);
        assertTrue("Legion 2 teleport after first (roll 4,2): "
            + teleport2.size(), teleport2.size() > 0);
    }

    public void testNonMulliganCannotDoubleTeleport()
    {
        // Without Mulligan, and without lords, no teleport at all
        // (Tower teleport requires numLords > 0, Mulligan bypass at line 262
        // requires Mulligan power)
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        Player player = new Player(game, "NormalPlayer", 0);
        // NO Mulligan
        player.setStartingTower(tower);
        game.addPlayer(player);

        TestLegion legion1 = new TestLegion(player, "Rd01", tower);
        player.addLegion(legion1);

        game.setTurnNumber(1);
        game.setMovementRoll(6);
        game.setMulliganDice(0, 0);

        // Without lords, even roll=6 doesn't give tower teleport
        Set<MasterHex> teleport1 = clientListTeleportMoves(legion1);
        assertEquals("No lords = no teleport even on roll 6",
            0, teleport1.size());
    }

    public void testMulliganTeleportWorksWithNullStartingTower()
    {
        // Reproduces the intermittent bug: client doesn't have startingTower
        // set due to race condition in PlayerClientSide initialization.
        // Fix: fall back to legion's current hex (which is the tower on turn 1).
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        Player player = new Player(game, "MulliganPlayer", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        // DO NOT set startingTower -- simulates the NPE-swallowing race
        // player.setStartingTower(tower);  <-- intentionally omitted!
        game.addPlayer(player);

        TestLegion legion1 = new TestLegion(player, "Rd01", tower);
        TestLegion legion2 = new TestLegion(player, "Rd02", tower);
        player.addLegion(legion1);
        player.addLegion(legion2);

        game.setTurnNumber(1);
        simulateTellMovementRoll(3, "rolled 3 (Mulligan: 2 and 3)", player);

        assertNull("startingTower should be null for this test",
            player.getStartingTower());

        // Should STILL work because of hex fallback
        Set<MasterHex> teleport1 = clientListTeleportMoves(legion1);
        assertTrue("Mulligan teleport should work even with null startingTower: "
            + teleport1.size(), teleport1.size() > 0);

        // Double teleport should also work
        legion1.setTeleported(true);
        Set<MasterHex> teleport2 = clientListTeleportMoves(legion2);
        assertTrue("Second teleport should work with null startingTower: "
            + teleport2.size(), teleport2.size() > 0);
    }

    /** Minimal Legion for testing. */
    private static class TestLegion extends Legion
    {
        TestLegion(Player player, String markerId, MasterHex hex)
        {
            super(player, markerId, hex);
        }

        @Override
        public void addCreature(net.sf.colossus.variant.CreatureType type) {}

        @Override
        public void removeCreature(net.sf.colossus.variant.CreatureType type) {}

        @Override
        public int getPointValue() { return 100; }
    }

    /** Legion with a lord — simulates the real game where split legions
     *  each have at least one lord (Titan or Angel). */
    private static class TestLegionWithLord extends TestLegion
    {
        TestLegionWithLord(Player player, String markerId, MasterHex hex)
        {
            super(player, markerId, hex);
        }

        @Override
        public int numLords() { return 1; }

        @Override
        public boolean hasTitan() { return false; }
    }

    /**
     * BUG REPRODUCER: Forced 6,6 on turn 1 with real lord-bearing legions.
     * Simulates the exact scenario the user sees:
     * - Mulligan turn 1, forced 6,6
     * - First legion teleports (has lord, roll=6 = normal tower teleport)
     * - Second legion should ALSO be able to teleport
     * - Both via tower teleport (lords > 0) AND Mulligan section
     *
     * The test explicitly sets legion1.setMoved(true) to match what the
     * server does after confirming the first move.
     */
    public void testForcedSixSixWithLordsSecondLegionCanTeleport()
    {
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        assertNotNull("Tower 100 must exist", tower);

        Player player = new Player(game, "MulliganPlayer", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        player.setStartingTower(tower);
        game.addPlayer(player);

        // Both legions have lords — like the real game after split
        TestLegionWithLord legion1 = new TestLegionWithLord(player, "Rd01", tower);
        TestLegionWithLord legion2 = new TestLegionWithLord(player, "Rd02", tower);
        player.addLegion(legion1);
        player.addLegion(legion2);

        game.setTurnNumber(1);
        // Forced 6,6 — what the server now sends
        simulateTellMovementRoll(6, "rolled 6 (Mulligan: 6 and 6)", player);

        assertEquals(6, game.getMovementRoll());
        assertEquals(6, game.getMulliganDie1());
        assertEquals(6, game.getMulliganDie2());

        // Legion 1: should have teleport moves (tower teleport, roll=6, has lord)
        Set<MasterHex> teleport1 = clientListTeleportMoves(legion1);
        assertTrue("Legion 1 (with lord) should have teleport on turn 1, roll 6: "
            + teleport1.size(), teleport1.size() > 0);

        // First legion teleports and moves away
        MasterHex dest1 = teleport1.iterator().next();
        legion1.setTeleported(true);
        legion1.setMoved(true);
        legion1.setCurrentHex(dest1);

        // KEY STATE: player.hasTeleported() = true now
        assertTrue("player.hasTeleported() must be true after first teleport",
            player.hasTeleported());

        // Legion 2: MUST still have teleport moves
        Set<MasterHex> teleport2 = clientListTeleportMoves(legion2);
        assertTrue("BUG: Legion 2 (with lord, on tower, roll=6, Mulligan turn 1) "
            + "MUST have teleport destinations after legion 1 teleported. "
            + "hasTeleported=" + player.hasTeleported()
            + " onTower=" + tower.getTerrain().isTower()
            + " numLords=" + legion2.numLords()
            + " roll=" + game.getMovementRoll()
            + " turn=" + game.getTurnNumber()
            + " Got: " + teleport2.size(),
            teleport2.size() > 0);

        // Verify teleport2 has destinations distinct from legion1's location
        Set<MasterHex> availableForSecond = new HashSet<MasterHex>(teleport2);
        availableForSecond.remove(dest1);
        assertTrue("Second legion must have at least one destination "
            + "different from where first went",
            availableForSecond.size() > 0);
    }
}
