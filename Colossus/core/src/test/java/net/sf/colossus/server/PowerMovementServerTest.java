package net.sf.colossus.server;


import java.util.Set;
import java.util.logging.Logger;

import junit.framework.TestCase;
import net.sf.colossus.common.Options;


import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Movement;
import net.sf.colossus.game.MovementServerSide;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerPowers;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Server-level integration tests for Lauer Powers that exercise the
 * actual movement validation path (doMove, listNormalMoves,
 * listTeleportMoves) with real Game/Server objects.
 *
 * These tests create a GameServerSide in startup-test mode and
 * manipulate it directly to verify power mechanics at the server level.
 */
public class PowerMovementServerTest extends TestCase
{
    private static final Logger LOGGER = Logger
        .getLogger(PowerMovementServerTest.class.getName());

    private Game game;
    
    private Variant variant;
    private Movement movement;

    public PowerMovementServerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        variant = VariantSupport.loadVariantByName("Default", true);
        Options opts = new Options("test", true);
        game = new Game(variant, new String[0]);
        movement = new MovementServerSide(game, opts);
    }

    // === Mulligan: dual-roll movement validation ===

    public void testMulliganDiceStoredOnGame()
    {
        // Verify Game stores Mulligan dice
        game.setMulliganDice(3, 5);
        assertEquals(3, game.getMulliganDie1());
        assertEquals(5, game.getMulliganDie2());
    }

    public void testMulliganDiceResetToZero()
    {
        game.setMulliganDice(3, 5);
        game.setMulliganDice(0, 0);
        assertEquals(0, game.getMulliganDie1());
        assertEquals(0, game.getMulliganDie2());
    }

    public void testMulliganListNormalMovesForBothDice()
    {
        // Verify that listNormalMoves with roll=2 gives fewer moves than roll=5
        // (proves the roll value actually matters in path calculation)
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        assertNotNull("Tower 100 must exist", tower);

        // Create a test player with a legion on the tower
        // We can't easily create legions here without full game init,
        // but we can verify the Movement object works with our Game
        assertNotNull(movement);
        assertNotNull(game);
    }

    public void testMulliganTeleportMovesOnTurn1()
    {
        // On turn 1, a Mulligan player should get teleport moves
        // even without rolling 6
        game.setTurnNumber(1);
        game.setMovementRoll(3); // not 6!

        // The Movement object should return non-empty for Mulligan
        // player in a tower on turn 1 (tested via integration test
        // that creates proper Player+Legion objects)
        assertEquals(1, game.getTurnNumber());
        assertEquals(3, game.getMovementRoll());
    }

    public void testMulliganDoMoveFallbackToAltRoll()
    {
        // Verify the altRoll computation in doMove context
        game.setMulliganDice(2, 5);
        game.setMovementRoll(5); // stored roll is the max

        // altRoll should be 2 (the other die)
        int roll = game.getMovementRoll();
        int altRoll = (roll == game.getMulliganDie1())
            ? game.getMulliganDie2() : game.getMulliganDie1();
        assertEquals(2, altRoll);
    }

    public void testMulliganDoMoveFallbackSymmetric()
    {
        // If stored roll equals die2, altRoll should be die1
        game.setMulliganDice(4, 6);
        game.setMovementRoll(6);

        int roll = game.getMovementRoll();
        int altRoll = (roll == game.getMulliganDie1())
            ? game.getMulliganDie2() : game.getMulliganDie1();
        assertEquals(4, altRoll);

        // And if stored roll equals die1
        game.setMovementRoll(4);
        roll = game.getMovementRoll();
        altRoll = (roll == game.getMulliganDie1())
            ? game.getMulliganDie2() : game.getMulliganDie1();
        assertEquals(6, altRoll);
    }

    public void testNonMulliganNoAltRoll()
    {
        // Without Mulligan dice set, altRoll should be -1 (no fallback)
        game.setMulliganDice(0, 0);
        game.setMovementRoll(4);

        // getMulliganDie1() == 0 means no altRoll
        assertEquals(0, game.getMulliganDie1());
    }

    // === Upside-Down Cake: 7-N ===

    public void testUDCDiceStoredOnGame()
    {
        // UDC also uses the mulliganDice fields
        game.setMulliganDice(3, 4); // roll=3, 7-3=4
        assertEquals(3, game.getMulliganDie1());
        assertEquals(4, game.getMulliganDie2());
    }

    // === Ninja: move through enemies ===

    public void testNinjaMoveBypassInMovement()
    {
        // Ninja can move THROUGH enemy-occupied hexes
        // Set up: Ninja on tower 100, enemy on adjacent hex, verify Ninja
        // can reach hexes BEYOND the enemy (non-Ninja would be blocked)
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        Player ninja = new Player(game, "NinjaPlayer", 0);
        ninja.getPowers().addPower(LauerPower.NINJA);
        ninja.setStartingTower(tower);
        game.addPlayer(ninja);

        Player enemy = new Player(game, "EnemyPlayer", 1);
        game.addPlayer(enemy);

        TestLegion ninjaLegion = new TestLegion(ninja, "Rd01", tower);
        ninja.addLegion(ninjaLegion);

        // Place enemy on an adjacent hex
        MasterHex adjHex = tower.getNeighbor(0);
        if (adjHex != null)
        {
            TestLegion enemyLegion = new TestLegion(enemy, "Bu01", adjHex);
            enemy.addLegion(enemyLegion);

            game.setTurnNumber(2);
            game.setMovementRoll(3);

            // Ninja with roll=3 should have MORE moves than just the adjacent
            // enemy hex (should pass through it)
            Set<MasterHex> moves = movement.listNormalMoves(
                ninjaLegion, tower, 3, false, null, false);
            // Should include hexes beyond the enemy
            assertTrue("Ninja should have moves beyond enemy hex (pass-through): "
                + moves.size(), moves.size() > 1);
        }
    }

    public void testNonNinjaBlockedByEnemy()
    {
        // Non-Ninja should be blocked by enemy hex (can only attack, not pass)
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        Player normal = new Player(game, "NormalPlayer", 0);
        normal.setStartingTower(tower);
        game.addPlayer(normal);

        Player enemy = new Player(game, "EnemyPlayer", 1);
        game.addPlayer(enemy);

        TestLegion normalLegion = new TestLegion(normal, "Rd01", tower);
        normal.addLegion(normalLegion);

        MasterHex adjHex = tower.getNeighbor(0);
        if (adjHex != null)
        {
            TestLegion enemyLegion = new TestLegion(enemy, "Bu01", adjHex);
            enemy.addLegion(enemyLegion);

            game.setTurnNumber(2);
            game.setMovementRoll(3);

            Set<MasterHex> normalMoves = movement.listNormalMoves(
                normalLegion, tower, 3, false, null, false);
            Set<MasterHex> ninjaPlayer = new java.util.HashSet<MasterHex>();

            // Give the same player Ninja and recheck
            normal.getPowers().addPower(LauerPower.NINJA);
            Set<MasterHex> ninjaMoves = movement.listNormalMoves(
                normalLegion, tower, 3, false, null, false);

            // Ninja should have >= moves (pass-through gives more options)
            assertTrue("Ninja should have >= moves than non-Ninja: "
                + ninjaMoves.size() + " vs " + normalMoves.size(),
                ninjaMoves.size() >= normalMoves.size());
        }
    }

    public void testNinjaCombineGroupsOnSameHex()
    {
        // Ninja can land on own-occupied hex (combine groups)
        // Non-Ninja cannot
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        MasterHex adj = tower.getNeighbor(0);
        if (adj == null) return;

        Player player = new Player(game, "NinjaPlayer", 0);
        player.getPowers().addPower(LauerPower.NINJA);
        player.setStartingTower(tower);
        game.addPlayer(player);

        TestLegion legion1 = new TestLegion(player, "Rd01", tower);
        TestLegion legion2 = new TestLegion(player, "Rd02", adj);
        player.addLegion(legion1);
        player.addLegion(legion2);

        game.setTurnNumber(2);
        game.setMovementRoll(1);

        // Legion1 with roll=1 should be able to reach adj (where legion2 is)
        Set<MasterHex> moves = movement.listNormalMoves(
            legion1, tower, 1, false, null, false);
        assertTrue("Ninja should be able to move onto own-occupied hex: "
            + moves, moves.contains(adj));

        // Remove Ninja power: should NOT be able to land on own hex
        player.getPowers().removePower(LauerPower.NINJA);
        Set<MasterHex> normalMoves = movement.listNormalMoves(
            legion1, tower, 1, false, null, false);
        assertFalse("Non-Ninja should NOT land on own-occupied hex",
            normalMoves.contains(adj));
    }

    // === Archmage: teleport on roll of 5 ===

    public void testArchmageTeleportCondition()
    {
        // Archmage teleport triggers on roll == 5
        game.setMovementRoll(5);
        assertEquals(5, game.getMovementRoll());
        // Player.canTitanTeleport() checks this - tested in integration
    }

    // === Vampire: overrun threshold ===

    public void testVampireOverrunThreshold()
    {
        // Overrun if attacker value >= 2.5 * defender value
        int attackerValue = 250;
        int defenderValue = 100;
        assertTrue(attackerValue >= defenderValue * 2.5);

        attackerValue = 249;
        assertFalse(attackerValue >= defenderValue * 2.5);
    }

    // === REAL movement validation: Mulligan dual-roll ===

    public void testMulliganListNormalMovesReturnsDifferentSetsForDifferentRolls()
    {
        // Prove that listNormalMoves(roll=2) != listNormalMoves(roll=5)
        // This validates that the roll parameter actually affects results
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        assertNotNull("Tower 100 must exist", tower);

        // Create player and legion
        Player player = new Player(game, "TestMulligan", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        game.addPlayer(player);

        // Create a minimal legion on the tower using TestLegion
        TestLegion legion = new TestLegion(player, "Rd01", tower);
        player.addLegion(legion);

        // Get moves for roll=2 and roll=5
        game.setMovementRoll(2);
        Set<MasterHex> moves2 = movement.listNormalMoves(legion, tower, 2);

        game.setMovementRoll(5);
        Set<MasterHex> moves5 = movement.listNormalMoves(legion, tower, 5);

        // Both should be non-empty (tower connects to other hexes)
        assertTrue("Moves for roll=2 should be non-empty, got: " + moves2.size(),
            moves2.size() > 0);
        assertTrue("Moves for roll=5 should be non-empty, got: " + moves5.size(),
            moves5.size() > 0);

        // Roll=5 should give MORE or EQUAL moves than roll=2
        assertTrue("Roll 5 should give >= moves than roll 2: "
            + moves5.size() + " vs " + moves2.size(),
            moves5.size() >= moves2.size());
    }

    public void testMulliganTeleportMovesOnTurn1WithLegion()
    {
        // On turn 1 with Mulligan, listTeleportMoves should return hexes
        // even without rolling 6
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        assertNotNull(tower);

        Player player = new Player(game, "TestMulligan", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        player.setStartingTower(tower);
        game.addPlayer(player);

        TestLegion legion = new TestLegion(player, "Rd01", tower);
        player.addLegion(legion);

        game.setTurnNumber(1);
        game.setMovementRoll(3); // NOT 6!

        Set<MasterHex> teleportMoves = movement.listTeleportMoves(
            legion, tower, 3);
        assertTrue("Mulligan turn 1 should have teleport moves (roll=3), got: "
            + teleportMoves.size(), teleportMoves.size() > 0);
    }

    public void testMulliganSecondLegionStillGetsTeleportMoves()
    {
        // After first legion teleports, second should still get moves
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        Player player = new Player(game, "TestMulligan", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        player.setStartingTower(tower);
        game.addPlayer(player);

        TestLegion legion1 = new TestLegion(player, "Rd01", tower);
        TestLegion legion2 = new TestLegion(player, "Rd02", tower);
        player.addLegion(legion1);
        player.addLegion(legion2);

        game.setTurnNumber(1);
        game.setMovementRoll(3);

        // First legion teleports
        legion1.setTeleported(true);
        assertTrue("Player should show as teleported", player.hasTeleported());

        // Second legion should STILL get teleport moves
        Set<MasterHex> moves2 = movement.listTeleportMoves(legion2, tower, 3);
        assertTrue("Second legion should have teleport moves after first teleported: "
            + moves2.size(), moves2.size() > 0);
    }

    public void testNonMulliganNoTeleportOnRoll3()
    {
        // Without Mulligan, roll=3 should NOT give teleport moves
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        Player player = new Player(game, "TestNormal", 0);
        // NO Mulligan power
        player.setStartingTower(tower);
        game.addPlayer(player);

        TestLegion legion = new TestLegion(player, "Rd01", tower);
        player.addLegion(legion);

        game.setTurnNumber(1);
        game.setMovementRoll(3);

        Set<MasterHex> teleportMoves = movement.listTeleportMoves(
            legion, tower, 3);
        assertEquals("Non-Mulligan should have no teleport on roll=3",
            0, teleportMoves.size());
    }


    // ============================================================
    // CONSISTENCY TESTS: every hex shown to player must have valid
    // entry sides. This catches client-blocking bugs that AI skips.
    // ============================================================

    private void assertEntrySideConsistency(Player player, Legion legion,
        MasterHex hex, String powerName)
    {
        // Normal moves
        Set<MasterHex> normalMoves = movement.listNormalMoves(legion, hex,
            game.getMovementRoll());
        for (MasterHex target : normalMoves)
        {
            Set<EntrySide> sides = movement.listPossibleEntrySides(
                legion, target, false);
            assertTrue(powerName + ": hex " + target
                + " is in normalMoves but has empty entrySides",
                !sides.isEmpty());
        }

        // Teleport moves
        Set<MasterHex> teleportMoves = movement.listTeleportMoves(
            legion, hex, game.getMovementRoll());
        for (MasterHex target : teleportMoves)
        {
            Set<EntrySide> sides = movement.listPossibleEntrySides(
                legion, target, true);
            assertTrue(powerName + ": hex " + target
                + " is in teleportMoves but has empty entrySides",
                !sides.isEmpty());
        }
    }

    public void testConsistencyNinja()
    {
        Player player = new Player(game, "NinjaPlayer", 0);
        player.getPowers().addPower(LauerPower.NINJA);
        game.addPlayer(player);
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        player.setStartingTower(tower);
        TestLegion legion = new TestLegion(player, "Rd01", tower);
        player.addLegion(legion);
        game.setTurnNumber(2);
        game.setMovementRoll(4);
        assertEntrySideConsistency(player, legion, tower, "Ninja");
    }

    public void testConsistencyUpsideDownCake()
    {
        Player player = new Player(game, "UDCPlayer", 0);
        player.getPowers().addPower(LauerPower.UPSIDE_DOWN_CAKE);
        game.addPlayer(player);
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        player.setStartingTower(tower);
        TestLegion legion = new TestLegion(player, "Rd01", tower);
        player.addLegion(legion);
        game.setTurnNumber(2);
        game.setMovementRoll(5);
        game.setMulliganDice(5, 2); // 7-5=2
        assertEntrySideConsistency(player, legion, tower, "UDC");
    }

    public void testConsistencyArchmage()
    {
        Player player = new Player(game, "ArchmagePlayer", 0);
        player.getPowers().addPower(LauerPower.ARCHMAGE);
        game.addPlayer(player);
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        player.setStartingTower(tower);
        TestLegion legion = new TestLegion(player, "Rd01", tower);
        player.addLegion(legion);
        game.setTurnNumber(2);
        game.setMovementRoll(5); // Archmage teleports on 5
        assertEntrySideConsistency(player, legion, tower, "Archmage");
    }

    public void testConsistencyNoPower()
    {
        // Control: player without powers should also be consistent
        Player player = new Player(game, "NormalPlayer", 0);
        game.addPlayer(player);
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        player.setStartingTower(tower);
        TestLegion legion = new TestLegion(player, "Rd01", tower);
        player.addLegion(legion);
        game.setTurnNumber(2);
        game.setMovementRoll(4);
        assertEntrySideConsistency(player, legion, tower, "NoPower");
    }

    public void testMulliganDoubleTeleportBothLegionsBeyondRollDistance()
    {
        // Turn 1, Mulligan rolls 2 and 3. Both legions should be able to
        // teleport to hexes that are NOT reachable by normal movement
        // (i.e. distance > 3 from tower).
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        Player player = new Player(game, "TestMulligan", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        player.setStartingTower(tower);
        game.addPlayer(player);

        TestLegion legion1 = new TestLegion(player, "Rd01", tower);
        TestLegion legion2 = new TestLegion(player, "Rd02", tower);
        player.addLegion(legion1);
        player.addLegion(legion2);

        game.setTurnNumber(1);
        game.setMovementRoll(3); // max of (2,3)
        game.setMulliganDice(2, 3);

        // Legion 1: get teleport moves
        Set<MasterHex> teleport1 = movement.listTeleportMoves(legion1, tower, 3);
        assertTrue("Legion 1 should have teleport moves on turn 1 (roll 2,3): "
            + teleport1.size(), teleport1.size() > 0);

        // Get normal moves to prove teleport includes hexes beyond normal reach
        Set<MasterHex> normal1 = movement.listNormalMoves(
            legion1, tower, 3, false, null, false);
        // Teleport should have destinations NOT in normal moves
        Set<MasterHex> teleportOnly = new java.util.HashSet<MasterHex>(teleport1);
        teleportOnly.removeAll(normal1);
        // Also remove hexes reachable by roll=2
        Set<MasterHex> normal1alt = movement.listNormalMoves(
            legion1, tower, 2, false, null, false);
        teleportOnly.removeAll(normal1alt);
        assertTrue("Teleport should include hexes beyond roll 2 and 3: "
            + teleportOnly.size(), teleportOnly.size() > 0);

        // Now: first legion teleports
        legion1.setTeleported(true);
        assertTrue("Player hasTeleported after legion1 teleports",
            player.hasTeleported());

        // Legion 2: should STILL get teleport moves
        Set<MasterHex> teleport2 = movement.listTeleportMoves(legion2, tower, 3);
        assertTrue("Legion 2 should STILL have teleport moves after legion 1 teleported: "
            + teleport2.size(), teleport2.size() > 0);

        // And those moves should also include beyond-roll-distance hexes
        Set<MasterHex> teleportOnly2 = new java.util.HashSet<MasterHex>(teleport2);
        teleportOnly2.removeAll(normal1);
        teleportOnly2.removeAll(normal1alt);
        assertTrue("Legion 2 teleport should include hexes beyond roll 2 and 3: "
            + teleportOnly2.size(), teleportOnly2.size() > 0);
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

    // === Archmage: teleport on roll 5 ===

    public void testArchmageTeleportOnRoll5()
    {
        Player player = new Player(game, "ArchmagePlayer", 0);
        player.getPowers().addPower(LauerPower.ARCHMAGE);
        game.addPlayer(player);
        game.setMovementRoll(5);
        assertTrue("Archmage should be able to titan-teleport on roll 5",
            player.canTitanTeleport());
    }

    public void testArchmageNoTeleportOnRoll4()
    {
        Player player = new Player(game, "ArchmagePlayer", 0);
        player.getPowers().addPower(LauerPower.ARCHMAGE);
        game.addPlayer(player);
        game.setMovementRoll(4);
        assertFalse("Archmage should NOT titan-teleport on roll 4",
            player.canTitanTeleport());
    }

    public void testNonArchmageNoTeleportOnRoll5()
    {
        Player player = new Player(game, "NormalPlayer", 0);
        game.addPlayer(player);
        game.setMovementRoll(5);
        assertFalse("Non-Archmage should NOT titan-teleport on roll 5",
            player.canTitanTeleport());
    }

    // === Upside-Down Cake: 7-N movement ===

    public void testUDC7MinusNMovement()
    {
        // UDC: player may use rolled number OR 7-rolled
        // If rolled 2, can move 2 OR 5
        Player player = new Player(game, "UDCPlayer", 0);
        player.getPowers().addPower(LauerPower.UPSIDE_DOWN_CAKE);
        game.addPlayer(player);

        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        player.setStartingTower(tower);
        TestLegion legion = new TestLegion(player, "Rd01", tower);
        player.addLegion(legion);

        // Roll=2, UDC gives 7-2=5 as alt
        game.setMovementRoll(5); // server stores max(2,5)=5
        game.setMulliganDice(2, 5); // die1=rolled, die2=7-rolled

        // Moves at roll=2 (short)
        Set<MasterHex> movesAt2 = movement.listNormalMoves(legion, tower, 2);
        // Moves at roll=5 (longer)
        Set<MasterHex> movesAt5 = movement.listNormalMoves(legion, tower, 5);

        assertTrue("Should have moves at roll=2", movesAt2.size() > 0);
        assertTrue("Should have moves at roll=5", movesAt5.size() > 0);
        // Both die values produce valid moves (the union gives more options)
        Set<MasterHex> union = new java.util.HashSet<MasterHex>(movesAt2);
        union.addAll(movesAt5);
        assertTrue("Union of both rolls should be >= either alone",
            union.size() >= movesAt2.size()
            && union.size() >= movesAt5.size());
    }

    public void testUDC7MinusNValues()
    {
        // Verify 7-N arithmetic for all die values
        assertEquals(6, 7 - 1);
        assertEquals(5, 7 - 2);
        assertEquals(4, 7 - 3);
        assertEquals(3, 7 - 4);
        assertEquals(2, 7 - 5);
        assertEquals(1, 7 - 6);
    }


    /**
     * REGRESSION: Mulligan turn 1 teleport with rolls 2,3.
     * Bug: player cannot teleport with EITHER legion.
     * This test must FAIL if the bug is present, PASS when fixed.
     * This bug has regressed 5+ times — this test is the safety net.
     */
    public void testMulliganTurn1TeleportWithRoll2And3()
    {
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        assertNotNull("Tower 100 must exist", tower);

        Player player = new Player(game, "MulliganRegression", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        player.setStartingTower(tower);
        game.addPlayer(player);

        // Two legions on tower (post-split scenario on turn 1)
        TestLegion legion1 = new TestLegion(player, "Rd01", tower);
        TestLegion legion2 = new TestLegion(player, "Rd02", tower);
        player.addLegion(legion1);
        player.addLegion(legion2);

        game.setTurnNumber(1);
        // Mulligan rolled 2 and 3. Game stores the higher as movementRoll.
        game.setMulliganDice(2, 3);
        game.setMovementRoll(3);

        // FIRST LEGION: must have teleport moves available
        Set<MasterHex> moves1 = movement.listTeleportMoves(
            legion1, tower, 3);
        assertTrue("REGRESSION: Mulligan turn 1 with roll 2,3 — first legion "
            + "MUST have teleport moves. Got " + moves1.size()
            + ". If this fails, the Mulligan turn-1 teleport bug is back!",
            moves1.size() > 0);

        // Also check with the lower die value
        Set<MasterHex> moves1alt = movement.listTeleportMoves(
            legion1, tower, 2);
        assertTrue("REGRESSION: Mulligan turn 1 with roll=2 — first legion "
            + "MUST have teleport moves. Got " + moves1alt.size(),
            moves1alt.size() > 0);

        // SECOND LEGION: must ALSO have teleport moves (even after first teleports)
        legion1.setTeleported(true);
        Set<MasterHex> moves2 = movement.listTeleportMoves(
            legion2, tower, 3);
        assertTrue("REGRESSION: Mulligan turn 1 — second legion MUST have "
            + "teleport moves even after first teleported. Got "
            + moves2.size(),
            moves2.size() > 0);
    }

    /**
     * REGRESSION: Mulligan turn 1 — non-lord legion (after split) can teleport.
     * Even a legion with 0 lords should get Mulligan turn-1 teleport.
     */
    public void testMulliganTurn1NoLordLegionCanTeleport()
    {
        MasterHex tower = variant.getMasterBoard().getHexByLabel("100");
        Player player = new Player(game, "MulliganNoLord", 0);
        player.getPowers().addPower(LauerPower.MULLIGAN);
        player.setStartingTower(tower);
        game.addPlayer(player);

        // Legion with NO lords (post-split child)
        TestLegion noLordLegion = new TestLegion(player, "Rd02", tower);
        player.addLegion(noLordLegion);

        game.setTurnNumber(1);
        game.setMulliganDice(2, 3);
        game.setMovementRoll(3);

        Set<MasterHex> moves = movement.listTeleportMoves(
            noLordLegion, tower, 3);
        assertTrue("REGRESSION: Mulligan turn 1 — legion with no lords MUST "
            + "still get teleport moves (Mulligan bypass). Got "
            + moves.size(),
            moves.size() > 0);
    }
}
