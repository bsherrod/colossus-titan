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
