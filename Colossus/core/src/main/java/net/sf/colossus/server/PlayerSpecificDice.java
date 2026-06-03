package net.sf.colossus.server;


import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.util.RandomDotOrgFileReader;


/**
 * A dice specifically owned by an individual player.
 * Supports scripted roll injection for testing: push specific values
 * onto the queue, and they'll be returned before falling back to random.
 */

public class PlayerSpecificDice
{
    private static final Logger LOGGER = Logger
        .getLogger(PlayerSpecificDice.class.getName());

    private final RandomDotOrgFileReader randomSource;

    /** Scripted rolls queue. When non-empty, nextRoll() returns from here. */
    private final Queue<Integer> scriptedRolls = new LinkedList<Integer>();

    private final int[] MovementStats = new int[6];
    private int mmCount = 0;

    private final int[] BattleStats = new int[6];
    private int bsCount = 0;

    public PlayerSpecificDice()
    {
        this.randomSource = new RandomDotOrgFileReader();
        for (int i = 0; i < 6; i++)
        {
            MovementStats[i] = 0;
            BattleStats[i] = 0;
        }
    }

    /** Push scripted movement rolls (used before random fallback). */
    public void pushScriptedRolls(int... rolls)
    {
        for (int r : rolls)
        {
            scriptedRolls.add(r);
        }
    }

    /** Clear any remaining scripted rolls. */
    public void clearScriptedRolls()
    {
        scriptedRolls.clear();
    }

    public int rollMovementDie()
    {
        int roll;
        if (!scriptedRolls.isEmpty())
        {
            roll = scriptedRolls.poll();
            LOGGER.fine("Using scripted movement roll: " + roll);
        }
        else
        {
            roll = randomSource.nextRoll();
        }
        MovementStats[roll - 1]++;
        mmCount++;
        return roll;
    }

    public int rollBattleDie()
    {
        int roll = randomSource.nextRoll();
        BattleStats[roll - 1]++;
        bsCount++;
        return roll;
    }

    public void printMovementRollStats()
    {
        printRollStats("Movement rolls", mmCount, MovementStats);
    }

    public void printBattleRollStats()
    {
        printRollStats("Battle rolls", bsCount, BattleStats);
    }

    private void printRollStats(String what, int rcount, int[] stats)
    {
        LOGGER.log(Level.FINEST, "[rstats] Current D6 distribution for "
            + what + " (" + rcount + " rolls, " + (rcount / 6) + " each):\n"
            + "[rstats] \t1: " + stats[0] + "\n" + "[rstats] \t2: " + stats[1]
            + "\n" + "[rstats] \t3: " + stats[2] + "\n" + "[rstats] \t4: "
            + stats[3] + "\n" + "[rstats] \t5: " + stats[4] + "\n"
            + "[rstats] \t6: " + stats[5]);
    }

}
