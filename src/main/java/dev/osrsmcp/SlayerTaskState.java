package dev.osrsmcp;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Current Slayer task snapshot. Derived entirely from varps/varbits + a game data-table
 * lookup (the same mechanism RuneLite's own SlayerPlugin uses internally, confirmed via
 * its source) - no chat parsing, no dependency on SlayerPlugin itself.
 * <p>
 * Field names use {@code @SerializedName} to match the server's snake_case JSON contract
 * (the Rust side derives serde field names verbatim from Rust naming conventions).
 */
@Getter
public class SlayerTaskState implements ConsumableState {
    private final transient String playerName;
    private final String task;
    @Nullable
    private final String master;
    @SerializedName("amount_remaining")
    private final int amountRemaining;
    @SerializedName("initial_amount")
    private final int initialAmount;
    private final int points;
    @SerializedName("task_streak")
    private final int taskStreak;
    @SerializedName("wilderness_streak")
    private final int wildernessStreak;

    /** Exposed for the plugin's task-change detection, not part of the wire payload. */
    public final transient int taskId;

    public SlayerTaskState(String playerName, String task, @Nullable String master, int amountRemaining,
                            int initialAmount, int points, int taskStreak, int wildernessStreak, int taskId) {
        this.playerName = playerName;
        this.task = task;
        this.master = master;
        this.amountRemaining = amountRemaining;
        this.initialAmount = initialAmount;
        this.points = points;
        this.taskStreak = taskStreak;
        this.wildernessStreak = wildernessStreak;
        this.taskId = taskId;
    }

    /**
     * Reads the current task from the client, returning {@code null} when no task is
     * assigned (taskId &lt;= 0) - callers should skip updating the snapshot in that case
     * rather than sending a fabricated empty task.
     */
    @Nullable
    public static SlayerTaskState fromClient(String playerName, Client client) {
        int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
        if (taskId <= 0) return null;

        int amountRemaining = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
        int initialAmount = client.getVarpValue(VarPlayerID.SLAYER_COUNT_ORIGINAL);
        int points = client.getVarbitValue(VarbitID.SLAYER_POINTS);
        int taskStreak = client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED);
        int wildernessStreak = client.getVarbitValue(VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED);

        String taskName = resolveTaskName(client, taskId);
        if (taskName == null) return null;

        // NOTE: no reliable non-chat source found for the assigning master's identity -
        // left null (best-effort, matches the plan's documented gap).
        return new SlayerTaskState(playerName, taskName, null, amountRemaining, initialAmount,
                points, taskStreak, wildernessStreak, taskId);
    }

    /**
     * Pure, testable: the only signal available (without chat parsing) for whether a task
     * was finished normally is whether its kill count reached zero before the assigning varp
     * changed. Skip vs. cancel can't be told apart from this alone, so both collapse to
     * "unknown" rather than guessing.
     */
    public static String inferOutcome(int amountRemainingAtChange) {
        return amountRemainingAtChange <= 0 ? "completed" : "unknown";
    }

    @Nullable
    private static String resolveTaskName(Client client, int taskId) {
        List<Integer> rowIds = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, taskId);
        if (rowIds.isEmpty()) return null;

        Object[] field = client.getDBTableField(rowIds.get(0), DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
        if (field == null || field.length == 0 || !(field[0] instanceof String)) return null;

        return (String) field[0];
    }

    @Override
    public Object get() {
        return this;
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SlayerTaskState)) return false;

        SlayerTaskState other = (SlayerTaskState) o;
        return taskId == other.taskId
                && amountRemaining == other.amountRemaining
                && points == other.points
                && taskStreak == other.taskStreak
                && wildernessStreak == other.wildernessStreak;
    }
}
