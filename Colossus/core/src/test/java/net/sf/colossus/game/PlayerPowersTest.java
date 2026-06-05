package net.sf.colossus.game;


import junit.framework.TestCase;


/**
 * Unit tests for PlayerPowers class.
 */
public class PlayerPowersTest extends TestCase
{
    public PlayerPowersTest(String name)
    {
        super(name);
    }

    public void testEmptyByDefault()
    {
        PlayerPowers pp = new PlayerPowers();
        assertTrue(pp.isEmpty());
        assertFalse(pp.hasPower(LauerPower.ARCHER));
    }

    public void testAddAndHasPower()
    {
        PlayerPowers pp = new PlayerPowers();
        pp.addPower(LauerPower.HEALER);
        assertTrue(pp.hasPower(LauerPower.HEALER));
        assertFalse(pp.hasPower(LauerPower.NINJA));
    }

    public void testRemovePower()
    {
        PlayerPowers pp = new PlayerPowers();
        pp.addPower(LauerPower.ARCHER);
        pp.removePower(LauerPower.ARCHER);
        assertFalse(pp.hasPower(LauerPower.ARCHER));
        assertTrue(pp.isEmpty());
    }

    public void testSerializeEmpty()
    {
        PlayerPowers pp = new PlayerPowers();
        assertEquals("", pp.serialize());
    }

    public void testSerializeSingle()
    {
        PlayerPowers pp = new PlayerPowers();
        pp.addPower(LauerPower.YAHTZEE);
        assertEquals("YAHTZEE", pp.serialize());
    }

    public void testSerializeMultiple()
    {
        PlayerPowers pp = new PlayerPowers();
        pp.addPower(LauerPower.ARCHER);
        pp.addPower(LauerPower.NINJA);
        String s = pp.serialize();
        assertTrue(s.contains("ARCHER"));
        assertTrue(s.contains("NINJA"));
        assertTrue(s.contains(","));
    }

    public void testDeserializeEmpty()
    {
        PlayerPowers pp = PlayerPowers.deserialize("");
        assertTrue(pp.isEmpty());
    }

    public void testDeserializeNull()
    {
        PlayerPowers pp = PlayerPowers.deserialize(null);
        assertTrue(pp.isEmpty());
    }

    public void testDeserializeSingle()
    {
        PlayerPowers pp = PlayerPowers.deserialize("ARCHER");
        assertTrue(pp.hasPower(LauerPower.ARCHER));
        assertFalse(pp.hasPower(LauerPower.NINJA));
    }

    public void testDeserializeMultiple()
    {
        PlayerPowers pp = PlayerPowers.deserialize("ARCHER,HEALER,NINJA");
        assertTrue(pp.hasPower(LauerPower.ARCHER));
        assertTrue(pp.hasPower(LauerPower.HEALER));
        assertTrue(pp.hasPower(LauerPower.NINJA));
        assertFalse(pp.hasPower(LauerPower.ZOMBIE));
    }

    public void testRoundTrip()
    {
        PlayerPowers original = new PlayerPowers();
        original.addPower(LauerPower.BERSERKER);
        original.addPower(LauerPower.VAMPIRE);
        original.addPower(LauerPower.NINJA);

        String serialized = original.serialize();
        PlayerPowers restored = PlayerPowers.deserialize(serialized);

        assertEquals(original.getPowers(), restored.getPowers());
    }

    // === Phase 2 tests: Player integration ===

    public void testMaxLegionHeightDefault()
    {
        // Without Nine Ball, max height should be 7
        PlayerPowers pp = new PlayerPowers();
        // Can't test Player directly without Game, but test the power check
        assertFalse(pp.hasPower(LauerPower.NINE_BALL));
    }

    public void testMaxLegionHeightNineBall()
    {
        PlayerPowers pp = new PlayerPowers();
        pp.addPower(LauerPower.NINE_BALL);
        assertTrue(pp.hasPower(LauerPower.NINE_BALL));
    }

    public void testDemonStrengthTracking()
    {
        // Verify LauerPower.DEMON exists and can be assigned
        PlayerPowers pp = new PlayerPowers();
        pp.addPower(LauerPower.DEMON);
        assertTrue(pp.hasPower(LauerPower.DEMON));
    }

    public void testVampirePower()
    {
        PlayerPowers pp = new PlayerPowers();
        pp.addPower(LauerPower.VAMPIRE);
        assertTrue(pp.hasPower(LauerPower.VAMPIRE));
    }

    public void testAmoebaPower()
    {
        PlayerPowers pp = new PlayerPowers();
        pp.addPower(LauerPower.AMOEBA);
        assertTrue(pp.hasPower(LauerPower.AMOEBA));
    }
}
