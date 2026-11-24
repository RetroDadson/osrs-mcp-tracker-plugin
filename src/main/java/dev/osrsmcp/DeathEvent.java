package dev.osrsmcp;

import javax.annotation.Nullable;

/**
 * POST body for {@code /death}, matching the server's DeathEvent DTO. Deliberately carries no
 * location: where a player died is not needed and is not collected.
 */
public class DeathEvent {
    public final String name;
    @Nullable
    public final String killer;

    public DeathEvent(String name, @Nullable String killer) {
        this.name = name;
        this.killer = killer;
    }
}
