package net.sf.colossus.server;

import java.util.List;

import junit.framework.TestCase;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Tests for Administrator recruiting power.
 * Administrator: need one fewer creature for upward recruiting.
 * E.g. 1 Gargoyle recruits Cyclops in Brush (normally needs 2).
 * E.g. 2 Lions recruit Griffon in Desert (normally needs 3).
 */
public class AdministratorRecruitTest extends TestCase
{
    private GameServerSide game;
    private Player player;
    private Variant variant;

    public AdministratorRecruitTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        // doRecruit requires history; init it for test context
        java.lang.reflect.Field histField =
            GameServerSide.class.getDeclaredField("history");
        histField.setAccessible(true);
        histField.set(game, new History());
        player = game.createAndAddPlayer("Admin", "SimpleAI");
        player.getPowers().addPower(LauerPower.ADMINISTRATOR);
    }

    /**
     * 1 Gargoyle should recruit Cyclops in Brush with Administrator.
     * Normal: 2 Gargoyles needed. Administrator: 2-1 = 1.
     */
    public void testOneGargoyleRecruitsCyclopsInBrush()
    {
        // Find a Brush hex
        MasterHex brushHex = findHexByTerrain("Brush");
        assertNotNull("Must find a Brush hex", brushHex);

        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");
        assertNotNull("Gargoyle type", gargoyle);
        assertNotNull("Cyclops type", cyclops);

        // Create legion with just 1 Gargoyle + Titan (need at least 2 for valid legion)
        CreatureType titan = variant.getCreatureByName("Titan");
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, player, game, titan, gargoyle);
        player.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(legion, brushHex);

        assertTrue("With Administrator, 1 Gargoyle should recruit Cyclops in Brush. "
            + "Got: " + recruits,
            recruits.contains(cyclops));
    }

    /**
     * 2 Lions should recruit Griffon in Desert with Administrator.
     * Normal: 3 Lions needed. Administrator: 3-1 = 2.
     */
    public void testTwoLionsRecruitGriffonInDesert()
    {
        MasterHex desertHex = findHexByTerrain("Desert");
        assertNotNull("Must find a Desert hex", desertHex);

        CreatureType lion = variant.getCreatureByName("Lion");
        CreatureType griffon = variant.getCreatureByName("Griffon");
        assertNotNull("Lion type", lion);
        assertNotNull("Griffon type", griffon);

        CreatureType titan = variant.getCreatureByName("Titan");
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            desertHex, desertHex, player, game, titan, lion, lion);
        player.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(legion, desertHex);

        assertTrue("With Administrator, 2 Lions should recruit Griffon in Desert. "
            + "Got: " + recruits,
            recruits.contains(griffon));
    }

    /**
     * Without Administrator, 1 Gargoyle should NOT recruit Cyclops.
     */
    public void testOneGargoyleCannotRecruitWithoutAdministrator()
    {
        // Remove power
        player.getPowers().removePower(LauerPower.ADMINISTRATOR);

        MasterHex brushHex = findHexByTerrain("Brush");
        assertNotNull("Must find a Brush hex", brushHex);

        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");
        CreatureType titan = variant.getCreatureByName("Titan");

        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, player, game, titan, gargoyle);
        player.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(legion, brushHex);

        assertFalse("Without Administrator, 1 Gargoyle should NOT recruit Cyclops. "
            + "Got: " + recruits,
            recruits.contains(cyclops));
    }

    /**
     * BUG: Server-side findEligibleRecruiters rejects Centaur as recruiter
     * for Warbear in Woods with Administrator (needs 3, have 2, -1 = 2 OK).
     * The server validates the recruiter but doesn't apply the -1 reduction.
     * This test should FAIL before the fix.
     */
    public void testCentaurIsValidRecruiterForWarbearWithAdministrator()
    {
        MasterHex woodsHex = findHexByTerrain("Woods");
        assertNotNull("Must find a Woods hex", woodsHex);

        CreatureType centaur = variant.getCreatureByName("Centaur");
        CreatureType warbear = variant.getCreatureByName("Warbear");
        CreatureType titan = variant.getCreatureByName("Titan");
        assertNotNull("Centaur type", centaur);
        assertNotNull("Warbear type", warbear);

        // 2 Centaurs in Woods: normally need 3 for Warbear, Administrator -1 = 2
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            woodsHex, woodsHex, player, game, titan, centaur, centaur);
        player.addLegion(legion);

        // First verify the recruit IS eligible
        List<CreatureType> recruits = game.findEligibleRecruits(legion, woodsHex);
        assertTrue("Warbear should be recruitable with 2 Centaurs + Administrator. "
            + "Got: " + recruits,
            recruits.contains(warbear));

        // NOW verify Centaur is listed as a valid recruiter
        // (this is what the server checks in doRecruit validation)
        List<CreatureType> validRecruiters = game.findEligibleRecruiters(
            legion, "Warbear");
        assertTrue("Centaur must be a valid recruiter for Warbear with Administrator. "
            + "Got: " + validRecruiters
            + " (this fails if findEligibleRecruiters doesn't apply -1)",
            validRecruiters.contains(centaur));
    }

    /**
     * Administrator 25% rule: any creature worth at least 25% more points
     * can recruit any terrain creature available.
     * E.g. a Unicorn (12 pts) in Brush should recruit Cyclops (9 pts)
     * because 12*4=48 >= 9*5=45.
     */
    public void testTwentyFivePercentRuleServerSide()
    {
        MasterHex brushHex = findHexByTerrain("Brush");
        assertNotNull("Must find a Brush hex", brushHex);

        CreatureType unicorn = variant.getCreatureByName("Unicorn");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");
        CreatureType titan = variant.getCreatureByName("Titan");
        assertNotNull("Unicorn type", unicorn);
        assertNotNull("Cyclops type", cyclops);

        // Unicorn is 12 pts, Cyclops is 9 pts. 12*4=48 >= 9*5=45 → qualifies
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, player, game, titan, unicorn);
        player.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(legion, brushHex);

        assertTrue("With Administrator 25% rule, Unicorn (12pts) should enable "
            + "recruiting Cyclops (9pts) in Brush. Got: " + recruits,
            recruits.contains(cyclops));
    }

    /**
     * Administrator 25% rule negative: creature NOT worth 25% more cannot
     * recruit via this rule.
     * E.g. Ogre (6 pts) cannot recruit Gorgon (12 pts) via 25% rule
     * because 6*4=24 < 12*5=60. (Use Plains to avoid normal recruit paths.)
     */
    public void testTwentyFivePercentRuleRejectsInsufficientValue()
    {
        MasterHex plainsHex = findHexByTerrain("Plains");
        assertNotNull("Must find a Plains hex", plainsHex);

        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType gorgon = variant.getCreatureByName("Gorgon");

        // Ogre is 6 pts, Gorgon is 12 pts. 6*4=24 < 12*5=60 → does NOT qualify
        // Plains has Centaur as base recruit, not Gorgon. So Gorgon must come from 25% rule only.
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            plainsHex, plainsHex, player, game, ogre, ogre);
        player.addLegion(legion);

        List<CreatureType> recruits = game.findEligibleRecruits(legion, plainsHex);

        assertFalse("Ogre (6pts) should NOT recruit Gorgon (12pts) via 25% rule. "
            + "Got: " + recruits,
            recruits.contains(gorgon));
    }

    /**
     * Administrator pre-battle recruit must NOT push legion over max height.
     * A legion at 7 should not get an 8th creature from pre-battle recruit.
     */
    public void testPreBattleRecruitSkippedAtMaxHeight()
    {
        MasterHex brushHex = findHexByTerrain("Brush");
        assertNotNull(brushHex);

        CreatureType titan = variant.getCreatureByName("Titan");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");

        // Create a legion at max height (7)
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, player, game,
            titan, ogre, ogre, gargoyle, gargoyle, cyclops, cyclops);
        player.addLegion(legion);

        assertEquals("Legion should be at height 7", 7, legion.getHeight());

        // Attempt addCreature — should be rejected
        boolean added = legion.addCreature(ogre, true);
        assertFalse("addCreature must reject when legion is at max height",
            added);
        assertEquals("Legion height must remain 7", 7, legion.getHeight());
    }

    /**
     * BUG: Recruiting Cyclops with 1 Gargoyle (Administrator -1) should
     * add ONLY the Cyclops to the legion. The Gargoyle is the recruiter,
     * not an additional recruit. Legion should go from height N to N+1.
     */
    public void testRecruitOnlyAddsOneCreature()
    {
        MasterHex brushHex = findHexByTerrain("Brush");
        assertNotNull(brushHex);

        CreatureType titan = variant.getCreatureByName("Titan");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");

        // Start with [Titan, Gargoyle] = height 2
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, player, game, titan, gargoyle);
        player.addLegion(legion);

        int heightBefore = legion.getHeight();
        assertEquals("Should start at height 2", 2, heightBefore);

        // Simulate what doRecruit does: add the recruit creature
        boolean added = legion.addCreature(cyclops, true);
        assertTrue("Should successfully add Cyclops", added);

        assertEquals("After recruiting, height should be exactly +1",
            heightBefore + 1, legion.getHeight());
        assertEquals("Should have exactly 1 Gargoyle (the recruiter stays)",
            1, legion.numCreature(gargoyle));
        assertEquals("Should have exactly 1 Cyclops (the recruit)",
            1, legion.numCreature(cyclops));
    }

    /**
     * Verify a legion at height 7 cannot recruit even with Administrator.
     * This was the 8-high bug: addCreature must reject at max height.
     */
    public void testCannotRecruitAtMaxHeight()
    {
        MasterHex brushHex = findHexByTerrain("Brush");
        assertNotNull(brushHex);

        CreatureType titan = variant.getCreatureByName("Titan");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");

        // Legion at max height 7
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, player, game,
            titan, ogre, ogre, gargoyle, gargoyle, cyclops, cyclops);
        player.addLegion(legion);
        assertEquals(7, legion.getHeight());

        // Try to add — must be rejected
        boolean added = legion.addCreature(gargoyle, true);
        assertFalse("Must reject addCreature at height 7", added);
        assertEquals("Height must stay 7", 7, legion.getHeight());
    }

    /**
     * BUG: When Administrator recruits Cyclops with 1 Gargoyle in Brush,
     * the server sent numRecruiters=2 (unadjusted) to the client.
     * The client's PredictSplitNode then adds a phantom Gargoyle.
     * 
     * Fix: computeNumRecruitersForClient must return 1 (not 2) for Administrator.
     */
    public void testNumRecruitersAdjustedForAdministrator()
    {
        MasterHex brushHex = findHexByTerrain("Brush");
        assertNotNull(brushHex);

        CreatureType titan = variant.getCreatureByName("Titan");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");

        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, player, game, titan, gargoyle);
        player.addLegion(legion);

        // This calls the actual server method that computes what's sent to clients
        int numRecruiters = game.computeNumRecruitersForClient(
            legion, gargoyle, cyclops);

        assertEquals("Administrator must adjust numRecruiters from 2 to 1 "
            + "(otherwise client adds phantom Gargoyle via PredictSplitNode)",
            1, numRecruiters);
    }

    /**
     * BUG: On turn 1, legion Ogre+Ogre+Gargoyle moves to Hills and recruits
     * Minotaur. Expected: height goes from 3 to 4. Actual: height goes to 5
     * (an extra Ogre appears). This test calls doRecruit exactly once and
     * asserts height increases by exactly 1.
     */
    public void testDoRecruitAddsExactlyOneCreatureInHills()
    {
        MasterHex hillsHex = findHexByTerrain("Hills");
        assertNotNull("Must find a Hills hex", hillsHex);

        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType minotaur = variant.getCreatureByName("Minotaur");
        assertNotNull(ogre);
        assertNotNull(gargoyle);
        assertNotNull(minotaur);

        // Legion: Ogre, Ogre, Gargoyle on Hills (after split on turn 1)
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            hillsHex, hillsHex, player, game, ogre, ogre, gargoyle);
        player.addLegion(legion);

        int heightBefore = legion.getHeight();
        assertEquals("Start at height 3", 3, heightBefore);

        // Verify Minotaur is recruitable (2 Ogres in Hills, Admin -1 = 1 needed)
        List<CreatureType> recruits = game.findEligibleRecruits(legion, hillsHex);
        assertTrue("Minotaur should be recruitable with Administrator. Got: "
            + recruits, recruits.contains(minotaur));

        // Recruit Minotaur with Ogre as recruiter (the actual doRecruit path)
        game.doRecruit(legion, minotaur, ogre);

        // THE BUG: height should be exactly 4, not 5
        assertEquals("After ONE doRecruit call, height must increase by exactly 1. "
            + "Legion contents: " + legion.getCreatures(),
            heightBefore + 1, legion.getHeight());

        // Verify exact contents: 2 Ogres + 1 Gargoyle + 1 Minotaur
        assertEquals("Should have exactly 2 Ogres", 2, legion.numCreature(ogre));
        assertEquals("Should have exactly 1 Gargoyle", 1, legion.numCreature(gargoyle));
        assertEquals("Should have exactly 1 Minotaur", 1, legion.numCreature(minotaur));
    }

    /**
     * BUG REPRODUCER: Server.didRecruit() computes numRecruiters directly
     * from TerrainRecruitLoader WITHOUT the Administrator -1 adjustment.
     * For 2 Ogres recruiting Minotaur in Hills (normally needs 3 Ogres),
     * it sends numRecruiters=3 to clients. The client then "reveals" 3 Ogres
     * in PredictSplitNode.revealCreatures(), but the legion only has 2,
     * so it adds a phantom Ogre.
     *
     * This test verifies that the numRecruiters value computed in
     * Server.didRecruit() matches the ADJUSTED value (2, not 3).
     * It should FAIL because Server.didRecruit() calls
     * TerrainRecruitLoader.numberOfRecruiterNeeded() directly instead of
     * game.computeNumRecruitersForClient().
     */
    public void testServerDidRecruitSendsAdjustedNumRecruiters()
    {
        MasterHex hillsHex = findHexByTerrain("Hills");
        assertNotNull(hillsHex);

        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType minotaur = variant.getCreatureByName("Minotaur");

        // Legion: 2 Ogres + Gargoyle on Hills
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            hillsHex, hillsHex, player, game, ogre, ogre, gargoyle);
        player.addLegion(legion);

        // This is what Server.didRecruit() CURRENTLY computes (the bug):
        int numRecruitersRaw = net.sf.colossus.xmlparser.TerrainRecruitLoader
            .numberOfRecruiterNeeded(ogre, minotaur,
                hillsHex.getTerrain(), hillsHex);

        // This is what it SHOULD compute (using the adjusted method):
        int numRecruitersAdjusted = game.computeNumRecruitersForClient(
            legion, ogre, minotaur);

        // Verify: adjusted is raw - 1 (Administrator -1 reduction)
        assertEquals(
            "computeNumRecruitersForClient must return raw-1 for Administrator. "
                + "Raw=" + numRecruitersRaw + " Adjusted=" + numRecruitersAdjusted
                + ". This prevents PredictSplitNode phantom creatures.",
            numRecruitersRaw - 1, numRecruitersAdjusted);
    }

    /**
     * BUG: Administrator attacker should recruit before battle.
     * When attacker with Administrator moves onto a hex with an enemy,
     * administratorPreBattleRecruit() should auto-recruit the best
     * available creature BEFORE the battle begins.
     *
     * Setup: Attacker legion [Ogre, Ogre, Gargoyle] on Brush hex
     * with Administrator. Pre-battle should recruit Cyclops (1 Gargoyle
     * needed with Admin -1).
     */
    public void testAttackerPreBattleRecruitAddsCreature()
    {
        MasterHex brushHex = findHexByTerrain("Brush");
        assertNotNull(brushHex);

        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType gargoyle = variant.getCreatureByName("Gargoyle");
        CreatureType cyclops = variant.getCreatureByName("Cyclops");

        // Attacker legion on Brush hex with Administrator
        LegionServerSide legion = new LegionServerSide("Rd01", null,
            brushHex, brushHex, player, game, ogre, ogre, gargoyle);
        player.addLegion(legion);

        int heightBefore = legion.getHeight();
        assertEquals("Start at height 3", 3, heightBefore);

        // Verify Cyclops is recruitable (1 Gargoyle with Admin -1)
        java.util.List<CreatureType> recruits = game.findEligibleRecruits(
            legion, brushHex);
        assertTrue("Cyclops should be recruitable on Brush with Admin. Got: "
            + recruits, recruits.contains(cyclops));

        // Call the pre-battle recruit (same as engage2 would call)
        game.administratorPreBattleRecruit(legion, brushHex);

        // Assert: creature was added
        assertEquals("Pre-battle recruit should add exactly 1 creature",
            heightBefore + 1, legion.getHeight());

        // The best recruit on Brush with Gargoyle+Admin is Cyclops
        assertTrue("Should have recruited Cyclops",
            legion.numCreature(cyclops) == 1);
    }

    private MasterHex findHexByTerrain(String terrainName)
    {
        for (MasterHex hex : variant.getMasterBoard().getAllHexes())
        {
            if (hex.getTerrain().getId().equals(terrainName))
            {
                return hex;
            }
        }
        return null;
    }
}
