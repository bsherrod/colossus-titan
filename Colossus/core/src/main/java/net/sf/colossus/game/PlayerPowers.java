package net.sf.colossus.game;


import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;


/**
 * Holds the set of Lauer Powers assigned to a player.
 * Wraps an EnumSet for type safety and provides serialization.
 */
public class PlayerPowers
{
    private final EnumSet<LauerPower> powers;

    public PlayerPowers()
    {
        this.powers = EnumSet.noneOf(LauerPower.class);
    }

    private PlayerPowers(EnumSet<LauerPower> powers)
    {
        this.powers = powers;
    }

    public boolean hasPower(LauerPower p)
    {
        return powers.contains(p);
    }

    public Set<LauerPower> getPowers()
    {
        return Collections.unmodifiableSet(powers);
    }

    public void addPower(LauerPower p)
    {
        powers.add(p);
    }

    public void removePower(LauerPower p)
    {
        powers.remove(p);
    }

    public boolean isEmpty()
    {
        return powers.isEmpty();
    }

    /**
     * Serializes to comma-separated enum names, e.g. "ARCHER,HEALER,NINJA".
     * Empty set serializes to empty string.
     */
    public String serialize()
    {
        if (powers.isEmpty())
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (LauerPower p : powers)
        {
            if (sb.length() > 0)
            {
                sb.append(",");
            }
            sb.append(p.name());
        }
        return sb.toString();
    }

    /**
     * Deserializes from comma-separated enum names.
     * Null or empty string returns empty set.
     */
    public static PlayerPowers deserialize(String s)
    {
        PlayerPowers pp = new PlayerPowers();
        if (s == null || s.trim().isEmpty())
        {
            return pp;
        }
        String[] parts = s.split(",");
        for (String part : parts)
        {
            String trimmed = part.trim();
            if (!trimmed.isEmpty())
            {
                pp.powers.add(LauerPower.valueOf(trimmed));
            }
        }
        return pp;
    }
}
