package dev.osrsmcp;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

/** POST body for {@code /death}, matching the server's DeathEvent DTO. */
public class DeathEvent {
    public final String name;
    @SerializedName("world_x")
    @Nullable
    public final Integer worldX;
    @SerializedName("world_y")
    @Nullable
    public final Integer worldY;
    @Nullable
    public final Integer plane;
    @Nullable
    public final String killer;

    public DeathEvent(String name, @Nullable Integer worldX, @Nullable Integer worldY,
                       @Nullable Integer plane, @Nullable String killer) {
        this.name = name;
        this.worldX = worldX;
        this.worldY = worldY;
        this.plane = plane;
        this.killer = killer;
    }
}
