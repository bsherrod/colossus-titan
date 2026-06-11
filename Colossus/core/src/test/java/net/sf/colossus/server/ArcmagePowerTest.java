package net.sf.colossus.server;

import java.util.Set;

import junit.framework.TestCase;
import net.sf.colossus.game.Battle;
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
 * Scripted tests for Archmage power behaviors.
 */
public class ArcmagePowerTest extends TestCase
{
    private GameServerSide game;
    private Variant variant;
    private Player archmagePlayer;
    private Player opponent;

    public ArcmagePowerTest(String name)
    {
        super(name);
    }

    @Override
    protected void setUp()
    {
        variant = VariantSupport.loadVariantByName("Default", true);
        game = GameServerSide.makeNewGameServerSide(variant);
        archmagePlayer = game.createAndAddPlayer("Archmage", "SimpleAI");
        opponent = game.createAndAddPlayer("Opp", "SimpleAI");
        archmagePlayer.getPowers().addPower(LauerPower.ARCHMAGE);
    }

    private BattleServerSide setupBattle(CreatureType[] attackerCreatures,
        CreatureType[] defenderCreatures)
    {
        MasterHex hex = variant.getMasterBoard().getHexByLabel("3"); // Brush
        LegionServerSide attacker = new LegionServerSide("Rd03", null,
            hex, hex, archmagePlayer, game, attackerCreatures);
        LegionServerSide defender = new LegionServerSide("Bu03", null,
            hex, hex, opponent, game, defenderCreatures);
        archmagePlayer.addLegion(attacker);
        opponent.addLegion(defender);
        attacker.setEntrySide(EntrySide.RIGHT);
        game.createBattle(attacker, defender,
            BattleServerSide.LegionTags.ATTACKER, hex, BattlePhase.FIGHT);
        return game.getBattleSS();
    }

    private void placeCreature(BattleServerSide battle,
        net.sf.colossus.game.Creature creature, String hexLabel)
    {
        MasterBoardTerrain terrain = battle.getLocation().getTerrain();
        BattleHex battleHex = terrain.getHexByLabel(hexLabel);
        creature.setCurrentHex(battleHex);
    }

    /**
     * Archmage: Titan is a rangestriker (can find rangestrike targets).
     */
    public void testTitanCanRangestrike()
    {
        CreatureType titan = variant.getCreatureByName("Titan");
        CreatureType troll = variant.getCreatureByName("Troll");
        assertFalse("Titan is not a natural rangestriker",
            titan.isRangestriker());

        BattleServerSide battle = setupBattle(
            new CreatureType[]{titan}, new CreatureType[]{troll});

        CreatureServerSide archmageTitan = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion()
            .getCritter(0);

        // Titan skill=4, range 4 should be fine
        placeCreature(battle, archmageTitan, "D1");
        placeCreature(battle, target, "D4");

        // Archmage Titan should be treated as rangestriker
        assertTrue("Archmage Titan is a rangestriker",
            archmageTitan.isRangestriker());

        Set<BattleHex> targets = battle.findTargetHexes(archmageTitan, true);
        assertTrue("Archmage Titan must have rangestrike targets",
            targets.contains(target.getCurrentHex()));
    }

    /**
     * Archmage: Titan uses magic missile (warlock advantage -- ignores LOS).
     */
    public void testTitanUsesMagicMissile()
    {
        CreatureType titan = variant.getCreatureByName("Titan");
        CreatureType troll = variant.getCreatureByName("Troll");

        BattleServerSide battle = setupBattle(
            new CreatureType[]{titan}, new CreatureType[]{troll});

        CreatureServerSide archmageTitan = battle.getAttackingLegion()
            .getCritter(0);

        // Archmage Titan should use magic missile (warlock abilities)
        assertTrue("Archmage Titan uses magic missile",
            archmageTitan.useMagicMissile());
    }

    /**
     * Archmage: Titan rangestrike dice = power/2.
     */
    public void testTitanRangestrikeDice()
    {
        CreatureType titan = variant.getCreatureByName("Titan");
        CreatureType troll = variant.getCreatureByName("Troll");

        BattleServerSide battle = setupBattle(
            new CreatureType[]{titan}, new CreatureType[]{troll});

        CreatureServerSide archmageTitan = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion()
            .getCritter(0);

        placeCreature(battle, archmageTitan, "D1");
        placeCreature(battle, target, "D4");

        BattleStrikeServerSide strike = new BattleStrikeServerSide(game);
        int dice = strike.getDice(archmageTitan, target, true);
        // Titan power starts at 6 (6*4 lord), rangestrike = 6/2 = 3
        int expectedDice = archmageTitan.getPower() / 2;
        assertEquals("Archmage Titan rangestrike dice = power/2",
            expectedDice, dice);
    }

    /**
     * Without Archmage, Titan cannot rangestrike.
     */
    public void testTitanCannotRangestrikeWithoutArchmage()
    {
        archmagePlayer.getPowers().removePower(LauerPower.ARCHMAGE);

        CreatureType titan = variant.getCreatureByName("Titan");
        CreatureType troll = variant.getCreatureByName("Troll");

        BattleServerSide battle = setupBattle(
            new CreatureType[]{titan}, new CreatureType[]{troll});

        CreatureServerSide normalTitan = battle.getAttackingLegion()
            .getCritter(0);
        CreatureServerSide target = battle.getDefendingLegion()
            .getCritter(0);

        placeCreature(battle, normalTitan, "D1");
        placeCreature(battle, target, "D4");

        assertFalse("Normal Titan is NOT a rangestriker",
            normalTitan.isRangestriker());

        Set<BattleHex> targets = battle.findTargetHexes(normalTitan, true);
        assertFalse("Normal Titan must NOT have rangestrike targets",
            targets.contains(target.getCurrentHex()));
    }

    /**
     * Archmage: canTitanTeleport returns true on roll of 5.
     * Note: GameServerSide.getMovementRoll() delegates to activePlayer's roll,
     * so we must set the roll on the PlayerServerSide, not Game.
     */
    public void testCanTitanTeleportOnRollOf5()
    {
        ((PlayerServerSide)archmagePlayer).setMovementRoll(5);
        assertTrue("Archmage player can titan teleport on roll 5",
            archmagePlayer.canTitanTeleport());
    }

    /**
     * Archmage: canTitanTeleport on roll of 4 with score=0 is false.
     */
    public void testCannotTitanTeleportOnRollOf4WithLowScore()
    {
        ((PlayerServerSide)archmagePlayer).setMovementRoll(4);
        assertFalse("Archmage player cannot teleport on roll 4 with 0 score",
            archmagePlayer.canTitanTeleport());
    }
}
