package net.sf.colossus.game;


import junit.framework.TestCase;


/**
 * Unit tests for LauerPower enum.
 */
public class LauerPowerTest extends TestCase
{
    public LauerPowerTest(String name)
    {
        super(name);
    }

    public void testAllPowersExist()
    {
        assertEquals(23, LauerPower.getAll().length);
    }

    public void testDisplayNamesNonNull()
    {
        for (LauerPower p : LauerPower.getAll())
        {
            assertNotNull(p.getDisplayName());
            assertTrue(p.getDisplayName().length() > 0);
        }
    }

    public void testDescriptionsNonNull()
    {
        for (LauerPower p : LauerPower.getAll())
        {
            assertNotNull(p.getDescription());
            assertTrue(p.getDescription().length() > 0);
        }
    }

    public void testToStringReturnsDisplayName()
    {
        assertEquals("Archer", LauerPower.ARCHER.toString());
        assertEquals("Upside-Down Cake",
            LauerPower.UPSIDE_DOWN_CAKE.toString());
    }

    public void testGetAllReturnsValues()
    {
        assertEquals(LauerPower.values().length, LauerPower.getAll().length);
    }
}
