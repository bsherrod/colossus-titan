package net.sf.colossus.server;

import junit.framework.TestCase;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.LauerPower;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Scripted tests for Marines power.
 * Rule: melee +floor(str/4), extra hits ceil(str/5), no terrain penalties.
 */
public class MarinesPowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player marinesPlayer;
    private Player opponent;

    public MarinesPowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        marinesPlayer = game.createAndAddPlayer("Marines", "SimpleAI");
        opponent = game.createAndAddPlayer("Opp", "SimpleAI");
        marinesPlayer.getPowers().addPower(LauerPower.MARINES);
    }

    private BattleServerSide setupBattle(CreatureType[] attackerTypes,
        CreatureType[] defenderTypes)
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3");
        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, marinesPlayer, game, attackerTypes);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, opponent, game, defenderTypes);
        marinesPlayer.addLegion(attacker);
        opponent.addLegion(defender);
        attacker.setEntrySide(EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        return game.getBattleSS();
    }

    /**
     * Marines melee bonus: Ogre (str 6) gets +floor(6/4)=+1 → 7 dice.
     */
    public void testMeleeBonusOgre()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");
        BattleServerSide battle = setupBattle(
            new CreatureType[]{ogre}, new CreatureType[]{troll});
        CreatureServerSide striker = battle.getAttackingLegion().getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion().getCritter(0);

        MasterBoardTerrain terrain = battle.getLocation().getTerrain();
        striker.setCurrentHex(terrain.getHexByLabel("C3"));
        target.setCurrentHex(terrain.getHexByLabel("C4"));

        BattleStrikeServerSide strike = new BattleStrikeServerSide(game);
        int dice = strike.getDice(striker, target, false);
        // Ogre 6 + floor(6/4)=1 = 7
        assertEquals("Marines Ogre melee dice", 7, dice);
    }

    /**
     * Marines melee bonus: Troll (str 8) gets +floor(8/4)=+2 → 10 dice.
     */
    public void testMeleeBonusTroll()
    {
        CreatureType troll = variant.getCreatureByName("Troll");
        CreatureType ogre = variant.getCreatureByName("Ogre");
        BattleServerSide battle = setupBattle(
            new CreatureType[]{troll}, new CreatureType[]{ogre});
        CreatureServerSide striker = battle.getAttackingLegion().getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion().getCritter(0);

        MasterBoardTerrain terrain = battle.getLocation().getTerrain();
        striker.setCurrentHex(terrain.getHexByLabel("C3"));
        target.setCurrentHex(terrain.getHexByLabel("C4"));

        BattleStrikeServerSide strike = new BattleStrikeServerSide(game);
        int dice = strike.getDice(striker, target, false);
        assertEquals("Marines Troll melee dice", 10, dice);
    }

    /**
     * Marines extra hits: Ogre (str 6) → ceil(6/5)=2 extra hits.
     */
    public void testExtraHitsOgre()
    {
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");
        BattleServerSide battle = setupBattle(
            new CreatureType[]{ogre}, new CreatureType[]{troll});
        CreatureServerSide creature = battle.getAttackingLegion().getCritter(0);
        // Ogre 6 + ceil(6/5)=2 = 8
        assertEquals("Marines Ogre defensivePower", 8,
            creature.getDefensivePower());
    }

    /**
     * Without Marines: base dice only.
     */
    public void testWithoutMarinesNoBonus()
    {
        marinesPlayer.getPowers().removePower(LauerPower.MARINES);
        CreatureType ogre = variant.getCreatureByName("Ogre");
        CreatureType troll = variant.getCreatureByName("Troll");
        BattleServerSide battle = setupBattle(
            new CreatureType[]{ogre}, new CreatureType[]{troll});
        CreatureServerSide striker = battle.getAttackingLegion().getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion().getCritter(0);

        MasterBoardTerrain terrain = battle.getLocation().getTerrain();
        striker.setCurrentHex(terrain.getHexByLabel("C3"));
        target.setCurrentHex(terrain.getHexByLabel("C4"));

        BattleStrikeServerSide strike = new BattleStrikeServerSide(game);
        assertEquals("No Marines = base 6", 6,
            strike.getDice(striker, target, false));
        assertEquals("No Marines defensivePower = base 6", 6,
            striker.getDefensivePower());
    }
}
