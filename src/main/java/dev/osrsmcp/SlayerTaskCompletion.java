package dev.osrsmcp;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

/** POST body for {@code /slayer-task-completed}, matching the server's SlayerTaskCompletion DTO. */
public class SlayerTaskCompletion {
    public final String name;
    public final String task;
    @Nullable
    public final String master;
    @SerializedName("amount_completed")
    public final int amountCompleted;
    public final String outcome;

    public SlayerTaskCompletion(String name, String task, @Nullable String master, int amountCompleted, String outcome) {
        this.name = name;
        this.task = task;
        this.master = master;
        this.amountCompleted = amountCompleted;
        this.outcome = outcome;
    }
}
