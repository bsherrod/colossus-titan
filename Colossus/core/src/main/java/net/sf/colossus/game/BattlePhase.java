/**
 *
 */
package net.sf.colossus.game;


public enum BattlePhase
{
    SUMMON("Summon", true, false, "summons"), RECRUIT("Recruit", true, false,
        "recruits"), OGG("Ogg Attack", false, false, "ogg attacks"),
        MOVE("Move", true, false, "moves"), HEAL("Heal", false, false,
        "heals"), BERSERK("Berserk",
        false, false, "declares berserkers"), DEATH_STRIKE("Death Strike",
        false, false, "declares death strikes"), FIGHT("Fight", false,
        true, "strikes"), STRIKEBACK("Strikeback", false, true,
        "strikes back"), NINJA_MOVE("Ninja Move", true, false,
        "ninja moves");

    /**
     * Determine if the phase is part of the fighting.
     *
     * @return true iff the phase is either FIGHT or STRIKEBACK.
     */
    public boolean isFightPhase()
    {
        return isFightPhase;
    }

    /**
     * Determine if the phase is part of the fighting.
     * Right now we consider also SUMMON or RECRUT as move phase
     * (currently they are own phases; I would like those actions
     *  to happen as part of the move phase instead, then this here
     *  can be changed).
     *
     * @return true iff the phase is a move phase;
     *
     */
    public boolean isMovePhase()
    {
        return isMovePhase;
    }

    /**
     * Returns a non-localized UI string for the phase.
     */
    @Override
    public String toString()
    {
        return name;
    }

    public String getDoesWhat()
    {
        return doesWhat;
    }

    private final String name;
    private final boolean isMovePhase;
    private final boolean isFightPhase;
    private final String doesWhat;

    private BattlePhase(String name, boolean isMovePhase,
        boolean isFightPhase, String doesWhat)
    {
        this.name = name;
        this.isMovePhase = isMovePhase;
        this.isFightPhase = isFightPhase;
        this.doesWhat = doesWhat;
    }
}
