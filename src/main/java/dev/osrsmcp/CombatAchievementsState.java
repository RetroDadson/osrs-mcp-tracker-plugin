package dev.osrsmcp;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

/**
 * Combat Achievement tier completion counts: [easy, medium, hard, elite, master, grandmaster].
 * Plain varbit reads, no chat parsing or plugin dependency needed.
 */
public class CombatAchievementsState implements ConsumableState {
    private final transient String playerName;
    private final int[] tierCounts;

    public CombatAchievementsState(String playerName, Client client) {
        this.playerName = playerName;
        this.tierCounts = new int[]{
                client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_EASY),
                client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_MEDIUM),
                client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_HARD),
                client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_ELITE),
                client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_MASTER),
                client.getVarbitValue(VarbitID.CA_TOTAL_TASKS_COMPLETED_GRANDMASTER),
        };
    }

    @Override
    public Object get() {
        return tierCounts;
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CombatAchievementsState)) return false;

        CombatAchievementsState other = (CombatAchievementsState) o;
        for (int i = 0; i < tierCounts.length; ++i) {
            if (tierCounts[i] != other.tierCounts[i]) return false;
        }

        return true;
    }
}
